package io.jenkins.plugins.graphql;

import com.google.common.annotations.VisibleForTesting;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.export.ModelBuilder;
import org.reflections8.Reflections;
import org.reflections8.scanners.ResourcesScanner;
import org.reflections8.scanners.SubTypesScanner;
import org.reflections8.util.ClasspathHelper;
import org.reflections8.util.ConfigurationBuilder;
import org.reflections8.util.FilterBuilder;

import java.lang.reflect.Field;
import java.util.Vector;
import java.util.logging.Logger;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassUtils {
    private static final Logger LOGGER = Logger.getLogger(ClassUtils.class.getName());
    static final String ENHANCER = "$MockitoMock$";

    public static Set<Class<?>> getAllInterfaces(Class<?> instance) {
        Set<Class<?>> interfaces = new HashSet<>();
        if (instance == null) {
            return interfaces;
        }
        for (Class<?> interfaceClazz : instance.getInterfaces()) {
            interfaces.add(interfaceClazz);
            interfaces.addAll(getAllInterfaces(interfaceClazz));
        }
        interfaces.addAll(getAllInterfaces(instance.getSuperclass()));
        return interfaces;
    }

    public static Set<Class<?>> getAllSuperClasses(Class<?> instance) {
        Set<Class<?>> superclasses = new HashSet<>();
        while (instance.getSuperclass() != null) {
            superclasses.add(instance.getSuperclass());
            instance = instance.getSuperclass();
        }
        return superclasses;
    }


    public static Class<?> getRealClass(Class<?> clazz) {
        Class<?> type = clazz;
        while(type.getSimpleName().contains(ClassUtils.ENHANCER)) {
            type = type.getSuperclass();
        }
        return type;
    }

    // FIXME - memoize
    public static String getGraphQLClassName(Class<?> clazz) {
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

    @VisibleForTesting
    public static Set<Class<?>> _getAllClassesCache = null;

    private static Set<Class<?>> _getAllClasses() {
        if (_getAllClassesCache != null) { return _getAllClassesCache; }
        _getAllClassesCache = new HashSet<>();

        List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());
        if (Jenkins.getInstanceOrNull() != null) {
            classLoadersList.addAll(
                Jenkins.getInstanceOrNull().getPluginManager().getPlugins().stream()
                    .map(i -> i.classLoader).collect(Collectors.toList())
            );
        }

        try {
            final Field f = ClassLoader.class.getDeclaredField("classes");
            boolean oldAccessible = f.isAccessible();
            f.setAccessible(true);
            for (ClassLoader classLoader : classLoadersList) {
                Vector<Class> classes =  (Vector<Class>) f.get(classLoader);
                for (Class clazz : classes) {
                    if (clazz.getName().toLowerCase().contains("jenkins") || clazz.getName().toLowerCase().contains("hudson")) {
                        _getAllClassesCache.add(clazz);
                    }
                }
            }
            f.setAccessible(oldAccessible);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.info("Unable to use classloader, so falling back to reflections: " + e.getMessage());
        }

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
                    .includePackage("com.cloudbees")
                    .includePackage("hudson.model")
                    .includePackage("hudson.plugins")
                    .includePackage("io.jenkins")
                    .includePackage("jenkins.plugins")
                    .includePackage("org.jenkins")
                    .includePackage("org.jenkinsci")
                    .includePackage("org.jenkinsci.plugins.workflow.job")
            )
        );

        _getAllClassesCache.addAll(reflections.getSubTypesOf(Object.class));
        LOGGER.info(
            _getAllClassesCache.stream().filter(i -> i.getName().contains("WorkflowRun")).collect(Collectors.toList()).toString()
        );
        LOGGER.info(
            _getAllClassesCache.stream().filter(i -> i.getName().contains("CauseAction")).collect(Collectors.toList()).toString()
        );
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
