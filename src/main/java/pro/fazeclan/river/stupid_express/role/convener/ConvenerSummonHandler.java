package pro.fazeclan.river.stupid_express.role.convener;

import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import pro.fazeclan.river.stupid_express.cca.AbilityCooldownComponent;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.record.StupidExpressReplay;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerMomentumComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerPlayerComponent;

import java.util.List;
import java.util.UUID;

public class ConvenerSummonHandler {

    /**
     * 召集冷却：90 秒。
     */
    public static final int SUMMON_COOLDOWN_TICKS = 90 * 20;

    /**
     * 其他玩家被强制变形成尸体样貌的持续时间：30 秒。
     */
    public static final int SUMMON_MORPH_TICKS = 30 * 20;

    /**
     * 每次成功召集后都会给全局计时器额外增加 60 秒。
     */
    public static final int SUMMON_TIME_BONUS_TICKS = 60 * 20;

    /**
     * 成功召集后，召集者自己的爆发加速持续时间：15 秒。
     */
    public static final int SUMMON_SPEED_DURATION_TICKS = 15 * 20;

    /**
     * 成功召集后，召集者速度变成原来的 2 倍。
     *
     * <p>属性 modifier 这里写的是“额外 +100%”，
     * 也就是最终值 = 原值 * (1 + 1.0) = 原来的 2 倍。</p>
     */
    public static final double SUMMON_SPEED_MULTIPLIER_BONUS = 0.7D;

    private ConvenerSummonHandler() {}

    public static void init() {
        UseEntityCallback.EVENT.register((player, level, interactionHand, entity, entityHitResult) -> {
            if (!(player instanceof ServerPlayer convener)) {
                return InteractionResult.PASS;
            }
            if (!(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.PASS;
            }
            if (!convener.gameMode.isSurvival()) {
                return InteractionResult.PASS;
            }

            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(serverLevel);
            if (!gameWorldComponent.isRole(convener, SERoles.CONVENER)) {
                return InteractionResult.PASS;
            }
            if (!(entity instanceof PlayerBodyEntity body)) {
                return InteractionResult.PASS;
            }

            AbilityCooldownComponent cooldownComponent = AbilityCooldownComponent.KEY.get(convener);
            if (cooldownComponent.hasCooldown()) {
                return InteractionResult.PASS;
            }

            List<ServerPlayer> alivePlayers = serverLevel.getPlayers(GameFunctions::isPlayerAliveAndSurvival);
            if (alivePlayers.isEmpty()) {
                return InteractionResult.PASS;
            }

            UUID disguiseTarget = body.getPlayerUuid();
            double targetX = body.getX();
            double targetY = body.getY();
            double targetZ = body.getZ();

            // 先销毁尸体，再执行后续逻辑，保证一具尸体只能被成功召集一次。
            body.remove(Entity.RemovalReason.DISCARDED);

            // 召集者自己会立刻同步成尸体原主的外观，并且保持到主动解除或下一次召集。
            ConvenerDisguiseComponent.KEY.get(convener).setPersistentDisguise(disguiseTarget);

            // 所有存活玩家都会被瞬移到尸体位置，并获得 30 秒的强制变形。
            for (ServerPlayer alivePlayer : alivePlayers) {
                alivePlayer.teleportTo(targetX, targetY, targetZ);

                if (alivePlayer == convener) {
                    continue;
                }

                // 被召集的玩家如果正处于疯魔状态，则这里强制结束疯魔。
                // Wathe 客户端的疯魔皮肤渲染是直接根据 psychoTicks 是否大于 0 判断，
                // 所以只要这里 stop + sync，外观就会自动退回玩家原本皮肤。
                PlayerPsychoComponent psychoComponent = PlayerPsychoComponent.KEY.get(alivePlayer);
                if (psychoComponent.getPsychoTicks() > 0) {
                    psychoComponent.stopPsycho();
                    psychoComponent.sync();
                }

                // 物品、技能、特殊事件的封控只对“被召集的其他活人”生效，
                // 不影响召集者自己，避免把召集者一并锁死。
                ConvenerSummonLockdownHelper.applySummonLockdown(alivePlayer);
                ConvenerDisguiseComponent.KEY.get(alivePlayer).setTimedDisguise(disguiseTarget, SUMMON_MORPH_TICKS);
            }

            // 召集进度与已解锁头像都挂在召集者自己的组件上，客户端界面会直接读取这里。
            ConvenerPlayerComponent convenerComponent = ConvenerPlayerComponent.KEY.get(convener);
            ConvenerMomentumComponent.KEY.get(convener).activate();

            // 在真正结算本次召集前，再按“本局真实参局人数”兜底重算一次目标值。
            // 这样即便开局分配身份时暂时拿到了错误的 1，也会在第一次召集前被修正回来。
            convenerComponent.setRequiredSummons(ConvenerWinHelper.getRequiredSummons(serverLevel));
            convenerComponent.unlockDisguise(disguiseTarget);
            convenerComponent.incrementSummonCount();
            convenerComponent.sync();
            /*
             * 召集事件需要带上：
             * 1. 这次用的是谁的尸体；
             * 2. 当前召集进度；
             * 3. 本局目标次数。
             *
             * 这里放在进度真正累加之后记录，回放里展示的就是“本次成功后的最新进度”。
             */
            CompoundTag extra = new CompoundTag();
            extra.putUUID("corpse_owner", disguiseTarget);
            extra.putInt("summon_count", convenerComponent.getSummonCount());
            extra.putInt("required_summons", convenerComponent.getRequiredSummons());
            GameRecordManager.recordGlobalEvent(serverLevel, StupidExpressReplay.CONVENER_SUMMON_EVENT, convener, extra);

            GameTimeComponent.KEY.get(serverLevel).addTime(SUMMON_TIME_BONUS_TICKS);

            cooldownComponent.setCooldown(SUMMON_COOLDOWN_TICKS);
            cooldownComponent.sync();

            if (convenerComponent.hasReachedSummonGoal()) {
                ConvenerWinHelper.declareConvenerWin(serverLevel, convener);
            }

            return InteractionResult.CONSUME;
        });
    }
}
