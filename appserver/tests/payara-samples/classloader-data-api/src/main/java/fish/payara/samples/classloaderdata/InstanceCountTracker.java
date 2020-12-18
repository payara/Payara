/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.samples.classloaderdata;

import java.lang.ref.Reference;
import org.glassfish.web.loader.WebappClassLoader;
import org.glassfish.web.loader.WebappClassLoaderFinalizer;

/**
 *
 * @author Cuba Stanley
 */
public class InstanceCountTracker {
    
    private static int previousInstanceCount;
    
    public static int getInstanceCount() {
        System.gc();
        Reference<? extends WebappClassLoader> referenceFromQueue;
        while((referenceFromQueue = WebappClassLoader.referenceQueue.poll()) != null) {
            ((WebappClassLoaderFinalizer)referenceFromQueue).cleanupAction();
            referenceFromQueue.clear();
        }
        int newCount = WebappClassLoader.getInstanceCount();
        previousInstanceCount = newCount;
        return previousInstanceCount;
    }
    
    public static int getPreviousInstanceCount() {
        return previousInstanceCount;
    }
    
}
