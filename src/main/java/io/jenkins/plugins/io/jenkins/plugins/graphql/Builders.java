package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.Scalars;
import graphql.schema.*;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Property;
import org.kohsuke.stapler.export.TypeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Builders {
    private final static Logger LOGGER = Logger.getLogger(GraphQLSchemaGenerator.class.getName());
    private static final ModelBuilder MODEL_BUILDER = new ModelBuilder();



    private final HashMap<String, GraphQLOutputType> javaTypesToGraphqlTypes = new HashMap<>();

    public Builders() {
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

    private GraphQLOutputType createSchemaClassName(Class clazz) {
        if (javaTypesToGraphqlTypes.containsKey(clazz.getSimpleName())) {
            return javaTypesToGraphqlTypes.get(clazz.getSimpleName());
        }
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
        Model<?> model;
        try {
            model = MODEL_BUILDER.get(clazz);
        } catch (org.kohsuke.stapler.export.NotExportableException e) {
            return null;
        }

        ArrayList<Model<?>> queue = new ArrayList();
        queue.add(model);

        Model<?> superModel = model.superModel;
        while (superModel != null) {
            queue.add(superModel);
            superModel = superModel.superModel;
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

        for (Model<?>_model : queue) {
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

                fieldBuilder.field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(p.name)
                        .type(className)
                        .build()
                );
            }
        }

        GraphQLObjectType fooType =  fieldBuilder.build();
        LOGGER.log(Level.WARNING, fooType.toString());
        return fooType;
    }
}
