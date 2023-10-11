package fish.payara.microprofile.metrics.cdi.interceptor;

@FunctionalInterface
public interface ThreeFunctionResolver <U,V,W,R> {
    public R apply(U u, V v, W w);
}
