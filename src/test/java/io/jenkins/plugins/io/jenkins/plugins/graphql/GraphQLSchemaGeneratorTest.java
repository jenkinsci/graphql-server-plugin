package io.jenkins.plugins.io.jenkins.plugins.graphql;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class GraphQLSchemaGeneratorTest {
    @Rule

    public JenkinsRule j = new JenkinsRule();
    private Builders builder;
    private GraphQLSchema graphQLSchema;

    @Before
    public void setup() {
        builder = new Builders();
        builder.addExtraTopLevelClasses(Arrays.asList(FreeStyleProject.class));
        graphQLSchema = builder.buildSchema();
    }

    @Test
    public void timestampAsRFC() throws IOException {
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(1559344577604L);

        Run run = Mockito.mock(FreeStyleBuild.class);
        Mockito.when(run.getTimestamp()).thenReturn(c);

        GraphQLObjectType graphqlRun = (GraphQLObjectType) graphQLSchema.getType("hudson_model_Run");
        ExecutionResult executeResult = _queryDataSet(graphQLSchema, run, graphqlRun, "timestamp");

        assertEquals(
            JSONObject.fromObject("{\"test\": {\"timestamp\": \"2019-05-31T23:16:17.604Z\"}}"),
            JSONObject.fromObject(executeResult.getData())
        );
    }

//    @SuppressWarnings("rawtypes")
//    @Extension
//    public static class FakeCauseAction extends TransientActionFactory<Job> {
//        @Override
//        public Class<Job> type() {
//            return Job.class;
//        }
//
//        @Override
//        public Collection<? extends Action> createFor(Job j) {
//            return Collections.singleton(new CauseAction(new Cause() {
//                @Override
//                public String getShortDescription() {
//                    return "My Cause";
//                }
//            }));
//        }
//    }

    @Test
    public void actions() throws IOException {
        FreeStyleProject freeStyleProject = j.createFreeStyleProject();

        GraphQLInterfaceType graphqlRun = (GraphQLInterfaceType) graphQLSchema.getType("hudson_model_Job");
        ExecutionResult executeResult = _queryDataSet(graphQLSchema, freeStyleProject, graphqlRun, "_class\nactions { _class }");

        assertEquals(
            JSONObject.fromObject("{\"test\":{\"_class\":\"hudson_model_FreeStyleProject\",\"actions\":[{\"_class\":\"jenkins_model_RenameAction\"},{\"_class\":\"com_cloudbees_plugins_credentials_ViewCredentialsAction\"}]}}"),
            JSONObject.fromObject(executeResult.getData())
        );
    }

    private ExecutionResult _queryDataSet(GraphQLSchema existingSchema, Object data, GraphQLOutputType graphqlRun, String fields) {
        GraphQLSchema builtSchema;

        builtSchema = GraphQLSchema.newSchema(existingSchema)
            .query(
                GraphQLObjectType.newObject().name("QueryType").field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("test")
                    .type(graphqlRun)
                    .dataFetcher(environment -> data)
                    .build()
                ).build()
            )
            .additionalType(graphqlRun)
            .build();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query("query { test { " + fields + " } }").build();
        return GraphQL.newGraphQL(builtSchema).build().execute(executionInput);
    }

    public String getIntrospectionQuery() throws IOException {
        return Resources.toString(
            Resources.getResource("introspectiveQuery.graphql"),
            Charsets.UTF_8
        );
    }

    private static class SchemaTypeResponse {
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

        static SchemaTypeResponse newSchemaTypeResponse() {
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

    @Test
    @WithoutJenkins
    public void generateSchemaString() throws IOException {
        Builders builder = new Builders();
        GraphQLSchema graphQLSchema = builder.buildSchema();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(getIntrospectionQuery()).build();
        ExecutionResult executionResult = GraphQL.newGraphQL(graphQLSchema).build().execute(executionInput);

        assertArrayEquals(
            new HashMap[] {
                SchemaTypeResponse.newSchemaTypeResponse()
                    .name("hudson_model_Action")
                    .kind("INTERFACE")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"__hudson_model_Action\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"com_cloudbees_plugins_credentials_CredentialsSelectHelper_WrappedCredentialsStore\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"com_cloudbees_plugins_credentials_CredentialsStoreAction\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"com_cloudbees_plugins_credentials_SystemCredentialsProvider_UserFacingAction\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"com_cloudbees_plugins_credentials_UserCredentialsProvider_UserFacingAction\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"com_cloudbees_plugins_credentials_ViewCredentialsAction\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"com_cloudbees_plugins_credentials_ViewCredentialsAction_RootActionImpl\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_CauseAction\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_MyViewsProperty\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_ParametersAction\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_ParametersDefinitionProperty\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_plugins_git_GitTagAction\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_plugins_git_util_BuildData\", \"ofType\":{}}")
                    .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"io_jenkins_plugins_io_jenkins_plugins_graphql_GraphQLRootAction\", \"ofType\":{}}")
                    .toHashMap()
            },
            new Gson().fromJson(
                new Gson().toJson(getSchemaType(executionResult, "hudson_model_Action")),
                HashMap[].class
            )
        );

        assertArrayEquals(
            new HashMap[] {
                SchemaTypeResponse.newSchemaTypeResponse()
                    .name("__hudson_model_Action")
                    .description("Generic implementation of Action with just _class defined")
                    .kind("OBJECT")
                    .interfaces("{\n" +
                        "        \"kind\": \"INTERFACE\",\n" +
                        "        \"name\": \"hudson_model_Action\",\n" +
                        "        \"ofType\": {}\n" +
                        "      }\n")
                    .toHashMap()
            },
            new Gson().fromJson(
                new Gson().toJson(getSchemaType(executionResult, "__hudson_model_Action")),
                HashMap[].class
            )
        );

        assertArrayEquals(
            new HashMap[] {
                SchemaTypeResponse.newSchemaTypeResponse()
                    .name("hudson_model_CauseAction")
                    .kind("OBJECT")
                    .interfaces("{\n" +
                        "        \"kind\": \"INTERFACE\",\n" +
                        "        \"name\": \"hudson_model_Action\",\n" +
                        "        \"ofType\": {}\n" +
                        "      }\n")
                    .fields("{\"name\":\"causes\",\"description\":{},\"args\":[],\"type\":{\"kind\":\"LIST\",\"name\":{},\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"hudson_model_Cause\",\"ofType\":{}}},\"isDeprecated\":false,\"deprecationReason\":{}}")
                    .toHashMap()
            },
            new Gson().fromJson(
                new Gson().toJson(getSchemaType(executionResult, "hudson_model_CauseAction")),
                HashMap[].class
            )
        );
    }

    private Object[] getSchemaType(ExecutionResult executionResult, String typeName) {
        return JSONObject.fromObject(executionResult.getData()).getJSONObject("__schema").getJSONArray("types").stream().filter(
            type -> ((JSONObject) type).getString("name").equals(typeName)
        ).toArray();
    }
}
