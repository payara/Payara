/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.uberjar.osgimain;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@dev.java.net
 */

public class Main implements BundleActivator {

    private static final String UBER_JAR_URI = "org.glassfish.embedded.osgimain.jarURI";

    public static final String AUTO_START_BUNDLES_PROP =
            "org.glassfish.embedded.osgimain.autostartBundles";

    private List<String> excludedModules = new ArrayList();

    private List<Bundle> autoStartBundles = new ArrayList();

    HashMap<String, Bundle> autostartBundles = new HashMap();

    private static final Logger logger = Logger.getLogger("embedded-glassfish");

    public void start(final BundleContext context) throws Exception {
        logger.logp(Level.FINER, "Main", "start", "Start has been called. BundleContext = {0}", context);


        URI embeddedJarURI = new URI(context.getProperty(UBER_JAR_URI));

        final String autoStartBundleLocation = context.getProperty(AUTO_START_BUNDLES_PROP);

//        String autoStartBundleLocation = context.getProperty(AUTO_START_BUNDLES_PROP); // TODO :: parse multiple values.

        logger.info("Please wait while the GlassFish is being initialized...");
        logger.finer("embeddedJar = " + embeddedJarURI + ", autoStartBundles = " + autoStartBundleLocation);
        logger.finer("Installing GlassFish bundles. Please wait.....");
        ExecutorService executor = Executors.newFixedThreadPool(10);

        Bundle[] bundles = context.getBundles();
        final Map<String, Bundle> installedBundles = new HashMap();
        if (bundles != null && bundles.length > 0) {
            for (Bundle b : bundles) {
                installedBundles.put(b.getSymbolicName(), b);
            }
        }
        final File embeddedJar = new File(embeddedJarURI);

        for (OSGIModule module : ModuleExtractor.extractModules(embeddedJar)) {
            final OSGIModule m = module;
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        String bundleLocation = m.getLocation();
                        if (!excludedModules.contains(bundleLocation)) {
                            Bundle installed = m.getBundleSymbolicName() != null ?
                                    installedBundles.get(m.getBundleSymbolicName()) : null;
                            if(installed == null) {
                                installed = context.installBundle(bundleLocation, m.getContentStream());
                            } else if(installed.getLastModified() < embeddedJar.lastModified()) {
                                // update the bundle if it is already installed and is older than uber jar.
                                installed.update(m.getContentStream());
                            }
                            if (autoStartBundleLocation.indexOf(bundleLocation) != -1) {
                                autostartBundles.put(bundleLocation, installed);
                            }
                        }
                        m.close();
                    } catch (Exception ex) {
                        logger.finer(ex.getMessage());
                    }
                }
            });
        }

        logger.finer("Waiting to complete installation of all bundles. Please wait.....");
        executor.shutdown();
        boolean completed = executor.awaitTermination(120, TimeUnit.SECONDS);
        logger.finer("Completed successfully ? " + completed);

        // Autostart the bundles in the order in which they are specified.
        if (autoStartBundleLocation != null) {
            StringTokenizer st = new StringTokenizer(autoStartBundleLocation, ",");
            while (st.hasMoreTokens()) {
                String bundleLocation = st.nextToken().trim();
                if (bundleLocation.isEmpty()) break;
                Bundle b = autostartBundles.get(bundleLocation);
                if (b != null) {
                    logger.finer("Starting bundle " + b);
                    try {
                        b.start();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    logger.finer("Started bundle " + b);
                } else {
                    logger.warning("Unable to find bundle with location " + bundleLocation);
                }
            }
        }

        logger.finer("Autostart bundles = " + autoStartBundles);
        for (Bundle bundle : autoStartBundles) {
            bundle.start();
        }
    }

    public void stop(BundleContext bundleContext) throws Exception {
        logger.logp(Level.FINER, "Main", "stop", "Stop has been called. BundleContext = {0}", bundleContext);
    }


}
