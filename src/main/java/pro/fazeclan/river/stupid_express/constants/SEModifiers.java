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

    }

}
