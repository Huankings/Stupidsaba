package pro.fazeclan.river.stupid_express.client.role.convener;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;
import pro.fazeclan.river.stupid_express.role.convener.packet.ConvenerMorphC2SPacket;

import java.util.UUID;

/**
 * 召集者背包里的头像按钮。
 *
 * <p>按钮只负责客户端展示与点击发包，真正的可选性校验仍由服务端完成。</p>
 */
public class ConvenerDisguiseButton extends Button {

    private static final int SLOT_HIGHLIGHT = 0x90FFBF49;
    private static final int SELF_BORDER = 0xC0F2B95B;
    private static final int CURRENT_BORDER = 0xD05734E5;

    private final LimitedInventoryScreen screen;
    private final UUID targetUuid;
    private final boolean isSelf;
    private final @Nullable PlayerInfo playerInfo;
    private final @Nullable AbstractClientPlayer livePlayer;

    public ConvenerDisguiseButton(
            LimitedInventoryScreen screen,
            int x,
            int y,
            UUID targetUuid,
            boolean isSelf,
            @Nullable PlayerInfo playerInfo,
            @Nullable AbstractClientPlayer livePlayer
    ) {
        super(
                x,
                y,
                16,
                16,
                Component.empty(),
                button -> ClientPlayNetworking.send(new ConvenerMorphC2SPacket(targetUuid)),
                DEFAULT_NARRATION
        );
        this.screen = screen;
        this.targetUuid = targetUuid;
        this.isSelf = isSelf;
        this.playerInfo = playerInfo;
        this.livePlayer = livePlayer;
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        ConvenerDisguiseComponent disguiseComponent = ConvenerDisguiseComponent.KEY.get(client.player);
        boolean selected = disguiseComponent.isDisguised() && this.targetUuid.equals(disguiseComponent.getDisguiseUuid());

        // 底座直接沿用现有商店槽位风格：
        // 1. 自己头像使用 TOOL 图标，表示“点自己=解除变形”；
        // 2. 其他头像使用 POISON 图标，表示“点别人=切换伪装目标”。
        context.blitSprite(
                this.isSelf ? ShopEntry.Type.TOOL.getTexture() : ShopEntry.Type.POISON.getTexture(),
                this.getX() - 7,
                this.getY() - 7,
                30,
                30
        );

        // 头像优先使用玩家列表里的皮肤缓存。
        // 如果目标玩家当前正好也在客户端世界里，则退回实时玩家实体皮肤。
        if (this.playerInfo != null && this.playerInfo.getSkin() != null) {
            PlayerFaceRenderer.draw(context, this.playerInfo.getSkin(), this.getX(), this.getY(), 16);
        } else if (this.livePlayer != null && this.livePlayer.getSkin() != null) {
            PlayerFaceRenderer.draw(context, this.livePlayer.getSkin(), this.getX(), this.getY(), 16);
        }

        if (this.isHovered()) {
            drawSlotHighlight(context, this.getX(), this.getY(), 0);
            Font font = client.font;
            Component hoverText = getDisplayName();
            context.renderTooltip(font, hoverText, this.getX() - 4 - font.width(hoverText) / 2, this.getY() - 10);
        }

        if (this.isSelf) {
            drawBorder(context, SELF_BORDER);
        }
        if (selected) {
            drawBorder(context, CURRENT_BORDER);
        }
    }

    private Component getDisplayName() {
        if (Minecraft.getInstance().player != null) {
            Component resolved = ConvenerDisguiseResolver.resolveDisguiseName(Minecraft.getInstance().player, this.targetUuid);
            if (resolved != null) {
                return resolved;
            }
        }
        return Component.literal(this.targetUuid.toString());
    }

    private void drawBorder(GuiGraphics context, int color) {
        int x = this.getX();
        int y = this.getY();
        context.fill(x - 2, y - 2, x + 18, y, color);
        context.fill(x - 2, y + 16, x + 18, y + 18, color);
        context.fill(x - 2, y - 2, x, y + 18, color);
        context.fill(x + 16, y - 2, x + 18, y + 18, color);
    }

    private void drawSlotHighlight(GuiGraphics context, int x, int y, int z) {
        context.fillGradient(RenderType.guiOverlay(), x, y, x + 16, y + 14, SLOT_HIGHLIGHT, SLOT_HIGHLIGHT, z);
        context.fillGradient(RenderType.guiOverlay(), x, y + 14, x + 15, y + 15, SLOT_HIGHLIGHT, SLOT_HIGHLIGHT, z);
        context.fillGradient(RenderType.guiOverlay(), x, y + 15, x + 14, y + 16, SLOT_HIGHLIGHT, SLOT_HIGHLIGHT, z);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // 这里先留空：
        // 召集者头像栏当前只作为鼠标点击辅助界面，不额外生成旁白文案。
    }
}
