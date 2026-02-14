package pro.fazeclan.river.stupid_express.client.mixin.role.thief;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.cca.AbilityCooldownComponent;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.thief.packet.ThiefTakeItemC2SPacket;

@Mixin(CrosshairRenderer.class)
public class ThiefCrosshairRendererMixin {
    
    @Shadow @Final private static ResourceLocation CROSSHAIR;
    @Shadow @Final private static ResourceLocation CROSSHAIR_TARGET;
    
    private static ResourceLocation THIEF_READY = 
        StupidExpress.id("hud/thief_ready");
    private static ResourceLocation THIEF_PROGRESS_FILL = 
        StupidExpress.id("hud/thief_progress_fill");
    private static ResourceLocation THIEF_PROGRESS_BACKGROUND = 
        StupidExpress.id("hud/thief_progress_background");
    
    private static boolean shouldShowThiefCrosshair(Minecraft client, LocalPlayer player) {
        
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
        if (!gameWorldComponent.isRole(player, SERoles.THIEF)) return false;
        if (!player.getMainHandItem().isEmpty()) return false;
        
        HitResult hitResult = client.hitResult;
        if (hitResult instanceof EntityHitResult) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            if (entityHit.getEntity() instanceof Player target) {
                return ThiefTakeItemC2SPacket.validateDistance(player, target);
            }
        }
        
        return false;
    }
    
    @Inject(
        method = "renderCrosshair",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onRenderCrosshair(Minecraft client, LocalPlayer player, GuiGraphics guiGraphics, DeltaTracker tickCounter, CallbackInfo ci) {
        if (!shouldShowThiefCrosshair(client, player)) {
            return;
        }
        
        ci.cancel();
        
        if (!client.options.getCameraType().isFirstPerson()) {
            return;
        }
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float)guiGraphics.guiWidth() / 2.0F, (float)guiGraphics.guiHeight() / 2.0F, 0.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        
        AbilityCooldownComponent abilityCooldownComponent = AbilityCooldownComponent.KEY.get(player);
        
        if (!abilityCooldownComponent.hasCooldown()) {
            guiGraphics.blitSprite(THIEF_READY, -5, 5, 10, 7);
        } else {
            float progress = 1.0f - ((float)abilityCooldownComponent.getCooldown() / (float)ThiefTakeItemC2SPacket.THIEF_COOLDOWN);
            
            guiGraphics.blitSprite(THIEF_PROGRESS_BACKGROUND, -5, 5, 10, 7);
            
            guiGraphics.blitSprite(THIEF_PROGRESS_FILL, 10, 7, 0, 0, -5, 5, (int)(progress * 10.0F), 7);
        }
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(-1.5F, -1.5F, 0.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, 
                                    GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, 
                                    GlStateManager.SourceFactor.ONE, 
                                    GlStateManager.DestFactor.ZERO);
        
        guiGraphics.blitSprite(CROSSHAIR_TARGET, 0, 0, 3, 3);
        
        guiGraphics.pose().popPose();
        guiGraphics.pose().popPose();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}