package pro.fazeclan.river.stupid_express.modifier.dual_personality;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 双重人格普通轮换阶段的聊天和语音辅助逻辑。
 *
 * <p>休眠人格玩法上不是死人，但也不能像普通活人一样向全场发声。
 * 因此这里把“休眠人格与活跃人格之间的私密沟通”和“对其他活人隐藏休眠人格消息”集中处理。</p>
 */
public final class DualPersonalityCommunicationHelper {

    private DualPersonalityCommunicationHelper() {
    }

    public static boolean shouldRestrictChat(ServerPlayer sender) {
        // 只有普通轮换阶段的休眠人格需要限制聊天；双活时两个人都恢复正常聊天。
        DualPersonalityComponent.PairState pair = getRotatingPair(sender);
        return pair != null && pair.isDormant(sender.getUUID());
    }

    public static void bridgeChatIfNeeded(PlayerChatMessage message, ServerPlayer sender) {
        /*
         * 无论发送者当前是活跃还是休眠，只要仍处于普通轮换阶段，
         * 另一人格都会在 actionbar 收到一份简短转发，方便两个人格低干扰沟通。
         */
        DualPersonalityComponent.PairState pair = getRotatingPair(sender);
        if (pair == null) {
            return;
        }

        ServerPlayer partner = getPartnerOnline(sender, pair);
        if (partner == null) {
            return;
        }

        String rawContent = message.signedContent();
        if (rawContent == null || rawContent.isBlank()) {
            return;
        }

        DualPersonalityManager.sendActionbar(
                partner,
                Component.translatable("message.stupid_express.dual_personality.chat_bridge", sender.getGameProfile().getName(), rawContent)
        );
    }

    public static void redirectDormantChat(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound params) {
        /*
         * Fabric 事件取消原版全服广播后，休眠人格的聊天由这里手动转发：
         * - 自己不用再收一遍；
         * - 活跃人格可以收到；
         * - 死亡/旁观玩家可以收到；
         * - 其他仍存活玩家收不到，避免休眠人格泄露信息。
         */
        OutgoingChatMessage outgoing = OutgoingChatMessage.create(message);
        DualPersonalityComponent.PairState pair = getRotatingPair(sender);
        if (pair == null) {
            return;
        }

        UUID activeUuid = pair.active;
        for (ServerPlayer recipient : sender.server.getPlayerList().getPlayers()) {
            if (recipient == sender) {
                continue;
            }
            if (!recipient.getUUID().equals(activeUuid) && GameFunctions.isPlayerAliveAndSurvival(recipient)) {
                continue;
            }
            recipient.sendChatMessage(outgoing, sender.shouldFilterMessageTo(recipient), params);
        }
    }

    public static boolean shouldBlockVoiceBetween(ServerPlayer sender, ServerPlayer receiver) {
        DualPersonalityComponent.PairState senderPair = getRotatingPair(sender);
        if (senderPair != null && senderPair.isDormant(sender.getUUID())) {
            /*
             * 休眠人格的语音只给另一人格和非存活玩家。
             * 另一人格会由静态语音包转发，这里把原始空间语音取消掉，避免重声和距离衰减。
             */
            if (receiver.getUUID().equals(senderPair.active)) {
                return true;
            }
            return GameFunctions.isPlayerAliveAndSurvival(receiver);
        }

        DualPersonalityComponent.PairState receiverPair = getRotatingPair(receiver);
        if (receiverPair != null && receiverPair.isDormant(receiver.getUUID())) {
            /*
             * 休眠人格不能听普通旁观者，也不能听其他活人。
             * 如果发送者是自己的活跃人格，也同样取消原始空间语音，改由静态包保证无衰减。
             */
            return true;
        }

        return false;
    }

    public static @Nullable ServerPlayer getStaticVoiceRecipient(ServerPlayer sender) {
        /*
         * Simple Voice Chat 的普通语音包带距离衰减。
         * 双重人格之间需要像“脑内沟通”一样无视距离，所以插件层会给这里返回的另一人格补发静态语音包。
         */
        DualPersonalityComponent.PairState pair = getRotatingPair(sender);
        if (pair == null) {
            return null;
        }
        return getPartnerOnline(sender, pair);
    }

    private static @Nullable DualPersonalityComponent.PairState getRotatingPair(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        if (!DualPersonalityManager.isActiveRound(player.level())) {
            /*
             * 结算 STOPPING 阶段不再保留双重人格脑内通信。
             * 这能避免旧配对组件还没完全同步清除时，聊天/语音仍被当作休眠人格处理。
             */
            return null;
        }
        DualPersonalityComponent.PairState pair = DualPersonalityComponent.KEY.get(player.level()).getPair(player.getUUID());
        if (pair == null || pair.doubleActive) {
            // 双活阶段不再限制通信，直接走原版/VoiceChat 原流程。
            return null;
        }
        return pair;
    }

    private static @Nullable ServerPlayer getPartnerOnline(ServerPlayer sender, DualPersonalityComponent.PairState pair) {
        UUID partnerUuid = pair.getPartner(sender.getUUID());
        return partnerUuid == null ? null : sender.server.getPlayerList().getPlayer(partnerUuid);
    }
}
