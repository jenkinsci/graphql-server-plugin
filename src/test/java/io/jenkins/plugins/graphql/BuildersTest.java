package io.jenkins.plugins.graphql;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import org.junit.Assert;
import org.junit.Test;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.Arrays;
import java.util.List;

public class BuildersTest {
    @ExportedBean
    private class TestExportedClass {
        @Exported
        public String getString() { return "String"; }
        @Exported
        public String[] getArrayString () { return new String[] { "Hi", "There" }; }
        @Exported
        public List<String> getListString () { return Arrays.asList("Hi", "There"); }
    }

    @Test
    public void buildObjectForArray() {
        Builders builders = new Builders() {
            @Override
            protected String createSchemaClassName(Class clazz) {
                return ClassUtils.getGraphQLClassName(clazz);
            }
        };

        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(
            builders.buildGraphQLTypeFromModel(TestExportedClass.class, false)
        );

        GraphQLObjectType graphQLObjectType = (GraphQLObjectType) typeRegistry.getType("TestExportedClass").get();
        Assert.assertEquals(
            null,
            graphQLObjectType.getDescription()
        );
        Assert.assertEquals(
            "java_lang_String",
            graphQLObjectType.getFieldDefinition("string").getType().toString()
        );
        Assert.assertEquals(
            "[java_lang_String]",
            graphQLObjectType.getFieldDefinition("arrayString").getType().toString()
        );
        Assert.assertEquals(
            "[java_lang_String]",
            graphQLObjectType.getFieldDefinition("listString").getType().toString()
        );
    }

    @Test
    public void confirmShouldIgnore() {
        Assert.assertFalse(Builders.shouldIgnoreClass(hudson.model.Cause.class));
        Assert.assertFalse(Builders.shouldIgnoreClass(hudson.model.Cause.UserIdCause.class));
        Assert.assertFalse(Builders.shouldIgnoreClass(hudson.model.CauseAction.class));
    }
}
