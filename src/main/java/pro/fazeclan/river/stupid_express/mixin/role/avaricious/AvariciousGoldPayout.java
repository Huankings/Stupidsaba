package pro.fazeclan.river.stupid_express.mixin.role.avaricious;

import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.CompoundTag;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.record.StupidExpressReplay;
import pro.fazeclan.river.stupid_express.role.avaricious.AvariciousGoldHandler;
import pro.fazeclan.river.stupid_express.role.avaricious.cca.AvariciousPayoutComponent;

@Mixin(MurderGameMode.class)
public class AvariciousGoldPayout {

    @Inject(
            method = "tickServerGameLoop",
            at = @At("TAIL")
    )
    private void payout(
            ServerLevel serverWorld, GameWorldComponent gameWorldComponent, CallbackInfo ci
    ) {
        GameTimeComponent timeComponent = GameTimeComponent.KEY.get(serverWorld);
        int time = timeComponent.time;

        AvariciousPayoutComponent payoutComponent = AvariciousPayoutComponent.KEY.get(serverWorld);
        if (!payoutComponent.hasTimerStartTime()) {
            /*
             * Wathe 的 GameTimeComponent 是“剩余时间”，会随对局推进递减。
             * 这里在服务端首次进入扒手结算逻辑时记录起点，并同步给客户端 HUD。
             * 后续发钱和 HUD 倒计时都按这个起点算 elapsed，保证显示时间和真实结算点一致。
             */
            payoutComponent.setTimerStartTime(time);
            return;
        }

        int elapsed = payoutComponent.getTimerStartTime() - time;

        if (elapsed % AvariciousGoldHandler.TIMER_TICKS != 0) return;

        for (ServerPlayer player : serverWorld.players()) {
            if (!gameWorldComponent.isRole(player, SERoles.AVARICIOUS)) continue;

            int nearbyPlayers = 0;
            for (ServerPlayer other : serverWorld.players()) {
                if (GameFunctions.isPlayerEliminated(other)) continue;
                if (other == player) continue;
                if (other.distanceTo(player) <= AvariciousGoldHandler.MAX_DISTANCE)
                    nearbyPlayers++;
            }

            if (nearbyPlayers > 0) {
                /*
                 * 扒手的这次收益是“附近人数 * 每人结算金币”。
                 * 用户现在希望回放展示的是最终总额，而不是逐个受害者名单，
                 * 因此这里直接算出一次结算的总金币数，只记一条事件。
                 */
                int stolenAmount = nearbyPlayers * AvariciousGoldHandler.PAYOUT_PER_PLAYER;
                PlayerShopComponent.KEY.get(player).addToBalance(stolenAmount);
                player.playNotifySound(WatheSounds.UI_SHOP_BUY, SoundSource.PLAYERS, 10.0f, 0.5f);
                CompoundTag extra = new CompoundTag();
                extra.putInt("amount", stolenAmount);
                GameRecordManager.recordGlobalEvent(serverWorld, StupidExpressReplay.AVARICIOUS_STOLE_COINS_EVENT, player, extra);
            }
        }
    }

}
