package pro.fazeclan.river.stupid_express.client.mixin.role.convener;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerDisguiseRenderHelper;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerDisguiseResolver;

public final class ConvenerPlayerRendererMixin {

    private ConvenerPlayerRendererMixin() {}

    @Mixin(PlayerRenderer.class)
    public abstract static class SkinMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

        protected SkinMixin(EntityRendererProvider.Context context,
                            PlayerModel<AbstractClientPlayer> model,
                            float shadowRadius) {
            super(context, model, shadowRadius);
        }

        @Inject(method = "<init>", at = @At("TAIL"))
        private void stupidexpress$cachePlayerModels(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
            // 只补充 slim/classic 两套标准玩家模型缓存，不改原版渲染器初始化流程。
            ConvenerDisguiseRenderHelper.initializePlayerModels(context);
        }

        @Inject(
                method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                at = @At("HEAD")
        )
        private void stupidexpress$applyDisguiseModel(AbstractClientPlayer player,
                                                      float entityYaw,
                                                      float partialTick,
                                                      PoseStack poseStack,
                                                      MultiBufferSource bufferSource,
                                                      int packedLight,
                                                      CallbackInfo ci) {
            stupidexpress$applyResolvedPlayerModel(player);
        }

        @Inject(method = "renderRightHand", at = @At("HEAD"))
        private void stupidexpress$applyRightHandModel(PoseStack poseStack,
                                                       MultiBufferSource bufferSource,
                                                       int packedLight,
                                                       AbstractClientPlayer player,
                                                       CallbackInfo ci) {
            // 第一人称手臂渲染读取的是 renderer.model 内部手臂部件，
            // 所以这里也要在渲染前同步切换 slim/classic 模型。
            stupidexpress$applyResolvedPlayerModel(player);
        }

        @Inject(method = "renderLeftHand", at = @At("HEAD"))
        private void stupidexpress$applyLeftHandModel(PoseStack poseStack,
                                                      MultiBufferSource bufferSource,
                                                      int packedLight,
                                                      AbstractClientPlayer player,
                                                      CallbackInfo ci) {
            stupidexpress$applyResolvedPlayerModel(player);
        }

        @Inject(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
        private void stupidexpress$replaceSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
            // 第三人称本体皮肤直接改这里。
            PlayerSkin disguiseSkin = ConvenerDisguiseResolver.resolveDisguiseSkin(player);
            if (disguiseSkin != null) {
                cir.setReturnValue(disguiseSkin.texture());
            }
        }

        @WrapOperation(
                method = "renderHand",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSkin()Lnet/minecraft/client/resources/PlayerSkin;"
                )
        )
        private PlayerSkin stupidexpress$replaceFirstPersonSkin(AbstractClientPlayer player, Operation<PlayerSkin> original) {
            // 第一人称手臂不会走 getTextureLocation，而是直接取 getSkin()。
            // 所以这里要单独包一次，确保召集者自己视角也能看到变形结果。
            PlayerSkin disguiseSkin = ConvenerDisguiseResolver.resolveDisguiseSkin(player);
            return disguiseSkin != null ? disguiseSkin : original.call(player);
        }

        @Unique
        private void stupidexpress$applyResolvedPlayerModel(AbstractClientPlayer player) {
            // 这一条解析链同时覆盖两种情况：
            // 1. 召集期间，所有活人被强制变形成尸体原主；
            // 2. 召集者自己在解锁后，主动切换到某个已解锁尸体皮肤。
            PlayerSkin displaySkin = ConvenerDisguiseResolver.resolveDisguiseSkin(player);
            if (displaySkin == null) {
                // 没在伪装时必须回退到玩家自己的原始皮肤模型，
                // 避免上一帧 slim/classic 状态残留到下一次正常渲染。
                displaySkin = player.getSkin();
            }

            PlayerModel<AbstractClientPlayer> resolvedModel =
                    ConvenerDisguiseRenderHelper.resolvePlayerModel(displaySkin);
            if (resolvedModel != null) {
                this.model = resolvedModel;
            }
        }
    }

    @Mixin(CapeLayer.class)
    public static class CapeMixin {

        @WrapOperation(
                method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSkin()Lnet/minecraft/client/resources/PlayerSkin;"
                )
        )
        private PlayerSkin stupidexpress$replaceCapeSkin(AbstractClientPlayer player, Operation<PlayerSkin> original) {
            // 披风同样从 PlayerSkin 里取 capeTexture，因此也要替换成目标皮肤。
            PlayerSkin disguiseSkin = ConvenerDisguiseResolver.resolveDisguiseSkin(player);
            return disguiseSkin != null ? disguiseSkin : original.call(player);
        }
    }
}
