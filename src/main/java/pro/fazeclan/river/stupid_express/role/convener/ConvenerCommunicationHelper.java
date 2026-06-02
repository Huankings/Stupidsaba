package pro.fazeclan.river.stupid_express.role.convener;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;

/**
 * 召集者通讯限制判定工具。
 *
 * <p>这里专门负责回答三个问题：
 * 1. 某个玩家现在是不是“被召集后的限时变形活人”；
 * 2. 两个玩家之间的语音现在应不应该互相隔离；
 * 3. 这个玩家现在应不应该被限制普通聊天的广播范围。
 *
 * <p>这样后续如果还要给别的职业加“沉默、封口、私聊隔离”等规则，
 * 可以继续沿着这套 helper 扩展，而不用把判断散落到 VoiceChat 插件、
 * Fabric 聊天事件和其它逻辑里到处复制。</p>
 */
public final class ConvenerCommunicationHelper {

    private ConvenerCommunicationHelper() {}

    /**
     * 判断玩家当前是否处于“被召集后的限时变形”状态。
     *
     * <p>这里明确排除召集者自己：
     * 召集者虽然也会在召集时变形，但其状态是无限时的 {@code -1}，
     * 而且需求里也明确说明了后续聊天可见者要包含召集者本人。</p>
     */
    public static boolean isTemporarilySummonedLivingPlayer(Player player) {
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return false;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
        if (gameWorldComponent.isRole(player, SERoles.CONVENER)) {
            return false;
        }

        return ConvenerDisguiseComponent.KEY.get(player).getMorphTicks() > 0;
    }

    /**
     * 服务端便捷重载。
     */
    public static boolean isTemporarilySummonedLivingPlayer(ServerPlayer player) {
        return isTemporarilySummonedLivingPlayer((Player) player);
    }

    /**
     * 判断两名玩家之间的语音是否需要互相隔离。
     *
     * <p>新规则不是“完全禁言”，而是“被召集的活人彼此听不到彼此”。
     * 所以这里要求发送者和接收者两边都处于召集后的限时变形中，
     * 才取消这一对语音转发。</p>
     */
    public static boolean shouldBlockVoiceBetween(ServerPlayer sender, ServerPlayer receiver) {
        return isTemporarilySummonedLivingPlayer(sender) && isTemporarilySummonedLivingPlayer(receiver);
    }

    /**
     * 当前是否需要限制该玩家的普通聊天广播范围。
     */
    public static boolean shouldRestrictChat(ServerPlayer player) {
        return isTemporarilySummonedLivingPlayer(player);
    }

    /**
     * 某个接收者是否可以收到“被召集活人”的受限聊天消息。
     *
     * <p>按需求，允许接收的人只有：
     * 1. 召集者本人；
     * 2. 非存活玩家；
     * 3. 不处于参局状态的玩家（例如没有被分配身份的旁观/局外玩家）。</p>
     */
    public static boolean canReceiveRestrictedChat(ServerPlayer recipient) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(recipient.level());
        if (gameWorldComponent.isRole(recipient, SERoles.CONVENER)) {
            return true;
        }

        if (!GameFunctions.isPlayerAliveAndSurvival(recipient)) {
            return true;
        }

        return gameWorldComponent.getRole(recipient) == null;
    }
}
