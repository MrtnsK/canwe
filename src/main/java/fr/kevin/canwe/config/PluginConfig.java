package fr.kevin.canwe.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class PluginConfig {

    private final boolean enabled;
    private final int checkHour;
    private final String apiToken;
    private final String targetVersion;
    private final List<String> ignoredPlugins;

    public PluginConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("enabled", true);
        this.checkHour = Math.max(0, Math.min(23, config.getInt("check-hour", 8)));
        this.apiToken = config.getString("modrinth.api-token", "");
        this.targetVersion = config.getString("modrinth.target-version", "26.1");
        this.ignoredPlugins = config.getStringList("ignored-plugins");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getCheckHour() {
        return checkHour;
    }

    public String getApiToken() {
        return apiToken;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public List<String> getIgnoredPlugins() {
        return ignoredPlugins;
    }

    public boolean hasApiToken() {
        return apiToken != null && !apiToken.isBlank();
    }
}
