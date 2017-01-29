/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.cdi.jsr107;

import fish.payara.nucleus.hazelcast.HazelcastCore;

/**
 *
 * @author steve
 */
public class AbstractJSR107Interceptor {
    
    HazelcastCore hzCore;

    public AbstractJSR107Interceptor() {
        hzCore = HazelcastCore.getCore();
    }
    
    protected boolean isEnabled() {
        return hzCore.isEnabled();
    }
    
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
