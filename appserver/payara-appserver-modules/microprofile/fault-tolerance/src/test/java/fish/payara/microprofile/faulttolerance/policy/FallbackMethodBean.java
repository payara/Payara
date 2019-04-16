package fish.payara.microprofile.faulttolerance.policy;

@SuppressWarnings("unused")
public interface FallbackMethodBean {

    /*
     * FallbackMethodDefaultMethodTest
     */

    default String fallbackMethodDefaultMethod_Fallback(int a, Long b) {
        return "fallbackMethodDefaultMethod";
    }

    /*
     * FallbackMethodInterfaceTest
     */

    public String fallbackMethodInterface_Fallback(int a, Long b);
}
