package io.github.samzhu.docmcp.infrastructure.github.strategy;

import io.github.samzhu.docmcp.infrastructure.github.GitHubFetchProperties;
import io.github.samzhu.docmcp.infrastructure.github.GitHubFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Archive 下載策略（優先級 1）
 * <p>
 * 直接下載 GitHub tarball，解壓後取得所有檔案。
 * </p>
 * <p>
 * 優點：
 * <ul>
 *   <li>只需 1 次 HTTP 請求</li>
 *   <li>無 Rate Limit</li>
 *   <li>最快的方式</li>
 *   <li>預載入所有檔案內容，無需額外下載</li>
 * </ul>
 * </p>
 * <p>
 * 缺點：
 * <ul>
 *   <li>不是所有專案都有 Release/Tag</li>
 *   <li>會下載整個專案（可能包含不需要的檔案）</li>
 * </ul>
 * </p>
 */
@Component
@ConditionalOnProperty(name = "docmcp.github.fetch.archive.enabled", havingValue = "true", matchIfMissing = true)
public class ArchiveFetchStrategy implements GitHubFetchStrategy {

    private static final Logger log = LoggerFactory.getLogger(ArchiveFetchStrategy.class);
    private static final String GITHUB_ARCHIVE_URL = "https://github.com/%s/%s/archive/refs/tags/%s.tar.gz";

    // 支援的文件副檔名
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".md", ".markdown", ".adoc", ".asciidoc", ".html", ".htm", ".txt", ".rst"
    );

    private final RestClient restClient;
    private final GitHubFetchProperties properties;

    public ArchiveFetchStrategy(RestClient.Builder restClientBuilder, GitHubFetchProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public int getPriority() {
        return properties.getArchive().getPriority();
    }

    @Override
    public String getName() {
        return "Archive";
    }

    @Override
    public boolean supports(String owner, String repo, String ref) {
        // 只支援 tag 格式（v1.0.0, 1.0.0, v4.0.1 等）
        // 不支援 branch 名稱如 main, master
        return ref.matches("^v?\\d+(\\.\\d+)*.*$");
    }

    @Override
    public Optional<FetchResult> fetch(String owner, String repo, String path, String ref) {
        String url = String.format(GITHUB_ARCHIVE_URL, owner, repo, ref);
        log.info("嘗試下載 Archive: {}", url);

        // 使用 OS 暫存目錄，避免大檔案導致 OOM
        Path tempFile = null;
        try {
            // 建立暫存檔（使用 OS 預設 temp 目錄）
            tempFile = Files.createTempFile("docmcp-archive-", ".tar.gz");
            log.debug("建立暫存檔: {}", tempFile);

            // 串流下載到暫存檔，避免整個檔案載入記憶體
            Resource resource = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(Resource.class);

            if (resource == null) {
                log.warn("Archive 下載失敗：空回應");
                return Optional.empty();
            }

            // 串流寫入暫存檔
            try (InputStream inputStream = resource.getInputStream();
                 OutputStream outputStream = Files.newOutputStream(tempFile)) {
                long bytesWritten = inputStream.transferTo(outputStream);
                log.info("Archive 下載成功，大小: {} bytes，暫存於: {}", bytesWritten, tempFile);
            }

            // 從暫存檔解壓並提取檔案
            return extractFilesFromPath(tempFile, owner, repo, path, ref);

        } catch (Exception e) {
            log.warn("Archive 策略失敗: {}", e.getMessage());
            return Optional.empty();
        } finally {
            // 清理暫存檔（成功或失敗都要刪除）
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.debug("已刪除暫存檔: {}", tempFile);
                } catch (IOException e) {
                    log.warn("刪除暫存檔失敗: {} - {}", tempFile, e.getMessage());
                }
            }
        }
    }

    /**
     * 從暫存檔解壓 tarball 並提取指定路徑下的檔案
     * <p>
     * 使用串流處理，避免將整個 tarball 載入記憶體
     * </p>
     *
     * @param tarballPath 暫存檔路徑
     * @param owner       儲存庫擁有者
     * @param repo        儲存庫名稱
     * @param targetPath  目標目錄路徑
     * @param ref         Git 參考
     * @return 取得結果
     */
    private Optional<FetchResult> extractFilesFromPath(Path tarballPath, String owner, String repo,
                                                        String targetPath, String ref) {
        List<GitHubFile> files = new ArrayList<>();
        Map<String, String> contents = new HashMap<>();

        // 計算 tarball 內的根目錄名稱（通常是 repo-tag）
        // 例如：spring-boot-4.0.1/
        String tagWithoutV = ref.startsWith("v") ? ref.substring(1) : ref;
        String rootPrefix = repo + "-" + tagWithoutV + "/";
        String targetPrefix = rootPrefix + targetPath;
        if (!targetPrefix.endsWith("/")) {
            targetPrefix += "/";
        }

        log.debug("解壓 Archive，目標路徑: {}", targetPrefix);

        // 使用 BufferedInputStream 提高讀取效率
        try (InputStream fis = Files.newInputStream(tarballPath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {

            TarArchiveEntry entry;
            int fileCount = 0;

            while ((entry = tais.getNextEntry()) != null) {
                String entryName = entry.getName();

                // 檢查是否在目標路徑下
                if (!entryName.startsWith(targetPrefix)) {
                    continue;
                }

                // 取得相對路徑（移除根目錄前綴）
                String relativePath = entryName.substring(rootPrefix.length());

                if (entry.isDirectory()) {
                    continue;
                }

                // 檢查是否為支援的檔案格式
                if (!isSupportedFile(entryName)) {
                    continue;
                }

                // 讀取檔案內容（單一文件通常不會太大，可以載入記憶體）
                byte[] contentBytes = tais.readNBytes((int) entry.getSize());
                String content = new String(contentBytes, StandardCharsets.UTF_8);

                // 建立 GitHubFile
                String fileName = entryName.substring(entryName.lastIndexOf('/') + 1);
                GitHubFile file = new GitHubFile(
                        fileName,
                        relativePath,
                        "", // sha 在 Archive 中不可用
                        entry.getSize(),
                        "file",
                        null // download_url 不需要，因為內容已預載入
                );

                files.add(file);
                contents.put(relativePath, content);
                fileCount++;

                if (fileCount % 50 == 0) {
                    log.debug("已處理 {} 個檔案...", fileCount);
                }
            }

            if (files.isEmpty()) {
                log.warn("Archive 中未找到任何符合條件的檔案，目標路徑: {}", targetPath);
                return Optional.empty();
            }

            log.info("Archive 解壓完成，找到 {} 個檔案", files.size());
            return Optional.of(FetchResult.withContents(files, contents, getName()));

        } catch (IOException e) {
            log.error("解壓 Archive 失敗: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 檢查是否為支援的檔案格式
     */
    private boolean isSupportedFile(String path) {
        String lowerPath = path.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lowerPath::endsWith);
    }
}
