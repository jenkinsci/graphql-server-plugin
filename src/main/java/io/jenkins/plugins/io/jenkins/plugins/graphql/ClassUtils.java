package io.jenkins.plugins.io.jenkins.plugins.graphql;

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

    public static Class<? extends Object> getRealClass(Object instance) {
        Class<? extends Object> type = instance.getClass();
        while(type.getSimpleName().contains(ClassUtils.ENHANCER)) {
            type = type.getSuperclass();
        }

        return type;
    }
}
