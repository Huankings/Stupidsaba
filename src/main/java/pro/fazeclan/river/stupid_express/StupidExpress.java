package pro.fazeclan.river.stupid_express;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.fazeclan.river.stupid_express.communication.StupidExpressCommunicationManager;
import pro.fazeclan.river.stupid_express.constants.SEItems;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.record.StupidExpressReplay;

public class StupidExpress implements ModInitializer {

    public static String MOD_ID = "stupid_express";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final StupidExpressConfig CONFIG = ConfigApiJava.registerAndLoadConfig(StupidExpressConfig::new);

    @Override
    public void onInitialize() {

        SERoles.init();
        SEModifiers.init();
        StupidExpressCommunicationManager.init();

        // mod stuff
        SEItems.init();
        StupidExpressReplay.init();

    }

    public static ResourceLocation id(String key) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, key);
    }

}
