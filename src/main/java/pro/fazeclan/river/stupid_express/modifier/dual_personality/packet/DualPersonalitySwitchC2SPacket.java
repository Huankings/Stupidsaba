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
 * 客户端按下人格切换键时发给服务端的空包。
 *
 * <p>包里不携带目标人格或倒计时信息，是为了避免客户端伪造状态。
 * 服务端收到后只把它当作“玩家请求提前切换”，最终是否允许由 DualPersonalityManager 判断。</p>
 */
public record DualPersonalitySwitchC2SPacket() implements CustomPacketPayload {

    public static final Type<DualPersonalitySwitchC2SPacket> ID =
            new Type<>(StupidExpress.id("dual_personality_switch"));

    public static final StreamCodec<FriendlyByteBuf, DualPersonalitySwitchC2SPacket> CODEC = StreamCodec.of(
            (buf, value) -> {
                // 空包：按键事件本身就是全部信息。
            },
            buf -> new DualPersonalitySwitchC2SPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, DualPersonalitySwitchC2SPacket::handle);
    }

    private static void handle(DualPersonalitySwitchC2SPacket payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        // 切回主线程处理世界组件，避免网络线程直接改游戏状态。
        context.server().execute(() -> DualPersonalityManager.requestEarlySwitch(player));
    }
}
