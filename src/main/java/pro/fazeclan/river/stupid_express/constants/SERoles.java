package pro.fazeclan.river.stupid_express.constants;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import lombok.Getter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.cca.AbilityCooldownComponent;
import pro.fazeclan.river.stupid_express.role.amnesiac.RoleSelectionHandler;
import pro.fazeclan.river.stupid_express.role.arsonist.ArsonistItemGivingHandler;
import pro.fazeclan.river.stupid_express.role.arsonist.OilDousingHandler;
import pro.fazeclan.river.stupid_express.role.arsonist.cca.DousedPlayerComponent;
import pro.fazeclan.river.stupid_express.role.avaricious.AvariciousGoldHandler;
import pro.fazeclan.river.stupid_express.role.convener.ConvenerSummonHandler;
import pro.fazeclan.river.stupid_express.role.convener.ConvenerDeathProtectionHandler;
import pro.fazeclan.river.stupid_express.role.convener.ConvenerWinHelper;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerMomentumComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerPlayerComponent;
import pro.fazeclan.river.stupid_express.role.convener.packet.ConvenerMorphC2SPacket;
import pro.fazeclan.river.stupid_express.role.initiate.InitiateShopHandler;
import pro.fazeclan.river.stupid_express.role.necromancer.RevivalSelectionHandler;
import pro.fazeclan.river.stupid_express.role.thief.ThiefItemTracker;
import pro.fazeclan.river.stupid_express.role.thief.packet.ThiefTakeItemC2SPacket;
import pro.fazeclan.river.stupid_express.shop.SEShopRegistry;

import java.util.HashMap;

public class SERoles {

    @Getter
    private static final HashMap<String, Role> ROLES = new HashMap<>();
    //失忆患者(中立)
    public static Role AMNESIAC = registerRole(new Role(
            StupidExpress.id("amnesiac"),
            0x9baae8,
            false,
            false,
            Role.MoodType.REAL,
            WatheRoles.CIVILIAN.getMaxSprintTime(),
            false
    ));
    //纵火犯(中立)
    public static Role ARSONIST = registerRole(new Role(
            StupidExpress.id("arsonist"),
            0xfc9526,
            false,
            false,
            Role.MoodType.FAKE,
            -1,
            true
    ));
    //扒手(杀手)
    public static Role AVARICIOUS = registerRole(new Role(
            StupidExpress.id("avaricious"),
            0x8f00ff,
            false,
            true,
            Role.MoodType.FAKE,
            -1,
            true
    ));
    //死灵法师(杀手)
    public static Role NECROMANCER = registerRole(new Role(
            StupidExpress.id("necromancer"),
            0x9457ff,
            false,
            true,
            Role.MoodType.FAKE,
            -1,
            true
    ));
    //初学者(中立)
    public static Role INITIATE = registerRole(new Role(
            StupidExpress.id("initiate"),
            0xffd154,
            false,
            false,
            Role.MoodType.REAL,
            WatheRoles.CIVILIAN.getMaxSprintTime(),
            true
    ));
    //小偷(中立)
    public static Role THIEF = registerRole(new Role(
        StupidExpress.id("thief"), 
        0x7a3002, 
        false, 
        false, 
        Role.MoodType.FAKE, 
        -1, 
        true
    ));
    //召集者(中立)
    public static Role CONVENER = registerRole(new Role(
            StupidExpress.id("convener"),
            0x5734e5,
            false,
            false,
            Role.MoodType.FAKE,
            -1,
            true
    ));

    public static void init() {

        // 在角色初始化阶段顺手注册自定义商店。
        // 这样后续新增职业商店时，只需要在这里补一行注册即可。
        SEShopRegistry.registerRoleShop(INITIATE, InitiateShopHandler::getShopEntries);

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
            var killerRoleCount = (int) Math.floor((float) GameFunctions.getReadyPlayerCount(level) / (float) gameWorldComponent.getKillerDividend());

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
        Harpymodloader.setRoleMaximum(THIEF, 1);
        ThiefTakeItemC2SPacket.register();
        ThiefItemTracker.init();

        ModdedRoleAssigned.EVENT.register((player, role) -> {
			if (role.equals(THIEF)) {
				AbilityCooldownComponent component = AbilityCooldownComponent.KEY.get(player);
				component.setCooldown(ThiefTakeItemC2SPacket.THIEF_COOLDOWN);
				component.sync();
			}
		});

        /// CONVENER

        Harpymodloader.setRoleMaximum(CONVENER, 1);
        ConvenerSummonHandler.init();
        ConvenerDeathProtectionHandler.init();
        ConvenerMorphC2SPacket.register();

        ModdedRoleAssigned.EVENT.register((player, role) -> {
            if (!role.equals(CONVENER)) {
                return;
            }

            // 召集者开局直接携带一把开锁器，
            // 方便其像你要求的那样拥有更强的前期机动与开门能力。
            player.addItem(WatheItems.LOCKPICK.getDefaultInstance());

            // 召集者是整局状态型角色，因此在分配身份的瞬间就初始化目标次数、
            // 已解锁头像列表以及当前伪装状态，避免旧数据残留到新对局。
            ConvenerPlayerComponent convenerComponent = ConvenerPlayerComponent.KEY.get(player);
            convenerComponent.initializeForRole();
            convenerComponent.setRequiredSummons(ConvenerWinHelper.getRequiredSummons(player.level()));
            convenerComponent.sync();

            ConvenerDisguiseComponent disguiseComponent = ConvenerDisguiseComponent.KEY.get(player);
            disguiseComponent.clearDisguise();

            ConvenerMomentumComponent.KEY.get(player).reset();

            AbilityCooldownComponent cooldownComponent = AbilityCooldownComponent.KEY.get(player);
            cooldownComponent.setCooldown(0);
            cooldownComponent.sync();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                var gameWorldComponent = GameWorldComponent.KEY.get(player.level());
                if (!gameWorldComponent.isRole(player, CONVENER)) {
                    continue;
                }

                // 身份刚分配出来的那一小段时间里，参局人数可能还没完全稳定。
                // 因此这里每 tick 兜底校正一次，直到组件里的目标值和本局真实人数一致为止。
                ConvenerWinHelper.refreshRequiredSummons(player);
            }
        });
    }

    public static Role registerRole(Role role) {
        WatheRoles.registerRole(role);
        ROLES.put(role.identifier().getPath(), role);
        return role;
    }

}
