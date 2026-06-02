package pro.fazeclan.river.stupid_express.mixin.role.convener;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取 Minecraft 原版单条物品冷却实例里的结束时间。
 *
 * <p>物品冷却的剩余 tick 并没有公开 API，
 * 这里只能通过 accessor 把 endTime 取出来，
 * 再结合 ItemCooldowns 本体的 tickCount 计算“当前剩余冷却”。</p>
 */
@Mixin(targets = "net.minecraft.world.item.ItemCooldowns$CooldownInstance")
public interface ItemCooldownInstanceAccessor {

    /**
     * 当前这条冷却会在第几个 tick 结束。
     */
    @Accessor("endTime")
    int stupid_express$getEndTime();
}
