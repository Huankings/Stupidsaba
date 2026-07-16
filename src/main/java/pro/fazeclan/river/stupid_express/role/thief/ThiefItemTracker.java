package pro.fazeclan.river.stupid_express.role.thief;

import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.game.GameFunctions;
import lombok.Getter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ThiefItemTracker {
    // 记录掉落的物品
    private static final List<UUID> ACTIVE_ENTITY_ITEMS = new ArrayList<>();
    // 跟踪玩家物品栏中的物品
    private static final List<ItemStack> ACTIVE_INVENTORY_ITEMS = new ArrayList<>();

    @Getter
    private static boolean weaponAvailable;
    
    public static void init() {
        // 注意世界中出现的物品
        ServerEntityEvents.ENTITY_LOAD.register((entity, serverLevel) -> {
            if (entity instanceof ItemEntity item && shouldTrack(item)) {
                trackEntityItem(item);
                updateTrackedInventoryItems(serverLevel);
                updateWeaponAvailable();
            }
        });

        // 注意世界中消失的物品
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, serverLevel) -> {
            if (entity instanceof ItemEntity item) {
                untrackEntityItem(item);
                updateTrackedInventoryItems(serverLevel);
                updateWeaponAvailable();
            }
        });

        // 当游戏模式开始初始化时检查库存
        GameEvents.ON_FINISH_INITIALIZE.register((world, gameWorldComponent) -> {
			updateTrackedInventoryItems((ServerLevel)world);
            updateWeaponAvailable();
		});

        // 在游戏开始/结束时重置
        GameEvents.ON_GAME_START.register((gameMode) -> {
			ThiefItemTracker.reset();
		});

        GameEvents.ON_GAME_STOP.register((gameMode) -> {
			ThiefItemTracker.reset();
		});
    }

    // 玩家死亡时检查物品栏
    public static void onKillPlayer(ServerPlayer victim) {
        updateTrackedInventoryItems((ServerLevel)victim.level());
        updateWeaponAvailable();
    }

    // 当玩家购买物品时检查库存
    public static void onBuyItem(ServerPlayer player) {
        updateTrackedInventoryItems((ServerLevel)player.level());
        updateWeaponAvailable();
    }

    private static void reset() {
        ACTIVE_ENTITY_ITEMS.clear();
        ACTIVE_INVENTORY_ITEMS.clear();
        weaponAvailable = false;
    }
    
    private static void trackEntityItem(ItemEntity item) {
        if (item == null || !item.isAlive()) return;
        
        UUID uuid = item.getUUID();
        if (!ACTIVE_ENTITY_ITEMS.contains(uuid)) {
            ACTIVE_ENTITY_ITEMS.add(uuid);
        }
    }
    
    private static void untrackEntityItem(ItemEntity item) {
        if (item == null) return;
        
        UUID uuid = item.getUUID();
        ACTIVE_ENTITY_ITEMS.remove(uuid);
    }

    // 扫描所有活着玩家的背包物品
    private static void updateTrackedInventoryItems(ServerLevel serverLevel) {
        List<ServerPlayer> alivePlayers = serverLevel.getPlayers(p -> GameFunctions.isPlayerAliveAndSurvival(p));
        
        ACTIVE_INVENTORY_ITEMS.clear();
        
        for (ServerPlayer player : alivePlayers) {
            player.getInventory().items.stream()
                .filter(stack -> !stack.isEmpty() && 
                    ThiefItemRules.isKeepGameGoing(stack.getItem()))
                .forEach(ACTIVE_INVENTORY_ITEMS::add);
        }
    }

    private static void updateWeaponAvailable() {
        weaponAvailable = !ACTIVE_ENTITY_ITEMS.isEmpty() || !ACTIVE_INVENTORY_ITEMS.isEmpty();
    }
    
    private static boolean shouldTrack(ItemEntity itemEntity) {
        if (itemEntity == null) return false;
        return ThiefItemRules.isKeepGameGoing(itemEntity.getItem().getItem());
    }
}