package fish.payara.microprofile.faulttolerance.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;

public class TestUtils {

    public static Object[] createNullArgumentsFor(Method method) {
        Object[] args = new Object[method.getParameterCount()];
        for (int i = 0; i < method.getParameterCount(); i++) {
            if (method.getParameterTypes()[i].isPrimitive()) {
                args[i] = Integer.valueOf(0);
            }
        }
        return args;
    }

    public static Method getMethod(Class<?> target, String name) {
        for (Method m : target.getDeclaredMethods()) {
            if (name.equals(m.getName())) {
                return m;
            }
        }
        if (target.getSuperclass() != Object.class) {
            return getMethod(target.getSuperclass(), name);
        }
        fail("Test setup failure: no method with name: "+name);
        return null;
    }

    public static void assertAnnotationInvalid(String expectedErrorMessage) {
        try {
            Method annotatedMethod = getAnnotatedMethod();
            FaultTolerancePolicy policy = FaultTolerancePolicy.asAnnotated(annotatedMethod);
            fail("Annotation should be invalid for " + annotatedMethod + " but got: " + policy);
        } catch (FaultToleranceDefinitionException ex) {
            String message = ex.getMessage();
            int endGeneralPart = message.indexOf(" annotated with ");
            assertTrue(endGeneralPart > 0);
            assertEquals(expectedErrorMessage, message.substring(endGeneralPart + 16));
        }
    }

    public static Method getAnnotatedMethod() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        int i = 0;
        while (!stackTraceElements[i].getClassName().endsWith("Test")) {
            i++;
        }
        StackTraceElement testMethodElement = stackTraceElements[i];
        String testName = testMethodElement.getMethodName();
        try {
            return TestUtils.getMethod(Class.forName(testMethodElement.getClassName()), testName + "_Method");
        } catch (Exception e) {
            throw new AssertionError("Failed to find annotated method in test class: ", e);
        }
    }
}
