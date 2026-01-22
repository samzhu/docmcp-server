package io.github.samzhu.docmcp.repository;

import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.model.Library;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 函式庫資料存取介面
 * <p>
 * 提供函式庫的 CRUD 操作及自訂查詢方法。
 * 使用 Spring Data JDBC 實作，ID 類型為 TSID 字串。
 * </p>
 */
@Repository
public interface LibraryRepository extends CrudRepository<Library, String> {

    /**
     * 根據名稱查找函式庫
     *
     * @param name 函式庫名稱
     * @return 函式庫（若存在）
     */
    Optional<Library> findByName(String name);

    /**
     * 根據分類查找函式庫列表
     *
     * @param category 分類名稱
     * @return 符合分類的函式庫列表
     */
    List<Library> findByCategory(String category);

    /**
     * 根據來源類型查找函式庫列表
     *
     * @param sourceType 來源類型
     * @return 符合來源類型的函式庫列表
     */
    @Query("SELECT * FROM libraries WHERE source_type = :sourceType")
    List<Library> findBySourceType(@Param("sourceType") SourceType sourceType);

    /**
     * 取得所有函式庫列表
     *
     * @return 所有函式庫
     */
    @Override
    List<Library> findAll();
}
