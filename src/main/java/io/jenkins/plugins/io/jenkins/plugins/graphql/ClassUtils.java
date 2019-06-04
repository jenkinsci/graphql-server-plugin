package io.jenkins.plugins.io.jenkins.plugins.graphql;

import org.kohsuke.stapler.export.ModelBuilder;
import org.reflections.Reflections;

import java.util.HashSet;
import java.util.Set;

public class ClassUtils {
    static final String ENHANCER = "$MockitoMock$";

    public static Set<Class<?>> getAllInterfaces(Class instance) {
        Set<Class<?>> interfaces = new HashSet<>();
        for (Class interfaceClazz : instance.getInterfaces()) {
            interfaces.add(interfaceClazz);
            interfaces.addAll(getAllInterfaces(interfaceClazz));
        }
        return interfaces;
    }

    public static Class getRealClass(Class clazz) {
        Class type = clazz;
        while(type.getSimpleName().contains(ClassUtils.ENHANCER)) {
            type = type.getSuperclass();
        }

        return type;
    }

    public static String getGraphQLClassName(Class clazz) {
        return getRealClass(clazz).getSimpleName().replaceAll("[^_0-9A-Za-z]", "_");
    }

    public static Set<Class> findSubclasses(ModelBuilder MODEL_BUILDER, Class clazz) {
        Set<Class> subClasses = new HashSet<>();
        for (Package pkg :  Package.getPackages()) {
            if (pkg.getName().toLowerCase().contains("jenkins") || pkg.getName().toLowerCase().contains("hudson")) {
                Reflections reflections = new Reflections(pkg.getName());
                for (Object subTypeClassObj : reflections.getSubTypesOf(clazz)) {
                    Class subTypeClazz = (Class) subTypeClassObj;
                    try {
                        MODEL_BUILDER.get(subTypeClazz);
                        subClasses.add(subTypeClazz);
                    } catch (org.kohsuke.stapler.export.NotExportableException e) {
                    }
                }
            }
        }
        return subClasses;
    }
}
