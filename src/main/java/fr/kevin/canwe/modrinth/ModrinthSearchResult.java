package fr.kevin.canwe.modrinth;

public class ModrinthSearchResult {

    private final String slug;
    private final String projectId;
    private final String title;

    public ModrinthSearchResult(String slug, String projectId, String title) {
        this.slug = slug;
        this.projectId = projectId;
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title + " (" + slug + ")";
    }
}
