package fish.payara.microprofile.config.cdi;

import static org.eclipse.microprofile.config.inject.ConfigProperties.UNCONFIGURED_PREFIX;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.inject.ConfigProperties;

import fish.payara.microprofile.config.cdi.model.ConfigPropertyModel;

public class ConfigPropertiesProducer {

    private static final Logger LOGGER = Logger.getLogger(ConfigPropertiesProducer.class.getName());

    @ConfigProperties
    public static final Object getGenericObject(InjectionPoint ip, BeanManager bm)
            throws InstantiationException, IllegalAccessException {
        Type type = ip.getType();
        if (!(type instanceof Class)) {
            throw new IllegalArgumentException("Unable to process injection point with @ConfigProperties of type " + type);
        }

        // Initialise the object. This may throw exceptions
        final Object object = ((Class) type).newInstance();

        // Model the class
        final AnnotatedType<?> annotatedType = bm.createAnnotatedType((Class) type);

        // Find the @ConfigProperties annotations, and calculate the property prefix
        final ConfigProperties injectionAnnotation = ip.getAnnotated().getAnnotation(ConfigProperties.class);
        final ConfigProperties classAnnotation = annotatedType.getAnnotation(ConfigProperties.class);
        final String prefix = parsePrefixes(injectionAnnotation, classAnnotation);

        for (AnnotatedField<?> field : annotatedType.getFields()) {

            // Find the java field and field name
            final Field javaField = field.getJavaMember();

            // Make sure the field is accessible
            javaField.setAccessible(true);

            // Model the field
            final InjectionPoint fieldInjectionPoint = bm.createInjectionPoint(field);
            final ConfigPropertyModel model = new ConfigPropertyModel(fieldInjectionPoint, prefix);

            try {
                final Object value = ConfigPropertyProducer.getGenericPropertyFromModel(model);

                if (value != null) {
                    javaField.set(object, value);
                }
            } catch (Exception ex) {
                if (javaField.get(object) == null) {
                    LOGGER.log(Level.WARNING, String.format("Unable to inject property with name %s into type %s.",
                            model.getName(), type.getTypeName()), ex);
                    throw ex;
                }
            }
        }

        return object;
    }

    private static String parsePrefixes(ConfigProperties injectionAnnotation, ConfigProperties classAnnotation) {
        final String injectionPrefix = parsePrefix(injectionAnnotation);
        if (injectionPrefix != null) {
            return injectionPrefix;
        }
        return parsePrefix(classAnnotation);
    }

    private static String parsePrefix(ConfigProperties annotation) {
        if (annotation == null) {
            return null;
        }
        final String value = annotation.prefix();
        if (value == null || value.equals(UNCONFIGURED_PREFIX)) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }
        return value + ".";
    }

}
