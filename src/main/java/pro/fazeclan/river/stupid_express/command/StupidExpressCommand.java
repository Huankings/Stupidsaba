package pro.fazeclan.river.stupid_express.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.ForcedDualPersonalityManager;
import pro.fazeclan.river.stupid_express.modifier.lovers.ForcedLoversManager;

public final class StupidExpressCommand {

    private static final SimpleCommandExceptionType SAME_PLAYER_EXCEPTION =
            new SimpleCommandExceptionType(Component.translatable("commands.stupid_express.set_lovers.same_player"));
    private static final SimpleCommandExceptionType SAME_DUAL_PERSONALITY_PLAYER_EXCEPTION =
            new SimpleCommandExceptionType(Component.translatable("commands.stupid_express.set_dual_personality.same_player"));

    private StupidExpressCommand() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        /*
         * 同时注册两个根命令：
         * /stupidexpress 兼容用户最初提出的写法；
         * /stupid_express 与 mod id 保持一致，方便之后记忆和脚本调用。
         */
        dispatcher.register(createRoot("stupidexpress"));
        dispatcher.register(createRoot("stupid_express"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRoot(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("setlovers")
                        .then(Commands.argument("first", EntityArgument.player())
                                .then(Commands.argument("second", EntityArgument.player())
                                        .executes(StupidExpressCommand::setLovers))))
                .then(Commands.literal("setdual_personality")
                        .then(Commands.argument("main", EntityArgument.player())
                                .then(Commands.argument("sub", EntityArgument.player())
                                        // main/sub 的顺序有意义：main 是开局活跃的主人格，sub 是开局休眠的副人格。
                                        .executes(StupidExpressCommand::setDualPersonality))))
                .then(createConstantsNode("Constants"))
                .then(createConstantsNode("constants"))
                .then(Commands.literal("remove")
                        .then(Commands.literal("lovers")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(StupidExpressCommand::removeLovers))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createConstantsNode(String name) {
        return Commands.literal(name)
                .then(Commands.literal("minplayerspawn")
                        .then(Commands.literal("dual_personality")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(2))
                                        .executes(StupidExpressCommand::setDualPersonalityMinPlayerSpawn))));
    }

    private static int setLovers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer first = EntityArgument.getPlayer(context, "first");
        ServerPlayer second = EntityArgument.getPlayer(context, "second");
        if (first.getUUID().equals(second.getUUID())) {
            throw SAME_PLAYER_EXCEPTION.create();
        }

        ForcedLoversManager.setPendingPair(first, second);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "commands.stupid_express.set_lovers.success",
                        first.getDisplayName(),
                        second.getDisplayName()
                ),
                true
        );
        return 1;
    }

    private static int setDualPersonality(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer main = EntityArgument.getPlayer(context, "main");
        ServerPlayer sub = EntityArgument.getPlayer(context, "sub");
        if (main.getUUID().equals(sub.getUUID())) {
            throw SAME_DUAL_PERSONALITY_PLAYER_EXCEPTION.create();
        }

        /*
         * 指令只写入“下一局强制队列”。
         * 真正给玩家加词条、清随机残留和写世界组件，会在 Harpy 分配词条时由 ForcedDualPersonalityManager 消费。
         */
        ForcedDualPersonalityManager.setPendingPair(main, sub);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "commands.stupid_express.set_dual_personality.success",
                        main.getDisplayName(),
                        sub.getDisplayName()
                ),
                true
        );
        return 1;
    }

    private static int setDualPersonalityMinPlayerSpawn(CommandContext<CommandSourceStack> context) {
        int amount = IntegerArgumentType.getInteger(context, "amount");
        // 这个配置会在每局 assignModifiers 开始时重新读取，立即影响之后开的新局。
        StupidExpress.CONFIG.modifiersSection.dualPersonalitySection.dualPersonalityMinPlayerSpawn = amount;
        StupidExpress.CONFIG.save();
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.stupid_express.constants.minplayerspawn.dual_personality.success", amount),
                true
        );
        return 1;
    }

    private static int removeLovers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ServerLevel playerLevel = player.serverLevel();
        ForcedLoversManager.RemovedPair removedPair = ForcedLoversManager.removePendingOrActivePair(playerLevel, player);
        if (removedPair == null) {
            context.getSource().sendFailure(Component.translatable(
                    "commands.stupid_express.remove_lovers.not_found",
                    player.getDisplayName()
            ));
            return 0;
        }

        Component resolvedPartnerName = Component.literal(ForcedLoversManager.describePlayer(removedPair.partner()));
        ServerPlayer onlinePartner = context.getSource().getServer().getPlayerList().getPlayer(removedPair.partner());
        if (onlinePartner != null) {
            resolvedPartnerName = onlinePartner.getDisplayName();
        }
        Component partnerName = resolvedPartnerName;

        context.getSource().sendSuccess(
                () -> Component.translatable(
                        removedPair.pending()
                                ? "commands.stupid_express.remove_lovers.pending_success"
                                : "commands.stupid_express.remove_lovers.active_success",
                        player.getDisplayName(),
                        partnerName
                ),
                true
        );
        return 1;
    }
}
