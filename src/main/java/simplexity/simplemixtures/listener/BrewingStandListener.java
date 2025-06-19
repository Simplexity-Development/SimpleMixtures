package simplexity.simplemixtures.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.ItemStack;
import simplexity.simplemixtures.config.Config;

public class BrewingStandListener implements Listener {

    @EventHandler(priority= EventPriority.MONITOR, ignoreCancelled = true)
    public void dropLeftoverFromBrew(BrewEvent event) {
        ItemStack ingredient = event.getContents().getIngredient();
        if (ingredient == null) return;
        Material leftover = Config.getInstance().getBrewingLeftover(ingredient.getType());
        if (leftover == null) return;

        ItemStack droppedItem = new ItemStack(leftover, 1);
        Location dropLocation = event.getBlock().getLocation();
        dropLocation.getWorld().dropItemNaturally(dropLocation, droppedItem);
    }

}
