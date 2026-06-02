package pro.fazeclan.river.stupid_express.role.convener.cca;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;

import java.util.UUID;

/**
 * 记录任意玩家当前是否处于“召集者变形”状态。
 *
 * <p>这里故意不把逻辑只绑在召集者自己身上，
 * 因为召集成功后全场存活玩家都要一起顶着同一张脸。
 * 服务端只负责维护 UUID 和剩余时间，客户端渲染层再去把皮肤、披风、名字替换掉。</p>
 */
public class ConvenerDisguiseComponent implements AutoSyncedComponent, ServerTickingComponent {

    public static final ComponentKey<ConvenerDisguiseComponent> KEY = ComponentRegistry.getOrCreate(
            StupidExpress.id("convener_disguise"),
            ConvenerDisguiseComponent.class
    );

    private final Player player;

    /**
     * 当前伪装目标的 UUID。
     * 为 null 时说明没有在变形。
     */
    private @Nullable UUID disguiseUuid;

    /**
     * 剩余变形时间。
     * 0 代表未变形；
     * 大于 0 代表限时强制变形；
     * -1 代表召集者自己的无限时手动/召集变形。
     */
    private int morphTicks;

    /**
     * 限时变形不需要每 tick 同步一次，5 tick 发一次已经足够平滑。
     */
    private int syncDelay;

    public ConvenerDisguiseComponent(Player player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public boolean isDisguised() {
        return this.disguiseUuid != null && this.morphTicks != 0;
    }

    public @Nullable UUID getDisguiseUuid() {
        return disguiseUuid;
    }

    public int getMorphTicks() {
        return morphTicks;
    }

    /**
     * 给普通玩家套上限时变形。
     */
    public void setTimedDisguise(UUID disguiseUuid, int ticks) {
        this.disguiseUuid = disguiseUuid;
        this.morphTicks = Math.max(1, ticks);
        this.syncDelay = 0;
        this.sync();
    }

    /**
     * 给召集者自己套上无限时变形。
     */
    public void setPersistentDisguise(UUID disguiseUuid) {
        this.disguiseUuid = disguiseUuid;
        this.morphTicks = -1;
        this.syncDelay = 0;
        this.sync();
    }

    /**
     * 主动卸除或回合结束时清空当前伪装。
     */
    public void clearDisguise() {
        this.disguiseUuid = null;
        this.morphTicks = 0;
        this.syncDelay = 0;
        this.sync();
    }

    /**
     * 只在真正有变形的时候推进倒计时。
     * 如果玩家已经死亡、观战或者变形到点，就立刻清掉伪装，避免残局里残留错误皮肤。
     */
    @Override
    public void serverTick() {
        if (!this.isDisguised()) {
            return;
        }

        if (!this.player.isAlive() || this.player.isSpectator()) {
            this.clearDisguise();
            return;
        }

        if (this.morphTicks > 0) {
            --this.morphTicks;

            if (this.morphTicks == 0) {
                this.clearDisguise();
                return;
            }

            ++this.syncDelay;
            if (this.syncDelay >= 5) {
                this.syncDelay = 0;
                this.sync();
            }
        }
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.morphTicks = tag.contains("morph_ticks") ? tag.getInt("morph_ticks") : 0;
        this.disguiseUuid = tag.contains("disguise_uuid") ? tag.getUUID("disguise_uuid") : null;

        if (this.morphTicks == 0) {
            this.disguiseUuid = null;
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        tag.putInt("morph_ticks", this.morphTicks);
        if (this.disguiseUuid != null) {
            tag.putUUID("disguise_uuid", this.disguiseUuid);
        }
    }
}
