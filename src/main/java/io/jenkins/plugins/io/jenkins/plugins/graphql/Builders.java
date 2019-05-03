package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.Scalars;
import graphql.language.ObjectTypeDefinition;
import graphql.language.StringValue;
import graphql.language.TypeDefinition;
import graphql.schema.*;
import hudson.model.Job;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Property;
import org.kohsuke.stapler.export.TypeUtil;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Builders {
    private final static Logger LOGGER = Logger.getLogger(GraphQLSchemaGenerator.class.getName());
    private static final ModelBuilder MODEL_BUILDER = new ModelBuilder();

    public static final GraphQLScalarType GraphQLClass = GraphQLScalarType.newScalar()
        .name("Class")
        .description("class")
        .coercing(new Coercing<String, String>() {
            public String serialize(Object input) {
                return input.toString();
            }

            public String parseValue(Object input) {
                return this.serialize(input);
            }

            public String parseLiteral(Object input) {
                if (!(input instanceof StringValue)) {
                    throw new CoercingParseLiteralException("Expected AST type 'StringValue' but was '" + input.getClass().toString() + "'.");
                } else {
                    return ((StringValue)input).getValue();
                }
            }
         }).build();

    private static final HashMap<String, GraphQLOutputType> javaTypesToGraphqlTypes = new HashMap<>();

    static {
        javaTypesToGraphqlTypes.put("boolean", Scalars.GraphQLBoolean);
        javaTypesToGraphqlTypes.put(Boolean.class.getSimpleName(), Scalars.GraphQLBoolean);

        javaTypesToGraphqlTypes.put("char", Scalars.GraphQLBoolean);
        javaTypesToGraphqlTypes.put(Character.class.getSimpleName(), Scalars.GraphQLChar);

        javaTypesToGraphqlTypes.put("byte", Scalars.GraphQLByte);
        javaTypesToGraphqlTypes.put(Byte.class.getSimpleName(), Scalars.GraphQLByte);

        javaTypesToGraphqlTypes.put("string", Scalars.GraphQLString);
        javaTypesToGraphqlTypes.put(String.class.getSimpleName(), Scalars.GraphQLString);

        javaTypesToGraphqlTypes.put("float", Scalars.GraphQLFloat);
        javaTypesToGraphqlTypes.put(Float.class.getSimpleName(), Scalars.GraphQLFloat);

        javaTypesToGraphqlTypes.put("integer", Scalars.GraphQLInt);
        javaTypesToGraphqlTypes.put("int", Scalars.GraphQLInt);
        javaTypesToGraphqlTypes.put(Integer.class.getSimpleName(), Scalars.GraphQLInt);

        javaTypesToGraphqlTypes.put("long", Scalars.GraphQLLong);
        javaTypesToGraphqlTypes.put(Long.class.getSimpleName(), Scalars.GraphQLLong);

        javaTypesToGraphqlTypes.put("double", Scalars.GraphQLLong);
        javaTypesToGraphqlTypes.put(Double.class.getSimpleName(), Scalars.GraphQLBigDecimal);

        javaTypesToGraphqlTypes.put("short", Scalars.GraphQLShort);
        javaTypesToGraphqlTypes.put(Short.class.getSimpleName(), Scalars.GraphQLShort);

    }

    /*** DONE STATIC */
    private HashMap<String, GraphQLObjectType> graphQLTypes = new HashMap();
    private PriorityQueue<Class> classQueue = new PriorityQueue<>(11, new Comparator<Class>() {
        @Override
        public int compare(Class o1, Class o2) {
            return o1.getName().compareTo(o2.getName());
        }
    });

    private GraphQLOutputType createSchemaClassName(Class clazz) {
        if (javaTypesToGraphqlTypes.containsKey(clazz.getSimpleName())) {
            return javaTypesToGraphqlTypes.get(clazz.getSimpleName());
        }
        try {
            Model<?> model = MODEL_BUILDER.get(clazz);
        } catch (org.kohsuke.stapler.export.NotExportableException e) {
            return Scalars.GraphQLString;
        }
        classQueue.add(clazz);
        // return clazz.isPrimitive() ||
        GraphQLTypeReference graphQLTypeReference = GraphQLTypeReference.typeRef(clazz.getSimpleName());
        if (graphQLTypeReference != null) {
            return graphQLTypeReference;
        }
        throw new RuntimeException("No such clazz: " + clazz.getSimpleName());
    }

    private Class getCollectionClass(Property p) {
        return TypeUtil.erasure(TypeUtil.getTypeArgument(TypeUtil.getBaseClass(p.getGenericType(), Collection.class), 0));
    }

    public GraphQLObjectType buildSchemaFromClass(Class clazz) {
        if (graphQLTypes.containsKey(clazz.getSimpleName())) {
            return graphQLTypes.get(clazz.getSimpleName());
        }
        GraphQLObjectType.Builder fieldBuilder = GraphQLObjectType.newObject()
            .name(clazz.getSimpleName())
            .description("moo")
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name("_class")
                    .description("Class Name")
                    .type(Scalars.GraphQLString)
                    .build()
            );

        Model<?> model;
        try {
            model = MODEL_BUILDER.get(clazz);
        } catch (org.kohsuke.stapler.export.NotExportableException e) {
            GraphQLObjectType type = fieldBuilder.build();
            graphQLTypes.put(clazz.getSimpleName(), type);
            return type;
        }

        ArrayList<Model<?>> queue = new ArrayList();
        queue.add(model);

        Model<?> superModel = model.superModel;
        while (superModel != null) {
            queue.add(superModel);
            superModel = superModel.superModel;
        }

        for (Model<?> _model : queue) {
            for (Property p : _model.getProperties()) {
                if (fieldBuilder.hasField(p.name)) {
                    continue;
                }
                Class propertyClazz = p.getType();

                GraphQLOutputType className = null;
                if ("id".equals(p.name)) {
                    className = Scalars.GraphQLID;
                } else if (propertyClazz.isInstance(Object[].class)) {
                    className = GraphQLList.list(createSchemaClassName(propertyClazz.getComponentType()));
                } else if (Collection.class.isAssignableFrom(propertyClazz)) {
                    className = GraphQLList.list(createSchemaClassName(getCollectionClass(p)));
                } else {
                    className = createSchemaClassName(propertyClazz);
                }

                fieldBuilder = fieldBuilder.field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(p.name)
                        .type(className)
                        .build()
                );
            }
        }

        GraphQLObjectType type = fieldBuilder.build();
        graphQLTypes.put(clazz.getSimpleName(), type);
        return type;
    }

    public GraphQLSchema buildSchema() {
        GraphQLObjectType queryType = GraphQLObjectType.newObject()
            .name("QueryType")

            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("allJobs")
                .type(GraphQLTypeReference.typeRef("Job"))
                .argument(GraphQLArgument.newArgument()
                    .name("start")
                    .type(GraphQLNonNull.nonNull(Scalars.GraphQLInt)))
                .argument(GraphQLArgument.newArgument()
                    .name("size")
                    .type(GraphQLNonNull.nonNull(Scalars.GraphQLInt)))
                // .dataFetcher(inputDF))
            ).build();


        classQueue.add(Job.class);
        while (!classQueue.isEmpty()) {
            this.buildSchemaFromClass(classQueue.poll());
        }
        return GraphQLSchema.newSchema()
            .query(queryType)
            .additionalTypes(new HashSet<GraphQLType>(graphQLTypes.values()))
            .build();
    }
}
