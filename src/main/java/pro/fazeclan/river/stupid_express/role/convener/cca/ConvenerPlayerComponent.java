package pro.fazeclan.river.stupid_express.role.convener.cca;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 负责保存召集者自己的整套“回合内状态”：
 * 1. 已解锁了哪些头像；
 * 2. 已经成功召集了多少次；
 * 3. 本局需要多少次召集才能直接获胜；
 * 4. 当前距离下一层反伤护盾还累计完成了多少个任务；
 * 5. 当前已经拥有多少层反伤护盾。
 */
public class ConvenerPlayerComponent implements AutoSyncedComponent {

    public static final ComponentKey<ConvenerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            StupidExpress.id("convener_player"),
            ConvenerPlayerComponent.class
    );

    private final Player player;
    private final Set<UUID> unlockedDisguises = new LinkedHashSet<>();

    /**
     * 召集者每完成 4 个任务，就会获得 1 层反伤护盾。
     *
     * <p>这里单独抽成常量，方便你后续直接改平衡数值。</p>
     */
    public static final int TASKS_PER_COUNTER_SHIELD = 4;

    private int summonCount;
    private int requiredSummons = 1;
    private int completedTasksTowardsShield;
    private int counterShieldLayers;

    public ConvenerPlayerComponent(Player player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /**
     * 回合结束后彻底清空。
     * 这里不默认保留自己的头像，避免普通玩家也残留一份“伪召集者状态”。
     */
    public void reset() {
        this.unlockedDisguises.clear();
        this.summonCount = 0;
        this.requiredSummons = 1;
        this.completedTasksTowardsShield = 0;
        this.counterShieldLayers = 0;
        this.sync();
    }

    /**
     * 真正被分配到召集者身份时调用。
     * 开局只有自己的头像可用，后续必须靠尸体召集来逐步解锁其他人。
     */
    public void initializeForRole() {
        this.unlockedDisguises.clear();
        this.unlockedDisguises.add(this.player.getUUID());
        this.summonCount = 0;
        this.requiredSummons = 1;
        this.completedTasksTowardsShield = 0;
        this.counterShieldLayers = 0;
        this.sync();
    }

    public Set<UUID> getUnlockedDisguises() {
        return unlockedDisguises;
    }

    public boolean hasUnlockedMorphs() {
        return this.unlockedDisguises.size() > 1;
    }

    public boolean knowsDisguise(UUID uuid) {
        return this.unlockedDisguises.contains(uuid);
    }

    public void unlockDisguise(UUID uuid) {
        this.unlockedDisguises.add(uuid);
        this.sync();
    }

    public int getSummonCount() {
        return summonCount;
    }

    public void incrementSummonCount() {
        ++this.summonCount;
    }

    public int getRequiredSummons() {
        return requiredSummons;
    }

    public void setRequiredSummons(int requiredSummons) {
        this.requiredSummons = Math.max(1, requiredSummons);
    }

    public boolean hasReachedSummonGoal() {
        return this.summonCount >= this.requiredSummons;
    }

    public int getCompletedTasksTowardsShield() {
        return this.completedTasksTowardsShield;
    }

    public int getCounterShieldLayers() {
        return this.counterShieldLayers;
    }

    public boolean hasCounterShield() {
        return this.counterShieldLayers > 0;
    }

    /**
     * 记录一次真实任务完成。
     *
     * <p>如果刚好凑够一轮任务，就会立刻折算成 1 层反伤护盾，
     * 并把本轮累计进度扣回去。</p>
     *
     * @return 这次任务完成后是否新获得了护盾层数
     */
    public boolean recordCompletedTask() {
        this.completedTasksTowardsShield++;
        if (this.completedTasksTowardsShield < TASKS_PER_COUNTER_SHIELD) {
            return false;
        }

        this.completedTasksTowardsShield -= TASKS_PER_COUNTER_SHIELD;
        this.counterShieldLayers++;
        return true;
    }

    /**
     * 计算距离下一层护盾还差多少个任务。
     */
    public int getTasksRemainingForNextShield() {
        return Math.max(0, TASKS_PER_COUNTER_SHIELD - this.completedTasksTowardsShield);
    }

    /**
     * 消耗一层反伤护盾。
     *
     * @return 是否真的成功消耗了护盾
     */
    public boolean consumeCounterShield() {
        if (this.counterShieldLayers <= 0) {
            return false;
        }
        this.counterShieldLayers--;
        return true;
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.unlockedDisguises.clear();
        if (tag.contains("unlocked_disguises")) {
            for (Tag entry : tag.getList("unlocked_disguises", Tag.TAG_INT_ARRAY)) {
                this.unlockedDisguises.add(NbtUtils.loadUUID(entry));
            }
        }

        this.summonCount = tag.contains("summon_count") ? tag.getInt("summon_count") : 0;
        this.requiredSummons = tag.contains("required_summons") ? Math.max(1, tag.getInt("required_summons")) : 1;
        this.completedTasksTowardsShield = tag.contains("completed_tasks_towards_shield") ? Math.max(0, tag.getInt("completed_tasks_towards_shield")) : 0;
        this.counterShieldLayers = tag.contains("counter_shield_layers") ? Math.max(0, tag.getInt("counter_shield_layers")) : 0;
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        ListTag unlockedList = new ListTag();
        for (UUID uuid : this.unlockedDisguises) {
            unlockedList.add(NbtUtils.createUUID(uuid));
        }
        tag.put("unlocked_disguises", unlockedList);
        tag.putInt("summon_count", this.summonCount);
        tag.putInt("required_summons", this.requiredSummons);
        tag.putInt("completed_tasks_towards_shield", this.completedTasksTowardsShield);
        tag.putInt("counter_shield_layers", this.counterShieldLayers);
    }
}
