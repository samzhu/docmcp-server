package io.github.samzhu.docmcp.mcp.dto;

/**
 * 搜尋結果項目
 * <p>
 * 用於表示單一搜尋結果的資訊。
 * </p>
 *
 * @param documentId   文件 ID（TSID 格式）
 * @param chunkId      區塊 ID（語意搜尋時使用，TSID 格式）
 * @param title        文件標題
 * @param path         文件路徑
 * @param content      匹配的內容片段
 * @param score        相關性分數
 * @param chunkIndex   區塊索引（語意搜尋時使用）
 */
public record SearchResultItem(
        String documentId,
        String chunkId,
        String title,
        String path,
        String content,
        double score,
        Integer chunkIndex
) {
    /**
     * 從文件搜尋結果建立
     *
     * @param documentId 文件 ID（TSID 格式）
     */
    public static SearchResultItem fromDocument(String documentId, String title,
                                                 String path, String content, double score) {
        return new SearchResultItem(documentId, null, title, path, content, score, null);
    }

    /**
     * 從區塊搜尋結果建立
     *
     * @param documentId 文件 ID（TSID 格式）
     * @param chunkId    區塊 ID（TSID 格式）
     */
    public static SearchResultItem fromChunk(String documentId, String chunkId, String title,
                                              String path, String content, double score,
                                              int chunkIndex) {
        return new SearchResultItem(documentId, chunkId, title, path, content, score, chunkIndex);
    }

    /**
     * 複製此搜尋結果並更新分數
     *
     * @param newScore 新的分數
     * @return 更新分數後的新搜尋結果項目
     */
    public SearchResultItem withScore(double newScore) {
        return new SearchResultItem(documentId, chunkId, title, path, content, newScore, chunkIndex);
    }
}
