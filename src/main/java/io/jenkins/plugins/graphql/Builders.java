package io.jenkins.plugins.graphql;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import graphql.Scalars;
import graphql.TypeResolutionEnvironment;
import graphql.language.FieldDefinition;
import graphql.schema.*;
import graphql.schema.idl.*;
import hudson.model.*;
import hudson.security.WhoAmI;
import io.jenkins.plugins.graphql.types.AdditionalScalarTypes;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Property;
import org.kohsuke.stapler.export.TypeUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Builders {
    private static final Logger LOGGER = Logger.getLogger(Builders.class.getName());
    private static final ModelBuilder MODEL_BUILDER = new ModelBuilder();

    private static final String ARG_OFFSET = "offset";
    private static final String ARG_LIMIT = "limit";
    private static final String ARG_TYPE = "type";
    private static final String ARG_ID = "id";

    private static String makeClassIdDefintion(final Class<?> clazz) {
        final Method idMethod = IdFinder.idMethod(clazz);
        if (idMethod == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("  \"UniqueID\"\n");
        sb.append("  id: ID\n");
        return sb.toString();
        // .dataFetcher(dataFetcher -> idMethod.invoke(dataFetcher.getSource()))
    }

    private static Iterator<?> slice(final Iterable<?> base, final int start, final int limit) {
        return Iterators.limit(Iterables.skip(base, start).iterator(), limit);
    }

    private static final HashMap<String, GraphQLScalarType> javaTypesToGraphqlTypes = new HashMap<>();

    static {
        javaTypesToGraphqlTypes.put("ID", Scalars.GraphQLID);

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

    /*** DONE STATIC */
    private HashMap<Class, Boolean> interfaceTypes = new HashMap();
    private HashMap<Class, String> graphQLTypes = new HashMap();
    private PriorityQueue<Class> classQueue = new PriorityQueue<>(11, Comparator.comparing(Class::getName));
    private List<Class> extraTopLevelClasses = new ArrayList<>();

    protected String createSchemaClassName(final Class clazz) {
        assert (clazz != null);

        if (javaTypesToGraphqlTypes.containsKey(clazz.getSimpleName())) {
            return javaTypesToGraphqlTypes.get(clazz.getSimpleName()).getName();
        }

        final boolean isInterface = Modifier.isInterface(clazz.getModifiers())
                || Modifier.isAbstract(clazz.getModifiers());

        // interfaces are never exported, so handle them seperately
        if (isInterface) {
            classQueue.addAll(ClassUtils.findSubclasses(MODEL_BUILDER, clazz));
        } else {
            try {
                MODEL_BUILDER.get(clazz);
            } catch (final org.kohsuke.stapler.export.NotExportableException e) {
                return Scalars.GraphQLString.getName();
            }
        }
        classQueue.add(clazz);
        return ClassUtils.getGraphQLClassName(clazz);
    }

    private Class getCollectionClass(final Property p) {
        return TypeUtil
                .erasure(TypeUtil.getTypeArgument(TypeUtil.getBaseClass(p.getGenericType(), Collection.class), 0));
    }

    private void buildSchemaFromClass(final Class<?> clazz) {
        if (graphQLTypes.containsKey(clazz)) {
            return;
        }

        if (shouldIgnoreClass(clazz)) {
            return;
        }

        boolean isInterface = Modifier.isInterface(clazz.getModifiers()) || Modifier.isAbstract(clazz.getModifiers());
        try {
            MODEL_BUILDER.get(clazz);
        } catch (final org.kohsuke.stapler.export.NotExportableException e) {
            isInterface = true;
        }

        interfaceTypes.put(clazz, isInterface);
        graphQLTypes.put(clazz, buildGraphQLTypeFromModel(clazz, isInterface));
    }

    static boolean shouldIgnoreClass(final Class clazz) {
        return clazz.isAnnotationPresent(NoExternalUse.class) || clazz.isAnonymousClass();
    }

    @SuppressWarnings("squid:S135")
    String buildGraphQLTypeFromModel(final Class clazz, final boolean isInterface) {
        final Model<?> model = MODEL_BUILDER.getOrNull(clazz, (Class) null, (String) null);
        final Set<String> fields = new HashSet();

        final StringBuilder sb = new StringBuilder();

        final Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance != null) {
            final Descriptor descriptor = instance.getDescriptor(clazz);
            if (descriptor != null) {
                sb.append("\"" + descriptor.getDisplayName() + "\"\n");
            }
        }

        if (isInterface) {
            sb.append("interface ");
        } else {
            sb.append("type ");
        }
        sb.append(ClassUtils.getGraphQLClassName(clazz));
        sb.append("%s {\n");
        sb.append("  \"Class Name\"\n");
        sb.append("  _class: String\n");
        // .dataFetcher(dataFetcher ->
        // ClassUtils.getRealClass(dataFetcher.getSource().getClass()).getName())
        sb.append(makeClassIdDefintion(clazz));

        if (model != null) {
            final ArrayList<Model<?>> queue = new ArrayList<>();
            queue.add(model);

            Model<?> superModel = model.superModel;
            while (superModel != null) {
                queue.add(superModel);
                superModel = superModel.superModel;
            }

            for (final Model<?> _model : queue) {
                for (final Property p : _model.getProperties()) {
                    if (fields.contains(p.name)) {
                        continue;
                    }
                    final Class propertyClazz = p.getType();

                    String className;
                    if ("id".equals(p.name)) {
                        continue; /// we handle it in a different way
                    } else if (propertyClazz.isArray()) {
                        className = "[" + createSchemaClassName(propertyClazz.getComponentType()) + "]";
                    } else if (Collection.class.isAssignableFrom(propertyClazz)) {
                        className = "[" + createSchemaClassName(getCollectionClass(p)) + "]";
                    } else {
                        className = createSchemaClassName(propertyClazz);
                    }

                    // .dataFetcher(dataFetchingEnvironment ->
                    // p.getValue(dataFetchingEnvironment.getSource()));

                    if (StringUtils.isNotEmpty(p.getJavadoc())) {
                        sb.append("  \"\"\"\n");
                        // indent with 2 spaces
                        sb.append(p.getJavadoc().replaceAll("(?m)^", "  "));
                        sb.append("\n  \"\"\"\n");
                    }
                    sb.append("  ");
                    sb.append(p.name);
                    sb.append(": ");
                    sb.append(className);
                    sb.append("\n");

                    /*
                     * if (className instanceof GraphQLList) {
                     * fieldBuilder.dataFetcher(dataFetchingEnvironment -> { final int offset =
                     * dataFetchingEnvironment.<Integer>getArgument(ARG_OFFSET); final int limit =
                     * dataFetchingEnvironment.<Integer>getArgument(ARG_LIMIT); final String id =
                     * dataFetchingEnvironment.getArgument(ARG_ID);
                     *
                     * List<?> valuesList; final Object values =
                     * p.getValue(dataFetchingEnvironment.getSource()); if (values instanceof List)
                     * { valuesList = ((List<?>) values); } else { valuesList =
                     * Arrays.asList((Object[]) values);
                     *
                     * } if (id != null && !id.isEmpty()) { for (final Object value : valuesList) {
                     * final Method method = IdFinder.idMethod(value.getClass()); if (method ==
                     * null) { continue; }
                     *
                     * final String objectId = String.valueOf(method.invoke(value)); if
                     * (id.equals(objectId)) { return Stream.of(value)
                     * .filter(StreamUtils::isAllowed) .toArray(); } } return null; }
                     *
                     * return Lists.newArrayList(slice(valuesList, offset, limit)) .stream()
                     * .filter(StreamUtils::isAllowed) .toArray(); });
                     * fieldBuilder.argument(GraphQLArgument.newArgument() .name(ARG_OFFSET)
                     * .type(Scalars.GraphQLInt) .defaultValue(0) )
                     * .argument(GraphQLArgument.newArgument() .name(ARG_LIMIT)
                     * .type(Scalars.GraphQLInt) .defaultValue(100) )
                     * .argument(GraphQLArgument.newArgument() .name(ARG_TYPE)
                     * .type(Scalars.GraphQLString) ) .argument(GraphQLArgument.newArgument()
                     * .name(ARG_ID) .type(Scalars.GraphQLID) ); }
                     */
                    fields.add(p.name);
                }
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    @SuppressWarnings("rawtypes")
    public GraphQLSchema buildSchema() {
        Pattern typeToInterface = Pattern.compile("^type ", Pattern.MULTILINE);
        final HashMap<String, Property> propertyMap = new HashMap<>();

//        final GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject().name("QueryType");
//
//        queryType.field(buildAllQuery(AbstractItem.class, "allItems"));
//        queryType.field(buildAllQuery(User.class));
//        queryType.field(buildActionQuery(WhoAmI.class));

        classQueue.add(AbstractItem.class);
        classQueue.add(Job.class);
        classQueue.add(User.class);
        classQueue.addAll(this.extraTopLevelClasses);

        while (!classQueue.isEmpty()) {
            final Class clazz = classQueue.poll();
            if (clazz == Object.class || clazz == Class.class) {
                continue;
            }
            this.buildSchemaFromClass(clazz);
        }

        StringBuilder sb = new StringBuilder();
        for (GraphQLScalarType type : new HashSet<GraphQLScalarType>(javaTypesToGraphqlTypes.values())) {
            sb.append("scalar " + type.getName() + "\n");
        }

        sb.append("\n");

        for (Class<?> interfaceClazz : this.graphQLTypes.keySet()) {
            List<String> interfaces = new LinkedList<>();
            if (!interfaceClazz.isInterface()) {
                for (Map.Entry<Class, String> entry1 : this.graphQLTypes.entrySet()) {
                    Class<?> instanceClazz = entry1.getKey();
                    //^(type|instance)
                    if (interfaceClazz == instanceClazz) {
                        continue;
                    }
                    if (instanceClazz.isAssignableFrom(interfaceClazz)) {
                        // if we "implement" it, then its now an interface
                        this.graphQLTypes.put(
                            entry1.getKey(),
                            typeToInterface.matcher(entry1.getValue()).replaceFirst("interface ")
                        );
                        interfaces.add(ClassUtils.getGraphQLClassName(instanceClazz));
                    }
                }
            }
            if (interfaces.size() > 0) {
                this.graphQLTypes.put(
                    interfaceClazz,
                    String.format(
                        this.graphQLTypes.get(interfaceClazz),
                        " implements " + String.join(" & ", interfaces)
                    )
                );
            } else {
                this.graphQLTypes.put(
                    interfaceClazz,
                    String.format(this.graphQLTypes.get(interfaceClazz), "")
                );
            }
        }

        sb.append(
            this.graphQLTypes.values().stream()
                .map( Object::toString )
                .collect( Collectors.joining( "\n\n" ) )
        );

        sb.append("\n");
        sb.append("schema {\n");
        sb.append("  query: QueryType\n");
        sb.append("}\n");

        sb.append("type QueryType {\n");
        sb.append("  allItems(offset: Int, limit: Int, Type: String, ID: ID): [" + ClassUtils.getGraphQLClassName(AbstractItem.class) + "]\n");
        sb.append("  allUsers(offset: Int, limit: Int, Type: String, ID: ID): [" + ClassUtils.getGraphQLClassName(User.class)+ "]\n");
        // sb.append("  whoami: " + ClassUtils.getGraphQLClassName(WhoAmI.class)+ "\n");
        sb.append("}\n");

        try {
            Files.write(sb.toString().getBytes(), Paths.get("./graphql.schema").toFile());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("sdl: " + sb.toString());
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sb.toString());
        RuntimeWiring.Builder runtimeWiring = RuntimeWiring.newRuntimeWiring();
        runtimeWiring.type("QueryType", new UnaryOperator<TypeRuntimeWiring.Builder>() {
            @Override
            public TypeRuntimeWiring.Builder apply(TypeRuntimeWiring.Builder builder) {
                return builder
                    .dataFetcher("allItems", getObjectDataFetcher(AbstractItem.class))
                    .dataFetcher("allUsers", getObjectDataFetcher(User.class));
            }
        });
        runtimeWiring.wiringFactory(new WiringFactory() {
            @Override
            public boolean providesScalar(ScalarWiringEnvironment environment) {
                for (GraphQLScalarType type : javaTypesToGraphqlTypes.values()) {
                    if (environment.getScalarTypeDefinition().getName().equals(type.getName())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public GraphQLScalarType getScalar(ScalarWiringEnvironment environment) {
                for (GraphQLScalarType type : javaTypesToGraphqlTypes.values()) {
                    if (environment.getScalarTypeDefinition().getName().equals(type.getName())) {
                        return type;
                    }
                }
                return null;
            }

            @Override
            public boolean providesTypeResolver(InterfaceWiringEnvironment environment) {
                return true;
            }

            @Override
            public TypeResolver getTypeResolver(InterfaceWiringEnvironment environment) {
                return new TypeResolver() {
                    @Override
                    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
                        Object javaObject = env.getObject();
                        return env.getSchema().getObjectType(ClassUtils.getGraphQLClassName(javaObject.getClass()));
                    }
                };
            }

            @Override
            public boolean providesDataFetcherFactory(FieldWiringEnvironment environment) {
                FieldDefinition fieldDef = environment.getFieldDefinition();
                if ("_class".equals(fieldDef.getName())) {
                    return true;
                }
                // return true;
                String name = environment.getParentType().getName() + "#" + environment.getFieldDefinition().getName();
                return propertyMap.containsKey(name);
            }

            @Override
            public <T> DataFetcherFactory<T> getDataFetcherFactory(FieldWiringEnvironment environment) {
                return environment1 -> new DataFetcher<T>() {
                    @Override
                    public T get(DataFetchingEnvironment environment1) throws Exception {
                        FieldDefinition fieldDef = environment.getFieldDefinition();
                        if ("_class".equals(fieldDef.getName())) {
                            return (T) environment1.getSource().getClass().getName();
                        }
                        String name = environment.getParentType().getName() + "#" + environment.getFieldDefinition().getName();
                        return (T) propertyMap.get(name).getValue(environment1.getSource());
                    }
                };
            }
        });
        SchemaGenerator schemaGenerator = new SchemaGenerator();

        this.graphQLTypes = null;
        this.classQueue = null;
        this.extraTopLevelClasses = null;

        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring.build());

        // private RuntimeWiring buildWiring() {
        //     return RuntimeWiring.newRuntimeWiring()
        //             .type(newTypeWiring("Query")
        //                     .dataFetcher("bookById", graphQLDataFetchers.getBookByIdDataFetcher()))
        //             .type(newTypeWiring("Book")
        //                     .dataFetcher("author", graphQLDataFetchers.getAuthorDataFetcher()))
        //             .build();
        // }
        // return GraphQLSchema.newSchema()
        //     .query(queryType.build())
        //     .codeRegistry(codeRegistry.build())
        //     .additionalTypes(types)
        //     .additionalType(ExtendedScalars.DateTime)
        //     .additionalType(AdditionalScalarTypes.GregrianCalendarScalar)
        //     .build();
    }

    private DataFetcher<Object> getObjectDataFetcher(Class<?> defaultClazz) {
        return dataFetchingEnvironment -> {
            Class clazz = defaultClazz;
            final Jenkins instance = Jenkins.getInstanceOrNull();
            final int offset = dataFetchingEnvironment.<Integer>getArgumentOrDefault(ARG_OFFSET, 0);
            final int limit = dataFetchingEnvironment.<Integer>getArgumentOrDefault(ARG_LIMIT, 100);
            final String clazzName = dataFetchingEnvironment.getArgumentOrDefault(ARG_TYPE, "");
            final String id = dataFetchingEnvironment.getArgumentOrDefault(ARG_ID, null);

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
        };
    }

    private GraphQLFieldDefinition buildActionQuery(final Class<? extends Action> actionClazz) {
        Action action;
        try {
            action = actionClazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }

        return GraphQLFieldDefinition.newFieldDefinition()
            .name(action.getUrlName())
            .type(GraphQLTypeReference.typeRef(createSchemaClassName(actionClazz)))
            .dataFetcher(new StaticDataFetcher(action))
            .build();
    }

    public void addExtraTopLevelClasses(final List<Class> clazzes) {
        this.extraTopLevelClasses.addAll(clazzes);
    }
}
