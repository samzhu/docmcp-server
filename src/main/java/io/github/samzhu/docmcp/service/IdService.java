package io.github.samzhu.docmcp.service;

import com.github.f4b6a3.tsid.TsidFactory;
import org.springframework.stereotype.Service;

/**
 * ID 生成服務
 * <p>
 * 使用 TSID (Time-Sorted Unique Identifier) 生成唯一識別碼。
 * TSID 是一種時間排序的唯一識別碼，格式為 13 字元的 Crockford Base32 字串，
 * 例如：0HZXJ8KYPKA9E
 * </p>
 *
 * <h3>使用範例</h3>
 * <pre>{@code
 * String newId = idService.generateId();
 * Library library = Library.create(newId, "spring-boot", ...);
 * }</pre>
 */
@Service
public class IdService {

    private final TsidFactory tsidFactory;

    public IdService(TsidFactory tsidFactory) {
        this.tsidFactory = tsidFactory;
    }

    /**
     * 生成新的 TSID
     * <p>
     * 產生 13 字元的 Crockford Base32 格式字串，
     * 具有時間排序特性，適合作為資料庫主鍵。
     * </p>
     *
     * @return 13 字元的 TSID 字串（例如：0HZXJ8KYPKA9E）
     */
    public String generateId() {
        return tsidFactory.create().toString();
    }
}
