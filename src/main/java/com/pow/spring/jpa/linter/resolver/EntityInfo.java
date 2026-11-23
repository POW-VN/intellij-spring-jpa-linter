package com.pow.spring.jpa.linter.resolver;

import java.util.*;

public class EntityInfo {
    private final String name;
    private final String filePath;
    private final Map<String, String> fields; // fieldName -> fieldType
    private final Map<String, String> relations; // fieldName -> targetEntityName
    private final Set<String> transientFields; // fieldName marked with @Transient
    private String superClassName;

    public EntityInfo(String name, String filePath) {
        this.name = name;
        this.filePath = filePath;
        this.fields = new HashMap<>();
        this.relations = new HashMap<>();
        this.transientFields = new HashSet<>();
    }

    public String getName() { return name; }
    public String getFilePath() { return filePath; }
    public Map<String, String> getFields() { return fields; }
    public Map<String, String> getRelations() { return relations; }
    public Set<String> getTransientFields() { return transientFields; }

    public String getSuperClassName() { return superClassName; }
    public void setSuperClassName(String superClassName) { this.superClassName = superClassName; }

    public void addField(String name, String type) {
        fields.put(name, type);
    }

    public void addRelation(String name, String targetEntity) {
        relations.put(name, targetEntity);
    }

    public void addTransientField(String name) {
        transientFields.add(name);
    }

    public boolean isTransient(String name) {
        return transientFields.contains(name);
    }
}
