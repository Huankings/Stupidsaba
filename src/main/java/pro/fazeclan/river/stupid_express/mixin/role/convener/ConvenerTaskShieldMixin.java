package pro.fazeclan.river.stupid_express.mixin.role.convener;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.TaskCompletePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.record.StupidExpressReplay;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerPlayerComponent;

/**
 * 监听 Wathe 真正的“任务完成”同步包，给召集者累计反伤护盾进度。
 *
 * <p>这里特地参考 KinsWathe 的 TaskCompleteIncomeMixin，
 * 直接监听 ServerPlayNetworking.send(...) 发出的 TaskCompletePayload，
 * 而不是去硬混 PlayerMoodComponent 的内部实现。
 *
 * <p>这样做的好处是：
 * 1. 能同时兼容原版 Wathe 与你本地这份做过调整的 Wathe；
 * 2. 只会在任务真正完成时触发，不会误判普通心情变化；
 * 3. 后续 Wathe 内部如果继续重构任务系统，这里的稳定性也更高。
 */
@Mixin(ServerPlayNetworking.class)
public abstract class ConvenerTaskShieldMixin {

    @Inject(
            method = "send(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
            at = @At("HEAD")
    )
    private static void stupidexpress$trackConvenerCompletedTasks(ServerPlayer player, CustomPacketPayload payload, CallbackInfo ci) {
        if (!(payload instanceof TaskCompletePayload)) {
            return;
        }
        if (!StupidExpress.CONFIG.rolesSection.convenerSection.convenerCounterShieldEnabled) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
        if (!gameWorldComponent.isRunning()) {
            return;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }
        if (!gameWorldComponent.isRole(player, SERoles.CONVENER)) {
            return;
        }

        // 召集者每完成一次真实任务都累计一次；
        // 凑满固定任务数后自动折算成 1 层护盾。
        ConvenerPlayerComponent convenerComponent = ConvenerPlayerComponent.KEY.get(player);
        boolean gainedShield = convenerComponent.recordCompletedTask();
        convenerComponent.sync();
        if (gainedShield) {
            /*
             * 护盾获取事件只在“这次任务刚好凑满一层”时记录，
             * 不在每个任务完成时都重复刷屏。
             */
            CompoundTag extra = new CompoundTag();
            extra.putInt("current_layers", convenerComponent.getCounterShieldLayers());
            GameRecordManager.recordGlobalEvent(player.serverLevel(), StupidExpressReplay.CONVENER_COUNTER_SHIELD_GAINED_EVENT, player, extra);
        }
    }
}
