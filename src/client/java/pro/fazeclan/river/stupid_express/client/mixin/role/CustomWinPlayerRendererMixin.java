package pro.fazeclan.river.stupid_express.client.mixin.role;

import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts.RoleAnnouncementText;
import dev.doctor4t.wathe.client.gui.RoundTextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pro.fazeclan.river.stupid_express.cca.CustomWinnerComponent;

@Mixin(RoundTextRenderer.class)
public class CustomWinPlayerRendererMixin {

    private static final int OTHER_COLOR = 0x808080;

    private static final Component CIVILIAN_TITLE = RoleAnnouncementTexts.CIVILIAN.titleText;
    private static final Component VIGILANTE_TITLE = RoleAnnouncementTexts.VIGILANTE.titleText;
    private static final Component KILLER_TITLE = RoleAnnouncementTexts.KILLER.titleText;

    private static final RoleAnnouncementText OTHER_TEAM = RoleAnnouncementTexts.CIVILIAN;
    private static final RoleAnnouncementText WINNER_TEAM = RoleAnnouncementTexts.VIGILANTE;

    @Redirect(method = "renderHud",
            at = @At(value = "INVOKE", 
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"),
            remap = false)
    private static int redirectCategoryTitles(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int color) {
        
        if (!isCustomWin()) {
            return guiGraphics.drawString(font, text, x, y, color);
        }

        CustomWinnerComponent customWinnerComponent = getCustomWinnerComponent();

        if (text.getString().equals(CIVILIAN_TITLE.getString())) {
            return drawReplacementTitleCenteredOnOriginal(
                    guiGraphics,
                    font,
                    text,
                    Component.translatable("category.custom.stupid_express.other"),
                    x,
                    y,
                    OTHER_COLOR
            );
        }
        
        if (text.getString().equals(VIGILANTE_TITLE.getString())) {
            return drawReplacementTitleCenteredOnOriginal(
                    guiGraphics,
                    font,
                    text,
                    Component.translatable("announcement.role.stupid_express." + customWinnerComponent.getWinningTextId()),
                    x,
                    y,
                    customWinnerComponent.getColor()
            );
        }
        
        if (text.getString().equals(KILLER_TITLE.getString())) {
            return 0;
        }
        
        return guiGraphics.drawString(font, text, x, y, color);
    }

    private static int drawReplacementTitleCenteredOnOriginal(
            GuiGraphics guiGraphics,
            Font font,
            Component originalTitle,
            Component replacementTitle,
            int originalX,
            int y,
            int color
    ) {
        /*
         * Wathe 在进入这里前，已经按照原阵营标题的宽度算好了左上角 X。
         * 如果直接复用这个 X 去画“纵火犯 / 召集者 / 恋人”等更长或更短的文本，
         * 文本中心就会从玩家头像组中心偏出去，看起来像右侧独立阵营标题没有对齐。
         *
         * 因此这里先从原文本左上角反推出原阵营中心点，再用替换文本自己的宽度重新居中。
         * 头像排布仍然沿用 Wathe 的原始布局，只修正被替换后的标题绘制位置。
         */
        int originalCenterX = originalX + font.width(originalTitle) / 2;
        int replacementX = originalCenterX - font.width(replacementTitle) / 2;
        return guiGraphics.drawString(font, replacementTitle, replacementX, y, color);
    }

    @Redirect(method = "renderHud",
            at = @At(value = "INVOKE", 
                    target = "Ldev/doctor4t/wathe/cca/GameRoundEndComponent$RoundEndData;role()Ldev/doctor4t/wathe/client/gui/RoleAnnouncementTexts$RoleAnnouncementText;"),
            remap = false)
    private static RoleAnnouncementText modifyPlayerRole(GameRoundEndComponent.RoundEndData entry) {
        
        if (!isCustomWin()) {
            return entry.role();
        }

        CustomWinnerComponent customWinnerComponent = getCustomWinnerComponent();

        /*
         * RoundEndData 里已经保存了回合结束瞬间的 GameProfile 快照，
         * 其中 UUID 不会因为玩家退出游戏而丢失。
         *
         * 旧逻辑会先在 client.level.players() 里查当前在线 Player，
         * 胜利者一旦退出就查不到实体，于是直接被归到 OTHER_TEAM。
         * 这就是“单独中立胜利者退出后，右侧胜利职业栏为空”的直接原因。
         * 因此这里必须直接使用结算快照里的 UUID 对比 CustomWinnerComponent 保存的 UUID 列表。
         */
        if (customWinnerComponent.isWinner(entry.player().getId())) {
            return WINNER_TEAM;
        } else {
            return OTHER_TEAM;
        }
    }

    private static CustomWinnerComponent getCustomWinnerComponent() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return null;
        return CustomWinnerComponent.KEY.get(client.level);
    }

    private static boolean isCustomWin() {
        CustomWinnerComponent customWinnerComponent = getCustomWinnerComponent();
        return customWinnerComponent != null && customWinnerComponent.hasCustomWinner();
    }
}
