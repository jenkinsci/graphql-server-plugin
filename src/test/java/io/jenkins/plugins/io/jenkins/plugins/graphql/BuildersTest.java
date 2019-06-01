package io.jenkins.plugins.io.jenkins.plugins.graphql;

import com.google.common.base.Joiner;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import hudson.model.FreeStyleBuild;
import hudson.model.Run;
import net.sf.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BuildersTest {
    @Test
    public void timestampAsRFC() throws IOException {
        Builders b = new Builders();
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(1559344577604L);

        Run run = Mockito.mock(FreeStyleBuild.class);
        Mockito.when(run.getTimestamp()).thenReturn(c);

        GraphQLObjectType graphqlRun = (GraphQLObjectType) b.buildSchema().getType("Run");

        ExecutionResult executeResult = _queryDataSet(run, graphqlRun, Arrays.asList("timestamp"));

        assertEquals(
            JSONObject.fromObject("{\"test\": {\"timestamp\": \"2019-05-31T16:16:17.604-07:00\"}}"),
            JSONObject.fromObject(executeResult.getData())
        );
    }

    private ExecutionResult _queryDataSet(Object data, GraphQLObjectType graphqlRun, List<String> fields) {
        GraphQLSchema builtSchema = GraphQLSchema.newSchema()
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

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query("query { test { " + Joiner.on(";").join(fields) + " } }").build();
        return GraphQL.newGraphQL(builtSchema).build().execute(executionInput);
    }

}
