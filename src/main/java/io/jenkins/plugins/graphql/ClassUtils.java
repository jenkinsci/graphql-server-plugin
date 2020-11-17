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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClassUtils {
    private static final Logger LOGGER = Logger.getLogger(ClassUtils.class.getName());
    static final String ENHANCER = "$MockitoMock$";

    private ClassUtils() {}

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

    public static String getGraphQLClassName(Class<?> clazz) {
        clazz = getRealClass(clazz);
        String name = clazz.getName().replaceAll("\\$[0-9]+$", "");
        name = name.replaceAll("[^_0-9A-Za-z]", "_");
        return name;
    }

    private static Set<Class<?>> getAllClassesCache = null;

    @VisibleForTesting
    public static synchronized  void setAllClassesCache(Set<Class<?>> data) {
        getAllClassesCache = data;
    }
    private static Set<Class<?>> getAllClasses() {
        if (getAllClassesCache != null) { return getAllClassesCache; }
        setAllClassesCache(new HashSet<>());

        List<ClassLoader> classLoadersList = new LinkedList<>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance != null) {
            classLoadersList.addAll(
                instance.getPluginManager().getPlugins()
                    .stream()
                    .map(i -> i.classLoader)
                    .collect(Collectors.toList())
            );
        }

        try {
            final Field f = ClassLoader.class.getDeclaredField("classes");
            boolean oldAccessible = f.isAccessible();
            f.setAccessible(true);
            for (ClassLoader classLoader : classLoadersList) {
                ArrayList<Class<?>> classes =  new ArrayList<>(
                    (Vector<Class<?>>) f.get(classLoader)
                );
                getAllClassesCache.addAll(
                    classes
                        .stream()
                        .filter( clazz -> clazz.getName().toLowerCase().contains("jenkins") || clazz.getName().toLowerCase().contains("hudson"))
                        .collect(Collectors.toList())
                );
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
                    .excludePackage("io.jenkins.cli.shaded")
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

        getAllClassesCache.addAll(reflections.getSubTypesOf(Object.class));
        return getAllClassesCache;
    }

    public static Set<Class<?>> findSubclasses(ModelBuilder modelBuilder, Class<?> interfaceClass) {
        Set<Class<?>> subClasses = new HashSet<>();
        for (Class<?> clazz : getAllClasses()) {
            if (interfaceClass.isAssignableFrom(clazz)) {
                try {
                    modelBuilder.get(clazz);
                    subClasses.add(clazz);
                } catch (org.kohsuke.stapler.export.NotExportableException e) {
                    // there's no model/export data
                    // so skip this class
                }
            }
        }
        return subClasses;
    }
}
