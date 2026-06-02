package pro.fazeclan.river.stupid_express.client.mixin.role.convener;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerDisguiseButton;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerDisguiseResolver;
import pro.fazeclan.river.stupid_express.client.ui.common.PagedPlayerScreenState;
import pro.fazeclan.river.stupid_express.client.ui.common.PlayerPageLayout;
import pro.fazeclan.river.stupid_express.client.ui.common.PlayerPageSwitchWidget;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerPlayerComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 召集者背包头像界面。
 *
 * <p>这一版在原本“点击头像切换伪装/点自己解除伪装”的基础上，
 * 额外接入了和 NoellesRoles 同风格的分页机制：
 * 1. 每页最多渲染 10 个头像；
 * 2. 超出后显示左右翻页按钮；
 * 3. 最后一页人数不足时，头像与翻页按钮一起整体居中；
 * 4. 翻页按钮独立于真正的伪装按钮，不会误触发伪装选择逻辑；
 * 5. 页码只在当前对局内缓存，关掉背包再打开时仍能回到刚才那一页。</p>
 *
 * <p>召集者的特殊点在于：可显示头像并不是固定玩家总数，
 * 而是随着“召集尸体”逐步解锁出来。所以这里的分页总数并不是按在线人数算，
 * 而是每次都实时根据“当前已解锁伪装列表”重新分页。</p>
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class ConvenerInventoryMixin extends LimitedHandledScreen<InventoryMenu> {

    @Shadow
    @Final
    public LocalPlayer player;

    /**
     * 当前背包界面里已经创建出来的所有召集者伪装按钮。
     *
     * <p>注意，这里存的是“全部已解锁目标对应的按钮”，而不是“当前页的按钮”。
     * 这样翻页时我们只需要切换 visible 和位置，不必每次重新 new 整批控件，
     * 可以减少按钮反复销毁/重建造成的状态复杂度。</p>
     */
    @Unique
    private final List<ConvenerDisguiseButton> stupidexpress$convenerButtons = new ArrayList<>();

    /**
     * 当前完整目标列表对应的顺序缓存。
     *
     * <p>只要列表内容或顺序发生变化，就按新顺序重建一遍按钮集合；
     * 没变化时则只更新分页布局，避免 render 阶段不断重复塞控件。</p>
     */
    @Unique
    private final List<UUID> stupidexpress$buttonTargets = new ArrayList<>();

    /**
     * 上一页按钮。
     */
    @Unique
    private PlayerPageSwitchWidget stupidexpress$previousPageWidget;

    /**
     * 下一页按钮。
     */
    @Unique
    private PlayerPageSwitchWidget stupidexpress$nextPageWidget;

    /**
     * 当前缓存的页码。
     *
     * <p>真正的持久化值保存在 PagedPlayerScreenState 里，这里只是当前界面实例的工作副本。</p>
     */
    @Unique
    private int stupidexpress$currentPage;

    /**
     * 标记当前界面实例是否已经从全局缓存中恢复过页码。
     *
     * <p>这样关闭再打开背包时能回到之前那一页，但在同一个 Screen 生命周期里
     * render 多次时又不会每帧把当前页强制重置回缓存值。</p>
     */
    @Unique
    private boolean stupidexpress$pageInitialized;

    public ConvenerInventoryMixin(InventoryMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void stupidexpress$initConvenerHeads(CallbackInfo ci) {
        stupidexpress$ensureConvenerWidgets();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void stupidexpress$renderConvenerText(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.player == null || this.player.level() == null) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(this.player.level());
        if (!gameWorldComponent.isRole(this.player, SERoles.CONVENER)) {
            return;
        }

        // 再做一次兜底刷新：
        // 如果玩家刚刚召集完尸体后立刻打开背包，而组件同步恰好慢了半拍，
        // render 阶段仍然能根据最新已解锁头像列表把分页和头像补出来。
        stupidexpress$ensureConvenerWidgets();

        ConvenerDisguiseComponent disguiseComponent = ConvenerDisguiseComponent.KEY.get(this.player);
        ConvenerPlayerComponent convenerComponent = ConvenerPlayerComponent.KEY.get(this.player);

        int centerX = ((LimitedInventoryScreen) (Object) this).width / 2;
        int baseY = (((LimitedInventoryScreen) (Object) this).height - 32) / 2 + 40;
        Component disguiseName = ConvenerDisguiseResolver.resolveDisguiseName(this.player, disguiseComponent.getDisguiseUuid());

        Component stateLine = Component.translatable(
                "hud.stupid_express.convener.current_disguise",
                disguiseName != null ? disguiseName : Component.translatable("hud.stupid_express.convener.waiting")
        );
        Component progressLine = Component.translatable(
                "hud.stupid_express.convener.progress",
                convenerComponent.getSummonCount(),
                convenerComponent.getRequiredSummons()
        );

        context.drawString(this.font, stateLine, centerX - this.font.width(stateLine) / 2, baseY, SERoles.CONVENER.color(), true);
        context.drawString(this.font, progressLine, centerX - this.font.width(progressLine) / 2, baseY + this.font.lineHeight + 2, SERoles.CONVENER.color(), true);
    }

    /**
     * 根据当前组件状态刷新召集者头像栏。
     *
     * <p>这里分成两层：
     * 1. 如果“已解锁头像列表”本身变化了，就重建按钮集合；
     * 2. 不管列表是否变化，都再按当前页重新布局一次。
     *
     * <p>这样既能适配“刚召集完又多解锁了一个头像”的动态增长，
     * 也能适配“玩家只是单纯切页/缩放界面/重新打开背包”的纯布局变化。</p>
     */
    @Unique
    private void stupidexpress$ensureConvenerWidgets() {
        if (this.player == null || this.player.level() == null) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(this.player.level());
        if (!gameWorldComponent.isRole(this.player, SERoles.CONVENER)) {
            stupidexpress$clearConvenerWidgets();
            return;
        }

        if (!this.stupidexpress$pageInitialized) {
            this.stupidexpress$pageInitialized = true;
            this.stupidexpress$currentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.CONVENER_PAGE_KEY);
        }

        ConvenerPlayerComponent convenerComponent = ConvenerPlayerComponent.KEY.get(this.player);
        ConvenerDisguiseComponent disguiseComponent = ConvenerDisguiseComponent.KEY.get(this.player);
        List<UUID> targets = stupidexpress$collectDisplayTargets(convenerComponent, disguiseComponent);

        if (!this.stupidexpress$buttonTargets.equals(targets)) {
            stupidexpress$rebuildTargetButtons(targets);
        }

        stupidexpress$refreshPagedLayout();
    }

    /**
     * 按最新目标列表重建所有召集者头像按钮。
     *
     * <p>这里的“重建”只发生在目标列表真的变化时，比如：
     * 1. 新召集了一具尸体，解锁了新的伪装目标；
     * 2. 当前正在伪装的人因为同步顺序问题需要兜底加入列表；
     * 3. 背包重新初始化，旧控件列表已经失效。</p>
     */
    @Unique
    private void stupidexpress$rebuildTargetButtons(List<UUID> targets) {
        stupidexpress$removeTargetButtons();
        this.stupidexpress$buttonTargets.clear();
        this.stupidexpress$buttonTargets.addAll(targets);

        int playerRowY = PlayerPageLayout.getPlayerRowY(((LimitedInventoryScreen) (Object) this).height);

        for (UUID targetUuid : targets) {
            PlayerInfo playerInfo = ConvenerDisguiseResolver.resolvePlayerInfo(targetUuid);
            AbstractClientPlayer livePlayer = ConvenerDisguiseResolver.resolveLivePlayer(targetUuid);

            ConvenerDisguiseButton child = new ConvenerDisguiseButton(
                    (LimitedInventoryScreen) (Object) this,
                    0,
                    playerRowY,
                    targetUuid,
                    targetUuid.equals(this.player.getUUID()),
                    playerInfo,
                    livePlayer
            );
            child.visible = false;
            this.stupidexpress$convenerButtons.add(child);
            addRenderableWidget(child);
        }
    }

    /**
     * 按当前页刷新按钮显示、坐标与翻页控件。
     *
     * <p>这里是整个分页逻辑的核心：
     * 1. 先根据目标总数算出总页数；
     * 2. 再把当前页 clamp 到合法范围；
     * 3. 然后只显示这一页应显示的那一段头像；
     * 4. 最后决定是否显示左右翻页按钮，并让按钮和头像一起整体居中。</p>
     */
    @Unique
    private void stupidexpress$refreshPagedLayout() {
        int totalPlayers = this.stupidexpress$convenerButtons.size();
        int totalPages = PlayerPageLayout.getTotalPageCount(totalPlayers);
        this.stupidexpress$currentPage = Math.max(0, Math.min(this.stupidexpress$currentPage, totalPages - 1));
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.CONVENER_PAGE_KEY, this.stupidexpress$currentPage);

        int startIndex = this.stupidexpress$currentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, totalPlayers);
        int visibleCount = Math.max(0, endIndex - startIndex);

        boolean showPrevious = this.stupidexpress$currentPage > 0;
        boolean showNext = this.stupidexpress$currentPage < totalPages - 1;

        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(
                ((LimitedInventoryScreen) (Object) this).width,
                visibleCount,
                showPrevious,
                showNext
        );
        int rowY = PlayerPageLayout.getPlayerRowY(((LimitedInventoryScreen) (Object) this).height);
        int slotIndex = 0;

        // 先把所有头像按钮统一隐藏，防止上一页残留。
        for (ConvenerDisguiseButton button : this.stupidexpress$convenerButtons) {
            button.visible = false;
        }

        // 如果当前页左边需要“上一页”，先占掉最左侧一个槽位。
        if (showPrevious) {
            stupidexpress$ensurePreviousPageWidget();
            this.stupidexpress$previousPageWidget.visible = true;
            this.stupidexpress$previousPageWidget.setPosition(groupStartX, rowY);
            slotIndex++;
        } else if (this.stupidexpress$previousPageWidget != null) {
            this.stupidexpress$previousPageWidget.visible = false;
        }

        // 再摆放当前页真正的伪装按钮。
        for (int index = startIndex; index < endIndex; index++) {
            ConvenerDisguiseButton button = this.stupidexpress$convenerButtons.get(index);
            button.visible = true;
            button.setPosition(groupStartX + slotIndex * PlayerPageLayout.SLOT_APART, rowY);
            slotIndex++;
        }

        // 如果当前页右边还需要“下一页”，把按钮接在当前页头像右侧继续排。
        if (showNext) {
            stupidexpress$ensureNextPageWidget();
            this.stupidexpress$nextPageWidget.visible = true;
            this.stupidexpress$nextPageWidget.setPosition(groupStartX + slotIndex * PlayerPageLayout.SLOT_APART, rowY);
        } else if (this.stupidexpress$nextPageWidget != null) {
            this.stupidexpress$nextPageWidget.visible = false;
        }
    }

    /**
     * 确保“上一页”按钮已经创建出来。
     *
     * <p>按钮只负责切换页码，不参与任何伪装目标选择逻辑，
     * 因此不会像真正的头像按钮那样发召集者变形包。</p>
     */
    @Unique
    private void stupidexpress$ensurePreviousPageWidget() {
        if (this.stupidexpress$previousPageWidget != null) {
            return;
        }

        this.stupidexpress$previousPageWidget = addRenderableWidget(new PlayerPageSwitchWidget(
                0,
                PlayerPageLayout.getPlayerRowY(((LimitedInventoryScreen) (Object) this).height),
                new ItemStack(Items.PURPLE_DYE),
                Component.translatable("gui.stupid_express.page.previous"),
                button -> {
                    this.stupidexpress$currentPage = Math.max(0, this.stupidexpress$currentPage - 1);
                    PagedPlayerScreenState.setPage(PagedPlayerScreenState.CONVENER_PAGE_KEY, this.stupidexpress$currentPage);
                    stupidexpress$refreshPagedLayout();
                }
        ));
        this.stupidexpress$previousPageWidget.visible = false;
    }

    /**
     * 确保“下一页”按钮已经创建出来。
     */
    @Unique
    private void stupidexpress$ensureNextPageWidget() {
        if (this.stupidexpress$nextPageWidget != null) {
            return;
        }

        this.stupidexpress$nextPageWidget = addRenderableWidget(new PlayerPageSwitchWidget(
                0,
                PlayerPageLayout.getPlayerRowY(((LimitedInventoryScreen) (Object) this).height),
                new ItemStack(Items.LIME_DYE),
                Component.translatable("gui.stupid_express.page.next"),
                button -> {
                    int totalPages = PlayerPageLayout.getTotalPageCount(this.stupidexpress$convenerButtons.size());
                    this.stupidexpress$currentPage = Math.min(totalPages - 1, this.stupidexpress$currentPage + 1);
                    PagedPlayerScreenState.setPage(PagedPlayerScreenState.CONVENER_PAGE_KEY, this.stupidexpress$currentPage);
                    stupidexpress$refreshPagedLayout();
                }
        ));
        this.stupidexpress$nextPageWidget.visible = false;
    }

    /**
     * 清掉当前界面实例上所有召集者相关控件。
     *
     * <p>这个方法会在玩家不是召集者、界面角色状态变化或界面彻底重建时调用，
     * 防止旧的一批头像和翻页按钮继续残留在背包里。</p>
     */
    @Unique
    private void stupidexpress$clearConvenerWidgets() {
        stupidexpress$removeTargetButtons();
        if (this.stupidexpress$previousPageWidget != null) {
            removeWidget(this.stupidexpress$previousPageWidget);
            this.stupidexpress$previousPageWidget = null;
        }
        if (this.stupidexpress$nextPageWidget != null) {
            removeWidget(this.stupidexpress$nextPageWidget);
            this.stupidexpress$nextPageWidget = null;
        }
        this.stupidexpress$buttonTargets.clear();
        this.stupidexpress$pageInitialized = false;
        this.stupidexpress$currentPage = 0;
    }

    /**
     * 仅移除伪装目标头像按钮本体。
     */
    @Unique
    private void stupidexpress$removeTargetButtons() {
        for (ConvenerDisguiseButton button : this.stupidexpress$convenerButtons) {
            removeWidget(button);
        }
        this.stupidexpress$convenerButtons.clear();
    }

    /**
     * 收集当前背包里应当显示哪些头像。
     *
     * <p>规则：
     * 1. 自己永远保留，作为“解除变形”的固定入口；
     * 2. 正常读取已解锁头像列表；
     * 3. 若当前已经伪装成某人，但解锁同步尚未到位，也把那个人兜底补进去；
     * 4. 顺序尽量贴近在线玩家列表，最后保证自己排在最前面。</p>
     */
    @Unique
    private List<UUID> stupidexpress$collectDisplayTargets(
            ConvenerPlayerComponent convenerComponent,
            ConvenerDisguiseComponent disguiseComponent
    ) {
        Set<UUID> orderedTargets = new LinkedHashSet<>();
        orderedTargets.add(this.player.getUUID());
        orderedTargets.addAll(convenerComponent.getUnlockedDisguises());

        if (disguiseComponent.getDisguiseUuid() != null) {
            orderedTargets.add(disguiseComponent.getDisguiseUuid());
        }

        List<UUID> sortedTargets = new ArrayList<>();
        if (this.player.connection != null) {
            for (UUID onlineUuid : this.player.connection.getOnlinePlayerIds()) {
                if (orderedTargets.contains(onlineUuid)) {
                    sortedTargets.add(onlineUuid);
                }
            }
        }

        for (UUID uuid : orderedTargets) {
            if (!sortedTargets.contains(uuid)) {
                sortedTargets.add(uuid);
            }
        }

        sortedTargets.sort(Comparator.comparing(uuid -> !uuid.equals(this.player.getUUID())));
        return sortedTargets;
    }
}
