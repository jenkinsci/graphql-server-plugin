package io.jenkins.plugins.graphql;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
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
import graphql.schema.StaticDataFetcher;
import graphql.schema.TypeResolver;
import hudson.model.AbstractItem;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.User;
import hudson.security.WhoAmI;
import io.jenkins.plugins.graphql.types.AdditionalScalarTypes;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Property;
import org.kohsuke.stapler.export.TypeUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Builders {
    private static final Logger LOGGER = Logger.getLogger(Builders.class.getName());
    private static final ModelBuilder MODEL_BUILDER = new ModelBuilder();

    private static final String ARG_OFFSET = "offset";
    private static final String ARG_LIMIT = "limit";
    private static final String ARG_TYPE = "type";
    private static final String ARG_ID = "id";

    private static GraphQLFieldDefinition makeClassFieldDefinition() {
        return GraphQLFieldDefinition.newFieldDefinition()
            .name("_class")
            .description("Class Name")
            .type(Scalars.GraphQLString)
            .dataFetcher(dataFetcher -> ClassUtils.getRealClass(dataFetcher.getSource().getClass()).getName())
//          .type(AdditionalScalarTypes.CLASS_SCALAR)
            .build();
    }
    private static void makeClassIdDefintion(Class<?> clazz, GraphQLObjectType.Builder fieldBuilder) {
        final Method idMethod = IdFinder.idMethod(clazz);
        if (idMethod == null) {
            return;
        }
        fieldBuilder.field(GraphQLFieldDefinition.newFieldDefinition()
            .name("id")
            .description("Unique ID")
            .type(Scalars.GraphQLID)
            .dataFetcher(dataFetcher -> idMethod.invoke(dataFetcher.getSource()))
        .build());
    }

    private static Iterator<?> slice(Iterable<?> base, int start, int limit) {
        return Iterators.limit(Iterables.skip(base, start).iterator(), limit);
    }


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

        javaTypesToGraphqlTypes.put("GregorianCalendar", AdditionalScalarTypes.GregrianCalendarScalar);
        javaTypesToGraphqlTypes.put("Calendar", AdditionalScalarTypes.GregrianCalendarScalar);
        javaTypesToGraphqlTypes.put("Date", AdditionalScalarTypes.GregrianCalendarScalar);

    }

    // Utility function to find distinct by class field
    private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor)
    {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    /*** DONE STATIC */
    private HashMap<Class, GraphQLObjectType.Builder> graphQLTypes = new HashMap();
    private HashMap<String, GraphQLObjectType.Builder> mockGraphQLTypes = new HashMap();
    private HashSet<Class> interfaces = new HashSet();
    private PriorityQueue<Class> classQueue = new PriorityQueue<>(11, Comparator.comparing(Class::getName));
    private List<Class> extraTopLevelClasses = new ArrayList<>();

    protected GraphQLOutputType createSchemaClassName(Class clazz) {
        assert(clazz != null);

        if (javaTypesToGraphqlTypes.containsKey(clazz.getSimpleName())) {
            return javaTypesToGraphqlTypes.get(clazz.getSimpleName());
        }

        // interfaces are never exported, so handle them seperately
        if (Modifier.isInterface(clazz.getModifiers()) || Modifier.isAbstract(clazz.getModifiers())) {
            if (!interfaces.contains(clazz)) {
                interfaces.add(clazz);
                classQueue.addAll(ClassUtils.findSubclasses(MODEL_BUILDER, clazz));
            }
        } else {
            try {
                MODEL_BUILDER.get(clazz);
            } catch (org.kohsuke.stapler.export.NotExportableException e) {
                return Scalars.GraphQLString;
            }
        }
        classQueue.add(clazz);
        return GraphQLTypeReference.typeRef(ClassUtils.getGraphQLClassName(clazz));
    }

    private Class getCollectionClass(Property p) {
        return TypeUtil.erasure(TypeUtil.getTypeArgument(TypeUtil.getBaseClass(p.getGenericType(), Collection.class), 0));
    }

    private void buildSchemaFromClass(Class<?> clazz) {
        if (graphQLTypes.containsKey(clazz)) {
            return;
        }
        if (shouldIgnoreClass(clazz)) {
            return;
        }

        boolean isInterface = Modifier.isInterface(clazz.getModifiers()) || Modifier.isAbstract(clazz.getModifiers());
        try {
            MODEL_BUILDER.get(clazz);
        } catch (org.kohsuke.stapler.export.NotExportableException e) {
            isInterface = true;
        }

        if (isInterface) {
            GraphQLObjectType.Builder fieldBuilder = GraphQLObjectType.newObject();
            fieldBuilder.name(ClassUtils.getGraphQLClassName(clazz))
                .field(makeClassFieldDefinition());
            makeClassIdDefintion(clazz, fieldBuilder);

            interfaces.add(clazz);
            mockGraphQLTypes.put(
                ClassUtils.getGraphQLClassName(clazz) + "__",
                GraphQLObjectType.newObject()
                    .name(ClassUtils.getGraphQLClassName(clazz) + "__")
                    .description("Generic implementation of " + clazz.getSimpleName() + " with just _class defined")
                    .field(makeClassFieldDefinition())
                    .withInterface(GraphQLTypeReference.typeRef(ClassUtils.getGraphQLClassName(clazz)))

            );
            makeClassIdDefintion(clazz, mockGraphQLTypes.get(ClassUtils.getGraphQLClassName(clazz) + "__"));

            graphQLTypes.put(clazz, fieldBuilder);
            return;
        }
        graphQLTypes.put(clazz, buildGraphQLTypeFromModel(clazz));
    }

    static boolean shouldIgnoreClass(Class clazz) {
        return clazz.isAnnotationPresent(NoExternalUse.class) || clazz.isAnonymousClass();
    }

    @SuppressWarnings("squid:S135")
    GraphQLObjectType.Builder buildGraphQLTypeFromModel(Class clazz) {

        Model<?> model = MODEL_BUILDER.get(clazz);

        GraphQLObjectType.Builder typeBuilder = GraphQLObjectType.newObject();

        Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance != null) {
            Descriptor descriptor = instance.getDescriptor(clazz);
            if (descriptor != null) {
                typeBuilder.description(descriptor.getDisplayName());
            }
        }

        typeBuilder
            .name(ClassUtils.getGraphQLClassName(clazz))
            .field(makeClassFieldDefinition());
        makeClassIdDefintion(clazz, typeBuilder);

        ArrayList<Model<?>> queue = new ArrayList<>();
        queue.add(model);

        Model<?> superModel = model.superModel;
        while (superModel != null) {
            queue.add(superModel);
            superModel = superModel.superModel;
        }

        for (Model<?> _model : queue) {
            for (Property p : _model.getProperties()) {
                if (typeBuilder.hasField(p.name)) {
                    continue;
                }
                Class propertyClazz = p.getType();

                GraphQLOutputType className;
                if ("id".equals(p.name)) {
                    continue; /// we handle it in a different way
                } else if (propertyClazz.isArray()) {
                    className = GraphQLList.list(createSchemaClassName(propertyClazz.getComponentType()));
                } else if (Collection.class.isAssignableFrom(propertyClazz)) {
                    className = GraphQLList.list(createSchemaClassName(getCollectionClass(p)));
                } else {
                    className = createSchemaClassName(propertyClazz);
                }

                GraphQLFieldDefinition.Builder fieldBuilder = GraphQLFieldDefinition.newFieldDefinition()
                    .name(p.name)
                    .type(className)
                    .dataFetcher(dataFetchingEnvironment -> p.getValue(dataFetchingEnvironment.getSource()));

                if (className instanceof GraphQLList) {
                    fieldBuilder.dataFetcher(dataFetchingEnvironment -> {
                        int offset = dataFetchingEnvironment.<Integer>getArgument(ARG_OFFSET);
                        int limit = dataFetchingEnvironment.<Integer>getArgument(ARG_LIMIT);
                        String id = dataFetchingEnvironment.getArgument(ARG_ID);

                        List<?> valuesList;
                        Object values = p.getValue(dataFetchingEnvironment.getSource());
                        if (values instanceof List) {
                            valuesList = ((List<?>)values);
                        } else {
                            valuesList = Arrays.asList((Object[]) values);

                        }
                        if (id != null && !id.isEmpty()) {
                            for (Object value : valuesList) {
                                Method method = IdFinder.idMethod(value.getClass());
                                if (method == null) {
                                    continue;
                                }

                                String objectId = String.valueOf(method.invoke(value));
                                if (id.equals(objectId)) {
                                    return Stream.of(value)
                                        .filter(StreamUtils::isAllowed)
                                        .toArray();
                                }
                            }
                            return null;
                        }

                        return Lists.newArrayList(slice(valuesList, offset, limit))
                            .stream()
                            .filter(StreamUtils::isAllowed)
                            .toArray();
                    });
                    fieldBuilder.argument(GraphQLArgument.newArgument()
                            .name(ARG_OFFSET)
                            .type(Scalars.GraphQLInt)
                            .defaultValue(0)
                        )
                        .argument(GraphQLArgument.newArgument()
                            .name(ARG_LIMIT)
                            .type(Scalars.GraphQLInt)
                            .defaultValue(100)
                        )
                        .argument(GraphQLArgument.newArgument()
                            .name(ARG_TYPE)
                            .type(Scalars.GraphQLString)
                        )
                        .argument(GraphQLArgument.newArgument()
                            .name(ARG_ID)
                            .type(Scalars.GraphQLID)
                        );
                }
                typeBuilder = typeBuilder.field(fieldBuilder.build());
            }
        }
        return typeBuilder;
    }

    @SuppressWarnings("rawtypes")
    public GraphQLSchema buildSchema() {
        GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject().name("QueryType");

        queryType.field(buildAllQuery(AbstractItem.class, "allItems"));
        queryType.field(buildAllQuery(User.class));
        queryType.field(buildActionQuery(WhoAmI.class));

        classQueue.add(AbstractItem.class);
        classQueue.add(Job.class);
        classQueue.add(User.class);
        classQueue.addAll(this.interfaces);
        classQueue.addAll(this.extraTopLevelClasses);

        while (!classQueue.isEmpty()) {
            Class clazz = classQueue.poll();
            if (clazz == Object.class || clazz == Class.class) { continue; }
            this.buildSchemaFromClass(clazz);
        }

        HashSet<GraphQLType> types = new HashSet<>();
        GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();

        for (Class clazz : interfaces) {
            GraphQLInterfaceType interfaceType = convertToInterface(graphQLTypes.remove(clazz).build());
            types.add(interfaceType);
            LOGGER.info("Interface:" + interfaceType.getName());
            codeRegistry.typeResolver(interfaceType.getName(), buildTypeResolver());
        }

        for (Class<?> interfaceClazz : interfaces) {
            for (Map.Entry<Class, GraphQLObjectType.Builder> instanceClazz : this.graphQLTypes.entrySet()) {
                if (interfaceClazz != instanceClazz.getKey() && interfaceClazz.isAssignableFrom(instanceClazz.getKey())) {
                    this.graphQLTypes.get(instanceClazz.getKey()).withInterface(GraphQLTypeReference.typeRef(ClassUtils.getGraphQLClassName(interfaceClazz)));
                }
            }
        }

        types.addAll(
            Stream.concat(
                this.mockGraphQLTypes.values().stream(),
                this.graphQLTypes.values().stream()
            )
                .map(GraphQLObjectType.Builder::build)
                .filter(distinctByKey(GraphQLObjectType::getName))
                .collect(Collectors.toList())
        );

        this.graphQLTypes = null;
        this.mockGraphQLTypes = null;
        this.interfaces = null;
        this.classQueue = null;
        this.extraTopLevelClasses = null;

        return GraphQLSchema.newSchema()
            .query(queryType.build())
            .codeRegistry(codeRegistry.build())
            .additionalTypes(types)
            .additionalType(ExtendedScalars.DateTime)
            .additionalType(AdditionalScalarTypes.GregrianCalendarScalar)
            .build();
    }

    private TypeResolver buildTypeResolver() {
        return env -> {
            Class realClazz = ClassUtils.getRealClass(env.getObject().getClass());
            String name = ClassUtils.getGraphQLClassName(realClazz);
            LOGGER.log(Level.INFO, "Attempting to find: {0}", name);
            if (env.getSchema().getObjectType(name) != null) {
                return env.getSchema().getObjectType(name);
            }

            // FIXME - I think i started this earlier to find the right impl, but then forgot about it
            // should probably ignore ones starting with __
            // find where name == graphqlname(class)
            // fall through to the __ one
            // maybe, cause that only checks the impls that match the subclass exactly
//                List<GraphQLType> impls = env.getSchema().getTypeMap()
//                    .values()
//                    .stream()
//                    .filter(i -> i instanceof GraphQLObjectType && ((GraphQLObjectType) i).getInterfaces().contains(env.getFieldType()))
//                    .collect(Collectors.toList());
            for (Class subclassClazz : ClassUtils.getAllSuperClasses(realClazz)) {
                name = ClassUtils.getGraphQLClassName(subclassClazz) + "__";
                LOGGER.log(Level.INFO, "Attempting to find subclass: {0}", name);
                GraphQLObjectType objectType = env.getSchema().getObjectType(name);
                if (objectType != null) {
                    return objectType;
                }
            }
            for (Class interfaceClazz : ClassUtils.getAllInterfaces(realClazz)) {
                name = ClassUtils.getGraphQLClassName(interfaceClazz) + "__";
                LOGGER.log(Level.INFO, "Attempting to find interface: {0}", name);
                GraphQLObjectType objectType = env.getSchema().getObjectType(name);
                if (objectType != null && objectType.getInterfaces().contains(env.getFieldType())) {
                    return objectType;
                }
            }
            return null;
        };
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

    public GraphQLFieldDefinition.Builder buildAllQuery( Class<?> defaultClazz) {
        return buildAllQuery(defaultClazz, "all" + defaultClazz.getSimpleName() + "s");
    }

    public GraphQLFieldDefinition.Builder buildAllQuery( Class<?> defaultClazz, String fieldName) {
        return GraphQLFieldDefinition.newFieldDefinition()
            .name(fieldName)
            .type(GraphQLList.list(createSchemaClassName(defaultClazz)))
            .argument(GraphQLArgument.newArgument()
                .name(ARG_OFFSET)
                .type(Scalars.GraphQLInt)
                .defaultValue(0)
            )
            .argument(GraphQLArgument.newArgument()
                .name(ARG_LIMIT)
                .type(Scalars.GraphQLInt)
                .defaultValue(100)
            )
            .argument(GraphQLArgument.newArgument()
                .name(ARG_TYPE)
                .type(Scalars.GraphQLString)
            )
            .argument(GraphQLArgument.newArgument()
                .name(ARG_ID)
                .type(Scalars.GraphQLID)
            )
            .dataFetcher(dataFetchingEnvironment -> {
                Class clazz = defaultClazz;
                Jenkins instance = Jenkins.getInstanceOrNull();
                int offset = dataFetchingEnvironment.<Integer>getArgument(ARG_OFFSET);
                int limit = dataFetchingEnvironment.<Integer>getArgument(ARG_LIMIT);
                String clazzName = dataFetchingEnvironment.getArgument(ARG_TYPE);
                String id = dataFetchingEnvironment.getArgument(ARG_ID);

                if (clazzName != null && !clazzName.isEmpty()) {
                    clazz = Class.forName(clazzName);
                }

                Iterable iterable;
                if (clazz == User.class) {
                    if (id != null && !id.isEmpty()) {
                        return Stream.of(User.get(id, false, Collections.emptyMap()))
                            .filter(Objects::nonNull)
                            .filter(StreamUtils::isAllowed)
                            .toArray();
                    }
                    iterable = User.getAll();
                } else {
                    if (id != null && !id.isEmpty()) {
                        if (instance == null) {
                            LOGGER.log(Level.SEVERE, "Jenkins.getInstanceOrNull() is null, panic panic die die");
                            return null;
                        }
                        return Stream.of(instance.getItemByFullName(id))
                            .filter(Objects::nonNull)
                            .filter(StreamUtils::isAllowed)
                            .toArray();
                    }

                    iterable = Items.allItems(
                        Jenkins.getAuthentication(),
                        Jenkins.getInstanceOrNull(),
                        clazz
                    );
                }
                return Lists.newArrayList(slice(iterable, offset, limit))
                    .stream()
                    .filter(StreamUtils::isAllowed)
                    .toArray();
            });
    }

    private GraphQLFieldDefinition buildActionQuery(Class<? extends Action> actionClazz) {
        Action action;
        try {
            action = actionClazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }

        return GraphQLFieldDefinition.newFieldDefinition()
            .name(action.getUrlName())
            .type(createSchemaClassName(actionClazz))
            .dataFetcher(new StaticDataFetcher(action))
            .build();
    }

    public void addExtraTopLevelClasses(List<Class> clazzes) {
        this.extraTopLevelClasses.addAll(clazzes);
    }
}
