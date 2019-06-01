package io.jenkins.plugins.io.jenkins.plugins.graphql.types;

import graphql.schema.GraphQLScalarType;
import io.jenkins.plugins.io.jenkins.plugins.graphql.types.scalars.GregrianCalendarScalar;

public class AdditionalScalarTypes {
    private AdditionalScalarTypes() { }

    public static final GraphQLScalarType gregrianCalendarScalar = new GregrianCalendarScalar();
}
