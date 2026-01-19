package io.github.samzhu.docmcp.web.api;

import io.github.samzhu.docmcp.domain.exception.DocumentNotFoundException;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 全域例外處理器
 * <p>
 * 處理 REST API 的例外，回傳標準的 Problem Detail 格式。
 * </p>
 */
@RestControllerAdvice(basePackages = "io.github.samzhu.docmcp.web.api")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 處理函式庫未找到例外
     */
    @ExceptionHandler(LibraryNotFoundException.class)
    public ProblemDetail handleLibraryNotFoundException(LibraryNotFoundException ex) {
        log.warn("Library not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Library Not Found");
        problem.setType(URI.create("https://docmcp.io/errors/library-not-found"));
        return problem;
    }

    /**
     * 處理文件未找到例外
     */
    @ExceptionHandler(DocumentNotFoundException.class)
    public ProblemDetail handleDocumentNotFoundException(DocumentNotFoundException ex) {
        log.warn("Document not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Document Not Found");
        problem.setType(URI.create("https://docmcp.io/errors/document-not-found"));
        return problem;
    }

    /**
     * 處理同步例外
     */
    @ExceptionHandler(SyncService.SyncException.class)
    public ProblemDetail handleSyncException(SyncService.SyncException ex) {
        log.warn("Sync error: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Sync Error");
        problem.setType(URI.create("https://docmcp.io/errors/sync-error"));
        return problem;
    }

    /**
     * 處理驗證例外
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "請求資料驗證失敗");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://docmcp.io/errors/validation-error"));
        problem.setProperty("errors", errors);
        return problem;
    }

    /**
     * 處理非法參數例外
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        problem.setType(URI.create("https://docmcp.io/errors/bad-request"));
        return problem;
    }

    /**
     * 處理其他未預期的例外
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "發生內部錯誤，請稍後再試"
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://docmcp.io/errors/internal-error"));
        return problem;
    }
}
