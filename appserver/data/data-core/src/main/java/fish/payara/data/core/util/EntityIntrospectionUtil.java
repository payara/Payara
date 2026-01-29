
package fish.payara.data.core.util;

import jakarta.persistence.Id;
import jakarta.data.exceptions.MappingException;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class EntityIntrospectionUtil {

    // Cache to avoid reflection overload
    private static final Map<Class<?>, Member> idAccessorCache = new ConcurrentHashMap<>();

    /**
     * Finds the accessor (Method or Field) for the ID of an entity class,
     * identified by the @Id annotation. Results are cached for performance.
     */
    public static Member findIdAccessor(Class<?> entityClass) {
        return idAccessorCache.computeIfAbsent(entityClass, key -> {
            // 1. Check methods first
            for (Method method : key.getMethods()) {
                if (method.isAnnotationPresent(Id.class)) {
                    return method;
                }
            }
            // 2. If no method found, check fields
            for (Field field : key.getFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    return field;
                }
            }
            // 3. Check non-public fields as a fallback
            for (Field field : key.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    try {
                        field.setAccessible(true);
                        return field;
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
            throw new MappingException("No @Id annotation found on any method or field for entity class: " + key.getName());
        });
    }

    public static String getIdFieldName(Class<?> entityClass) {
        Member idAccessor = findIdAccessor(entityClass);
        if (idAccessor instanceof Field) {
            return idAccessor.getName();
        } else if (idAccessor instanceof Method) {
            // When the ID accessor is a method, we need to find the actual field
            // that corresponds to this getter method, not derive it from the method name

            // First, try to find the field directly by @Id annotation
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    return field.getName();
                }
            }

            // If no field found with @Id, try to find it in public fields
            for (Field field : entityClass.getFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    return field.getName();
                }
            }

            // Only if we can't find the actual field, fall back to deriving from method name
            String methodName = idAccessor.getName();
            if (methodName.startsWith("get") && methodName.length() > 3) {
                return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
            }
            return methodName;
        }
        throw new MappingException("Cannot determine ID field name for entity class: " + entityClass.getName());
    }
}