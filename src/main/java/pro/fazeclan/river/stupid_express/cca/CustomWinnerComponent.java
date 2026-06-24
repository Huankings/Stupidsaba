package pro.fazeclan.river.stupid_express.cca;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomWinnerComponent implements AutoSyncedComponent {

    public static final ComponentKey<CustomWinnerComponent> KEY =
            ComponentRegistry.getOrCreate(StupidExpress.id("custom_winner"), CustomWinnerComponent.class);
    private final Level level;

    @Getter
    @Setter
    private String winningTextId = null;

    @Getter
    @Setter
    private int color = 0x000000;

    private List<UUID> winnerUuids = new ArrayList<>();

    public CustomWinnerComponent(Level level) {
        this.level = level;
    }

    public boolean hasCustomWinner() {
        return this.winningTextId != null;
    }

    public void sync() {
        CustomWinnerComponent.KEY.sync(this.level);
    }

    public void reset() {
        this.winningTextId = null;
        this.color = 0x000000;
        this.winnerUuids = new ArrayList<>();
        sync();
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.winningTextId = tag.contains("winning_text") ? tag.getString("winning_text") : null;
        /*
         * 自定义胜利者必须用 UUID 做持久数据源，不能在反序列化时立刻转成 Player 实体。
         *
         * 原来的实现会在这里调用 level.getPlayerByUUID(uuid)，然后过滤掉 null。
         * 这在结算界面里会有一个很隐蔽的问题：如果纵火犯 / 召集者 / 小偷等单独胜利者
         * 在胜利后退出游戏，客户端或服务端当前世界里就查不到对应 Player 实体，
         * 于是胜利者列表会被读成空列表。后续结算分组再判断“谁是胜利者”时，
         * 这个离线玩家就会被错误归进“其他”阵营，导致右侧胜利职业栏为空。
         *
         * 因此这里只保存 UUID；需要临时拿在线 Player 的旧接口会按需再查实体，
         * 但所有胜负判断和结算分组都必须走 isWinner(UUID)。
         */
        this.winnerUuids = tag.contains("winners")
                ? uuidListFromTag(tag, "winners")
                : new ArrayList<>();
        this.color = tag.contains("color") ? tag.getInt("color") : 0x000000;
    }

    private ArrayList<UUID> uuidListFromTag(CompoundTag tag, String listName) {
        ArrayList<UUID> result = new ArrayList<>();
        for (Tag e : tag.getList(listName, Tag.TAG_INT_ARRAY)) {
            result.add(NbtUtils.loadUUID(e));
        }
        return result;
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        if (this.winningTextId != null) {
            tag.putString("winning_text", this.winningTextId);
        }
        tag.put("winners", tagFromUuidList(this.winnerUuids));
        tag.putInt("color", this.color);
    }

    private ListTag tagFromUuidList(List<UUID> list) {
        ListTag ret = new ListTag();
        for (UUID player : list) {
            ret.add(NbtUtils.createUUID(player));
        }
        return ret;
    }

    public List<UUID> getWinnerUuids() {
        return this.winnerUuids;
    }

    public void setWinnerUuids(List<UUID> winnerUuids) {
        this.winnerUuids = new ArrayList<>(winnerUuids);
    }

    public boolean isWinner(UUID uuid) {
        return uuid != null && this.winnerUuids.contains(uuid);
    }

    public List<Player> getWinners() {
        /*
         * 保留旧的 getWinners() 形状，方便现有代码或其他扩展继续临时取在线玩家实体。
         * 注意：这个方法只适合“当前在线实体”场景，不能再用于结算归类。
         * 结算归类请使用 getWinnerUuids() 或 isWinner(UUID)，这样玩家离线后仍能被识别为胜利者。
         */
        return this.winnerUuids.stream()
                .map(this.level::getPlayerByUUID)
                .filter(player -> player != null)
                .toList();
    }

    public void setWinners(List<? extends Player> winners) {
        /*
         * 对外继续接受原来的 Player 列表调用方式；内部立即转换成 UUID。
         * 这样纵火犯、召集者、小偷等触发胜利的旧调用点不需要关心底层存储变化，
         * 同时又能保证胜利玩家退出游戏后，结算页面仍能用 UUID 找回正确归属。
         */
        this.winnerUuids = new ArrayList<>(winners.stream()
                .map(Player::getUUID)
                .toList());
    }
}
