package io.jenkins.plugins.io.jenkins.plugins.graphql;

import graphql.language.FieldDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactory;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.WiringFactory;

class ClassWiringFactory implements WiringFactory {
    @Override
    public boolean providesDataFetcherFactory(FieldWiringEnvironment environment) {
        FieldDefinition fieldDef = environment.getFieldDefinition();
        if ("_class".equals(fieldDef.getName())) {
            return true;
        }
        return false;
    }

    @Override
    public <T> DataFetcherFactory<T> getDataFetcherFactory(FieldWiringEnvironment environment) {
        return environment1 -> new DataFetcher<T>() {
            @Override
            public T get(DataFetchingEnvironment environment1) throws Exception {
                return (T) environment1.getSource().getClass().getName();
            }
        };
    }
}
