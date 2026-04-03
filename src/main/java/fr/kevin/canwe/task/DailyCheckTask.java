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
import java.util.ArrayList;
import java.util.List;
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

        Map<String, String> mappings = mappingManager.getAllMappings();

        List<String> compatibleList  = new ArrayList<>();
        List<String> incompatibleList = new ArrayList<>();
        List<String> ignoredList     = new ArrayList<>();

        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String pluginName = entry.getKey();
            String slug = entry.getValue();

            if (config.getIgnoredPlugins().contains(pluginName) || slug == null || slug.isBlank()) {
                ignoredList.add(pluginName);
                continue;
            }

            try {
                if (client.hasVersionForTarget(slug, targetVersion)) {
                    compatibleList.add(pluginName);
                } else {
                    incompatibleList.add(pluginName);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[CanWe] Check failed for \"" + pluginName + "\": " + e.getMessage());
                ignoredList.add(pluginName);
            }
        }

        plugin.getLogger().info("[CanWe] Check complete — "
                + compatibleList.size() + " compatible, "
                + incompatibleList.size() + " incompatible, "
                + ignoredList.size() + " ignored.");
        plugin.getLogger().info("[CanWe] Ignored     : " + ignoredList);
        plugin.getLogger().info("[CanWe] Compatible  : " + compatibleList);

        if (!incompatibleList.isEmpty()) {
            plugin.getLogger().warning("[CanWe] Incompatible: " + incompatibleList);
        }
    }
}
