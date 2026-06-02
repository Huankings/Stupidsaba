package pro.fazeclan.river.stupid_express.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import net.minecraft.server.level.ServerPlayer;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.communication.StupidExpressCommunicationManager;

/**
 * StupidExpress 的语音聊天插件入口。
 *
 * <p>当前先承接“召集者召集后的限时变形活人彼此听不到彼此”这条规则，
 * 后续如果还要加新的角色语音限制，也可以继续在这里统一追加。</p>
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
     * <p>这里不再走“麦克风包到服务端就整包取消”的旧方案，
     * 因为那样会直接导致被召集活人完全说不出话。
     *
     * <p>现在改为只在“发送者和接收者都处于被召集后的限时变形”时，
     * 取消这一对玩家之间的语音转发。
     * 这样可以实现：
     * 1. 被召集活人仍然可以正常开麦；
     * 2. 其他同样被召集的活人听不到；
     * 3. 召集者、死亡玩家、局外玩家依旧可以听到。</p>
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
        VoicechatPlugin.super.registerEvents(registration);
    }
}
