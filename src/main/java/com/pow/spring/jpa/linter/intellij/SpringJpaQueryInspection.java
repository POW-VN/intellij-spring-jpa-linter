package com.pow.spring.jpa.linter.intellij;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.pow.spring.jpa.linter.intellij.quickfix.AddQueryAnnotationQuickFix;
import com.pow.spring.jpa.linter.intellij.quickfix.RenameMethodQuickFix;
import com.pow.spring.jpa.linter.parser.MethodLexer;
import com.pow.spring.jpa.linter.resolver.EntityInfo;
import com.pow.spring.jpa.linter.resolver.EntityResolver;
import com.pow.spring.jpa.linter.validator.DiagnosticIssue;
import com.pow.spring.jpa.linter.validator.JavaMethodInfo;
import com.pow.spring.jpa.linter.validator.QueryValidator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpringJpaQueryInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethod(@NotNull PsiMethod method) {
                super.visitMethod(method);

                PsiClass containingClass = method.getContainingClass();
                if (containingClass == null || !containingClass.isInterface()) {
                    return;
                }

                PsiClass entityClass = extractEntityClass(containingClass);
                String entityName = null;
                if (entityClass != null) {
                    entityName = entityClass.getName();
                } else {
                    entityName = extractEntityNameFallback(containingClass);
                    if (entityName != null) {
                        entityClass = findClassInProject(method.getProject(), entityName);
                    }
                }

                if (entityName == null) {
                    return;
                }

                String methodName = method.getName();
                if (QueryValidator.isBuiltinMethod(methodName)) {
                    return;
                }

                EntityResolver resolver = new EntityResolver();
                if (entityClass != null) {
                    Set<String> visited = new HashSet<>();
                    registerEntityRecursively(method.getProject(), entityClass, resolver, visited);
                }

                QueryValidator validator = new QueryValidator(resolver);
                int effectiveParamCount = 0;
                boolean hasPageable = false;
                for (PsiParameter p : method.getParameterList().getParameters()) {
                    String typeText = p.getType().getPresentableText();
                    if (typeText.contains("Pageable")) {
                        hasPageable = true;
                    }
                    if (!typeText.contains("Pageable") && !typeText.contains("Sort") && !typeText.contains("Limit") && !typeText.contains("ScrollPosition") && !typeText.startsWith("Class")) {
                        effectiveParamCount++;
                    }
                }

                JavaMethodInfo methodInfo = new JavaMethodInfo(
                        methodName,
                        method.getReturnType() != null ? method.getReturnType().getPresentableText() : "void",
                        false,
                        effectiveParamCount,
                        hasPageable
                );

                List<DiagnosticIssue> issues = validator.validateMethod(methodInfo, entityName);

                for (DiagnosticIssue issue : issues) {
                    PsiElement nameIdentifier = method.getNameIdentifier();
                    if (nameIdentifier != null) {
                        List<LocalQuickFix> fixes = new ArrayList<>();
                        for (String suggestion : issue.getSuggestions()) {
                            if (suggestion != null && !suggestion.isEmpty()) {
                                fixes.add(new RenameMethodQuickFix(suggestion));
                            }
                        }
                        if (issue.getMessage().contains("missing 'By'")
                                || issue.getMessage().contains("Parameter count mismatch")
                                || issue.getMessage().contains("Unknown operator")
                                || issue.getMessage().contains("does not exist on entity")
                                || issue.getMessage().contains("Invalid")) {
                            fixes.add(new AddQueryAnnotationQuickFix());
                        }

                        holder.registerProblem(
                                nameIdentifier,
                                issue.getMessage(),
                                fixes.toArray(new LocalQuickFix[0])
                        );
                    }
                }
            }
        };
    }

    private PsiClass extractEntityClass(PsiClass repositoryInterface) {
        for (PsiClassType extendsType : repositoryInterface.getExtendsListTypes()) {
            PsiType[] parameters = extendsType.getParameters();
            if (parameters.length > 0 && parameters[0] instanceof PsiClassType classType) {
                PsiClass resolved = classType.resolve();
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return null;
    }

    private String extractEntityNameFallback(PsiClass repositoryInterface) {
        for (PsiClassType extendsType : repositoryInterface.getExtendsListTypes()) {
            String className = extendsType.getClassName();
            if ("JpaRepository".equals(className) || "CrudRepository".equals(className) || "Repository".equals(className)) {
                PsiType[] parameters = extendsType.getParameters();
                if (parameters.length > 0 && parameters[0] instanceof PsiClassType) {
                    return ((PsiClassType) parameters[0]).getClassName();
                }
            }
        }
        return null;
    }

    private void registerEntityRecursively(Project project, PsiClass psiClass, EntityResolver resolver, Set<String> visited) {
        if (psiClass == null) return;
        String entityName = psiClass.getName();
        if (entityName == null || visited.contains(entityName)) return;
        visited.add(entityName);

        EntityInfo info = new EntityInfo(entityName, psiClass.getQualifiedName() != null ? psiClass.getQualifiedName() : entityName);

        // 1. Register fields (from class and superclasses)
        for (PsiField field : psiClass.getAllFields()) {
            String fieldName = field.getName();
            PsiType fieldType = field.getType();
            info.addField(fieldName, fieldType.getPresentableText());

            if (field.hasAnnotation("jakarta.persistence.Transient")
                    || field.hasAnnotation("javax.persistence.Transient")
                    || field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                info.addTransientField(fieldName);
            }

            // Check if relation or complex object
            if (fieldType instanceof PsiClassType classType) {
                PsiType[] typeArgs = classType.getParameters();
                if (typeArgs.length > 0 && typeArgs[0] instanceof PsiClassType argClassType) {
                    PsiClass resolvedArg = argClassType.resolve();
                    if (isCandidateEntity(resolvedArg, project)) {
                        info.addRelation(fieldName, resolvedArg.getName());
                        registerEntityRecursively(project, resolvedArg, resolver, visited);
                    }
                } else {
                    PsiClass resolved = classType.resolve();
                    if (isCandidateEntity(resolved, project)) {
                        info.addRelation(fieldName, resolved.getName());
                        registerEntityRecursively(project, resolved, resolver, visited);
                    }
                }
            }
        }

        // 2. Also register properties from getter methods (covers Lombok/custom getters)
        for (PsiMethod m : psiClass.getAllMethods()) {
            String mName = m.getName();
            if (m.getParameterList().isEmpty() && m.getReturnType() != null) {
                String propName = null;
                if (mName.startsWith("get") && mName.length() > 3) {
                    propName = Character.toLowerCase(mName.charAt(3)) + mName.substring(4);
                } else if (mName.startsWith("is") && mName.length() > 2) {
                    propName = Character.toLowerCase(mName.charAt(2)) + mName.substring(3);
                }
                if (propName != null && !info.getFields().containsKey(propName)) {
                    info.addField(propName, m.getReturnType().getPresentableText());
                    if (m.hasAnnotation("jakarta.persistence.Transient")
                            || m.hasAnnotation("javax.persistence.Transient")) {
                        info.addTransientField(propName);
                    }
                }
            }
        }

        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null && !"java.lang.Object".equals(superClass.getQualifiedName())) {
            info.setSuperClassName(superClass.getName());
            registerEntityRecursively(project, superClass, resolver, visited);
        }

        resolver.registerEntity(info);
    }

    private boolean isCandidateEntity(PsiClass psiClass, Project project) {
        if (psiClass == null) return false;
        String qName = psiClass.getQualifiedName();
        if (qName == null || qName.startsWith("java.") || qName.startsWith("javax.swing.") || qName.startsWith("org.apache.")) {
            return false;
        }
        if (hasEntityAnnotation(psiClass)) {
            return true;
        }
        return psiClass.getContainingFile() != null &&
                GlobalSearchScope.projectScope(project).contains(psiClass.getContainingFile().getVirtualFile());
    }

    private boolean hasEntityAnnotation(PsiClass cls) {
        if (cls == null) return false;
        if (cls.hasAnnotation("jakarta.persistence.Entity")
                || cls.hasAnnotation("javax.persistence.Entity")
                || cls.hasAnnotation("jakarta.persistence.Table")
                || cls.hasAnnotation("javax.persistence.Table")
                || cls.hasAnnotation("jakarta.persistence.Embeddable")
                || cls.hasAnnotation("javax.persistence.Embeddable")
                || cls.hasAnnotation("jakarta.persistence.MappedSuperclass")
                || cls.hasAnnotation("javax.persistence.MappedSuperclass")
                || cls.hasAnnotation("org.springframework.data.mongodb.core.mapping.Document")) {
            return true;
        }
        PsiModifierList modifierList = cls.getModifierList();
        if (modifierList != null) {
            for (PsiAnnotation ann : modifierList.getAnnotations()) {
                String qualifiedName = ann.getQualifiedName();
                if (qualifiedName != null && (qualifiedName.endsWith(".Entity") || qualifiedName.endsWith(".Table") || qualifiedName.endsWith(".Document") || qualifiedName.endsWith(".MappedSuperclass"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private PsiClass findClassInProject(Project project, String className) {
        // Priority 1: Search project scope first (only project files)
        PsiClass[] projectClasses = PsiShortNamesCache.getInstance(project).getClassesByName(className, GlobalSearchScope.projectScope(project));
        if (projectClasses != null && projectClasses.length > 0) {
            for (PsiClass cls : projectClasses) {
                if (hasEntityAnnotation(cls)) {
                    return cls;
                }
            }
            return projectClasses[0];
        }

        // Priority 2: Search allScope with @Entity check
        PsiClass[] allClasses = PsiShortNamesCache.getInstance(project).getClassesByName(className, GlobalSearchScope.allScope(project));
        if (allClasses != null && allClasses.length > 0) {
            for (PsiClass cls : allClasses) {
                if (hasEntityAnnotation(cls)) {
                    return cls;
                }
            }
            return allClasses[0];
        }
        return null;
    }
}
