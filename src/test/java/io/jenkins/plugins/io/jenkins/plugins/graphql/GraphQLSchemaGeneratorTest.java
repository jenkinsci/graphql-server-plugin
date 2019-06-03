package io.jenkins.plugins.io.jenkins.plugins.graphql;

import com.google.gson.Gson;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import com.google.common.io.Resources;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import com.google.common.base.Charsets;
import net.sf.json.test.JSONAssert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class GraphQLSchemaGeneratorTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    public String getIntrospectionQuery() throws IOException {
        return Resources.toString(
            Resources.getResource("introspectiveQuery.graphql"),
            Charsets.UTF_8
        );
    }

    @Test
    @WithoutJenkins
    public void generateSchemaString() throws IOException {
        Builders builder = new Builders();
        GraphQLSchema graphQLSchema = builder.buildSchema();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(getIntrospectionQuery()).build();
        ExecutionResult executionResult = GraphQL.newGraphQL(graphQLSchema).build().execute(executionInput);
        assertArrayEquals(
            new Gson().fromJson("[\n" +
                "  {\n" +
                "    \"kind\": \"INTERFACE\",\n" +
                "    \"name\": \"Action\",\n" +
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
                "    \"inputFields\": {},\n" +
                "    \"interfaces\": {},\n" +
                "    \"enumValues\": {},\n" +
                "    \"possibleTypes\": [{\"kind\"=\"OBJECT\", name=\"__Action\", \"ofType\"={}}]\n" +
                "  }\n" +
                "]", HashMap[].class),
            new Gson().fromJson(
                new Gson().toJson(getSchemaType(executionResult, "Action")),
                HashMap[].class
            )
        );

        assertArrayEquals(
            new Gson().fromJson("[\n" +
                "  {\n" +
                "    \"kind\": \"OBJECT\",\n" +
                "    \"name\": \"__Action\",\n" +
                "    \"description\": \"Generic implementation of Action with just _class defined\",\n" +
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
                "    \"inputFields\": {},\n" +
                "    \"interfaces\": [{\"kind\"=\"INTERFACE\", name=\"Action\", \"ofType\"={}}],\n" +
                "    \"enumValues\": {},\n" +
                "    \"possibleTypes\": {}\n" +
                "  }\n" +
                "]", HashMap[].class),
            new Gson().fromJson(
                new Gson().toJson(getSchemaType(executionResult, "__Action")),
                HashMap[].class
            )
        );
    }

    private Object[] getSchemaType(ExecutionResult executionResult, String typeName) {
        return JSONObject.fromObject(executionResult.getData()).getJSONObject("__schema").getJSONArray("types").stream().filter(
            type -> ((JSONObject)type).getString("name").equals(typeName)
        ).toArray();
    }
}
