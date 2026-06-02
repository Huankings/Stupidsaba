package pro.fazeclan.river.stupid_express.client.ui.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 选人分页界面的客户端状态缓存。
 *
 * <p>这份状态只在“当前这一局游戏”内保留：
 * 1. 玩家关闭再打开背包时，可以回到刚才浏览的页数；
 * 2. 新一局开始、旧一局结束时，要主动清空，避免上一局的页码污染下一局。</p>
 *
 * <p>这里从一开始就做成按 key 通用缓存，后续 StupidExpress 里如果还有别的职业
 * 也需要背包头像分页，就不必再额外新建一套页码状态类了。</p>
 */
public final class PagedPlayerScreenState {
    public static final String CONVENER_PAGE_KEY = "convener";

    /**
     * 按界面 key 保存对应页码。
     *
     * <p>例如当前召集者翻到了第 2 页，只会影响召集者自己的分页界面，
     * 不会和后续其它职业的分页状态串在一起。</p>
     */
    private static final Map<String, Integer> PAGE_CACHE = new HashMap<>();

    private PagedPlayerScreenState() {
    }

    public static int getPage(String key) {
        return PAGE_CACHE.getOrDefault(key, 0);
    }

    public static void setPage(String key, int page) {
        PAGE_CACHE.put(key, Math.max(0, page));
    }

    /**
     * 开新局、停局或完成结算后统一清空所有分页缓存。
     */
    public static void reset() {
        PAGE_CACHE.clear();
    }
}
