/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.cdi.jsr107;

/**
 *
 * @author steve
 */
public class AbstractJSR107Interceptor {
    
    protected boolean shouldIEvict (Class<? extends Throwable>[] evictFor, Class<? extends Throwable>[] noEvictFor, Throwable exception) {
        return shouldICache(evictFor, noEvictFor, exception, false);
    }

    protected boolean shouldICache(Class<? extends Throwable>[] cacheFor, Class<? extends Throwable>[] noCacheFor, Throwable exception, boolean allEmpty ) {
        // if both arrays empty then all exceptions prevent caching
        if ((cacheFor == null || cacheFor.length == 0) && (noCacheFor == null || noCacheFor.length == 0)) {
            return allEmpty;
        }
        // both set test is in cacheFor but not in nocachefor
        if (cacheFor != null && cacheFor.length > 0 && noCacheFor != null && noCacheFor.length > 0) {
            boolean result = false;
            for (Class<? extends Throwable> cacheFor1 : cacheFor) {
                if (cacheFor1.isInstance(exception)) {
                    result = true;
                    for (Class<? extends Throwable> noCacheFor2 : noCacheFor) {
                        if (noCacheFor2.isInstance(exception)) {
                            result = false;
                        }
                    }
                }
            }
            return result;
        }
        // exception must be in cachefor to be cached.
        if (noCacheFor == null || noCacheFor.length == 0) {
            for (Class<? extends Throwable> cacheFor1 : cacheFor) {
                if (cacheFor1.isInstance(exception)) {
                    return true;
                }
            }
            return false;
        }
        // no cache for is set but cache for isn't, all exceptions are cacheable unless in nocachefor
        if (cacheFor == null || cacheFor.length == 0) {
            for (Class<? extends Throwable> noCacheFor1 : noCacheFor) {
                if (noCacheFor1.isInstance(exception)) {
                    return false;
                }
            }
        }
        return true;
    }
    
}
