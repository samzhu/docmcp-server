package io.github.samzhu.docmcp.domain.exception;

/**
 * 文件未找到例外
 * <p>
 * 當嘗試存取不存在的文件時拋出此例外。
 * </p>
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String message) {
        super(message);
    }

    public DocumentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 根據文件 ID 建立例外
     *
     * @param id 文件 ID（TSID 格式）
     * @return 例外實例
     */
    public static DocumentNotFoundException byId(String id) {
        return new DocumentNotFoundException("找不到文件 ID: " + id);
    }

    /**
     * 根據文件路徑建立例外
     *
     * @param path 文件路徑
     * @return 例外實例
     */
    public static DocumentNotFoundException byPath(String path) {
        return new DocumentNotFoundException("找不到文件路徑: " + path);
    }
}
