package pro.fazeclan.river.stupid_express.role.convener;

import dev.doctor4t.wathe.api.event.AllowPlayerDeath;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.record.StupidExpressReplay;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerPlayerComponent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 召集者的特殊死亡免疫处理。
 *
 * <p>目前这里只拦截 NoellesRoles 的巫毒魔法死亡，
 * 覆盖来源包括：
 * 1. 巫毒师 voodoo 的巫毒魔法；
 * 2. guesser 猜中/猜错后触发的 voodoo 魔法死亡。</p>
 */
public final class ConvenerDeathProtectionHandler {

    /**
     * NoellesRoles 的巫毒魔法死亡原因。
     *
     * <p>这里直接按命名空间和路径匹配，
     * 既能避免额外硬依赖字段初始化顺序，
     * 也能精确只拦巫毒魔法，不误伤其他死亡类型。</p>
     */
    public static final ResourceLocation VOODOO_MAGIC_DEATH_REASON = ResourceLocation.fromNamespaceAndPath("noellesroles", "voodoo");

    /**
     * 召集者反伤击杀所使用的专属死因。
     *
     * <p>单独使用一个新的死因，方便验尸官 / 日志 / 结算文本都能精确识别。</p>
     */
    public static final ResourceLocation COUNTER_KILL_DEATH_REASON = ResourceLocation.fromNamespaceAndPath("stupid_express", "convener_counter_kill");

    /**
     * 正在处理“召集者护盾救命”的玩家集合。
     *
     * <p>主要用来避免极端链式死亡里，同一个召集者在同一轮递归里重复进入护盾结算。</p>
     */
    private static final Set<UUID> SHIELD_PROCESSING = new HashSet<>();

    /**
     * 正在处理“召集者反伤”的攻击者集合。
     *
     * <p>这层保护是参考 NoellesRoles 的 controller / voodoo 连锁死亡防护思路，
     * 防止某些模组在 killPlayer 链式调用时，把同一个攻击者重复拉回反伤流程里，
     * 最终导致递归循环甚至把服务器堆栈打爆。</p>
     */
    private static final Set<UUID> COUNTER_KILL_PROCESSING = new HashSet<>();

    private ConvenerDeathProtectionHandler() {}

    public static void init() {
        AllowPlayerDeath.EVENT.register((playerEntity, killer, identifier) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(playerEntity.level());
            if (!gameWorldComponent.isRole(playerEntity, SERoles.CONVENER)) {
                return true;
            }

            if (!FabricLoader.getInstance().isModLoaded("noellesroles")) {
                return handleCounterShield(playerEntity, killer, identifier);
            }
            if (VOODOO_MAGIC_DEATH_REASON.equals(identifier)) {
                if (playerEntity instanceof net.minecraft.server.level.ServerPlayer serverConvener) {
                    net.minecraft.nbt.CompoundTag pendingDeathData = GameFunctions.getPendingExtraDeathData();
                    UUID voodooCasterUuid = pendingDeathData != null && pendingDeathData.contains("replay_actor")
                            ? pendingDeathData.getUUID("replay_actor")
                            : killer != null ? killer.getUUID() : null;
                    if (voodooCasterUuid != null) {
                        net.minecraft.nbt.CompoundTag extra = new net.minecraft.nbt.CompoundTag();
                        extra.putUUID("voodoo_player", voodooCasterUuid);
                        GameRecordManager.recordGlobalEvent(serverConvener.serverLevel(), StupidExpressReplay.CONVENER_VOODOO_IMMUNITY_EVENT, serverConvener, extra);
                    }
                }
                return false;
            }
            return handleCounterShield(playerEntity, killer, identifier);
        });
    }

    /**
     * 处理召集者的“反伤护盾”救命逻辑。
     */
    private static boolean handleCounterShield(net.minecraft.world.entity.player.Player playerEntity, net.minecraft.world.entity.player.Player killer, ResourceLocation identifier) {
        if (!StupidExpress.CONFIG.rolesSection.convenerSection.convenerCounterShieldEnabled) {
            return true;
        }

        ConvenerPlayerComponent convenerComponent = ConvenerPlayerComponent.KEY.get(playerEntity);
        if (!convenerComponent.hasCounterShield()) {
            return true;
        }

        // 反伤产生的专属死因不再继续触发召集者护盾，
        // 否则两个反伤/反伤 + 其它连锁机制很容易来回弹死，形成无限递归。
        if (COUNTER_KILL_DEATH_REASON.equals(identifier)) {
            return true;
        }

        if (!SHIELD_PROCESSING.add(playerEntity.getUUID())) {
            return true;
        }

        try {
            if (!convenerComponent.consumeCounterShield()) {
                return true;
            }
            convenerComponent.sync();
            /*
             * 召集者护盾的“挡住了什么伤害”这里单独接入回放。
             * source 用 stupidexpress 自己的专属来源 id，
             * 这样格式化器就能渲染成“反伤护盾抵挡住了来自谁的什么伤害”。
             */
            if (playerEntity instanceof net.minecraft.server.level.ServerPlayer serverConvener) {
                GameRecordManager.recordShieldBlocked(
                        serverConvener,
                        killer instanceof net.minecraft.server.level.ServerPlayer serverKiller ? serverKiller : null,
                        StupidExpressReplay.CONVENER_COUNTER_SHIELD_SOURCE,
                        GameFunctions.resolveDamageItemForBlockedDeath(killer, identifier),
                        buildBlockedExtra(identifier)
                );
            }

            // 护盾先生效保命，然后再尝试把造成这次死亡的攻击者反杀。
            // 如果这次死亡没有明确攻击者，或者攻击者就是召集者自己，
            // 那就只保命，不做反伤。
            if (killer != null && killer != playerEntity && GameFunctions.isPlayerAliveAndSurvival(killer)) {
                tryCounterKill(playerEntity, killer);
            }

            return false;
        } finally {
            SHIELD_PROCESSING.remove(playerEntity.getUUID());
        }
    }

    /**
     * 尝试执行一次召集者反伤。
     *
     * <p>如果攻击者最终没有立刻死亡，说明其自身还有额外保护（如其它护盾 / 护甲 / 特殊机制）。
     * 这时按你的要求，把攻击者心情值直接清零。</p>
     */
    private static void tryCounterKill(net.minecraft.world.entity.player.Player convener, net.minecraft.world.entity.player.Player killer) {
        if (!COUNTER_KILL_PROCESSING.add(killer.getUUID())) {
            return;
        }

        try {
            GameFunctions.killPlayer(killer, true, convener, COUNTER_KILL_DEATH_REASON);

            if (GameFunctions.isPlayerAliveAndSurvival(killer)) {
                PlayerMoodComponent.KEY.get(killer).setMood(0f);
            }
        } finally {
            COUNTER_KILL_PROCESSING.remove(killer.getUUID());
        }
    }

    /**
     * 把本次原始死亡原因附带进“护盾挡伤”回放里。
     *
     * <p>这样如果伤害不是传统武器造成的，例如巫毒魔法之类，
     * Wathe 的默认兜底格式化器仍然可以退回用 death reason 名称显示，
     * 不会再掉成“未知物品”。</p>
     */
    private static CompoundTag buildBlockedExtra(ResourceLocation identifier) {
        CompoundTag extra = new CompoundTag();
        extra.putString("death_reason", identifier.toString());
        return extra;
    }
}
