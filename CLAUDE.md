# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

```bash
mvn package        # builds target/canwe-1.0.0.jar (shaded, ready to drop in plugins/)
mvn package -q     # silent build
mvn compile        # compile only, no JAR
```

The build produces two JARs: `original-canwe-1.0.0.jar` (unshaded) and `canwe-1.0.0.jar` (shaded, use this one). There are no tests. Paper API is `provided` scope — it is not bundled. Gson is shaded and relocated to `fr.kevin.canwe.libs.gson` to avoid classpath conflicts.

## Architecture

This is a **Paper 1.21.1 plugin** that queries the **Modrinth API v2** every morning at 8:00 to check whether installed plugins have a release for the target Minecraft version (default `26.1`, Mojang's new year.quarter versioning format starting 2026).

### Startup flow (`CanWePlugin.onEnable`)

1. Load `config.yml` → `PluginConfig`
2. Load/create `mappings.yml` → `MappingManager`
3. Run `AutoReconciler` (async) — for each loaded plugin without a mapping, searches Modrinth and writes the slug if exactly one result is found; writes empty string (= ignored) if ambiguous
4. If `enabled: true`, schedule `DailyCheckTask` with an initial delay until the next 8:00 AM, then repeat every 24 h

### Key design points

- **`mappings.yml`** (in `plugins/CanWe/`) is the source of truth linking plugin names to Modrinth slugs. An empty string value means "ignore this plugin". The `ignored-plugins` list in `config.yml` overrides at runtime without touching `mappings.yml`.
- **`ModrinthClient`** uses `java.net.http.HttpClient` (no extra HTTP library). The `Authorization` header is the raw token string (Modrinth format). Version checks query `/v2/project/{slug}/version?game_versions=["26.1"]&loaders=["paper","folia"]`.
- **`DailyCheckTask`** is scheduled as a repeating async task via `runTaskTimerAsynchronously`. All Modrinth HTTP calls happen off the main thread. `runNow()` is exposed for the `/canwe check` command, which also dispatches async.
- **`/canwe reload`** cancels the existing task, re-reads config + mappings, recreates `ModrinthClient` with the (possibly updated) token, and reschedules.

### Package layout

| Package | Role |
|---|---|
| `fr.kevin.canwe` | Plugin entry point + command handler |
| `fr.kevin.canwe.config` | `PluginConfig` — typed wrapper around Bukkit's `FileConfiguration` |
| `fr.kevin.canwe.modrinth` | HTTP client + response model |
| `fr.kevin.canwe.mapping` | `MappingManager` — reads/writes `mappings.yml` via `YamlConfiguration` |
| `fr.kevin.canwe.reconciler` | `AutoReconciler` — async first-run slug discovery |
| `fr.kevin.canwe.task` | `DailyCheckTask` — scheduling and check logic |
