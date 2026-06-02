package pro.fazeclan.river.stupid_express.role.convener.cca;

import dev.doctor4t.wathe.game.GameFunctions;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.role.convener.ConvenerSummonHandler;

/**
 * 召集者在成功召集后获得的“短时爆发移速”组件。
 *
 * <p>这里不直接硬依赖 StarryExpress 的组件，
 * 而是复用它“持续一段时间的状态组件”思路，
 * 单独维护召集者自己的加速持续时间。</p>
 */
public class ConvenerMomentumComponent implements AutoSyncedComponent, ServerTickingComponent {

    public static final ComponentKey<ConvenerMomentumComponent> KEY = ComponentRegistry.getOrCreate(
            StupidExpress.id("convener_momentum"),
            ConvenerMomentumComponent.class
    );

    private final Player player;

    /**
     * 当前还剩多少 tick 的召集爆发加速。
     */
    @Getter
    private int ticks = 0;

    public ConvenerMomentumComponent(Player player) {
        this.player = player;
    }

    /**
     * 开启或刷新召集后的加速状态。
     */
    public void activate() {
        this.ticks = ConvenerSummonHandler.SUMMON_SPEED_DURATION_TICKS;
        sync();
    }

    /**
     * 完整重置组件状态并移除移速加成。
     */
    public void reset() {
        this.ticks = 0;
        sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void serverTick() {
        if (this.ticks <= 0) {
            return;
        }

        // 只有活着的召集者才能持续保有这段冲刺加速。
        if (!GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            reset();
            return;
        }

        this.ticks--;
        sync();
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.ticks = tag.contains("ticks") ? tag.getInt("ticks") : 0;
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        tag.putInt("ticks", this.ticks);
    }
}
