package io.jenkins.plugins.io.jenkins.plugins.graphql;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import graphql.Scalars;
import graphql.TypeResolutionEnvironment;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import hudson.DescriptorExtensionList;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.User;
import io.jenkins.plugins.io.jenkins.plugins.graphql.types.AdditionalScalarTypes;
import jenkins.model.Jenkins;
import jenkins.scm.RunWithSCM;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Property;
import org.kohsuke.stapler.export.TypeUtil;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterators.skip;

public class Builders {
    // private static final Logger LOGGER = Logger.getLogger(Builders.class.getName());
    private static final ModelBuilder MODEL_BUILDER = new ModelBuilder();

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
    private HashMap<Class, GraphQLObjectType.Builder> graphQLTypes = new HashMap();
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
            MODEL_BUILDER.get(clazz);
        } catch (org.kohsuke.stapler.export.NotExportableException e) {
            return Scalars.GraphQLString;
        }
        classQueue.add(clazz);
        GraphQLTypeReference graphQLTypeReference = GraphQLTypeReference.typeRef(clazz.getSimpleName());
        if (graphQLTypeReference != null) {
            return graphQLTypeReference;
        }
        throw new RuntimeException("No such clazz: " + clazz.getSimpleName());
    }

    private Class getCollectionClass(Property p) {
        return TypeUtil.erasure(TypeUtil.getTypeArgument(TypeUtil.getBaseClass(p.getGenericType(), Collection.class), 0));
    }

    public void buildSchemaFromClass(Class clazz) {
        if (graphQLTypes.containsKey(clazz)) {
            return;
        }
        GraphQLObjectType.Builder fieldBuilder = GraphQLObjectType.newObject();

        fieldBuilder.name(clazz.getSimpleName())
            .description("moo")
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name("_class")
                    .description("Class Name")
                    .type(Scalars.GraphQLString)
                    .dataFetcher(dataFetcher -> dataFetcher.getSource().getClass().getSimpleName())
                    .build()
            );

        Model<?> model;
        try {
            model = MODEL_BUILDER.get(clazz);
        } catch (org.kohsuke.stapler.export.NotExportableException e) {
            graphQLTypes.put(clazz, fieldBuilder);
            return;
        }

        for (Class topLevelClazz : INTERFACES) {
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
        graphQLTypes.put(clazz, fieldBuilder);
    }

    @SuppressWarnings("rawtypes")
    public GraphQLSchema buildSchema() {
        GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject().name("QueryType");

        for (Class clazz : Stream.concat(INTERFACES.stream(), TOP_LEVEL_CLASSES.stream()).toArray(Class[]::new)) {
            this.buildSchemaFromClass(clazz);
        }

        for (Class clazz : TOP_LEVEL_CLASSES) {
            queryType = builAllQuery(queryType, clazz);
        }

        for (TopLevelItemDescriptor d : DescriptorExtensionList.lookup(TopLevelItemDescriptor.class)) {
            if (!Modifier.isAbstract(d.clazz.getModifiers())) {
                for (Class topLevelClazz : TOP_LEVEL_CLASSES) {
                    if (d.clazz != topLevelClazz && topLevelClazz.isAssignableFrom(d.clazz)) {
                        classQueue.add(d.clazz);
                        queryType = builAllQuery(queryType, d.clazz);
                    }
                }
            }
        }

        while (!classQueue.isEmpty()) {
            this.buildSchemaFromClass(classQueue.poll());
        }

        HashSet<GraphQLType> types = new HashSet<>();
        GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();

        for (Class clazz : INTERFACES) {
            GraphQLInterfaceType interfaceType = convertToInterface(graphQLTypes.remove(clazz).build());
            types.add(interfaceType);
            codeRegistry.typeResolver(interfaceType.getName(), new TypeResolver() {
                @Override
                public GraphQLObjectType getType(TypeResolutionEnvironment env) {
                    String name = env.getObject().getClass().getSimpleName();
                    for (GraphQLType t : types) {
                        if (t.getName().equals(name)) {
                            return (GraphQLObjectType) t;
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
                public Iterator<?> slice(Iterator<?> base, int start, int limit) {
                    // fast-forward
                    int skipped = skip(base, start);
                    if (skipped < start) { //already at the end, nothing to return
                        Iterators.emptyIterator();
                    }
                    return Iterators.limit(base, limit);
                }

                @Override
                public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
                    int offset = dataFetchingEnvironment.<Integer>getArgument("offset");
                    int limit = dataFetchingEnvironment.<Integer>getArgument("limit");


                    if (clazz == User.class) {
                        return Lists.newArrayList(slice(User.getAll().iterator(), offset, limit));
                    }

                    Iterable<?> items = Items.allItems(
                        Jenkins.getAuthentication(),
                        Jenkins.getInstanceOrNull(),
                        clazz
                    );
                    return Lists.newArrayList(slice(
                        items.iterator(),
                        offset,
                        limit
                    ));
                }
            })
        );
    }
}
