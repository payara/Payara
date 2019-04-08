package fish.payara.microprofile.faulttolerance.model;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

public abstract class Policy implements Serializable {

    public static void checkAtLeast(long minimum, Method annotatedMethod, Class<? extends Annotation> annotationType,
            String attribute, long value) {
        if (value < minimum) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute)
                     + "value less than " + minimum + ": " + value);
        }
    }

    public static void checkAtLeast(double minimum, Method annotatedMethod, Class<? extends Annotation> annotationType,
            String attribute, double value) {
        if (value < minimum) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute)
                    + "value less than " + minimum + ": " + value);
        }
    }

    public static void checkAtLeast(String attribute1, long minimum, Method annotatedMethod,
            Class<? extends Annotation> annotationType, String attribute2, long value) {
        if (value < minimum) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute2)
                    + "value less than or equal to the " + attribute(attribute1) + "value: " + value);
        }
    }

    public static void checkAtMost(double maximum, Method annotatedMethod, Class<? extends Annotation> annotationType,
            String attribute, double value) {
        if (value > maximum) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute)
                    + "value greater than " + maximum + ": " + value);
        }
    }

    public static void checkReturnsSameAs(Method annotatedMethod, Class<? extends Annotation> annotationType,
            String attribute, Class<?> valueType, String valueMethodName, Class<?>... valueParameterTypes) {
        try {
            Method actual = valueType.getDeclaredMethod(valueMethodName, valueParameterTypes);
            if (actual.getReturnType() != annotatedMethod.getReturnType()) {
                throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute)
                        + "whose return type of does not match.");
            }
        } catch (NoSuchMethodException ex) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute)
                    + "refering to a method "+valueMethodName+" that does not exist for type: " + valueType.getName(), ex);
        }
    }

    protected static String describe(Method annotatedMethod, Class<? extends Annotation> annotationType, String attribute) {
        return "Method \"" + annotatedMethod.getName() + "\" in " + annotatedMethod.getDeclaringClass().getName()
                + " annotated with " + annotationType.getCanonicalName()
                + (attribute.isEmpty() ? " " : " has a " + attribute(attribute));
    }

    private static String attribute(String attribute) {
        return "value".equals(attribute) ? "" : attribute + " ";
    }
}
