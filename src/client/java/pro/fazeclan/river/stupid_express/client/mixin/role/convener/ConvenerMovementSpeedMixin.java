package pro.fazeclan.river.stupid_express.client.mixin.role.convener;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.convener.ConvenerSummonHandler;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerMomentumComponent;

/**
 * 召集者的爆发移速修正。
 *
 * <p>这里直接参考 StarryExpress 的 starstruck 做法，
 * 在客户端覆盖 Player#getSpeed 的返回值。
 * 因为 Wathe 本体本身就会在这里把玩家速度固定改成 0.07 / 0.1，
 * 所以单纯在服务端堆属性 modifier 看起来不会生效；
 * 必须顺着 Wathe 这一条速度链再往上覆盖一次。</p>
 */
@Mixin(value = Player.class, priority = 1600)
public abstract class ConvenerMovementSpeedMixin extends LivingEntity {

    protected ConvenerMovementSpeedMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyReturnValue(method = "getSpeed", at = @At("RETURN"))
    private float stupidexpress$doubleConvenerSpeed(float original) {
        Player player = (Player) (Object) this;
        if (!GameWorldComponent.KEY.get(player.level()).isRole(player, SERoles.CONVENER)) {
            return original;
        }
        if (ConvenerMomentumComponent.KEY.get(player).getTicks() <= 0) {
            return original;
        }

        // Wathe 默认活人速度是：走路 0.07、冲刺 0.1。
        // 这里按“变成原来的 2 倍”直接翻倍即可。
        return original * (float) (1.0D + ConvenerSummonHandler.SUMMON_SPEED_MULTIPLIER_BONUS);
    }
}
