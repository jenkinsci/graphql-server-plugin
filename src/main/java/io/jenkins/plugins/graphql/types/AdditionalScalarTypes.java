package io.jenkins.plugins.graphql.types;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import io.jenkins.plugins.graphql.types.scalars.GregrianCalendarScalar;

public class AdditionalScalarTypes {
    private AdditionalScalarTypes() { }

    public static final GraphQLScalarType gregrianCalendarScalar = new GregrianCalendarScalar();

    static final public GraphQLScalarType CLASS_SCALAR = GraphQLScalarType.newScalar().name("Class").description("Class").coercing(new Coercing() {
        @Override
        public String serialize(Object input) throws CoercingSerializeException {
            if (input instanceof Class) {
                return ((Class) input).getSimpleName();
            }
            throw new CoercingSerializeException(
                "Expected something we can convert to 'java.time.Class' but was '" + input.getClass().toString() + "'."
            );
        }

        @Override
        public Class parseValue(Object input) throws CoercingParseValueException {
            try {
                return Class.forName((String) input);
            } catch (ClassNotFoundException e) {
                throw new CoercingSerializeException(
                    "Expected a class we can look up."
                );
            }
        }

        @Override
        public Class parseLiteral(Object input) throws CoercingParseLiteralException {
            return this.parseValue(input);
        }
    }).build();
}
