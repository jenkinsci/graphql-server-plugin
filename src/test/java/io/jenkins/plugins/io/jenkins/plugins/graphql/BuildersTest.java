package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.TransientActionFactory;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;

//@RunWith(PowerMockRunner.class)
//@PowerMockIgnore({"javax.crypto.*", "javax.security.*", "javax.net.ssl.*"})
//@PrepareForTest({FreeStyleProject.class, AbstractProject.class, Actionable.class})
public class BuildersTest {
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

        GraphQLObjectType graphqlRun = (GraphQLObjectType) graphQLSchema.getType("Run");
        ExecutionResult executeResult = _queryDataSet(graphQLSchema, run, graphqlRun, "timestamp");

        assertEquals(
            JSONObject.fromObject("{\"test\": {\"timestamp\": \"2019-05-31T23:16:17.604Z\"}}"),
            JSONObject.fromObject(executeResult.getData())
        );
    }

    @SuppressWarnings("rawtypes")
    @Extension
    public static class FakeCauseAction extends TransientActionFactory<Job> {
        @Override
        public Class<Job> type() {
            return Job.class;
        }

        @Override
        public Collection<? extends Action> createFor(Job j) {
            return Collections.singleton(new CauseAction(new Cause() {
                @Override
                public String getShortDescription() {
                    return "My Cause";
                }
            }));
        }
    }

    @Test
    public void actions() throws IOException {
        FreeStyleProject freeStyleProject = j.createFreeStyleProject();

        GraphQLInterfaceType graphqlRun = (GraphQLInterfaceType) graphQLSchema.getType("Job");
        ExecutionResult executeResult = _queryDataSet(graphQLSchema, freeStyleProject, graphqlRun, "_class\nactions { _class }");

        assertEquals(
            JSONObject.fromObject("{\"test\":{\"_class\":\"FreeStyleProject\",\"actions\":[{\"_class\":\"CauseAction\"},{\"_class\":\"RenameAction\"}]}}"),
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

}
