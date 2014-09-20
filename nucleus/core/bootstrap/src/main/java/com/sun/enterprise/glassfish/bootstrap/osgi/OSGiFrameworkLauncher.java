/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.enterprise.glassfish.bootstrap.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Properties;
import java.util.ServiceLoader;

/**
 * Utility class which takes care of launching OSGi framework.
 * It lauches the framework in a daemon thread, because typically framework spawned threads inherit
 * parent thread's daemon status.
 *
 * It also provides a utility method to get hold of OSGi services registered in the system.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiFrameworkLauncher {

    private Properties properties;
    private Framework framework;

    public OSGiFrameworkLauncher(Properties properties) {
        this.properties = properties;
    }

    public Framework launchOSGiFrameWork() throws Exception {
        if (!isOSGiEnv()) {
            // Locate an OSGi framework and initialize it
            ServiceLoader<FrameworkFactory> frameworkFactories =
                    ServiceLoader.load(FrameworkFactory.class, getClass().getClassLoader());
            for (FrameworkFactory ff : frameworkFactories) {
                framework = ff.newFramework(properties);
                break;
            }
            if (framework == null) {
                throw new RuntimeException("No OSGi framework in classpath");
            }
            // init framework in a daemon thread so that the framework spwaned internal threads will be daemons
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        framework.init();
                    } catch (BundleException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            t.setDaemon(true);
            t.start();
            t.join();
        } else {
            throw new IllegalStateException("An OSGi framework is already running...");
        }
        return framework;
    }

    public <T> T getService(Class<T> type) throws Exception {
        if (framework == null) {
            throw new IllegalStateException("OSGi framework has not yet been launched.");
        }
        final BundleContext context = framework.getBundleContext();
        ServiceTracker tracker = new ServiceTracker(context, type.getName(), null);
        try {
            tracker.open(true);
            return type.cast(tracker.waitForService(0));
        } finally {
            tracker.close(); // no need to track further
        }
    }


    /**
     * Determine if we we are operating in OSGi env. We do this by checking what class loader is used to
     * this class.
     *
     * @return false if we are already called in the context of OSGi framework, else true.
     */
    private boolean isOSGiEnv() {
        return (getClass().getClassLoader() instanceof BundleReference);
    }


}
