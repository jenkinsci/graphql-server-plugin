package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.schema.GraphQLScalarType;

public class AdditionalScalarTypes {
    public static GraphQLScalarType gregrianCalendarScalar = new GregrianCalendarScalar();
}
