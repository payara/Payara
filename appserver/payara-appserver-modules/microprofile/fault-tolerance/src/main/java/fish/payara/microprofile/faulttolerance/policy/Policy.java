package fish.payara.microprofile.faulttolerance.policy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.logging.Level;

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
            checkReturnsSameAs(annotatedMethod, annotationType, attribute, actual);
        } catch (NoSuchMethodException ex) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute)
                    + "refering to a method "+valueMethodName+" that does not exist for type: " + valueType.getName(), ex);
        }
    }

    public static void checkReturnsSameAs(Method annotatedMethod, Class<? extends Annotation> annotationType,
            String attribute, Method value) {
        if (value.getReturnType() != annotatedMethod.getReturnType()) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute)
                    + "whose return type of does not match.");
        }
    }

    protected static String describe(Method annotatedMethod, Class<? extends Annotation> annotationType, String attribute) {
        return "Method \"" + annotatedMethod.getName() + "\" in " + annotatedMethod.getDeclaringClass().getName()
                + " annotated with " + annotationType.getSimpleName()
                + (attribute.isEmpty() ? " " : " has a " + attribute(attribute));
    }

    private static String attribute(String attribute) {
        return "value".equals(attribute) ? "" : attribute + " ";
    }

    public static boolean isCaught(Exception ex, Class<? extends Throwable>[] caught) {
        if (caught.length == 0) {
            return false;
        }
        if (caught[0] == Throwable.class) {
            return true;
        }
        for (Class<? extends Throwable> caughtType : caught) {
            if (ex.getClass() == caughtType) {
                CircuitBreakerPolicy.logger.log(Level.FINER, "Exception {0} matches a Throwable", ex.getClass().getSimpleName());
                return true;
            }
            if (caughtType.isAssignableFrom(ex.getClass())) {
                CircuitBreakerPolicy.logger.log(Level.FINER, "Exception {0} is a child of a Throwable: {1}",
                        new String[] { ex.getClass().getSimpleName(), caughtType.getSimpleName() });
                return true;
            }
        }
        return false;
    }
}
