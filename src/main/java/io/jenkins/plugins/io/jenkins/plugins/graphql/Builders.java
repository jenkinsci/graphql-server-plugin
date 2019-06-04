package io.jenkins.plugins.io.jenkins.plugins.graphql;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import graphql.Scalars;
import graphql.TypeResolutionEnvironment;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.TypeResolver;
import hudson.model.Action;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.User;
import io.jenkins.plugins.io.jenkins.plugins.graphql.types.AdditionalScalarTypes;
import jenkins.model.Jenkins;
import jenkins.scm.RunWithSCM;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Property;
import org.kohsuke.stapler.export.TypeUtil;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Builders {
    private static final Logger LOGGER = Logger.getLogger(Builders.class.getName());
    private static final ModelBuilder MODEL_BUILDER = new ModelBuilder();

    static private GraphQLFieldDefinition makeClassFieldDefinition() {
        return GraphQLFieldDefinition.newFieldDefinition()
            .name("_class")
            .description("Class Name")
            .type(Scalars.GraphQLString)
            .dataFetcher(dataFetcher -> ClassUtils.getRealClass(dataFetcher.getSource().getClass()).getSimpleName())
//          .type(AdditionalScalarTypes.CLASS_SCALAR)
            .build();
    }


    private static final HashMap<String, GraphQLOutputType> javaTypesToGraphqlTypes = new HashMap<>();

    /*package*/ static final Set<Class> INTERFACES = new HashSet<>(Arrays.asList(
        Job.class,
        RunWithSCM.class
    ));

    /*package*/ static final Set<Class> TOP_LEVEL_CLASSES = new HashSet<>(Arrays.asList(
        Job.class,
        User.class
    ));

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

        javaTypesToGraphqlTypes.put("GregorianCalendar", AdditionalScalarTypes.gregrianCalendarScalar);
        javaTypesToGraphqlTypes.put("Calendar", AdditionalScalarTypes.gregrianCalendarScalar);
        javaTypesToGraphqlTypes.put("Date", AdditionalScalarTypes.gregrianCalendarScalar);

    }

    /*** DONE STATIC */
    private HashMap<String, GraphQLObjectType.Builder> graphQLTypes = new HashMap();
    private HashSet<Class> interfaces = new HashSet(INTERFACES);
    private PriorityQueue<Class> classQueue = new PriorityQueue<>(11, Comparator.comparing(Class::getName));
    private List<Class> extraTopLevelClasses = new ArrayList<>();

    private GraphQLOutputType createSchemaClassName(Class clazz) {
        if (javaTypesToGraphqlTypes.containsKey(clazz.getSimpleName())) {
            return javaTypesToGraphqlTypes.get(clazz.getSimpleName());
        }

        // interfaces are never exported, so handle them seperately
        if (!Modifier.isInterface(clazz.getModifiers())) {
            try {
                MODEL_BUILDER.get(clazz);
            } catch (org.kohsuke.stapler.export.NotExportableException e) {
                return Scalars.GraphQLString;
            }
        } else {
            if (!interfaces.contains(clazz)) {
                interfaces.add(clazz);
                classQueue.addAll(ClassUtils.findSubclasses(MODEL_BUILDER, clazz));
            }
        }
        classQueue.add(clazz);
        return GraphQLTypeReference.typeRef(ClassUtils.getGraphQLClassName(clazz));
    }

    private Class getCollectionClass(Property p) {
        return TypeUtil.erasure(TypeUtil.getTypeArgument(TypeUtil.getBaseClass(p.getGenericType(), Collection.class), 0));
    }

    public void buildSchemaFromClass(Class clazz) {
        if (graphQLTypes.containsKey(clazz.getName())) {
            return;
        }
        GraphQLObjectType.Builder fieldBuilder = GraphQLObjectType.newObject();

        fieldBuilder.name(ClassUtils.getGraphQLClassName(clazz)).field(makeClassFieldDefinition());

        Model<?> model;
        try {
            model = MODEL_BUILDER.get(clazz);
        } catch (org.kohsuke.stapler.export.NotExportableException e) {
            // interfaces.add(clazz);
            graphQLTypes.put(clazz.getName(), fieldBuilder);
            graphQLTypes.put(
                clazz.getPackage() + ".__" + clazz.getSimpleName(),
                GraphQLObjectType.newObject()
                    .name("__" + ClassUtils.getGraphQLClassName(clazz))
                    .description("Generic implementation of " + clazz.getSimpleName() + " with just _class defined")
                    .field(makeClassFieldDefinition())
                    .withInterface(GraphQLTypeReference.typeRef(ClassUtils.getGraphQLClassName(clazz)))

            );
            return;
        }

        for (Class topLevelClazz : interfaces) {
            if (topLevelClazz != clazz && topLevelClazz.isAssignableFrom(clazz)) {
                fieldBuilder.withInterface(GraphQLTypeReference.typeRef(topLevelClazz.getSimpleName()));
            }
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
                        .dataFetcher(dataFetchingEnvironment -> p.getValue(dataFetchingEnvironment.getSource()))
                        .build()
                );
            }
        }
        graphQLTypes.put(clazz.getName(), fieldBuilder);
    }

    @SuppressWarnings("rawtypes")
    public GraphQLSchema buildSchema() {
        GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject().name("QueryType");

        for (Class clazz : Stream.concat(interfaces.stream(), TOP_LEVEL_CLASSES.stream()).toArray(Class[]::new)) {
            this.buildSchemaFromClass(clazz);
        }

        for (Class clazz : TOP_LEVEL_CLASSES) {
            queryType = builAllQuery(queryType, clazz);
        }

        for (Class clazz : this.extraTopLevelClasses) {
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                for (Class topLevelClazz : TOP_LEVEL_CLASSES) {
                    if (clazz != topLevelClazz && topLevelClazz.isAssignableFrom(clazz)) {
                        classQueue.add(clazz);
                        queryType = builAllQuery(queryType, clazz);
                    }
                }
            }
        }

        while (!classQueue.isEmpty()) {
            Class clazz = classQueue.poll();
            if (clazz == Object.class) { continue; }
            if (clazz == Class.class) { continue; }
            this.buildSchemaFromClass(clazz);
        }

        HashSet<GraphQLType> types = new HashSet<>();
        GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();

        for (Class clazz : interfaces) {
            GraphQLInterfaceType interfaceType = convertToInterface(graphQLTypes.remove(clazz.getName()).build());
            types.add(interfaceType);
            LOGGER.info("Interface:" + interfaceType.getName());
            codeRegistry.typeResolver(interfaceType.getName(), new TypeResolver() {
                @Override
                public GraphQLObjectType getType(TypeResolutionEnvironment env) {
                    Class realClazz = ClassUtils.getRealClass(env.getObject().getClass());
                    String name = realClazz.getSimpleName();
                    LOGGER.info(name);
                    if (env.getSchema().getObjectType(name) != null) {
                        return env.getSchema().getObjectType(name);
                    }
                    for (Class interfaceClazz : ClassUtils.getAllInterfaces(realClazz)) {
                        GraphQLObjectType objectType = env.getSchema().getObjectType(
                            "__" + interfaceClazz.getSimpleName()
                        );
                        if (objectType != null) {
                            return objectType;
                        }
                    }
                    return null;
                }
            });
        }

        types.addAll(
            this.graphQLTypes
                .values()
                .stream()
                .map(GraphQLObjectType.Builder::build)
                .collect(Collectors.toList())
        );



        return GraphQLSchema.newSchema()
            .query(queryType.build())
            .codeRegistry(codeRegistry.build())
            .additionalTypes(types)
            .additionalType(ExtendedScalars.DateTime)
            .additionalType(AdditionalScalarTypes.gregrianCalendarScalar)
            .build();
    }

    private GraphQLInterfaceType convertToInterface(GraphQLObjectType objectType) {
        GraphQLInterfaceType.Builder interfaceType = GraphQLInterfaceType.newInterface();
        interfaceType.name(objectType.getName());
        interfaceType.description(objectType.getDescription());
        for (GraphQLFieldDefinition fieldDefinition : objectType.getFieldDefinitions()) {
            interfaceType.field(fieldDefinition);
        }
        for (GraphQLDirective directive: objectType.getDirectives()) {
            interfaceType.withDirective(directive);
        }
        return interfaceType.build();
    }

    private GraphQLObjectType.Builder builAllQuery(GraphQLObjectType.Builder queryType, Class clazz) {
        return queryType.field(GraphQLFieldDefinition.newFieldDefinition()
            .name("all" + clazz.getSimpleName() + "s")
            .type(GraphQLList.list(GraphQLTypeReference.typeRef(clazz.getSimpleName())))
            .argument(GraphQLArgument.newArgument()
                .name("offset")
                .type(Scalars.GraphQLInt)
                .defaultValue(0)
            )
            .argument(GraphQLArgument.newArgument()
                .name("limit")
                .type(Scalars.GraphQLInt)
                .defaultValue(100)
            )
            .dataFetcher(new DataFetcher<Object>() {
                public Iterator<?> slice(Iterable<?> base, int start, int limit) {
                    return Iterators.limit(Iterables.skip(base, start).iterator(), limit);
                }

                @Override
                public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
                    int offset = dataFetchingEnvironment.<Integer>getArgument("offset");
                    int limit = dataFetchingEnvironment.<Integer>getArgument("limit");


                    if (clazz == User.class) {
                        return Lists.newArrayList(slice(User.getAll(), offset, limit));
                    }

                    Iterable<?> items = Items.allItems(
                        Jenkins.getAuthentication(),
                        Jenkins.getInstanceOrNull(),
                        clazz
                    );
                    return Lists.newArrayList(slice(
                        items,
                        offset,
                        limit
                    ));
                }
            })
        );
    }

    public void addExtraTopLevelClasses(List<Class> clazzes) {
        this.extraTopLevelClasses.addAll(clazzes);
    }
}
