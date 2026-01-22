package pro.fazeclan.river.stupid_express.client.mixin.modifier.lovers;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;

@Mixin(value = WatheClient.class, priority = 500)
public class LoversInstinctMixin {

    @Inject(method = "getInstinctHighlight", at = @At("HEAD"), cancellable = true)
    private static void loversHighlight(Entity target, CallbackInfoReturnable<Integer> cir) {
        var player = Minecraft.getInstance().player;
        var component = WorldModifierComponent.KEY.get(player.level());
        if (!StupidExpress.CONFIG.modifiersSection.loversSection.loversGlowToEachother) {
            return;
        }
        if (!(target instanceof Player potentialLover)) {
            return;
        }
        if (!component.isModifier(player, SEModifiers.LOVERS)) {
            return;
        }
        if (!component.isModifier(potentialLover, SEModifiers.LOVERS)) {
            return;
        }
        if (WatheClient.isPlayerSpectatingOrCreative()) {
            return;
        }
        cir.setReturnValue(SEModifiers.LOVERS.color());
    }

}
