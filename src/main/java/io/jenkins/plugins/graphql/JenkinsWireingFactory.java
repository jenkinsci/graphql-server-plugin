package io.jenkins.plugins.graphql;

import graphql.TypeResolutionEnvironment;
import graphql.language.FieldDefinition;
import graphql.schema.*;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.InterfaceWiringEnvironment;
import graphql.schema.idl.ScalarWiringEnvironment;
import graphql.schema.idl.WiringFactory;
import org.kohsuke.stapler.export.Property;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JenkinsWireingFactory implements WiringFactory {
    private final Map<String, GraphQLScalarType> javaTypesToGraphqlTypes;
    private final Map<String, Property> propertyMap;

    public JenkinsWireingFactory(HashMap<String, GraphQLScalarType> javaTypesToGraphqlTypes, Map<String, Property> propertyMap) {
        this.javaTypesToGraphqlTypes = javaTypesToGraphqlTypes;
        this.propertyMap = propertyMap;
    }

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
        public boolean providesTypeResolver(InterfaceWiringEnvironment
        environment) {
            return true;
        }

        @Override
        public TypeResolver getTypeResolver(InterfaceWiringEnvironment environment) {
            return new TypeResolver() {
                @Override
                public GraphQLObjectType getType(TypeResolutionEnvironment env) {
                    Object javaObject = env.getObject();
                    GraphQLType type = env.getSchema().getType(ClassUtils.getGraphQLClassName(javaObject.getClass()));
                    if (type == null) { return null; }

                    if (type instanceof GraphQLObjectType) {
                        return (GraphQLObjectType) type;
                    }
                    if (type instanceof GraphQLInterfaceType) {
                        List<GraphQLObjectType> implementations = env.getSchema().getImplementations((GraphQLInterfaceType) type);
                        if (implementations.size() == 0) {
                            return null;
                        }
                        return implementations.get(0);
                    }
                    return null;
/*                        Arrays.asList(javaObject.getClass().getInterfaces())
                            .stream()
                            .map(i -> env.getSchema().getType(ClassUtils.getGraphQLClassName(i)))
                            .filter(i -> i != null)
                            .toArray();*/
                }
            };
        }

        @Override
        public boolean providesDataFetcherFactory(FieldWiringEnvironment
        environment) {
            FieldDefinition fieldDef = environment.getFieldDefinition();
            if ("_class".equals(fieldDef.getName())) {
                return true;
            }
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
                    T value = (T) propertyMap.get(name).getValue(environment1.getSource());
                    if (value instanceof Collection) {
                        return (T) ((Collection) value)
                            .stream()
                            .filter(i -> environment1.getGraphQLSchema().getType(ClassUtils.getGraphQLClassName(i.getClass())) != null)
                            .collect(Collectors.toList());
                    }
                    return value;
                }
            };
        }
}
