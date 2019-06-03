package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.schema.*;
import hudson.model.*;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class BuildersTest {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private Builders builder;
    private GraphQLSchema graphQLSchema;

    @Before
    public void setup() {
        builder = new Builders();
        graphQLSchema = builder.buildSchema();
    }

    @Test
    @WithoutJenkins
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

    @Test
    public void actions() throws IOException, InterruptedException, ExecutionException {
        FreeStyleProject freeStyleProject = Mockito.mock(FreeStyleProject.class);
        Mockito.when(freeStyleProject.getActions()).thenReturn(Arrays.asList(
            new CauseAction(new Cause() {
                @Override
                public String getShortDescription() {
                    return "My Cause";
                }
            })
        ));
        GraphQLInterfaceType graphqlRun = (GraphQLInterfaceType) graphQLSchema.getType("Job");
        ExecutionResult executeResult = _queryDataSet(graphQLSchema, freeStyleProject, graphqlRun, "actions { _class }");

        assertEquals(
            JSONObject.fromObject("{\"test\": {\"timestamp\": \"2019-05-31T23:16:17.604Z\"}}"),
            JSONObject.fromObject(executeResult.getData())
        );
    }

    private ExecutionResult _queryDataSet(GraphQLSchema existingSchema, Object data, GraphQLOutputType graphqlRun, String fields) {
        GraphQLSchema builtSchema;
        GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry(existingSchema.getCodeRegistry());
//        codeRegistry.typeResolver("Job", env -> {
//            String name = Builders.getRealClass(env.getObject()).getSimpleName();
//            return existingSchema.getObjectType(FreeStyleProject.class.getSimpleName());
//        });

        builtSchema = GraphQLSchema.newSchema(existingSchema)
            .query(
                GraphQLObjectType.newObject().name("QueryType").field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("test")
                    .type(graphqlRun)
                    .dataFetcher(environment -> data)
                    .build()
                ).build()
            )
            .codeRegistry(codeRegistry.build())
            .additionalType(graphqlRun)
            .build();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query("query { test { " + fields + " } }").build();
        return GraphQL.newGraphQL(builtSchema).build().execute(executionInput);
    }

}
