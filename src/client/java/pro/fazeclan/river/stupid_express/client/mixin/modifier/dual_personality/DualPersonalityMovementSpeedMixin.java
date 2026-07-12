package pro.fazeclan.river.stupid_express.client.mixin.modifier.dual_personality;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;

@Mixin(value = Player.class, priority = 1700)
public abstract class DualPersonalityMovementSpeedMixin extends LivingEntity {

    protected DualPersonalityMovementSpeedMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyReturnValue(method = "getSpeed", at = @At("RETURN"))
    private float stupidexpress$boostDoubleActiveSpeed(float original) {
        Player player = (Player) (Object) this;
        if (!DualPersonalityClientState.isDoubleActive(player)) {
            return original;
        }
        // 双活阶段给两个玩家 1.5 倍移动速度；普通轮换阶段不改变任何速度。
        return original * 1.5F;
    }
}
