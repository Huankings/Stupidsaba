package pro.fazeclan.river.stupid_express.constants;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.economy.EconomyApi;
import dev.doctor4t.wathe.api.shop.ShopApi;
import dev.doctor4t.wathe.api.task.TaskCompletionApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import lombok.Getter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.nbt.CompoundTag;
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
import pro.fazeclan.river.stupid_express.record.StupidExpressReplay;
import pro.fazeclan.river.stupid_express.shop.SEShops;

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

        /*
         * 在角色初始化阶段注册自定义商店。
         * 商品列表仍由 InitiateShopHandler 按职业分类维护；Wathe ShopApi 负责实际渲染与购买流程。
         */
        ShopApi.registerRoleShop(INITIATE, SEShops.provider(InitiateShopHandler::getShopEntries));
        /*
         * 死灵法师是否拥有杀手商店由配置控制。
         * 旧实现用客户端 mixin 取消 LimitedInventoryScreen.init；Wathe 商店流程改为 ShopApi 后，
         * 继续 mixin 屏幕初始化会因为注入点变化而崩溃。这里改为公共商店修改器：
         * 客户端渲染和服务端购买都会看到同一份“空商店”，不会出现只隐藏按钮但服务端仍可买的问题。
         */
        ShopApi.registerShopModifier(
                StupidExpress.id("necromancer_no_shop"),
                ShopApi.DEFAULT_PRIORITY,
                (context, entries) -> {
                    if (context.role() == NECROMANCER && !StupidExpress.CONFIG.rolesSection.necromancerSection.necromancerHasShop) {
                        entries.clear();
                    }
                }
        );
        registerEconomyApi();

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

        /*
         * 扒手拥有杀手商店能力，所以按 Wathe 旧逻辑会自动吃到普通被动收入。
         * 但它的设计金币来源是“附近玩家结算”，这里用 DENY 关闭通用被动收入，
         * 保留自己的特殊结算逻辑。
         */
        EconomyApi.registerPassiveIncomeRule(
                StupidExpress.id("avaricious_no_default_passive_income"),
                EconomyApi.DEFAULT_PRIORITY,
                context -> context.role() == AVARICIOUS
                        ? EconomyApi.PassiveIncomeDecision.DENY
                        : EconomyApi.PassiveIncomeDecision.PASS
        );

        /*
         * 召集者完成任务增加反伤护盾进度。
         * 旧实现监听 TaskCompletePayload；现在直接监听任务完成 API，不再依赖网络同步包这个内部细节。
         */
        TaskCompletionApi.AFTER_TASK_COMPLETE.register(context -> {
            if (!StupidExpress.CONFIG.rolesSection.convenerSection.convenerCounterShieldEnabled) {
                return;
            }
            if (!context.gameWorld().isRunning()) {
                return;
            }
            if (!GameFunctions.isPlayerAliveAndSurvival(context.player())) {
                return;
            }
            if (context.role() != CONVENER) {
                return;
            }

            ConvenerPlayerComponent convenerComponent = ConvenerPlayerComponent.KEY.get(context.player());
            boolean gainedShield = convenerComponent.recordCompletedTask();
            convenerComponent.sync();
            if (gainedShield) {
                CompoundTag extra = new CompoundTag();
                extra.putInt("current_layers", convenerComponent.getCounterShieldLayers());
                GameRecordManager.recordGlobalEvent(
                        context.player().serverLevel(),
                        StupidExpressReplay.CONVENER_COUNTER_SHIELD_GAINED_EVENT,
                        context.player(),
                        extra
                );
            }
        });
    }

    public static Role registerRole(Role role) {
        WatheRoles.registerRole(role);
        ROLES.put(role.identifier().getPath(), role);
        return role;
    }

}
