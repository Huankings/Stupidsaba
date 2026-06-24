package pro.fazeclan.river.stupid_express.role.avaricious.cca;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;

/**
 * 保存扒手金币结算计时器的服务端权威起点。
 *
 * <p>扒手的实际发钱逻辑跑在服务端，而新的右下角 HUD 跑在客户端。
 * 如果客户端只靠本地世界时间取模，就可能和真实发钱点错开；因此这里把
 * “本局第一次开始计时的 GameTimeComponent.time”同步给客户端，HUD 和服务端
 * 都用同一个起点计算下一次结算时间。</p>
 */
public class AvariciousPayoutComponent implements AutoSyncedComponent {

    public static final ComponentKey<AvariciousPayoutComponent> KEY = ComponentRegistry.getOrCreate(
            StupidExpress.id("avaricious_payout"),
            AvariciousPayoutComponent.class
    );

    private final Level level;

    /**
     * 记录计时器开始时的 GameTimeComponent.time。
     *
     * <p>Wathe 的游戏时间是倒计时，也就是每 tick 递减；所以后续判断结算点时，
     * 要看“当前时间距离这个起点已经倒退了多少 tick”。-1 表示本局还没初始化。</p>
     */
    private int timerStartTime = -1;

    public AvariciousPayoutComponent(Level level) {
        this.level = level;
    }

    public void sync() {
        KEY.sync(this.level);
    }

    public int getTimerStartTime() {
        return this.timerStartTime;
    }

    public boolean hasTimerStartTime() {
        return this.timerStartTime >= 0;
    }

    public void setTimerStartTime(int timerStartTime) {
        this.timerStartTime = timerStartTime;
        this.sync();
    }

    /**
     * 每局重新分配扒手身份时清空旧起点，避免上一局的同步数据影响下一局 HUD。
     */
    public void reset() {
        this.timerStartTime = -1;
        this.sync();
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.timerStartTime = tag.contains("timer_start_time") ? tag.getInt("timer_start_time") : -1;
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        tag.putInt("timer_start_time", this.timerStartTime);
    }
}
