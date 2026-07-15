package pro.fazeclan.river.stupid_express.client.role.convener;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudContext;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import net.minecraft.resources.ResourceLocation;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SERoles;

public final class ConvenerMoodHud {
    private static final ResourceLocation CONVENER_MOOD = StupidExpress.id("hud/mood_convener");

    private ConvenerMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerRoleStyle(SERoles.CONVENER, MoodHudStyle
                .builder(CONVENER_MOOD)
                .bar(ConvenerMoodHud::renderGradientFlowBar)
                .build());
    }

    private static void renderGradientFlowBar(MoodHudContext context, int width, float alpha) {
        if (width <= 0 || alpha <= 0.0F) {
            return;
        }

        /*
         * 召集者保留原 mixin 的“流动渐变心情条”效果。
         * Wathe API 已经把坐标移到心情条原点，这里只需要逐列填色。
         */
        for (int x = 0; x < width; ++x) {
            context.drawContext().fill(x, 0, x + 1, 1, ConvenerColorHelper.getBarFlowColor(x, width, alpha));
        }
    }
}
