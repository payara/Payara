package fish.payara.microprofile.faulttolerance.policy;

import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.faulttolerance.Fallback;

@SuppressWarnings("unused")
public abstract class FallbackMethodBeanA<L, S> extends FallbackMethodBeanB<L> implements FallbackMethodBean {

    /*
     * FallbackMethodAbstractTest
     */

    @Fallback(fallbackMethod = "fallbackMethodAbstract_Fallback")
    public String fallbackMethodAbstract_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodAbstract");
    }

    abstract protected String fallbackMethodAbstract_Fallback(int a, Long b);


    /*
     * FallbackMethodGenericAbstractTest
     */

    @Fallback(fallbackMethod = "fallbackMethodGenericAbstract_Fallback")
    public String fallbackMethodGenericAbstract_Method(int a, L b) {
        throw new RuntimeException("fallbackMethodGenericAbstract");
    }

    protected abstract String fallbackMethodGenericAbstract_Fallback(int a, L b);

    /*
     * FallbackMethodGenericArrayTest
     */

    public String fallbackMethodGenericArray_Fallback(S[][] arg) {
        return "fallbackMethodGenericArray";
    }

    /*
     * FallbackMethodGenericComplexTest
     */

    public String fallbackMethodGenericComplex_Fallback(List<Set<S>> a) {
        return "fallbackMethodGenericComplex";
    }

    /*
     * FallbackMethodGenericTest
     */

    public String fallbackMethodGeneric_Fallback(int a, L b) {
        return "fallbackMethodGeneric";
    }

    /*
     * FallbackMethodGenericWildcardTest
     */

    public String fallbackMethodGenericWildcard_Fallback(List<? extends S> a) {
        return "fallbackMethodGenericWildcard";
    }

    /*
     * FallbackMethodInPackageTest
     */

    String fallbackMethodInPackage_Fallback(int a, Long b) {
        return "fallbackMethodInPackage";
    }

    /*
     * FallbackMethodInterfaceTest
     */

    @Fallback(fallbackMethod = "fallbackMethodInterface_Fallback")
    public String fallbackMethodInterface_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodInterface");
    }

    /*
     * FallbackMethodSubclassOverrideTest
     */

    @Override
    protected String fallbackMethodSubclassOverride_Fallback(int a, Long b) {
        return "fallbackMethodSubclassOverride";
    }

    /*
     * FallbackMethodSuperclassTest
     */

    protected String fallbackMethodSuperclass_Fallback(int a, Long b) {
        return "fallbackMethodSuperclass";
    }

}
