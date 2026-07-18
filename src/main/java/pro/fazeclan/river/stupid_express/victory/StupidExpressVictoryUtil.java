package pro.fazeclan.river.stupid_express.victory;

import dev.doctor4t.wathe.api.win.CustomVictory;
import dev.doctor4t.wathe.api.win.VictoryApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

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

    public static VictoryApi.VictoryResult customWinUuids(
            ResourceLocation id,
            int color,
            Collection<UUID> winnerUuids
    ) {
        /*
         * 词条独立胜利和普通职业独立胜利有一个重要差别：
         * 普通职业通常只需要把“当前还活着并触发胜利的玩家”写进 winners；
         * 但恋人 / 双重人格是一组词条阵营，另一半即使已经死亡，也应该跟随同组玩家一起算进胜利阵营。
         *
         * 因此这里额外提供 UUID 入口，允许规则类直接把“本局属于这个词条阵营的玩家 UUID”
         * 写入 Wathe 的 CustomVictory。Wathe 结算快照本来就保存了死亡玩家的 UUID 和职业显示信息，
         * 客户端拿到这些 UUID 后，会把死亡者带红叉画在右侧“独立胜利阵营”，而不是误分到左侧“其他”。
         */
        return VictoryApi.VictoryResult.customWin(
                CustomVictory.builder(id, color)
                        .winners(winnerUuids)
                        .build()
        );
    }
}
