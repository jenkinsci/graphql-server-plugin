package io.jenkins.plugins.io.jenkins.plugins.graphql;

import com.fasterxml.jackson.annotation.JsonProperty;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Actionable;
import hudson.model.Job;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.Stapler;
import javax.annotation.CheckForNull;
import javax.management.Query;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@Extension
public class RootAction extends Actionable implements hudson.model.RootAction {
    private final static Logger LOGGER = Logger.getLogger(RootAction.class.getName());

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "graphql";
    }

    public class JobFilter {
        private String nameContains;

        @JsonProperty("name_contains")
        public String getNameContains() {
            return nameContains;
        }

        public void setNameContains(String nameContains) {
            this.nameContains = nameContains;
        }
    }
    public static class Query {
        @GraphQLQuery(name = "allJobs")
        public List<String> allJobs(/*
                JobFilter filter,
                @GraphQLArgument(name="skip", defaultValue="0") Number skip,
                @GraphQLArgument(name="limit", defaultValue="0") Number limit
            */) {
            LOGGER.info("allJobs");
            return Collections.emptyList();
        }
    }
    private static GraphQLSchema buildSchema() {
        Query query = new Query();

        return new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(query, Query.class)
                .generate();
    }

    @SuppressWarnings("unused")
    // @RequirePOST
    public void doIndex() throws IOException, ServletException {
        StaplerRequest req = Stapler.getCurrentRequest();
        StaplerResponse res = Stapler.getCurrentResponse();

        /* START - One time generation */
        String schema = "type Query{MagicSchool: String}";
        ExtensionList<Job> jobsTypes = ExtensionList.lookup(hudson.model.Job.class);

        if (jobsTypes.size() > -1) {
            throw new Error(new Integer(jobsTypes.size()).toString());
        }

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().type("Query", builder -> builder.dataFetcher("MagicSchool", new StaticDataFetcher("Hogwards"))).build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        GraphQL build = GraphQL.newGraphQL(graphQLSchema).build();
        /* END - One time generation */

        // Get the POST stream
        String body = IOUtils.toString(req.getInputStream(), "UTF-8");
        if (body.isEmpty()) {
            throw new Error("No body");
        }

        LOGGER.info("Body: " + body);
        JSONObject jsonRequest = JSONObject.fromObject(body);;

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(jsonRequest.getString("query")).build();
        ExecutionResult executionResult = build.execute(executionInput);

        ServletOutputStream outputStream = res.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(outputStream, "UTF-8");
        osw.write(JSONObject.fromObject(executionResult.toSpecification()).toString(2));
        osw.flush();
        osw.close();
    }

    @Override
    public String getSearchUrl() {
        return null;
    }
}
