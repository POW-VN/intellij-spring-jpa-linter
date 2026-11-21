package com.pow.spring.jpa.linter.explainer;

public class ExplanationResult {
    private final String summary;
    private final String sqlPreview;
    private final String markdownContent;
    private final String htmlContent;

    public ExplanationResult(String summary, String sqlPreview, String markdownContent, String htmlContent) {
        this.summary = summary;
        this.sqlPreview = sqlPreview;
        this.markdownContent = markdownContent;
        this.htmlContent = htmlContent;
    }

    public String getSummary() { return summary; }
    public String getSqlPreview() { return sqlPreview; }
    public String getMarkdownContent() { return markdownContent; }
    public String getHtmlContent() { return htmlContent; }
}
