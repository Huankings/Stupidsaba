package pro.fazeclan.river.stupid_express.role.arsonist;

import dev.doctor4t.wathe.index.WatheItems;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import pro.fazeclan.river.stupid_express.constants.SEItems;
import pro.fazeclan.river.stupid_express.constants.SERoles;

public class ArsonistItemGivingHandler {

    public static void init() {
        ModdedRoleAssigned.EVENT.register(((player, role) -> {
            if (role.equals(SERoles.ARSONIST)) {
                // 纵火犯开局除了自己的专属纵火工具外，
                // 还额外携带一把原版 Wathe 的撬棍，方便前期破门与机动。
                player.addItem(SEItems.JERRY_CAN.getDefaultInstance());
                player.addItem(SEItems.LIGHTER.getDefaultInstance());
                player.addItem(WatheItems.CROWBAR.getDefaultInstance());
            }
        }));
    }

}
