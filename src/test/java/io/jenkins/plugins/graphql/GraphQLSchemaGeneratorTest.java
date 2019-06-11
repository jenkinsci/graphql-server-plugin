package io.jenkins.plugins.graphql;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.User;
import io.jenkins.plugins.graphql.utils.SchemaTypeResponse;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.export.ExportedBean;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraphQLSchemaGeneratorTest {
    @Rule

    public JenkinsRule j = new JenkinsRule();
    private GraphQLSchema graphQLSchema;

    @ExportedBean
    public class FakeRun extends Run {
        protected FakeRun(@Nonnull Job job, @Nonnull Calendar timestamp) {
            super(job, timestamp);
        }
    }

    @Before
    public void setup() {

        ClassUtils._getAllClassesCache = MockClassUtils.mock_getAllClassesList();

        Builders builder = new Builders();
        builder.addExtraTopLevelClasses(Arrays.asList(FreeStyleProject.class));
        graphQLSchema = builder.buildSchema();
    }

    @Test
    public void timestampAsRFC() {
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(1559344577604L);

        FakeRun run = Mockito.mock(FakeRun.class);
        Mockito.when(run.getTimestamp()).thenReturn(c);

        GraphQLObjectType graphqlRun = (GraphQLObjectType) graphQLSchema.getType("io_jenkins_plugins_graphql_GraphQLSchemaGeneratorTest_FakeRun");
        ExecutionResult executeResult = _queryDataSet(graphQLSchema, run, graphqlRun, "timestamp");

        assertEquals(
            JSONObject.fromObject("{\"test\": {\"timestamp\": \"2019-05-31T23:16:17.604Z\"}}"),
            JSONObject.fromObject(executeResult.getData())
        );
    }

    @Test
    public void actions() throws IOException {
        FreeStyleProject freeStyleProject = j.createFreeStyleProject();

        GraphQLInterfaceType graphqlRun = (GraphQLInterfaceType) graphQLSchema.getType("hudson_model_Job");
        ExecutionResult executeResult = _queryDataSet(graphQLSchema, freeStyleProject, graphqlRun, "_class\nactions { _class }");

        assertEquals(
            JSONObject.fromObject("{\"test\":{\"_class\":\"hudson.model.FreeStyleProject\",\"actions\":[{\"_class\":\"jenkins.model.RenameAction\"},{\"_class\":\"com.cloudbees.plugins.credentials.ViewCredentialsAction\"}]}}"),
            JSONObject.fromObject(executeResult.getData())
        );
    }

    @Test
    public void getJobByName() throws IOException {
        j.createFreeStyleProject("one");
        j.createFreeStyleProject("two");

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            .query("query { allJobs(id: \"one\") { _class\nname } }")
            .build();
        ExecutionResult executeResult = GraphQL.newGraphQL(graphQLSchema).build().execute(executionInput);
        if (executeResult.getErrors().size() != 0) {
            throw new Error(executeResult.getErrors().get(0).getMessage());
        }

        assertEquals(
            JSONObject.fromObject("{\"allJobs\":[{\"_class\":\"hudson.model.FreeStyleProject\", \"name\": \"one\"}]}"),
            JSONObject.fromObject(executeResult.getData())
        );
    }

    @Test
    public void getJobByNameNonExistant() throws IOException {
        j.createFreeStyleProject("one");
        j.createFreeStyleProject("two");

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            .query("query { allJobs(id: \"nonexistant\") { _class\nname } }")
            .build();
        ExecutionResult executeResult = GraphQL.newGraphQL(graphQLSchema).build().execute(executionInput);
        if (executeResult.getErrors().size() != 0) {
            throw new Error(executeResult.getErrors().get(0).getMessage());
        }

        assertEquals(
            JSONObject.fromObject("{\"allJobs\":[ ]}"),
            JSONObject.fromObject(executeResult.getData())
        );
    }

    @Test
    public void getUserByName() throws IOException {
        User.get("alice", true, Collections.emptyMap());

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            .query("query { allUsers(id: \"alice\") { _class\nid } }")
            .build();
        ExecutionResult executeResult = GraphQL.newGraphQL(graphQLSchema).build().execute(executionInput);
        if (executeResult.getErrors().size() != 0) {
            throw new Error(executeResult.getErrors().get(0).getMessage());
        }

        assertEquals(
            JSONObject.fromObject("{\"allUsers\":[{\"_class\":\"hudson.model.User\",\"id\":\"alice\"}]}"),
            JSONObject.fromObject(executeResult.getData())
        );
    }

    @Test
    public void getUserByNameNonExistant() throws IOException {
        User.get("alice", true, Collections.emptyMap());

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            .query("query { allUsers(id: \"nonexistant\") { _class\nid } }")
            .build();
        ExecutionResult executeResult = GraphQL.newGraphQL(graphQLSchema).build().execute(executionInput);
        if (executeResult.getErrors().size() != 0) {
            throw new Error(executeResult.getErrors().get(0).getMessage());
        }

        assertEquals(
            JSONObject.fromObject("{\"allUsers\":[]}"),
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

    @Test
    public void generateSchemaString() throws IOException {
        Builders builder = new Builders();
        GraphQLSchema graphQLSchema = builder.buildSchema();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(getIntrospectionQuery()).build();
        ExecutionResult executionResult = GraphQL.newGraphQL(graphQLSchema).build().execute(executionInput);
        System.out.println(graphQLSchema.getTypeMap().keySet());

        assertEquals(
            SchemaTypeResponse.newSchemaTypeResponse()
                .name("hudson_model_Action")
                .kind("INTERFACE")
                .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"__hudson_model_Action\", \"ofType\":{}}")
                .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_CauseAction\", \"ofType\":{}}")
                .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_MyViewsProperty\", \"ofType\":{}}")
                .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_ParametersAction\", \"ofType\":{}}")
                .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_ParametersDefinitionProperty\", \"ofType\":{}}")
                .toHashMap(),
            new Gson().fromJson(
                new Gson().toJson(getSchemaType(executionResult, "hudson_model_Action")),
                HashMap.class
            )
        );

        assertEquals(
            SchemaTypeResponse.newSchemaTypeResponse()
                .name("__hudson_model_Action")
                .description("Generic implementation of Action with just _class defined")
                .kind("OBJECT")
                .interfaces("{\n" +
                    "        \"kind\": \"INTERFACE\",\n" +
                    "        \"name\": \"hudson_model_Action\",\n" +
                    "        \"ofType\": {}\n" +
                    "      }\n")
                .toHashMap(),
            new Gson().fromJson(
                new Gson().toJson(getSchemaType(executionResult, "__hudson_model_Action")),
                HashMap.class
            )
        );

        assertEquals(
            SchemaTypeResponse.newSchemaTypeResponse()
                .name("hudson_model_CauseAction")
                .kind("OBJECT")
                .interfaces("{\n" +
                    "        \"kind\": \"INTERFACE\",\n" +
                    "        \"name\": \"hudson_model_Action\",\n" +
                    "        \"ofType\": {}\n" +
                    "      }\n")
                .fields("{\"name\":\"causes\",\"description\":{},\"args\":["+
                        "{\"name\":\"id\", \"description\":{}, \"type\":{\"kind\":\"SCALAR\", \"name\":\"ID\", \"ofType\":{}}, \"defaultValue\":{}},"+
                        "{\"name\":\"limit\", \"description\":{}, type={\"kind\":\"SCALAR\", \"name\":\"Int\", \"ofType\":{}}, \"defaultValue\":\"100\"},"+
                        "{\"name\":\"offset\", \"description\":{}, \"type\":{\"kind\":SCALAR, \"name\":\"Int\", \"ofType\":{}}, \"defaultValue\":\"0\"},"+
                        "{\"name\":\"type\", \"description\":{}, \"type\":{\"kind\":\"SCALAR\", \"name\":\"String\", \"ofType\":{}}, \"defaultValue\":{}}"+
                    "],\"type\":{\"kind\":\"LIST\",\"name\":{},\"ofType\":{\"kind\":\"INTERFACE\",\"name\":\"hudson_model_Cause\",\"ofType\":{}}},\"isDeprecated\":false,\"deprecationReason\":{}}")
                .toHashMap(),
            new Gson().fromJson(
                new Gson().toJson(getSchemaType(executionResult, "hudson_model_CauseAction")),
                HashMap.class
            )
        );

        assertEquals(
            SchemaTypeResponse.newSchemaTypeResponse()
                .name("hudson_model_Cause_UserIdCause")
                .kind("OBJECT")
                .interfaces("{\n" +
                    "        \"kind\": \"INTERFACE\",\n" +
                    "        \"name\": \"hudson_model_Cause\",\n" +
                    "        \"ofType\": {}\n" +
                    "      }\n")
                .fields("{\"name\":\"shortDescription\", \"description\":{}, \"args\":[], \"type\":{\"kind\":\"SCALAR\", \"name\":\"String\", \"ofType\":{}}, \"isDeprecated\": false, \"deprecationReason\":{}}")
                .fields("{\"name\":\"userId\", \"description\":{}, \"args\":[], \"type\":{\"kind\":\"SCALAR\", \"name\":\"String\", \"ofType\":{}}, \"isDeprecated\": false, \"deprecationReason\":{}}")
                .fields("{\"name\":\"userName\", \"description\":{}, \"args\":[], \"type\":{\"kind\":\"SCALAR\", \"name\":\"String\", \"ofType\":{}}, \"isDeprecated\": false, \"deprecationReason\":{}}")
                .toHashMap(),
            new Gson().fromJson(
                new Gson().toJson(getSchemaType(executionResult, "hudson_model_Cause_UserIdCause")),
                HashMap.class
            )
        );
        assertTrue(true);
    }

    private Object getSchemaType(ExecutionResult executionResult, String typeName) {
        return JSONObject.fromObject(executionResult.getData()).getJSONObject("__schema").getJSONArray("types").stream().filter(
            type -> ((JSONObject) type).getString("name").equals(typeName)
        ).toArray()[0];
    }
}
