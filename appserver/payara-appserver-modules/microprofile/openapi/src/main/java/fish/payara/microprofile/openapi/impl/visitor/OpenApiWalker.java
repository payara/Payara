package fish.payara.microprofile.openapi.impl.visitor;

import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import fish.payara.microprofile.openapi.api.visitor.ApiVisitor;
import fish.payara.microprofile.openapi.api.visitor.ApiWalker;

public class OpenApiWalker implements ApiWalker {

    private final OpenAPI api;
    private final Set<Class<?>> classes;
    private final Map<String, Set<Class<?>>> resourceMapping;

    public OpenApiWalker(OpenAPI api, Set<Class<?>> classes) {
        this.api = api;

        this.resourceMapping = new HashMap<>();
        generateResourceMapping(classes);

        this.classes = new TreeSet<>(new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> class1, Class<?> class2) {
                if (class1.equals(class2)) {
                    return 0;
                }
                // Non contextual objects at the start
                if (!class1.isAnnotationPresent(ApplicationPath.class) && !class1.isAnnotationPresent(Path.class)) {
                    return -1;
                }
                // Followed by applications
                if (class1.isAnnotationPresent(ApplicationPath.class)) {
                    return -1;
                }
                // Followed by everything else
                return 1;
            }
        });
        this.classes.addAll(classes);
    }

    @Override
    public void accept(ApiVisitor visitor) {
        for (Class<?> clazz : classes) {

            // Visit each class
            String path = getResourcePath(clazz);
            visitor.visitClass(clazz, new OpenApiContext(api, path));

            for (Method method : clazz.getDeclaredMethods()) {

                // Visit each method
                path = getResourcePath(method);
                method.setAccessible(true);
                visitor.visitMethod(method, new OpenApiContext(api, path));

                // Visit each parameter
                for (Parameter parameter : method.getParameters()) {
                    visitor.visitParameter(parameter, new OpenApiContext(api, path));
                }
            }

            // Visit each field
            for (Field field : clazz.getDeclaredFields()) {

                field.setAccessible(true);
                visitor.visitField(field, new OpenApiContext(api, path));
            }
        }
    }

    private String getResourcePath(GenericDeclaration declaration) {
        if (declaration instanceof Method) {
            Method method = (Method) declaration;

            // If the method is a valid resource
            if (method.isAnnotationPresent(GET.class) || method.isAnnotationPresent(POST.class)
                    || method.isAnnotationPresent(PUT.class) || method.isAnnotationPresent(DELETE.class)
                    || method.isAnnotationPresent(HEAD.class) || method.isAnnotationPresent(OPTIONS.class)
                    || method.isAnnotationPresent(PATCH.class)) {
                if (method.isAnnotationPresent(Path.class)) {
                    return getResourcePath(method.getDeclaringClass())
                            + method.getDeclaredAnnotation(Path.class).value();
                } else {
                    return getResourcePath(method.getDeclaringClass());
                }
            }
        }
        if (declaration instanceof Class) {
            Class<?> clazz = (Class<?>) declaration;

            // If the class is a resource and contains a mapping
            if (clazz.isAnnotationPresent(Path.class)) {
                for (String key : resourceMapping.keySet()) {
                    if (resourceMapping.get(key).contains(clazz)) {
                        return key + clazz.getDeclaredAnnotation(Path.class).value();
                    }
                }
            }
        }
        return null;
    }

    private void generateResourceMapping(Set<Class<?>> classList) {
        for (Class<?> clazz : classList) {
            if (clazz.isAnnotationPresent(ApplicationPath.class) && Application.class.isAssignableFrom(clazz)) {
                // Produce the mapping
                String key = clazz.getDeclaredAnnotation(ApplicationPath.class).value();
                Set<Class<?>> resourceClasses = new HashSet<>();
                resourceMapping.put(key, resourceClasses);

                try {
                    Application app = (Application) clazz.newInstance();
                    // Add all classes contained in the application
                    resourceClasses.addAll(app.getClasses());
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        // If there is one application and it's empty, add all classes
        if (resourceMapping.keySet().size() == 1) {
            Set<Class<?>> classes = resourceMapping.values().iterator().next();
            if (classes.isEmpty()) {
                classes.addAll(classList);
            }
        }
    }

}