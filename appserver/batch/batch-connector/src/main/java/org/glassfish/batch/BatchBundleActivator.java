package org.glassfish.batch;

import org.osgi.framework.*;

/**
 * @author Mahesh Kannan
 *
 */
public class BatchBundleActivator
        implements BundleActivator, BundleListener {

    @Override
    public void start(BundleContext context) throws Exception {
//        System.out.println("BatchBundleActivator::start ==> " + context.getBundle().getSymbolicName());
//        context.addBundleListener(this);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
//        System.out.println("BatchBundleActivator::stop ==> " + context.getBundle().getSymbolicName());
    }

    @Override
    public void bundleChanged(BundleEvent event) {
//        System.out.println("BatchBundleActivator::bundleChanged ==> " + event.getBundle().getSymbolicName());
    }
}
