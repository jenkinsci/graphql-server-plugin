package io.jenkins.plugins.graphql.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SchemaFieldBuilder {
    private final HashMap<String, Object> data = new HashMap<>();

    private SchemaFieldBuilder() {
        this.data.put("name", null);
        this.data.put("description", null);
        this.data.put("args", new HashSet<>());
        this.data.put("type", new HashMap<>());
        this.data.put("isDeprecated", false);
        this.data.put("deprecationReason", null);

    }

    public HashMap<String, Object> toHashMap() {
        return this.data;
    }

    public static SchemaFieldBuilder newFieldBuilder() {
        return new SchemaFieldBuilder();
    }

    public SchemaFieldBuilder name(String name) {
        this.data.put("name", name);
        return this;
    }

    public SchemaFieldBuilder description(String description) {
        this.data.put("description", description);
        return this;
    }

    public SchemaFieldBuilder type(String kind, String name, Object ofType) {
        HashMap<String, Object> newType = new HashMap<>();
        newType.put("kind", kind);
        newType.put("name", name);
        newType.put("ofType", ofType);

        this.data.put("type", newType);
        return this;
    }

    public SchemaFieldBuilder args(String name, String description, String defaultValue, Map type) {
        HashMap<String, Object> newArg = new HashMap<>();
        newArg.put("name", name);
        newArg.put("description", description);
        newArg.put("type", type);
        newArg.put("defaultValue", defaultValue);

        ((Set) this.data.get("args")).add(newArg);
        return this;
    }
}
