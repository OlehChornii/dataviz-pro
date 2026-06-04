package com.dataviz.di.container;

import com.dataviz.di.annotation.*;
import com.dataviz.di.exception.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class DIContainer {

    private static final Logger LOG = Logger.getLogger(DIContainer.class.getName());

    private record BeanDefinition(
            Class<?>  type,
            String    name,
            boolean   singleton,
            Object    instance
    ) {}

    private final Map<Class<?>, List<BeanDefinition>> registry = new ConcurrentHashMap<>();

    private final Map<String, BeanDefinition> namedRegistry = new ConcurrentHashMap<>();

    private final Map<Class<?>, Object> singletonCache = new ConcurrentHashMap<>();

    private final ThreadLocal<Set<Class<?>>> creationStack =
            ThreadLocal.withInitial(LinkedHashSet::new);

    public <T> DIContainer register(Class<T> implClass) {
        return register(implClass, resolveName(implClass));
    }

    public <T> DIContainer register(Class<T> implClass, String name) {
        Objects.requireNonNull(implClass, "implClass must not be null");
        boolean isSingleton = implClass.isAnnotationPresent(Singleton.class)
                || implClass.isAnnotationPresent(Service.class)
                || implClass.isAnnotationPresent(Repository.class);

        BeanDefinition def = new BeanDefinition(implClass, name, isSingleton, null);

        Set<Class<?>> keys = resolveKeys(implClass);
        for (Class<?> key : keys) {
            registry.computeIfAbsent(key, k -> new ArrayList<>()).add(def);
        }
        namedRegistry.put(name, def);

        LOG.fine(() -> "Registered: %s as '%s' (singleton=%b)".formatted(implClass.getSimpleName(), name, isSingleton));
        return this;
    }

    public <T> DIContainer registerInstance(Class<T> type, T instance) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(instance);
        String name = resolveName(instance.getClass());
        BeanDefinition def = new BeanDefinition(type, name, true, instance);
        registry.computeIfAbsent(type, k -> new ArrayList<>()).add(def);
        namedRegistry.put(name, def);
        singletonCache.put(type, instance);
        LOG.fine(() -> "Registered instance: " + type.getSimpleName());
        return this;
    }

    public <T> T resolve(Class<T> type) {
        List<BeanDefinition> defs = registry.get(type);
        if (defs == null || defs.isEmpty()) {
            throw new DIException("No component registered for: " + type.getName());
        }
        if (defs.size() > 1) {
            throw new DIException(
                    "Multiple components registered for: %s. Use @Named to disambiguate."
                            .formatted(type.getName()));
        }
        return createOrGet(defs.get(0));
    }

    @SuppressWarnings("unchecked")
    public <T> T resolveNamed(String name, Class<T> type) {
        BeanDefinition def = namedRegistry.get(name);
        if (def == null) {
            throw new DIException("No component with name: " + name);
        }
        return type.cast(createOrGet(def));
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> resolveAll(Class<T> type) {
        List<BeanDefinition> defs = registry.getOrDefault(type, List.of());
        return defs.stream()
                .map(d -> (T) createOrGet(d))
                .toList();
    }

    public void shutdown() {
        LOG.info("DIContainer shutdown initiated");
        singletonCache.values().forEach(instance -> {
            for (Method m : instance.getClass().getDeclaredMethods()) {
                if (m.isAnnotationPresent(PreDestroy.class)) {
                    try {
                        m.setAccessible(true);
                        m.invoke(instance);
                        LOG.fine(() -> "@PreDestroy: " + m.getName() + " on " + instance.getClass().getSimpleName());
                    } catch (Exception e) {
                        LOG.severe("@PreDestroy error: " + e.getMessage());
                    }
                }
            }
        });
        singletonCache.clear();
        LOG.info("DIContainer shutdown complete");
    }

    @SuppressWarnings("unchecked")
    private <T> T createOrGet(BeanDefinition def) {
        if (def.instance() != null) {
            return (T) def.instance();
        }

        if (def.singleton()) {
            Object existing = singletonCache.get(def.type());
            if (existing != null) {
                return (T) existing;
            }

            Object created = createInstance(def);

            Object prev = singletonCache.putIfAbsent(def.type(), created);
            return (T) (prev != null ? prev : created);
        }

        return (T) createInstance(def);
    }

    private Object createInstance(BeanDefinition def) {
        Class<?> type = def.type();

        Set<Class<?>> stack = creationStack.get();
        if (!stack.add(type)) {
            throw new CircularDependencyException(
                    "Circular dependency detected: " + stack + " -> " + type.getSimpleName());
        }

        try {
            Object instance = instantiate(type);
            injectFields(instance);
            injectSetters(instance);
            invokePostConstruct(instance);
            return instance;
        } finally {
            stack.remove(type);
            if (stack.isEmpty()) creationStack.remove();
        }
    }

    private Object instantiate(Class<?> type) {
        for (Constructor<?> ctor : type.getDeclaredConstructors()) {
            if (ctor.isAnnotationPresent(Inject.class)) {
                return invokeConstructor(ctor);
            }
        }
        try {
            Constructor<?> noArg = type.getDeclaredConstructor();
            noArg.setAccessible(true);
            return noArg.newInstance();
        } catch (NoSuchMethodException e) {
            throw new DIException(
                    "No @Inject constructor or no-arg constructor found for: " + type.getName());
        } catch (Exception e) {
            throw new DIException("Failed to instantiate: " + type.getName(), e);
        }
    }

    private Object invokeConstructor(Constructor<?> ctor) {
        ctor.setAccessible(true);
        Parameter[] params = ctor.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            args[i] = resolveParameter(params[i]);
        }
        try {
            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new DIException("Constructor injection failed for: " + ctor.getDeclaringClass().getName(), e);
        }
    }

    private void injectFields(Object instance) {
        Class<?> currentType = instance.getClass();
        while (currentType != null && currentType != Object.class) {
            for (Field field : currentType.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Inject.class)) continue;

                field.setAccessible(true);
                Object dep = resolveField(field);
                try {
                    field.set(instance, dep);
                    final Class<?> logType = currentType;
                    final Field logField = field;
                    LOG.fine(() -> "Field inject: %s.%s".formatted(logType.getSimpleName(), logField.getName()));
                } catch (IllegalAccessException e) {
                    throw new DIException("Field injection failed: " + field.getName(), e);
                }
            }
            currentType = currentType.getSuperclass();
        }
    }

    private void injectSetters(Object instance) {
        for (Method method : instance.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Inject.class)) continue;
            if (method.getParameterCount() == 0) continue;
            method.setAccessible(true);
            Parameter[] params = method.getParameters();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                args[i] = resolveParameter(params[i]);
            }
            try {
                method.invoke(instance, args);
                LOG.fine(() -> "Setter inject: " + method.getName());
            } catch (Exception e) {
                throw new DIException("Setter injection failed: " + method.getName(), e);
            }
        }
    }

    private Object resolveField(Field field) {
        Named named = field.getAnnotation(Named.class);
        Inject inject = field.getAnnotation(Inject.class);
        try {
            if (named != null) return resolveNamed(named.value(), field.getType());
            return resolve(field.getType());
        } catch (DIException e) {
            if (!inject.required()) return null;
            throw e;
        }
    }

    private Object resolveParameter(Parameter param) {
        Named named = param.getAnnotation(Named.class);
        if (named != null) return resolveNamed(named.value(), param.getType());

        if (param.getType() == List.class) {
            Type generic = param.getParameterizedType();
            if (generic instanceof ParameterizedType pt) {
                Type typeArg = pt.getActualTypeArguments()[0];
                if (typeArg instanceof Class<?> elementType) {
                    return resolveAll(elementType);
                }
            }
            return new ArrayList<>();
        }

        return resolve(param.getType());
    }

    private void invokePostConstruct(Object instance) {
        for (Method m : instance.getClass().getDeclaredMethods()) {
            if (m.isAnnotationPresent(PostConstruct.class)) {
                m.setAccessible(true);
                try {
                    m.invoke(instance);
                    LOG.fine(() -> "@PostConstruct: " + m.getName());
                } catch (Exception e) {
                    throw new DIException("@PostConstruct failed: " + m.getName(), e);
                }
            }
        }
    }

    private Set<Class<?>> resolveKeys(Class<?> type) {
        Set<Class<?>> keys = new LinkedHashSet<>();
        keys.add(type);
        keys.addAll(Arrays.asList(type.getInterfaces()));
        Class<?> sup = type.getSuperclass();
        while (sup != null && sup != Object.class) {
            keys.add(sup);
            keys.addAll(Arrays.asList(sup.getInterfaces()));
            sup = sup.getSuperclass();
        }
        return keys;
    }

    private String resolveName(Class<?> type) {
        if (type.isAnnotationPresent(Component.class)) {
            String v = type.getAnnotation(Component.class).value();
            if (!v.isEmpty()) return v;
        }
        if (type.isAnnotationPresent(Service.class)) {
            String v = type.getAnnotation(Service.class).value();
            if (!v.isEmpty()) return v;
        }
        if (type.isAnnotationPresent(Repository.class)) {
            String v = type.getAnnotation(Repository.class).value();
            if (!v.isEmpty()) return v;
        }
        String name = type.getSimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}