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
import pro.fazeclan.river.stupid_express.modifier.lovers.LoversPairComponent;

@Mixin(value = WatheClient.class, priority = 500)
public class LoversInstinctMixin {

    @Inject(method = "getInstinctHighlight", at = @At("HEAD"), cancellable = true)
    private static void loversHighlight(Entity target, CallbackInfoReturnable<Integer> cir) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var component = WorldModifierComponent.KEY.get(player.level());
        var pairComponent = LoversPairComponent.KEY.get(player.level());
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
        /*
         * 多对恋人时只让“自己的伴侣”发光。
         * 其他恋人对仍然是恋人词条，但不是当前玩家的伴侣，不能暴露给本能。
         */
        if (!pairComponent.arePartnersOrFallback(
                player.getUUID(),
                potentialLover.getUUID(),
                component.getAllWithModifier(SEModifiers.LOVERS)
        )) {
            return;
        }
        if (WatheClient.isPlayerSpectatingOrCreative()) {
            return;
        }
        cir.setReturnValue(SEModifiers.LOVERS.color());
    }

}
