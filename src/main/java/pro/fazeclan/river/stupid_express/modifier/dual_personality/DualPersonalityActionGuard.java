package pro.fazeclan.river.stupid_express.modifier.dual_personality;

import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.agmas.noellesroles.AbilityPlayerComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.cca.AbilityCooldownComponent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 休眠人格的服务端动作封锁器。
 *
 * <p>休眠人格在客户端会被放进旁观模式，但只靠客户端状态并不可靠：
 * 其它扩展模组可能直接发能力包，或者某些物品使用逻辑不检查玩家模式。
 * 所以这里每 tick 维护短冷却，作为服务端侧的最后防线。</p>
 */
public final class DualPersonalityActionGuard {

    // 用 4 秒滚动窗口封锁能力。每 tick 续上，切回活跃后冷却也会很快自然消失。
    public static final int DORMANT_LOCK_TICKS = 4 * 20;

    // 反射桥接失败只记录一次，避免服务器日志每 tick 被刷爆。
    private static final Set<String> REPORTED_REFLECTION_FAILURES = Collections.synchronizedSet(new HashSet<>());

    private DualPersonalityActionGuard() {
    }

    public static boolean isDormant(ServerPlayer player) {
        /*
         * 这里只认 ACTIVE 局内阶段。
         * Wathe 结算 STOPPING 期间旧组件可能还没被客户端完全同步清除，
         * 但休眠人格封锁不能继续影响结算/大厅交互。
         */
        return player != null
                && DualPersonalityManager.isActiveRound(player.level())
                && DualPersonalityComponent.KEY.get(player.level()).isDormant(player.getUUID());
    }

    public static void maintainDormantLock(ServerPlayer player) {
        if (!isDormant(player)) {
            return;
        }

        /*
         * 休眠人格处于特殊存活旁观，正常客户端已经无法交互。
         * 这里仍然每 tick 维持一个短冷却窗口，是为了防其它扩展模组
         * 直接发能力包绕过客户端状态。窗口很短，切回活跃人格后不会被多锁一整分钟。
         */
        applyStupidExpressAbilityCooldown(player);
        applyNoellesAbilityCooldown(player);
        applyReflectedAbilityCooldown(player, "org.BsXinQin.kinswathe.component.AbilityPlayerComponent");
        applyReflectedAbilityCooldown(player, "org.aussiebox.starexpress.cca.AbilityComponent");
        applyItemCooldowns(player);
    }

    private static void applyStupidExpressAbilityCooldown(ServerPlayer player) {
        // 本模组自己的能力冷却组件可以直接访问，保证小偷等能力不会被休眠人格触发。
        AbilityCooldownComponent component = AbilityCooldownComponent.KEY.get(player);
        if (component.getCooldown() < DORMANT_LOCK_TICKS) {
            component.setCooldown(DORMANT_LOCK_TICKS);
            component.sync();
        }
    }

    private static void applyNoellesAbilityCooldown(ServerPlayer player) {
        // noellesroles 是显式依赖/兼容目标，mod 存在时直接调用它公开的组件。
        if (!FabricLoader.getInstance().isModLoaded("noellesroles")) {
            return;
        }
        AbilityPlayerComponent component = AbilityPlayerComponent.KEY.get(player);
        if (component.cooldown < DORMANT_LOCK_TICKS) {
            component.setCooldown(DORMANT_LOCK_TICKS);
        }
    }

    private static void applyItemCooldowns(ServerPlayer player) {
        /*
         * 物品冷却比拦每一个使用包更兼容。
         * Wathe 原版物品、本模组物品、常见扩展物品都放进同一个集合，
         * 未安装扩展模组时 addRegisteredItem 会自动跳过。
         */
        Set<Item> items = new LinkedHashSet<>();
        items.add(WatheItems.KNIFE);
        items.add(WatheItems.REVOLVER);
        items.add(WatheItems.DERRINGER);
        items.add(WatheItems.GRENADE);
        items.add(WatheItems.PSYCHO_MODE);
        items.add(WatheItems.BLACKOUT);

        // 纵火犯物品已经迁移到 NoellesRoles，通过注册表软取，避免继续硬依赖本模组已删除的物品常量。
        addRegisteredItem(items, "noellesroles", "jerry_can");
        addRegisteredItem(items, "noellesroles", "lighter");
        addRegisteredItem(items, "noellesroles", "throwing_axe");
        addRegisteredItem(items, "noellesroles", "robber_pistol");
        addRegisteredItem(items, "noellesroles", "timed_bomb");
        addRegisteredItem(items, "noellesroles", "power_restoration");
        addRegisteredItem(items, "kinswathe", "blowgun");
        addRegisteredItem(items, "kinswathe", "hunting_knife");
        addRegisteredItem(items, "kinswathe", "knockout_drug");
        addRegisteredItem(items, "kinswathe", "poison_injector");
        addRegisteredItem(items, "kinswathe", "pan");
        addRegisteredItem(items, "starexpress", "tape");

        for (Item item : items) {
            if (item != null) {
                player.getCooldowns().addCooldown(item, DORMANT_LOCK_TICKS);
            }
        }
    }

    private static void addRegisteredItem(Set<Item> items, String namespace, String path) {
        // 通过注册表查物品，避免硬依赖没有安装的扩展 mod 类。
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return;
        }
        items.add(BuiltInRegistries.ITEM.get(id));
    }

    private static void applyReflectedAbilityCooldown(ServerPlayer player, String componentClassName) {
        try {
            /*
             * kinswathe、starexpress 等扩展不一定总安装。
             * 为了不把它们做成硬依赖，这里只在类存在时用反射访问 KEY/get/cooldown/sync。
             */
            Class<?> componentClass = Class.forName(componentClassName);
            Object componentKey = readStaticField(componentClass, "KEY");
            Object component = findMethod(componentKey.getClass(), "get", 1).invoke(componentKey, player);
            if (component == null) {
                return;
            }

            int currentCooldown = readIntField(component, "cooldown");
            if (currentCooldown >= DORMANT_LOCK_TICKS) {
                return;
            }

            writeIntField(component, "cooldown", DORMANT_LOCK_TICKS);
            findMethod(componentClass, "sync", 0).invoke(component);
        } catch (ReflectiveOperationException exception) {
            if (REPORTED_REFLECTION_FAILURES.add(componentClassName)) {
                StupidExpress.LOGGER.warn("双重人格休眠人格跨模组技能封锁桥接失败：{}", componentClassName, exception);
            }
        }
    }

    private static Object readStaticField(Class<?> owner, String fieldName) throws ReflectiveOperationException {
        return findField(owner, fieldName).get(null);
    }

    private static int readIntField(Object instance, String fieldName) throws ReflectiveOperationException {
        return findField(instance.getClass(), fieldName).getInt(instance);
    }

    private static void writeIntField(Object instance, String fieldName, int value) throws ReflectiveOperationException {
        findField(instance.getClass(), fieldName).setInt(instance, value);
    }

    private static Field findField(Class<?> owner, String fieldName) throws NoSuchFieldException {
        try {
            // 先找 public 字段，失败再找 declared 字段，兼容不同扩展组件的可见性写法。
            Field field = owner.getField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        }
    }

    private static Method findMethod(Class<?> owner, String methodName, int parameterCount) throws NoSuchMethodException {
        // 只按方法名和参数数量匹配，避免不同版本返回值/泛型签名细节导致桥接失效。
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                method.setAccessible(true);
                return method;
            }
        }
        for (Method method : owner.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(owner.getName() + "#" + methodName + "/" + parameterCount);
    }
}
