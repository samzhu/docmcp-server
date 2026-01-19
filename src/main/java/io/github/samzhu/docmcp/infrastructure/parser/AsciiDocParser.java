package io.github.samzhu.docmcp.infrastructure.parser;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AsciiDoc 文件解析器
 * <p>
 * 使用 asciidoctorj 解析 AsciiDoc 文件。
 * </p>
 */
@Service
public class AsciiDocParser implements DocumentParser {

    private final Asciidoctor asciidoctor;

    public AsciiDocParser() {
        this.asciidoctor = Asciidoctor.Factory.create();
    }

    @Override
    public ParsedDocument parse(String content, String path) {
        if (content == null || content.isBlank()) {
            return new ParsedDocument("", "", List.of(), Map.of());
        }

        try {
            Document document = asciidoctor.load(content, Options.builder().build());

            // 擷取標題
            String title = document.getDoctitle();
            if (title == null || title.isBlank()) {
                title = extractFileNameWithoutExtension(path);
            }

            // 擷取程式碼區塊
            List<ParsedDocument.CodeBlock> codeBlocks = extractCodeBlocks(document);

            // 元資料
            Map<String, Object> metadata = Map.of(
                    "path", path,
                    "format", "asciidoc",
                    "codeBlockCount", codeBlocks.size()
            );

            return new ParsedDocument(title, content, codeBlocks, metadata);
        } catch (Exception e) {
            // 解析失敗時，回傳基本資訊
            return new ParsedDocument(
                    extractFileNameWithoutExtension(path),
                    content,
                    List.of(),
                    Map.of("path", path, "format", "asciidoc", "parseError", e.getMessage())
            );
        }
    }

    @Override
    public boolean supports(String path) {
        if (path == null) return false;
        String lowerPath = path.toLowerCase();
        return lowerPath.endsWith(".adoc") || lowerPath.endsWith(".asciidoc") || lowerPath.endsWith(".asc");
    }

    @Override
    public String getDocType() {
        return "asciidoc";
    }

    private List<ParsedDocument.CodeBlock> extractCodeBlocks(Document document) {
        List<ParsedDocument.CodeBlock> codeBlocks = new ArrayList<>();
        extractCodeBlocksRecursively(document, codeBlocks);
        return codeBlocks;
    }

    private void extractCodeBlocksRecursively(StructuralNode node,
                                               List<ParsedDocument.CodeBlock> result) {
        for (StructuralNode child : node.getBlocks()) {
            if (child instanceof Block block && "listing".equals(child.getContext())) {
                String language = (String) child.getAttribute("language", "text");
                String code = block.getSource();

                result.add(new ParsedDocument.CodeBlock(
                        language,
                        code,
                        block.getTitle() != null ? block.getTitle() : "",
                        0,  // AsciiDoc 不提供行號
                        0
                ));
            }

            // 遞迴處理子節點
            if (child.getBlocks() != null && !child.getBlocks().isEmpty()) {
                extractCodeBlocksRecursively(child, result);
            }
        }
    }

    private String extractFileNameWithoutExtension(String path) {
        if (path == null) return "";

        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;

        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
}
