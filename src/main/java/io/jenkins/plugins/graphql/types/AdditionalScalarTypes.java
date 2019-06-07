package io.jenkins.plugins.graphql.types;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.GregorianCalendar;

public class AdditionalScalarTypes {
    private AdditionalScalarTypes() {
    }

    static final public GraphQLScalarType ClassScalar = GraphQLScalarType.newScalar().name("Class").description("Class").coercing(new Coercing() {
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

    static final public GraphQLScalarType GregrianCalendarScalar = GraphQLScalarType.newScalar()
        .name("GregrianCalendar")
        .description("An RFC-3339 compliant Full Date Scalar")
        .coercing(new Coercing<GregorianCalendar, String>() {
            @Override
            public String serialize(Object input) throws CoercingSerializeException {
                if (input instanceof GregorianCalendar) {
                    return ((GregorianCalendar) input).toZonedDateTime().withZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
                throw new CoercingSerializeException(
                    "Expected something we can convert to 'java.time.GregorianCalendar' but was '" + input.getClass().toString() + "'."
                );
            }

            @Override
            public GregorianCalendar parseValue(Object input) throws CoercingParseValueException {
                if (input instanceof String) {
                    TemporalAccessor dt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse((String) input);
                    GregorianCalendar calendar = new GregorianCalendar();
                    calendar.setTime((Date) dt);
                    return calendar;
                }
                throw new CoercingSerializeException(
                    "Expected a 'String' but was '" + input.getClass().toString() + "'."
                );
            }

            @Override
            public GregorianCalendar parseLiteral(Object input) throws CoercingParseLiteralException {
                return this.parseValue(input);
            }
        }).build();
}
