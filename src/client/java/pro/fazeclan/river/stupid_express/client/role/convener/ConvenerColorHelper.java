package pro.fazeclan.river.stupid_express.client.role.convener;

import java.util.UUID;

/**
 * 召集者专用的流动配色工具。
 *
 * <p>这一套颜色同时服务于：
 * 1. 左上角心情值条；
 * 2. 本能透视下的活人描边颜色。
 *
 * <p>这样可以保证两个地方看到的是同一组“浅蓝 -> 深蓝 -> 紫 -> 粉”的流动色系，
 * 后续如果你还想继续微调颜色或速度，只需要改这一处即可。</p>
 */
public final class ConvenerColorHelper {

    /**
     * 整套流动色的完整循环周期。
     *
     * <p>这里保留和心情条一致的慢速流动节奏，让颜色过渡更柔和，不会闪得太快。</p>
     */
    private static final long FLOW_PERIOD_MS = 12_000L;

    /**
     * 召集者流动色调色板。
     *
     * <p>颜色顺序严格按需求里提到的：
     * 浅蓝、天蓝、深蓝、浅紫、紫色、深紫、浅粉、粉色、深粉。</p>
     */
    public static final int[] FLOW_COLORS = new int[] {
            0x95D6FF,
            0x54C1FF,
            0x1B6BFF,
            0xB29BFF,
            0x7B5CFF,
            0x4E2ED6,
            0xFFB0E4,
            0xFF78C9,
            0xE63B9E
    };

    private ConvenerColorHelper() {}

    /**
     * 获取心情值条某一列像素当前应显示的颜色。
     *
     * <p>通过“条内位置 + 全局时间相位”的方式，让整条色带持续从左往右平滑流动。</p>
     */
    public static int getBarFlowColor(int x, int width, float alpha) {
        float position = ((float) x / Math.max(1, width - 1)) * FLOW_COLORS.length;
        return getAnimatedColor(getCurrentPhase() + position, alpha);
    }

    /**
     * 获取某个玩家在本能透视里当前应显示的流动颜色。
     *
     * <p>这里额外叠加了一个基于 UUID 的相位偏移：
     * 不同玩家会落在同一条流动色带的不同位置上，
     * 因此不会出现“所有活人同时变成完全一样的颜色”的死板效果。</p>
     */
    public static int getPlayerFlowColor(UUID uuid) {
        int hash = Math.floorMod(uuid.hashCode(), 4096);
        float uuidOffset = ((float) hash / 4096.0F) * FLOW_COLORS.length;
        return getAnimatedColor(getCurrentPhase() + uuidOffset, 1.0F);
    }

    /**
     * 根据“色带位置”计算当前插值后的平滑颜色。
     */
    public static int getAnimatedColor(float palettePosition, float alpha) {
        float floor = (float) Math.floor(palettePosition);
        int leftIndex = Math.floorMod((int) floor, FLOW_COLORS.length);
        int rightIndex = (leftIndex + 1) % FLOW_COLORS.length;
        float blend = palettePosition - floor;
        return interpolateColor(FLOW_COLORS[leftIndex], FLOW_COLORS[rightIndex], blend, alpha);
    }

    /**
     * 读取当前全局时间相位。
     */
    private static float getCurrentPhase() {
        long current = System.currentTimeMillis() % FLOW_PERIOD_MS;
        return ((float) current / (float) FLOW_PERIOD_MS) * FLOW_COLORS.length;
    }

    /**
     * 在两个 RGB 颜色之间做线性插值，同时叠加 Alpha。
     */
    private static int interpolateColor(int left, int right, float progress, float alpha) {
        int lr = (left >> 16) & 0xFF;
        int lg = (left >> 8) & 0xFF;
        int lb = left & 0xFF;
        int rr = (right >> 16) & 0xFF;
        int rg = (right >> 8) & 0xFF;
        int rb = right & 0xFF;

        int r = Math.round(lr + (rr - lr) * progress);
        int g = Math.round(lg + (rg - lg) * progress);
        int b = Math.round(lb + (rb - lb) * progress);
        int a = Math.round(alpha * 255.0F) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
