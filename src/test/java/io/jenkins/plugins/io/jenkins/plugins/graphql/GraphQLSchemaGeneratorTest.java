package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class GraphQLSchemaGeneratorTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

//    @Test
//    @WithoutJenkins
//    public void generateSchemaString() {
//        GraphQLSchemaGenerator graphQLSchemaGenerator = new GraphQLSchemaGenerator();
//        String schema = graphQLSchemaGenerator.generateSchemaString();
//        assertEquals("schema {\n" +
//            "    query: Query\n" +
//            "}\n" +
//            "type Query {\n" +
//            "    allJobs: [Job]!\n" +
//            "}\n" +
//            "type HealthReport { \n" +
//            "    _class: String!\n" +
//            "    description: String\n" +
//            "    iconClassName: String\n" +
//            "    iconUrl: String\n" +
//            "    score: Int\n" +
//            "}\n" +
//            "\n" +
//            "\n" +
//            "interface Job { \n" +
//            "    _class: String!\n" +
//            "    allBuilds: [Run]\n" +
//            "    buildable: Boolean\n" +
//            "    builds: [Run]\n" +
//            "    color: String\n" +
//            "    firstBuild: Run\n" +
//            "    healthReport: [HealthReport]\n" +
//            "    inQueue: Boolean\n" +
//            "    keepDependencies: Boolean\n" +
//            "    lastBuild: Run\n" +
//            "    lastCompletedBuild: Run\n" +
//            "    lastFailedBuild: Run\n" +
//            "    lastStableBuild: Run\n" +
//            "    lastSuccessfulBuild: Run\n" +
//            "    lastUnstableBuild: Run\n" +
//            "    lastUnsuccessfulBuild: Run\n" +
//            "    nextBuildNumber: Int\n" +
//            "    property: [JobProperty]\n" +
//            "    queueItem: Item\n" +
//            "    description: String\n" +
//            "    displayName: String\n" +
//            "    displayNameOrNull: String\n" +
//            "    fullDisplayName: String\n" +
//            "    fullName: String\n" +
//            "    name: String\n" +
//            "    url: String\n" +
//            "    actions: [Action]\n" +
//            "}\n" +
//            "\n" +
//            "\n" +
//            "type JobProperty { \n" +
//            "    _class: String!\n" +
//            "}\n" +
//            "\n" +
//            "\n" +
//            "type Item { \n" +
//            "    _class: String!\n" +
//            "    blocked: Boolean\n" +
//            "    buildable: Boolean\n" +
//            "    id: ID\n" +
//            "    inQueueSince: Int\n" +
//            "    params: String\n" +
//            "    stuck: Boolean\n" +
//            "    task: String\n" +
//            "    url: String\n" +
//            "    why: String\n" +
//            "    actions: [Action]\n" +
//            "}\n" +
//            "\n" +
//            "\n" +
//            "type Run { \n" +
//            "    _class: String!\n" +
//            "    artifacts: [String]\n" +
//            "    building: Boolean\n" +
//            "    description: String\n" +
//            "    displayName: String\n" +
//            "    duration: Int\n" +
//            "    estimatedDuration: Int\n" +
//            "    executor: String\n" +
//            "    fingerprint: [String]\n" +
//            "    fullDisplayName: String\n" +
//            "    id: ID\n" +
//            "    keepLog: Boolean\n" +
//            "    number: Int\n" +
//            "    queueId: Int\n" +
//            "    result: String\n" +
//            "    timestamp: String\n" +
//            "    url: String\n" +
//            "    actions: [Action]\n" +
//            "}\n" +
//            "\n" +
//            "\n" +
//            "type Action { \n" +
//            "    _class: String!\n" +
//            "}\n", schema);
//    }

//    @Test
//    public void generateSchema() throws Exception {
//        FreeStyleProject freestyle = j.createFreeStyleProject("freestyle");
//        Run r = freestyle.scheduleBuild2(0).waitForStart();
//        j.waitForCompletion(r);
//
//        GraphQLSchemaGenerator graphQLSchemaGenerator = new GraphQLSchemaGenerator();
//        GraphQL graphQL = graphQLSchemaGenerator.generateSchema();
//        ExecutionResult execute = graphQL.execute("query {\n" +
//            "  allJobs {\n" +
//            "    _class\n" +
//            "    color\n" +
//            "    buildable\n" +
//            "    nextBuildNumber\n" +
//            "    allBuilds {\n" +
//            "      _class\n" +
//            "      displayName\n" +
//            "      id\n" +
//            "    }\n" +
//            "  }\n" +
//            "}");
//        assertEquals(
//            JSONObject.fromObject("{\"data\": {\"allJobs\": [{\"_class\":\"hudson.model.FreeStyleProject\", \"color\":\"blue\", \"buildable\":true, \"nextBuildNumber\":2, \"allBuilds\":[{\"_class\":\"hudson.model.FreeStyleBuild\", \"displayName\":\"#1\", \"id\":\"1\"}]}]}}"),
//            JSONObject.fromObject(execute.toSpecification())
//        );
//    }
}
