// Portions Copyright [2020] [Payara Foundation and/or its affiliates]
package org.glassfish.web.loader;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 *
 * @author Cuba Stanley
 */
public class WebappClassLoaderFinalizer extends PhantomReference<WebappClassLoader> {
    
    public WebappClassLoaderFinalizer(WebappClassLoader t, ReferenceQueue<? super WebappClassLoader> rq) {
        super(t, rq);
    }
    
    public void cleanupAction() {
        WebappClassLoader.decreaseInstanceCount();
    }
    
}
