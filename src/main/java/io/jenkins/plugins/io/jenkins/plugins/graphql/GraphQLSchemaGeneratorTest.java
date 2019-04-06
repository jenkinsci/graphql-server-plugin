package io.jenkins.plugins.io.jenkins.plugins.graphql;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class GraphQLSchemaGeneratorTest {

    @Test
    public void generateSchemaString() {
        GraphQLSchemaGenerator graphQLSchemaGenerator = new GraphQLSchemaGenerator();
        String schema = graphQLSchemaGenerator.generateSchemaString();
        assertEquals("schema {\n" +
            "    query: QueryType\n" +
            "}\n" +
            "type QueryType {\n" +
            "allJobs: [Job]\n" +
            "\n" +
            "}\n" +
            "type Job { \n" +
            "_class: String!\n" +
            "description:String\n" +
            "displayName:String\n" +
            "displayNameOrNull:String\n" +
            "fullDisplayName:String\n" +
            "fullName:String\n" +
            "name:String\n" +
            "url:String\n" +
            "buildable:Boolean\n" +
            "inQueue:Boolean\n" +
            "keepDependencies:Boolean\n" +
            "nextBuildNumber:Int\n" +
            "\n" +
            "}\n\ng", schema);
    }
}
