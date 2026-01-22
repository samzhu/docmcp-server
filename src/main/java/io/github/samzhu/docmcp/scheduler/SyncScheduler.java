package io.github.samzhu.docmcp.scheduler;

import io.github.samzhu.docmcp.config.FeatureFlags;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.repository.LibraryRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import io.github.samzhu.docmcp.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 同步排程器
 * <p>
 * 負責定時執行文件同步任務。
 * 預設每天凌晨 2 點執行，可透過配置調整。
 * </p>
 */
@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)");

    private final SyncService syncService;
    private final LibraryRepository libraryRepository;
    private final LibraryVersionRepository versionRepository;
    private final FeatureFlags featureFlags;

    public SyncScheduler(SyncService syncService,
                          LibraryRepository libraryRepository,
                          LibraryVersionRepository versionRepository,
                          FeatureFlags featureFlags) {
        this.syncService = syncService;
        this.libraryRepository = libraryRepository;
        this.versionRepository = versionRepository;
        this.featureFlags = featureFlags;
    }

    /**
     * 定時同步任務
     * <p>
     * 每天凌晨 2 點執行，遍歷所有 GitHub 來源的函式庫並同步最新版本的文件。
     * 只有在 docmcp.features.sync-scheduling=true 時才會執行。
     * </p>
     */
    @Scheduled(cron = "${docmcp.sync.cron:0 0 2 * * *}")
    public void scheduledSync() {
        if (!featureFlags.isSyncScheduling()) {
            log.debug("Sync scheduling is disabled, skipping scheduled sync");
            return;
        }

        log.info("Starting scheduled sync for all libraries");

        try {
            Iterable<Library> libraries = libraryRepository.findAll();

            for (Library library : libraries) {
                if (library.getSourceType() != SourceType.GITHUB) {
                    log.debug("Skipping non-GitHub library: {}", library.getName());
                    continue;
                }

                syncLibrary(library);
            }

            log.info("Scheduled sync completed");

        } catch (Exception e) {
            log.error("Scheduled sync failed", e);
        }
    }

    private void syncLibrary(Library library) {
        String sourceUrl = library.getSourceUrl();
        if (sourceUrl == null || sourceUrl.isBlank()) {
            log.warn("Library {} has no source URL, skipping", library.getName());
            return;
        }

        Matcher matcher = GITHUB_URL_PATTERN.matcher(sourceUrl);
        if (!matcher.find()) {
            log.warn("Invalid GitHub URL for library {}: {}", library.getName(), sourceUrl);
            return;
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);

        List<LibraryVersion> versions = versionRepository.findByLibraryId(library.getId());
        for (LibraryVersion version : versions) {
            try {
                log.info("Syncing library: {} version: {}", library.getName(), version.getVersion());
                String docsPath = version.getDocsPath() != null ? version.getDocsPath() : "docs";
                String ref = version.getVersion();

                syncService.syncFromGitHub(version.getId(), owner, repo, docsPath, ref);

            } catch (Exception e) {
                log.error("Failed to sync library {} version {}: {}",
                        library.getName(), version.getVersion(), e.getMessage());
            }
        }
    }
}
