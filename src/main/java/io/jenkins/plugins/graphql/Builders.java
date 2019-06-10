package io.jenkins.plugins.graphql;

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
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.User;
import io.jenkins.plugins.graphql.types.AdditionalScalarTypes;
import jenkins.model.Jenkins;
import jenkins.scm.RunWithSCM;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Property;
import org.kohsuke.stapler.export.TypeUtil;

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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
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
            .dataFetcher(dataFetcher -> ClassUtils.getRealClass(dataFetcher.getSource().getClass()).getName())
//          .type(AdditionalScalarTypes.CLASS_SCALAR)
            .build();
    }

    private static final HashMap<String, GraphQLOutputType> javaTypesToGraphqlTypes = new HashMap<>();

    /*package*/ private static final Set<Class> INTERFACES = new HashSet<>(Arrays.asList(
        Job.class,
        RunWithSCM.class
    ));

    /*package*/ private static final Set<Class> TOP_LEVEL_CLASSES = new HashSet<>(Arrays.asList(
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
    private HashSet<Class> interfaces = new HashSet(INTERFACES);
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

    private void buildSchemaFromClass(Class clazz) {
        if (graphQLTypes.containsKey(clazz)) {
            return;
        }
        if (shouldIgnoreClass(clazz)) {
            return;
        }

        try {
            MODEL_BUILDER.get(clazz);
        } catch (org.kohsuke.stapler.export.NotExportableException e) {
            GraphQLObjectType.Builder fieldBuilder = GraphQLObjectType.newObject();
            fieldBuilder.name(ClassUtils.getGraphQLClassName(clazz)).field(makeClassFieldDefinition());
            interfaces.add(clazz);
            mockGraphQLTypes.put(
                "__" + ClassUtils.getGraphQLClassName(clazz),
                GraphQLObjectType.newObject()
                    .name("__" + ClassUtils.getGraphQLClassName(clazz))
                    .description("Generic implementation of " + clazz.getSimpleName() + " with just _class defined")
                    .field(makeClassFieldDefinition())
                    .withInterface(GraphQLTypeReference.typeRef(ClassUtils.getGraphQLClassName(clazz)))

            );
            graphQLTypes.put(clazz, fieldBuilder);
            return;
        }
        graphQLTypes.put(clazz, buildGraphQLTypeFromModel(clazz));
    }

    public static boolean shouldIgnoreClass(Class clazz) {
        if (clazz.isAnnotationPresent(NoExternalUse.class)) {
            return true;
        }
        if (clazz.isAnonymousClass()) {
            return true;
        }
        return false;
    }

    protected GraphQLObjectType.Builder buildGraphQLTypeFromModel(Class clazz) {

        Model<?> model = MODEL_BUILDER.get(clazz);

        GraphQLObjectType.Builder typeBuilder = GraphQLObjectType.newObject();
        typeBuilder.name(ClassUtils.getGraphQLClassName(clazz)).field(makeClassFieldDefinition());

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
                    className = Scalars.GraphQLID;
                } else if (propertyClazz.isArray()) {
                    className = GraphQLList.list(createSchemaClassName(propertyClazz.getComponentType()));
                } else if (Collection.class.isAssignableFrom(propertyClazz)) {
                    className = GraphQLList.list(createSchemaClassName(getCollectionClass(p)));
                } else {
                    className = createSchemaClassName(propertyClazz);
                }

                typeBuilder = typeBuilder.field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(p.name)
                        .type(className)
                        .dataFetcher(dataFetchingEnvironment -> p.getValue(dataFetchingEnvironment.getSource()))
                        .build()
                );
            }
        }
        return typeBuilder;
    }

    @SuppressWarnings("rawtypes")
    public GraphQLSchema buildSchema() {
        GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject().name("QueryType");

        classQueue.addAll(this.TOP_LEVEL_CLASSES);
        classQueue.addAll(this.interfaces);
        classQueue.addAll(this.extraTopLevelClasses);

        while (!classQueue.isEmpty()) {
            Class clazz = classQueue.poll();
            if (clazz == Object.class) { continue; }
            if (clazz == Class.class) { continue; }
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
            for (Class<?> instanceClazz : this.graphQLTypes.keySet()) {
                if (interfaceClazz != instanceClazz && interfaceClazz.isAssignableFrom(instanceClazz)) {
                    this.graphQLTypes.get(instanceClazz).withInterface(GraphQLTypeReference.typeRef(ClassUtils.getGraphQLClassName(interfaceClazz)));
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

        for (Class clazz : TOP_LEVEL_CLASSES) {
            queryType = queryType.field(buildAllQuery(clazz));
        }

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
        return new TypeResolver() {
            @Override
            public GraphQLObjectType getType(TypeResolutionEnvironment env) {
                Class realClazz = ClassUtils.getRealClass(env.getObject().getClass());
                String name = ClassUtils.getGraphQLClassName(realClazz);
                LOGGER.info("Attempting to find: " + name);
                if (env.getSchema().getObjectType(name) != null) {
                    return env.getSchema().getObjectType(name);
                }

                List<GraphQLType> impls = env.getSchema().getTypeMap().values().stream().filter(i -> i instanceof GraphQLObjectType && ((GraphQLObjectType) i).getInterfaces().contains(env.getFieldType())).collect(Collectors.toList());
                for (Class subclassClazz : ClassUtils.getAllSuperClasses(realClazz)) {
                    name = "__" + ClassUtils.getGraphQLClassName(subclassClazz);
                    LOGGER.info("Attempting to find subclass: " + name);
                    GraphQLObjectType objectType = env.getSchema().getObjectType(name);
                    if (objectType != null) {
                        return objectType;
                    }
                }
                for (Class interfaceClazz : ClassUtils.getAllInterfaces(realClazz)) {
                    name = "__" + ClassUtils.getGraphQLClassName(interfaceClazz);
                    LOGGER.info("Attempting to find interface: " + name);
                    GraphQLObjectType objectType = env.getSchema().getObjectType(name);
                    if (objectType != null && objectType.getInterfaces().contains(env.getFieldType())) {
                        return objectType;
                    }
                }
                return null;
            }
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

    public GraphQLFieldDefinition.Builder buildAllQuery( Class<?> clazz) {
        return GraphQLFieldDefinition.newFieldDefinition()
            .name("all" + clazz.getSimpleName() + "s")
            .type(GraphQLList.list(GraphQLTypeReference.typeRef(ClassUtils.getGraphQLClassName(clazz))))
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
            .argument(GraphQLArgument.newArgument()
                .name("type")
                .type(Scalars.GraphQLString)
            )
            .argument(GraphQLArgument.newArgument()
                .name("id")
                .type(Scalars.GraphQLID)
            )
            .dataFetcher(new DataFetcher<Object>() {
                public Iterator<?> slice(Iterable<?> base, int start, int limit) {
                    return Iterators.limit(Iterables.skip(base, start).iterator(), limit);
                }

                @Override
                public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
                    Class _clazz = clazz;
                    int offset = dataFetchingEnvironment.<Integer>getArgument("offset");
                    int limit = dataFetchingEnvironment.<Integer>getArgument("limit");
                    String clazzName = dataFetchingEnvironment.getArgument("type");
                    String id = dataFetchingEnvironment.getArgument("id");

                    if (clazzName != null && !clazzName.isEmpty()) {
                        _clazz = Class.forName(clazzName);
                    }

                    Iterable iterable;
                    if (_clazz == User.class) {
                        if (id != null && !id.isEmpty()) {
                            return Stream.of(User.get(id, false, Collections.emptyMap()))
                                .filter(Objects::nonNull)
                                .toArray();
                        }
                        iterable = User.getAll();
                    } else {
                        if (id != null && !id.isEmpty()) {
                            return Stream.of(Objects.requireNonNull(Jenkins.getInstanceOrNull()).getItemByFullName(id))
                                .filter(Objects::nonNull)
                                .toArray();
                        }

                        iterable = Items.allItems(
                            Jenkins.getAuthentication(),
                            Jenkins.getInstanceOrNull(),
                            _clazz
                        );
                    }
                    return Lists.newArrayList(slice(iterable, offset, limit));
                }
            });
    }

    public void addExtraTopLevelClasses(List<Class> clazzes) {
        this.extraTopLevelClasses.addAll(clazzes);
    }
}
