package fengliu.peca.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GameModeSuggestionProvider implements SuggestionProvider<ServerCommandSource> {

    public static final Collection<String> gameModeList = List.of("creative", "survival", "adventure", "spectator");

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        for (String gamemode : gameModeList) {
            builder.suggest(gamemode);
        }
        return builder.buildFuture();
    }
}
