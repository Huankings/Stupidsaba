package pro.fazeclan.river.stupid_express.role.thief;

import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public class ThiefItemRules {

    // 获取物品的资源位置
    public static ResourceLocation getId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    // 从mod + 名称构建一个资源位置
    public static ResourceLocation getId(String modId, String itemName) {
        return ResourceLocation.fromNamespaceAndPath(modId, itemName);
    }

    // 检查小偷是否被允许拿这个物品
    public static boolean canTake(Item item) {
        return CAN_TAKE.contains(getId(item));
    }

    // 如果为是，当游戏中物品可用且小偷还活着时，游戏不会结束
    public static boolean isKeepGameGoing(Item item) {
        return KEEP_GAME_GOING.contains(getId(item));
    }

    private static final List<ResourceLocation> KEEP_GAME_GOING = initKeepGameGoing();
    private static final List<ResourceLocation> CAN_TAKE = initCanTake();

    private static List<ResourceLocation> initKeepGameGoing() {
        List<ResourceLocation> list = new ArrayList<>();
        list.add(getId(WatheItems.REVOLVER));
        list.add(getId(WatheItems.KNIFE));
        list.add(getId("noellesroles", "hunting_knife")); // Hunting Knife from Hunter role in noellesroles
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
        list.add(getId("noellesroles", "hunting_knife"));
        list.add(getId("noellesroles", "dream_imprint"));
        list.add(getId("noellesroles", "knockout_drug"));
        list.add(getId("noellesroles", "poison_injector"));
        list.add(getId("noellesroles", "blowgun"));
        list.add(getId("noellesroles", "pill"));
        list.add(getId("noellesroles", "tape"));

        return list;
    }
}