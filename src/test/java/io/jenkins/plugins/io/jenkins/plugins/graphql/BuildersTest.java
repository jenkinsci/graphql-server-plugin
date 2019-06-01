package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeRuntimeWiring;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import org.junit.Test;
import org.mockito.Mockito;
import sun.util.resources.LocaleData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import static org.junit.Assert.*;

public class BuildersTest {

    @Test
    public void buildSchemaFromClass() {
//        Builders b = new Builders();
//        GraphQLObjectType graphQLObjectType = b.buildSchemaFromClass(FreeStyleProject.class);
//        assertEquals(
//            "GraphQLObjectType{name='FreeStyleProject', description='moo', fieldDefinitionsByName=[_class, actions, allBuilds, buildable, builds, color, concurrentBuild, description, displayName, displayNameOrNull, downstreamProjects, firstBuild, fullDisplayName, fullName, healthReport, inQueue, keepDependencies, labelExpression, lastBuild, lastCompletedBuild, lastFailedBuild, lastStableBuild, lastSuccessfulBuild, lastUnstableBuild, lastUnsuccessfulBuild, name, nextBuildNumber, property, queueItem, scm, upstreamProjects, url], interfaces=[]}",
//            graphQLObjectType.toString()
//        );
    }

    @Test
    public void buildSchema() {
        Builders b = new Builders();
        assertEquals(
            "GraphQLObjectType{name='FreeStyleProject', description='moo', fieldDefinitionsByName=[_class, actions, allBuilds, buildable, builds, color, concurrentBuild, description, displayName, displayNameOrNull, downstreamProjects, firstBuild, fullDisplayName, fullName, healthReport, inQueue, keepDependencies, labelExpression, lastBuild, lastCompletedBuild, lastFailedBuild, lastStableBuild, lastSuccessfulBuild, lastUnstableBuild, lastUnsuccessfulBuild, name, nextBuildNumber, property, queueItem, scm, upstreamProjects, url], interfaces=[]}",
            b.buildSchema().toString()
        );
    }

    @Test
    public void produceExtendedScalars() throws IOException {
        Builders b = new Builders();
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(1559344577604L);

        Run run = Mockito.mock(FreeStyleBuild.class);
        Mockito.when(run.getTimestamp()).thenReturn(c);

        GraphQLObjectType graphqlRun = (GraphQLObjectType) b.buildSchema().getType("Run");

        assertEquals(
            "GraphQLObjectType{name='Run', description='moo', fieldDefinitionsByName=[_class, actions, artifacts, building, description, displayName, duration, estimatedDuration, executor, fingerprint, fullDisplayName, id, keepLog, number, queueId, result, timestamp, url], interfaces=[]}",
            graphqlRun.toString()
        );

        GraphQLSchema builtSchema = GraphQLSchema.newSchema()
            .query(
                GraphQLObjectType.newObject().name("QueryType").field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("test")
                    .type(graphqlRun)
                    .dataFetcher(environment -> run)
                    .build()
                ).build()
            )
            .additionalType(graphqlRun)
            .build();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query("query { test { timestamp } }").build();
        ExecutionResult executeResult = GraphQL.newGraphQL(builtSchema).build().execute(executionInput);

        assertEquals(
            "{test={timestamp=2019-05-31T16:16:17.604-07:00}}",
            executeResult.getData()
        );
    }

}
