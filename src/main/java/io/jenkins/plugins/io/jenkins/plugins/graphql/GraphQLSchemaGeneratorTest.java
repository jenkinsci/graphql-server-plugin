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
            "    allJobs: [Job]\n" +
            "}\n" +
            "type HealthReport { \n" +
            "    _class: String!\n" +
            "    description: String\n" +
            "    iconClassName: String\n" +
            "    iconUrl: String\n" +
            "    score: String\n" +
            "}\n" +
            "\n" +
            "\n" +
            "type Job { \n" +
            "    _class: String!\n" +
            "    actions: [Action]\n" +
            "    description: String\n" +
            "    displayName: String\n" +
            "    displayNameOrNull: String\n" +
            "    fullDisplayName: String\n" +
            "    fullName: String\n" +
            "    name: String\n" +
            "    url: String\n" +
            "    allBuilds: [Run]\n" +
            "    buildable: String\n" +
            "    builds: [Run]\n" +
            "    color: String\n" +
            "    firstBuild: Run\n" +
            "    healthReport: [HealthReport]\n" +
            "    inQueue: String\n" +
            "    keepDependencies: String\n" +
            "    lastBuild: Run\n" +
            "    lastCompletedBuild: Run\n" +
            "    lastFailedBuild: Run\n" +
            "    lastStableBuild: Run\n" +
            "    lastSuccessfulBuild: Run\n" +
            "    lastUnstableBuild: Run\n" +
            "    lastUnsuccessfulBuild: Run\n" +
            "    nextBuildNumber: String\n" +
            "    property: [JobProperty]\n" +
            "    queueItem: Item\n" +
            "}\n" +
            "\n" +
            "\n" +
            "type JobProperty { \n" +
            "    _class: String!\n" +
            "}\n" +
            "\n" +
            "\n" +
            "type Item { \n" +
            "    _class: String!\n" +
            "    actions: [Action]\n" +
            "    blocked: String\n" +
            "    buildable: String\n" +
            "    id: String\n" +
            "    inQueueSince: String\n" +
            "    params: String\n" +
            "    stuck: String\n" +
            "    task: String\n" +
            "    url: String\n" +
            "    why: String\n" +
            "}\n" +
            "\n" +
            "\n" +
            "type Run { \n" +
            "    _class: String!\n" +
            "    actions: [Action]\n" +
            "    artifacts: [String]\n" +
            "    building: String\n" +
            "    description: String\n" +
            "    displayName: String\n" +
            "    duration: String\n" +
            "    estimatedDuration: String\n" +
            "    executor: String\n" +
            "    fingerprint: [String]\n" +
            "    fullDisplayName: String\n" +
            "    id: String\n" +
            "    keepLog: String\n" +
            "    number: String\n" +
            "    queueId: String\n" +
            "    result: String\n" +
            "    timestamp: String\n" +
            "    url: String\n" +
            "}\n" +
            "\n" +
            "\n" +
            "type Action { \n" +
            "    _class: String!\n" +
            "}\n", schema);
    }
}
