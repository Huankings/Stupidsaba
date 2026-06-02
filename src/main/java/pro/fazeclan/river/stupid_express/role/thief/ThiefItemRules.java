package pro.fazeclan.river.stupid_express.role.thief;

import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public class ThiefItemRules {

    // Gets the ResourceLocation for an item
    public static ResourceLocation getId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    // Builds an ResourceLocation from mod + name
    public static ResourceLocation getId(String modId, String itemName) {
        return ResourceLocation.fromNamespaceAndPath(modId, itemName);
    }

    // Checks if thief is allowed to take the item
    public static boolean canTake(Item item) {
        return CAN_TAKE.contains(getId(item));
    }

    // If true, game doesn't end when item is available in the game and the thief is alive
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
        list.add(getId("noellesroles", "throwing_axe"));//noellesroles throwing_axe
        list.add(getId("noellesroles", "robber_pistol"));
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
        list.add(getId(WatheItems.BAT));
        list.add(getId("noellesroles", "throwing_axe"));//noellesroles throwing_axe
        list.add(getId("noellesroles", "robber_pistol"));
        list.add(getId("noellesroles", "master_key")); // Master Key from Conductor role in noellesroles
        list.add(getId("noellesroles", "capture_device"));
        list.add(getId("noellesroles", "defense_vial"));
        list.add(getId("noellesroles", "delusion_vial"));
        list.add(getId("noellesroles", "role_mine"));
        list.add(getId("kinswathe", "hunting_knife"));
        list.add(getId("kinswathe", "dream_imprint"));
        list.add(getId("kinswathe", "knockout_drug"));
        list.add(getId("kinswathe", "poison_injector"));
        list.add(getId("kinswathe", "blowgun"));
        list.add(getId("kinswathe", "pill"));
        list.add(getId("starryexpress", "tape"));

        return list;
    }
}