package simplexity.simplemixtures.config;

import io.papermc.paper.potion.PotionMix;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import simplexity.simplemixtures.SimpleMixtures;

import java.util.*;

public class Config {

    private enum ChoiceType {
        EXACT, MATERIAL
    }

    private static Config instance;
    private static final Map<NamespacedKey, PotionMix> recipesAdded = new HashMap<>();
    private static final Map<Material, Material> brewingLeftover = new HashMap<>();
    private static final Map<String, RecipeChoice> recipeChoices = new HashMap<>();
    private static final Map<String, ItemStack> itemStacks = new HashMap<>();

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
        recipeChoices.clear();
        itemStacks.clear();

        registerItemStacks(config);
        registerRecipeChoices(config);
        registerRecipes(config);
        registerLeftovers(config);
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

        RecipeChoice input = potionConfig.isConfigurationSection("input")
                ? getRecipeChoice(potionConfig.getConfigurationSection("input"))
                : recipeChoices.getOrDefault(potionConfig.getString("input"), null);
        if (input == null) return;
        RecipeChoice ingredient = potionConfig.isConfigurationSection("ingredient")
                ? getRecipeChoice(potionConfig.getConfigurationSection("ingredient"))
                : recipeChoices.getOrDefault(potionConfig.getString("ingredient"), null);
        if (ingredient == null) return;
        ItemStack result = potionConfig.isItemStack("result")
                ? potionConfig.getItemStack("result")
                : itemStacks.getOrDefault(potionConfig.getString("result"), null);
        if (result == null) return;
        NamespacedKey namespacedKey = new NamespacedKey(SimpleMixtures.getPlugin(), key);

        PotionMix potionMix = new PotionMix(namespacedKey, result, input, ingredient);
        Bukkit.getServer().getPotionBrewer().addPotionMix(potionMix);
        recipesAdded.put(namespacedKey, potionMix);
    }

    private void registerRecipeChoices(FileConfiguration config) {
        ConfigurationSection placeholderConfig = config.getConfigurationSection("recipe_choices");
        if (placeholderConfig == null) return;
        for (String key : placeholderConfig.getKeys(false)) {
            RecipeChoice choice = getRecipeChoice(placeholderConfig.getConfigurationSection(key));
            if (choice == null) continue;
            recipeChoices.put(key, choice);
        }
    }

    private void registerItemStacks(FileConfiguration config) {
        ConfigurationSection placeholderConfig = config.getConfigurationSection("item_stacks");
        if (placeholderConfig == null) return;
        for (String key : placeholderConfig.getKeys(false)) {
            if (!placeholderConfig.isItemStack(key)) continue;
            itemStacks.put(key, placeholderConfig.getItemStack(key));
        }
    }

    private RecipeChoice getRecipeChoice(ConfigurationSection recipeChoiceConfig) {
        if (recipeChoiceConfig == null) return null;
        ChoiceType type;
        try {
            type = ChoiceType.valueOf(recipeChoiceConfig.getString("match_type"));
        } catch (IllegalArgumentException e) {
            return null;
        }

        return switch (type) {
            case EXACT ->
                    new RecipeChoice.ExactChoice(getExactChoices(recipeChoiceConfig.getList("arguments")));
            case MATERIAL ->
                    new RecipeChoice.MaterialChoice(getMaterialList(recipeChoiceConfig.getStringList("arguments")));
        };
    }

    private List<ItemStack> getExactChoices(List<?> arguments) {
        ArrayList<ItemStack> items = new ArrayList<>();
        if (arguments == null) return items;

        Map<String,ItemStack> itemStacks = Config.itemStacks;
        for (Object object : arguments) {
            if (object instanceof ItemStack item) items.add(item);
            if (object instanceof String string && itemStacks.containsKey(string)) items.add(itemStacks.get(string));
        }
        return items;
    }

    private List<Material> getMaterialList(List<String> materials) {
        List<Material> materialsList = new ArrayList<>();

        for (String material : materials) {
            try {
                materialsList.add(Material.valueOf(material.toUpperCase()));
            }
            catch (IllegalArgumentException ignored) {
                SimpleMixtures.getPlugin().getLogger().warning("[SimpleMixtures] Invalid material type: " + material);
            }
        }

        return materialsList;
    }

    public PotionMix getRecipe(String name) {
        NamespacedKey key = new NamespacedKey(SimpleMixtures.getPlugin(), name);
        return recipesAdded.getOrDefault(key, null);
    }

    public Map<NamespacedKey,PotionMix> getRecipes() {
        return Collections.unmodifiableMap(recipesAdded);
    }

    public Map<String,RecipeChoice> getRecipeChoices() {
        return Collections.unmodifiableMap(recipeChoices);
    }

    public Map<String,ItemStack> getItemStacks() {
        return Collections.unmodifiableMap(itemStacks);
    }

    private void clearRecipes() {
        for (NamespacedKey recipe : recipesAdded.keySet()) {
            Bukkit.getServer().getPotionBrewer().removePotionMix(recipe);
        }
        recipesAdded.clear();
    }

}
