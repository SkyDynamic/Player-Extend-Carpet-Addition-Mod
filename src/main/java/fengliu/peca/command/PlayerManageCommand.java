package fengliu.peca.command;

import carpet.patches.EntityPlayerMPFake;
import carpet.settings.SettingsManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fengliu.peca.PecaSettings;
import fengliu.peca.player.sql.PlayerData;
import fengliu.peca.player.sql.PlayerSql;
import fengliu.peca.util.CommandUtil;
import fengliu.peca.util.GameModeSuggestionProvider;
import fengliu.peca.util.Page;
import fengliu.peca.util.TextClickUtil;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.text.*;
import net.minecraft.world.GameMode;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PlayerManageCommand {
    private static final LiteralArgumentBuilder<ServerCommandSource> PlayerManageCmd = literal("playerManage")
            .requires((player) -> SettingsManager.canUseCommand(player, PecaSettings.commandPlayerManage));

    public static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher) {
        PlayerManageCmd
            .then(literal("list").executes(c -> find(c, PlayerSql::readPlayer)))
            .then(literal("clone").then(argument("purpose", StringArgumentType.string())
                .executes(PlayerManageCommand::clonePlayer)
                .then(literal("to").then(argument("name", StringArgumentType.string()).executes(PlayerManageCommand::clonePlayer))
                .then(literal("in").then(argument("gamemode", StringArgumentType.string()).suggests(new GameModeSuggestionProvider())
                    .executes(PlayerManageCommand::clonePlayer)
                    .then(literal("to").then(argument("name", StringArgumentType.string()).executes(PlayerManageCommand::clonePlayer)))))))

            .then(literal("find")

                .then(argument("name", StringArgumentType.string())
                    .executes(c -> find(c, PlayerSql::readPlayer))
                    .then(makeFindAtCommand())
                    .then(makeFindInCommand())
                    .then(makeFindInDimensionCommand()))

                .then(literal("gamemode").then(argument("gamemode", StringArgumentType.string()).suggests(new GameModeSuggestionProvider())
                    .executes(c -> find(c, PlayerSql::readPlayer))
                    .then(makeFindIsCommand())
                    .then(makeFindAtCommand())
                    .then(makeFindInDimensionCommand())))

                .then(literal("pos").then(argument("pos", Vec3ArgumentType.vec3())
                    .executes(c -> find(c, PlayerSql::readPlayer))
                    .then(literal("inside")
                        .then(argument("offset", IntegerArgumentType.integer(0))
                            .executes(c -> find(c, PlayerSql::readPlayer))
                            .then(makeFindIsCommand())
                            .then(makeFindInCommand())
                            .then(makeFindInDimensionCommand())))))

                .then(literal("dimension").then(argument("dimension", DimensionArgumentType.dimension())
                    .executes(c -> find(c, PlayerSql::readPlayer))
                    .then(makeFindIsCommand())
                    .then(makeFindAtCommand())
                    .then(makeFindInCommand())))

            .then(argument("player", EntityArgumentType.players())
                .then(literal("save").then(argument("purpose", StringArgumentType.string()).executes(PlayerManageCommand::save)))
                .then(literal("info").executes(PlayerManageCommand::info)))
            .then(literal("id").then(argument("id", LongArgumentType.longArg(0))
                .then(literal("info").executes(PlayerManageCommand::infoId))
                .then(literal("delete").executes(PlayerManageCommand::delete))
                .then(literal("execute")
                    .executes(PlayerManageCommand::execute)
                    .then(literal("add").then(argument("command", StringArgumentType.string())
                        .executes(PlayerManageCommand::executeAdd)))
                    .then(literal("del").then(argument("index", IntegerArgumentType.integer(1))
                        .executes(PlayerManageCommand::executeDel)))
                    .then(literal("set").then(argument("index", IntegerArgumentType.integer(1))
                        .then(argument("command", StringArgumentType.string()).executes(PlayerManageCommand::executeSet))))
                    .then(literal("clear").executes(PlayerManageCommand::executeClear)))))));

        dispatcher.register(PlayerManageCmd);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFindIsCommand() {
        return literal("is").then(argument("name", StringArgumentType.string())
                .executes(c -> find(c, PlayerSql::readPlayer)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFindAtCommand() {
        return literal("at").then(argument("pos", Vec3ArgumentType.vec3())
                .executes(c -> find(c, PlayerSql::readPlayer))
                .then(literal("inside")
                        .then(argument("offset", IntegerArgumentType.integer(0))
                                .executes(c -> find(c, PlayerSql::readPlayer)))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFindInCommand() {
        return literal("in").then(argument("gamemode", StringArgumentType.string()).suggests(new GameModeSuggestionProvider())
                .executes(c -> find(c, PlayerSql::readPlayer)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFindInDimensionCommand() {
        return literal("in").then(argument("dimension", DimensionArgumentType.dimension())
                .executes(c -> find(c, PlayerSql::readPlayer)));
    }

    private static String getLoggedText(ServerCommandSource context, PlayerData playerData) {
        String loggedText;
        ServerPlayerEntity player = context.getServer().getPlayerManager().getPlayer(playerData.name());
        if (player == null) {
            loggedText = "peca.info.command.player.not.logged";
        } else if (!(player instanceof EntityPlayerMPFake)) {
            loggedText = "peca.info.command.player.not.fake";
        } else {
            loggedText = "peca.info.command.player.logged";
        }
        return loggedText;
    }

    public static class PlayerPage extends Page<PlayerData> {

        public PlayerPage(ServerCommandSource context, List<PlayerData> data) {
            super(context, data);
        }

        @Override
        public List<MutableText> putPageData(PlayerData pageData, int index) {
            List<MutableText> texts = new ArrayList<>();
            texts.add(new LiteralText(String.format("[%s] ", index))
                    .append(pageData.name())
                    .append(" - ")
                    .append(new TranslatableText(getLoggedText(this.context, pageData)))
                    .append(" ")
                    .append(new LiteralText(pageData.dimension().getPath() + " ").setStyle(Style.EMPTY.withColor(0xAAAAAA)))
                    .append(new LiteralText(String.format("x: %d y: %d z:%d", (int) pageData.pos().x, (int) pageData.pos().y, (int) pageData.pos().z)).setStyle(Style.EMPTY.withColor(0x00AAAA)))
            );
            texts.add(new TranslatableText("peca.info.command.player.purpose", "§6" + pageData.purpose())
                    .append(TextClickUtil.runText(new TranslatableText("peca.info.command.player.spawn"), String.format(
                            "/player %s spawn at %g %g %g facing %g %g in %s in %s",
                            pageData.name(),
                            pageData.pos().x,
                            pageData.pos().y,
                            pageData.pos().z,
                            pageData.yaw(),
                            pageData.pitch(),
                            pageData.dimension().getPath(),
                            pageData.gamemode().getName()
                    )))
                    .append(TextClickUtil.runText(new TranslatableText("peca.info.command.player.kill"), String.format("/player %s kill", pageData.name())))
                    .append(TextClickUtil.runText(new TranslatableText("peca.info.command.player.info"), String.format("/playerManage id %s info", pageData.id())))
                    .append(TextClickUtil.runText(new TranslatableText("peca.info.command.player.delete"), String.format("/playerManage id %s delete", pageData.id())))
            );
            return texts;
        }
    }

    private static int clonePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String purpose = StringArgumentType.getString(context, "purpose");
        if (purpose.isEmpty()) {
            context.getSource().sendError(new TranslatableText("peca.info.command.error.not.bot.purpose"));
            return -1;
        }

        PlayerData data= PlayerData.fromPlayer(context.getSource().getPlayer());
        String name = CommandUtil.getArgOrDefault(() -> StringArgumentType.getString(context, "name"), data.name());
        GameMode gamemode = CommandUtil.getArgOrDefault(() -> GameMode.byName(StringArgumentType.getString(context, "gamemode")), data.gamemode());

        CommandUtil.booleanPrintMsg(PlayerSql.savePlayer(new PlayerData(
                        data.id(), name, data.dimension(), data.pos(), data.yaw(), data.pitch(), gamemode, data.flying(),
                        data.execute(), data.purpose(), data.createTime(), data.createPlayerUuid(), data.lastModifiedTime(), data.lastModifiedPlayerUuid()
                ), context.getSource().getPlayer(), purpose),
                new TranslatableText("peca.info.command.player.save", name),
                new TranslatableText("peca.info.command.player.save", name),
                context
        );
        return Command.SINGLE_SUCCESS;
    }

    interface Find {
        List<PlayerData> run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException;
    }

    private static int find(CommandContext<ServerCommandSource> context, Find find) throws CommandSyntaxException {
        List<PlayerData> lists = null;
        try {
            lists = find.run(context);
        } catch (CommandSyntaxException e) {
            context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.find.empty"), false);
            return -1;
        }

        if (lists.isEmpty()) {
            context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.find.empty"), false);
            return -1;
        }

        Page<?> page = new PlayerPage(context.getSource(), lists);
        PecaCommand.addPage(Objects.requireNonNull(context.getSource().getPlayer()), page);
        page.look();
        return Command.SINGLE_SUCCESS;
    }

    private static int save(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String purpose = StringArgumentType.getString(context, "purpose");
        if (purpose.isEmpty()) {
            context.getSource().sendError(new TranslatableText("peca.info.command.error.not.bot.purpose"));
            return -1;
        }

        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        CommandUtil.booleanPrintMsg(PlayerSql.savePlayer(player, context.getSource().getPlayer(), purpose),
                new TranslatableText("peca.info.command.player.save", player.getName()),
                new TranslatableText("peca.info.command.player.save", player.getName()),
                context
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int delete(CommandContext<ServerCommandSource> context) {
        long id = LongArgumentType.getLong(context, "id");
        CommandUtil.booleanPrintMsg(PlayerSql.deletePlayer(id),
                new TranslatableText("peca.info.command.player.delete.info", id),
                new TranslatableText("peca.info.command.error.player.delete", id),
                context
        );
        return Command.SINGLE_SUCCESS;
    }

    private static void printInfo(CommandContext<ServerCommandSource> context, PlayerData playerData) {
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.name", playerData.name() + " - " + new TranslatableText(getLoggedText(context.getSource(), playerData)).getString()), false);
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.dimension", playerData.dimension().getPath()).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.pos", playerData.pos().x, playerData.pos().y, playerData.pos().z).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.pos.overworld", playerData.pos().x * 8, playerData.pos().y * 8, playerData.pos().z * 8).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.yaw", playerData.yaw()).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.pitch", playerData.pitch()).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.gamemode", playerData.gamemode().getName()).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        if (playerData.id() == -1) {
            return;
        }

        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.execute.info",
                playerData.execute().toString().replace("[", "§3[").replace("]", "§3]").replace(",", "§3,").replace("\"", "§6\"")), false);
        ServerPlayerEntity createPlayer = context.getSource().getServer().getPlayerManager().getPlayer(playerData.createPlayerUuid());
        if (createPlayer != null) {
            context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.create.player", createPlayer.getName()).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        }
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.create.player.uuid", playerData.createPlayerUuid()).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.create.time", playerData.createTime()).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        ServerPlayerEntity lastModifiedPlayer = context.getSource().getServer().getPlayerManager().getPlayer(playerData.lastModifiedPlayerUuid());
        if (lastModifiedPlayer != null) {
            context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.last.modified.player", lastModifiedPlayer.getName()).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        }
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.last.modified.player.uuid", playerData.lastModifiedPlayerUuid()).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.last.modified.time", playerData.lastModifiedTime()).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.purpose", playerData.purpose()), false);
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.id", playerData.id()).setStyle(Style.EMPTY.withColor(0x00AAAA)), false);
        context.getSource().sendFeedback(new TranslatableText("peca.info.command.player.data.info").setStyle(Style.EMPTY.withColor(0xFF5555)), false);
        context.getSource().sendFeedback(
                TextClickUtil.suggestText(new TranslatableText("peca.info.command.player.spawn.suggest"), String.format(
                                "/player %s spawn at %g %g %g facing %g %g in %s in %s",
                                playerData.name(),
                                playerData.pos().x,
                                playerData.pos().y,
                                playerData.pos().z,
                                playerData.yaw(),
                                playerData.pitch(),
                                playerData.dimension().getPath(),
                                playerData.gamemode().getName()
                        ))
                        .append(TextClickUtil.runText(new TranslatableText("peca.info.command.player.execute"), String.format("/playerManage id %s execute", playerData.id())))
                        .append(TextClickUtil.runText(new TranslatableText("peca.info.command.player.stop"), String.format("/player %s stop", playerData.name())))
                        .append(TextClickUtil.runText(new TranslatableText("peca.info.command.player.kill"), String.format("/player %s kill", playerData.name())))
                        .append(TextClickUtil.runText(new TranslatableText("peca.info.command.player.delete"), String.format("/playerManage id %s delete", playerData.id()))), false);
    }

    private static int info(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        if (!(player instanceof EntityPlayerMPFake)) {
            context.getSource().sendError(new TranslatableText(""));
            return -1;
        }

        printInfo(context, PlayerData.fromPlayer(EntityArgumentType.getPlayer(context, "player")));
        return Command.SINGLE_SUCCESS;
    }

    private static int infoId(CommandContext<ServerCommandSource> context) {
        printInfo(context, PlayerSql.readPlayer(LongArgumentType.getLong(context, "id")));
        return Command.SINGLE_SUCCESS;
    }

    interface Execute {
        JsonArray run(JsonArray executeArray);
    }

    private static int setExecute(CommandContext<ServerCommandSource> context, Execute execute, Text errorText) {
        long id = LongArgumentType.getLong(context, "id");
        PlayerData playerData = PlayerSql.readPlayer(id);
        JsonArray newArray = execute.run(playerData.execute());
        if (newArray == null) {
            return Command.SINGLE_SUCCESS;
        }

        if (!PlayerSql.executeUpdate(id, newArray)) {
            context.getSource().sendError(errorText);
            return -1;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        return setExecute(context, executeArray -> {
            executeArray.forEach(command -> {
                context.getSource().getServer().getCommandManager().execute(context.getSource(), command.getAsString());
            });
            return executeArray;
        }, null);
    }

    private static int executeAdd(CommandContext<ServerCommandSource> context) {
        String command = StringArgumentType.getString(context, "command");
        if (!command.startsWith("/player")) {
            context.getSource().sendError(new TranslatableText("peca.info.command.error.execute.add.starts"));
            return -1;
        }

        return setExecute(context, executeArray -> {
            executeArray.add(command);
            return executeArray;
        }, new TranslatableText("peca.info.command.error.execute.add"));
    }

    private static int executeDel(CommandContext<ServerCommandSource> context) {
        int index = IntegerArgumentType.getInteger(context, "index") - 1;
        return setExecute(context, executeArray -> {
            if (index > executeArray.size()) {
                return executeArray;
            }
            executeArray.remove(index);
            return executeArray;
        }, new TranslatableText("peca.info.command.error.execute.del"));
    }

    private static int executeSet(CommandContext<ServerCommandSource> context) {
        int index = IntegerArgumentType.getInteger(context, "index") - 1;
        String command = StringArgumentType.getString(context, "command");
        if (!command.startsWith("/player")) {
            context.getSource().sendError(new TranslatableText("peca.info.command.error.execute.add.starts"));
            return -1;
        }

        return setExecute(context, executeArray -> {
            if (index > executeArray.size()) {
                return executeArray;
            }
            executeArray.set(index, new JsonPrimitive(command));
            return executeArray;
        }, new TranslatableText("peca.info.command.error.execute.set"));
    }

    private static int executeClear(CommandContext<ServerCommandSource> context) {
        return setExecute(context, executeArray -> JsonParser.parseString("[]").getAsJsonArray(), new TranslatableText("peca.info.command.error.execute.clear"));
    }
}
