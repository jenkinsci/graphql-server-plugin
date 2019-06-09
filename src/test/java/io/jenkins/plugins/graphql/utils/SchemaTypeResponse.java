package io.jenkins.plugins.graphql.utils;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import io.jenkins.plugins.graphql.GraphQLSchemaGeneratorTest;

import java.util.ArrayList;
import java.util.HashMap;

public class SchemaTypeResponse {
    private final HashMap<String, Object> data = new HashMap<>();

    private SchemaTypeResponse() {
        this.data.putAll(new Gson().fromJson("{\n" +
            "    \"inputFields\": {},\n" +
            "    \"interfaces\": {},\n" +
            "    \"possibleTypes\": {},\n" +
            "    \"kind\": \"Object\",\n" +
            "    \"name\": \"\",\n" +
            "    \"description\": {},\n" +
            "    \"fields\": [\n" +
            "      {\n" +
            "        \"name\": \"_class\",\n" +
            "        \"description\": \"Class Name\",\n" +
            "        \"args\": [],\n" +
            "        \"type\": {\n" +
            "          \"kind\": \"SCALAR\",\n" +
            "          \"name\": \"String\",\n" +
            "          \"ofType\": {}\n" +
            "        },\n" +
            "        \"isDeprecated\": false,\n" +
            "        \"deprecationReason\": {}\n" +
            "      }\n" +
            "    ],\n" +
            "    \"enumValues\": {}\n" +
            "  }\n", new TypeToken<HashMap<String, Object>>() {}.getType()));
    }

    static public SchemaTypeResponse newSchemaTypeResponse() {
        return new SchemaTypeResponse();
    }

    public SchemaTypeResponse name(String name) {
        this.data.put("name", name);
        return this;
    }

    public SchemaTypeResponse description(String description) {
        this.data.put("description", description);
        return this;
    }

    public SchemaTypeResponse kind(String anInterface) {
        this.data.put("kind", anInterface.toUpperCase());
        return this;
    }

    public HashMap<String, Object> toHashMap() {
        return this.data;
    }

    public SchemaTypeResponse possibleTypes(String json) {
        ArrayList<Object> possibleTypes = new ArrayList<>();
        if (this.data.get("possibleTypes") instanceof ArrayList) {
            possibleTypes = (ArrayList<Object>) this.data.get("possibleTypes");
        }
        possibleTypes.add(new Gson().fromJson(json, HashMap.class));
        this.data.put("possibleTypes", possibleTypes);
        return this;
    }

    public SchemaTypeResponse interfaces(String json) {
        ArrayList<Object> interfaces = new ArrayList<>();
        if (this.data.get("interfaces") instanceof ArrayList) {
            interfaces = (ArrayList<Object>) this.data.get("interfaces");
        }
        interfaces.add(new Gson().fromJson(json, HashMap.class));
        this.data.put("interfaces", interfaces);
        return this;
    }

    public SchemaTypeResponse fields(String json) {
        ArrayList<Object> fields = new ArrayList<>();
        if (this.data.get("fields") instanceof ArrayList) {
            fields = (ArrayList<Object>) this.data.get("fields");
        }
        fields.add(new Gson().fromJson(json, LinkedTreeMap.class));
        this.data.put("fields", fields);
        return this;
    }
}
