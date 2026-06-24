package pro.fazeclan.river.stupid_express.role.thief.packet;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.cca.AbilityCooldownComponent;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.record.StupidExpressReplay;
import pro.fazeclan.river.stupid_express.role.thief.ThiefItemRules;

import java.util.UUID;

public record ThiefTakeItemC2SPacket(UUID targetUuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ThiefTakeItemC2SPacket> ID = 
        new CustomPacketPayload.Type<>(
            StupidExpress.id("thief_take_item")
        );
    
    public static final StreamCodec<FriendlyByteBuf, ThiefTakeItemC2SPacket> CODEC = StreamCodec.of(
        (buf, value) -> buf.writeUUID(value.targetUuid),
        buf -> new ThiefTakeItemC2SPacket(buf.readUUID())
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public final static int THIEF_COOLDOWN = 70 * 20;
    
    public static void handle(ThiefTakeItemC2SPacket payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        context.server().execute(() -> {
            ServerPlayer target = context.server().getPlayerList().getPlayer(payload.targetUuid);
            if (target != null) {
                handleThiefTakeItem(player, target);
            }
        });
    }
    
    private static void handleThiefTakeItem(ServerPlayer thief, ServerPlayer target) {
        AbilityCooldownComponent abilityCooldownComponent = AbilityCooldownComponent.KEY.get(thief);
        
        // Validation call
        if (!validateStealAttempt(thief, target, abilityCooldownComponent)) {
            return;
        }
        /*
         * 只有在服务端验证通过、这次偷窃真正成立时，才记录“尝试偷取”事件。
         * 这样可以避免冷却中、距离不够等无效交互也污染回放。
         */
        CompoundTag attemptExtra = new CompoundTag();
        attemptExtra.putUUID("target_player", target.getUUID());
        GameRecordManager.recordGlobalEvent(thief.serverLevel(), StupidExpressReplay.THIEF_ATTEMPT_EVENT, thief, attemptExtra);

        // Count stealable items first
        int count = 0;
        for (ItemStack stack : target.getInventory().items) {
            if (!stack.isEmpty() && ThiefItemRules.canTake(stack.getItem())) {
                count++;
            }
        }
        
        // If no item found, apply half cooldown and notify
        if (count == 0) {
            abilityCooldownComponent.setCooldown(THIEF_COOLDOWN / 5);
            abilityCooldownComponent.sync();
            thief.sendSystemMessage(
                Component.literal("§e" + target.getName().getString() + "§c has no items you can steal!")
            );
            CompoundTag failExtra = new CompoundTag();
            failExtra.putUUID("target_player", target.getUUID());
            GameRecordManager.recordGlobalEvent(thief.serverLevel(), StupidExpressReplay.THIEF_FAIL_EVENT, thief, failExtra);

            thief.playNotifySound(
                SoundEvents.DYE_USE,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
            );
            return;
        }
        
        // Pick a random item
        int targetIndex = thief.getRandom().nextInt(count);
        int currentIndex = 0;
        int slotIndex = -1;
        ItemStack stolenItem = ItemStack.EMPTY;
        
        for (int i = 0; i < target.getInventory().items.size(); i++) {
            ItemStack stack = target.getInventory().items.get(i);
            if (!stack.isEmpty() && ThiefItemRules.canTake(stack.getItem())) {
                if (currentIndex == targetIndex) {
                    slotIndex = i;
                    stolenItem = stack.copy(); // Use copy to avoid using original stack
                    break;
                }
                currentIndex++;
            }
        }
        
        // Take the item
        target.getInventory().items.set(slotIndex, ItemStack.EMPTY);
        
        /*
         * 这里先额外拷贝一份“纯记录用”的物品快照。
         *
         * 原版 Inventory#add 会直接修改传入的 ItemStack：
         * 1. 如果成功塞进背包，count 可能会被扣到 0；
         * 2. 变空后的 stack 再去取物品名时，就会退化成 AIR。
         *
         * 这正是之前回放里显示成“[空气]”的根因。
         * 因此后续回放数据必须基于这份未被消费的快照来写。
         */
        ItemStack replaySnapshot = stolenItem.copy();

        // Give to thief
        if (!thief.getInventory().add(stolenItem)) {
            // Drop if inventory full
            ItemEntity itemEntity = new ItemEntity(
                thief.level(),
                thief.getX(),
                thief.getY(),
                thief.getZ(),
                stolenItem
            );
            thief.level().addFreshEntity(itemEntity);
        }
        
        // Success - full cooldown, message, sound
        abilityCooldownComponent.setCooldown(THIEF_COOLDOWN);
        abilityCooldownComponent.sync();
        CompoundTag successExtra = new CompoundTag();
        successExtra.putUUID("target_player", target.getUUID());
        successExtra.putString("item", BuiltInRegistries.ITEM.getKey(replaySnapshot.getItem()).toString());
        successExtra.putString("item_name", Component.Serializer.toJson(replaySnapshot.getHoverName(), thief.registryAccess()));
        GameRecordManager.recordGlobalEvent(thief.serverLevel(), StupidExpressReplay.THIEF_SUCCESS_EVENT, thief, successExtra);
        thief.sendSystemMessage(
            Component.literal("§aYou stole from §e" + target.getName().getString())
        );

        thief.playNotifySound(
            SoundEvents.ARMOR_EQUIP_CHAIN.value(),
            SoundSource.PLAYERS,
            1.0F,
            1.0F
        );
    }

    public static boolean validateStealAttempt(Player thief, Player target, AbilityCooldownComponent abilityCooldownComponent) {
        // Null/removed checks
        if (thief == null || target == null) return false;
        if (thief.isRemoved() || target.isRemoved()) return false;

        // Role check
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(thief.level());
        if (!gameComponent.isRole(thief, SERoles.THIEF)) return false;

        // Game state checks
        if (GameFunctions.isPlayerEliminated(target) || 
            GameFunctions.isPlayerEliminated(thief)) return false;
        
        // Self steal check
        if (thief.getUUID().equals(target.getUUID())) return false;
        
        // World check
        if (!thief.level().dimension().equals(target.level().dimension())) return false;
        
        // Cooldown check
        if (abilityCooldownComponent.hasCooldown()) return false;
        
        // Distance check
        if (!validateDistance(thief, target)) {
            return false;
        }
        
        // Line of sight check
        if (!thief.hasLineOfSight(target)) return false;
        
        return true;
    }

    public static boolean validateDistance(Player thief, Player target) {
        // Null/removed checks
        if (thief == null || target == null) return false;
        if (thief.isRemoved() || target.isRemoved()) return false;

        // Check if in same world/dimension
        if (!thief.level().dimension().equals(target.level().dimension())) return false;

        // Distance check
        double distance = thief.distanceTo(target);
        
        // Add leniency to server side check
        if (thief.level().isClientSide) {
            if (distance > 1.0) return false;
        } else {
            if (distance > 1.2) return false;
        }
        
        return true;
    }
    
    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        
        // Use method reference to static handle method
        // Before I was doing it wrong by using a non static handle method ¯\_(ツ)_/¯
        ServerPlayNetworking.registerGlobalReceiver(ID, ThiefTakeItemC2SPacket::handle);
    }
}
