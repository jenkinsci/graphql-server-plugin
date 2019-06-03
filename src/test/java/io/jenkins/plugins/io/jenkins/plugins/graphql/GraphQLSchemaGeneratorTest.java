package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import com.google.common.io.Resources;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import com.google.common.base.Charsets;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import java.io.IOException;

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
        assertEquals(
            JSONArray.fromObject("[\n" +
                "  {\n" +
                "    \"kind\": \"INTERFACE\",\n" +
                "    \"name\": \"Action\",\n" +
                "    \"description\": null,\n" +
                "    \"fields\": [\n" +
                "      {\n" +
                "        \"name\": \"_class\",\n" +
                "        \"description\": \"Class Name\",\n" +
                "        \"args\": [],\n" +
                "        \"type\": {\n" +
                "          \"kind\": \"SCALAR\",\n" +
                "          \"name\": \"String\",\n" +
                "          \"ofType\": null\n" +
                "        },\n" +
                "        \"isDeprecated\": false,\n" +
                "        \"deprecationReason\": null\n" +
                "      }\n" +
                "    ],\n" +
                "    \"inputFields\": null,\n" +
                "    \"interfaces\": null,\n" +
                "    \"enumValues\": null,\n" +
                "    \"possibleTypes\": []\n" +
                "  }\n" +
                "]"),
            JSONArray.fromObject(
                getSchemaType(executionResult, "Action")
            )
        );
    }

    private Object[] getSchemaType(ExecutionResult executionResult, String typeName) {
        return JSONObject.fromObject(executionResult.getData()).getJSONObject("__schema").getJSONArray("types").stream().filter(
            type -> ((JSONObject)type).getString("name").equals(typeName)
        ).toArray();
    }
}
