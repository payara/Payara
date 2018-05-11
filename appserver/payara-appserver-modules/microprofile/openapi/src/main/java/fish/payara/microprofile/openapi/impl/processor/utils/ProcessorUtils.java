package fish.payara.microprofile.openapi.impl.processor.utils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProcessorUtils {

    /**
     * Private constructor to hide implicit public one.
     */
    private ProcessorUtils() {

    }

    private static final Logger LOGGER = Logger.getLogger(ProcessorUtils.class.getName());

    /**
     * Gets the set of classes contained within a {@link ClassLoader}. The set
     * returned will not be null, but could be empty.
     * 
     * @param classLoader the classloader to get the classes from.
     * @return the set of classes managed by the classloader.
     */
    @SuppressWarnings("unchecked")
    public static Set<Class<?>> getClassesFromLoader(ClassLoader classLoader) {
        Set<Class<?>> classes = new HashSet<>();
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            classes = new HashSet<>((Vector<Class<?>>) classesField.get(classLoader));
            classesField.setAccessible(false);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Unable to get classes from classloader.", ex);
        }
        return classes;
    }
}