package pro.fazeclan.river.stupid_express.client.mixin.role.convener;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.cca.AbilityCooldownComponent;
import pro.fazeclan.river.stupid_express.client.StupidExpressClient;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerDisguiseResolver;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerPlayerComponent;

import java.util.UUID;

@Mixin(RoleNameRenderer.class)
public class ConvenerHudMixin {

    @Inject(method = "renderHud", at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/game/GameFunctions;isPlayerSpectatingOrCreative(Lnet/minecraft/world/entity/player/Player;)Z"))
    private static void stupidexpress$raycastBody(Font renderer, LocalPlayer player, GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        // 召集者与死灵法师一样依赖“准星正在对着哪具尸体”。
        // 这里提前做一次尸体射线检测，供后面的提示文字和交互逻辑复用。
        float range = GameFunctions.isPlayerSpectatingOrCreative(player) ? 8.0F : 2.0F;
        HitResult line = ProjectileUtil.getHitResultOnViewVector(player, entity -> entity instanceof PlayerBodyEntity, range);
        StupidExpressClient.targetBody = null;
        if (!(line instanceof EntityHitResult hitResult)) {
            return;
        }
        if (hitResult.getEntity() instanceof PlayerBodyEntity body) {
            StupidExpressClient.targetBody = body;
        }
    }

    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void stupidexpress$renderConvenerHud(Font renderer, LocalPlayer player, GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(client.player.level());
        if (!gameWorldComponent.isRole(client.player, SERoles.CONVENER)) {
            return;
        }

        if (!WatheClient.isPlayerSpectatingOrCreative()) {
            renderCrosshairPrompt(renderer, client.player, context);
        }

        renderStatusLines(renderer, client.player, context);
    }

    private static void renderCrosshairPrompt(Font renderer, LocalPlayer player, GuiGraphics context) {
        if (StupidExpressClient.targetBody == null) {
            return;
        }

        // 冷却中显示剩余秒数；冷却结束后显示可交互提示。
        AbilityCooldownComponent cooldownComponent = AbilityCooldownComponent.KEY.get(player);
        Component status = cooldownComponent.hasCooldown()
                ? Component.translatable("hud.stupid_express.convener.cooldown", cooldownComponent.getCooldown() / 20)
                : Component.translatable("hud.stupid_express.convener.select_body");

        context.pose().pushPose();
        context.pose().translate(context.guiWidth() / 2.0f, context.guiHeight() / 2.0f + 6.0f, 0.0f);
        context.pose().scale(0.6f, 0.6f, 1.0f);
        context.drawString(renderer, status, -renderer.width(status) / 2, 32, SERoles.CONVENER.color());
        context.pose().popPose();
    }

    private static void renderStatusLines(Font renderer, LocalPlayer player, GuiGraphics context) {
        ConvenerPlayerComponent convenerComponent = ConvenerPlayerComponent.KEY.get(player);
        ConvenerDisguiseComponent disguiseComponent = ConvenerDisguiseComponent.KEY.get(player);
        boolean counterShieldEnabled = StupidExpress.CONFIG.rolesSection.convenerSection.convenerCounterShieldEnabled;

        // 若尚未解锁任何他人头像，则明确告诉玩家“变形功能还没解锁”。
        Component disguiseLine = convenerComponent.hasUnlockedMorphs()
                ? Component.translatable("hud.stupid_express.convener.current_disguise", resolveHudDisguiseName(player, disguiseComponent.getDisguiseUuid()))
                : Component.translatable("hud.stupid_express.convener.locked");

        // 护盾功能关闭时，右下角只保留原本真正需要的两行状态。
        Component shieldLine = null;
        Component taskLine = null;
        if (counterShieldEnabled) {
            shieldLine = Component.translatable(
                    "hud.stupid_express.convener.counter_shield_layers",
                    convenerComponent.getCounterShieldLayers()
            );
            taskLine = Component.translatable(
                    "hud.stupid_express.convener.tasks_to_next_shield",
                    convenerComponent.getTasksRemainingForNextShield()
            );
        }

        // 召集胜利进度始终保留，让玩家随时知道距离单独阵营结算还差多少次。
        Component summonLine = Component.translatable(
                "hud.stupid_express.convener.progress",
                convenerComponent.getSummonCount(),
                convenerComponent.getRequiredSummons()
        );

        int baseX = context.guiWidth() - 8;
        int lineSpacing = renderer.lineHeight + 2;

        if (!counterShieldEnabled) {
            int summonLineY = context.guiHeight() - 10;
            int disguiseLineY = summonLineY - lineSpacing;

            context.drawString(renderer, disguiseLine, baseX - renderer.width(disguiseLine), disguiseLineY, SERoles.CONVENER.color(), true);
            context.drawString(renderer, summonLine, baseX - renderer.width(summonLine), summonLineY, SERoles.CONVENER.color(), true);
            return;
        }

        int taskLineY = context.guiHeight() - 10;
        int summonLineY = taskLineY - lineSpacing;
        int disguiseLineY = summonLineY - lineSpacing;
        int shieldLineY = disguiseLineY - lineSpacing;

        context.drawString(renderer, shieldLine, baseX - renderer.width(shieldLine), shieldLineY, SERoles.CONVENER.color(), true);
        context.drawString(renderer, disguiseLine, baseX - renderer.width(disguiseLine), disguiseLineY, SERoles.CONVENER.color(), true);
        context.drawString(renderer, summonLine, baseX - renderer.width(summonLine), summonLineY, SERoles.CONVENER.color(), true);
        context.drawString(renderer, taskLine, baseX - renderer.width(taskLine), taskLineY, SERoles.CONVENER.color(), true);
    }

    private static Component resolveHudDisguiseName(LocalPlayer player, UUID disguiseUuid) {
        Component disguiseName = ConvenerDisguiseResolver.resolveDisguiseName(player, disguiseUuid);
        return disguiseName != null ? disguiseName : Component.translatable("hud.stupid_express.convener.waiting");
    }
}
