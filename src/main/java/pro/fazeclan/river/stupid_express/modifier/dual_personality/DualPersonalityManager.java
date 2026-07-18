package pro.fazeclan.river.stupid_express.modifier.dual_personality;

import dev.doctor4t.wathe.api.PlayerLifeStateApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.record.StupidExpressReplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 双重人格的服务端状态机。
 *
 * <p>这个类负责把“词条”变成真实玩法：普通阶段每 60 秒轮换活跃人格，
 * 休眠人格被服务端维持为特殊存活旁观；活跃人格受到致命伤害时进入双活阶段；
 * 双活结束后再走 Wathe 的正常击杀/结算流程。</p>
 */
public final class DualPersonalityManager {

    // 普通阶段的人格轮换间隔：60 秒。
    public static final int SWITCH_INTERVAL_TICKS = 60 * 20;
    // 双活阶段基础时长：40 秒。
    public static final int DOUBLE_ACTIVE_BASE_TICKS = 40 * 20;
    // 双活期间每次成功匕首击杀额外奖励 10 秒。
    public static final int DOUBLE_ACTIVE_KILL_BONUS_TICKS = 10 * 20;
    // 双活期间匕首冷却压到 1 秒，具体覆盖点在 KnifeStabPayload mixin。
    public static final int DOUBLE_ACTIVE_KNIFE_COOLDOWN_TICKS = 20;
    // 客户端速度提示常量，实际速度倍率在客户端 getSpeed mixin 中实现。
    public static final int DOUBLE_ACTIVE_SPEED_PERCENT = 50;
    // 双重人格统一使用的显示颜色：词条、HUD、actionbar、胜利文本都复用它。
    public static final int COLOR = 0x7633db;
    // 开局身份提示延后 2 秒发送，避免和开局发词条公告的 actionbar 挤在一起。
    public static final int INITIAL_ROLE_MESSAGE_DELAY_TICKS = 2 * 20;

    // 双活倒计时归零时使用的自定义死亡原因，用于 replay 和死亡提示格式化。
    public static final ResourceLocation DOUBLE_ACTIVE_TIMEOUT_DEATH_REASON =
            StupidExpress.id("dual_active_timeout");

    /*
     * 双活超时需要真正杀死两个人格。
     * 但我们又在 tryInterceptFatalDeath 中拦截普通致命死亡来开启双活。
     * 这个集合是“本次死亡是我们主动触发的超时死亡”的临时标记，
     * 防止超时死亡再次被拦截成新的双活，形成死循环。
     */
    private static final Set<UUID> FORCE_TIMEOUT_DEATHS = new HashSet<>();
    /**
     * 客户端同步上来的“人格切换键显示文本”。
     *
     * <p>这个字符串是按玩家客户端当前绑定实际同步过来的，不再是语言文件里的“功能名称”。
     * 这样 actionbar 才能显示成“按下 U 键”或“按下 1 键”，而不是“按下双重人格切换键键”。</p>
     */
    private static final Map<UUID, String> SWITCH_KEY_LABELS = new HashMap<>();

    private DualPersonalityManager() {
    }

    public static void init() {
        // 服务端每 tick 维护普通轮换、休眠状态压制、双活倒计时。
        ServerTickEvents.END_SERVER_TICK.register(DualPersonalityManager::tickServer);
        // 掉线/重连会影响 active/dormant 的控制权，需要专门修正状态。
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> handleDisconnect(handler.getPlayer()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> handleJoin(handler.getPlayer()));

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            // 休眠人格即使客户端意外发出交互，也在服务端拒绝，避免旁观态点尸体/按钮/实体。
            if (player instanceof ServerPlayer serverPlayer && DualPersonalityActionGuard.isDormant(serverPlayer)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    public static void refreshModifierMaximum(ServerLevel level, List<ServerPlayer> players) {
        /*
         * Harpy 的随机词条池用 MODIFIER_MAX 控制是否可能抽到词条。
         * 双重人格至少需要两个人，同时还支持配置“达到多少参局人数才进池”。
         */
        int minPlayers = Math.max(2, StupidExpress.CONFIG.modifiersSection.dualPersonalitySection.dualPersonalityMinPlayerSpawn);
        Harpymodloader.MODIFIER_MAX.put(SEModifiers.DUAL_PERSONALITY.identifier(), players.size() >= minPlayers ? 1 : 0);
    }

    public static void requestEarlySwitch(ServerPlayer player) {
        // 客户端 Y 键只是一条请求，真正能不能提前切换必须由服务端检查当前状态。
        if (!isActiveRound(player.level())) {
            return;
        }
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(player.level());
        DualPersonalityComponent.PairState pair = component.getPair(player.getUUID());
        if (pair == null || pair.doubleActive || pair.paused || !pair.isActive(player.getUUID())) {
            return;
        }

        switchPersonalities(player.serverLevel(), component, pair, true);
    }

    public static boolean tryInterceptFatalDeath(ServerPlayer victim) {
        /*
         * 这个方法在 Wathe 清除 aliveOverride 之前调用。
         * 普通致命伤害会被取消并转成“双活”；双活超时死亡则通过 FORCE_TIMEOUT_DEATHS 放行。
         */
        if (FORCE_TIMEOUT_DEATHS.contains(victim.getUUID())) {
            return false;
        }
        if (!isActiveRound(victim.level())) {
            return false;
        }

        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(victim.level());
        DualPersonalityComponent.PairState pair = component.getPair(victim.getUUID());
        if (pair == null || pair.doubleActive || !pair.isActive(victim.getUUID())) {
            return false;
        }

        UUID partnerUuid = pair.getPartner(victim.getUUID());
        if (partnerUuid == null || victim.server.getPlayerList().getPlayer(partnerUuid) == null) {
            // 另一人格不在线时不强行开启双活，交给 Wathe 原死亡流程处理。
            return false;
        }

        enterDoubleActive(victim.serverLevel(), component, pair, victim);
        return true;
    }

    public static void onSuccessfulKill(ServerPlayer killer, Entity victim, ResourceLocation deathReason) {
        // 只有双活阶段的匕首击杀加时间，其他死因或普通阶段都不影响倒计时。
        if (!"knife_stab".equals(deathReason.getPath())) {
            return;
        }
        if (!isActiveRound(killer.level())) {
            return;
        }
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(killer.level());
        DualPersonalityComponent.PairState pair = component.getPair(killer.getUUID());
        if (pair == null || !pair.doubleActive || !pair.contains(killer.getUUID())) {
            return;
        }
        if (victim instanceof ServerPlayer serverVictim && GameFunctions.isPlayerAliveAndSurvival(serverVictim)) {
            return;
        }

        pair.doubleActiveTicks += DOUBLE_ACTIVE_KILL_BONUS_TICKS;
        component.sync();
    }

    public static boolean shouldBlockRevolverPickup(ServerPlayer player, ItemEntity itemEntity) {
        /*
         * 用户确认“好人阵营不能捡枪”按 Wathe 的 isInnocent 判断。
         * 所以这里不只挡普通乘客，也会挡义警等被 Wathe 归为 innocent 的身份。
         */
        if (player == null || itemEntity == null || !itemEntity.getItem().is(WatheItems.REVOLVER)) {
            return false;
        }
        if (!isActiveRound(player.level())) {
            return false;
        }
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(player.level());
        if (!component.isDoubleActive(player.getUUID())) {
            return false;
        }
        return GameWorldComponent.KEY.get(player.level()).isInnocent(player);
    }

    public static boolean shouldSuppressInnocentRevolverPenalty(
            ServerPlayer shooter,
            Player target,
            boolean targetNormallyInnocent
    ) {
        /*
         * Wathe 的左轮惩罚是先问“目标是否 innocent”，再决定是否反噬、掉枪和清空理智值。
         * 双活阶段的好人双重人格已经进入独立杀戮窗口，此时其他好人开枪阻止他，
         * 不应该再被 Wathe 当成“好人误伤好人”惩罚。
         */
        if (shooter == null || target == null || !targetNormallyInnocent) {
            return false;
        }
        if (shooter.getUUID().equals(target.getUUID()) || shooter.level() != target.level()) {
            return false;
        }
        if (!isActiveRound(target.level())) {
            return false;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(target.level());
        if (!gameWorldComponent.isInnocent(shooter)) {
            return false;
        }

        /*
         * 只在“双活中的双重人格目标”上豁免惩罚。
         * 普通轮换阶段仍然保留 Wathe 原本的好人误伤惩罚，避免好人随意枪击休眠/活跃人格。
         */
        return DualPersonalityComponent.KEY.get(target.level()).isDoubleActive(target.getUUID());
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            tickLevel(level);
        }
    }

    private static void tickLevel(ServerLevel level) {
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(level);
        if (component.getPairs().isEmpty()) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(level);
        if (gameWorldComponent.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE) {
            /*
             * Wathe 的 isRunning() 在 STOPPING 结算阶段也会返回 true。
             * 双重人格的相机、休眠锁和双活倒计时只属于真正的 ACTIVE 局内阶段，
             * 进入结算后就不再维护，防止残留状态继续影响玩家。
             */
            return;
        }

        // tick 中可能移除配对，所以复制一份列表迭代，避免边遍历边修改原列表。
        for (DualPersonalityComponent.PairState pair : new ArrayList<>(component.getPairs())) {
            if (pair.doubleActive) {
                tickDoubleActive(level, component, pair);
            } else {
                tickRotatingPair(level, component, pair);
            }
        }
    }

    private static void tickRotatingPair(ServerLevel level, DualPersonalityComponent component, DualPersonalityComponent.PairState pair) {
        ServerPlayer active = level.getServer().getPlayerList().getPlayer(pair.active);
        ServerPlayer dormant = level.getServer().getPlayerList().getPlayer(pair.dormant);

        /*
         * 普通轮换阶段最怕掉线导致控制权卡死。
         * active 掉线时，让原 dormant 临时接管；dormant 掉线时，只暂停倒计时等待回归。
         */
        if (active == null) {
            handleActiveOffline(level, component, pair);
            return;
        }
        if (dormant == null) {
            handleDormantOffline(component, pair, active);
            return;
        }

        ensureActive(active);
        ensureDormant(dormant, active);
        // 休眠人格每 tick 都压技能/物品冷却，属于服务端防绕过兜底。
        DualPersonalityActionGuard.maintainDormantLock(dormant);

        if (pair.paused) {
            // 暂停时仍维持 active/dormant 的模式和相机，但不继续扣轮换倒计时。
            return;
        }

        if (!pair.initialMessageSent) {
            /*
             * 开局身份提示不再和词条/阵营的开局 actionbar 同步发送。
             * 先让它自己空转 2 秒，再发出“你当前为谁、当前人格状态是什么”的提示，
             * 这样开局时信息不会挤成一团。
             */
            if (pair.initialMessageDelayTicks > 0) {
                pair.initialMessageDelayTicks--;
            }
            if (pair.initialMessageDelayTicks <= 0) {
                sendInitialRoleMessages(active, dormant, pair);
                pair.initialMessageSent = true;
                component.sync();
            }
        }

        pair.switchTicks--;
        sendCountdownWarnings(active, dormant, pair.switchTicks);
        if (pair.switchTicks <= 0) {
            // 倒计时归零自动互换 active/dormant。
            switchPersonalities(level, component, pair, false);
        } else if (pair.switchTicks % 20 == 0) {
            // 客户端只需要秒级刷新，没必要每 tick 同步完整世界组件。
            component.sync();
        }
    }

    public static void updateSwitchKeyLabel(UUID playerUuid, String keyLabel) {
        if (playerUuid == null) {
            return;
        }

        /*
         * 这里存的不是语言文件里的“按钮名称”，而是客户端当前绑定的实际显示文本。
         * 客户端会在按键改动时重新同步，所以这个缓存既能显示当前按键，也能在服务器端独立发 actionbar 时直接复用。
         */
        String sanitized = keyLabel == null ? "" : keyLabel.trim();
        if (sanitized.isEmpty()) {
            SWITCH_KEY_LABELS.remove(playerUuid);
        } else {
            SWITCH_KEY_LABELS.put(playerUuid, sanitized);
        }
    }

    private static void tickDoubleActive(ServerLevel level, DualPersonalityComponent component, DualPersonalityComponent.PairState pair) {
        ServerPlayer main = level.getServer().getPlayerList().getPlayer(pair.main);
        ServerPlayer sub = level.getServer().getPlayerList().getPlayer(pair.sub);

        /*
         * 双活阶段允许被正常伤害提前杀死。
         *
         * 之前这里只要玩家对象还在线，就每 tick 无条件 ensureActive。
         * Wathe 真正击杀后会把玩家切成 spectator；下一 tick 又被这里拉回 adventure，
         * 结果表现成“双活期间谁都杀不死”。所以必须先按 Wathe 的局内存活判定过滤：
         * 已经死亡的双重人格不再恢复模式、不再被相机/工具逻辑拉回局内。
         */
        boolean mainAlive = main != null && GameFunctions.isPlayerAliveAndSurvival(main);
        boolean subAlive = sub != null && GameFunctions.isPlayerAliveAndSurvival(sub);

        // 双活阶段仍存活的人格才恢复为正常可行动玩家，同时持续执行好人禁枪规则。
        if (mainAlive) {
            ensureActive(main);
            removeRevolversFromInnocent(main);
        }
        if (subAlive) {
            ensureActive(sub);
            removeRevolversFromInnocent(sub);
        }

        if (!mainAlive && !subAlive) {
            // 两个人格都已被提前击杀时，双活状态已经没有继续倒计时的对象。
            component.removePair(pair.main);
            return;
        }

        pair.doubleActiveTicks--;
        if (pair.doubleActiveTicks <= 0) {
            // 时间耗尽后只强杀仍然存活的人格；已经提前死亡的人不能再次生成尸体/回放。
            if (mainAlive) {
                forceTimeoutDeath(main);
            }
            if (subAlive) {
                forceTimeoutDeath(sub);
            }
            component.removePair(pair.main);
            return;
        }

        if (pair.doubleActiveTicks % 20 == 0) {
            component.sync();
        }
    }

    private static void switchPersonalities(
            ServerLevel level,
            DualPersonalityComponent component,
            DualPersonalityComponent.PairState pair,
            boolean manual
    ) {
        // 人格切换只交换当前控制权，不改变 main/sub 的身份方向。
        UUID oldActive = pair.active;
        pair.active = pair.dormant;
        pair.dormant = oldActive;
        pair.switchTicks = SWITCH_INTERVAL_TICKS;
        pair.paused = false;
        pair.pauseReason = DualPersonalityComponent.PauseReason.NONE;

        ServerPlayer active = level.getServer().getPlayerList().getPlayer(pair.active);
        ServerPlayer dormant = level.getServer().getPlayerList().getPlayer(pair.dormant);
        if (active != null) {
            // 新活跃人格恢复冒险模式和自己的视角。
            ensureActive(active);
        }
        if (dormant != null && active != null) {
            // 新休眠人格进入特殊存活旁观，并把相机固定到活跃人格。
            ensureDormant(dormant, active);
        }

        sendSwitchedMessage(active, true);
        sendSwitchedMessage(dormant, false);
        component.sync();
    }

    private static void enterDoubleActive(
            ServerLevel level,
            DualPersonalityComponent component,
            DualPersonalityComponent.PairState pair,
            ServerPlayer trigger
    ) {
        // 致命伤害触发解离：普通轮换停止，两个人格同时获得行动权和双活倒计时。
        pair.doubleActive = true;
        pair.paused = false;
        pair.pauseReason = DualPersonalityComponent.PauseReason.NONE;
        pair.doubleActiveTicks = DOUBLE_ACTIVE_BASE_TICKS;

        ServerPlayer main = level.getServer().getPlayerList().getPlayer(pair.main);
        ServerPlayer sub = level.getServer().getPlayerList().getPlayer(pair.sub);

        if (main != null) {
            activateDoubleActivePlayer(main);
        }
        if (sub != null) {
            activateDoubleActivePlayer(sub);
        }

        sendActionbar(main, Component.translatable("message.stupid_express.dual_personality.dissociated"));
        sendActionbar(sub, Component.translatable("message.stupid_express.dual_personality.dissociated"));

        GameRecordManager.recordGlobalEvent(level, StupidExpressReplay.DUAL_ACTIVE_STARTED_EVENT, trigger, null);
        component.sync();
    }

    private static void activateDoubleActivePlayer(ServerPlayer player) {
        // 双活启动时保证基础杀戮工具存在；如果背包已有同类物品就不重复塞。
        ensureActive(player);
        giveIfMissing(player, WatheItems.KNIFE);
        giveIfMissing(player, WatheItems.CROWBAR);
        removeRevolversFromInnocent(player);
    }

    private static void giveIfMissing(ServerPlayer player, Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return;
            }
        }
        player.addItem(item.getDefaultInstance());
    }

    private static void removeRevolversFromInnocent(ServerPlayer player) {
        if (!GameWorldComponent.KEY.get(player.level()).isInnocent(player)) {
            return;
        }
        // 双活时如果好人阵营已经持有左轮，也立即掉到地上，保持“不能持枪”的规则一致。
        removeRevolversFromList(player, player.getInventory().items);
        removeRevolversFromList(player, player.getInventory().offhand);
    }

    private static void removeRevolversFromList(ServerPlayer player, List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.is(WatheItems.REVOLVER)) {
                continue;
            }
            player.drop(stack.copy(), true, false);
            stacks.set(i, ItemStack.EMPTY);
        }
    }

    private static void ensureActive(ServerPlayer player) {
        /*
         * changeGameModeAsGameplayAlive 会同时处理 Wathe 的“特殊存活”状态。
         * 只有模式或 aliveOverride 不对时才调用，减少每 tick 重复发包和状态抖动。
         */
        if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE || PlayerLifeStateApi.hasAliveOverride(player)) {
            PlayerLifeStateApi.changeGameModeAsGameplayAlive(player, GameType.ADVENTURE);
        }
        player.setCamera(player);
    }

    private static void ensureDormant(ServerPlayer dormant, ServerPlayer active) {
        /*
         * 休眠人格不是死亡，而是“Wathe 仍认为存活”的旁观。
         * 这样胜利判定仍能把他算作活着的人格，但实际操作权被旁观模式和 ActionGuard 锁住。
         */
        if (dormant.gameMode.getGameModeForPlayer() != GameType.SPECTATOR || !PlayerLifeStateApi.hasAliveOverride(dormant)) {
            PlayerLifeStateApi.changeGameModeAsGameplayAlive(dormant, GameType.SPECTATOR);
        }
        dormant.setCamera(active);
        dormant.teleportTo(active.getX(), active.getY(), active.getZ());
    }

    private static void handleDormantOffline(
            DualPersonalityComponent component,
            DualPersonalityComponent.PairState pair,
            ServerPlayer active
    ) {
        // 休眠人格掉线不改变当前控制权，只暂停倒计时，避免在线的活跃人格被切到离线玩家身上。
        if (pair.paused && pair.pauseReason == DualPersonalityComponent.PauseReason.DORMANT_OFFLINE) {
            return;
        }
        pair.paused = true;
        pair.pauseReason = DualPersonalityComponent.PauseReason.DORMANT_OFFLINE;
        sendActionbar(active, Component.translatable(
                "message.stupid_express.dual_personality.dormant_left",
                Component.literal(nameOf(active.server, pair.dormant))
        ));
        component.sync();
    }

    private static void handleActiveOffline(
            ServerLevel level,
            DualPersonalityComponent component,
            DualPersonalityComponent.PairState pair
    ) {
        /*
         * 活跃人格掉线时，如果休眠人格在线，就立即把休眠人格提为活跃人格。
         * 同时暂停倒计时，等原活跃人格回来后作为休眠人格继续这一组关系。
         */
        ServerPlayer oldDormant = level.getServer().getPlayerList().getPlayer(pair.dormant);
        if (oldDormant == null) {
            pair.paused = true;
            pair.pauseReason = DualPersonalityComponent.PauseReason.ACTIVE_OFFLINE;
            component.sync();
            return;
        }
        if (pair.paused && pair.pauseReason == DualPersonalityComponent.PauseReason.ACTIVE_OFFLINE && pair.isActive(oldDormant.getUUID())) {
            return;
        }

        UUID disconnectedActive = pair.active;
        pair.active = oldDormant.getUUID();
        pair.dormant = disconnectedActive;
        pair.switchTicks = SWITCH_INTERVAL_TICKS;
        pair.paused = true;
        pair.pauseReason = DualPersonalityComponent.PauseReason.ACTIVE_OFFLINE;

        ensureActive(oldDormant);
        sendActionbar(oldDormant, Component.translatable(
                "message.stupid_express.dual_personality.active_left",
                Component.literal(nameOf(level.getServer(), disconnectedActive))
        ));
        component.sync();
    }

    private static void handleDisconnect(ServerPlayer player) {
        // Fabric 的断线事件比 tick 更早发现玩家离开，可以更快地修正相机/控制权。
        SWITCH_KEY_LABELS.remove(player.getUUID());
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(player.level());
        DualPersonalityComponent.PairState pair = component.getPair(player.getUUID());
        if (pair == null || pair.doubleActive) {
            return;
        }

        if (pair.isDormant(player.getUUID())) {
            ServerPlayer active = player.server.getPlayerList().getPlayer(pair.active);
            if (active != null) {
                handleDormantOffline(component, pair, active);
            }
        } else if (pair.isActive(player.getUUID())) {
            handleActiveOffline(player.serverLevel(), component, pair);
        }
    }

    private static void handleJoin(ServerPlayer player) {
        // 重连后根据暂停原因恢复普通轮换，并重新把休眠人格相机锁到活跃人格。
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(player.level());
        DualPersonalityComponent.PairState pair = component.getPair(player.getUUID());
        if (pair == null || pair.doubleActive) {
            return;
        }

        ServerPlayer active = player.server.getPlayerList().getPlayer(pair.active);
        ServerPlayer dormant = player.server.getPlayerList().getPlayer(pair.dormant);
        if (active == null || dormant == null) {
            return;
        }

        if (pair.paused && pair.pauseReason == DualPersonalityComponent.PauseReason.DORMANT_OFFLINE && pair.isDormant(player.getUUID())) {
            pair.paused = false;
            pair.pauseReason = DualPersonalityComponent.PauseReason.NONE;
            sendActionbar(active, Component.translatable("message.stupid_express.dual_personality.dormant_returned", player.getDisplayName()));
            sendActionbar(dormant, Component.translatable("message.stupid_express.dual_personality.dormant_returned", player.getDisplayName()));
        } else if (pair.paused && pair.pauseReason == DualPersonalityComponent.PauseReason.ACTIVE_OFFLINE && pair.isDormant(player.getUUID())) {
            pair.paused = false;
            pair.pauseReason = DualPersonalityComponent.PauseReason.NONE;
            pair.switchTicks = SWITCH_INTERVAL_TICKS;
            sendActionbar(active, Component.translatable("message.stupid_express.dual_personality.active_returned", player.getDisplayName()));
            sendActionbar(dormant, Component.translatable("message.stupid_express.dual_personality.active_returned", player.getDisplayName()));
        }

        ensureDormant(dormant, active);
        component.sync();
    }

    private static void forceTimeoutDeath(ServerPlayer player) {
        if (player == null) {
            return;
        }
        // 临时标记本次 killPlayer 为“双活超时强制死亡”，让死亡拦截器放行 Wathe 原流程。
        FORCE_TIMEOUT_DEATHS.add(player.getUUID());
        try {
            GameFunctions.killPlayer(player, true, null, DOUBLE_ACTIVE_TIMEOUT_DEATH_REASON, new CompoundTag());
        } finally {
            FORCE_TIMEOUT_DEATHS.remove(player.getUUID());
        }
    }

    private static void sendInitialRoleMessages(
            ServerPlayer active,
            ServerPlayer dormant,
            DualPersonalityComponent.PairState pair
    ) {
        sendActionbar(active, Component.translatable(
                "message.stupid_express.dual_personality.initial_state",
                Component.translatable(pair.isMain(active.getUUID()) ? "text.stupid_express.dual_personality.main" : "text.stupid_express.dual_personality.sub"),
                Component.translatable("text.stupid_express.dual_personality.active")
        ));
        sendActionbar(dormant, Component.translatable(
                "message.stupid_express.dual_personality.initial_state",
                Component.translatable(pair.isMain(dormant.getUUID()) ? "text.stupid_express.dual_personality.main" : "text.stupid_express.dual_personality.sub"),
                Component.translatable("text.stupid_express.dual_personality.dormant")
        ));
    }

    private static Component getSwitchKeyLabelText(ServerPlayer player) {
        String keyLabel = player == null ? null : SWITCH_KEY_LABELS.get(player.getUUID());
        if (keyLabel == null || keyLabel.isBlank()) {
            keyLabel = "U";
        }
        return Component.literal(keyLabel);
    }

    private static void sendCountdownWarnings(ServerPlayer active, ServerPlayer dormant, int ticksLeft) {
        int secondsLeft = ticksLeft / 20;
        if (ticksLeft == 30 * 20 || ticksLeft == 15 * 20 || ticksLeft == 8 * 20) {
            // actionbar 只在关键秒数提示，避免覆盖玩家其它提示过于频繁。
            sendActionbar(active, Component.translatable(
                    "message.stupid_express.dual_personality.switch_countdown",
                    secondsLeft,
                    Component.translatable("text.stupid_express.dual_personality.can"),
                    getSwitchKeyLabelText(active)
            ));
            sendActionbar(dormant, Component.translatable(
                    "message.stupid_express.dual_personality.switch_countdown",
                    secondsLeft,
                    Component.translatable("text.stupid_express.dual_personality.cannot"),
                    getSwitchKeyLabelText(dormant)
            ));
        } else if (ticksLeft == 3 * 20) {
            sendActionbar(active, Component.translatable("message.stupid_express.dual_personality.switch_soon"));
            sendActionbar(dormant, Component.translatable("message.stupid_express.dual_personality.switch_soon"));
        }
    }

    private static void sendSwitchedMessage(ServerPlayer player, boolean active) {
        if (player == null) {
            return;
        }
        sendActionbar(player, Component.translatable(
                "message.stupid_express.dual_personality.switched",
                Component.translatable(active ? "text.stupid_express.dual_personality.active" : "text.stupid_express.dual_personality.dormant"),
                Component.translatable(active ? "text.stupid_express.dual_personality.can" : "text.stupid_express.dual_personality.cannot"),
                getSwitchKeyLabelText(player)
        ));
    }

    public static void sendActionbar(ServerPlayer player, Component message) {
        if (player == null) {
            return;
        }
        // 统一染成双重人格颜色，便于玩家和其它角色提示区分。
        MutableComponent colored = Component.empty().append(message).withStyle(style -> style.withColor(COLOR));
        player.displayClientMessage(colored, true);
    }

    public static void clearRoundState(ServerLevel level) {
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(level);
        if (component.getPairs().isEmpty()) {
            FORCE_TIMEOUT_DEATHS.clear();
            SWITCH_KEY_LABELS.clear();
            return;
        }

        /*
         * 这里用于“最终离开本局”时清掉双重人格运行态。
         * 注意不要在 stopGame 刚进入 STOPPING 时调用：
         * 结算黑幕/死亡展示阶段仍需要主副人格关系来维持副人格的主人格外观和准星名字。
         * 等 Wathe finalizeGame 把玩家传回准备大厅后再清，才不会让结算画面提前露馅。
         */
        for (DualPersonalityComponent.PairState pair : new ArrayList<>(component.getPairs())) {
            releaseCamera(level, pair.main);
            releaseCamera(level, pair.sub);
        }
        component.clear();
        FORCE_TIMEOUT_DEATHS.clear();
        SWITCH_KEY_LABELS.clear();
    }

    public static boolean isActiveRound(net.minecraft.world.level.Level level) {
        return level != null
                && GameWorldComponent.KEY.get(level).getGameStatus() == GameWorldComponent.GameStatus.ACTIVE;
    }

    private static void releaseCamera(ServerLevel level, UUID playerUuid) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            player.setCamera(player);
        }
    }

    private static String nameOf(MinecraftServer server, UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player.getScoreboardName();
        }
        return ForcedDualPersonalityManager.describePlayer(uuid);
    }
}
