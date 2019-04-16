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

    /*
     * Common Helper Methods
     */
    static Object[] createNullArgumentsFor(Method method) {
        Object[] args = new Object[method.getParameterCount()];
        for (int i = 0; i < method.getParameterCount(); i++) {
            if (method.getParameterTypes()[i].isPrimitive()) {
                args[i] = Integer.valueOf(0);
            }
        }
        return args;
    }

    static Method getMethod(Class<?> target, String name) {
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
}
