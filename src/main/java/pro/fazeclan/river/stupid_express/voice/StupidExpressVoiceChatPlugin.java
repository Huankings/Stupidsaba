package pro.fazeclan.river.stupid_express.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.communication.StupidExpressCommunicationManager;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityCommunicationHelper;

/**
 * StupidExpress 的语音聊天插件入口。
 *
 * <p>当前承接双重人格普通轮换阶段的语音规则：
 * 两个人格之间额外补发无距离衰减的静态语音。</p>
 *
 * <p>后续如果还要加新的角色语音限制，也可以继续在这里统一追加。</p>
 */
public class StupidExpressVoiceChatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return StupidExpress.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        VoicechatPlugin.super.initialize(api);
    }

    /**
     * 过滤即将发送给单个接收者的语音包。
     *
     * <p>双重人格的休眠语音隔离统一由 StupidExpressCommunicationManager 判定，
     * 这样 VoiceChat 的不同声音包类型都能走同一套服务端规则。</p>
     */
    private void handleSoundPacket(SoundPacketEvent<?> event) {
        ServerPlayer sender = resolveServerPlayer(event.getSenderConnection());
        ServerPlayer receiver = resolveServerPlayer(event.getReceiverConnection());
        if (sender == null || receiver == null) {
            return;
        }

        if (StupidExpressCommunicationManager.shouldBlockVoiceBetween(sender, receiver)) {
            event.cancel();
        }
    }

    private void handleMicrophonePacket(MicrophonePacketEvent event) {
        /*
         * MicrophonePacketEvent 是“发送者刚把麦克风数据交给服务端”的时机。
         * 我们在这里拿到原始语音包，再定向补发给另一人格。
         */
        VoicechatServerApi api = event.getVoicechat();
        ServerPlayer sender = resolveServerPlayer(event.getSenderConnection());
        if (sender == null) {
            return;
        }

        ServerPlayer recipient = DualPersonalityCommunicationHelper.getStaticVoiceRecipient(sender);
        if (recipient == null) {
            return;
        }

        /*
         * 双重人格两个人格之间的语音必须无视距离衰减。
         * VoiceChat 的普通实体/位置语音仍带空间坐标，因此这里旁路再发一份静态语音包；
         * 原始空间包会在 handleSoundPacket 中对这对人格互相取消，避免听到双重声音。
         */
        VoicechatConnection connection = api.getConnectionOf(recipient.getUUID());
        if (connection != null) {
            StaticSoundPacket redirectedPacket = event.getPacket().staticSoundPacketBuilder().build();
            api.sendStaticSoundPacketTo(connection, redirectedPacket);
        }
    }

    /**
     * 从 VoiceChat 的连接对象里安全取出服务端玩家实例。
     *
     * <p>VoiceChat API 的连接对象外面包了一层抽象玩家，
     * 这里做一次统一拆包，避免每个事件处理器都重复写空判和类型判定。</p>
     */
    private ServerPlayer resolveServerPlayer(VoicechatConnection connection) {
        if (connection == null || connection.getPlayer() == null || connection.getPlayer().getPlayer() == null) {
            return null;
        }

        Object rawPlayer = connection.getPlayer().getPlayer();
        return rawPlayer instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // 就近语音、实体绑定语音、静态语音三类包都一起过滤，
        // 这样无论 VoiceChat 最终采用哪种声音包分发方式，都能保持同样的隔离规则。
        registration.registerEvent(LocationalSoundPacketEvent.class, this::handleSoundPacket);
        registration.registerEvent(EntitySoundPacketEvent.class, this::handleSoundPacket);
        registration.registerEvent(StaticSoundPacketEvent.class, this::handleSoundPacket);
        registration.registerEvent(MicrophonePacketEvent.class, this::handleMicrophonePacket);
        VoicechatPlugin.super.registerEvents(registration);
    }
}
