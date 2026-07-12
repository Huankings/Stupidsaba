package pro.fazeclan.river.stupid_express.modifier.dual_personality;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.harpymodloader.modifiers.Modifier;
import org.jetbrains.annotations.Nullable;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 管理 `/stupidexpress setdual_personality <主人格> <副人格>` 指令产生的下一局强制配对。
 *
 * <p>指令是在对局外或开局前执行的，但真正写入词条要等 Harpy 分配词条时才能拿到本局玩家列表。
 * 所以这里先缓存 UUID，等 assignModifiers 运行到合适注入点时再消费。</p>
 */
public final class ForcedDualPersonalityManager {

    /*
     * pendingSubs 是有方向的指定队列：
     * key = 主人格，value = 副人格。
     * 这和恋人不同，恋人是双向等价关系；双重人格必须保存“谁先醒来”。
     */
    private static final Map<UUID, UUID> pendingSubs = new LinkedHashMap<>();
    private static final Map<UUID, String> pendingNames = new HashMap<>();

    private ForcedDualPersonalityManager() {
    }

    public static void setPendingPair(ServerPlayer main, ServerPlayer sub) {
        UUID mainUuid = main.getUUID();
        UUID subUuid = sub.getUUID();

        /*
         * 同一个玩家不能同时出现在两组强制双重人格里。
         * 如果管理员重新指定 A->B 或 B->C，旧关系会被移除，以最后一次指令为准。
         */
        removePendingPair(mainUuid);
        removePendingPair(subUuid);

        pendingSubs.put(mainUuid, subUuid);
        pendingNames.put(mainUuid, main.getScoreboardName());
        pendingNames.put(subUuid, sub.getScoreboardName());
    }

    public static ApplyResult consumeAndApplyPendingPairs(ServerLevel level, List<ServerPlayer> players) {
        if (pendingSubs.isEmpty()) {
            return ApplyResult.empty();
        }

        // 先把 pending 复制出来再清空，避免应用过程中再次触发读取导致重复消费。
        List<DualPersonalityComponent.Pair> requestedPairs = getUniquePendingPairs();
        pendingSubs.clear();
        pendingNames.clear();

        /*
         * 强制指定双重人格时，本局双重人格以指令队列为准。
         * 先清掉随机分配出来的 DUAL_PERSONALITY，再写入有效强制组，
         * 这样不会出现“管理员指定 + 随机残留”的混合状态。
         */
        WorldModifierComponent modifierComponent = WorldModifierComponent.KEY.get(level);
        removeAllDualPersonalityModifiers(modifierComponent);

        DualPersonalityComponent dualComponent = DualPersonalityComponent.KEY.get(level);
        dualComponent.clear();

        Map<UUID, ServerPlayer> participants = new HashMap<>();
        for (ServerPlayer player : players) {
            // 只允许本局实际参局玩家成为强制双重人格；旁观/不在列表内的玩家会被跳过。
            participants.put(player.getUUID(), player);
        }

        List<DualPersonalityComponent.Pair> appliedPairs = new ArrayList<>();
        int skippedPairs = 0;

        for (DualPersonalityComponent.Pair pair : requestedPairs) {
            ServerPlayer main = participants.get(pair.main());
            ServerPlayer sub = participants.get(pair.sub());
            if (main == null || sub == null) {
                // 指令指定的人不在本局时，不保留到下一局，避免旧指令长期污染之后的游戏。
                skippedPairs++;
                continue;
            }

            // Harpy 的词条组件仍然是玩家是否“拥有双重人格”的来源，两边都必须补上词条。
            addModifierIfMissing(modifierComponent, main.getUUID(), SEModifiers.DUAL_PERSONALITY);
            addModifierIfMissing(modifierComponent, sub.getUUID(), SEModifiers.DUAL_PERSONALITY);
            appliedPairs.add(pair);
        }

        if (!appliedPairs.isEmpty()) {
            dualComponent.setForcedPairs(appliedPairs);
        } else {
            dualComponent.sync();
        }
        modifierComponent.sync();

        return new ApplyResult(appliedPairs.size(), skippedPairs);
    }

    private static @Nullable UUID removePendingPair(UUID playerUuid) {
        /*
         * pendingSubs 是 main -> sub，因此移除时要同时查 key 和 value：
         * 玩家既可能是某组主人格，也可能是某组副人格。
         */
        UUID subUuid = pendingSubs.remove(playerUuid);
        UUID mainUuid = null;
        if (subUuid == null) {
            for (Map.Entry<UUID, UUID> entry : pendingSubs.entrySet()) {
                if (entry.getValue().equals(playerUuid)) {
                    mainUuid = entry.getKey();
                    subUuid = playerUuid;
                    break;
                }
            }
            if (mainUuid != null) {
                pendingSubs.remove(mainUuid);
            }
        }

        pendingNames.remove(playerUuid);
        if (subUuid != null) {
            pendingNames.remove(subUuid);
        }
        if (mainUuid != null) {
            pendingNames.remove(mainUuid);
        }
        return subUuid;
    }

    private static List<DualPersonalityComponent.Pair> getUniquePendingPairs() {
        List<DualPersonalityComponent.Pair> pairs = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : pendingSubs.entrySet()) {
            UUID main = entry.getKey();
            UUID sub = entry.getValue();
            if (visited.contains(main) || visited.contains(sub)) {
                // 理论上 setPendingPair 已经去重，这里再兜底，防止手动修改缓存或未来改动造成重复参组。
                continue;
            }
            pairs.add(new DualPersonalityComponent.Pair(main, sub));
            visited.add(main);
            visited.add(sub);
        }
        return pairs;
    }

    public static void removeAllDualPersonalityModifiers(WorldModifierComponent modifierComponent) {
        // 强制指定模式下，本局双重人格完全以指令队列为准，先删除随机残留再写入强制组。
        Collection<ArrayList<Modifier>> allModifiers = modifierComponent.getModifiers().values();
        for (ArrayList<Modifier> modifiers : allModifiers) {
            modifiers.removeIf(SEModifiers.DUAL_PERSONALITY::equals);
        }
    }

    private static void addModifierIfMissing(WorldModifierComponent modifierComponent, UUID playerUuid, Modifier modifier) {
        if (!modifierComponent.isModifier(playerUuid, modifier)) {
            modifierComponent.addModifier(playerUuid, modifier);
        }
    }

    public static String describePlayer(UUID uuid) {
        // 玩家离线后 displayName 不一定还能取到，pendingNames 用于日志/提示里尽量显示人名。
        return pendingNames.getOrDefault(uuid, uuid.toString());
    }

    public record ApplyResult(int appliedPairs, int skippedPairs) {
        public static ApplyResult empty() {
            return new ApplyResult(0, 0);
        }

        public boolean changedAnything() {
            // skipped 也算“发生过处理”，因为它代表一条强制指令被消费并作废了。
            return this.appliedPairs > 0 || this.skippedPairs > 0;
        }
    }
}
