package pro.fazeclan.river.stupid_express.role.arsonist;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import pro.fazeclan.river.stupid_express.constants.SEItems;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.record.StupidExpressReplay;
import pro.fazeclan.river.stupid_express.role.arsonist.cca.DousedPlayerComponent;

public class OilDousingHandler {

    public static void init() {
        UseEntityCallback.EVENT.register(((player, level, interactionHand, entity, entityHitResult) -> {
            if (!(player instanceof ServerPlayer interacting)) {
                return InteractionResult.PASS;
            }
            if (!interacting.gameMode.isSurvival()) {
                return InteractionResult.PASS;
            }
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
            if (!gameWorldComponent.isRole(player, SERoles.ARSONIST)) {
                return InteractionResult.PASS;
            }
            if (!(entity instanceof ServerPlayer victim)) {
                return InteractionResult.PASS;
            }

            var item = player.getItemInHand(interactionHand);
            if (!item.is(SEItems.JERRY_CAN)) {
                return InteractionResult.PASS;
            }
            if (interacting.getCooldowns().isOnCooldown(item.getItem())) {
                return InteractionResult.PASS;
            }
            if (interacting.gameMode.isSurvival()) {
                var alivePlayers = ((ServerLevel) level).getPlayers(GameFunctions::isPlayerAliveAndSurvival);
                var playerCount = alivePlayers.size();
                var dousedPlayers = alivePlayers.stream().filter(p -> DousedPlayerComponent.KEY.get(p).isDoused()).toList();
                var cd = 45 - (5/3.0) * (double) playerCount;

                if (playerCount > 15) {
                    cd = 20;
                }

                interacting.getCooldowns().addCooldown(item.getItem(), (int) (cd * 20));
                if (dousedPlayers.size() >= (int) (alivePlayers.size() * 0.3)) {
                    boolean lighterWasOnCooldown = interacting.getCooldowns().isOnCooldown(SEItems.LIGHTER);
                    interacting.getCooldowns().addCooldown(SEItems.LIGHTER, (int) (cd * 20));
                    /*
                     * 只有当打火机是“本次第一次进入冷却”时，才记录开始事件。
                     * 如果它原本就在冷却中，这里只是刷新/延长冷却，不重复播报。
                     */
                    if (!lighterWasOnCooldown) {
                        GameRecordManager.recordGlobalEvent(interacting.serverLevel(), StupidExpressReplay.ARSONIST_LIGHTER_COOLDOWN_STARTED_EVENT, interacting, null);
                        StupidExpressReplay.trackLighterCooldown(interacting);
                    }
                }
            }
            DousedPlayerComponent doused = DousedPlayerComponent.KEY.get(victim);
            doused.setDoused(true);
            doused.sync();
            /*
             * 只有真正成功把“被浇油状态”写进目标组件之后，才记录这次纵火行为，
             * 这样可以避免冷却中、拿错物品等失败交互也混进回放。
             */
            CompoundTag extra = new CompoundTag();
            extra.putUUID("target_player", victim.getUUID());
            GameRecordManager.recordGlobalEvent(interacting.serverLevel(), StupidExpressReplay.ARSONIST_DOUSED_EVENT, interacting, extra);

            interacting.playNotifySound(SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 1.0f);

            return InteractionResult.CONSUME;
        }));
    }

}
