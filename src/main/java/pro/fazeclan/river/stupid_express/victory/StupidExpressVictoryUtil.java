package pro.fazeclan.river.stupid_express.victory;

import dev.doctor4t.wathe.api.win.CustomVictory;
import dev.doctor4t.wathe.api.win.VictoryApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * StupidExpress 胜利规则的公共小工具。
 *
 * <p>各职业 / 词条自己的规则类都放回了对应分类目录里，
 * 但“把一组玩家写成 Wathe 自定义独立胜利”的代码完全相同。
 * 抽到这里可以避免五六个规则类重复写 CustomVictory.builder(...)。</p>
 */
public final class StupidExpressVictoryUtil {
    private StupidExpressVictoryUtil() {
    }

    public static VictoryApi.VictoryResult customWin(
            ResourceLocation id,
            int color,
            List<? extends Player> winners
    ) {
        /*
         * Wathe 的 CustomVictory 只保存 UUID，不保存 Player 实体。
         * 这样即使结算期间玩家离线，客户端仍然可以根据结算快照判断谁是真正赢家。
         */
        return VictoryApi.VictoryResult.customWin(
                CustomVictory.builder(id, color)
                        .winnersFromPlayers(winners)
                        .build()
        );
    }
}
