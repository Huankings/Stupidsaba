package pro.fazeclan.river.stupid_express.mixin.role.convener;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * 读取原版 ItemCooldowns 的内部状态。
 *
 * <p>召集者的封控需求需要判断“当前剩余冷却是否已经比目标更长”，
 * 但原版只暴露了是否冷却中和百分比，没有直接给剩余 tick，
 * 所以这里把内部 map 和 tickCount 暴露出来供服务端工具类计算。</p>
 */
@Mixin(ItemCooldowns.class)
public interface ItemCooldownsAccessor {

    /**
     * 当前玩家所有物品的冷却表。
     */
    @Accessor("cooldowns")
    Map<Item, Object> stupid_express$getCooldowns();

    /**
     * 原版冷却系统当前所处的全局 tick 计数。
     */
    @Accessor("tickCount")
    int stupid_express$getTickCount();
}
