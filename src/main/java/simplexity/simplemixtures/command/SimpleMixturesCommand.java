package simplexity.simplemixtures.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import simplexity.simplemixtures.command.subcommands.ReloadCommand;
import simplexity.simplemixtures.command.subcommands.SerializeCommand;

@SuppressWarnings("UnstableApiUsage")
public class SimpleMixturesCommand {

    public static final String COMMAND_PERMISSION = "simplemixtures.command";

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("simplemixtures")
                .requires(src -> src.getSender().hasPermission(COMMAND_PERMISSION));
        new SerializeCommand().subcommandTo(builder);
        new ReloadCommand().subcommandTo(builder);
        return builder;
    }

}
