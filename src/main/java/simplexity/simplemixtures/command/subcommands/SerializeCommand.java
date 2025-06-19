package simplexity.simplemixtures.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class SerializeCommand implements SubCommand {

    public static final String SERIALIZE_COMMAND = "simplemixtures.serialize";

    @Override
    public void subcommandTo(@NotNull LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal("serialize").requires(this::canExecute)
                .executes(this::execute));
    }

    @Override
    public int execute(@NotNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<ItemStack> itemList = new ArrayList<>();
        Player player = (Player) ctx.getSource().getSender();
        itemList.add(player.getInventory().getItemInMainHand());

        YamlConfiguration yml = new YamlConfiguration();
        yml.set("item", itemList);
        String rawYml = yml.saveToString();

        StringBuilder itemYml = new StringBuilder();
        String[] lines = rawYml.split("\n");
        for (int i = 1; i < lines.length; i++) {
            itemYml.append(lines[i].substring(2)).append("\n");
        }

        player.sendMessage(
                Component.text("SimpleMixtures")
                        .color(TextColor.color(255, 255, 85))
                        .append(Component.text(" » ")
                                .color(TextColor.color(42, 42, 42))
                        )
                        .append(Component.text("[Copy to Clipboard]")
                                .clickEvent(ClickEvent.copyToClipboard(itemYml.toString()))
                                .hoverEvent(HoverEvent.showText(Component.text(itemYml.toString())))
                                .color(TextColor.color(0, 0, 255))
                        )
        );

        return Command.SINGLE_SUCCESS;
    }

    @Override
    public boolean canExecute(@NotNull CommandSourceStack css) {
        return css.getSender() instanceof Player player && player.hasPermission(SERIALIZE_COMMAND);
    }
}
