package io.github.samzhu.docmcp.repository;

import io.github.samzhu.docmcp.domain.model.CodeExample;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 程式碼範例資料存取介面
 * <p>
 * 提供程式碼範例的 CRUD 操作及自訂查詢方法。
 * ID 類型為 TSID 字串。
 * </p>
 */
@Repository
public interface CodeExampleRepository extends CrudRepository<CodeExample, String> {

    /**
     * 取得指定文件的所有程式碼範例
     *
     * @param documentId 文件 ID（TSID 格式）
     * @return 程式碼範例列表
     */
    @Query("SELECT * FROM code_examples WHERE document_id = :documentId ORDER BY start_line")
    List<CodeExample> findByDocumentId(@Param("documentId") String documentId);

    /**
     * 依函式庫和語言查詢程式碼範例
     * <p>
     * 可指定版本和程式語言進行篩選，依建立時間降序排列。
     * </p>
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @param version   版本（可為 null 表示最新版本）
     * @param language  程式語言（可為 null 表示不限語言）
     * @param limit     結果數量上限
     * @return 程式碼範例列表
     */
    @Query("""
            SELECT ce.* FROM code_examples ce
            JOIN documents d ON ce.document_id = d.id
            JOIN library_versions lv ON d.version_id = lv.id
            WHERE lv.library_id = :libraryId
            AND (:version IS NULL OR lv.version = :version)
            AND (:language IS NULL OR ce.language = :language)
            ORDER BY ce.created_at DESC
            LIMIT :limit
            """)
    List<CodeExample> findByLibraryAndLanguage(
            @Param("libraryId") String libraryId,
            @Param("version") String version,
            @Param("language") String language,
            @Param("limit") int limit
    );

    /**
     * 取得指定函式庫中所有可用的程式語言
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @param version   版本（可為 null）
     * @return 程式語言列表
     */
    @Query("""
            SELECT DISTINCT ce.language FROM code_examples ce
            JOIN documents d ON ce.document_id = d.id
            JOIN library_versions lv ON d.version_id = lv.id
            WHERE lv.library_id = :libraryId
            AND (:version IS NULL OR lv.version = :version)
            ORDER BY ce.language
            """)
    List<String> findDistinctLanguagesByLibrary(
            @Param("libraryId") String libraryId,
            @Param("version") String version
    );
}
