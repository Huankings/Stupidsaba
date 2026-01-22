package pro.fazeclan.river.stupid_express.constants;

import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.harpymodloader.events.ModifierAssigned;
import org.agmas.harpymodloader.modifiers.HMLModifiers;
import org.agmas.harpymodloader.modifiers.Modifier;
import pro.fazeclan.river.stupid_express.StupidExpress;

public class SEModifiers {

    public static Modifier LOVERS = HMLModifiers.registerModifier(new Modifier(
            StupidExpress.id("lovers"),
            0xf38aff,
            null,
            null,
            false,
            false
    ));

    public static void init() {

        assignModifierComponents();

        /// LOVERS
        Harpymodloader.MODIFIER_MAX.put(StupidExpress.id("lovers"), 1);

    }

    public static void assignModifierComponents() {

        /// LOVERS
        ModifierAssigned.EVENT.register(((player, modifier) -> {
            if (!modifier.equals(LOVERS)) {
                return;
            }
            if (!(player instanceof ServerPlayer lover)) {
                return;
            }

            var level = (ServerLevel) lover.level();
            var gameWorldComponent = GameWorldComponent.KEY.get(level);

            // choose second lover
            ServerPlayer loverTwo;

            do {
                loverTwo = level.getRandomPlayer();
            } while (loverTwo == null || gameWorldComponent.getRole(loverTwo) == null || !gameWorldComponent.isInnocent(loverTwo)
                    || gameWorldComponent.isRole(loverTwo, WatheRoles.VIGILANTE) || lover.equals(loverTwo));

            // assign both lovers
            var worldModifierComponent = WorldModifierComponent.KEY.get(level);
            worldModifierComponent.addModifier(loverTwo.getUUID(), LOVERS);
        }));

    }

}
