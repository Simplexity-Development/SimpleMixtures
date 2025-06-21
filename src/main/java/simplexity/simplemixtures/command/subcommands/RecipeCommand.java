package simplexity.simplemixtures.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.potion.PotionMix;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;
import simplexity.simplemixtures.config.Config;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class RecipeCommand implements SubCommand {

    public static final String RECIPE_COMMAND = "simplemixtures.recipe";

    @Override
    public void subcommandTo(@NotNull LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal("recipe").requires(this::canExecute).executes(this::execute));
    }

    @Override
    public int execute(@NotNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Map<NamespacedKey,PotionMix> recipe = Config.getInstance().getRecipes();
        Map<String, RecipeChoice> recipeChoices = Config.getInstance().getRecipeChoices();
        Map<String, ItemStack> itemStacks = Config.getInstance().getItemStacks();

        StringBuilder sb = new StringBuilder("Recipes:");
        for (NamespacedKey key : recipe.keySet()) {
            sb.append("\n  ").append(key);
        }

        sb.append("\n\nRecipe Choices:");
        for (String key : recipeChoices.keySet()) {
            sb.append("\n  ").append(key);
        }

        sb.append("\n\nItem Stacks:");
        for (String key : itemStacks.keySet()) {
            sb.append("\n  ").append(key);
        }
        ctx.getSource().getSender().sendPlainMessage(sb.toString());

        return Command.SINGLE_SUCCESS;
    }

    @Override
    public boolean canExecute(@NotNull CommandSourceStack css) {
        return css.getSender().hasPermission(RECIPE_COMMAND);
    }
}
