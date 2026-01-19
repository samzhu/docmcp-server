package io.github.samzhu.docmcp.repository;

import io.github.samzhu.docmcp.domain.enums.ApiKeyStatus;
import io.github.samzhu.docmcp.domain.model.ApiKey;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API Key 資料存取介面
 * <p>
 * 提供 API Key 的 CRUD 操作及自訂查詢方法。
 * </p>
 */
@Repository
public interface ApiKeyRepository extends CrudRepository<ApiKey, UUID> {

    /**
     * 根據前綴查找 API Key
     *
     * @param keyPrefix Key 前綴
     * @return API Key（若存在）
     */
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);

    /**
     * 根據狀態查找 API Key 列表
     *
     * @param status API Key 狀態
     * @return 符合狀態的 API Key 列表
     */
    List<ApiKey> findByStatus(ApiKeyStatus status);

    /**
     * 取得所有 API Key（依建立時間降序）
     *
     * @return API Key 列表
     */
    @Query("SELECT * FROM api_keys ORDER BY created_at DESC")
    List<ApiKey> findAllOrderByCreatedAtDesc();

    /**
     * 根據名稱查找 API Key
     *
     * @param name 名稱
     * @return API Key（若存在）
     */
    Optional<ApiKey> findByName(String name);
}
