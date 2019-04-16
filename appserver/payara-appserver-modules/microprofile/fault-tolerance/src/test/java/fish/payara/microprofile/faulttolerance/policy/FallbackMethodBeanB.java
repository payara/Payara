package fish.payara.microprofile.faulttolerance.policy;

import static org.junit.Assert.fail;

import java.lang.reflect.Method;

import org.eclipse.microprofile.faulttolerance.Fallback;

import fish.payara.microprofile.faulttolerance.policy.sub.FallbackMethodBeanC;

@SuppressWarnings("unused")
public class FallbackMethodBeanB<L> extends FallbackMethodBeanC {

    /*
     * FallbackMethodGenericDeepTest
     */

    public String fallbackMethodGenericDeep_Fallback(int a, L b) {
        return "fallbackMethodGenericDeep";
    }

    /*
     * FallbackMethodSubclassOverrideTest
     */

    @Fallback(fallbackMethod = "fallbackMethodSubclassOverride_Fallback")
    public String fallbackMethodSubclassOverride_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodSubclassOverride");
    }

    protected String fallbackMethodSubclassOverride_Fallback(int a, Long b) {
        // This fallback method should not be called as it is overridden in subclass
        return "Not this fallback";
    }

    /*
     * FallbackMethodSubclassTest
     */

    @Fallback(fallbackMethod = "fallbackMethodSubclass_Fallback")
    public String fallbackMethodSubclass_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodSubclass");
    }

    /*
     * FallbackMethodSuperclassPrivateTest
     */

    private String fallbackMethodSuperclassPrivate_Fallback(int a, Long b) {
        return "fallbackMethodSuperclassPrivate";
    }

}
