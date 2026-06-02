package pro.fazeclan.river.stupid_express.client.ui.common;

/**
 * 玩家头像分页布局工具。
 *
 * <p>这里把“每页最多显示多少人”“头像之间的固定间距”“翻页按钮放在哪里”
 * 这些和具体职业无关的规则统一收口到一个地方，后续其它职业如果也要接入
 * 背包头像分页，就可以直接复用这里的常量和坐标计算，避免每个职业各写一套
 * 导致位置、间距、容量都不一致。</p>
 */
public final class PlayerPageLayout {
    /**
     * 每一页最多渲染的玩家头像数量。
     *
     * <p>当前按需求固定为 10，后续如果你想统一改成 8、12 或其它数字，
     * 只需要修改这一处常量即可。</p>
     */
    public static final int PLAYERS_PER_PAGE = 10;

    /**
     * 头像按钮之间沿用原本界面的固定间距，保持和旧版视觉风格一致。
     */
    public static final int SLOT_APART = 36;

    /**
     * 原版这类头像条通常都会带一个 +9 的视觉修正。
     *
     * <p>这里继续保留这个偏移，避免分页接入后整排头像整体向左或向右错半格。</p>
     */
    public static final int SLOT_X_OFFSET = 9;

    private PlayerPageLayout() {
    }

    /**
     * 玩家头像这一整排按钮的 Y 坐标。
     *
     * <p>继续沿用召集者当前背包头像条所在的纵向位置，
     * 避免接入分页后整排按钮突然跳到别的位置。</p>
     */
    public static int getPlayerRowY(int screenHeight) {
        return (screenHeight - 32) / 2 + 80;
    }

    /**
     * 按“当前页真实显示人数”来重新居中头像。
     *
     * <p>这样最后一页就算不足 10 个人，也不会左对齐挤在某一边，
     * 而是依旧像原版那样从中间开始展开。</p>
     */
    public static int getCenteredPlayerStartX(int screenWidth, int visiblePlayerCount) {
        return screenWidth / 2 - visiblePlayerCount * SLOT_APART / 2 + SLOT_X_OFFSET;
    }

    /**
     * 计算“固定 10 个头像位”整块区域的左侧起点。
     *
     * <p>这个方法适合给“按钮永远钉在固定头像区域两侧”的方案使用。
     * 虽然召集者当前改成了按钮也跟随内容整体居中，但保留这套计算后，
     * 后面如果有别的职业想做固定边缘翻页，也可以直接复用。</p>
     */
    public static int getFixedAreaStartX(int screenWidth) {
        return screenWidth / 2 - PLAYERS_PER_PAGE * SLOT_APART / 2 + SLOT_X_OFFSET;
    }

    /**
     * 左侧“上一页”按钮在固定布局方案下的 X 坐标。
     */
    public static int getPreviousButtonX(int screenWidth) {
        return getFixedAreaStartX(screenWidth) - SLOT_APART;
    }

    /**
     * 右侧“下一页”按钮在固定布局方案下的 X 坐标。
     */
    public static int getNextButtonX(int screenWidth) {
        return getFixedAreaStartX(screenWidth) + PLAYERS_PER_PAGE * SLOT_APART;
    }

    /**
     * 计算“上一页按钮 + 当前页头像 + 下一页按钮”这一整条内容的起始 X。
     *
     * <p>这里专门给“按钮也跟随本页内容一起整体居中”的方案使用。
     * 这样最后一页人数不足时，翻页按钮不会还钉在最边缘，
     * 而是会和当前页真正显示出来的头像一起居中排列。</p>
     */
    public static int getCenteredGroupStartX(int screenWidth, int visiblePlayerCount, boolean showPrevious, boolean showNext) {
        int buttonCount = (showPrevious ? 1 : 0) + (showNext ? 1 : 0);
        int totalSlots = visiblePlayerCount + buttonCount;
        return screenWidth / 2 - totalSlots * SLOT_APART / 2 + SLOT_X_OFFSET;
    }

    /**
     * 根据总玩家数计算总页数。
     *
     * <p>这里至少返回 1，避免极端情况下出现 0 页，导致翻页边界判断出错。</p>
     */
    public static int getTotalPageCount(int totalPlayers) {
        return Math.max(1, (totalPlayers + PLAYERS_PER_PAGE - 1) / PLAYERS_PER_PAGE);
    }
}
