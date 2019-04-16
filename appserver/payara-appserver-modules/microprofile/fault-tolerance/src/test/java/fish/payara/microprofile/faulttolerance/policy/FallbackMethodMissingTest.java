package fish.payara.microprofile.faulttolerance.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
import org.junit.Test;

/**
 * Tests scenarios for invalid declarations as given in the TCK
 * {@code org.eclipse.microprofile.fault.tolerance.tck.fallbackmethod} package. Names used are based on the TCK tests
 * names.
 * 
 * Test and annotated method are linked by naming conventions. Annotated method and its fallback method are linked by
 * the {@link Fallback} annotation. All names are unique.
 * 
 * These tests can not be run in TCK since they expect the {@link FaultToleranceDefinitionException} to be thrown
 * unwrapped during bootstrap while weld does wrap these when thrown failing the TCK tests.
 *
 * @author Jan Bernitt
 */
@SuppressWarnings("unused")
public class FallbackMethodMissingTest extends FallbackMethodBeanB<Long> {

    /*
     * FallbackMethodOutOfPackageTest
     */

    @Test
    public void fallbackMethodOutOfPackage() {
        assertMissingFallbackMethod(
                "Method \"fallbackMethodOutOfPackage_Method\" in fish.payara.microprofile.faulttolerance.policy.FallbackMethodMissingTest annotated with Fallback has a fallbackMethod that refers to a method that is not accessible.");
    }

    @Fallback(fallbackMethod = "fallbackMethodOutOfPackage_Fallback")
    public String fallbackMethodOutOfPackage_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodOutOfPackage");
    }

    /*
     * FallbackMethodSubclassTest
     */

    @Test
    public void fallbackMethodSubclass() {
        assertMissingFallbackMethod(
                "Fallback method \"fallbackMethodSubclass_Fallback\" not defined for fish.payara.microprofile.faulttolerance.policy.FallbackMethodBeanB");
    }

    public String fallbackMethodSubclass_Fallback(int a, Long b) {
        return "fallbackMethodSubclass";
    }

    /*
     * FallbackMethodSuperclassPrivateTest
     */

    @Test
    public void fallbackMethodSuperclassPrivate() {
        assertMissingFallbackMethod(
                "Method \"fallbackMethodSuperclassPrivate_Method\" in fish.payara.microprofile.faulttolerance.policy.FallbackMethodMissingTest annotated with Fallback has a fallbackMethod that refers to a method that is not accessible.");
    }

    @Fallback(fallbackMethod = "fallbackMethodSuperclassPrivate_Fallback")
    public String fallbackMethodSuperclassPrivate_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodSuperclassPrivate");
    }

    /*
     * FallbackMethodWildcardNegativeTest
     */

    @Test
    public void fallbackMethodWildcardNegative() {
        assertMissingFallbackMethod(
                "Fallback method \"fallbackMethodWildcardNegative_Fallback\" not defined for fish.payara.microprofile.faulttolerance.policy.FallbackMethodMissingTest");
    }

    @Fallback(fallbackMethod = "fallbackMethodWildcardNegative_Fallback")
    public String fallbackMethodWildcardNegative_Method(List<? extends Number> a) {
        throw new RuntimeException("fallbackMethodWildcardNegative");
    }

    public String fallbackMethodWildcardNegative_Fallback(List<? extends Integer> b) {
        return "fallbackMethodWildcardNegative";
    }

    /*
     * Helper Methods
     */

    private void assertMissingFallbackMethod(String expectedErrorMessage) {
        assertMissingFallbackMethod(getClass(), expectedErrorMessage);
    }

    private void assertMissingFallbackMethod(Class<?> target, String expectedErrorMessage) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String testName = stackTraceElements[target == getClass() ? 3 : 2].getMethodName();
        Method annotatedMethod = getMethod(target, testName + "_Method");
        try {
            FallbackPolicy policy = new FallbackPolicy(annotatedMethod, null,
                    annotatedMethod.getAnnotation(Fallback.class).fallbackMethod());
            fail("Fallback method should be invalid for " + annotatedMethod + " but got: " + policy);
        } catch (FaultToleranceDefinitionException ex) {
            assertEquals(expectedErrorMessage, ex.getMessage());
        }
    }
}
