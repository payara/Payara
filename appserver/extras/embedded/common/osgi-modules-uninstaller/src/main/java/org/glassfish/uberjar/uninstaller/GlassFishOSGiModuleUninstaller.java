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

package org.glassfish.uberjar.uninstaller;

import org.osgi.framework.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@dev.java.net
 */

public class GlassFishOSGiModuleUninstaller implements BundleActivator, BundleListener {

    private static Logger logger = Logger.getLogger("embedded-glassfish");

    private final String uberSymbolicName = "org.glassfish.embedded.glassfish-activator";
    private Bundle myself;

    public void start(BundleContext bundleContext) throws Exception {
        myself = bundleContext.getBundle();
        bundleContext.addBundleListener(this);
        logger.info("EmbeddedGlassFishUninstaller started");
    }

    public void stop(BundleContext bundleContext) throws Exception {
        logger.info("EmbeddedGlassFishUninstaller stopped");
    }

    public void bundleChanged(BundleEvent bundleEvent) {
        if (bundleEvent.getType() == BundleEvent.UNINSTALLED) {
            String uninstalledBundle = bundleEvent.getBundle().getSymbolicName();
            if (uberSymbolicName.equals(uninstalledBundle)) {
                logger.info("Embedded GlassFish UberJar is uninstalled. " +
                        "Hence uninstalling all the GlassFish bundles.");
                // bundleEvent.getBundle().getBundleContext(0 returns null, hence use it from 'myself'
                BundleContext context = myself.getBundleContext();
                List<Bundle> uninstalled = new ArrayList();
                logger.info("BundleContext = " + context);
                for (Bundle b : context.getBundles()) {
                    if (/*!b.equals(myself) && */b.getLocation().indexOf("glassfish-embedded") != -1) {
                        try {
                            b.uninstall();
                            uninstalled.add(b);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }

/*
                // Make sure the OSGI cache is cleaned up.
                
                // PackageAdmin throws NoClassDefFoundException possibly
                // because the felix classes are bundled in uber.jar -- check with Sahoo.

                // Workaround : type 'refresh' in the gogo shell after uninstalling.
                ServiceReference ref =
                        context.getServiceReference(PackageAdmin.class.getName());
                PackageAdmin pa = ref == null ? null : (PackageAdmin) context.getService(ref);
                logger.info("ref = " + ref + ", pa  = " + pa);
                if(pa != null) {
                    pa.refreshPackages(uninstalled.toArray(new Bundle[0]));
                }

*/


/*
                try {
                    if (myself != null) {
                        myself.uninstall();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
*/
                logger.info("Finished uninstalling all GlassFish bundles");
            }
        }
    }

}
