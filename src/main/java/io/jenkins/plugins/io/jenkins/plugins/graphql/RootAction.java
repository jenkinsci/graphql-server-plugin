package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Actionable;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Logger;

@Extension
@SuppressWarnings("unused")
public class RootAction extends Actionable implements hudson.model.RootAction {
    private final static Logger LOGGER = Logger.getLogger(RootAction.class.getName());
    private static GraphQL builtSchema;
    private static GraphQLSchemaGenerator graphQLSchemaGenerator;

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

    @Initializer(after = InitMilestone.JOB_LOADED)
    public static void init() {
        graphQLSchemaGenerator = new GraphQLSchemaGenerator();
        builtSchema = graphQLSchemaGenerator.generateSchema();
    }

    @SuppressWarnings("unused")
    // @RequirePOST
    public void doIndex() throws IOException {
        StaplerRequest req = Stapler.getCurrentRequest();
        StaplerResponse res = Stapler.getCurrentResponse();

        // Get the POST stream
        String body = IOUtils.toString(req.getInputStream(), "UTF-8");
        if (body.isEmpty()) {
            throw new Error("No body");
        }

        LOGGER.info("Body: " + body);
        JSONObject jsonRequest = JSONObject.fromObject(body);;

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(jsonRequest.getString("query")).build();
        ExecutionResult executionResult = builtSchema.execute(executionInput);

        ServletOutputStream outputStream = res.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(outputStream, "UTF-8");
        osw.write(JSONObject.fromObject(executionResult.toSpecification()).toString(2));
        osw.flush();
        osw.close();
    }

    @SuppressWarnings("unused")
    public String getSchemaString() {
        return graphQLSchemaGenerator.getSchema();
    }

    @SuppressWarnings("unused")
    public void doSchema(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        Jenkins.getInstanceOrNull().checkPermission(Jenkins.ADMINISTER);
        rsp.setStatus(HttpServletResponse.SC_OK);
        rsp.setContentType("text/html");
        req.getView(this, "schema.jelly").forward(req, rsp);
    }

    @SuppressWarnings("unused")
    public void doClient(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        Jenkins.getInstanceOrNull().checkPermission(Jenkins.ADMINISTER);
        rsp.setStatus(HttpServletResponse.SC_OK);
        rsp.setContentType("text/html");
        req.getView(this, "client.jelly").forward(req, rsp);
    }

    @Override
    public String getSearchUrl() {
        return null;
    }
}
