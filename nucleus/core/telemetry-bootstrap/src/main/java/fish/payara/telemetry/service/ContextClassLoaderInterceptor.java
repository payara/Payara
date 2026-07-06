package fish.payara.telemetry.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class ContextClassLoaderInterceptor implements InvocationHandler {
    private final ClassLoader contextClassLoader;
    private final Object target;

    ContextClassLoaderInterceptor(Object target) {
        this.contextClassLoader = Thread.currentThread().getContextClassLoader();
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T wrap(T target) {
        return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), target.getClass().getInterfaces(),
                new ContextClassLoaderInterceptor(target));
    }

    static <T> T wrapIfOtherClassloader(T e, ClassLoader otelClassloader) {
        return e.getClass().getClassLoader() == otelClassloader ? e : wrap(e);
    }

}
