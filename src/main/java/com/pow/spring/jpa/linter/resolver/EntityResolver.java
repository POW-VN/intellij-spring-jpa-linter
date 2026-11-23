package com.pow.spring.jpa.linter.resolver;

import java.util.*;

public class EntityResolver {

    private final Map<String, EntityInfo> entityCache = new HashMap<>();

    public void registerEntity(EntityInfo entity) {
        entityCache.put(entity.getName(), entity);
        resolveInheritanceAndSmartRelations();
    }

    public EntityInfo getEntity(String entityName) {
        return entityCache.get(entityName);
    }

    public void resolveInheritanceAndSmartRelations() {
        // 1. Resolve inheritance: merge fields and relations from superclass
        for (EntityInfo entity : entityCache.values()) {
            mergeSuperClassFields(entity, new HashSet<>());
        }

        // 2. Smart auto-detect relations:
        // If field type (or generic argument) matches an existing entity in cache,
        // automatically mark it as relation
        for (EntityInfo entity : entityCache.values()) {
            for (Map.Entry<String, String> entry : new HashMap<>(entity.getFields()).entrySet()) {
                String field = entry.getKey();
                String type = entry.getValue();
                if (!entity.getRelations().containsKey(field)) {
                    String typeCandidate = type;
                    if (type.contains("<") && type.contains(">")) {
                        int start = type.lastIndexOf('<');
                        int end = type.indexOf('>', start);
                        if (start != -1 && end != -1) {
                            typeCandidate = type.substring(start + 1, end).trim();
                        }
                    }
                    if (entityCache.containsKey(typeCandidate)) {
                        entity.addRelation(field, typeCandidate);
                    }
                }
            }
        }
    }

    private void mergeSuperClassFields(EntityInfo entity, Set<String> visited) {
        if (entity.getSuperClassName() == null || visited.contains(entity.getName())) {
            return;
        }
        visited.add(entity.getName());

        EntityInfo superEntity = entityCache.get(entity.getSuperClassName());
        if (superEntity != null) {
            mergeSuperClassFields(superEntity, visited);
            for (Map.Entry<String, String> entry : superEntity.getFields().entrySet()) {
                if (!entity.getFields().containsKey(entry.getKey())) {
                    entity.addField(entry.getKey(), entry.getValue());
                }
            }
            for (Map.Entry<String, String> entry : superEntity.getRelations().entrySet()) {
                if (!entity.getRelations().containsKey(entry.getKey())) {
                    entity.addRelation(entry.getKey(), entry.getValue());
                }
            }
            for (String tField : superEntity.getTransientFields()) {
                entity.addTransientField(tField);
            }
        }
    }

    public static class PropertyResolution {
        public final boolean valid;
        public final String resolvedProperty;
        public final String finalType;
        public final String error;
        public final String suggestion;
        public final List<String> pathSegments;
        public final boolean isTransient;

        public PropertyResolution(boolean valid, String resolvedProperty, String finalType, String error, String suggestion, List<String> pathSegments, boolean isTransient) {
            this.valid = valid;
            this.resolvedProperty = resolvedProperty;
            this.finalType = finalType;
            this.error = error;
            this.suggestion = suggestion;
            this.pathSegments = pathSegments;
            this.isTransient = isTransient;
        }

        public static PropertyResolution success(String prop, String type, List<String> pathSegments) {
            return new PropertyResolution(true, prop, type, null, null, pathSegments, false);
        }

        public static PropertyResolution success(String prop, String type) {
            return new PropertyResolution(true, prop, type, null, null, Collections.singletonList(prop), false);
        }

        public static PropertyResolution failure(String error, String suggestion) {
            return new PropertyResolution(false, null, null, error, suggestion, null, false);
        }

        public static PropertyResolution transientError(String field, String entityName) {
            return new PropertyResolution(false, null, null, "Cannot query on @Transient property '" + field + "' on entity '" + entityName + "'.", null, null, true);
        }
    }

    public PropertyResolution resolvePropertyPath(String entityName, String rawProperty) {
        EntityInfo entity = entityCache.get(entityName);
        if (entity == null) {
            return PropertyResolution.success(rawProperty, "Object");
        }

        // 1. Direct property check
        String matched = findMatchingField(entity, rawProperty);
        if (matched != null) {
            if (entity.isTransient(matched)) {
                return PropertyResolution.transientError(matched, entityName);
            }
            return PropertyResolution.success(matched, entity.getFields().get(matched), Collections.singletonList(matched));
        }

        // 2. Support underscore traversal: Room_Building_Manager_Name
        if (rawProperty.contains("_")) {
            String[] segments = rawProperty.split("_");
            EntityInfo currentEntity = entity;
            String currentType = "Object";
            List<String> pathSegments = new ArrayList<>();

            for (int i = 0; i < segments.length; i++) {
                String seg = segments[i];
                String matchedField = findMatchingField(currentEntity, seg);

                if (matchedField == null) {
                    String closest = findClosestField(currentEntity, seg);
                    String suggestionMsg = closest != null ? " Did you mean: '" + closest + "'?" : "";
                    return PropertyResolution.failure(
                            "Property '" + seg + "' does not exist on entity '" + currentEntity.getName() + "'." + suggestionMsg,
                            closest
                    );
                }

                if (currentEntity.isTransient(matchedField)) {
                    return PropertyResolution.transientError(matchedField, currentEntity.getName());
                }

                pathSegments.add(matchedField);
                currentType = currentEntity.getFields().get(matchedField);

                // If not last segment, traverse to related entity
                if (i < segments.length - 1) {
                    String targetEntity = currentEntity.getRelations().get(matchedField);
                    if (targetEntity == null) {
                        targetEntity = currentType; // fallback to type name
                    }
                    EntityInfo nextEntity = entityCache.get(targetEntity);
                    if (nextEntity != null) {
                        currentEntity = nextEntity;
                    }
                }
            }
            return PropertyResolution.success(rawProperty, currentType, pathSegments);
        }

        // 3. Try recursive camelCase segmentation traversal
        PropertyResolution camelResolution = tryCamelCaseTraversal(entity, rawProperty);
        if (camelResolution != null && camelResolution.valid) {
            return camelResolution;
        }

        // Typo suggestion
        String closest = findClosestField(entity, rawProperty);
        String suggestionMsg = closest != null ? " Did you mean: '" + closest + "'?" : "";
        return PropertyResolution.failure(
                "Property '" + rawProperty + "' does not exist on entity '" + entityName + "'." + suggestionMsg,
                closest
        );
    }

    private PropertyResolution tryCamelCaseTraversal(EntityInfo entity, String prop) {
        List<String> sortedFields = new ArrayList<>(entity.getFields().keySet());
        sortedFields.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String field : sortedFields) {
            if (entity.isTransient(field)) continue;
            if (prop.length() > field.length() && prop.toLowerCase().startsWith(field.toLowerCase())) {
                char nextChar = prop.charAt(field.length());
                if (Character.isUpperCase(nextChar) || nextChar == '_') {
                    String targetEntityName = entity.getRelations().get(field);
                    if (targetEntityName == null) {
                        targetEntityName = entity.getFields().get(field);
                    }
                    EntityInfo nextEntity = entityCache.get(targetEntityName);
                    if (nextEntity != null) {
                        String remainder = prop.substring(field.length());
                        if (remainder.startsWith("_")) remainder = remainder.substring(1);
                        PropertyResolution sub = resolvePropertyPath(nextEntity.getName(), remainder);
                        if (sub.valid) {
                            List<String> combined = new ArrayList<>();
                            combined.add(field);
                            if (sub.pathSegments != null) {
                                combined.addAll(sub.pathSegments);
                            } else {
                                combined.add(remainder);
                            }
                            return PropertyResolution.success(prop, sub.finalType, combined);
                        }
                    }
                }
            }
        }
        return null;
    }

    private String findMatchingField(EntityInfo entity, String name) {
        for (String field : entity.getFields().keySet()) {
            if (field.equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    private String findClosestField(EntityInfo entity, String name) {
        String bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String field : entity.getFields().keySet()) {
            if (entity.isTransient(field)) continue;
            int d = computeLevenshtein(name.toLowerCase(), field.toLowerCase());
            if (d < bestDistance && d <= 3) {
                bestDistance = d;
                bestMatch = field;
            }
        }
        return bestMatch;
    }

    public static int computeLevenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
}
