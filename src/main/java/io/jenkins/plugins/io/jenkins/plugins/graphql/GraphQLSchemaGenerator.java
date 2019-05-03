package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.GraphQL;
import graphql.Scalars;
import graphql.TypeResolutionEnvironment;
import graphql.language.FieldDefinition;
import graphql.language.SDLDefinition;
import graphql.schema.*;
import graphql.schema.idl.*;
import hudson.DescriptorExtensionList;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Property;
import org.kohsuke.stapler.export.TypeUtil;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GraphQLSchemaGenerator {
    private final static Logger LOGGER = Logger.getLogger(GraphQLSchemaGenerator.class.getName());

    /*package*/ static final Set<Class> TOP_LEVEL_CLASSES = new HashSet<Class>(Arrays.asList(
        Job.class
    ));

    /**********************/

    public GraphQL generateSchema() {
//        SchemaParser schemaParser = new SchemaParser();
//        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(
//            "schema {\n    query: Query\n}\n\n" +
//            "           type Query { allJobs: [Job] }\n"
//        ); // this.getSchema());
//        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
//            .wiringFactory(new WiringFactory() {
//                @Override
//                public boolean providesTypeResolver(InterfaceWiringEnvironment environment) {
//                    return true;
//                }
//
//                @Override
//                public TypeResolver getTypeResolver(InterfaceWiringEnvironment environment) {
//                    return new TypeResolver() {
//                        @Override
//                        public GraphQLObjectType getType(TypeResolutionEnvironment env) {
//                            Object javaObject = env.getObject();
////                            for (Class clazz : TOP_LEVEL_CLASSES) {
////                                if (clazz.isAssignableFrom(javaObject.getClass())) {
////                                    return env.getSchema().getObjectType(clazz.getSimpleName());
////                                }
////                            }
//                            return  env.getSchema().getObjectType(javaObject.getClass().getSimpleName());
//                        }
//                    };
//                }
//
//                @Override
//                public boolean providesDataFetcherFactory(FieldWiringEnvironment environment) {
//                    FieldDefinition fieldDef = environment.getFieldDefinition();
//                    if ("_class".equals(fieldDef.getName())) {
//                        return true;
//                    }
//                    String name = environment.getParentType().getName() + "#" + environment.getFieldDefinition().getName();
//                    return propertyMap.containsKey(name);
//                }
//
//                @Override
//                public <T> DataFetcherFactory<T> getDataFetcherFactory(FieldWiringEnvironment environment) {
//                    return environment1 -> (DataFetcher<T>) environment11 -> {
//                        FieldDefinition fieldDef = environment.getFieldDefinition();
//                        if ("_class".equals(fieldDef.getName())) {
//                            return (T) environment11.getSource().getClass().getName();
//                        }
//                        String name = environment.getParentType().getName() + "#" + environment.getFieldDefinition().getName();
//                        return (T) propertyMap.get(name).getValue(environment11.getSource());
//                    };
//                }
//            })
//            .type("Query", typeWiring -> {
//                TypeRuntimeWiring.Builder builder = typeWiring
//                    .dataFetcher("allJobs", dataFetchingEnvironment -> Items.allItems(
//                        Jenkins.getAuthentication(),
//                        Jenkins.getInstanceOrNull(),
//                        Job.class
//                    ));
//                for (TopLevelItemDescriptor d : DescriptorExtensionList.lookup(TopLevelItemDescriptor.class)) {
//                    if (Job.class.isAssignableFrom(d.clazz)) {
//                        builder = builder.dataFetcher("all" + d.clazz.getSimpleName(), dataFetchingEnvironment -> Items.allItems(
//                            Jenkins.getAuthentication(),
//                            Jenkins.getInstanceOrNull(),
//                            d.clazz
//                        ));
//                    }
//                }
//                return builder;
//            }).build();
//
//        SchemaGenerator schemaGenerator = new SchemaGenerator();

        Builders b = new Builders();
        // typeDefinitionRegistry.add(b.buildSchemaFromClass(FreeStyleProject.class).getDefinition());

        // GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        return GraphQL.newGraphQL(b.buildSchema()).build();
    }

}
