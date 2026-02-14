package pro.fazeclan.river.stupid_express.client.mixin.role.thief;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.fazeclan.river.stupid_express.cca.AbilityCooldownComponent;
import pro.fazeclan.river.stupid_express.role.thief.packet.ThiefTakeItemC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Mixin(Player.class)
public class ThiefTakeItemMixin {
    
    @Inject(
        method = "interactOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void onInteract(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        
        Level level = player.level();
        if (level.isClientSide()) {
            if (!(entity instanceof Player)) return;

            AbilityCooldownComponent abilityCooldownComponent = AbilityCooldownComponent.KEY.get(player);
            
            Player target = (Player) entity;

            if (!ThiefTakeItemC2SPacket.validateStealAttempt(player, target, abilityCooldownComponent)) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }

            if (!player.getMainHandItem().isEmpty()) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
            
            ClientPlayNetworking.send(new ThiefTakeItemC2SPacket(target.getUUID()));
            
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}