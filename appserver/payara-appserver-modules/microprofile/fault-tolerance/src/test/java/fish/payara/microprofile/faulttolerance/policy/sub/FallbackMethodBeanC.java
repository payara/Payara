package fish.payara.microprofile.faulttolerance.policy.sub;

@SuppressWarnings("unused")
public class FallbackMethodBeanC {

    /*
     * FallbackMethodOutOfPackageTest
     */

    String fallbackMethodOutOfPackage_Fallback(int a, Long b) {
        return "fallbackMethodOutOfPackage";
    }
}
