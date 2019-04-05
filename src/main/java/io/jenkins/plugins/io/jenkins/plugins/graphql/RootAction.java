package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.language.FieldDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactory;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import graphql.schema.idl.WiringFactory;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.model.Actionable;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Property;
import org.kohsuke.stapler.export.TypeUtil;

import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

@Extension
public class RootAction extends Actionable implements hudson.model.RootAction {
    private final static Logger LOGGER = Logger.getLogger(RootAction.class.getName());
    private static GraphQL builtSchema;
    /*package*/ static ModelBuilder MODEL_BUILDER = new ModelBuilder();
    private static final HashMap<String, String> javaTypesToGraphqlTypes = new HashMap<>();

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

    public static class ClassWiringFactory implements WiringFactory {
        @Override
        public boolean providesDataFetcherFactory(FieldWiringEnvironment environment) {
            FieldDefinition fieldDef = environment.getFieldDefinition();
            if ("_class".equals(fieldDef.getName())) {
                return true;
            }
            return false;
        }

        @Override
        public <T> DataFetcherFactory<T> getDataFetcherFactory(FieldWiringEnvironment environment) {
            return environment1 -> new DataFetcher<T>() {
                @Override
                public T get(DataFetchingEnvironment environment1) throws Exception {
                    return (T) environment1.getSource().getClass().getName();
                }
            };
        }
    }

    // @Initializer(after = InitMilestone.JOB_LOADED)
    public static void init() {
        javaTypesToGraphqlTypes.put("boolean", "Boolean");
        javaTypesToGraphqlTypes.put("string", "String");
        javaTypesToGraphqlTypes.put("float", "Float");
        javaTypesToGraphqlTypes.put("integer", "Int");
        javaTypesToGraphqlTypes.put("int", "Int");

        StringBuilder queryBuilder = new StringBuilder();
        StringBuilder typesBuilder = new StringBuilder();

        queryBuilder.append("allJobs: [Job]\n");
        typesBuilder.append(buildSchemaFromClass(Job.class).toString());

        for (TopLevelItemDescriptor d : DescriptorExtensionList.lookup(TopLevelItemDescriptor.class)) {
            if (Job.class.isAssignableFrom(d.clazz)) {
                queryBuilder.append("all" + d.clazz.getSimpleName() + ": [" + d.clazz.getSimpleName() + "]\n");
                typesBuilder.append(buildSchemaFromClass(d.clazz).toString());
            }
        }
        String schema = "schema {\n" +
                "    query: QueryType\n" +
                "}\n" +
                "type QueryType {\n" +
                    queryBuilder.toString() + "\n" +
                "}\n" +
                typesBuilder.toString() + "\n";

        LOGGER.info("Schema: " + schema);
        // if (!schema.isEmpty()) { throw new Error(schema); }
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .wiringFactory(new ClassWiringFactory())
                .type("QueryType", typeWiring -> {
                    TypeRuntimeWiring.Builder builder = typeWiring
                            .dataFetcher("allJobs", dataFetchingEnvironment -> Items.allItems(
                                    Jenkins.getAuthentication(),
                                    Jenkins.getInstanceOrNull(),
                                    Job.class
                            ));
                    for (TopLevelItemDescriptor d : DescriptorExtensionList.lookup(TopLevelItemDescriptor.class)) {
                        if (Job.class.isAssignableFrom(d.clazz)) {
                            builder = builder.dataFetcher("all" + d.clazz.getSimpleName(), dataFetchingEnvironment -> Items.allItems(
                                    Jenkins.getAuthentication(),
                                    Jenkins.getInstanceOrNull(),
                                    d.clazz
                            ));
                        }
                    }
                    return builder;
                }).build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        builtSchema = GraphQL.newGraphQL(graphQLSchema).build();
    }

    private static StringBuilder buildSchemaFromClass(Class clazz) {
        StringBuilder typeBuilder = new StringBuilder();
        typeBuilder.append("type " + clazz.getSimpleName() + " { \n_class: String!\n");
        Model<? extends TopLevelItem> model = MODEL_BUILDER.get(clazz);
        typeBuilder.append(createSchema(model));
        typeBuilder.append("\n");
        typeBuilder.append("}\n");
        return typeBuilder;
    }

    private static String createSchemaClassName(Property p, Class clazz) {
        if (clazz.isPrimitive() ||
            clazz.isAssignableFrom(String.class) ||
            clazz.isAssignableFrom(Integer.class) ||
            clazz.isAssignableFrom(Long.class) ||
            clazz.isAssignableFrom(Double.class) ||
            clazz.isAssignableFrom(Float.class) ||
            clazz.isAssignableFrom(Boolean.class) ||
            clazz.isAssignableFrom(Character.class) ||
            clazz.isAssignableFrom(Byte.class) ||
            clazz.isAssignableFrom(Void.class) ||
            clazz.isAssignableFrom(Short.class)
        ) {
            return javaTypesToGraphqlTypes.getOrDefault(clazz.getSimpleName(), clazz.getSimpleName());
        }
        return null;
    }

    private static String createSchema(Model<?> model) {
        StringBuilder sb = new StringBuilder();
        if (model.superModel != null) {
            sb.append(createSchema(model.superModel));
        }
        for (Property p : model.getProperties()) {
            Class clazz = p.getType();


            String className = null;
            if (clazz.isInstance(Object[].class)) {
                className = createSchemaClassName(p, clazz.getComponentType());
                if (className != null) {
                    className = "[" + className + "]";
                }
            }
            else if (Collection.class.isAssignableFrom(clazz)) {
                className = createSchemaClassName(p, TypeUtil.erasure(TypeUtil.getTypeArgument(TypeUtil.getBaseClass(p.getGenericType(), Collection.class),0)));
                if (className != null) {
                    className = "[" + className + "]";
                }
            } else {
                className = createSchemaClassName(p, clazz);
            }

            if (className == null) { continue; }
            sb.append(p.name);
            sb.append(":");
            sb.append(className);
            sb.append("\n");
        }
        return sb.toString();
    }

    @SuppressWarnings("unused")
    // @RequirePOST
    public void doIndex() throws IOException, ServletException {
        StaplerRequest req = Stapler.getCurrentRequest();
        StaplerResponse res = Stapler.getCurrentResponse();

        /* START - One time generation */
        /* FIXME */ init();
        /* END - One time generation */

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

    @Override
    public String getSearchUrl() {
        return null;
    }
}
