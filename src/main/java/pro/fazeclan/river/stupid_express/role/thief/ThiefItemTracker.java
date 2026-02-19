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
    // Keep track of dropped items
    private static final List<UUID> ACTIVE_ENTITY_ITEMS = new ArrayList<>();
    // Keep track of items in player inventories
    private static final List<ItemStack> ACTIVE_INVENTORY_ITEMS = new ArrayList<>();

    @Getter
    private static boolean weaponAvailable;
    
    public static void init() {
        // Watch for items spawning in the world
        ServerEntityEvents.ENTITY_LOAD.register((entity, serverLevel) -> {
            if (entity instanceof ItemEntity item && shouldTrack(item)) {
                trackEntityItem(item);
                updateTrackedInventoryItems(serverLevel);
                updateWeaponAvailable();
            }
        });

        // Watch for items despawning in the world
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, serverLevel) -> {
            if (entity instanceof ItemEntity item) {
                untrackEntityItem(item);
                updateTrackedInventoryItems(serverLevel);
                updateWeaponAvailable();
            }
        });

        // Check inventories when the gamemode starts initializing
        GameEvents.ON_FINISH_INITIALIZE.register((world, gameWorldComponent) -> {
			updateTrackedInventoryItems((ServerLevel)world);
            updateWeaponAvailable();
		});

        // Reset when game starts/ends
        GameEvents.ON_GAME_START.register((gameMode) -> {
			ThiefItemTracker.reset();
		});

        GameEvents.ON_GAME_STOP.register((gameMode) -> {
			ThiefItemTracker.reset();
		});
    }

    // Check inventories when a player dies
    public static void onKillPlayer(ServerPlayer victim) {
        updateTrackedInventoryItems((ServerLevel)victim.level());
        updateWeaponAvailable();
    }

    // Check Inventories when a player buys a item
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

    // Scan all alive players items in their inventory
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