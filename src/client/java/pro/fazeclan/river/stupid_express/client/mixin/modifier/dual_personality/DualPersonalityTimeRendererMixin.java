package pro.fazeclan.river.stupid_express.client.mixin.modifier.dual_personality;

import dev.doctor4t.wathe.client.gui.TimeRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager;

@Mixin(TimeRenderer.class)
public class DualPersonalityTimeRendererMixin {

    @Inject(method = "renderHud", at = @At("HEAD"), cancellable = true)
    private static void stupidexpress$renderDoubleActiveTimer(
            Font renderer,
            LocalPlayer player,
            GuiGraphics context,
            float delta,
            CallbackInfo ci
    ) {
        /*
         * 双活阶段的倒计时比原本的回合时间更重要。
         * 当组件里存在 doubleActiveTicks 时，直接接管 Wathe 的时间 HUD，显示双活剩余时间。
         * 注意这里必须只认 ACTIVE，对局 STOPPING 结算时 Wathe 的 isRunning() 仍为 true，
         * 如果不加这道守卫，双活时间会在胜利画面继续残留。
         */
        int time = DualPersonalityClientState.getDoubleActiveTicks(player);
        if (time <= 0) {
            return;
        }

        if (Math.abs(TimeRenderer.view.getTarget() - time) > 10) {
            // 沿用 Wathe 时间渲染器的缓动变量，避免倒计时跳变时数字动画突兀。
            TimeRenderer.offsetDelta = time > TimeRenderer.view.getTarget() ? .6F : -.6F;
        }
        TimeRenderer.offsetDelta = Mth.lerp(delta / 16.0F, TimeRenderer.offsetDelta, 0.0F);
        TimeRenderer.view.setTarget(time);

        context.pose().pushPose();
        context.pose().translate(context.guiWidth() / 2.0F, 6.0F, 0.0F);
        // 使用双重人格颜色渲染，让玩家明确这是双活倒计时，不是普通回合时间。
        TimeRenderer.view.render(renderer, context, 0, 0, DualPersonalityManager.COLOR | 0xFF000000, delta);
        context.pose().popPose();
        ci.cancel();
    }
}
