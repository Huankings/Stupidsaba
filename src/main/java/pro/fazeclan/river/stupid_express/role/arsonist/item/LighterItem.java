package pro.fazeclan.river.stupid_express.role.arsonist.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.win.CustomVictory;
import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.arsonist.cca.DousedPlayerComponent;

import java.util.List;

public class LighterItem extends Item {

    public LighterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        GameWorldComponent gwc = GameWorldComponent.KEY.get(level);

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(player.getItemInHand(interactionHand));
        }
        if (!gwc.isRole(player, SERoles.ARSONIST)) {
            return InteractionResultHolder.pass(player.getItemInHand(interactionHand));
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(player.getItemInHand(interactionHand));
        }
        var server = player.getServer();
        var players = server.getPlayerList().getPlayers();
        var alivePlayers = players.stream().filter(GameFunctions::isPlayerAliveAndSurvival).toList();
        var dousedPlayers = alivePlayers.stream().filter(p -> DousedPlayerComponent.KEY.get(p).isDoused()).toList();
        if (dousedPlayers.size() >= (int) (alivePlayers.size() * 0.3)) {
            for (ServerPlayer doused : dousedPlayers) {
                GameFunctions.killPlayer(doused, true, player, StupidExpress.id("ignited"));
                DousedPlayerComponent.KEY.get(doused).reset();
            }
            player.playNotifySound(SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

            var playersLeft = players.stream().filter(GameFunctions::isPlayerAliveAndSurvival).count();
            if (playersLeft == 1) {
                /*
                 * 点火后只剩纵火犯时，直接把胜利交给 Wathe 的公开接口。
                 * 这样服务端胜负、客户端顶部公告、右侧独立胜利阵营和 didWin 音效都由本体统一处理。
                 */
                VictoryApi.endGameWithCustomVictory(
                        serverLevel,
                        CustomVictory.of(SERoles.ARSONIST.identifier(), SERoles.ARSONIST.color(), List.of(player))
                );
            }
        } else {
            player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
            GameFunctions.killPlayer(player, true, player, StupidExpress.id("failed_ignite"));
        }
        return InteractionResultHolder.pass(player.getItemInHand(interactionHand));
    }
}
