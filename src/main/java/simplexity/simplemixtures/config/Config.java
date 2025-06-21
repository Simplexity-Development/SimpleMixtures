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
import org.slf4j.Logger;
import simplexity.simplemixtures.SimpleMixtures;

import java.util.*;

public class Config {

    private enum ChoiceType {
        EXACT, MATERIAL
    }

    private static Logger logger;
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
        logger = plugin.getSLF4JLogger();
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
        if (leftovers == null) {
            logger.info("There is no leftovers configuration, skipping...");
            return;
        }
        for (String leftover : leftovers.getKeys(false)) {
            Material leftoverMaterial = Material.getMaterial(leftover);
            if (leftoverMaterial == null) {
                logger.warn("Failed to load leftover result (Invalid Material): {}", leftover);
                continue;
            }

            for (String ingredient : leftovers.getStringList(leftover)) {
                Material ingredientMaterial = Material.getMaterial(ingredient);
                if (ingredientMaterial == null) {
                    logger.warn("Failed to load leftover ingredient (Invalid Material): {}", ingredient);
                    continue;
                }

                brewingLeftover.put(ingredientMaterial, leftoverMaterial);
                logger.debug("Successfully registered leftover: {} -> {}", ingredientMaterial, leftoverMaterial);
            }
        }
    }

    private void registerRecipes(FileConfiguration config) {
        ConfigurationSection recipesConfiguration = config.getConfigurationSection("recipes");
        if (recipesConfiguration == null) {
            logger.warn("""
            There is no recipes configuration. The whole plugin relies on this configuration.
            If you are not adding recipes, consider removing SimpleMixtures.
            Skipping...
            """);
            return;
        }
        recipesConfiguration.getKeys(false).forEach(potionKey -> {
            if (registerPotion(recipesConfiguration, potionKey)) {
                logger.debug("Successfully registered brewing recipe with key {}", potionKey);
            }
            else {
                logger.warn("Failed to load brewing recipe with key {}", potionKey);
            }
        });
    }

    private boolean registerPotion(ConfigurationSection recipes, String key) {
        ConfigurationSection potionConfig = recipes.getConfigurationSection(key);
        if (potionConfig == null) return false;

        RecipeChoice input = potionConfig.isConfigurationSection("input")
                ? getRecipeChoice(potionConfig.getConfigurationSection("input"))
                : recipeChoices.getOrDefault(potionConfig.getString("input"), null);
        if (input == null) {
            logger.warn("No valid input provided at {}", key);
            return false;
        }
        RecipeChoice ingredient = potionConfig.isConfigurationSection("ingredient")
                ? getRecipeChoice(potionConfig.getConfigurationSection("ingredient"))
                : recipeChoices.getOrDefault(potionConfig.getString("ingredient"), null);
        if (ingredient == null) {
            logger.warn("No valid ingredient provided at {}", key);
            return false;
        }
        ItemStack result = potionConfig.isItemStack("result")
                ? potionConfig.getItemStack("result")
                : itemStacks.getOrDefault(potionConfig.getString("result"), null);
        if (result == null) {
            logger.warn("No valid result provided at {}", key);
            return false;
        }
        NamespacedKey namespacedKey = new NamespacedKey(SimpleMixtures.getPlugin(), key);

        PotionMix potionMix = new PotionMix(namespacedKey, result, input, ingredient);
        Bukkit.getServer().getPotionBrewer().addPotionMix(potionMix);
        recipesAdded.put(namespacedKey, potionMix);
        return true;
    }

    private void registerRecipeChoices(FileConfiguration config) {
        ConfigurationSection placeholderConfig = config.getConfigurationSection("recipe_choices");
        if (placeholderConfig == null) {
            logger.info("There is no recipe_choices configuration, skipping...");
            return;
        }
        for (String key : placeholderConfig.getKeys(false)) {
            RecipeChoice choice = getRecipeChoice(placeholderConfig.getConfigurationSection(key));
            if (choice == null) {
                logger.warn("Failed to register recipe_choice at {}", key);
                continue;
            }
            recipeChoices.put(key, choice);
            logger.debug("Successfully registered recipe_choice {}", key);
        }
    }

    private void registerItemStacks(FileConfiguration config) {
        ConfigurationSection placeholderConfig = config.getConfigurationSection("item_stacks");
        if (placeholderConfig == null) {
            logger.info("There is no item_stacks configuration, skipping...");
            return;
        }
        for (String key : placeholderConfig.getKeys(false)) {
            if (!placeholderConfig.isItemStack(key)) {
                logger.warn("""
                Failed to register item_stack at {}.
                Consider using "/simplemixtures serialize" to get yml formatted itemstack data.
                """, key);
                continue;
            }
            itemStacks.put(key, placeholderConfig.getItemStack(key));
            logger.debug("Successfully registered item_stack {}", key);
        }
    }

    private RecipeChoice getRecipeChoice(ConfigurationSection recipeChoiceConfig) {
        if (recipeChoiceConfig == null) return null;
        ChoiceType type;
        try {
            type = ChoiceType.valueOf(recipeChoiceConfig.getString("match_type"));
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to load recipe choice (Invalid Match Type)");
            return null;
        }

        if (type == ChoiceType.EXACT) {
            List<ItemStack> itemStackList = getExactChoices(recipeChoiceConfig.getList("arguments"));
            if (itemStackList.isEmpty()) {
                logger.warn("Failed to load EXACT type recipe choice");
                return null;
            }
            return new RecipeChoice.ExactChoice(itemStackList);
        }

        if (type == ChoiceType.MATERIAL) {
            List<Material> materialList = getMaterialList(recipeChoiceConfig.getStringList("arguments"));
            if (materialList.isEmpty()) {
                logger.warn("Failed to load MATERIAL type recipe choice");
                return null;
            }
            return new RecipeChoice.MaterialChoice(materialList);
        }

        return null;
    }

    private List<ItemStack> getExactChoices(List<?> arguments) {
        ArrayList<ItemStack> items = new ArrayList<>();
        if (arguments == null) {
            logger.warn("Failed to load exact choices, \"arguments\" is missing");
            return items;
        }

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
                logger.warn("Invalid material type: {}", material);
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
