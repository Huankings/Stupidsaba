package pro.fazeclan.river.stupid_express.client.mixin.modifier.lovers;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.StupidExpressClient;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;

import java.util.ArrayList;
import java.util.UUID;

@Mixin(RoleNameRenderer.class)
public abstract class LoversHudMixin {

    @Shadow
    private static float nametagAlpha;

    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void loversHud(Font renderer, LocalPlayer player, GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {

        var clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer == null || clientPlayer.connection == null) {
            // 修复说明：
            // 恋人 HUD 依赖客户端本地玩家和玩家列表网络连接。
            // 切世界、重连、观战切换视角的瞬间，这两个对象可能短暂为空；
            // 如果这里继续往下取恋人资料，就会直接在客户端渲染线程里空指针崩溃。
            return;
        }
        var clientWorld = clientPlayer.level();

        var component = WorldModifierComponent.KEY.get(clientPlayer.level());
        var config = StupidExpress.CONFIG;
        if (component.isModifier(clientPlayer, SEModifiers.LOVERS)
                && WatheClient.isPlayerAliveAndInSurvival()) {

            // 修复说明：
            // 这里显式复制一份恋人列表，不直接修改组件返回的集合。
            // 这样既能安全移除自己，也能避免组件如果返回内部集合时被 HUD 渲染代码误改。
            var lovers = new ArrayList<>(component.getAllWithModifier(SEModifiers.LOVERS));
            lovers.remove(clientPlayer.getUUID());

            var textYPos = context.guiHeight() - 12;
            var textXPos = 18;

            for (UUID uuid : lovers) {
                context.pose().pushPose();

                var loverInfo = clientPlayer.connection.getPlayerInfo(uuid);
                if (loverInfo == null || loverInfo.getProfile() == null) {
                    // 修复说明：
                    // 恋人双方的玩家资料不一定会和 HUD 渲染完全同步。
                    // 原代码这里一旦拿到空值，不是崩溃就是直接中断整段 HUD 渲染；
                    // 改成跳过当前这名尚未同步完成的恋人，等下一帧资料到齐后再显示。
                    context.pose().popPose();
                    continue;
                }

                Component name;
                if (!config.modifiersSection.loversSection.loversKnowImmediately) {
                    name = Component.translatable("hud.stupid_express.lovers.notification");
                    textXPos -= 14;
                } else {
                    name = Component.translatable("tip.stupid_express.lovers.partner", loverInfo.getProfile().getName());
                }

                var role = GameWorldComponent.KEY.get(clientWorld).getRole(clientPlayer);
                if (role != null) {
                    if (GameWorldComponent.KEY.get(clientWorld).getRole(clientPlayer).identifier().equals(ResourceLocation.parse("noellesroles:executioner"))) {
                        textYPos -= 15;
                    }
                }
                if (config.modifiersSection.loversSection.loversKnowImmediately) {
                    var loverSkin = loverInfo.getSkin();
                    // 修复说明：
                    // 某些情况下玩家资料先同步到了，但皮肤纹理还没准备好。
                    // 这里不强行绘制头像，避免“立即知晓恋人”模式在头像渲染阶段继续触发空指针。
                    if (loverSkin != null) {
                        PlayerFaceRenderer.draw(context, loverSkin.texture(), 2, textYPos - 2, 12);
                    }
                }
                context.drawString(renderer, name, textXPos, textYPos, SEModifiers.LOVERS.color());

                context.pose().popPose();
                textXPos += 14;
            }

        }
    }

    @Inject(
            method = "renderHud",
            at = @At("TAIL")
    )
    private static void renderLovers(Font renderer, LocalPlayer player, GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        var clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer == null) {
            // 修复说明：
            // 观战 HUD 也跑在客户端渲染阶段，本地玩家对象偶发为空时必须直接退出，
            // 否则下面读取世界和目标恋人时会连锁触发空指针。
            return;
        }
        var clientLevel = clientPlayer.level();
        var component = WorldModifierComponent.KEY.get(clientLevel);
        if (StupidExpressClient.target == null) {
            return;
        }
        if (!component.isModifier(StupidExpressClient.target, SEModifiers.LOVERS)) {
            return;
        }
        var config = StupidExpress.CONFIG;
        if (WatheClient.isPlayerAliveAndInSurvival()
                && !config.modifiersSection.loversSection.loversKnowImmediately
                && component.isModifier(clientPlayer, SEModifiers.LOVERS)) {
            stupidexpress$renderLoversHud(renderer, context, Component.translatable("hud.stupid_express.lovers.partner"));
        } else if (WatheClient.isPlayerSpectatingOrCreative()) {
            // 修复说明：
            // Extended 里的补丁本质上是在观战 HUD 发生空指针时直接吞掉异常。
            // 这里我们直接从源头修：如果观战目标的另一位恋人还没同步到当前客户端，
            // 就先跳过这一帧，不再执行 null.getName() 这种会导致崩端的调用。
            var lovers = new ArrayList<>(component.getAllWithModifier(SEModifiers.LOVERS));
            lovers.remove(StupidExpressClient.target.getUUID());
            for (UUID uuid : lovers) {
                var loverPlayer = clientLevel.getPlayerByUUID(uuid);
                if (loverPlayer == null) {
                    continue;
                }
                stupidexpress$renderLoversHud(renderer, context, Component.translatable(
                        "hud.stupid_express.lovers.in_love",
                        loverPlayer.getName()
                ));
            }
        }
    }

    @Unique
    private static void stupidexpress$renderLoversHud(Font renderer, GuiGraphics context, Component component) {

        context.pose().pushPose();
        context.pose().translate(context.guiWidth() / 2.0f, context.guiHeight() / 2.0f - 35.0f, 0.0f);
        context.pose().scale(0.6f, 0.6f, 1.0f);

        context.drawString(
                renderer,
                component,
                -renderer.width(component) / 2,
                32,
                SEModifiers.LOVERS.color() | (int) (nametagAlpha * 255.0F) << 24
        );

        context.pose().popPose();
    }

}
