package pro.fazeclan.river.stupid_express.constants;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.economy.EconomyApi;
import dev.doctor4t.wathe.api.shop.ShopApi;
import dev.doctor4t.wathe.api.task.TaskCompletionApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import lombok.Getter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.cca.AbilityCooldownComponent;
import pro.fazeclan.river.stupid_express.role.initiate.InitiateShopHandler;
import pro.fazeclan.river.stupid_express.role.thief.ThiefItemTracker;
import pro.fazeclan.river.stupid_express.role.thief.packet.ThiefTakeItemC2SPacket;
import pro.fazeclan.river.stupid_express.shop.SEShops;

import java.util.HashMap;

public class SERoles {

    @Getter
    private static final HashMap<String, Role> ROLES = new HashMap<>();
    //初学者(普通中立)
    public static Role INITIATE = registerRole(new Role(
            StupidExpress.id("initiate"),
            0xffd154,
            false,
            false,
            Role.MoodType.REAL,
            WatheRoles.CIVILIAN.getMaxSprintTime(),
            true
    ));
    //小偷(独立中立)
    public static Role THIEF = registerRole(new Role(
        StupidExpress.id("thief"), 
        0x7a3002, 
        false, 
        false, 
        Role.MoodType.FAKE, 
        -1, 
        true
    ));

    public static void init() {

        /*
         * 在角色初始化阶段注册自定义商店。
         * 商品列表仍由 InitiateShopHandler 按职业分类维护；Wathe ShopApi 负责实际渲染与购买流程。
         */
        ShopApi.registerRoleShop(INITIATE, SEShops.provider(InitiateShopHandler::getShopEntries));
        registerEconomyApi();

        /// INITIATE

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var playerList = server.getPlayerList().getPlayers();
            if (playerList.isEmpty()) {
                return;
            }
            var level = playerList.getFirst().level();
            var gameWorldComponent = GameWorldComponent.KEY.get(level);
            var killerRoleCount = (int) Math.floor((float) GameFunctions.getReadyPlayerCount(level) / (float) gameWorldComponent.getKillerDividend());

            if (killerRoleCount > 1) {
                Harpymodloader.setRoleMaximum(INITIATE, 1); // setting the other initiate will be my job
            } else {
                Harpymodloader.setRoleMaximum(INITIATE, 0);
            }
        });

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
    }

    private static void registerEconomyApi() {
        /*
         * 初学者是中立真实心情角色，但它有自己的商店和金币目标，
         * 因此需要显式注册金币 HUD，避免再 mixin StoreRenderer 复制整段渲染逻辑。
         */
        EconomyApi.registerBalanceHudRole(INITIATE);

        /*
         * 初学者的任务金币现在直接基于 Wathe 的真实任务完成事件发放。
         * 这比旧的 setMood 注入更准确：只有任务真正完成才触发，也不会被其他心情变化误判。
         */
        TaskCompletionApi.registerTaskIncomeProvider(
                StupidExpress.id("initiate_task_income"),
                TaskCompletionApi.DEFAULT_PRIORITY,
                context -> context.role() == INITIATE ? 50 : 0
        );
    }

    public static Role registerRole(Role role) {
        WatheRoles.registerRole(role);
        ROLES.put(role.identifier().getPath(), role);
        return role;
    }

}
