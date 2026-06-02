package pro.fazeclan.river.stupid_express.shop;

/**
 * 让通用商店逻辑能操作 PlayerShopComponent 的最小适配接口。
 *
 * <p>之所以不用直接在工具类里访问 mixin 的 shadow 字段，
 * 是为了把“购买流程”和“Mixin 注入细节”分离开，方便后续复用。</p>
 */
public interface PlayerShopComponentAccessor {

    int stupid_express$getBalance();

    void stupid_express$setBalance(int balance);

    void stupid_express$sync();
}
