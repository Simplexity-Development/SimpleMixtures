package simplexity.simplemixtures.config;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import io.papermc.paper.potion.PotionMix;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import simplexity.simplemixtures.SimpleMixtures;

import java.util.*;

public class Config {

    private enum ChoiceType {
        EXACT, MATERIAL
    }

    private static Config instance;
    private static Set<NamespacedKey> recipesAdded = new HashSet<>();

    private final Map<Material, Material> brewingLeftover = new HashMap<>();

    public static @NotNull Config getInstance() {
        if (instance == null) instance = new Config();
        return instance;
    }

    private Config() {
        reload();
    }

    public void reload() {
        SimpleMixtures plugin = SimpleMixtures.getPlugin();
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        clearRecipes();
        brewingLeftover.clear();
        registerLeftovers(config);
        registerRecipes(config);
    }

    public @Nullable Material getBrewingLeftover(@NotNull Material ingredient) {
        return brewingLeftover.getOrDefault(ingredient, null);
    }

    private void registerLeftovers(FileConfiguration config) {
        ConfigurationSection leftovers = config.getConfigurationSection("leftovers");
        if (leftovers != null) {
            for (String leftover : leftovers.getKeys(false)) {
                Material leftoverMaterial = Material.getMaterial(leftover);
                if (leftoverMaterial == null) continue;

                for (String ingredient : leftovers.getStringList(leftover)) {
                    Material ingredientMaterial = Material.getMaterial(ingredient);
                    if (ingredientMaterial == null) continue;

                    brewingLeftover.put(ingredientMaterial, leftoverMaterial);
                }
            }
        }
    }

    private void registerRecipes(FileConfiguration config) {
        ConfigurationSection recipesConfiguration = config.getConfigurationSection("recipes");
        if (recipesConfiguration == null) return;
        recipesConfiguration.getKeys(false)
                .forEach(potionKey -> registerPotion(recipesConfiguration, potionKey));
    }

    private void registerPotion(ConfigurationSection recipes, String key) {
        ConfigurationSection potionConfig = recipes.getConfigurationSection(key);
        if (potionConfig == null) return;

        RecipeChoice input = getRecipeChoice(potionConfig.getConfigurationSection("input"));
        if (input == null) return;
        RecipeChoice ingredient = getRecipeChoice(potionConfig.getConfigurationSection("ingredient"));
        if (ingredient == null) return;
        ItemStack result = potionConfig.getItemStack("result");
        if (result == null) return;
        NamespacedKey namespacedKey = new NamespacedKey(SimpleMixtures.getPlugin(), key);

        PotionMix potionMix = new PotionMix(namespacedKey, result, input, ingredient);
        Bukkit.getServer().getPotionBrewer().addPotionMix(potionMix);
        recipesAdded.add(namespacedKey);
    }

    private RecipeChoice getRecipeChoice(ConfigurationSection recipeChoiceConfig) {
        if (recipeChoiceConfig == null) return null;
        ChoiceType type;
        try {
            type = ChoiceType.valueOf(recipeChoiceConfig.getString("match_type"));
        } catch (IllegalArgumentException e) {
            return null;
        }

        try {
            return switch (type) {
                case EXACT ->
                        new RecipeChoice.ExactChoice((List<ItemStack>) recipeChoiceConfig.getList("arguments", new ArrayList<ItemStack>()));
                case MATERIAL ->
                        new RecipeChoice.MaterialChoice(getMaterialList(recipeChoiceConfig.getStringList("arguments")));
            };
        }
        catch (ClassCastException ignored) {
            SimpleMixtures.getPlugin().getLogger().warning("[SimpleMixtures] Configuration at " + recipeChoiceConfig.getCurrentPath() + " is not valid.");
            return null;
        }
    }

    private List<Material> getMaterialList(List<String> materials) {
        List<Material> materialsList = new ArrayList<>();

        for (String material : materials) {
            try {
                materialsList.add(Material.valueOf(material.toUpperCase()));
            }
            catch (IllegalArgumentException ignored) { }
        }

        return materialsList;
    }

    private void clearRecipes() {
        for (NamespacedKey recipe : recipesAdded) {
            Bukkit.getServer().getPotionBrewer().removePotionMix(recipe);
        }
        recipesAdded.clear();
    }

}
