package fish.payara.microprofile.openapi.impl.model.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the aggregated annotations on a type, its fields and methods including annotations "inherited" from
 * super-classes and implemented interfaces.
 */
public final class AnnotationInfo<T> {

    private static final Map<Class<?>, AnnotationInfo<?>> TYPES = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> AnnotationInfo<T> valueOf(Class<T> type) {
        return (AnnotationInfo<T>) TYPES.computeIfAbsent(type, key -> new AnnotationInfo<>(key));
    }

    private final Class<T> type;
    private final Map<Class<? extends Annotation>, Annotation> typeAnnotations = new ConcurrentHashMap<>();
    private final Map<String, Map<Class<? extends Annotation>, Annotation>> fieldAnnotations = new ConcurrentHashMap<>();
    private final Map<String, Map<Class<? extends Annotation>, Annotation>> methodAnnotations = new ConcurrentHashMap<>();
    private final Map<String, Map<Class<? extends Annotation>, Annotation>> methodParameterAnnotations = new ConcurrentHashMap<>();

    private AnnotationInfo(Class<T> type) {
        this.type = type;
        init(type);
    }

    public Class<T> getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return (A) typeAnnotations.get(annotationType);
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationType, Field field) {
        return (A) fieldAnnotations.get(field.getName()).get(annotationType);
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationType, Method method) {
        return (A) methodAnnotations.get(getSignature(method)).get(annotationType);
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationType, Parameter parameter) {
        return (A) methodParameterAnnotations.get(getIdentifier(parameter)).get(annotationType);
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationType, AnnotatedElement element) {
        Class<?> kind = element.getClass();
        if (kind == Class.class) {
            return getAnnotation(annotationType);
        }
        if (kind == Field.class) {
            return getAnnotation(annotationType, (Field) element);
        }
        if (kind == Method.class) {
            return getAnnotation(annotationType, (Method) element);
        }
        if (kind == Parameter.class) {
            return getAnnotation(annotationType, (Parameter) element);
        }
        return null;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType, Field field) {
        return getAnnotation(annotationType, field) != null;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType, Method method) {
        return getAnnotation(annotationType, method) != null;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType, Parameter parameter) {
        return getAnnotation(annotationType, parameter) != null;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType, AnnotatedElement element) {
        return getAnnotation(annotationType, element) != null;
    }

    @SafeVarargs
    public final boolean anyAnnotationPresent(AnnotatedElement element, Class<? extends Annotation>... annotationTypes) {
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            if (isAnnotationPresent(annotationType, element)) {
                return true;
            }
        }
        return false;
    }

    public int getAnnotationCount(Parameter parameter) {
        return methodParameterAnnotations.get(getIdentifier(parameter)).size();
    }

    private void init(Class<?> type) {
        // recurse first so that re-stated annotations "override"
        Class<?> supertype = type.getSuperclass();
        if (supertype != null && supertype != Object.class) {
            init(supertype);
        }
        for (Class<?> implementedInterface : type.getInterfaces()) {
            init(implementedInterface);
        }
        // collect annotations
        putAll(type.getDeclaredAnnotations(), typeAnnotations);
        for (Field field : type.getDeclaredFields()) {
            putAll(field.getDeclaredAnnotations(), 
                    fieldAnnotations.computeIfAbsent(field.getName(), key -> new ConcurrentHashMap<>()));
        }
        for (Method method : type.getDeclaredMethods()) {
            putAll(method.getDeclaredAnnotations(),
                methodAnnotations.computeIfAbsent(getSignature(method), key -> new ConcurrentHashMap<>()));
            for (Parameter parameter : method.getParameters()) {
                putAll(parameter.getDeclaredAnnotations(), 
                        methodParameterAnnotations.computeIfAbsent(getIdentifier(parameter), key -> new ConcurrentHashMap<>()));
            }
        }
    }

    private static void putAll(Annotation[] annotations, Map<Class<? extends Annotation>, Annotation> map) {
        for (Annotation a : annotations) {
            map.put(a.annotationType(), a);
        }
    }

    private static String getIdentifier(Parameter parameter) {
        try {
            java.lang.reflect.Field index = parameter.getClass().getDeclaredField("index");
            index.setAccessible(true);
            return getSignature(parameter.getDeclaringExecutable()) + "#" + index.getInt(parameter);
        } catch (Exception e) {
            return getSignature(parameter.getDeclaringExecutable()) + "#" + parameter.toString();
        }
    }

    private static String getSignature(Executable method) {
        StringBuilder signature = new StringBuilder();
        signature.append(method.getName());
        signature.append('(');
        for (Class<?> parameterType : method.getParameterTypes()) {
            signature.append(parameterType.getName()).append(", ");
        }
        signature.append(')');
        return signature.toString();
    }
}