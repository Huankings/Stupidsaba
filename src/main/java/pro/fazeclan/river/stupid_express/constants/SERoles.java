package pro.fazeclan.river.stupid_express.constants;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import lombok.Getter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.Util;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import pro.fazeclan.river.stupid_express.BuyableShopEntry;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.cca.AbilityCooldownComponent;
import pro.fazeclan.river.stupid_express.role.amnesiac.RoleSelectionHandler;
import pro.fazeclan.river.stupid_express.role.arsonist.ArsonistItemGivingHandler;
import pro.fazeclan.river.stupid_express.role.arsonist.OilDousingHandler;
import pro.fazeclan.river.stupid_express.role.arsonist.cca.DousedPlayerComponent;
import pro.fazeclan.river.stupid_express.role.avaricious.AvariciousGoldHandler;
import pro.fazeclan.river.stupid_express.role.necromancer.RevivalSelectionHandler;
import pro.fazeclan.river.stupid_express.role.thief.packet.ThiefTakeItemC2SPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SERoles {

    @Getter
    private static final HashMap<String, Role> ROLES = new HashMap<>();

    public static Role AMNESIAC = registerRole(new Role(
            StupidExpress.id("amnesiac"),
            0x9baae8,
            false,
            false,
            Role.MoodType.REAL,
            WatheRoles.CIVILIAN.getMaxSprintTime(),
            false
    ));

    public static Role ARSONIST = registerRole(new Role(
            StupidExpress.id("arsonist"),
            0xfc9526,
            false,
            false,
            Role.MoodType.FAKE,
            -1,
            true
    ));

    public static Role AVARICIOUS = registerRole(new Role(
            StupidExpress.id("avaricious"),
            0x8f00ff,
            false,
            true,
            Role.MoodType.FAKE,
            -1,
            true
    ));

    public static Role NECROMANCER = registerRole(new Role(
            StupidExpress.id("necromancer"),
            0x9457ff,
            false,
            true,
            Role.MoodType.FAKE,
            -1,
            true
    ));

    public static Role INITIATE = registerRole(new Role(
            StupidExpress.id("initiate"),
            0xffd154,
            false,
            false,
            Role.MoodType.REAL,
            WatheRoles.CIVILIAN.getMaxSprintTime(),
            true
    ));

    public static Role THIEF = registerRole(new Role(
        StupidExpress.id("thief"), 
        0x7a3002, 
        false, 
        false, 
        Role.MoodType.FAKE, 
        -1, 
        true
    ));

    public static List<ShopEntry> INITIATE_SHOP = Util.make(new ArrayList<>(), entries -> {
        entries.add(new BuyableShopEntry(WatheItems.KNIFE.getDefaultInstance(), 200, ShopEntry.Type.WEAPON));
    });

    public static void init() {

        /// AMNESIAC

        Harpymodloader.setRoleMaximum(AMNESIAC, 1);
        RoleSelectionHandler.init();

        /// ARSONIST

        Harpymodloader.setRoleMaximum(ARSONIST, 1);
        OilDousingHandler.init();
        ArsonistItemGivingHandler.init();

        ResetPlayerEvent.EVENT.register(player -> {
            var component = DousedPlayerComponent.KEY.get(player);
            component.reset();
            component.sync();
        });

        /// NECROMANCER

        RevivalSelectionHandler.init();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var playerList = server.getPlayerList().getPlayers();
            if (playerList.isEmpty()) {
                return;
            }
            var level = playerList.getFirst().level();
            var gameWorldComponent = GameWorldComponent.KEY.get(level);
            var killerRoleCount = (int) Math.floor((float) playerList.size() / (float) gameWorldComponent.getKillerDividend());

            if (killerRoleCount > 1) {
                Harpymodloader.setRoleMaximum(NECROMANCER, 1);
                Harpymodloader.setRoleMaximum(AVARICIOUS, 1);
                Harpymodloader.setRoleMaximum(INITIATE, 1); // setting the other initiate will be my job
            } else {
                Harpymodloader.setRoleMaximum(NECROMANCER, 0);
                Harpymodloader.setRoleMaximum(AVARICIOUS, 0);
                Harpymodloader.setRoleMaximum(INITIATE, 0);
            }
        });

        /// AVARICIOUS

        AvariciousGoldHandler.onGameStart();

        /// THIEF

        ThiefTakeItemC2SPacket.register();
        ModdedRoleAssigned.EVENT.register((player, role) -> {
			if (role.equals(THIEF)) {
				AbilityCooldownComponent component = AbilityCooldownComponent.KEY.get(player);
				component.setCooldown(ThiefTakeItemC2SPacket.THIEF_COOLDOWN);
				component.sync();
			}
		});
    }

    public static Role registerRole(Role role) {
        WatheRoles.registerRole(role);
        ROLES.put(role.identifier().getPath(), role);
        return role;
    }

}
