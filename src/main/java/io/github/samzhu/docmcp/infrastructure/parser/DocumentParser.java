package io.github.samzhu.docmcp.infrastructure.parser;

/**
 * 文件解析器介面
 * <p>
 * 定義文件解析的標準介面，支援不同格式的文件解析。
 * </p>
 */
public interface DocumentParser {

    /**
     * 解析文件內容
     *
     * @param content 文件原始內容
     * @param path    文件路徑（用於判斷格式和設定元資料）
     * @return 解析後的文件
     */
    ParsedDocument parse(String content, String path);

    /**
     * 是否支援此檔案類型
     *
     * @param path 檔案路徑
     * @return 是否支援
     */
    boolean supports(String path);

    /**
     * 取得解析器支援的文件類型名稱
     *
     * @return 文件類型名稱
     */
    String getDocType();
}
