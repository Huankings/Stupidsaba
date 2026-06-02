package pro.fazeclan.river.stupid_express.client.ui.common;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 玩家头像分页按钮。
 *
 * <p>这里故意不复用任何“玩家头像按钮”类，而是单独做一个翻页控件。
 * 这样点击上一页/下一页时，只会触发翻页回调，不会误走职业本身的选人逻辑。
 * 对召集者而言，这一点尤其重要，因为翻页按钮绝不能被当成“切换伪装目标”。</p>
 */
public class PlayerPageSwitchWidget extends Button {

    private static final int SLOT_HIGHLIGHT = 0x90FFBF49;

    /**
     * 按钮中心渲染的物品图标。
     *
     * <p>左侧会放紫色染料，右侧会放黄绿色染料，直接借物品图标来提示翻页方向。</p>
     */
    private final ItemStack iconStack;

    /**
     * 鼠标悬停时显示的文本。
     *
     * <p>文本本身走语言文件，方便后续统一本地化。</p>
     */
    private final Component tooltipText;

    public PlayerPageSwitchWidget(int x, int y, ItemStack iconStack, Component tooltipText, OnPress onPress) {
        super(x, y, 16, 16, tooltipText, onPress, DEFAULT_NARRATION);
        this.iconStack = iconStack;
        this.tooltipText = tooltipText;
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        // 底座继续复用 Wathe 现成的商店槽位风格，
        // 这样分页按钮能自然融进原本的背包头像界面里，不会显得过于突兀。
        context.blitSprite(ShopEntry.Type.TOOL.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
        context.renderItem(this.iconStack, this.getX(), this.getY());

        if (this.isHovered()) {
            drawHighlight(context);
            context.renderTooltip(Minecraft.getInstance().font, this.tooltipText, mouseX, mouseY);
        }
    }

    private void drawHighlight(GuiGraphics context) {
        int x = this.getX();
        int y = this.getY();
        context.fillGradient(RenderType.guiOverlay(), x, y, x + 16, y + 14, SLOT_HIGHLIGHT, SLOT_HIGHLIGHT, 0);
        context.fillGradient(RenderType.guiOverlay(), x, y + 14, x + 15, y + 15, SLOT_HIGHLIGHT, SLOT_HIGHLIGHT, 0);
        context.fillGradient(RenderType.guiOverlay(), x, y + 15, x + 14, y + 16, SLOT_HIGHLIGHT, SLOT_HIGHLIGHT, 0);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // 当前分页按钮主要服务于鼠标点击界面，
        // 因此这里暂时不额外拼接旁白内容，避免产生重复播报。
    }
}
