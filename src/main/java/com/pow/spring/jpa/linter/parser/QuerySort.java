package com.pow.spring.jpa.linter.parser;

public class QuerySort {
    private final String raw;
    private final String propertyName;
    private final String direction;
    private final String directionError;

    public QuerySort(String raw, String propertyName, String direction, String directionError) {
        this.raw = raw;
        this.propertyName = propertyName;
        this.direction = direction;
        this.directionError = directionError;
    }

    public String getRaw() { return raw; }
    public String getPropertyName() { return propertyName; }
    public String getDirection() { return direction; }
    public String getDirectionError() { return directionError; }
}
