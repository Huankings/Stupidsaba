package pro.fazeclan.river.stupid_express.communication;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import pro.fazeclan.river.stupid_express.role.convener.ConvenerCommunicationHelper;

/**
 * StupidExpress 的统一通讯限制管理器。
 *
 * <p>当前先接入两类限制：
 * 1. 召集者召集后的“限时变形活人”语音互相隔离；
 * 2. 这些玩家的普通聊天改为“只对特定观察者可见”。
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
        return ConvenerCommunicationHelper.shouldBlockVoiceBetween(sender, receiver);
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
        if (!ConvenerCommunicationHelper.shouldRestrictChat(sender)) {
            return true;
        }

        redirectRestrictedChatMessage(message, sender, params);
        return false;
    }

    /**
     * 将聊天消息定向广播给允许接收的人。
     *
     * <p>这里尽量复用原版聊天发送链：
     * 仍然使用 {@link OutgoingChatMessage#create(PlayerChatMessage)} 和
     * {@link ServerPlayer#sendChatMessage(OutgoingChatMessage, boolean, ChatType.Bound)}，
     * 只是把接收者列表从“所有人”缩小到需求允许的范围。
     *
     * <p>这样比直接降级成 system message 更接近原版聊天表现，
     * 也能更自然地兼容客户端的聊天展示逻辑。</p>
     */
    private static void redirectRestrictedChatMessage(
            PlayerChatMessage message,
            ServerPlayer sender,
            ChatType.Bound params
    ) {
        OutgoingChatMessage outgoingChatMessage = OutgoingChatMessage.create(message);

        for (ServerPlayer recipient : sender.server.getPlayerList().getPlayers()) {
            if (recipient == sender) {
                continue;
            }

            if (!ConvenerCommunicationHelper.canReceiveRestrictedChat(recipient)) {
                continue;
            }

            // 沿用原版聊天过滤方向：以“发送者 -> 接收者”的关系决定是否需要过滤。
            recipient.sendChatMessage(
                    outgoingChatMessage,
                    sender.shouldFilterMessageTo(recipient),
                    params
            );
        }
    }
}
