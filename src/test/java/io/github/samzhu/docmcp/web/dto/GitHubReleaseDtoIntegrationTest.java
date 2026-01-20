package io.github.samzhu.docmcp.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.docmcp.infrastructure.github.GitHubRelease;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GitHubReleaseDto 整合測試
 * <p>
 * 實際呼叫 GitHub API 取得 Spring Boot releases，
 * 驗證轉換為 GitHubReleaseDto 後所有欄位都正確處理（無 null 顯示問題）。
 * </p>
 */
@DisplayName("GitHubReleaseDto 整合測試（實際呼叫 GitHub API）")
class GitHubReleaseDtoIntegrationTest {

    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String OWNER = "spring-projects";
    private static final String REPO = "spring-boot";
    private static final int LIMIT = 20;
    private static final String DOCS_PATH = "docs";

    private static RestClient restClient;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        restClient = RestClient.builder().build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("從 GitHub API 取得 20 筆 releases，轉換後所有 name 都不應為 null")
    void shouldConvert20ReleasesWithoutNullName() throws Exception {
        // Given: 從 GitHub API 取得 20 筆 releases
        String url = String.format("%s/repos/%s/%s/releases?per_page=%d",
                GITHUB_API_BASE, OWNER, REPO, LIMIT);

        String response = restClient.get()
                .uri(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DocMCP-Server-Test")
                .retrieve()
                .body(String.class);

        List<GitHubRelease> releases = parseReleases(response);

        // 確認取得足夠的資料
        assertThat(releases)
                .as("應該取得至少 %d 筆 releases", LIMIT)
                .hasSizeGreaterThanOrEqualTo(LIMIT);

        // When: 轉換為 GitHubReleaseDto
        List<GitHubReleaseDto> dtos = releases.stream()
                .limit(LIMIT)
                .map(r -> GitHubReleaseDto.from(r, false, DOCS_PATH))
                .toList();

        // Then: 驗證所有 20 筆的 name 都不為 null
        assertThat(dtos).hasSize(LIMIT);

        for (int i = 0; i < dtos.size(); i++) {
            GitHubReleaseDto dto = dtos.get(i);

            assertThat(dto.name())
                    .as("第 %d 筆 release (tagName=%s) 的 name 不應為 null", i + 1, dto.tagName())
                    .isNotNull();

            assertThat(dto.name())
                    .as("第 %d 筆 release (tagName=%s) 的 name 不應為空字串", i + 1, dto.tagName())
                    .isNotBlank();

            assertThat(dto.tagName())
                    .as("第 %d 筆 release 的 tagName 不應為 null", i + 1)
                    .isNotNull();

            assertThat(dto.version())
                    .as("第 %d 筆 release 的 version 不應為 null", i + 1)
                    .isNotNull();

            // 印出每筆資料供確認
            System.out.printf("[%2d] tagName=%-12s version=%-10s name=%s%n",
                    i + 1, dto.tagName(), dto.version(), dto.name());
        }
    }

    @Test
    @DisplayName("驗證 name 為 null 時會 fallback 到 tagName")
    void shouldVerifyNullNameFallbackToTagName() throws Exception {
        // Given: 從 GitHub API 取得 releases
        String url = String.format("%s/repos/%s/%s/releases?per_page=%d",
                GITHUB_API_BASE, OWNER, REPO, LIMIT);

        String response = restClient.get()
                .uri(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DocMCP-Server-Test")
                .retrieve()
                .body(String.class);

        List<GitHubRelease> releases = parseReleases(response);

        // 統計原始 name 為 null 或空的數量
        long nullOrEmptyNameCount = releases.stream()
                .limit(LIMIT)
                .filter(r -> r.name() == null || r.name().isBlank())
                .count();

        System.out.printf("原始 releases 中有 %d 筆 name 為 null 或空%n", nullOrEmptyNameCount);

        // When: 轉換為 GitHubReleaseDto
        List<GitHubReleaseDto> dtos = releases.stream()
                .limit(LIMIT)
                .map(r -> GitHubReleaseDto.from(r, false, DOCS_PATH))
                .toList();

        // Then: 轉換後應該都有有效的 name（fallback 到 tagName）
        for (GitHubReleaseDto dto : dtos) {
            assertThat(dto.name())
                    .as("轉換後的 name 不應為 null 或空，tagName=%s", dto.tagName())
                    .isNotBlank();
        }

        // 驗證 fallback 邏輯：如果原始 name 為 null，轉換後應該等於 tagName
        for (int i = 0; i < Math.min(LIMIT, releases.size()); i++) {
            GitHubRelease original = releases.get(i);
            GitHubReleaseDto dto = dtos.get(i);

            if (original.name() == null || original.name().isBlank()) {
                assertThat(dto.name())
                        .as("當原始 name 為 null/空時，應 fallback 到 tagName")
                        .isEqualTo(original.tagName());
            }
        }
    }

    /**
     * 解析 GitHub API 回傳的 releases JSON
     */
    private List<GitHubRelease> parseReleases(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<GitHubRelease> releases = new ArrayList<>();

        if (root.isArray()) {
            for (JsonNode node : root) {
                String publishedAtStr = node.path("published_at").asText(null);
                OffsetDateTime publishedAt = publishedAtStr != null
                        ? OffsetDateTime.parse(publishedAtStr)
                        : null;

                // 注意：asText() 對 null 值會回傳空字串 ""，需要特別處理
                String name = node.has("name") && !node.get("name").isNull()
                        ? node.get("name").asText()
                        : null;

                releases.add(new GitHubRelease(
                        node.path("id").asLong(),
                        node.path("tag_name").asText(),
                        name,
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
    }
}
