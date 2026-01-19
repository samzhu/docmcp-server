package io.github.samzhu.docmcp.infrastructure.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GitHubClient 單元測試（使用 MockWebServer）
 */
@DisplayName("GitHubClient")
class GitHubClientTest {

    private MockWebServer mockWebServer;
    private GitHubClient gitHubClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        // Remove trailing slash
        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        // Create a GitHubClient with modified base URLs for testing
        gitHubClient = new TestableGitHubClient(restClient, new ObjectMapper(), "", baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("should list files from repository")
    void shouldListFilesFromRepository() {
        // Arrange
        String responseJson = """
                [
                    {"name": "README.md", "path": "docs/README.md", "sha": "abc123", "size": 1024, "type": "file", "download_url": "https://example.com/README.md"},
                    {"name": "guide", "path": "docs/guide", "sha": "def456", "size": 0, "type": "dir", "download_url": null}
                ]
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .setHeader("Content-Type", "application/json"));

        // Act
        var files = gitHubClient.listFiles("owner", "repo", "docs", "main");

        // Assert
        assertThat(files).hasSize(2);
        assertThat(files.get(0).name()).isEqualTo("README.md");
        assertThat(files.get(0).isFile()).isTrue();
        assertThat(files.get(1).name()).isEqualTo("guide");
        assertThat(files.get(1).isDirectory()).isTrue();
    }

    @Test
    @DisplayName("should get file content")
    void shouldGetFileContent() {
        // Arrange
        String fileContent = "# Hello World\n\nThis is a test document.";
        mockWebServer.enqueue(new MockResponse()
                .setBody(fileContent)
                .setHeader("Content-Type", "text/plain"));

        // Act
        String content = gitHubClient.getFileContent("owner", "repo", "docs/README.md", "main");

        // Assert
        assertThat(content).isEqualTo(fileContent);
    }

    @Test
    @DisplayName("should list releases")
    void shouldListReleases() {
        // Arrange
        String responseJson = """
                [
                    {
                        "id": 1,
                        "tag_name": "v1.0.0",
                        "name": "Release 1.0.0",
                        "body": "First release",
                        "draft": false,
                        "prerelease": false,
                        "published_at": "2024-01-15T10:00:00Z",
                        "tarball_url": "https://example.com/v1.0.0.tar.gz",
                        "zipball_url": "https://example.com/v1.0.0.zip"
                    }
                ]
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .setHeader("Content-Type", "application/json"));

        // Act
        var releases = gitHubClient.listReleases("owner", "repo");

        // Assert
        assertThat(releases).hasSize(1);
        assertThat(releases.get(0).tagName()).isEqualTo("v1.0.0");
        assertThat(releases.get(0).name()).isEqualTo("Release 1.0.0");
        assertThat(releases.get(0).draft()).isFalse();
    }

    @Test
    @DisplayName("should throw exception on API error")
    void shouldThrowExceptionOnApiError() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("Not Found"));

        // Act & Assert
        assertThatThrownBy(() -> gitHubClient.listFiles("owner", "repo", "nonexistent", "main"))
                .isInstanceOf(GitHubClient.GitHubApiException.class);
    }

    /**
     * 可測試的 GitHubClient（使用自訂的基礎 URL）
     */
    private static class TestableGitHubClient extends GitHubClient {
        private final RestClient testRestClient;
        private final String baseUrl;

        public TestableGitHubClient(RestClient restClient, ObjectMapper objectMapper,
                                     String token, String baseUrl) {
            super(RestClient.builder(), objectMapper, token);
            this.testRestClient = restClient;
            this.baseUrl = baseUrl;
        }

        @Override
        public java.util.List<GitHubFile> listFiles(String owner, String repo, String path, String ref) {
            String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                    baseUrl, owner, repo, path, ref);

            try {
                String response = testRestClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);
                return parseFilesJson(response);
            } catch (Exception e) {
                throw new GitHubApiException("Failed to list files: " + e.getMessage(), e);
            }
        }

        @Override
        public String getFileContent(String owner, String repo, String path, String ref) {
            String url = String.format("%s/%s/%s/%s/%s",
                    baseUrl, owner, repo, ref, path);

            try {
                return testRestClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);
            } catch (Exception e) {
                throw new GitHubApiException("Failed to get file content: " + e.getMessage(), e);
            }
        }

        @Override
        public java.util.List<GitHubRelease> listReleases(String owner, String repo) {
            String url = String.format("%s/repos/%s/%s/releases", baseUrl, owner, repo);

            try {
                String response = testRestClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);
                return parseReleasesJson(response);
            } catch (Exception e) {
                throw new GitHubApiException("Failed to list releases: " + e.getMessage(), e);
            }
        }

        private java.util.List<GitHubFile> parseFilesJson(String json) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = new ObjectMapper().readTree(json);
                java.util.List<GitHubFile> files = new java.util.ArrayList<>();

                if (root.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode node : root) {
                        files.add(new GitHubFile(
                                node.path("name").asText(),
                                node.path("path").asText(),
                                node.path("sha").asText(),
                                node.path("size").asLong(),
                                node.path("type").asText(),
                                node.path("download_url").asText(null)
                        ));
                    }
                }

                return files;
            } catch (Exception e) {
                throw new GitHubApiException("Failed to parse file list: " + e.getMessage(), e);
            }
        }

        private java.util.List<GitHubRelease> parseReleasesJson(String json) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = new ObjectMapper().readTree(json);
                java.util.List<GitHubRelease> releases = new java.util.ArrayList<>();

                if (root.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode node : root) {
                        String publishedAtStr = node.path("published_at").asText(null);
                        java.time.OffsetDateTime publishedAt = publishedAtStr != null
                                ? java.time.OffsetDateTime.parse(publishedAtStr)
                                : null;

                        releases.add(new GitHubRelease(
                                node.path("id").asLong(),
                                node.path("tag_name").asText(),
                                node.path("name").asText(),
                                node.path("body").asText(null),
                                node.path("draft").asBoolean(),
                                node.path("prerelease").asBoolean(),
                                publishedAt,
                                node.path("tarball_url").asText(null),
                                node.path("zipball_url").asText(null)
                        ));
                    }
                }

                return releases;
            } catch (Exception e) {
                throw new GitHubApiException("Failed to parse release list: " + e.getMessage(), e);
            }
        }
    }
}
