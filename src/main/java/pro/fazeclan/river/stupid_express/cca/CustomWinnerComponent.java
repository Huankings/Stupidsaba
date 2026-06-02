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
import java.util.Objects;
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

    @Getter
    @Setter
    private List<Player> winners = new ArrayList<>();

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
        this.winners = new ArrayList<>();
        sync();
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.winningTextId = tag.contains("winning_text") ? tag.getString("winning_text") : null;
        this.winners = tag.contains("winners")
                ? uuidListFromTag(tag, "winners").stream().map(this.level::getPlayerByUUID).filter(Objects::nonNull).toList()
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
        tag.put("winners", tagFromUuidList(this.winners.stream().map(Player::getUUID).toList()));
        tag.putInt("color", this.color);
    }

    private ListTag tagFromUuidList(List<UUID> list) {
        ListTag ret = new ListTag();
        for (UUID player : list) {
            ret.add(NbtUtils.createUUID(player));
        }
        return ret;
    }
}
