package io.github.samzhu.docmcp.infrastructure.parser;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Markdown 文件解析器
 * <p>
 * 使用 flexmark 解析 Markdown 文件，擷取標題、內容和程式碼區塊。
 * </p>
 */
@Service
public class MarkdownParser implements DocumentParser {

    private final Parser parser;

    public MarkdownParser() {
        this.parser = Parser.builder().build();
    }

    @Override
    public ParsedDocument parse(String content, String path) {
        if (content == null || content.isBlank()) {
            return new ParsedDocument("", "", List.of(), Map.of());
        }

        Node document = parser.parse(content);

        // 擷取標題
        String title = extractTitle(document, path);

        // 擷取程式碼區塊
        List<ParsedDocument.CodeBlock> codeBlocks = extractCodeBlocks(document);

        // 元資料
        Map<String, Object> metadata = Map.of(
                "path", path,
                "format", "markdown",
                "codeBlockCount", codeBlocks.size()
        );

        return new ParsedDocument(title, content, codeBlocks, metadata);
    }

    @Override
    public boolean supports(String path) {
        if (path == null) return false;
        String lowerPath = path.toLowerCase();
        return lowerPath.endsWith(".md") || lowerPath.endsWith(".markdown");
    }

    @Override
    public String getDocType() {
        return "markdown";
    }

    private String extractTitle(Node document, String path) {
        // 嘗試從第一個 H1 標題取得標題
        TitleVisitor visitor = new TitleVisitor();
        visitor.visit(document);

        if (visitor.title != null) {
            return visitor.title;
        }

        // 如果沒有 H1，使用檔案名稱
        return extractFileNameWithoutExtension(path);
    }

    private List<ParsedDocument.CodeBlock> extractCodeBlocks(Node document) {
        CodeBlockVisitor visitor = new CodeBlockVisitor();
        visitor.visit(document);
        return visitor.codeBlocks;
    }

    private String extractFileNameWithoutExtension(String path) {
        if (path == null) return "";

        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;

        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    /**
     * 標題擷取訪問器
     */
    private static class TitleVisitor {
        String title = null;
        private final NodeVisitor visitor;

        TitleVisitor() {
            this.visitor = new NodeVisitor(new VisitHandler<>(Heading.class, this::visitHeading));
        }

        void visit(Node node) {
            visitor.visit(node);
        }

        void visitHeading(Heading heading) {
            if (title == null && heading.getLevel() == 1) {
                title = heading.getText().toString().trim();
            }
        }
    }

    /**
     * 程式碼區塊擷取訪問器
     */
    private static class CodeBlockVisitor {
        List<ParsedDocument.CodeBlock> codeBlocks = new ArrayList<>();
        private final NodeVisitor visitor;

        CodeBlockVisitor() {
            this.visitor = new NodeVisitor(new VisitHandler<>(FencedCodeBlock.class, this::visitFencedCodeBlock));
        }

        void visit(Node node) {
            visitor.visit(node);
        }

        void visitFencedCodeBlock(FencedCodeBlock codeBlock) {
            String language = codeBlock.getInfo().toString().trim();
            String code = codeBlock.getContentChars().toString();

            // 取得程式碼前的文字作為描述
            String description = "";
            Node prev = codeBlock.getPrevious();
            if (prev != null) {
                description = prev.getChars().toString().trim();
                // 限制描述長度
                if (description.length() > 200) {
                    description = description.substring(0, 200) + "...";
                }
            }

            int startLine = codeBlock.getStartLineNumber();
            int endLine = codeBlock.getEndLineNumber();

            codeBlocks.add(new ParsedDocument.CodeBlock(
                    language.isEmpty() ? "text" : language,
                    code,
                    description,
                    startLine,
                    endLine
            ));
        }
    }
}
