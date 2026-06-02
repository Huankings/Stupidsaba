package pro.fazeclan.river.stupid_express.role.convener.packet;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerPlayerComponent;

import java.util.UUID;

/**
 * 召集者背包头像选择数据包。
 * 只负责“选择已解锁头像”或“点击自己头像解除变形”两种行为。
 */
public record ConvenerMorphC2SPacket(UUID targetUuid) implements CustomPacketPayload {

    public static final Type<ConvenerMorphC2SPacket> ID = new Type<>(StupidExpress.id("convener_morph"));

    public static final StreamCodec<FriendlyByteBuf, ConvenerMorphC2SPacket> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeUUID(value.targetUuid),
            buf -> new ConvenerMorphC2SPacket(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, ConvenerMorphC2SPacket::handle);
    }

    private static void handle(ConvenerMorphC2SPacket payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
            if (!gameWorldComponent.isRole(player, SERoles.CONVENER)) {
                return;
            }

            ConvenerDisguiseComponent disguiseComponent = ConvenerDisguiseComponent.KEY.get(player);

            // 点自己的头像等价于“立即解除当前变形”，不需要任何冷却。
            if (payload.targetUuid().equals(player.getUUID())) {
                disguiseComponent.clearDisguise();
                return;
            }

            ConvenerPlayerComponent convenerComponent = ConvenerPlayerComponent.KEY.get(player);
            if (!convenerComponent.knowsDisguise(payload.targetUuid())) {
                return;
            }

            disguiseComponent.setPersistentDisguise(payload.targetUuid());
        });
    }
}
