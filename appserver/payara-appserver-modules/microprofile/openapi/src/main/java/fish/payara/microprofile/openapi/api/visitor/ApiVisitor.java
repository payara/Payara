package fish.payara.microprofile.openapi.api.visitor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Represents an object that can visit components of an API.
 */
public interface ApiVisitor {

    /**
     * @param clazz the class to visit.
     * @param context the current context of the API.
     */
    void visitClass(Class<?> clazz, ApiContext context);

    /**
     * @param method the method to visit.
     * @param context the current context of the API.
     */
    void visitMethod(Method method, ApiContext context);

    /**
     * @param field the field to visit.
     * @param context the current context of the API.
     */
    void visitField(Field field, ApiContext context);

    /**
     * @param parameter the parameter to visit.
     * @param context the current context of the API.
     */
    void visitParameter(Parameter parameter, ApiContext context);
}