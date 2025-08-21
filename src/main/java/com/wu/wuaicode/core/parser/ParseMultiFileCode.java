package com.wu.wuaicode.core.parser;

import com.wu.wuaicode.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseMultiFileCode implements CodeParser<MultiFileCodeResult>{

    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * 解析多文件代码（HTML + CSS + JS）
     */
    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        
        // 首先尝试标准格式解析
        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);
        
        // 如果标准格式解析失败，尝试智能提取
        if (htmlCode == null || cssCode == null || jsCode == null) {
            System.out.println("标准格式解析失败，尝试智能提取...");
            return parseCodeIntelligently(codeContent);
        }
        
        // 设置HTML代码
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        // 设置CSS代码
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        // 设置JS代码
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }
        return result;
    }

    /**
     * 智能解析代码（当标准格式失败时使用）
     */
    private MultiFileCodeResult parseCodeIntelligently(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        
        // 查找HTML代码块
        String htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null) {
            // 从HTML中提取内联的CSS和JavaScript
            String inlineCss = extractInlineCss(htmlCode);
            String inlineJs = extractInlineJs(htmlCode);
            
            // 清理HTML代码，移除内联样式和脚本
            String cleanHtml = cleanHtmlCode(htmlCode);
            
            result.setHtmlCode(cleanHtml);
            result.setCssCode(inlineCss != null ? inlineCss : "");
            result.setJsCode(inlineJs != null ? inlineJs : "");
            
            System.out.println("智能提取完成：");
            System.out.println("HTML长度: " + cleanHtml.length());
            System.out.println("CSS长度: " + (inlineCss != null ? inlineCss.length() : 0));
            System.out.println("JS长度: " + (inlineJs != null ? inlineJs.length() : 0));
        }
        
        return result;
    }

    /**
     * 提取HTML代码
     */
    private String extractHtmlCode(String content) {
        // 查找HTML代码块
        Pattern htmlPattern = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher matcher = htmlPattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 如果没有找到HTML代码块，尝试查找整个HTML文档
        Pattern fullHtmlPattern = Pattern.compile("<!DOCTYPE html[\\s\\S]*?</html>", Pattern.CASE_INSENSITIVE);
        matcher = fullHtmlPattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(0);
        }
        
        return null;
    }

    /**
     * 从HTML中提取内联CSS
     */
    private String extractInlineCss(String htmlCode) {
        Pattern stylePattern = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = stylePattern.matcher(htmlCode);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 从HTML中提取内联JavaScript
     */
    private String extractInlineJs(String htmlCode) {
        Pattern scriptPattern = Pattern.compile("<script[^>]*>([\\s\\S]*?)</script>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = scriptPattern.matcher(htmlCode);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 清理HTML代码，移除内联样式和脚本，添加外部引用
     */
    private String cleanHtmlCode(String htmlCode) {
        // 移除<style>标签
        String cleanHtml = htmlCode.replaceAll("<style[^>]*>[\\s\\S]*?</style>", "");
        
        // 移除<script>标签
        cleanHtml = cleanHtml.replaceAll("<script[^>]*>[\\s\\S]*?</script>", "");
        
        // 在</head>之前添加CSS引用
        if (cleanHtml.contains("</head>")) {
            cleanHtml = cleanHtml.replace("</head>", 
                "    <link rel=\"stylesheet\" href=\"style.css\">\n</head>");
        }
        
        // 在</body>之前添加JS引用
        if (cleanHtml.contains("</body>")) {
            cleanHtml = cleanHtml.replace("</body>", 
                "    <script src=\"script.js\"></script>\n</body>");
        }
        
        return cleanHtml.trim();
    }

    /**
     * 根据正则模式提取代码
     *
     * @param content 原始内容
     * @param pattern 正则模式
     * @return 提取的代码
     */
    private static String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
