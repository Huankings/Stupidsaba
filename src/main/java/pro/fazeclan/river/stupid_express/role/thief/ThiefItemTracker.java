package pro.fazeclan.river.stupid_express.role.thief;

import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ThiefItemTracker {
    private static final List<UUID> ACTIVE_ITEMS = new ArrayList<>();
    
    public static void init() {
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if (trackedEntity instanceof ItemEntity item && shouldTrack(item)) {
                trackItem(item);
            }
        });
    }

    public static void clear() {
        ACTIVE_ITEMS.clear();
    }
    
    public static void trackItem(ItemEntity item) {
        if (item == null || !item.isAlive()) return;
        
        UUID uuid = item.getUUID();
        if (!ACTIVE_ITEMS.contains(uuid)) {
            ACTIVE_ITEMS.add(uuid);
        }
    }
    
    public static void untrackItem(ItemEntity item) {
        if (item == null) return;
        
        UUID uuid = item.getUUID();
        if (ACTIVE_ITEMS.contains(uuid)) {
            ACTIVE_ITEMS.remove(uuid);
        }
    }
    
    public static boolean keepGameGoing(ServerLevel serverLevel) {
        if (serverLevel == null) return false;
        
        synchronized (ACTIVE_ITEMS) {
            ACTIVE_ITEMS.removeIf(uuid -> {
                Entity entity = serverLevel.getEntity(uuid);
                return entity == null || !entity.isAlive() || entity.isRemoved();
            });
            
            return !ACTIVE_ITEMS.isEmpty();
        }
    }
    
    public static boolean shouldTrack(ItemEntity itemEntity) {
        if (itemEntity == null || !itemEntity.isAlive()) return false;
        return ThiefItemRules.isKeepGameGoing(itemEntity.getItem().getItem());
    }
}