package io.jenkins.plugins.graphql.utils;

import java.util.HashMap;
import java.util.Map;

public class SchemaTypeBuilder {
    private final HashMap<String, Object> data = new HashMap<>();

    private SchemaTypeBuilder() {
        this.data.put("kind", "SCALAR");
        this.data.put("name", "Int");
        this.data.put("ofType", null);

    }

    public HashMap<String, Object> toHashMap() {
        return this.data;
    }

    public static SchemaTypeBuilder newTypeBuilder() {
        return new SchemaTypeBuilder();
    }

    public SchemaTypeBuilder kind(String kind) {
        this.data.put("kind", kind);
        return this;
    }

    public SchemaTypeBuilder name(String name) {
        this.data.put("name", name);
        return this;
    }

    public SchemaTypeBuilder ofType(String ofType) {
        this.data.put("ofType", ofType);
        return this;
    }

    public SchemaTypeBuilder ofType(Map ofType) {
        this.data.put("ofType", ofType);
        return this;
    }
}
