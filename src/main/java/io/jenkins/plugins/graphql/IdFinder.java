package io.jenkins.plugins.graphql;

import org.reflections8.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

public class IdFinder {
    private IdFinder() { }

    public static Method idMethod(Class<?> clazz) {

        Set<Method> getters = ReflectionUtils.getAllMethods(clazz,
            ReflectionUtils.withModifier(Modifier.PUBLIC), ReflectionUtils.withPrefix("get"));

        for (Method getter : getters) {
            if ("getid".equalsIgnoreCase(getter.getName())) {
                return getter;
            }
            if ("getfullname".equalsIgnoreCase(getter.getName())) {
                return getter;
            }
        }
        return null;
    }
}
