package io.jenkins.plugins.io.jenkins.plugins.graphql;

import io.jenkins.plugins.io.jenkins.plugins.graphql.utils.Memoizer;
import org.kohsuke.stapler.export.ModelBuilder;
import org.reflections.Reflections;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ClassUtils {
    static final String ENHANCER = "$MockitoMock$";

    // FIXME - memoize
    public static Set<Class<?>> getAllInterfaces(Class instance) {
        Set<Class<?>> interfaces = new HashSet<>();
        if (instance == null) {
            return interfaces;
        }
        for (Class interfaceClazz : instance.getInterfaces()) {
            interfaces.add(interfaceClazz);
            interfaces.addAll(getAllInterfaces(interfaceClazz));
        }
        interfaces.addAll(getAllInterfaces(instance.getSuperclass()));
        return interfaces;
    }

    // FIXME - memoize (maybe)
    public static Class getRealClass(Class clazz) {
        Class type = clazz;
        while(type.getSimpleName().contains(ClassUtils.ENHANCER)) {
            type = type.getSuperclass();
        }
        return type;
    }

    // FIXME - memoize
    public static String getGraphQLClassName(Class clazz) {
        return getRealClass(clazz).getSimpleName().replaceAll("[^_0-9A-Za-z]", "_");
    }

    private static Set<Class> _getAllClassesCache = null;
    private static Set<Class> _getAllClasses() {
        if (_getAllClassesCache != null) { return _getAllClassesCache; }
        _getAllClassesCache = new HashSet<>();

        for (Package pkg :  Package.getPackages()) {
            if (pkg.getName().toLowerCase().contains("jenkins") || pkg.getName().toLowerCase().contains("hudson")) {
                Reflections reflections = new Reflections(pkg.getName());
                _getAllClassesCache.addAll( reflections.getSubTypesOf(Object.class));
            }
        }
        return _getAllClassesCache;
    }

    public static Set<Class> findSubclasses(ModelBuilder MODEL_BUILDER, Class clazz) {
        Set<Class> subClasses = new HashSet<>();
        for (Class subTypeClazz : _getAllClasses()) {
            if (subTypeClazz.isAssignableFrom(clazz)) {
                try {
                    MODEL_BUILDER.get(subTypeClazz);
                    subClasses.add(subTypeClazz);
                } catch (org.kohsuke.stapler.export.NotExportableException e) {
                }
            }
        }
        return subClasses;
    }
}
