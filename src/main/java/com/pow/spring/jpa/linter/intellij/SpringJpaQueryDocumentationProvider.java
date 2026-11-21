package com.pow.spring.jpa.linter.intellij;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.pow.spring.jpa.linter.explainer.ExplanationResult;
import com.pow.spring.jpa.linter.explainer.QueryExplainer;
import com.pow.spring.jpa.linter.parser.MethodLexer;
import com.pow.spring.jpa.linter.parser.ParsedQueryMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SpringJpaQueryDocumentationProvider extends AbstractDocumentationProvider {

    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        PsiMethod method = resolveMethod(element, originalElement);
        if (method == null) {
            return null;
        }

        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null || !containingClass.isInterface()) {
            return null;
        }

        String entityName = extractEntityName(containingClass);
        if (entityName == null) {
            entityName = extractEntityNameFallback(containingClass);
        }
        if (entityName == null) {
            return null;
        }

        String methodName = method.getName();
        if (!MethodLexer.isDerivedQueryMethod(methodName)) {
            return null;
        }

        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod(methodName, entityName);
        List<String> paramNames = new ArrayList<>();
        for (PsiParameter p : method.getParameterList().getParameters()) {
            paramNames.add(p.getName());
        }

        ExplanationResult res = QueryExplainer.explainQueryMethod(parsed, entityName, paramNames, "en");
        if (res != null && res.getHtmlContent() != null && !res.getHtmlContent().isEmpty()) {
            return res.getHtmlContent();
        }

        return null;
    }

    @Override
    public @Nullable String generateHoverDoc(PsiElement element, @Nullable PsiElement originalElement) {
        return generateDoc(element, originalElement);
    }

    @Override
    public @Nullable PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file, @Nullable PsiElement contextElement, int targetOffset) {
        if (contextElement != null) {
            PsiMethod method = PsiTreeUtil.getParentOfType(contextElement, PsiMethod.class, false);
            if (method != null) {
                PsiClass containingClass = method.getContainingClass();
                if (containingClass != null && containingClass.isInterface() && MethodLexer.isDerivedQueryMethod(method.getName())) {
                    return method;
                }
            }
        }
        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset);
    }

    @Override
    public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        PsiMethod method = resolveMethod(element, originalElement);
        if (method == null) {
            return null;
        }

        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null || !containingClass.isInterface()) {
            return null;
        }

        String entityName = extractEntityName(containingClass);
        if (entityName == null) {
            entityName = extractEntityNameFallback(containingClass);
        }
        if (entityName == null) {
            return null;
        }

        String methodName = method.getName();
        if (!MethodLexer.isDerivedQueryMethod(methodName)) {
            return null;
        }

        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod(methodName, entityName);
        List<String> paramNames = new ArrayList<>();
        for (PsiParameter p : method.getParameterList().getParameters()) {
            paramNames.add(p.getName());
        }

        ExplanationResult res = QueryExplainer.explainQueryMethod(parsed, entityName, paramNames, "en");
        return "Spring Data JPA Query\nAction: " + res.getSummary() + "\n\n" + res.getSqlPreview();
    }

    private PsiMethod resolveMethod(PsiElement element, PsiElement originalElement) {
        if (element instanceof PsiMethod) {
            return (PsiMethod) element;
        }
        if (originalElement != null) {
            PsiMethod m = PsiTreeUtil.getParentOfType(originalElement, PsiMethod.class, false);
            if (m != null) {
                return m;
            }
        }
        if (element != null) {
            PsiMethod m = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    private String extractEntityName(PsiClass repositoryInterface) {
        for (PsiClassType extendsType : repositoryInterface.getExtendsListTypes()) {
            PsiType[] parameters = extendsType.getParameters();
            if (parameters.length > 0 && parameters[0] instanceof PsiClassType classType) {
                PsiClass resolved = classType.resolve();
                if (resolved != null && resolved.getName() != null) {
                    return resolved.getName();
                }
                return classType.getClassName();
            }
        }
        return null;
    }

    private String extractEntityNameFallback(PsiClass repositoryInterface) {
        String name = repositoryInterface.getName();
        if (name != null && name.endsWith("Repository")) {
            return name.substring(0, name.length() - "Repository".length());
        }
        return null;
    }
}
