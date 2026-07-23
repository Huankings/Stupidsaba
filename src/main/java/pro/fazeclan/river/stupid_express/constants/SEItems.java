package pro.fazeclan.river.stupid_express.constants;

import dev.doctor4t.ratatouille.util.registrar.ItemRegistrar;
import dev.doctor4t.wathe.Wathe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import pro.fazeclan.river.stupid_express.StupidExpress;

public class SEItems {

    private static ItemRegistrar registrar = new ItemRegistrar(StupidExpress.MOD_ID);

    private static final ResourceKey<CreativeModeTab> EQUIPMENT_GROUP = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wathe.id("equipment"));

    public static final TagKey<Item> DRINKS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(StupidExpress.MOD_ID, "drinks"));

    public static void init() {
        registrar.registerEntries();
    }

}
