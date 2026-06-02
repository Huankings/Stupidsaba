package pro.fazeclan.river.stupid_express.client.mixin.role.convener;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerDisguiseResolver;
import pro.fazeclan.river.stupid_express.role.convener.ConvenerCommunicationHelper;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;

import java.util.UUID;

@Mixin(RoleNameRenderer.class)
public class ConvenerRoleNameMixin {

    /**
     * 记录最近一次被 RoleNameRenderer 锁定的玩家，
     * 在准心提示淡出期间继续沿用这份状态，避免鼠标刚移开时闪出“杀手同伙”。
     */
    private static boolean stupidexpress$lastShouldHideCohortHint;

    @WrapOperation(method = "renderHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDisplayName()Lnet/minecraft/network/chat/Component;"))
    private static Component stupidexpress$replaceDisplayName(Player instance, Operation<Component> original) {
        // RoleNameRenderer 在准心锁定玩家时会先取一次显示名，
        // 所以这里顺手把“这名玩家是否处于召集后的限时变形”缓存下来。
        // 后面哪怕鼠标已经移开，HUD 淡出阶段也还能继续用同一份判定。
        stupidexpress$lastShouldHideCohortHint = ConvenerCommunicationHelper.isTemporarilySummonedLivingPlayer(instance);

        ConvenerDisguiseComponent disguiseComponent = ConvenerDisguiseComponent.KEY.get(instance);
        if (!disguiseComponent.isDisguised()) {
            return original.call(instance);
        }

        // 准星下的人名如果不一起替换，玩家会看到“脸变了但名字没变”，
        // 所以这里和皮肤替换保持同一套目标解析顺序。
        UUID disguiseUuid = disguiseComponent.getDisguiseUuid();
        if (disguiseUuid == null || disguiseUuid.equals(instance.getUUID())) {
            return original.call(instance);
        }
        Component disguiseName = ConvenerDisguiseResolver.resolveDisguiseName(instance, disguiseUuid);
        return disguiseName != null ? disguiseName : original.call(instance);
    }

    @WrapOperation(
            method = "renderHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
            )
    )
    private static MutableComponent stupidexpress$hideCohortHintDuringSummon(
            String key,
            Operation<MutableComponent> original
    ) {
        if (!"game.tip.cohort".equals(key)) {
            return original.call(key);
        }

        // 被召集活人的变形期间，杀手准心对准该玩家时只能看到名字，
        // 不能再靠“杀手同伙”这一行额外文本去反推出阵营身份。
        if (stupidexpress$lastShouldHideCohortHint) {
            return Component.empty();
        }

        return original.call(key);
    }

    @WrapOperation(
            method = "renderHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I",
                    ordinal = 1
            )
    )
    private static int stupidexpress$skipCohortHintDraw(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            int x,
            int y,
            int color,
            Operation<Integer> original
    ) {
        // 这里再在“最终真正绘制 cohort 小字”的位置补一道兜底。
        // 即使前面文本构造阶段因为极短暂的准心同步误差漏过去了，
        // 只要这帧的 cohort 文案本质上属于被召集活人的淡出/显示流程，就直接不画出来。
        if (stupidexpress$lastShouldHideCohortHint) {
            return 0;
        }

        return original.call(guiGraphics, font, text, x, y, color);
    }
}
