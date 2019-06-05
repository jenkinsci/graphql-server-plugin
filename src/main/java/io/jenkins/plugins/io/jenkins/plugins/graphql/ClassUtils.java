package io.jenkins.plugins.io.jenkins.plugins.graphql;

import io.jenkins.plugins.io.jenkins.plugins.graphql.utils.Memoizer;
import org.kohsuke.stapler.export.ModelBuilder;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
        clazz = getRealClass(clazz);
        String name = clazz.getName().replaceAll("\\$[0-9]+$", "");
//        String name = clazz.getSimpleName();
//        if (name.isEmpty()) {
//            String[] parts = clazz.getName().replaceAll("\\$[0-9]+$", "").split("\\.");
//            for (int partNum = parts.length - 1; partNum > 0; partNum--) {
//                name = parts[partNum];
//                if (!name.isEmpty()) { break; }
//            }
//        }
        name = name.replaceAll("[^_0-9A-Za-z]", "_");
//        assert(name.isEmpty());
        return name;
    }

    private static Set<Class> _getAllClassesCache = null;
    private static Set<Class> _getAllClasses() {
        if (_getAllClassesCache != null) { return _getAllClassesCache; }
        _getAllClassesCache = new HashSet<>();

        List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());

        Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setScanners(
                new SubTypesScanner(
                    false /* don't exclude Object.class */
                ),
                new ResourcesScanner()
            )
            .setUrls(
                ClasspathHelper.forClassLoader(
                    classLoadersList.toArray(
                        new ClassLoader[0]
                    )
                )
            )
            .filterInputsBy(
                new FilterBuilder()
                    .includePackage("hudson.model")
                    .includePackage("org.jenkinsci")
                    .includePackage("hudson.plugins")
                    .includePackage("io.jenkins")
                    .includePackage("jenkins.plugins")
                    .includePackage("com.cloudbees")
                    .includePackage("org.jenkins")
            )
        );

        _getAllClassesCache.addAll(reflections.getSubTypesOf(Object.class));
        return _getAllClassesCache;
    }

    public static Set<Class> findSubclasses(ModelBuilder MODEL_BUILDER, Class interfaceClass) {
        Set<Class> subClasses = new HashSet<>();
        for (Class clazz : _getAllClasses()) {
            if (interfaceClass.isAssignableFrom(clazz)) {
                try {
                    MODEL_BUILDER.get(clazz);
                    subClasses.add(clazz);
                } catch (org.kohsuke.stapler.export.NotExportableException e) {
                }
            }
        }
        return subClasses;
    }
}
