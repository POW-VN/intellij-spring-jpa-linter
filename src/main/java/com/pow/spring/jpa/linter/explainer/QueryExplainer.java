package com.pow.spring.jpa.linter.explainer;

import com.pow.spring.jpa.linter.parser.*;

import java.util.*;

public class QueryExplainer {

    public static ExplanationResult explainQueryMethod(
            ParsedQueryMethod parsed,
            String entityName,
            List<String> paramNames,
            String lang
    ) {
        if (!parsed.isDerivedQuery()) {
            return new ExplanationResult("", "", "", "");
        }

        if (entityName == null || entityName.isEmpty()) {
            entityName = "Entity";
        }
        if (lang == null || lang.isEmpty()) {
            lang = "en";
        }

        String alias = String.valueOf(Character.toLowerCase(entityName.charAt(0)));
        int paramIndex = 0;

        String actionDesc = "";
        String selectClause = alias;
        boolean isCount = false;
        boolean isExists = false;
        boolean isDelete = false;

        String subject = parsed.getSubject() != null ? parsed.getSubject() : "find";

        if ("count".equals(subject)) {
            actionDesc = "vi".equals(lang) ? "Đếm số lượng bản ghi `" + entityName + "`" : "Count the number of `" + entityName + "` records";
            selectClause = "COUNT(" + alias + ")";
            isCount = true;
        } else if ("exists".equals(subject)) {
            actionDesc = "vi".equals(lang) ? "Kiểm tra sự tồn tại của bản ghi `" + entityName + "`" : "Check if any `" + entityName + "` record exists";
            selectClause = "CASE WHEN COUNT(" + alias + ") > 0 THEN true ELSE false END";
            isExists = true;
        } else if ("delete".equals(subject) || "remove".equals(subject)) {
            actionDesc = "vi".equals(lang) ? "Xóa các bản ghi `" + entityName + "`" : "Delete `" + entityName + "` records";
            isDelete = true;
        } else {
            if (parsed.getLimit() != null && parsed.getLimit() == 1) {
                actionDesc = "vi".equals(lang) ? "Tìm bản ghi đầu tiên của `" + entityName + "`" : "Find the first `" + entityName + "` record";
            } else if (parsed.getLimit() != null && parsed.getLimit() > 1) {
                actionDesc = "vi".equals(lang) ? "Tìm tối đa " + parsed.getLimit() + " bản ghi đầu tiên của `" + entityName + "`" : "Find up to top " + parsed.getLimit() + " `" + entityName + "` records";
            } else {
                actionDesc = "vi".equals(lang) ? "Tìm kiếm các bản ghi `" + entityName + "`" : "Find `" + entityName + "` records";
            }

            if (parsed.isDistinct()) {
                actionDesc += "vi".equals(lang) ? " (không trùng lặp)" : " (distinct)";
                selectClause = "DISTINCT " + alias;
            }
        }

        List<String> criteriaDescriptions = new ArrayList<>();
        List<String> whereClauses = new ArrayList<>();

        for (int i = 0; i < parsed.getConditions().size(); i++) {
            QueryCondition cond = parsed.getConditions().get(i);
            String rawProp = cond.getPropertyName();
            String propPath = formatPropertyPath(rawProp, alias);

            String condText = "";
            String sqlCond = "";

            String fallbackP1 = ":" + decapitalize(rawProp);
            String p1Name = (paramNames != null && paramIndex < paramNames.size()) ? ":" + paramNames.get(paramIndex++) : fallbackP1;

            switch (cond.getOperator()) {
                case "IsNull":
                    condText = "vi".equals(lang) ? "`" + rawProp + "` là NULL" : "`" + rawProp + "` is NULL";
                    sqlCond = propPath + " IS NULL";
                    break;
                case "IsNotNull":
                case "NotNull":
                    condText = "vi".equals(lang) ? "`" + rawProp + "` khác NULL" : "`" + rawProp + "` is NOT NULL";
                    sqlCond = propPath + " IS NOT NULL";
                    break;
                case "IsTrue":
                case "True":
                    condText = "vi".equals(lang) ? "`" + rawProp + "` là TRUE" : "`" + rawProp + "` is TRUE";
                    sqlCond = propPath + " = true";
                    break;
                case "IsFalse":
                case "False":
                    condText = "vi".equals(lang) ? "`" + rawProp + "` là FALSE" : "`" + rawProp + "` is FALSE";
                    sqlCond = propPath + " = false";
                    break;
                case "Between": {
                    String fallbackP2 = ":" + decapitalize(rawProp) + "End";
                    String p2Name = (paramNames != null && paramIndex < paramNames.size()) ? ":" + paramNames.get(paramIndex++) : fallbackP2;
                    condText = "vi".equals(lang) ? "`" + rawProp + "` trong khoảng " + p1Name + " và " + p2Name : "`" + rawProp + "` between " + p1Name + " and " + p2Name;
                    sqlCond = propPath + " BETWEEN " + p1Name + " AND " + p2Name;
                    break;
                }
                case "LessThan":
                case "Before":
                    condText = "`" + rawProp + "` < " + p1Name;
                    sqlCond = propPath + " < " + p1Name;
                    break;
                case "LessThanEqual":
                case "BeforeEqual":
                    condText = "`" + rawProp + "` <= " + p1Name;
                    sqlCond = propPath + " <= " + p1Name;
                    break;
                case "GreaterThan":
                case "After":
                    condText = "`" + rawProp + "` > " + p1Name;
                    sqlCond = propPath + " > " + p1Name;
                    break;
                case "GreaterThanEqual":
                case "AfterEqual":
                    condText = "`" + rawProp + "` >= " + p1Name;
                    sqlCond = propPath + " >= " + p1Name;
                    break;
                case "In":
                    condText = "vi".equals(lang) ? "`" + rawProp + "` nằm trong tập hợp " + p1Name : "`" + rawProp + "` in " + p1Name;
                    sqlCond = propPath + " IN (" + p1Name + ")";
                    break;
                case "NotIn":
                    condText = "vi".equals(lang) ? "`" + rawProp + "` không nằm trong tập hợp " + p1Name : "`" + rawProp + "` not in " + p1Name;
                    sqlCond = propPath + " NOT IN (" + p1Name + ")";
                    break;
                case "StartingWith":
                    condText = "vi".equals(lang) ? "`" + rawProp + "` bắt đầu bằng " + p1Name : "`" + rawProp + "` starts with " + p1Name;
                    sqlCond = propPath + " LIKE CONCAT(" + p1Name + ", '%')";
                    break;
                case "EndingWith":
                    condText = "vi".equals(lang) ? "`" + rawProp + "` kết thúc bằng " + p1Name : "`" + rawProp + "` ends with " + p1Name;
                    sqlCond = propPath + " LIKE CONCAT('%', " + p1Name + ")";
                    break;
                case "Containing":
                    condText = "vi".equals(lang) ? "`" + rawProp + "` chứa " + p1Name : "`" + rawProp + "` contains " + p1Name;
                    sqlCond = propPath + " LIKE CONCAT('%', " + p1Name + ", '%')";
                    break;
                case "Like":
                    condText = "`" + rawProp + "` LIKE " + p1Name;
                    sqlCond = propPath + " LIKE " + p1Name;
                    break;
                case "NotLike":
                    condText = "`" + rawProp + "` NOT LIKE " + p1Name;
                    sqlCond = propPath + " NOT LIKE " + p1Name;
                    break;
                case "IgnoreCase":
                    condText = "vi".equals(lang) ? "LOWER(`" + rawProp + "`) = LOWER(" + p1Name + ") (không phân biệt hoa thường)" : "LOWER(`" + rawProp + "`) = LOWER(" + p1Name + ") (ignore case)";
                    sqlCond = "LOWER(" + propPath + ") = LOWER(" + p1Name + ")";
                    break;
                default:
                    condText = "`" + rawProp + "` = " + p1Name;
                    sqlCond = propPath + " = " + p1Name;
                    break;
            }

            criteriaDescriptions.add(condText);
            whereClauses.add(sqlCond);
        }

        List<String> orderDescriptions = new ArrayList<>();
        List<String> orderClauses = new ArrayList<>();
        for (QuerySort s : parsed.getSorts()) {
            String propPath = formatPropertyPath(s.getPropertyName(), alias);
            String dirUpper = s.getDirection() != null ? s.getDirection().toUpperCase() : "ASC";
            orderDescriptions.add("`" + s.getPropertyName() + "` " + dirUpper);
            orderClauses.add(propPath + " " + dirUpper);
        }

        // Build SQL
        StringBuilder sql = new StringBuilder();
        if (isDelete) {
            sql.append("DELETE FROM ").append(entityName).append(" ").append(alias).append("\n");
        } else {
            sql.append("SELECT ").append(selectClause).append("\n");
            sql.append("FROM ").append(entityName).append(" ").append(alias).append("\n");
        }

        if (!whereClauses.isEmpty()) {
            sql.append("WHERE ");
            for (int i = 0; i < whereClauses.size(); i++) {
                if (i > 0) {
                    String conj = (i - 1 < parsed.getConjunctions().size()) ? parsed.getConjunctions().get(i - 1) : "And";
                    sql.append(" ").append(conj.toUpperCase()).append(" ");
                }
                sql.append(whereClauses.get(i));
            }
            sql.append("\n");
        }

        if (!orderClauses.isEmpty()) {
            sql.append("ORDER BY ").append(String.join(", ", orderClauses)).append("\n");
        }

        if (parsed.getLimit() != null && !isCount && !isExists && !isDelete) {
            sql.append("LIMIT ").append(parsed.getLimit());
        }

        String sqlStr = sql.toString().trim();

        // Build Markdown
        StringBuilder md = new StringBuilder();
        md.append("Spring Data JPA Query\n\n");
        md.append("JPQL / SQL:\n\n```sql\n").append(sqlStr).append("\n```\n\n");
        md.append(lang.equals("vi") ? "Mục đích: " : "Action: ").append(actionDesc).append("\n\n");

        if (!criteriaDescriptions.isEmpty()) {
            md.append(lang.equals("vi") ? "Tiêu chí lọc:\n\n" : "Criteria:\n\n");
            for (int i = 0; i < criteriaDescriptions.size(); i++) {
                String desc = criteriaDescriptions.get(i);
                String prefix = "";
                if (i > 0) {
                    String conj = (i - 1 < parsed.getConjunctions().size()) ? parsed.getConjunctions().get(i - 1) : "And";
                    prefix = "**" + conj.toUpperCase() + "** ";
                }
                md.append("  \u2022 ").append(prefix).append(desc).append("\n");
            }
            md.append("\n");
        }

        if (!orderDescriptions.isEmpty()) {
            md.append(lang.equals("vi") ? "Sắp xếp:\n\n" : "Order by:\n\n");
            md.append("  \u2022 ").append(String.join(", ", orderDescriptions)).append("\n\n");
        }

        // Build HTML
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family: sans-serif; padding: 4px;'>");
        html.append("<h4 style='margin: 0 0 8px 0; color: #4a88c7;'>Spring Data JPA Query</h4>");
        html.append("<div style='margin-bottom: 4px;'><b>JPQL / SQL:</b></div>");
        html.append("<pre style='background: #2b2b2b; color: #a9b7c6; padding: 8px 10px; border-radius: 4px; font-family: monospace; font-size: 12px; line-height: 1.4; margin: 4px 0 10px 0;'><code>")
            .append(escapeHtml(sqlStr))
            .append("</code></pre>");
        html.append("<p style='margin: 4px 0;'><b>Action:</b> ").append(escapeHtml(actionDesc)).append("</p>");

        if (!criteriaDescriptions.isEmpty()) {
            html.append("<p style='margin: 6px 0 2px 0;'><b>Criteria:</b></p>");
            html.append("<ul style='margin: 2px 0; padding-left: 20px;'>");
            for (int i = 0; i < criteriaDescriptions.size(); i++) {
                String desc = criteriaDescriptions.get(i).replace("`", "");
                String prefix = "";
                if (i > 0) {
                    String conj = (i - 1 < parsed.getConjunctions().size()) ? parsed.getConjunctions().get(i - 1) : "And";
                    prefix = "<b>" + conj.toUpperCase() + "</b> ";
                }
                html.append("<li>").append(prefix).append(escapeHtml(desc)).append("</li>");
            }
            html.append("</ul>");
        }

        if (!orderDescriptions.isEmpty()) {
            html.append("<p style='margin: 6px 0 2px 0;'><b>Order by:</b></p>");
            html.append("<ul style='margin: 2px 0; padding-left: 20px;'>");
            for (String order : orderDescriptions) {
                html.append("<li>").append(escapeHtml(order.replace("`", ""))).append("</li>");
            }
            html.append("</ul>");
        }
        html.append("</div>");

        return new ExplanationResult(actionDesc, sqlStr, md.toString().trim(), html.toString());
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String formatPropertyPath(String rawProp, String alias) {
        if (rawProp.contains("_")) {
            String[] parts = rawProp.split("_");
            StringBuilder sb = new StringBuilder(alias);
            for (String part : parts) {
                sb.append(".").append(decapitalize(part));
            }
            return sb.toString();
        }
        return alias + "." + decapitalize(rawProp);
    }

    private static String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
