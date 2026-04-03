package fr.kevin.canwe.mapping;

import fr.kevin.canwe.CanWePlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class MappingManager {

    private static final String FILE_NAME = "mappings.yml";

    private final CanWePlugin plugin;
    private final File mappingFile;
    private YamlConfiguration config;

    public MappingManager(CanWePlugin plugin) {
        this.plugin = plugin;
        this.mappingFile = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    private void load() {
        if (!mappingFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                mappingFile.createNewFile();
                config = new YamlConfiguration();
                config.set("mappings", new HashMap<>());
                config.options().setHeader(List.of(
                        "CanWe - Plugin to Modrinth Mappings",
                        "",
                        "Maps each plugin name to its Modrinth slug or project ID.",
                        "Leave the value empty (\"\") to ignore a plugin.",
                        "",
                        "Example:",
                        "  mappings:",
                        "    EssentialsX: essentialsx",
                        "    WorldEdit: worldedit",
                        "    MyCustomPlugin: \"\""
                ));
                config.save(mappingFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create mappings.yml", e);
                config = new YamlConfiguration();
            }
        } else {
            config = YamlConfiguration.loadConfiguration(mappingFile);
        }
    }

    /**
     * Returns the Modrinth slug/ID for the given plugin name, or null if not set.
     */
    public String getMapping(String pluginName) {
        return config.getString("mappings." + pluginName);
    }

    /**
     * Returns true if this plugin has a mapping entry (even if empty).
     */
    public boolean hasEntry(String pluginName) {
        return config.contains("mappings." + pluginName);
    }

    /**
     * Returns true if this plugin should be skipped (empty mapping or explicitly ignored).
     */
    public boolean isIgnored(String pluginName, List<String> ignoredList) {
        if (ignoredList.contains(pluginName)) {
            return true;
        }
        String mapping = getMapping(pluginName);
        return mapping != null && mapping.isBlank();
    }

    /**
     * Saves a Modrinth slug for the given plugin name.
     */
    public void setMapping(String pluginName, String slug) {
        config.set("mappings." + pluginName, slug);
        save();
    }

    /**
     * Marks a plugin as ignored (sets mapping to empty string).
     */
    public void setIgnored(String pluginName) {
        config.set("mappings." + pluginName, "");
        save();
    }

    /**
     * Returns all current mappings as a Map<pluginName, slug>.
     */
    public Map<String, String> getAllMappings() {
        Map<String, String> result = new HashMap<>();
        if (!config.isConfigurationSection("mappings")) {
            return result;
        }
        for (String key : config.getConfigurationSection("mappings").getKeys(false)) {
            result.put(key, config.getString("mappings." + key, ""));
        }
        return result;
    }

    public void save() {
        try {
            config.save(mappingFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save mappings.yml", e);
        }
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(mappingFile);
    }
}
