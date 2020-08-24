package io.jenkins.plugins.graphql;

import graphql.language.ObjectTypeDefinition;
import graphql.language.Type;
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

        String sdl = String.format(builders.buildGraphQLTypeFromModel(TestExportedClass.class, false), "");
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);

        ObjectTypeDefinition graphQLObjectType = (ObjectTypeDefinition) typeRegistry.getType(ClassUtils.getGraphQLClassName(TestExportedClass.class)).get();
        Assert.assertEquals(
            null,
            graphQLObjectType.getDescription()
        );
        Assert.assertEquals(
            "TypeName{name='java_lang_String'}",
            getFieldType(graphQLObjectType, "string").toString()
        );
        Assert.assertEquals(
            "ListType{type=TypeName{name='java_lang_String'}}",
            getFieldType(graphQLObjectType, "arrayString").toString()
        );
        Assert.assertEquals(
            "ListType{type=TypeName{name='java_lang_String'}}",
            getFieldType(graphQLObjectType, "listString").toString()
        );
    }

    private Type getFieldType(ObjectTypeDefinition graphQLObjectType, String key) {
        return graphQLObjectType.getFieldDefinitions().stream().filter(i -> i.getName().equals(key)).findFirst().get().getType();
    }

    @Test
    public void confirmShouldIgnore() {
        Assert.assertFalse(Builders.shouldIgnoreClass(hudson.model.Cause.class));
        Assert.assertFalse(Builders.shouldIgnoreClass(hudson.model.Cause.UserIdCause.class));
        Assert.assertFalse(Builders.shouldIgnoreClass(hudson.model.CauseAction.class));
    }
}
