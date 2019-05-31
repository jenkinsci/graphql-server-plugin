package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.schema.GraphQLObjectType;
import hudson.model.FreeStyleProject;
import org.junit.Test;

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

}
