package com.pow.spring.jpa.linter.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class RenameMethodQuickFix implements LocalQuickFix {

    private final String newMethodName;

    public RenameMethodQuickFix(String newMethodName) {
        this.newMethodName = newMethodName;
    }

    @Override
    public @NotNull String getName() {
        return "Rename method to '" + newMethodName + "'";
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

        if (method != null) {
            method.setName(newMethodName);
        }
    }

    public String getNewMethodName() {
        return newMethodName;
    }
}
