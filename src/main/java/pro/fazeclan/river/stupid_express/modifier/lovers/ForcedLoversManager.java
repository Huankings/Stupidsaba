package pro.fazeclan.river.stupid_express.modifier.lovers;

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

public final class ForcedLoversManager {

    /*
     * pendingPartners 保存“下一局开局时要强制指定的恋人”。
     * 这和 HarpyModLoader 的 /forceRole、/forceModifier 一样，是运行时调试队列：
     * 指令执行时不立刻影响当前局，而是在下一次 Wathe 初始化职业/词条时消费。
     */
    private static final Map<UUID, UUID> pendingPartners = new LinkedHashMap<>();
    private static final Map<UUID, String> pendingNames = new HashMap<>();

    private ForcedLoversManager() {
    }

    public static void setPendingPair(ServerPlayer first, ServerPlayer second) {
        UUID firstUuid = first.getUUID();
        UUID secondUuid = second.getUUID();

        /*
         * 覆盖规则：
         * 一个玩家只能被安排进一对指定恋人。
         * 例如先执行 a+b，再执行 a+c，就会拆掉 a+b 并建立 a+c；
         * 如果 c 原本还有 c+d，也会同时拆掉 c+d，避免一人多配对。
         */
        removePendingPair(firstUuid);
        removePendingPair(secondUuid);

        pendingPartners.put(firstUuid, secondUuid);
        pendingPartners.put(secondUuid, firstUuid);
        pendingNames.put(firstUuid, first.getScoreboardName());
        pendingNames.put(secondUuid, second.getScoreboardName());
    }

    public static @Nullable RemovedPair removePendingOrActivePair(ServerLevel level, ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        UUID pendingPartner = removePendingPair(playerUuid);
        if (pendingPartner != null) {
            return new RemovedPair(playerUuid, pendingPartner, true);
        }

        LoversPairComponent pairComponent = LoversPairComponent.KEY.get(level);
        if (!pairComponent.isForcedPair(playerUuid)) {
            return null;
        }

        UUID activePartner = pairComponent.removePair(playerUuid);
        if (activePartner == null) {
            return null;
        }

        WorldModifierComponent modifierComponent = WorldModifierComponent.KEY.get(level);
        removeModifier(modifierComponent, playerUuid, SEModifiers.LOVERS);
        removeModifier(modifierComponent, activePartner, SEModifiers.LOVERS);
        modifierComponent.sync();

        return new RemovedPair(playerUuid, activePartner, false);
    }

    private static @Nullable UUID removePendingPair(UUID playerUuid) {
        UUID partnerUuid = pendingPartners.remove(playerUuid);
        pendingNames.remove(playerUuid);
        if (partnerUuid != null) {
            pendingPartners.remove(partnerUuid);
            pendingNames.remove(partnerUuid);
        }
        return partnerUuid;
    }

    public static ApplyResult consumeAndApplyPendingPairs(ServerLevel level, List<ServerPlayer> players) {
        if (pendingPartners.isEmpty()) {
            return ApplyResult.empty();
        }

        List<LoversPairComponent.Pair> requestedPairs = getUniquePendingPairs();
        pendingPartners.clear();
        pendingNames.clear();

        /*
         * 一旦管理员使用了 setlovers 指令，本局恋人分配就以指令为准。
         * Harpy 原本可能已经随机分出一对恋人；这里先清掉本局所有 LOVERS，
         * 再只写入有效的指定配对，避免“指定恋人 + 随机恋人”混在一起。
         */
        WorldModifierComponent modifierComponent = WorldModifierComponent.KEY.get(level);
        removeAllLoversModifiers(modifierComponent);

        LoversPairComponent pairComponent = LoversPairComponent.KEY.get(level);
        pairComponent.clear();

        Map<UUID, ServerPlayer> participants = new HashMap<>();
        for (ServerPlayer player : players) {
            participants.put(player.getUUID(), player);
        }

        List<LoversPairComponent.Pair> appliedPairs = new ArrayList<>();
        int skippedPairs = 0;

        for (LoversPairComponent.Pair pair : requestedPairs) {
            ServerPlayer first = participants.get(pair.first());
            ServerPlayer second = participants.get(pair.second());
            if (first == null || second == null) {
                /*
                 * 需求要求：指定的两名玩家只要有一方没有参与本局，这次指定就作废。
                 * 因此这里不保留到下一局，也不会给参与的一方补 LOVERS。
                 */
                skippedPairs++;
                continue;
            }

            addModifierIfMissing(modifierComponent, first.getUUID(), SEModifiers.LOVERS);
            addModifierIfMissing(modifierComponent, second.getUUID(), SEModifiers.LOVERS);
            appliedPairs.add(pair);
        }

        if (!appliedPairs.isEmpty()) {
            pairComponent.setForcedPairs(appliedPairs);
        } else {
            pairComponent.sync();
        }
        modifierComponent.sync();

        return new ApplyResult(appliedPairs.size(), skippedPairs);
    }

    private static List<LoversPairComponent.Pair> getUniquePendingPairs() {
        List<LoversPairComponent.Pair> pairs = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : pendingPartners.entrySet()) {
            UUID first = entry.getKey();
            UUID second = entry.getValue();
            if (visited.contains(first) || visited.contains(second)) {
                continue;
            }
            pairs.add(new LoversPairComponent.Pair(first, second));
            visited.add(first);
            visited.add(second);
        }
        return pairs;
    }

    public static void removeAllLoversModifiers(WorldModifierComponent modifierComponent) {
        Collection<ArrayList<Modifier>> allModifiers = modifierComponent.getModifiers().values();
        for (ArrayList<Modifier> modifiers : allModifiers) {
            modifiers.removeIf(SEModifiers.LOVERS::equals);
        }
    }

    private static void addModifierIfMissing(WorldModifierComponent modifierComponent, UUID playerUuid, Modifier modifier) {
        if (!modifierComponent.isModifier(playerUuid, modifier)) {
            modifierComponent.addModifier(playerUuid, modifier);
        }
    }

    private static void removeModifier(WorldModifierComponent modifierComponent, UUID playerUuid, Modifier modifier) {
        modifierComponent.getModifiers(playerUuid).removeIf(modifier::equals);
    }

    public static String describePlayer(UUID uuid) {
        return pendingNames.getOrDefault(uuid, uuid.toString());
    }

    public record RemovedPair(UUID player, UUID partner, boolean pending) {
    }

    public record ApplyResult(int appliedPairs, int skippedPairs) {
        public static ApplyResult empty() {
            return new ApplyResult(0, 0);
        }

        public boolean changedAnything() {
            return this.appliedPairs > 0 || this.skippedPairs > 0;
        }
    }
}
