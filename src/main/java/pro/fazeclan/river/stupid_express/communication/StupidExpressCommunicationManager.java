package pro.fazeclan.river.stupid_express.communication;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityCommunicationHelper;

/**
 * StupidExpress 的统一通讯限制管理器。
 *
 * <p>当前只接入双重人格普通轮换阶段：
 * 休眠人格只能和另一人格/非存活玩家沟通。
 *
 * <p>后续如果还要加入类似“沉默者、禁言者、只能死者听见”等扩展职业规则，
 * 可以继续在这里集中挂接，而不是分别去改 VoiceChat 插件和 Fabric 事件注册点。</p>
 */
public final class StupidExpressCommunicationManager {

    private StupidExpressCommunicationManager() {}

    /**
     * 注册所有服务端通讯限制事件。
     */
    public static void init() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(StupidExpressCommunicationManager::handleRestrictedChatMessage);
    }

    /**
     * 当前是否应该阻断某一对玩家之间的语音传递。
     */
    public static boolean shouldBlockVoiceBetween(ServerPlayer sender, ServerPlayer receiver) {
        return DualPersonalityCommunicationHelper.shouldBlockVoiceBetween(sender, receiver);
    }

    /**
     * 处理被限制玩家的普通聊天消息。
     *
     * <p>返回 {@code false} 时，Fabric 会取消原版的全服广播。
     * 我们随后手动把消息只发送给允许看到的人。</p>
     */
    private static boolean handleRestrictedChatMessage(
            PlayerChatMessage message,
            ServerPlayer sender,
            ChatType.Bound params
    ) {
        /*
         * 双重人格聊天先做 actionbar 桥接，再决定是否取消原版广播。
         * 如果发送者是休眠人格，后续由 redirectDormantChat 手动发给允许接收的人。
         */
        DualPersonalityCommunicationHelper.bridgeChatIfNeeded(message, sender);
        if (DualPersonalityCommunicationHelper.shouldRestrictChat(sender)) {
            DualPersonalityCommunicationHelper.redirectDormantChat(message, sender, params);
            return false;
        }

        return true;
    }
}
