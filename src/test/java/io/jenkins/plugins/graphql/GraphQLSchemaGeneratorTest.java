package io.jenkins.plugins.graphql;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.User;
import hudson.security.csrf.CrumbIssuer;
import io.jenkins.plugins.graphql.utils.JsonMapFlattener;
import io.jenkins.plugins.graphql.utils.SchemaFieldBuilder;
import io.jenkins.plugins.graphql.utils.SchemaTypeBuilder;
import io.jenkins.plugins.graphql.utils.SchemaTypeResponse;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.test.JSONAssert;
import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.export.ExportedBean;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

        ClassUtils.setAllClassesCache(MockClassUtils.mock_getAllClassesList());

        Builders builder = new Builders();
        builder.addExtraTopLevelClasses(Collections.singletonList(FreeStyleProject.class));
        graphQLSchema = builder.buildSchema();
        GraphQLRootAction.setBuiltSchema(graphQLSchema);
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
            new Gson().fromJson("{\"test\": {\"timestamp\": \"2019-05-31T23:16:17.604Z\"}}", Map.class),
            executeResult.getData()
        );
    }

    @Test
    public void actions() throws IOException {
        FreeStyleProject freeStyleProject = j.createFreeStyleProject();

        GraphQLObjectType graphqlRun = (GraphQLObjectType) graphQLSchema.getType("hudson_model_FreeStyleProject");
        ExecutionResult executeResult = _queryDataSet(graphQLSchema, freeStyleProject, graphqlRun, "_class\nactions { _class }");

        assertEquals(
            new Gson().fromJson("{test={_class=hudson.model.FreeStyleProject, actions=[{_class=jenkins.model.RenameAction}, {_class=org.jenkinsci.plugins.displayurlapi.actions.JobDisplayAction}, {_class=com.cloudbees.plugins.credentials.ViewCredentialsAction}]}}", Map.class),
            executeResult.getData()
        );
    }

    @Test
    public void getJobByName() throws IOException {
        j.createFreeStyleProject("one");
        j.createFreeStyleProject("two");

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            .query("query { allItems(id: \"one\") { _class\nname } }")
            .build();
        ExecutionResult executeResult = GraphQL.newGraphQL(graphQLSchema).build().execute(executionInput);
        if (executeResult.getErrors().size() != 0) {
            throw new Error(executeResult.getErrors().get(0).getMessage());
        }

        assertEquals(
            new Gson().fromJson("{\"allItems\":[{\"_class\":\"hudson.model.FreeStyleProject\", \"name\": \"one\"}]}", Map.class),
            executeResult.getData()
        );
    }

    @Test
    public void getJobByNameNonExistent() throws IOException {
        j.createFreeStyleProject("one");
        j.createFreeStyleProject("two");

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            .query("query { allItems(id: \"nonexistant\") { _class\nname } }")
            .build();
        ExecutionResult executeResult = GraphQL.newGraphQL(graphQLSchema).build().execute(executionInput);
        if (executeResult.getErrors().size() != 0) {
            throw new Error(executeResult.getErrors().get(0).getMessage());
        }

        assertEquals(
            new Gson().fromJson("{\"allItems\":[ ]}", Map.class),
            executeResult.getData()
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
            new Gson().fromJson("{\"allUsers\":[{\"_class\":\"hudson.model.User\",\"id\":\"alice\"}]}", Map.class),
            executeResult.getData()
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
            new Gson().fromJson("{\"allUsers\":[]}", Map.class),
            executeResult.getData()
        );
    }

    public JSONObject postQuery(String username, String password, String query) throws Exception {
        CrumbIssuer crumbIssuer = j.jenkins.getCrumbIssuer();
        assertNotNull(crumbIssuer);
        String CrumbField = crumbIssuer.getCrumbRequestField();
        String CrumbValue = crumbIssuer.getCrumb();

        JSONObject body = new JSONObject();
        body.put("query", query);

        JenkinsRule.WebClient wc = j.createWebClient().withBasicCredentials(username, password);
        WebRequest req = new WebRequest(new URL(j.jenkins.getRootUrl() + "graphql/"), HttpMethod.POST);
        req.setAdditionalHeader(CrumbField, CrumbValue);
        req.setAdditionalHeader("Content-Type","application/json");
        req.setRequestBody(body.toString());

        String rawResult = wc.getPage(req).getWebResponse().getContentAsString();
        assertNotNull(rawResult);

        JSONObject result = JSONObject.fromObject(rawResult);
        assertNotNull(result);

        return result.getJSONObject("data");
    }

    @Test
    public void whoamiNoAuth() throws Exception {
        JSONObject data = postQuery(
            null,
            null,
            "query { whoAmI { authorities\ndetails\nanonymous\nname\n } }"
        );
        JSONObject whoamiData = data.getJSONObject("whoAmI");

        assertArrayEquals(
            new String[] { "anonymous" },
            Lists.newArrayList(whoamiData.getJSONArray("authorities").iterator()).toArray()
        );
        assertEquals(
            "null",
            whoamiData.optString("details", "null")
        );
        assertEquals(
            true,
            whoamiData.getBoolean("anonymous")
        );
        assertEquals(
            "anonymous",
            whoamiData.getString("name")
        );
    }

    @Test
    public void whoamiAuth() throws Exception {
        JenkinsRule.DummySecurityRealm dummySecurityRealm = j.createDummySecurityRealm();
        j.jenkins.setSecurityRealm(dummySecurityRealm);

        JSONObject data = postQuery(
            "alice",
            "alice",
            "query { whoAmI { authorities\ndetails\nanonymous\nname\n } }"
        );

        JSONObject whoamiData = data.getJSONObject("whoAmI");

        assertArrayEquals(
            new String[] { "authenticated" },
            Lists.newArrayList(whoamiData.getJSONArray("authorities").iterator()).toArray()
        );
        assertEquals(
            "org.acegisecurity.ui.WebAuthenticationDetails@957e: RemoteIpAddress: 127.0.0.1; SessionId: null",
            whoamiData.optString("details", null)
        );
        assertEquals(
            false,
            whoamiData.getBoolean("anonymous")
        );
        assertEquals(
            "alice",
            whoamiData.getString("name")
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

        HashMap<String, ?> actionType = getSchemaType(executionResult, "hudson_model_Action");
        assertNotNull(actionType);
        HashMap<String, ?> __actionType = getSchemaType(executionResult, "hudson_model_Action__");
        assertNotNull(__actionType);
        HashMap<String, ?> causeActionType = getSchemaType(executionResult, "hudson_model_CauseAction");
        assertNotNull(causeActionType);
        HashMap<String, ?> causeUserIdActionType = getSchemaType(executionResult, "hudson_model_Cause_UserIdCause");
        assertNotNull(causeUserIdActionType);


        assertSchemaType(
            SchemaTypeResponse.newSchemaTypeResponse()
                .name("hudson_model_Action")
                .kind("INTERFACE")
                .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_Action__\", \"ofType\": null}")
                .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_CauseAction\", \"ofType\": null}")
                .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_MyViewsProperty\", \"ofType\": null}")
                .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_ParametersAction\", \"ofType\": null}")
                .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_model_ParametersDefinitionProperty\", \"ofType\": null}")
                .possibleTypes("{\"kind\":\"OBJECT\", \"name\":\"hudson_security_WhoAmI\", \"ofType\": null}")
                .toHashMap(),
            actionType
        );

        assertSchemaType(
            SchemaTypeResponse.newSchemaTypeResponse()
                .name("hudson_model_FreeStyleProject")
                .kind("OBJECT")
                .description("Freestyle project")
                .interfaces("INTERFACE", "hudson_model_AbstractProject", null)
                .interfaces("INTERFACE", "hudson_model_Job", null)
                .interfaces("INTERFACE", "hudson_model_AbstractItem", null)
                .interfaces("INTERFACE", "hudson_model_Project", null)
                .toHashMap(),
            getSchemaType(executionResult, "hudson_model_FreeStyleProject")
        );

        assertSchemaType(
            SchemaTypeResponse.newSchemaTypeResponse()
                .name("hudson_model_Action__")
                .description("Generic implementation of Action with just _class defined")
                .kind("OBJECT")
                .interfaces("INTERFACE", "hudson_model_Action", null)
                .toHashMap(),
            __actionType
        );

//        fields:
//        Expected :[{name=_class, description=Class Name, args=[], type={kind=SCALAR, name=String, ofType=null}, isDeprecated=false, deprecationReason=null}, {args=[{defaultValue=0, name=type, description=null, type={inputFields=null, interfaces=null, possibleTypes=null, kind=Object, name=String, description=null, fields=[{name=_class, description=Class Name, args=[], type={kind=SCALAR, name=String, ofType=null}, isDeprecated=false, deprecationReason=null}], enumValues=null}}, {defaultValue=0, name=offset, description=null, type={inputFields=null, interfaces=null, possibleTypes=null, kind=Object, name=Int, description=null, fields=[{name=_class, description=Class Name, args=[], type={kind=SCALAR, name=String, ofType=null}, isDeprecated=false, deprecationReason=null}], enumValues=null}}, {defaultValue=0, name=limit, description=null, type={inputFields=null, interfaces=null, possibleTypes=null, kind=Object, name=Int, description=null, fields=[{name=_class, description=Class Name, args=[], type={kind=SCALAR, name=String, ofType=null}, isDeprecated=false, deprecationReason=null}], enumValues=null}}, {defaultValue=0, name=id, description=null, type={inputFields=null, interfaces=null, possibleTypes=null, kind=Object, name=Int, description=null, fields=[{name=_class, description=Class Name, args=[], type={kind=SCALAR, name=String, ofType=null}, isDeprecated=false, deprecationReason=null}], enumValues=null}}], deprecationReason=null, isDeprecated=false, name=causes, description=null, type={kind=LIST, name=null, ofType={inputFields=null, interfaces=null, possibleTypes=null, kind=INTERFACE, name=hudson_model_Cause, description=null, fields=[{name=_class, description=Class Name, args=[], type={kind=SCALAR, name=String, ofType=null}, isDeprecated=false, deprecationReason=null}], enumValues=null}}}]
//        Actual   :[{name=_class, description=Class Name, args=[], type={kind=SCALAR, name=String, ofType=null}, isDeprecated=false, deprecationReason=null}, {name=causes, description=null, args=[{name=offset, description=null, type={kind=SCALAR, name=Int, ofType=null}, defaultValue=0}, {name=limit, description=null, type={kind=SCALAR, name=Int, ofType=null}, defaultValue=100}, {name=type, description=null, type={kind=SCALAR, name=String, ofType=null}, defaultValue=null}, {name=id, description=null, type={kind=SCALAR, name=ID, ofType=null}, defaultValue=null}], type={kind=LIST, name=null, ofType={kind=INTERFACE, name=hudson_model_Cause, ofType=null}}, isDeprecated=false, deprecationReason=null}]
        assertSchemaType(
            SchemaTypeResponse.newSchemaTypeResponse()
                .name("hudson_model_CauseAction")
                .kind("OBJECT")
                .interfaces("INTERFACE", "hudson_model_Action", null)
                .fields(
                    SchemaFieldBuilder.newFieldBuilder()
                        .name("causes")
                        .args("offset",null,"0",SchemaTypeResponse.newSchemaTypeResponse().name("Int").kind("SCALAR").toHashMap())
                        .args("limit",null,"100",SchemaTypeResponse.newSchemaTypeResponse().name("Int").kind("SCALAR").toHashMap())
                        .args("type",null,null,SchemaTypeResponse.newSchemaTypeResponse().name("String").kind("SCALAR").toHashMap())
                        .args("id",null,null,SchemaTypeResponse.newSchemaTypeResponse().name("ID").kind("SCALAR").toHashMap())
                        .type("LIST",null, SchemaTypeBuilder.newTypeBuilder().kind("INTERFACE").name("hudson_model_Cause").toHashMap())
                        .toHashMap()
                )
                .toHashMap(),
            causeActionType
        );

        assertSchemaType(
            SchemaTypeResponse.newSchemaTypeResponse()
                .name("hudson_model_Cause_UserIdCause")
                .kind("OBJECT")
                .interfaces("INTERFACE", "hudson_model_Cause", null)
                .fields("{\"name\":\"shortDescription\", \"description\":null, \"args\":[], \"type\":{\"kind\":\"SCALAR\", \"name\":\"String\", \"ofType\": null}, \"isDeprecated\": false, \"deprecationReason\":null}")
                .fields("{\"name\":\"userId\", \"description\":null, \"args\":[], \"type\":{\"kind\":\"SCALAR\", \"name\":\"String\", \"ofType\": null}, \"isDeprecated\": false, \"deprecationReason\":null}")
                .fields("{\"name\":\"userName\", \"description\":null, \"args\":[], \"type\":{\"kind\":\"SCALAR\", \"name\":\"String\", \"ofType\": null}, \"isDeprecated\": false, \"deprecationReason\":null}")
                .toHashMap(),
            causeUserIdActionType
        );
    }

    private void assertSchemaType(Map expected, Map actual) {
        Map expectedFlattened = JsonMapFlattener.flatten(expected);
        Map actualFlattened = JsonMapFlattener.flatten(actual);


        for (Object key : expectedFlattened.keySet()) {
            try {
                assertEquals(key.toString() + " is equal", expectedFlattened.get(key), actualFlattened.get(key));
            } catch (ComparisonFailure e) {
                e.printStackTrace();
                assertEquals(expectedFlattened, actualFlattened);
            }
        }
    }

    private HashMap<String, ?> getSchemaType(ExecutionResult executionResult, String typeName) {
        Map data = executionResult.getData();
        Map schema = (Map) data.get("__schema");
        Set types = new HashSet<>((List) schema.get("types"));
        for (Object type : types) {
            HashMap typeMap = (HashMap) type;
            if (typeMap.get("name").equals(typeName)) {
                return typeMap;
            }
        }
        return null;
    }
}
