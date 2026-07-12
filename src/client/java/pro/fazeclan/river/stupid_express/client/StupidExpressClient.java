package pro.fazeclan.river.stupid_express.client;

import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.ratatouille.util.TextUtils;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import pro.fazeclan.river.stupid_express.client.ui.common.PagedPlayerScreenState;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityKeybinds;
import pro.fazeclan.river.stupid_express.constants.SEItems;

public class StupidExpressClient implements ClientModInitializer {

    public static Player target;
    public static PlayerBodyEntity targetBody;

    @Override
    public void onInitializeClient() {
        // 分页缓存只在当前对局内生效。
        // 开局、停局、结算结束时统一清空，避免上一把浏览过的页码残留到下一把。
        GameEvents.ON_GAME_START.register(gameMode -> {
            PagedPlayerScreenState.reset();
            DualPersonalityClientState.resetTransientRenderState();
        });
        GameEvents.ON_GAME_STOP.register(gameMode -> {
            PagedPlayerScreenState.reset();
            /*
             * Wathe 的 TimeRenderer 使用静态滚动数字对象。
             * 双活 HUD 借用它显示 40 秒倒计时，所以停局时要立刻清掉，
             * 否则结算画面或下一次普通时间 HUD 会复用旧数字。
             */
            DualPersonalityClientState.resetTransientRenderState();
        });
        GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            PagedPlayerScreenState.reset();
            DualPersonalityClientState.resetTransientRenderState();
        });

        DualPersonalityKeybinds.init();

        ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipFlag, list) -> {
            if (itemStack.is(SEItems.JERRY_CAN)) list.addAll(TextUtils.getTooltipForItem(itemStack.getItem(), Style.EMPTY.withColor(8421504)));
            if (itemStack.is(SEItems.LIGHTER)) list.addAll(TextUtils.getTooltipForItem(itemStack.getItem(), Style.EMPTY.withColor(8421504)));
        });

    }
}
