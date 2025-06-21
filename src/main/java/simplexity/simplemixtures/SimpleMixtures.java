package simplexity.simplemixtures;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import simplexity.simplemixtures.command.SimpleMixturesCommand;
import simplexity.simplemixtures.config.Config;
import simplexity.simplemixtures.listener.BrewingStandListener;

@SuppressWarnings("UnstableApiUsage")
public final class SimpleMixtures extends JavaPlugin {

    private static SimpleMixtures plugin;

    @Override
    public void onEnable() {
        plugin = this;
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(SimpleMixturesCommand.createCommand().build());
        });

        Bukkit.getServer().getPluginManager().registerEvents(new BrewingStandListener(), this);
        plugin.saveDefaultConfig();

        try {
            Config.getInstance();
        }
        catch (Exception ignored) {
            this.getLogger().severe("Your configuration is invalid. Please double check it and try again.");
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static SimpleMixtures getPlugin() {
        return plugin;
    }

}
