package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.schema.*;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.GregorianCalendar;

public class GregrianCalendarScalar extends GraphQLScalarType {
    public GregrianCalendarScalar() {
        super("GregrianCalendar", "An RFC-3339 compliant Full Date Scalar", new Coercing<GregorianCalendar, String>() {
            @Override
            public String serialize(Object input) throws CoercingSerializeException {
                if (input instanceof GregorianCalendar) {
                    return ((GregorianCalendar) input).toZonedDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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
        });
    }
}
