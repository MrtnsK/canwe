package fr.kevin.canwe.task;

import fr.kevin.canwe.CanWePlugin;
import fr.kevin.canwe.config.PluginConfig;
import fr.kevin.canwe.mapping.MappingManager;
import fr.kevin.canwe.modrinth.ModrinthClient;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.logging.Level;

public class DailyCheckTask {

    private static final long TICKS_PER_DAY = 24L * 60 * 60 * 20;
    private static final int CHECK_HOUR = 8;

    private final CanWePlugin plugin;
    private BukkitTask task;

    public DailyCheckTask(CanWePlugin plugin) {
        this.plugin = plugin;
    }

    public void schedule() {
        long delayTicks = computeTicksUntilNextCheck();
        plugin.getLogger().info("Next compatibility check scheduled in "
                + (delayTicks / 20 / 60) + " minutes (at " + CHECK_HOUR + ":00).");

        task = new BukkitRunnable() {
            @Override
            public void run() {
                runCheck();
            }
        }.runTaskTimerAsynchronously(plugin, delayTicks, TICKS_PER_DAY);
    }

    public void cancel() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /** Runs the check immediately (blocking, must be called from an async context). */
    public void runNow() {
        runCheck();
    }

    private long computeTicksUntilNextCheck() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime next = now.toLocalDate().atTime(CHECK_HOUR, 0).atZone(ZoneId.systemDefault());
        if (!now.isBefore(next)) {
            next = next.plusDays(1);
        }
        long seconds = ChronoUnit.SECONDS.between(now, next);
        return Math.max(seconds * 20L, 1L);
    }

    private void runCheck() {
        PluginConfig config = plugin.getPluginConfig();

        if (!config.isEnabled()) {
            return;
        }

        if (!config.hasApiToken()) {
            plugin.getLogger().warning("[CanWe] Daily check skipped: no API token configured in config.yml.");
            return;
        }

        String targetVersion = config.getTargetVersion();
        ModrinthClient client = plugin.getModrinthClient();
        MappingManager mappingManager = plugin.getMappingManager();

        plugin.getLogger().info("[CanWe] Starting daily compatibility check for version " + targetVersion + "...");

        Map<String, String> mappings = mappingManager.getAllMappings();

        int compatible = 0;
        int incompatible = 0;
        int ignored = 0;

        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String pluginName = entry.getKey();
            String slug = entry.getValue();

            if (config.getIgnoredPlugins().contains(pluginName) || slug == null || slug.isBlank()) {
                plugin.getLogger().info("[CanWe] ~ " + pluginName + " → ignored (no Modrinth mapping)");
                ignored++;
                continue;
            }

            try {
                boolean available = client.hasVersionForTarget(slug, targetVersion);
                if (available) {
                    plugin.getLogger().info("[CanWe] + " + pluginName + " (" + slug + ") → compatible with " + targetVersion);
                    compatible++;
                } else {
                    plugin.getLogger().warning("[CanWe] ! " + pluginName + " (" + slug + ") → NO version for " + targetVersion + " on Modrinth");
                    incompatible++;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "[CanWe] ? " + pluginName + " → check failed: " + e.getMessage());
                ignored++;
            }
        }

        plugin.getLogger().info("[CanWe] Check complete — "
                + compatible + " compatible, "
                + incompatible + " incompatible, "
                + ignored + " ignored.");

        if (incompatible > 0) {
            plugin.getLogger().warning("[CanWe] " + incompatible + " plugin(s) are not yet available for " + targetVersion
                    + ". Check the logs above for details.");
        }
    }
}
