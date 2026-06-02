package pro.fazeclan.river.stupid_express.role.convener;

import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.agmas.noellesroles.AbilityPlayerComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.cca.AbilityCooldownComponent;
import pro.fazeclan.river.stupid_express.constants.SEItems;
import pro.fazeclan.river.stupid_express.mixin.role.convener.ItemCooldownInstanceAccessor;
import pro.fazeclan.river.stupid_express.mixin.role.convener.ItemCooldownsAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 召集者成功发动“召集”后，对场上活人统一追加封控冷却的工具类。
 *
 * <p>这次需求牵涉到多个扩展模组：
 * Wathe、StupidExpress、NoellesRoles、KinsWathe、StarryExpress，
 * 因此这里专门做一层集中桥接，避免把召集逻辑主流程塞满跨模组判断。</p>
 *
 * <p>实现策略如下：
 * 1. Wathe / StupidExpress / NoellesRoles：当前工程已经有编译依赖，直接调用；
 * 2. KinsWathe / StarryExpress：当前工程没有编译依赖，全部改用注册表查询和反射桥接；
 * 3. 所有时长都写成常量，后续你要微调数值时只需要改这里即可。</p>
 */
public final class ConvenerSummonLockdownHelper {

    /**
     * 武器类道具统一冷却：45 秒。
     */
    public static final int WEAPON_ITEM_COOLDOWN_TICKS = 45 * 20;

    /**
     * 疯魔模式统一冷却：70 秒。
     */
    public static final int PSYCHO_MODE_COOLDOWN_TICKS = 70 * 20;

    /**
     * 停电统一冷却：30 秒。
     */
    public static final int BLACKOUT_COOLDOWN_TICKS = 30 * 20;

    /**
     * 角色技能统一冷却：60 秒。
     */
    public static final int ABILITY_COOLDOWN_TICKS = 60 * 20;

    /**
     * 特殊事件类道具统一冷却：60 秒。
     */
    public static final int SPECIAL_EVENT_COOLDOWN_TICKS = 60 * 20;

    private static final String MOD_KINS_WATHE = "kinswathe";
    private static final String MOD_STARRY_EXPRESS = "starexpress";
    private static final String MOD_HARPY_SIMPLE_ROLES = "harpysimpleroles";

    /**
     * 反射桥接如果失败，只记录一次日志，避免每次召集都刷一屏异常。
     */
    private static final Set<String> REPORTED_REFLECTION_FAILURES = Collections.synchronizedSet(new HashSet<>());

    private ConvenerSummonLockdownHelper() {}

    /**
     * 对一名玩家施加召集封控需要的全部冷却。
     *
     * <p>这里被召集到尸体处的活人都会走一遍，
     * 所以只要在召集成功后统一调用即可。</p>
     */
    public static void applySummonLockdown(ServerPlayer player) {
        applyWeaponCooldowns(player);
        applyPsychoModeCooldown(player);
        applyBlackoutCooldown(player);
        applyAbilityCooldowns(player);
        applySpecialEventCooldowns(player);
    }

    /**
     * 给所有“武器 / 进攻 / 干扰”类道具统一追加 45 秒冷却。
     *
     * <p>这里不只覆盖 Wathe 本体，也会顺带覆盖：
     * 1. StupidExpress 自己的纵火犯道具；
     * 2. NoellesRoles 额外新增的几件武器；
     * 3. KinsWathe / StarryExpress / HarpySimpleRoles 若存在时的对应物品。</p>
     */
    private static void applyWeaponCooldowns(ServerPlayer player) {
        Set<Item> weaponItems = new LinkedHashSet<>();

        // Wathe 原版武器与危险道具
        weaponItems.add(WatheItems.KNIFE);
        weaponItems.add(WatheItems.REVOLVER);
        weaponItems.add(WatheItems.DERRINGER);
        weaponItems.add(WatheItems.GRENADE);

        // StupidExpress 自己的纵火犯链条道具也一起封住，避免召集期间继续泼油/点火。
        weaponItems.add(SEItems.JERRY_CAN);
        weaponItems.add(SEItems.LIGHTER);

        // NoellesRoles：用户点名要求覆盖的三件武器。
        addRegisteredItem(weaponItems, "noellesroles", "throwing_axe");
        addRegisteredItem(weaponItems, "noellesroles", "robber_pistol");
        addRegisteredItem(weaponItems, "noellesroles", "timed_bomb");

        // KinsWathe：当前工程没有编译依赖，统一走注册表 id 兜底。
        addRegisteredItem(weaponItems, MOD_KINS_WATHE, "blowgun");
        addRegisteredItem(weaponItems, MOD_KINS_WATHE, "hunting_knife");
        addRegisteredItem(weaponItems, MOD_KINS_WATHE, "knockout_drug");
        addRegisteredItem(weaponItems, MOD_KINS_WATHE, "poison_injector");
        addRegisteredItem(weaponItems, MOD_KINS_WATHE, "pan");

        // StarryExpress：胶带会直接影响玩家行动，因此这次也按封控物品处理。
        addRegisteredItem(weaponItems, MOD_STARRY_EXPRESS, "tape");

        // HarpySimpleRoles：顺手兼容它的毒液和强盗左轮。
        addRegisteredItem(weaponItems, MOD_HARPY_SIMPLE_ROLES, "toxin");
        addRegisteredItem(weaponItems, MOD_HARPY_SIMPLE_ROLES, "bandit_revolver");

        setItemsCooldown(player, weaponItems, WEAPON_ITEM_COOLDOWN_TICKS);
    }

    /**
     * 疯魔模式单独固定成 70 秒冷却。
     */
    private static void applyPsychoModeCooldown(ServerPlayer player) {
        setItemCooldown(player, WatheItems.PSYCHO_MODE, PSYCHO_MODE_COOLDOWN_TICKS);
    }

    /**
     * 停电单独固定成 30 秒冷却。
     */
    private static void applyBlackoutCooldown(ServerPlayer player) {
        setItemCooldown(player, WatheItems.BLACKOUT, BLACKOUT_COOLDOWN_TICKS);
    }

    /**
     * 给各模组的技能冷却组件统一追加 50 秒冷却。
     *
     * <p>这里特别注意两点：
     * 1. StupidExpress 自己直接使用本地组件；
     * 2. KinsWathe 不能直接调用它的 setAbilityCooldown，
     *    因为它内部会把 NoellesRoles 某些冷却强行桥成更长时间，
     *    不符合这次“固定 30 秒”的需求，所以这里改为直接反射写字段并同步。</p>
     */
    private static void applyAbilityCooldowns(ServerPlayer player) {
        AbilityCooldownComponent abilityCooldownComponent = AbilityCooldownComponent.KEY.get(player);
        if (abilityCooldownComponent.getCooldown() < ABILITY_COOLDOWN_TICKS) {
            abilityCooldownComponent.setCooldown(ABILITY_COOLDOWN_TICKS);
            abilityCooldownComponent.sync();
        }

        if (FabricLoader.getInstance().isModLoaded("noellesroles")) {
            AbilityPlayerComponent noellesAbilityComponent = AbilityPlayerComponent.KEY.get(player);
            if (noellesAbilityComponent.cooldown < ABILITY_COOLDOWN_TICKS) {
                noellesAbilityComponent.setCooldown(ABILITY_COOLDOWN_TICKS);
            }
        }

        if (FabricLoader.getInstance().isModLoaded(MOD_KINS_WATHE)) {
            applyReflectedComponentCooldown(
                    player,
                    "org.BsXinQin.kinswathe.component.AbilityPlayerComponent",
                    ABILITY_COOLDOWN_TICKS
            );
        }

        if (FabricLoader.getInstance().isModLoaded(MOD_STARRY_EXPRESS)) {
            applyReflectedComponentCooldown(
                    player,
                    "org.aussiebox.starexpress.cca.AbilityComponent",
                    ABILITY_COOLDOWN_TICKS
            );
        }
    }

    /**
     * 给“特殊事件类”道具统一追加 60 秒冷却。
     *
     * <p>这次按你的需求，覆盖：
     * 1. KinsWathe 黑客商店的三种刷新图标；
     * 2. NoellesRoles 工程师的电力恢复。</p>
     */
    private static void applySpecialEventCooldowns(ServerPlayer player) {
        Set<Item> specialEventItems = new LinkedHashSet<>();

        addRegisteredItem(specialEventItems, MOD_KINS_WATHE, "icon_weapon_cooldown_refresh");
        addRegisteredItem(specialEventItems, MOD_KINS_WATHE, "icon_ability_cooldown_refresh");
        addRegisteredItem(specialEventItems, MOD_KINS_WATHE, "icon_potion_effect_refresh");

        addRegisteredItem(specialEventItems, "noellesroles", "power_restoration");

        setItemsCooldown(player, specialEventItems, SPECIAL_EVENT_COOLDOWN_TICKS);
    }

    /**
     * 批量给一组物品施加同一段冷却。
     */
    private static void setItemsCooldown(ServerPlayer player, Set<Item> items, int cooldownTicks) {
        for (Item item : items) {
            setItemCooldown(player, item, cooldownTicks);
        }
    }

    /**
     * 对单个物品写入冷却。
     *
     * <p>这里会先读取该物品当前“真实剩余冷却 tick”：
     * 如果本来就比目标值更长，则直接保留原冷却，不会反向缩短。
     * 这样武器、疯魔、停电、特殊事件就和 ability 一样，
     * 都满足“只延长，不缩短”的安全逻辑。</p>
     */
    private static void setItemCooldown(ServerPlayer player, Item item, int cooldownTicks) {
        if (item == null) {
            return;
        }

        if (getRemainingItemCooldown(player, item) >= cooldownTicks) {
            return;
        }
        player.getCooldowns().addCooldown(item, cooldownTicks);
    }

    /**
     * 读取某个物品当前还剩多少 tick 冷却。
     *
     * <p>如果物品当前没有在冷却中，则返回 0。</p>
     */
    private static int getRemainingItemCooldown(ServerPlayer player, Item item) {
        ItemCooldownsAccessor accessor = (ItemCooldownsAccessor) player.getCooldowns();
        Object cooldownInstance = accessor.stupid_express$getCooldowns().get(item);
        if (cooldownInstance == null) {
            return 0;
        }

        int endTime = ((ItemCooldownInstanceAccessor) cooldownInstance).stupid_express$getEndTime();
        int remainingTicks = endTime - accessor.stupid_express$getTickCount();
        return Math.max(0, remainingTicks);
    }

    /**
     * 按注册表 id 查找其它模组的物品。
     *
     * <p>这样即便当前工程没有把该模组列为编译依赖，
     * 只要对方真的加载进来了，也依然能安全地追加冷却。</p>
     */
    private static void addRegisteredItem(Set<Item> items, String namespace, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return;
        }

        Item item = BuiltInRegistries.ITEM.get(id);
        if (item != null) {
            items.add(item);
        }
    }

    /**
     * 通过反射给“没有编译依赖的模组”的技能组件写入冷却。
     *
     * <p>桥接步骤统一为：
     * 1. 取出组件类里的静态 KEY；
     * 2. 用 KEY.get(player) 拿到玩家组件；
     * 3. 读取 cooldown 字段；
     * 4. 若当前冷却不足 60 秒，则直接把 cooldown 写成目标值；
     * 5. 调用 sync() 同步到客户端。</p>
     */
    private static void applyReflectedComponentCooldown(ServerPlayer player, String componentClassName, int cooldownTicks) {
        try {
            Class<?> componentClass = Class.forName(componentClassName);
            Object componentKey = readStaticField(componentClass, "KEY");
            Object component = findMethod(componentKey.getClass(), "get", 1).invoke(componentKey, player);

            if (component == null) {
                return;
            }

            int currentCooldown = readIntField(component, "cooldown");
            if (currentCooldown >= cooldownTicks) {
                return;
            }

            writeIntField(component, "cooldown", cooldownTicks);
            findMethod(componentClass, "sync", 0).invoke(component);
        } catch (ReflectiveOperationException exception) {
            reportReflectionFailure(componentClassName, exception);
        }
    }

    /**
     * 读取静态字段。
     */
    private static Object readStaticField(Class<?> owner, String fieldName) throws ReflectiveOperationException {
        return findField(owner, fieldName).get(null);
    }

    /**
     * 读取整数字段。
     */
    private static int readIntField(Object instance, String fieldName) throws ReflectiveOperationException {
        return findField(instance.getClass(), fieldName).getInt(instance);
    }

    /**
     * 写入整数字段。
     */
    private static void writeIntField(Object instance, String fieldName, int value) throws ReflectiveOperationException {
        findField(instance.getClass(), fieldName).setInt(instance, value);
    }

    /**
     * 兼容 public / private 字段查询。
     */
    private static Field findField(Class<?> owner, String fieldName) throws NoSuchFieldException {
        try {
            Field field = owner.getField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        }
    }

    /**
     * 兼容 public / private 方法查询。
     *
     * <p>这里只按“方法名 + 参数数量”寻找，
     * 是为了降低不同映射环境下的桥接脆弱度。</p>
     */
    private static Method findMethod(Class<?> owner, String methodName, int parameterCount) throws NoSuchMethodException {
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

    /**
     * 反射桥接失败时只记一次日志，防止刷屏。
     */
    private static void reportReflectionFailure(String componentClassName, ReflectiveOperationException exception) {
        if (!REPORTED_REFLECTION_FAILURES.add(componentClassName)) {
            return;
        }

        StupidExpress.LOGGER.warn(
                "召集者的跨模组技能冷却桥接失败，已跳过组件 {} 的封控处理。",
                componentClassName,
                exception
        );
    }
}
