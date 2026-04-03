package fr.kevin.canwe;

import fr.kevin.canwe.config.PluginConfig;
import fr.kevin.canwe.mapping.MappingManager;
import fr.kevin.canwe.modrinth.ModrinthClient;
import fr.kevin.canwe.reconciler.AutoReconciler;
import fr.kevin.canwe.task.DailyCheckTask;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class CanWePlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private MappingManager mappingManager;
    private ModrinthClient modrinthClient;
    private DailyCheckTask dailyCheckTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        pluginConfig = new PluginConfig(getConfig());

        mappingManager = new MappingManager(this);
        modrinthClient = new ModrinthClient(pluginConfig.getApiToken());

        if (!pluginConfig.hasApiToken()) {
            getLogger().warning("No Modrinth API token configured. Set 'modrinth.api-token' in config.yml.");
            getLogger().warning("Auto-reconciliation and daily checks require a valid token.");
        }

        AutoReconciler reconciler = new AutoReconciler(this, mappingManager, modrinthClient, pluginConfig.getIgnoredPlugins());
        reconciler.run();

        if (pluginConfig.isEnabled()) {
            dailyCheckTask = new DailyCheckTask(this);
            dailyCheckTask.schedule();
        } else {
            getLogger().info("Daily check is disabled (enabled: false in config.yml).");
        }

        getLogger().info("CanWe enabled. Target version: " + pluginConfig.getTargetVersion());
    }

    @Override
    public void onDisable() {
        if (dailyCheckTask != null) {
            dailyCheckTask.cancel();
        }
        getLogger().info("CanWe disabled.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("canwe")) {
            return false;
        }

        if (!sender.hasPermission("canwe.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6CanWe §7— Modrinth Compatibility Checker");
            sender.sendMessage("§7/canwe check §8- Run the compatibility check now");
            sender.sendMessage("§7/canwe reload §8- Reload configuration and mappings");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "check" -> {
                sender.sendMessage("§6[CanWe] §7Running compatibility check... see console for results.");
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                    DailyCheckTask immediateCheck = new DailyCheckTask(this);
                    immediateCheck.runNow();
                });
            }
            case "reload" -> {
                reloadConfig();
                pluginConfig = new PluginConfig(getConfig());
                mappingManager.reload();
                modrinthClient = new ModrinthClient(pluginConfig.getApiToken());

                if (dailyCheckTask != null) {
                    dailyCheckTask.cancel();
                }
                if (pluginConfig.isEnabled()) {
                    dailyCheckTask = new DailyCheckTask(this);
                    dailyCheckTask.schedule();
                }

                sender.sendMessage("§6[CanWe] §7Configuration and mappings reloaded.");
            }
            default -> {
                sender.sendMessage("§cUnknown subcommand. Use /canwe for help.");
            }
        }

        return true;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public MappingManager getMappingManager() {
        return mappingManager;
    }

    public ModrinthClient getModrinthClient() {
        return modrinthClient;
    }
}
