package pro.fazeclan.river.stupid_express.client.mixin.role.convener;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerColorHelper;
import pro.fazeclan.river.stupid_express.constants.SERoles;

@Mixin(MoodRenderer.class)
public class ConvenerMoodMixin {

    @Shadow public static float moodOffset;
    @Shadow public static float moodTextWidth;
    @Shadow public static float moodRender;
    @Shadow public static float moodAlpha;

    @Unique
    private static final ResourceLocation CONVENER_MOOD = StupidExpress.id("hud/mood_convener");

    @Inject(method = "renderKiller", at = @At("HEAD"), cancellable = true)
    private static void stupidexpress$renderConvenerMood(Font renderer, GuiGraphics context, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(client.player.level());
        if (!gameWorldComponent.isRole(client.player, SERoles.CONVENER)) {
            return;
        }

        context.pose().pushPose();
        context.pose().translate(0.0F, 3.0F * moodOffset, 0.0F);
        context.blitSprite(CONVENER_MOOD, 5, 6, 14, 17);
        context.pose().popPose();

        context.pose().pushPose();
        context.pose().translate(0.0F, 10.0F * moodOffset, 0.0F);
        context.pose().translate(26.0F, 8.0F + renderer.lineHeight, 0.0F);

        // 召集者的假心情条不走“整条单色变化”：
        // 而是把多段颜色拆成一条流动渐变带，从左到右缓慢移动。
        renderGradientFlowBar(context, Math.max(1, Math.round((moodTextWidth - 8.0F) * moodRender)), moodAlpha);
        context.pose().popPose();

        ci.cancel();
    }

    @Unique
    private static void renderGradientFlowBar(GuiGraphics context, int width, float alpha) {
        if (width <= 0 || alpha <= 0f) {
            return;
        }

        for (int x = 0; x < width; ++x) {
            context.fill(x, 0, x + 1, 1, ConvenerColorHelper.getBarFlowColor(x, width, alpha));
        }
    }
}
