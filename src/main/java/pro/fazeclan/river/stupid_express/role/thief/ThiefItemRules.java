package pro.fazeclan.river.stupid_express.role.thief;

import java.util.ArrayList;
import java.util.List;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ThiefItemRules {

    public static ResourceLocation getId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static ResourceLocation getId(String modId, String itemName) {
        return ResourceLocation.fromNamespaceAndPath(modId, itemName);
    }

    public static boolean canTake(Item item) {
        return CAN_TAKE.contains(getId(item));
    }

    public static boolean isKeepGameGoing(Item item) {
        return KEEP_GAME_GOING.contains(getId(item));
    }

    private static final List<ResourceLocation> KEEP_GAME_GOING = initKeepGameGoing();
    private static final List<ResourceLocation> CAN_TAKE = initCanTake();

    private static List<ResourceLocation> initKeepGameGoing() {
        List<ResourceLocation> list = new ArrayList<>();
        list.add(getId(WatheItems.REVOLVER));
        list.add(getId(WatheItems.KNIFE));
        list.add(getId("kinswathe", "hunting_knife")); // Hunting Knife from Hunter role in kins wathe
        return list;
    }

    private static List<ResourceLocation> initCanTake() {
        List<ResourceLocation> list = new ArrayList<>();
        list.add(getId(WatheItems.REVOLVER));
        list.add(getId(WatheItems.KNIFE));
        list.add(getId(WatheItems.GRENADE));
        list.add(getId(WatheItems.SCORPION));
        list.add(getId(WatheItems.POISON_VIAL));
        list.add(getId(WatheItems.CROWBAR));
        list.add(getId(WatheItems.LOCKPICK));
        list.add(getId(WatheItems.FIRECRACKER));
        list.add(getId(WatheItems.BODY_BAG));
        list.add(getId(WatheItems.NOTE));
        list.add(getId("noellesroles", "master_key")); // Master Key from Conductor role in noellesroles
        return list;
    }
}