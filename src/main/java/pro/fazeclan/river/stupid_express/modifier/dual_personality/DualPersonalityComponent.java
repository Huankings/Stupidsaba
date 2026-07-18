package pro.fazeclan.river.stupid_express.modifier.dual_personality;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 双重人格的“世界级”状态组件。
 *
 * <p>这里不挂在单个玩家身上，而是挂在世界上，是因为一组双重人格天然需要同时知道两名玩家：
 * 主人格、副人格、当前谁活跃、谁休眠、是否进入双活、倒计时还剩多少等信息都必须成对保存。</p>
 *
 * <p>词条本身只负责让 Wathe/Harpy 知道“这个玩家拥有双重人格”；真正的配对和运行状态都在这个组件里。</p>
 */
public class DualPersonalityComponent implements AutoSyncedComponent {

    // CCA 的组件键。注册在世界组件上后，服务端和客户端都可以通过 KEY.get(level) 读取当前对局状态。
    public static final ComponentKey<DualPersonalityComponent> KEY =
            ComponentRegistry.getOrCreate(StupidExpress.id("dual_personality"), DualPersonalityComponent.class);

    private static final String PAIRS_KEY = "pairs";
    private static final String MAIN_KEY = "main";
    private static final String SUB_KEY = "sub";
    private static final String ACTIVE_KEY = "active";
    private static final String DORMANT_KEY = "dormant";
    private static final String FORCED_KEY = "forced";
    private static final String DOUBLE_ACTIVE_KEY = "double_active";
    private static final String SWITCH_TICKS_KEY = "switch_ticks";
    private static final String DOUBLE_ACTIVE_TICKS_KEY = "double_active_ticks";
    private static final String PAUSED_KEY = "paused";
    private static final String PAUSE_REASON_KEY = "pause_reason";
    private static final String INITIAL_MESSAGE_SENT_KEY = "initial_message_sent";
    private static final String INITIAL_MESSAGE_DELAY_TICKS_KEY = "initial_message_delay_ticks";

    private final Level level;

    /*
     * 当前世界里所有双重人格配对。
     * 用户确认允许一局出现多对双重人格，所以这里用 List 保存多组 PairState，
     * 而不是像单例词条那样只保存一个 main/sub。
     */
    private final List<PairState> pairs = new ArrayList<>();

    public DualPersonalityComponent(Level level) {
        this.level = level;
    }

    public void sync() {
        KEY.sync(this.level);
    }

    public void clear() {
        // 对局结束或强制配对重写时清空旧状态，防止上一局的人格关系残留到下一局。
        this.pairs.clear();
        sync();
    }

    public List<PairState> getPairs() {
        return this.pairs;
    }

    public void setRandomPair(UUID main, UUID sub) {
        // 随机分配时，触发 ModifierAssigned 的玩家作为主人格，补出来的候选人作为副人格。
        setPair(main, sub, false, true);
    }

    public void setForcedPairs(Collection<Pair> pairs) {
        /*
         * 强制指定可能一次带来多对。
         * 先逐对写入但暂不同步，最后统一 sync，避免客户端收到一串中间状态。
         */
        for (Pair pair : pairs) {
            setPair(pair.main(), pair.sub(), true, false);
        }
        sync();
    }

    public void setPair(UUID main, UUID sub, boolean forced, boolean sync) {
        if (main == null || sub == null || main.equals(sub)) {
            return;
        }

        /*
         * 双重人格的一组关系是“有方向”的：main 是主人格，sub 是副人格。
         * 因此如果管理员先指定 A->B，之后又指定 B->A，
         * 这里会先拆掉旧组，再按新的方向创建状态，主副身份随指令同步反转。
         */
        removePair(main, false);
        removePair(sub, false);

        PairState state = new PairState(main, sub, forced);
        this.pairs.add(state);
        if (sync) {
            sync();
        }
    }

    public @Nullable PairState getPair(UUID player) {
        if (player == null) {
            return null;
        }
        for (PairState pair : this.pairs) {
            if (pair.contains(player)) {
                return pair;
            }
        }
        return null;
    }

    public @Nullable UUID getPartner(UUID player) {
        PairState pair = getPair(player);
        return pair == null ? null : pair.getPartner(player);
    }

    public @Nullable UUID removePair(UUID player) {
        // 对外暴露的移除接口：只要传入任意一方，就会拆掉整组人格关系。
        return removePair(player, true);
    }

    private @Nullable UUID removePair(UUID player, boolean sync) {
        if (player == null) {
            return null;
        }

        for (int i = 0; i < this.pairs.size(); i++) {
            PairState pair = this.pairs.get(i);
            if (!pair.contains(player)) {
                continue;
            }

            UUID partner = pair.getPartner(player);
            this.pairs.remove(i);
            if (sync) {
                sync();
            }
            return partner;
        }
        return null;
    }

    public boolean isDormant(UUID player) {
        // 双活状态下没有“休眠人格”，因此最终判断会落到 PairState#isDormant。
        PairState pair = getPair(player);
        return pair != null && pair.isDormant(player);
    }

    public boolean isActive(UUID player) {
        PairState pair = getPair(player);
        return pair != null && pair.isActive(player);
    }

    public boolean isDoubleActive(UUID player) {
        PairState pair = getPair(player);
        return pair != null && pair.doubleActive && pair.contains(player);
    }

    public int getDoubleActiveTicks(UUID player) {
        PairState pair = getPair(player);
        return pair != null && pair.doubleActive && pair.contains(player) ? pair.doubleActiveTicks : 0;
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.pairs.clear();

        /*
         * 世界组件会被存档保存。即使服务器重启，已经开始的人格轮换/双活倒计时也能恢复。
         * 这里读取时跳过缺少 main/sub 的脏数据，避免坏档直接让整局加载失败。
         */
        for (Tag element : tag.getList(PAIRS_KEY, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag pairTag) || !pairTag.contains(MAIN_KEY) || !pairTag.contains(SUB_KEY)) {
                continue;
            }

            UUID main = NbtUtils.loadUUID(pairTag.get(MAIN_KEY));
            UUID sub = NbtUtils.loadUUID(pairTag.get(SUB_KEY));
            PairState state = new PairState(main, sub, pairTag.getBoolean(FORCED_KEY));
            if (pairTag.contains(ACTIVE_KEY)) {
                state.active = NbtUtils.loadUUID(pairTag.get(ACTIVE_KEY));
            }
            if (pairTag.contains(DORMANT_KEY)) {
                state.dormant = NbtUtils.loadUUID(pairTag.get(DORMANT_KEY));
            }
            state.doubleActive = pairTag.getBoolean(DOUBLE_ACTIVE_KEY);
            state.switchTicks = pairTag.getInt(SWITCH_TICKS_KEY);
            state.doubleActiveTicks = pairTag.getInt(DOUBLE_ACTIVE_TICKS_KEY);
            state.paused = pairTag.getBoolean(PAUSED_KEY);
            state.pauseReason = PauseReason.fromSerialized(pairTag.getString(PAUSE_REASON_KEY));
            state.initialMessageSent = pairTag.getBoolean(INITIAL_MESSAGE_SENT_KEY);
            if (pairTag.contains(INITIAL_MESSAGE_DELAY_TICKS_KEY)) {
                state.initialMessageDelayTicks = pairTag.getInt(INITIAL_MESSAGE_DELAY_TICKS_KEY);
            }
            this.pairs.add(state);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        ListTag pairList = new ListTag();
        for (PairState pair : this.pairs) {
            // 这里保存的是完整运行态，不只是配对关系；掉线暂停、倒计时、双活剩余时间都必须一起落盘。
            CompoundTag pairTag = new CompoundTag();
            pairTag.put(MAIN_KEY, NbtUtils.createUUID(pair.main));
            pairTag.put(SUB_KEY, NbtUtils.createUUID(pair.sub));
            pairTag.put(ACTIVE_KEY, NbtUtils.createUUID(pair.active));
            pairTag.put(DORMANT_KEY, NbtUtils.createUUID(pair.dormant));
            pairTag.putBoolean(FORCED_KEY, pair.forced);
            pairTag.putBoolean(DOUBLE_ACTIVE_KEY, pair.doubleActive);
            pairTag.putInt(SWITCH_TICKS_KEY, pair.switchTicks);
            pairTag.putInt(DOUBLE_ACTIVE_TICKS_KEY, pair.doubleActiveTicks);
            pairTag.putBoolean(PAUSED_KEY, pair.paused);
            pairTag.putString(PAUSE_REASON_KEY, pair.pauseReason.serialized);
            pairTag.putBoolean(INITIAL_MESSAGE_SENT_KEY, pair.initialMessageSent);
            pairTag.putInt(INITIAL_MESSAGE_DELAY_TICKS_KEY, pair.initialMessageDelayTicks);
            pairList.add(pairTag);
        }
        tag.put(PAIRS_KEY, pairList);
    }

    /**
     * 单对双重人格在本局中的全部运行状态。
     *
     * <p>main/sub 是身份方向，整局不变；active/dormant 是当前控制权，60 秒轮换时会互换。
     * 这样可以同时表达“你是主人格/副人格”和“你当前是否能行动”两层概念。</p>
     */
    public static class PairState {
        // 主人格：开局默认活跃，也是副人格没有其它伪装时要显示成的外观来源。
        public final UUID main;
        // 副人格：开局默认休眠，但轮换后同样可以成为活跃人格。
        public final UUID sub;
        // 当前能正常行动、拿道具、使用能力的那个人格。
        public UUID active;
        // 当前处于特殊存活旁观、相机跟随 active、能力被封锁的那个人格。
        public UUID dormant;
        // 是否来自管理员指令指定。主要用于调试/后续扩展，不影响运行逻辑。
        public boolean forced;
        // 受到致命伤害后进入“双活”阶段，两个人都变成可行动状态。
        public boolean doubleActive;
        // 普通轮换阶段剩余 tick，默认 60 秒。
        public int switchTicks = DualPersonalityManager.SWITCH_INTERVAL_TICKS;
        // 双活阶段剩余 tick，归零后两个人格一起按自定义死亡原因结算。
        public int doubleActiveTicks;
        // 掉线时暂停普通轮换，避免一方离线导致另一方被无意义切换。
        public boolean paused;
        // 暂停原因要区分“休眠掉线”和“活跃掉线”，因为恢复时处理方式不同。
        public PauseReason pauseReason = PauseReason.NONE;
        // 开局身份提示只发送一次，避免每 tick 都刷 actionbar。
        public boolean initialMessageSent;
        // 开局身份提示延后发送的剩余 tick，避免和词条/阵营开局 actionbar 挤在同一瞬间。
        public int initialMessageDelayTicks = DualPersonalityManager.INITIAL_ROLE_MESSAGE_DELAY_TICKS;

        public PairState(UUID main, UUID sub, boolean forced) {
            this.main = main;
            this.sub = sub;
            this.active = main;
            this.dormant = sub;
            this.forced = forced;
        }

        public boolean contains(UUID player) {
            return player != null && (player.equals(this.main) || player.equals(this.sub));
        }

        public boolean isMain(UUID player) {
            return player != null && player.equals(this.main);
        }

        public boolean isSub(UUID player) {
            return player != null && player.equals(this.sub);
        }

        public boolean isActive(UUID player) {
            return player != null && player.equals(this.active);
        }

        public boolean isDormant(UUID player) {
            // 双活时两个人格都能行动，所以不再承认任何一方是休眠态。
            return player != null && !this.doubleActive && player.equals(this.dormant);
        }

        public @Nullable UUID getPartner(UUID player) {
            if (player == null) {
                return null;
            }
            if (player.equals(this.main)) {
                return this.sub;
            }
            if (player.equals(this.sub)) {
                return this.main;
            }
            return null;
        }

        public boolean isOnlineReadyForRotation() {
            // 通信 helper 用这个状态判断是否仍处于普通轮换阶段，而不是双活或掉线暂停。
            return !this.doubleActive && !this.paused;
        }
    }

    /**
     * 普通轮换阶段的暂停原因。
     *
     * <p>ACTIVE_OFFLINE 比 DORMANT_OFFLINE 更特殊：活跃人格掉线时，会临时把原休眠人格提为活跃人格，
     * 同时暂停倒计时，等掉线者回来后再作为休眠人格恢复轮换。</p>
     */
    public enum PauseReason {
        NONE("none"),
        DORMANT_OFFLINE("dormant_offline"),
        ACTIVE_OFFLINE("active_offline");

        private final String serialized;

        PauseReason(String serialized) {
            this.serialized = serialized;
        }

        static PauseReason fromSerialized(String serialized) {
            for (PauseReason reason : values()) {
                if (reason.serialized.equals(serialized)) {
                    return reason;
                }
            }
            return NONE;
        }
    }

    public record Pair(UUID main, UUID sub) {
        // 用于强制分配队列的轻量配对记录，避免把完整运行态暴露给指令层。
    }
}
