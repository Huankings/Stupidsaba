package pro.fazeclan.river.stupid_express.client.appearance;

/**
 * StupidExpress 客户端外观 / 准心名字接入的优先级表。
 *
 * <p>优先级集中存放，方便确认“强制全员伪装、主动伪装、低优先级词条”之间的覆盖关系。</p>
 */
public final class StupidExpressAppearancePriorities {
    /**
     * 召集者召集尸体后的限时变形高于普通主动变形与双重人格。
     * NoellesRoles 灵术师本地出窍使用更高优先级，因此仍然可以盖过这里。
     */
    public static final int CONVENER = 1000;

    /**
     * 双重人格只是身份词条外观兜底，低于召集者和其它主动变形。
     */
    public static final int DUAL_PERSONALITY = -100;

    private StupidExpressAppearancePriorities() {
    }
}
