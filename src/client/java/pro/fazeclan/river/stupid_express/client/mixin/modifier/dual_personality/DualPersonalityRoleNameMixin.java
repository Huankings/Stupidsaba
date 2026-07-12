package pro.fazeclan.river.stupid_express.client.mixin.modifier.dual_personality;

import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;

@Mixin(RoleNameRenderer.class)
public class DualPersonalityRoleNameMixin {

    @Shadow
    private static float nametagAlpha;

    @Shadow
    private static float noteAlpha;

    @Inject(method = "renderHud", at = @At("HEAD"), cancellable = true)
    private static void stupidexpress$hideRoleNameForDormantPersonality(
            Font renderer,
            LocalPlayer player,
            GuiGraphics context,
            DeltaTracker tickCounter,
            CallbackInfo ci
    ) {
        /*
         * 休眠人格的相机被服务端强制挂到活跃人格身上。
         * Wathe 的 RoleNameRenderer 仍会用“休眠玩家实体”做准星检测，
         * 容易命中被附身观看的活跃人格，并在屏幕中央显示活跃人格名字/阵营提示。
         * 休眠人格不应该通过这个 HUD 获得额外信息，因此本帧直接隐藏并清掉淡出残留。
         */
        if (DualPersonalityClientState.isDormant(player)) {
            nametagAlpha = 0.0F;
            noteAlpha = 0.0F;
            ci.cancel();
        }
    }
}
