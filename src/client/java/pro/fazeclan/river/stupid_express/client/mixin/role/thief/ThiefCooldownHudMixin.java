package pro.fazeclan.river.stupid_express.client.mixin.role.thief;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import pro.fazeclan.river.stupid_express.cca.AbilityCooldownComponent;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;

@Mixin(Gui.class)
public abstract class ThiefCooldownHudMixin {
    
    @Shadow
    public abstract Font getFont();
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        
        if (client.level == null || client.player == null) return;
        
        DebugScreenOverlay debugOverlay = client.getDebugOverlay();
        if (debugOverlay != null && debugOverlay.showDebugScreen()) return;
        
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(client.player.level());

        if (gameWorldComponent.isRole(client.player, SERoles.THIEF)) {
            if (!WatheClient.isPlayerAliveAndInSurvival() && !client.player.isCreative()) {
                return;
            }

            AbilityCooldownComponent abilityCooldownComponent = AbilityCooldownComponent.KEY.get(client.player);
            
            // Actually get the keybind name lol
            String keybindName = Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage().getString();
            
            MutableComponent displayText;
            
            if (abilityCooldownComponent.hasCooldown()) {
                long seconds = abilityCooldownComponent.getCooldown() / 20;
                displayText = Component.translatable("hud.stupid_express.thief.cooldown", seconds, keybindName);
            } else {
                displayText = Component.translatable("hud.stupid_express.thief.ready", keybindName);
            }
            
            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();
            
            int x = screenWidth - getFont().width(displayText) - 10;
            int y = screenHeight - 10;
            
            int color = SERoles.THIEF.color();
            
            guiGraphics.drawString(
                getFont(),
                displayText,
                x,
                y,
                color,
                true
            );
        }
    }
}