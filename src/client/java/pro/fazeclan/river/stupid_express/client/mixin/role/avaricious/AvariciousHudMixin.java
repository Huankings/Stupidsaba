package pro.fazeclan.river.stupid_express.client.mixin.role.avaricious;

import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.avaricious.AvariciousGoldHandler;
import pro.fazeclan.river.stupid_express.role.avaricious.cca.AvariciousPayoutComponent;

@Mixin(Gui.class)
public abstract class AvariciousHudMixin {

    private static final int GOLD_TEXT_COLOR = 0xFFAA00;
    private static final int RIGHT_MARGIN = 8;
    private static final int BOTTOM_MARGIN = 10;

    @Shadow
    public abstract Font getFont();

    @Inject(method = "render", at = @At("TAIL"))
    private void stupidexpress$renderAvariciousHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }

        DebugScreenOverlay debugOverlay = client.getDebugOverlay();
        if (debugOverlay != null && debugOverlay.showDebugScreen()) {
            return;
        }

        LocalPlayer player = client.player;
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
        if (!gameWorldComponent.isRole(player, SERoles.AVARICIOUS)) {
            return;
        }

        /*
         * HUD 只给正在参与本局的扒手看。
         * 这样玩家死亡、旁观或创造模式调试时，不会残留一份与玩法状态无关的倒计时。
         */
        if (!WatheClient.isPlayerAliveAndInSurvival() && !player.isCreative()) {
            return;
        }

        Component timerLine = Component.translatable(
                "hud.stupid_express.avaricious.payout_timer",
                getSecondsUntilNextPayout(player)
        );
        Component expectedPayoutLine = Component.translatable(
                "hud.stupid_express.avaricious.expected_payout",
                getExpectedPayout(player)
        );

        Font font = this.getFont();
        int baseX = guiGraphics.guiWidth() - RIGHT_MARGIN;
        int payoutLineY = guiGraphics.guiHeight() - BOTTOM_MARGIN;
        int timerLineY = payoutLineY - font.lineHeight - 2;

        guiGraphics.drawString(
                font,
                timerLine,
                baseX - font.width(timerLine),
                timerLineY,
                GOLD_TEXT_COLOR,
                true
        );
        guiGraphics.drawString(
                font,
                expectedPayoutLine,
                baseX - font.width(expectedPayoutLine),
                payoutLineY,
                SERoles.AVARICIOUS.color(),
                true
        );
    }

    private static int getSecondsUntilNextPayout(LocalPlayer player) {
        GameTimeComponent timeComponent = GameTimeComponent.KEY.get(player.level());
        AvariciousPayoutComponent payoutComponent = AvariciousPayoutComponent.KEY.get(player.level());

        if (!payoutComponent.hasTimerStartTime()) {
            /*
             * 服务端刚开局时需要一个 tick 来写入并同步起点。
             * 在客户端还没收到起点前，先显示完整周期，避免出现负数或跳动文本。
             */
            return Math.max(1, (AvariciousGoldHandler.TIMER_TICKS + 19) / 20);
        }

        /*
         * GameTimeComponent.time 是倒计时，所以 elapsed 要用“起点 - 当前剩余时间”。
         * 这个算法和服务端 AvariciousGoldPayout 完全一致，HUD 才会严格对齐真实发钱时刻。
         */
        int elapsed = Math.max(0, payoutComponent.getTimerStartTime() - timeComponent.time);
        int remainder = elapsed % AvariciousGoldHandler.TIMER_TICKS;
        int ticksRemaining = remainder == 0
                ? AvariciousGoldHandler.TIMER_TICKS
                : AvariciousGoldHandler.TIMER_TICKS - remainder;

        return Math.max(1, (ticksRemaining + 19) / 20);
    }

    private static int getExpectedPayout(LocalPlayer player) {
        int nearbyPlayers = 0;
        for (Player other : player.level().players()) {
            if (other == player) {
                continue;
            }

            /*
             * 服务端结算时会排除已淘汰玩家；客户端这里复用同一个 Wathe 判定，
             * 让“预计可偷取”尽量等同于如果此刻结算时的真实收益。
             */
            if (GameFunctions.isPlayerEliminated(other)) {
                continue;
            }

            if (other.distanceTo(player) <= AvariciousGoldHandler.MAX_DISTANCE) {
                nearbyPlayers++;
            }
        }

        return nearbyPlayers * AvariciousGoldHandler.PAYOUT_PER_PLAYER;
    }
}
