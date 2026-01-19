package io.github.samzhu.docmcp.domain.exception;

/**
 * 函式庫未找到例外
 * <p>
 * 當嘗試存取不存在的函式庫時拋出此例外。
 * </p>
 */
public class LibraryNotFoundException extends RuntimeException {

    public LibraryNotFoundException(String message) {
        super(message);
    }

    public LibraryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 根據函式庫名稱建立例外
     *
     * @param name 函式庫名稱
     * @return 例外實例
     */
    public static LibraryNotFoundException byName(String name) {
        return new LibraryNotFoundException("找不到函式庫: " + name);
    }

    /**
     * 根據函式庫 ID 建立例外
     *
     * @param id 函式庫 ID
     * @return 例外實例
     */
    public static LibraryNotFoundException byId(String id) {
        return new LibraryNotFoundException("找不到函式庫 ID: " + id);
    }
}
