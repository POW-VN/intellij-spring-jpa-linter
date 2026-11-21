package com.pow.spring.jpa.linter.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class AddQueryAnnotationQuickFix implements LocalQuickFix {

    @Override
    public @NotNull String getName() {
        return "Add '@Query(\"\")' annotation";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "Spring Data JPA query method fixes";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();
        PsiMethod method = null;
        if (element instanceof PsiMethod) {
            method = (PsiMethod) element;
        } else if (element != null) {
            method = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
        }

        if (method == null) {
            return;
        }

        PsiModifierList modifierList = method.getModifierList();
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiAnnotation annotation = factory.createAnnotationFromText("@org.springframework.data.jpa.repository.Query(\"\")", method);
        if (modifierList != null) {
            modifierList.addBefore(annotation, modifierList.getFirstChild());
        }
    }
}
