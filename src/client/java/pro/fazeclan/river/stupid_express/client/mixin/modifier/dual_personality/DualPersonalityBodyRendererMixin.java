package pro.fazeclan.river.stupid_express.client.mixin.modifier.dual_personality;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityBodyRenderHelper;

@Mixin(value = PlayerBodyEntityRenderer.class, priority = 1100)
public abstract class DualPersonalityBodyRendererMixin extends LivingEntityRenderer<PlayerBodyEntity, PlayerModel<PlayerBodyEntity>> {

    protected DualPersonalityBodyRendererMixin(
            EntityRendererProvider.Context context,
            PlayerModel<PlayerBodyEntity> model,
            float shadowRadius
    ) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void stupidexpress$cacheBodyModels(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        // Wathe 尸体渲染器按 owner 的 slim/classic 创建；副人格尸体显示主人格时需要能动态切模型。
        DualPersonalityBodyRenderHelper.initializeBodyModels(context);
    }

    @Inject(
            method = "renderBody(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
            at = @At("HEAD")
    )
    private void stupidexpress$applyDualPersonalityBodyModel(
            PlayerBodyEntity body,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float alpha,
            CallbackInfo ci
    ) {
        PlayerSkin skin = DualPersonalityBodyRenderHelper.resolveDualPersonalityBodySkin(body);
        PlayerModel<PlayerBodyEntity> resolvedModel = DualPersonalityBodyRenderHelper.resolveBodyModel(skin);
        if (resolvedModel != null) {
            this.model = resolvedModel;
        }
    }

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void stupidexpress$useMainPersonalityBodyTexture(
            PlayerBodyEntity body,
            CallbackInfoReturnable<ResourceLocation> cir
    ) {
        /*
         * 只在“尸体 owner 是副人格，并且本局组件还保留主副关系”时接管贴图。
         * 其它尸体继续交给 Wathe / NoellesRoles 原本的尸体皮肤解析逻辑。
         */
        ResourceLocation texture = DualPersonalityBodyRenderHelper.resolveDualPersonalityBodyTexture(body);
        if (texture != null) {
            cir.setReturnValue(texture);
        }
    }
}
