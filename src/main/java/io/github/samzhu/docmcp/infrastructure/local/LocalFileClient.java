package io.github.samzhu.docmcp.infrastructure.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地文件讀取客戶端
 * <p>
 * 提供本地文件系統的讀取功能，支援遞迴遍歷目錄和 glob 模式匹配。
 * </p>
 */
@Component
public class LocalFileClient {

    private static final Logger log = LoggerFactory.getLogger(LocalFileClient.class);

    /**
     * 讀取目錄下符合模式的所有文件
     *
     * @param basePath 基礎目錄路徑
     * @param pattern  glob 模式（如 "**\/*.md"）
     * @return 文件內容列表
     * @throws IOException 讀取失敗時拋出
     */
    public List<FileContent> readDirectory(Path basePath, String pattern) throws IOException {
        if (!Files.exists(basePath)) {
            throw new IOException("目錄不存在: " + basePath);
        }

        if (!Files.isDirectory(basePath)) {
            throw new IOException("路徑不是目錄: " + basePath);
        }

        List<FileContent> files = new ArrayList<>();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

        Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relativePath = basePath.relativize(file);
                if (matcher.matches(relativePath)) {
                    try {
                        String content = Files.readString(file, StandardCharsets.UTF_8);
                        files.add(new FileContent(
                                relativePath.toString(),
                                content,
                                attrs.size(),
                                attrs.lastModifiedTime().toMillis()
                        ));
                    } catch (IOException e) {
                        log.warn("無法讀取文件: {}", file, e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.warn("訪問文件失敗: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });

        log.info("從 {} 讀取了 {} 個文件（模式: {}）", basePath, files.size(), pattern);
        return files;
    }

    /**
     * 讀取單一文件
     *
     * @param filePath 文件路徑
     * @return 文件內容
     * @throws IOException 讀取失敗時拋出
     */
    public FileContent readFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("文件不存在: " + filePath);
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IOException("路徑不是文件: " + filePath);
        }

        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
        String content = Files.readString(filePath, StandardCharsets.UTF_8);

        return new FileContent(
                filePath.getFileName().toString(),
                content,
                attrs.size(),
                attrs.lastModifiedTime().toMillis()
        );
    }

    /**
     * 列出目錄下符合模式的所有文件路徑
     *
     * @param basePath 基礎目錄路徑
     * @param pattern  glob 模式
     * @return 相對路徑列表
     * @throws IOException 讀取失敗時拋出
     */
    public List<String> listFiles(Path basePath, String pattern) throws IOException {
        if (!Files.exists(basePath)) {
            throw new IOException("目錄不存在: " + basePath);
        }

        List<String> paths = new ArrayList<>();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

        Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path relativePath = basePath.relativize(file);
                if (matcher.matches(relativePath)) {
                    paths.add(relativePath.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return paths;
    }

    /**
     * 文件內容記錄
     *
     * @param path             相對路徑
     * @param content          文件內容
     * @param size             文件大小（字節）
     * @param lastModifiedTime 最後修改時間（毫秒）
     */
    public record FileContent(
            String path,
            String content,
            long size,
            long lastModifiedTime
    ) {}
}
