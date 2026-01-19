package io.github.samzhu.docmcp.infrastructure.parser;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTML 文件解析器
 * <p>
 * 使用 jsoup 解析 HTML 文件，並可轉換為 Markdown。
 * </p>
 */
@Service
public class HtmlParser implements DocumentParser {

    private final FlexmarkHtmlConverter htmlToMarkdownConverter;

    public HtmlParser() {
        this.htmlToMarkdownConverter = FlexmarkHtmlConverter.builder().build();
    }

    @Override
    public ParsedDocument parse(String content, String path) {
        if (content == null || content.isBlank()) {
            return new ParsedDocument("", "", List.of(), Map.of());
        }

        try {
            Document document = Jsoup.parse(content);

            // 擷取標題
            String title = extractTitle(document, path);

            // 擷取程式碼區塊
            List<ParsedDocument.CodeBlock> codeBlocks = extractCodeBlocks(document);

            // 轉換為 Markdown（方便後續處理）
            String markdownContent = convertToMarkdown(document);

            // 元資料
            Map<String, Object> metadata = Map.of(
                    "path", path,
                    "format", "html",
                    "originalFormat", "html",
                    "codeBlockCount", codeBlocks.size()
            );

            return new ParsedDocument(title, markdownContent, codeBlocks, metadata);
        } catch (Exception e) {
            return new ParsedDocument(
                    extractFileNameWithoutExtension(path),
                    content,
                    List.of(),
                    Map.of("path", path, "format", "html", "parseError", e.getMessage())
            );
        }
    }

    @Override
    public boolean supports(String path) {
        if (path == null) return false;
        String lowerPath = path.toLowerCase();
        return lowerPath.endsWith(".html") || lowerPath.endsWith(".htm");
    }

    @Override
    public String getDocType() {
        return "html";
    }

    private String extractTitle(Document document, String path) {
        // 嘗試從 <title> 標籤取得
        String title = document.title();
        if (title != null && !title.isBlank()) {
            return title;
        }

        // 嘗試從第一個 <h1> 取得
        Element h1 = document.selectFirst("h1");
        if (h1 != null) {
            return h1.text();
        }

        // 使用檔案名稱
        return extractFileNameWithoutExtension(path);
    }

    private List<ParsedDocument.CodeBlock> extractCodeBlocks(Document document) {
        List<ParsedDocument.CodeBlock> codeBlocks = new ArrayList<>();

        // 尋找 <pre><code> 區塊
        Elements codeElements = document.select("pre code, pre.highlight, code.highlight");

        for (Element element : codeElements) {
            String language = detectLanguage(element);
            String code = element.text();

            // 取得前一個文字元素作為描述
            String description = "";
            Element prev = element.parent().previousElementSibling();
            if (prev != null && (prev.tagName().equals("p") || prev.tagName().matches("h[1-6]"))) {
                description = prev.text();
                if (description.length() > 200) {
                    description = description.substring(0, 200) + "...";
                }
            }

            codeBlocks.add(new ParsedDocument.CodeBlock(
                    language,
                    code,
                    description,
                    0,
                    0
            ));
        }

        return codeBlocks;
    }

    private String detectLanguage(Element element) {
        // 從 class 屬性檢測語言
        String className = element.className();
        if (className != null && !className.isEmpty()) {
            // 常見的語言類別模式
            if (className.contains("language-")) {
                int start = className.indexOf("language-") + 9;
                int end = className.indexOf(' ', start);
                return end > start ? className.substring(start, end) : className.substring(start);
            }
            if (className.contains("lang-")) {
                int start = className.indexOf("lang-") + 5;
                int end = className.indexOf(' ', start);
                return end > start ? className.substring(start, end) : className.substring(start);
            }
            // 直接使用第一個類別名稱
            String[] classes = className.split("\\s+");
            for (String cls : classes) {
                if (isLikelyLanguage(cls)) {
                    return cls;
                }
            }
        }

        // 從 data-language 屬性取得
        String dataLang = element.attr("data-language");
        if (!dataLang.isEmpty()) {
            return dataLang;
        }

        return "text";
    }

    private boolean isLikelyLanguage(String className) {
        String[] commonLanguages = {
                "java", "javascript", "js", "python", "py", "ruby", "go", "rust",
                "c", "cpp", "csharp", "cs", "typescript", "ts", "kotlin", "swift",
                "php", "bash", "shell", "sh", "sql", "yaml", "yml", "json", "xml",
                "html", "css", "scss", "sass", "markdown", "md"
        };

        for (String lang : commonLanguages) {
            if (className.equalsIgnoreCase(lang)) {
                return true;
            }
        }
        return false;
    }

    private String convertToMarkdown(Document document) {
        // 移除 script 和 style 標籤
        document.select("script, style, nav, footer, header").remove();

        // 取得主要內容（嘗試常見的內容選擇器）
        Element content = document.selectFirst("article, main, .content, #content, .documentation");
        if (content == null) {
            content = document.body();
        }

        if (content == null) {
            return "";
        }

        return htmlToMarkdownConverter.convert(content.html());
    }

    private String extractFileNameWithoutExtension(String path) {
        if (path == null) return "";

        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;

        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
}
