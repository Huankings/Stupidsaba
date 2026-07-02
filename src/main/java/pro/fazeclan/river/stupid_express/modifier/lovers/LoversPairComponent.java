package pro.fazeclan.river.stupid_express.modifier.lovers;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LoversPairComponent implements AutoSyncedComponent {

    public static final ComponentKey<LoversPairComponent> KEY =
            ComponentRegistry.getOrCreate(StupidExpress.id("lovers_pairs"), LoversPairComponent.class);

    private static final String PAIRS_KEY = "pairs";
    private static final String FIRST_KEY = "first";
    private static final String SECOND_KEY = "second";
    private static final String FORCED_KEY = "forced";

    private final Level level;

    /*
     * 重要说明：
     * HarpyModLoader 的 WorldModifierComponent 只能回答“某个玩家有没有 LOVERS 词条”，
     * 不能回答“这个玩家具体和谁是一对”。
     *
     * 旧版恋人只有一对时，可以把所有带 LOVERS 的玩家直接视作同一组；
     * 现在指令允许同时指定多对恋人，如果仍然只看 LOVERS 集合，
     * HUD、本能发光、殉情、胜利判定都会把 a+b 与 c+d 混成四人恋人组。
     *
     * 因此这里单独保存一张双向伴侣表：
     * a -> b，同时 b -> a。查询时只需要拿自己的 UUID 就能找到真正的伴侣。
     */
    private final Map<UUID, UUID> partners = new HashMap<>();

    /*
     * forcedPlayers 用来标记“这一对是不是由 /stupid_express setlovers 指令指定的”。
     * remove lovers 指令只处理指定恋人，避免误删正常随机生成的恋人。
     */
    private final Set<UUID> forcedPlayers = new HashSet<>();

    public LoversPairComponent(Level level) {
        this.level = level;
    }

    public void sync() {
        KEY.sync(this.level);
    }

    public void clear() {
        this.partners.clear();
        this.forcedPlayers.clear();
        sync();
    }

    public void setRandomPair(UUID first, UUID second) {
        setPair(first, second, false, true);
    }

    public void setForcedPairs(Collection<Pair> pairs) {
        /*
         * 批量写入开局强制恋人时只在最后同步一次，避免多对恋人时重复同步世界组件。
         */
        for (Pair pair : pairs) {
            setPair(pair.first(), pair.second(), true, false);
        }
        sync();
    }

    private void setPair(UUID first, UUID second, boolean forced, boolean sync) {
        if (first == null || second == null || first.equals(second)) {
            return;
        }

        /*
         * 一个玩家同一时间只能有一个伴侣。
         * 如果先指定 a+b，之后又指定 a+c，这里会先拆掉 a+b，再建立 a+c。
         */
        removePair(first, false);
        removePair(second, false);

        this.partners.put(first, second);
        this.partners.put(second, first);
        if (forced) {
            this.forcedPlayers.add(first);
            this.forcedPlayers.add(second);
        }
        if (sync) {
            sync();
        }
    }

    public @Nullable UUID getPartner(UUID player) {
        return player == null ? null : this.partners.get(player);
    }

    public boolean arePartners(UUID first, UUID second) {
        return first != null && second != null && second.equals(this.partners.get(first));
    }

    public boolean isForcedPair(UUID player) {
        return player != null && this.forcedPlayers.contains(player) && this.partners.containsKey(player);
    }

    public @Nullable UUID removePair(UUID player) {
        return removePair(player, true);
    }

    private @Nullable UUID removePair(UUID player, boolean sync) {
        if (player == null) {
            return null;
        }

        UUID partner = this.partners.remove(player);
        this.forcedPlayers.remove(player);
        if (partner != null) {
            this.partners.remove(partner);
            this.forcedPlayers.remove(partner);
            if (sync) {
                sync();
            }
        }
        return partner;
    }

    public @Nullable UUID getPartnerOrFallback(UUID player, Collection<UUID> loversWithModifier) {
        UUID explicitPartner = getPartner(player);
        if (explicitPartner != null) {
            return explicitPartner;
        }

        /*
         * 兜底逻辑：
         * 如果组件数据因为旧存档、旧插件或调试命令缺失，但当前局只有两个 LOVERS，
         * 那么仍然可以沿用旧版“两个恋人天然互为伴侣”的语义。
         * 当 LOVERS 超过两个时，无法安全推断配对关系，必须返回 null。
         */
        if (player == null || loversWithModifier == null || loversWithModifier.size() != 2 || !loversWithModifier.contains(player)) {
            return null;
        }
        for (UUID uuid : loversWithModifier) {
            if (!player.equals(uuid)) {
                return uuid;
            }
        }
        return null;
    }

    public boolean arePartnersOrFallback(UUID first, UUID second, Collection<UUID> loversWithModifier) {
        if (arePartners(first, second)) {
            return true;
        }
        UUID fallbackPartner = getPartnerOrFallback(first, loversWithModifier);
        return second != null && second.equals(fallbackPartner);
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.partners.clear();
        this.forcedPlayers.clear();

        for (Tag element : tag.getList(PAIRS_KEY, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag pairTag) || !pairTag.contains(FIRST_KEY) || !pairTag.contains(SECOND_KEY)) {
                continue;
            }

            UUID first = NbtUtils.loadUUID(pairTag.get(FIRST_KEY));
            UUID second = NbtUtils.loadUUID(pairTag.get(SECOND_KEY));
            boolean forced = pairTag.getBoolean(FORCED_KEY);
            setPair(first, second, forced, false);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        ListTag pairsTag = new ListTag();
        Set<UUID> visited = new HashSet<>();

        for (Map.Entry<UUID, UUID> entry : this.partners.entrySet()) {
            UUID first = entry.getKey();
            UUID second = entry.getValue();
            if (first == null || second == null || visited.contains(first) || visited.contains(second)) {
                continue;
            }

            CompoundTag pairTag = new CompoundTag();
            pairTag.put(FIRST_KEY, NbtUtils.createUUID(first));
            pairTag.put(SECOND_KEY, NbtUtils.createUUID(second));
            pairTag.putBoolean(FORCED_KEY, this.forcedPlayers.contains(first) && this.forcedPlayers.contains(second));
            pairsTag.add(pairTag);

            visited.add(first);
            visited.add(second);
        }

        tag.put(PAIRS_KEY, pairsTag);
    }

    public record Pair(UUID first, UUID second) {
    }
}
