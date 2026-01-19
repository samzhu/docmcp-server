package io.github.samzhu.docmcp.repository;

import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 函式庫版本資料存取介面
 * <p>
 * 提供函式庫版本的 CRUD 操作及自訂查詢方法。
 * </p>
 */
@Repository
public interface LibraryVersionRepository extends CrudRepository<LibraryVersion, UUID> {

    /**
     * 查找指定函式庫的最新版本
     *
     * @param libraryId 函式庫 ID
     * @return 最新版本（若存在）
     */
    @Query("SELECT * FROM library_versions WHERE library_id = :libraryId AND is_latest = true")
    Optional<LibraryVersion> findLatestByLibraryId(@Param("libraryId") UUID libraryId);

    /**
     * 查找指定函式庫的特定版本
     *
     * @param libraryId 函式庫 ID
     * @param version   版本號
     * @return 版本資訊（若存在）
     */
    @Query("SELECT * FROM library_versions WHERE library_id = :libraryId AND version = :version")
    Optional<LibraryVersion> findByLibraryIdAndVersion(
            @Param("libraryId") UUID libraryId,
            @Param("version") String version
    );

    /**
     * 取得指定函式庫的所有版本
     *
     * @param libraryId 函式庫 ID
     * @return 版本列表
     */
    @Query("SELECT * FROM library_versions WHERE library_id = :libraryId ORDER BY created_at DESC")
    List<LibraryVersion> findByLibraryId(@Param("libraryId") UUID libraryId);

    /**
     * 取得指定函式庫中特定狀態的版本
     *
     * @param libraryId 函式庫 ID
     * @param status    版本狀態
     * @return 符合條件的版本列表
     */
    @Query("SELECT * FROM library_versions WHERE library_id = :libraryId AND status = :status")
    List<LibraryVersion> findByLibraryIdAndStatus(
            @Param("libraryId") UUID libraryId,
            @Param("status") VersionStatus status
    );
}
