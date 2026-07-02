package pro.fazeclan.river.stupid_express.mixin.modifier.lovers;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.cca.CustomWinnerComponent;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.modifier.lovers.LoversPairComponent;

import java.util.List;
import java.util.UUID;

@Mixin(value = MurderGameMode.class, priority = 900)
public class LoversLoopMixin {

    @Inject(
            method = "tickServerGameLoop",
            at = @At(
                    value = "FIELD",
                    target = "Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;NONE:Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;",
                    ordinal = 3,
                    opcode = Opcodes.GETSTATIC
            ),
            cancellable = true
    )
    private void loversWinCheck(
            ServerLevel serverWorld,
            GameWorldComponent gameWorldComponent,
            CallbackInfo ci,
            @Local(name = "winStatus") GameFunctions.WinStatus winStatus
    ) {

        var config = StupidExpress.CONFIG;
        var loversAlive = false;

        var modifierComponent = WorldModifierComponent.KEY.get(serverWorld);
        var pairComponent = LoversPairComponent.KEY.get(serverWorld);

        var remainingPlayers = serverWorld.getPlayers(GameFunctions::isPlayerAliveAndSurvival);
        var remainingLovers = remainingPlayers.stream().filter(p -> modifierComponent.isModifier(p, SEModifiers.LOVERS)).toList();
        var remainingLoverUuids = remainingLovers.stream().map(ServerPlayer::getUUID).toList();

        /*
         * 多对恋人不能再用“剩余恋人数量是否等于 1”判断殉情。
         * 现在每个恋人都有自己的伴侣 UUID；只要自己的伴侣不在存活恋人列表里，
         * 这个玩家就会因为失去恋人而死亡。
         */
        for (ServerPlayer lover : remainingLovers) {
            UUID partnerUuid = pairComponent.getPartnerOrFallback(lover.getUUID(), remainingLoverUuids);
            if (partnerUuid == null || !remainingLoverUuids.contains(partnerUuid)) {
                GameFunctions.killPlayer(lover, true, null, StupidExpress.id("broken_heart"));
                return;
            }
        }

        for (ServerPlayer player : remainingLovers) {
            loversAlive = true;

            // 检查是否满足“只有恋人获胜”的条件
            if (remainingPlayers.size() == remainingLovers.size()) {
                var ce = CustomWinnerComponent.KEY.get(serverWorld);
                ce.setWinningTextId(SEModifiers.LOVERS.identifier().getPath());
                ce.setWinners(remainingLovers);
                ce.setColor(SEModifiers.LOVERS.color());
                ce.sync();

                GameRoundEndComponent.KEY.get(serverWorld)
                        .setRoundEndData(serverWorld.players(), GameFunctions.WinStatus.KILLERS);

                GameFunctions.stopGame(serverWorld);
                ci.cancel();
                return;
            }

            // 检查是否满足“与杀手恋人”获胜条件.
            if (config.modifiersSection.loversSection.loversWinWithKillers) {
                var lover = stupidexpress$getLivingPartner(player, pairComponent, remainingLoverUuids, remainingLovers);
                if (lover == null) {
                    continue;
                }
                if (gameWorldComponent.isInnocent(player) && gameWorldComponent.isInnocent(lover)) {
                    continue;
                }
                var remainingNonInnocent = remainingPlayers.stream().filter(hostile -> !gameWorldComponent.isInnocent(hostile)).toList();
                if (remainingPlayers.size() - 1 != remainingNonInnocent.size()) {
                    continue;
                }
                GameRoundEndComponent.KEY.get(serverWorld)
                        .setRoundEndData(serverWorld.players(), GameFunctions.WinStatus.KILLERS);
                GameFunctions.stopGame(serverWorld);
                ci.cancel();
                return;
            }
        }

        // 检查如果恋人不能和乘客阵营一起获胜的时候，则让游戏继续进行下去
        if (loversAlive
                && !config.modifiersSection.loversSection.loversWinWithCivilians
                && (winStatus == GameFunctions.WinStatus.KILLERS || winStatus == GameFunctions.WinStatus.PASSENGERS)) {
            ci.cancel();
        }

    }

    private static ServerPlayer stupidexpress$getLivingPartner(
            ServerPlayer player,
            LoversPairComponent pairComponent,
            List<UUID> remainingLoverUuids,
            List<ServerPlayer> remainingLovers
    ) {
        UUID partnerUuid = pairComponent.getPartnerOrFallback(player.getUUID(), remainingLoverUuids);
        if (partnerUuid == null) {
            return null;
        }
        for (ServerPlayer lover : remainingLovers) {
            if (partnerUuid.equals(lover.getUUID())) {
                return lover;
            }
        }
        return null;
    }

}
