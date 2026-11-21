package com.pow.spring.jpa.linter.validator;

public class JavaMethodInfo {
    private final String name;
    private final String returnType;
    private final boolean hasQueryAnnotation;
    private final int parameterCount;
    private final boolean hasPageable;

    public JavaMethodInfo(String name, String returnType, boolean hasQueryAnnotation, int parameterCount, boolean hasPageable) {
        this.name = name;
        this.returnType = returnType;
        this.hasQueryAnnotation = hasQueryAnnotation;
        this.parameterCount = parameterCount;
        this.hasPageable = hasPageable;
    }

    public JavaMethodInfo(String name, String returnType, boolean hasQueryAnnotation, int parameterCount) {
        this(name, returnType, hasQueryAnnotation, parameterCount, false);
    }

    public String getName() { return name; }
    public String getReturnType() { return returnType; }
    public boolean isHasQueryAnnotation() { return hasQueryAnnotation; }
    public int getParameterCount() { return parameterCount; }
    public boolean isHasPageable() { return hasPageable; }
}
