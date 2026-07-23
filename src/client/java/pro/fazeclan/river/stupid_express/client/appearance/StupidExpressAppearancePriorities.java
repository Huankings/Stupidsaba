package pro.fazeclan.river.stupid_express.client.appearance;

/**
 * StupidExpress 客户端外观 / 准心名字接入的优先级表。
 *
 * <p>优先级集中存放，方便确认“强制全员伪装、主动伪装、低优先级词条”之间的覆盖关系。</p>
 */
public final class StupidExpressAppearancePriorities {
    /**
     * 双重人格只是身份词条外观兜底，低于任何主动外观覆盖。
     */
    public static final int DUAL_PERSONALITY = -100;

    /**
     * 双重人格休眠态的准心名字隐藏优先级。
     *
     * <p>这里保留一个更高的值，是为了确保“当前相机挂在休眠人格身上”时，
     * HudVisibility 的隐藏结果能压过低优先级的名字/身份展示。</p>
     */
    public static final int DUAL_PERSONALITY_DORMANT_VISIBILITY = 1000;

    private StupidExpressAppearancePriorities() {
    }
}
