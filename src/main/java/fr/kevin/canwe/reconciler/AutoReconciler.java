package fr.kevin.canwe.reconciler;

import fr.kevin.canwe.CanWePlugin;
import fr.kevin.canwe.mapping.MappingManager;
import fr.kevin.canwe.modrinth.ModrinthClient;
import fr.kevin.canwe.modrinth.ModrinthSearchResult;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class AutoReconciler {

    private final CanWePlugin plugin;
    private final MappingManager mappingManager;
    private final ModrinthClient modrinthClient;
    private final List<String> ignoredPlugins;

    public AutoReconciler(CanWePlugin plugin, MappingManager mappingManager,
                          ModrinthClient modrinthClient, List<String> ignoredPlugins) {
        this.plugin = plugin;
        this.mappingManager = mappingManager;
        this.modrinthClient = modrinthClient;
        this.ignoredPlugins = ignoredPlugins;
    }

    /**
     * Runs the auto-reconciliation asynchronously.
     * For each loaded plugin without a mapping, searches Modrinth and saves if unique match found.
     */
    public void run() {
        List<String> pluginsToReconcile = findPluginsWithoutMapping();

        if (pluginsToReconcile.isEmpty()) {
            return;
        }

        plugin.getLogger().info("[AutoReconciler] Searching Modrinth for " + pluginsToReconcile.size() + " unmapped plugin(s)...");

        new BukkitRunnable() {
            @Override
            public void run() {
                for (String pluginName : pluginsToReconcile) {
                    reconcilePlugin(pluginName);
                }
                plugin.getLogger().info("[AutoReconciler] Done. Check mappings.yml to review or fill in missing entries.");
            }
        }.runTaskAsynchronously(plugin);
    }

    private List<String> findPluginsWithoutMapping() {
        List<String> result = new ArrayList<>();
        for (Plugin loaded : plugin.getServer().getPluginManager().getPlugins()) {
            String name = loaded.getName();
            if (name.equals(plugin.getName())) continue;
            if (ignoredPlugins.contains(name)) continue;
            if (!mappingManager.hasEntry(name)) {
                result.add(name);
            }
        }
        return result;
    }

    private void reconcilePlugin(String pluginName) {
        try {
            List<ModrinthSearchResult> results = modrinthClient.searchProject(pluginName);

            if (results.size() == 1) {
                ModrinthSearchResult match = results.get(0);
                mappingManager.setMapping(pluginName, match.getSlug());
                plugin.getLogger().info("[AutoReconciler] Mapped \"" + pluginName + "\" → " + match.getSlug()
                        + " (\"" + match.getTitle() + "\")");
            } else if (results.isEmpty()) {
                mappingManager.setIgnored(pluginName);
                plugin.getLogger().warning("[AutoReconciler] No Modrinth project found for \"" + pluginName
                        + "\". Marked as ignored. Edit mappings.yml if this is wrong.");
            } else {
                mappingManager.setIgnored(pluginName);
                plugin.getLogger().warning("[AutoReconciler] Multiple results found for \"" + pluginName
                        + "\". Please set the correct slug in mappings.yml manually:");
                for (ModrinthSearchResult result : results) {
                    plugin.getLogger().warning("  - " + result.getSlug() + " (" + result.getTitle() + ")");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "[AutoReconciler] Failed to search Modrinth for \"" + pluginName + "\": " + e.getMessage());
        }
    }
}
