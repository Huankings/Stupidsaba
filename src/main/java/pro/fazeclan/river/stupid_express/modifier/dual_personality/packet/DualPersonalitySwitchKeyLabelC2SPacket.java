package pro.fazeclan.river.stupid_express.modifier.dual_personality.packet;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager;

/**
 * 客户端同步“双重人格切换键当前显示文本”的数据包。
 *
 * <p>这个包不传功能名，也不传翻译 key，而是直接传客户端当前绑定后的可见文本，
 * 例如 U、1、鼠标 4。这样服务器在发送 actionbar 时就能直接写成“按下 U 键”，
 * 而不会把“功能名称”再读一次，导致出现“按下双重人格切换键键”的重复提示。</p>
 */
public record DualPersonalitySwitchKeyLabelC2SPacket(String keyLabel) implements CustomPacketPayload {

    public static final Type<DualPersonalitySwitchKeyLabelC2SPacket> ID =
            new Type<>(StupidExpress.id("dual_personality_switch_key_label"));

    public static final StreamCodec<FriendlyByteBuf, DualPersonalitySwitchKeyLabelC2SPacket> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeUtf(value.keyLabel, 32),
            buf -> new DualPersonalitySwitchKeyLabelC2SPacket(buf.readUtf(32))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, DualPersonalitySwitchKeyLabelC2SPacket::handle);
    }

    private static void handle(DualPersonalitySwitchKeyLabelC2SPacket payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            DualPersonalityManager.updateSwitchKeyLabel(player.getUUID(), payload.keyLabel());
        });
    }
}
