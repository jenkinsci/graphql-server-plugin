package io.jenkins.plugins.graphql;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphqlTypeBuilder;

public class GraphqlTypeBuilderWrapper {
    private final GraphqlTypeBuilder builder;
    private String name;

    public GraphqlTypeBuilderWrapper(GraphqlTypeBuilder builder) {
        this.builder = builder;
    }

    public void description(String description) {
        if (this.builder instanceof GraphQLObjectType.Builder) {
            ((GraphQLObjectType.Builder) this.builder).description(description);
        }
        if (this.builder instanceof GraphQLInterfaceType.Builder) {
            ((GraphQLInterfaceType.Builder) this.builder).description(description);
        }
    }

    public void name(String name) {
        this.name = name;
        if (this.builder instanceof GraphQLObjectType.Builder) {
            ((GraphQLObjectType.Builder) this.builder).name(name);
        }
        if (this.builder instanceof GraphQLInterfaceType.Builder) {
            ((GraphQLInterfaceType.Builder) this.builder).name(name);
        }
    }

    public void field(GraphQLFieldDefinition fields) {
        if (this.builder instanceof GraphQLObjectType.Builder) {
            ((GraphQLObjectType.Builder) this.builder).field(fields);
        }
        if (this.builder instanceof GraphQLInterfaceType.Builder) {
            ((GraphQLInterfaceType.Builder) this.builder).field(fields);
        }
    }

    public boolean hasField(String name) {
        if (this.builder instanceof GraphQLObjectType.Builder) {
            return ((GraphQLObjectType.Builder) this.builder).hasField(name);
        }
        if (this.builder instanceof GraphQLInterfaceType.Builder) {
            return ((GraphQLInterfaceType.Builder) this.builder).hasField(name);
        }
        throw new RuntimeException("Unknown type: " + this.builder.getClass().getName());
    }

    public GraphqlTypeBuilder getBuilder() {
        return this.builder;
    }

    public void withInterface(GraphQLTypeReference typeRef) {
        if (this.builder instanceof GraphQLObjectType.Builder) {
            ((GraphQLObjectType.Builder) this.builder).withInterface(typeRef);
            return;
        }
        throw new RuntimeException("Unknown type: " + this.builder.getClass().getName());
    }

    public GraphQLType build() {
        if (this.builder instanceof GraphQLObjectType.Builder) {
            return ((GraphQLObjectType.Builder) this.builder).build();
        }
        if (this.builder instanceof GraphQLInterfaceType.Builder) {
            return ((GraphQLInterfaceType.Builder) this.builder).build();
        }
        throw new RuntimeException("Unknown type: " + this.builder.getClass().getName());
    }

    public boolean isInterface() {
        return this.builder instanceof GraphQLInterfaceType.Builder;
    }

    public String getName() {
        return this.name;
    }
}
