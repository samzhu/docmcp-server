package io.github.samzhu.docmcp.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件分塊服務
 * <p>
 * 將長文件分割成適合向量嵌入的小區塊。
 * 使用滑動視窗策略，保持區塊間的上下文重疊。
 * </p>
 */
@Service
public class DocumentChunker {

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_OVERLAP = 200;

    /**
     * 將文件分割成區塊（使用預設參數）
     *
     * @param content 文件內容
     * @return 區塊結果列表
     */
    public List<ChunkResult> chunk(String content) {
        return chunk(content, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    /**
     * 將文件分割成區塊
     * <p>
     * 使用滑動視窗策略，在段落或句子邊界進行分割，
     * 並保持區塊間的上下文重疊。
     * </p>
     *
     * @param content   文件內容
     * @param chunkSize 區塊大小（字元數）
     * @param overlap   重疊大小（字元數）
     * @return 區塊結果列表
     */
    public List<ChunkResult> chunk(String content, int chunkSize, int overlap) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        // 驗證參數
        if (chunkSize <= 0) {
            chunkSize = DEFAULT_CHUNK_SIZE;
        }
        if (overlap < 0 || overlap >= chunkSize) {
            overlap = Math.min(DEFAULT_OVERLAP, chunkSize / 5);
        }

        List<ChunkResult> chunks = new ArrayList<>();
        int contentLength = content.length();

        // 如果內容小於區塊大小，直接回傳單一區塊
        if (contentLength <= chunkSize) {
            chunks.add(new ChunkResult(0, content, estimateTokenCount(content)));
            return chunks;
        }

        int start = 0;
        int chunkIndex = 0;

        while (start < contentLength) {
            int end = Math.min(start + chunkSize, contentLength);

            // 如果不是最後一個區塊，嘗試在自然邊界處分割
            if (end < contentLength) {
                end = findNaturalBreakPoint(content, start, end);
            }

            String chunkContent = content.substring(start, end);
            chunks.add(new ChunkResult(chunkIndex, chunkContent, estimateTokenCount(chunkContent)));

            // 移動起始位置（考慮重疊）
            int step = end - start - overlap;
            if (step <= 0) {
                step = chunkSize - overlap;
            }
            start += step;
            chunkIndex++;

            // 防止無限迴圈
            if (start >= contentLength) {
                break;
            }
        }

        return chunks;
    }

    /**
     * 在自然邊界處尋找分割點
     * <p>
     * 優先順序：段落邊界 > 句子邊界 > 單詞邊界 > 原位置
     * </p>
     */
    private int findNaturalBreakPoint(String content, int start, int preferredEnd) {
        int searchStart = Math.max(start, preferredEnd - 200);

        // 優先尋找段落邊界（雙換行）
        int paragraphBreak = content.lastIndexOf("\n\n", preferredEnd);
        if (paragraphBreak >= searchStart) {
            return paragraphBreak + 2;
        }

        // 尋找單換行
        int lineBreak = content.lastIndexOf("\n", preferredEnd);
        if (lineBreak >= searchStart) {
            return lineBreak + 1;
        }

        // 尋找句子邊界（句號、問號、驚嘆號後跟空格）
        for (int i = preferredEnd; i >= searchStart; i--) {
            char c = content.charAt(i);
            if ((c == '.' || c == '!' || c == '?' || c == '。' || c == '！' || c == '？') &&
                    i + 1 < content.length() && Character.isWhitespace(content.charAt(i + 1))) {
                return i + 2;
            }
        }

        // 尋找單詞邊界（空格）
        int spaceBreak = content.lastIndexOf(" ", preferredEnd);
        if (spaceBreak >= searchStart) {
            return spaceBreak + 1;
        }

        // 找不到好的分割點，使用原位置
        return preferredEnd;
    }

    /**
     * 估算 token 數量
     * <p>
     * 使用簡單的規則估算：
     * - 英文大約 4 個字元 = 1 token
     * - 中文大約 1.5 個字元 = 1 token
     * </p>
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int englishChars = 0;
        int chineseChars = 0;
        int otherChars = 0;

        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                if (c >= '\u4e00' && c <= '\u9fff') {
                    chineseChars++;
                } else {
                    englishChars++;
                }
            } else {
                otherChars++;
            }
        }

        // 估算 token 數量
        return (int) (englishChars / 4.0 + chineseChars / 1.5 + otherChars / 4.0);
    }

    /**
     * 區塊結果
     *
     * @param index      區塊索引（從 0 開始）
     * @param content    區塊內容
     * @param tokenCount 估算的 token 數量
     */
    public record ChunkResult(
            int index,
            String content,
            int tokenCount
    ) {}
}
