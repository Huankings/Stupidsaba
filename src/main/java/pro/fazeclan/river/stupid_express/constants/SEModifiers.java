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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.packet.DualPersonalitySwitchC2SPacket;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.packet.DualPersonalitySwitchKeyLabelC2SPacket;
import pro.fazeclan.river.stupid_express.modifier.lovers.LoversPairComponent;

import java.util.List;

public class SEModifiers {
    private static final Logger LOGGER = LoggerFactory.getLogger(SEModifiers.class);

    //恋人(词条)
    public static Modifier LOVERS = HMLModifiers.registerModifier(new Modifier(
            StupidExpress.id("lovers"),
            0xf38aff,
            null,
            null,
            false,
            false
    ));

    //双重人格(词条)
    public static Modifier DUAL_PERSONALITY = HMLModifiers.registerModifier(new Modifier(
            StupidExpress.id("dual_personality"),
            DualPersonalityManager.COLOR,
            null,
            null,
            false,
            false
    ));

    public static void init() {

        assignModifierComponents();
        // 双重人格有服务端 tick、断线恢复和实体交互封锁，需要在词条初始化时一起注册。
        DualPersonalityManager.init();
        // 客户端 Y 键请求使用的 C2S 包也在这里注册服务端接收器。
        DualPersonalitySwitchC2SPacket.register();
        // 同步客户端当前按键显示文本，方便服务端 actionbar 直接显示“按下 U 键”之类的内容。
        DualPersonalitySwitchKeyLabelC2SPacket.register();

        /// LOVERS
        Harpymodloader.MODIFIER_MAX.put(StupidExpress.id("lovers"), 1);

        /// DUAL_PERSONALITY
        // 默认先不进随机池，开局分配前由 DualPersonalityAssignMixin 按配置和参局人数刷新。
        Harpymodloader.MODIFIER_MAX.put(StupidExpress.id("dual_personality"), 0);

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
            var worldModifierComponent = WorldModifierComponent.KEY.get(level);

            // 旧写法是不断 randomPlayer 直到抽中合法目标。
            // 但如果这局里根本不存在第二个可成为恋人的玩家，就会无限循环并触发服务器 watchdog。
            // 这里改成先筛出全部合法候选人，再从列表里随机选一个。
            List<ServerPlayer> candidates = level.players().stream()
                    .filter(candidate -> !candidate.equals(lover))
                    .filter(candidate -> gameWorldComponent.getRole(candidate) != null)
                    .filter(gameWorldComponent::isInnocent)
                    .filter(candidate -> !gameWorldComponent.isRole(candidate, WatheRoles.VIGILANTE))
                    .filter(candidate -> !worldModifierComponent.isModifier(candidate, LOVERS))
                    .toList();

            if (candidates.isEmpty()) {
                // 候选为空时直接安全跳过，不再让整局初始化卡死。
                LOGGER.warn("恋人词条分配已跳过：玩家 {} 在本局中没有可用的第二恋人候选者。", lover.getScoreboardName());
                return;
            }

            ServerPlayer loverTwo = candidates.get(level.random.nextInt(candidates.size()));

            // assign both lovers
            // 只给另一位补词条即可；当前触发事件的这位已经是第一位恋人。
            if (!worldModifierComponent.isModifier(loverTwo, LOVERS)) {
                worldModifierComponent.addModifier(loverTwo.getUUID(), LOVERS);
            }
            LoversPairComponent.KEY.get(level).setRandomPair(lover.getUUID(), loverTwo.getUUID());
        }));

        /// DUAL_PERSONALITY
        ModifierAssigned.EVENT.register(((player, modifier) -> {
            if (!modifier.equals(DUAL_PERSONALITY)) {
                return;
            }
            if (!(player instanceof ServerPlayer mainPersonality)) {
                return;
            }

            var level = (ServerLevel) mainPersonality.level();
            var gameWorldComponent = GameWorldComponent.KEY.get(level);
            var worldModifierComponent = WorldModifierComponent.KEY.get(level);

            /*
             * Harpy 随机抽到的这个玩家作为主人格。
             * 这里再从本局其它有职业、且尚未拥有双重人格词条的玩家中补一个副人格。
             * 不要求阵营相同，因为用户确认双重人格可以和各种职业/词条叠加。
             */
            List<ServerPlayer> candidates = level.players().stream()
                    .filter(candidate -> !candidate.equals(mainPersonality))
                    .filter(candidate -> gameWorldComponent.getRole(candidate) != null)
                    .filter(candidate -> !worldModifierComponent.isModifier(candidate, DUAL_PERSONALITY))
                    .toList();

            if (candidates.isEmpty()) {
                LOGGER.warn("双重人格词条分配已跳过：玩家 {} 在本局中没有可用的副人格候选者。", mainPersonality.getScoreboardName());
                return;
            }

            ServerPlayer subPersonality = candidates.get(level.random.nextInt(candidates.size()));
            if (!worldModifierComponent.isModifier(subPersonality, DUAL_PERSONALITY)) {
                // 副人格也要补 Harpy 词条，否则 HUD/胜利判定只会认主人格。
                worldModifierComponent.addModifier(subPersonality.getUUID(), DUAL_PERSONALITY);
            }
            // 世界组件保存真正的主副关系和初始 active/dormant 状态。
            DualPersonalityComponent.KEY.get(level).setRandomPair(mainPersonality.getUUID(), subPersonality.getUUID());
        }));

    }

}
