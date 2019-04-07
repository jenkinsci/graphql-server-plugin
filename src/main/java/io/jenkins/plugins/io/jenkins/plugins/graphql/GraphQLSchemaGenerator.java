package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.GraphQL;
import graphql.language.FieldDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactory;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import hudson.DescriptorExtensionList;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Property;
import org.kohsuke.stapler.export.TypeUtil;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class GraphQLSchemaGenerator {
    /*package*/ static ModelBuilder MODEL_BUILDER = new ModelBuilder();
    /*package*/ static final Set<Class> STRING_TYPES = new HashSet<Class>(Arrays.asList(
        String.class,
        URL.class
    ));

    /*package*/ static final Set<Class> PRIMITIVE_TYPES = new HashSet<Class>(Arrays.asList(
        Integer.class,
        Long.class,
        Boolean.class,
        Short.class,
        Character.class,
        Float.class,
        Double.class
    ));

    private static final HashMap<String, String> javaTypesToGraphqlTypes = new HashMap<>();
    static {
        javaTypesToGraphqlTypes.put("boolean", "Boolean");
        javaTypesToGraphqlTypes.put("string", "String");
        javaTypesToGraphqlTypes.put("float", "Float");
        javaTypesToGraphqlTypes.put("integer", "Int");
        javaTypesToGraphqlTypes.put("int", "Int");
        javaTypesToGraphqlTypes.put("long", "Int");
    }

    private final HashSet<Class> classes;
    private final HashMap<String, Property> propertyMap;
    private String schemaString;


    public GraphQLSchemaGenerator() {
        classes = new HashSet<Class>();
        propertyMap = new HashMap<>();
    }

    public String generateSchemaString() {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append("    allJobs: [Job]!\n");
        classes.add(Job.class);
        findAllClasses(Job.class);

        for (TopLevelItemDescriptor d : DescriptorExtensionList.lookup(TopLevelItemDescriptor.class)) {
            if (Job.class.isAssignableFrom(d.clazz)) {
                queryBuilder.append("    all" + d.clazz.getSimpleName() + ": [" + d.clazz.getSimpleName() + "]!\n");
                classes.add(d.clazz);
                findAllClasses(d.clazz);
            }
        }
        return "schema {\n" +
            "    query: Query\n" +
            "}\n" +
            "type Query {\n" +
            queryBuilder.toString() +
            "}\n" +
            classes.stream()
                .sorted(Comparator.comparing(Object::toString))
                .map( clazz -> buildSchemaFromClass(clazz).toString())
                .collect( Collectors.joining( "\n\n" ) );
    }

    private void findAllClasses(Class clazz) {
        Model<? extends TopLevelItem> model = MODEL_BUILDER.get(clazz);

        if (model.superModel != null) {
            findAllClasses(model.superModel.type);
        }
        for (Property p : model.getProperties()) {
            Class propertyClazz = p.getType();

            if (clazz.isInstance(Object[].class)) {
                classes.add(propertyClazz.getComponentType());
            } else if (Collection.class.isAssignableFrom(propertyClazz)) {
                classes.add(getCollectionClass(p));
            } else if (!isScalarClassType(propertyClazz)){
                try {
                    MODEL_BUILDER.get(propertyClazz);
                } catch (org.kohsuke.stapler.export.NotExportableException e) {
                    continue;
                }
                classes.add(propertyClazz);
            }
        }
    }

    private Class getCollectionClass(Property p) {
        return TypeUtil.erasure(TypeUtil.getTypeArgument(TypeUtil.getBaseClass(p.getGenericType(), Collection.class), 0));
    }

    private StringBuilder buildSchemaFromClass(Class clazz) {
        StringBuilder typeBuilder = new StringBuilder();
        typeBuilder.append("type ");
        typeBuilder.append(clazz.getSimpleName());
//        if (classes.contains(clazz.getSuperclass())) {
//            typeBuilder.append(" implements ");
//            typeBuilder.append(clazz.getSuperclass().getSimpleName());
//        }
        typeBuilder.append(" { \n");
        typeBuilder.append("    _class: String!\n");
        typeBuilder.append(createSchema(clazz.getSimpleName(), clazz));
        typeBuilder.append("}\n");
        return typeBuilder;
    }

    private String createSchemaClassName(Class clazz) {
        if (isScalarClassType(clazz)) {
            return javaTypesToGraphqlTypes.getOrDefault(clazz.getSimpleName(), clazz.getSimpleName());
        }
        if (!classes.contains(clazz)) {
            return "String";
        }
        return clazz.getSimpleName();
    }

    private boolean isScalarClassType(Class clazz) {
        return clazz.isPrimitive() ||
            clazz.isAssignableFrom(String.class) ||
            clazz.isAssignableFrom(Integer.class) ||
            clazz.isAssignableFrom(Long.class) ||
            clazz.isAssignableFrom(Double.class) ||
            clazz.isAssignableFrom(Float.class) ||
            clazz.isAssignableFrom(Boolean.class) ||
            clazz.isAssignableFrom(Character.class) ||
            clazz.isAssignableFrom(Byte.class) ||
            clazz.isAssignableFrom(Void.class) ||
            clazz.isAssignableFrom(Short.class);
    }

    private String createSchema(String containerTypeName, Class clazz) {
        StringBuilder sb = new StringBuilder();
        Model<? extends TopLevelItem> model;
        try {
            model = MODEL_BUILDER.get(clazz);
        } catch (org.kohsuke.stapler.export.NotExportableException e) {
            return "";
        }
        for (Property p : model.getProperties()) {
            if (propertyMap.containsKey(containerTypeName + "#" + p.name)) {
                continue;
            }
            Class propertyClazz = p.getType();

            String className = null;
            if ("id".equals(p.name)) {
                className = "ID";
            } else if (propertyClazz.isInstance(Object[].class)) {
                className = "[" + createSchemaClassName(propertyClazz.getComponentType()) + "]";
            } else if (Collection.class.isAssignableFrom(propertyClazz)) {
                className = "[" + createSchemaClassName(getCollectionClass(p)) + "]";
            } else {
                className = createSchemaClassName(propertyClazz);
            }

            sb.append("    ");
            sb.append(p.name);
            sb.append(": ");
            sb.append(className);
            if (propertyClazz.isAnnotationPresent(Nonnull.class)) {
                sb.append("!");
            }
            sb.append("\n");
            propertyMap.put(containerTypeName + "#" + p.name, p);
        }
        if (model.superModel != null) {
            sb.append(createSchema(containerTypeName, model.superModel.type));
        }
        return sb.toString();
    }

    public String getSchema() {
        if (this.schemaString == null) {
            this.schemaString = this.generateSchemaString();
        }
        return this.schemaString;

    }

    /**********************/

    public GraphQL generateSchema() {
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(this.getSchema());
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
            .wiringFactory(new WiringFactory() {
                @Override
                public boolean providesDataFetcherFactory(FieldWiringEnvironment environment) {
                    FieldDefinition fieldDef = environment.getFieldDefinition();
                    if ("_class".equals(fieldDef.getName())) {
                        return true;
                    }
                    String name = environment.getParentType().getName() + "#" + environment.getFieldDefinition().getName();
                    return propertyMap.containsKey(name);
                }

                @Override
                public <T> DataFetcherFactory<T> getDataFetcherFactory(FieldWiringEnvironment environment) {
                    return environment1 -> (DataFetcher<T>) environment11 -> {
                        FieldDefinition fieldDef = environment.getFieldDefinition();
                        if ("_class".equals(fieldDef.getName())) {
                            return (T) environment11.getSource().getClass().getName();
                        }
                        String name = environment.getParentType().getName() + "#" + environment.getFieldDefinition().getName();
                        return (T) propertyMap.get(name).getValue(environment11.getSource());
                    };
                }
            })
            .type("Query", typeWiring -> {
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
        return GraphQL.newGraphQL(graphQLSchema).build();
    }

}
