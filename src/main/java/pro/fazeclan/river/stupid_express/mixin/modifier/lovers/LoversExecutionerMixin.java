package pro.fazeclan.river.stupid_express.mixin.modifier.lovers;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.executioner.ExecutionerPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.modifier.lovers.LoversPairComponent;

import java.util.List;
import java.util.UUID;

@Mixin(ExecutionerPlayerComponent.class)
public class LoversExecutionerMixin {

    @Shadow
    @Final
    private Player player;

    @Inject(method = "serverTick", at = @At(value = "INVOKE", target = "Ljava/util/Collections;shuffle(Ljava/util/List;)V"))
    private void excludeLoversIfLover(CallbackInfo ci, @Local(name = "innocentPlayers") List<UUID> innocentPlayers) {

        var modifierComponent = WorldModifierComponent.KEY.get(player.level());
        var pairComponent = LoversPairComponent.KEY.get(player.level());
        innocentPlayers.removeIf(target ->
                modifierComponent.isModifier(player, SEModifiers.LOVERS)
                && modifierComponent.isModifier(target, SEModifiers.LOVERS)
                && pairComponent.arePartnersOrFallback(
                        player.getUUID(),
                        target,
                        modifierComponent.getAllWithModifier(SEModifiers.LOVERS)
                )
        );

    }

}
