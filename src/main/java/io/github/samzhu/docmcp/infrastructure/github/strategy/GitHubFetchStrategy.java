package io.github.samzhu.docmcp.infrastructure.github.strategy;

import java.util.Optional;

/**
 * GitHub 內容取得策略介面
 * <p>
 * 定義從 GitHub 取得內容的策略介面。
 * 各策略依優先級順序嘗試，失敗時自動降級到下一個策略。
 * </p>
 * <p>
 * 優先級順序：
 * <ol>
 *   <li>Archive 下載（最快、無 Rate Limit）</li>
 *   <li>Git Tree API（1 次 API 呼叫）</li>
 *   <li>Contents API（遞迴呼叫，有 Rate Limit 風險）</li>
 * </ol>
 * </p>
 */
public interface GitHubFetchStrategy {

    /**
     * 取得策略優先級（數字越小越優先）
     *
     * @return 優先級數值
     */
    int getPriority();

    /**
     * 取得策略名稱（用於日誌和監控）
     *
     * @return 策略名稱
     */
    String getName();

    /**
     * 檢查此策略是否適用於指定的請求
     *
     * @param owner 儲存庫擁有者
     * @param repo  儲存庫名稱
     * @param ref   Git 參考（branch、tag 或 commit）
     * @return 是否支援
     */
    boolean supports(String owner, String repo, String ref);

    /**
     * 執行取得操作
     *
     * @param owner 儲存庫擁有者
     * @param repo  儲存庫名稱
     * @param path  目錄路徑
     * @param ref   Git 參考（branch、tag 或 commit）
     * @return 取得結果，失敗時返回 empty
     */
    Optional<FetchResult> fetch(String owner, String repo, String path, String ref);
}
