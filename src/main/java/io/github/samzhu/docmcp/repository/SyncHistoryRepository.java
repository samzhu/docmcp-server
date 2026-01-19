package io.github.samzhu.docmcp.repository;

import io.github.samzhu.docmcp.domain.enums.SyncStatus;
import io.github.samzhu.docmcp.domain.model.SyncHistory;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 同步歷史資料存取介面
 * <p>
 * 提供同步歷史的 CRUD 操作及自訂查詢方法。
 * </p>
 */
@Repository
public interface SyncHistoryRepository extends CrudRepository<SyncHistory, UUID> {

    /**
     * 取得指定版本的同步歷史（依開始時間降序）
     *
     * @param versionId 版本 ID
     * @return 同步歷史列表
     */
    @Query("SELECT * FROM sync_history WHERE version_id = :versionId ORDER BY started_at DESC")
    List<SyncHistory> findByVersionIdOrderByStartedAtDesc(@Param("versionId") UUID versionId);

    /**
     * 取得指定版本最新的特定狀態同步記錄
     *
     * @param versionId 版本 ID
     * @param status    同步狀態
     * @return 同步歷史（若存在）
     */
    @Query("""
            SELECT * FROM sync_history
            WHERE version_id = :versionId AND status = :status
            ORDER BY started_at DESC
            LIMIT 1
            """)
    Optional<SyncHistory> findFirstByVersionIdAndStatusOrderByStartedAtDesc(
            @Param("versionId") UUID versionId,
            @Param("status") String status
    );

    /**
     * 取得指定版本最新的同步記錄
     *
     * @param versionId 版本 ID
     * @return 最新的同步歷史（若存在）
     */
    @Query("""
            SELECT * FROM sync_history
            WHERE version_id = :versionId
            ORDER BY started_at DESC
            LIMIT 1
            """)
    Optional<SyncHistory> findLatestByVersionId(@Param("versionId") UUID versionId);

    /**
     * 檢查是否有正在執行的同步任務
     *
     * @param versionId 版本 ID
     * @return 是否有執行中的任務
     */
    @Query("""
            SELECT EXISTS(
                SELECT 1 FROM sync_history
                WHERE version_id = :versionId AND status = 'RUNNING'
            )
            """)
    boolean hasRunningSyncTask(@Param("versionId") UUID versionId);

    /**
     * 取得所有同步歷史（依開始時間降序，限制數量）
     *
     * @param limit 結果數量上限
     * @return 同步歷史列表
     */
    @Query("SELECT * FROM sync_history ORDER BY started_at DESC LIMIT :limit")
    List<SyncHistory> findAllOrderByStartedAtDesc(@Param("limit") int limit);

    /**
     * 取得指定版本的同步歷史（限制數量）
     *
     * @param versionId 版本 ID
     * @param limit     結果數量上限
     * @return 同步歷史列表
     */
    @Query("""
            SELECT * FROM sync_history
            WHERE version_id = :versionId
            ORDER BY started_at DESC
            LIMIT :limit
            """)
    List<SyncHistory> findByVersionIdOrderByStartedAtDescLimit(
            @Param("versionId") UUID versionId,
            @Param("limit") int limit
    );
}
