package fr.kevin.canwe.modrinth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ModrinthClient {

    private static final String BASE_URL = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "CanWe/1.0.0 (github.com/kevin/canwe)";

    private final HttpClient httpClient;
    private final String apiToken;

    public ModrinthClient(String apiToken) {
        this.apiToken = apiToken;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Searches for a plugin on Modrinth by name.
     * Returns a list of matching results (plugins/mods only).
     */
    public List<ModrinthSearchResult> searchProject(String name) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String facets = URLEncoder.encode("[[\"project_type:plugin\"]]", StandardCharsets.UTF_8);
        String url = BASE_URL + "/search?query=" + encodedQuery + "&facets=" + facets + "&limit=5";

        HttpRequest request = buildRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Modrinth search failed with status " + response.statusCode());
        }

        List<ModrinthSearchResult> results = new ArrayList<>();
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray hits = json.getAsJsonArray("hits");

        for (JsonElement element : hits) {
            JsonObject hit = element.getAsJsonObject();
            String slug = hit.get("slug").getAsString();
            String projectId = hit.get("project_id").getAsString();
            String title = hit.get("title").getAsString();
            results.add(new ModrinthSearchResult(slug, projectId, title));
        }

        return results;
    }

    /**
     * Checks if a project has a version available for the given game version and the "paper" loader.
     */
    public boolean hasVersionForTarget(String projectIdOrSlug, String targetVersion) throws IOException, InterruptedException {
        String encodedVersions = URLEncoder.encode("[\"" + targetVersion + "\"]", StandardCharsets.UTF_8);
        String encodedLoaders = URLEncoder.encode("[\"paper\",\"folia\"]", StandardCharsets.UTF_8);
        String url = BASE_URL + "/project/" + projectIdOrSlug + "/version"
                + "?game_versions=" + encodedVersions
                + "&loaders=" + encodedLoaders;

        HttpRequest request = buildRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            return false;
        }
        if (response.statusCode() != 200) {
            throw new IOException("Modrinth version check failed with status " + response.statusCode() + " for project " + projectIdOrSlug);
        }

        JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
        return !versions.isEmpty();
    }

    private HttpRequest buildRequest(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", USER_AGENT)
                .GET();

        if (apiToken != null && !apiToken.isBlank()) {
            builder.header("Authorization", apiToken);
        }

        return builder.build();
    }
}
