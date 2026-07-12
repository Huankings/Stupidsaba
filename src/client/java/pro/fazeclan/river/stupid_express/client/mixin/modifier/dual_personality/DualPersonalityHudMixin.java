package pro.fazeclan.river.stupid_express.client.mixin.modifier.dual_personality;

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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;

import java.util.UUID;

@Mixin(RoleNameRenderer.class)
public abstract class DualPersonalityHudMixin {

    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void stupidexpress$renderDualPersonalityPartner(
            Font renderer,
            LocalPlayer player,
            GuiGraphics context,
            DeltaTracker tickCounter,
            CallbackInfo ci
    ) {
        /*
         * Wathe 的角色 HUD 已经渲染完成后，再在左下角追加“另一人格”提示。
         * 这里不改原 HUD 主流程，降低和其它职业/词条 HUD 的冲突概率。
         */
        var clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer == null
                || clientPlayer.connection == null
                || !DualPersonalityClientState.isActiveRound(clientPlayer)
                || !WatheClient.isPlayerAliveAndInSurvival()) {
            return;
        }

        WorldModifierComponent modifierComponent = WorldModifierComponent.KEY.get(clientPlayer.level());
        if (!modifierComponent.isModifier(clientPlayer, SEModifiers.DUAL_PERSONALITY)) {
            return;
        }

        DualPersonalityComponent dualComponent = DualPersonalityComponent.KEY.get(clientPlayer.level());
        UUID partnerUuid = dualComponent.getPartner(clientPlayer.getUUID());
        if (partnerUuid == null) {
            // 只有词条但组件里没有配对时不渲染，避免随机分配失败时显示错误信息。
            return;
        }

        var partnerInfo = clientPlayer.connection.getPlayerInfo(partnerUuid);
        if (partnerInfo == null || partnerInfo.getProfile() == null) {
            return;
        }

        int textY = context.guiHeight() - 12 - getExistingLowerLeftHudOffset(clientPlayer, modifierComponent);
        if (partnerInfo.getSkin() != null) {
            // 画头像能让玩家更快确认另一人格是谁，尤其多人局里比只显示名字更醒目。
            PlayerFaceRenderer.draw(context, partnerInfo.getSkin().texture(), 2, textY - 2, 12);
        }

        Component name = Component.translatable(
                "tip.stupid_express.dual_personality.partner",
                partnerInfo.getProfile().getName()
        );
        context.drawString(renderer, name, 18, textY, SEModifiers.DUAL_PERSONALITY.color());
    }

    private static int getExistingLowerLeftHudOffset(LocalPlayer player, WorldModifierComponent modifierComponent) {
        int offset = 0;
        var role = GameWorldComponent.KEY.get(player.level()).getRole(player);
        if (role != null && role.identifier().equals(ResourceLocation.parse("noellesroles:executioner"))) {
            // 仇杀客/处刑人类 HUD 也占左下角一行，双重人格向上避让。
            offset += 15;
        }
        if (modifierComponent.isModifier(player, SEModifiers.LOVERS)) {
            // 双重人格允许和恋人叠加，因此这里再给恋人提示留一行空间。
            offset += 15;
        }
        return offset;
    }
}
