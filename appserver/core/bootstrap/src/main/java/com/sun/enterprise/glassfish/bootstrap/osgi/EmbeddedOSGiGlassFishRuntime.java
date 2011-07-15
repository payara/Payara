/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.glassfish.bootstrap.ASMainHelper;
import com.sun.enterprise.glassfish.bootstrap.GlassFishImpl;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.bootstrap.BootException;
import com.sun.enterprise.module.bootstrap.Main;
import com.sun.enterprise.module.bootstrap.ModuleStartup;
import com.sun.enterprise.module.bootstrap.StartupContext;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.jvnet.hk2.component.Habitat;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
* @author Sanjeeb.Sahoo@Sun.COM
*/
public class EmbeddedOSGiGlassFishRuntime extends GlassFishRuntime {

    // TODO(Sahoo): Merge with StarticGlassFishRuntime and elevate to higher package level.

    List<GlassFish> gfs = new ArrayList<GlassFish>();

    public EmbeddedOSGiGlassFishRuntime() {
    }

    @Override
    public synchronized GlassFish newGlassFish(GlassFishProperties gfProps) throws GlassFishException {
        try {
            // set env props before updating config, because configuration update may actually trigger
            // some code to be executed which may be depending on the environment variable values.
            setEnv(gfProps.getProperties());
            final StartupContext startupContext = new StartupContext(gfProps.getProperties());
            final ServiceTracker hk2Tracker = new ServiceTracker(getBundleContext(), Main.class.getName(), null);
            hk2Tracker.open();
            final Main main = (Main) hk2Tracker.waitForService(0);
            hk2Tracker.close();
            final ModulesRegistry mr = ModulesRegistry.class.cast(getBundleContext().getService(getBundleContext().getServiceReference(ModulesRegistry.class.getName())));
            final Habitat habitat = main.createHabitat(mr, startupContext);
            final ModuleStartup gfKernel = main.findStartupService(mr, habitat, null, startupContext);
            GlassFish glassFish = createGlassFish(gfKernel, habitat, gfProps.getProperties());
            gfs.add(glassFish);
            getBundleContext().registerService(GlassFish.class.getName(), glassFish, gfProps.getProperties());
            return glassFish;
        } catch (BootException ex) {
            throw new GlassFishException(ex);
        } catch (InterruptedException ex) {
            throw new GlassFishException(ex);
        }
    }

    public synchronized void shutdown() throws GlassFishException {
        for (GlassFish gf : gfs) {
            if (gf.getStatus() != GlassFish.Status.DISPOSED) {
                try {
                    gf.dispose();
                } catch (GlassFishException e) {
                    e.printStackTrace();
                }
            }
        }
        gfs.clear();
        shutdownInternal();
        System.out.println("Completed shutdown of GlassFish runtime");
    }

    private void setEnv(Properties properties) {
        final String installRootValue = properties.getProperty(com.sun.enterprise.glassfish.bootstrap.Constants.INSTALL_ROOT_PROP_NAME);
        if (installRootValue != null && !installRootValue.isEmpty()) {
            File installRoot = new File(installRootValue);
            System.setProperty(com.sun.enterprise.glassfish.bootstrap.Constants.INSTALL_ROOT_PROP_NAME, installRoot.getAbsolutePath());
            final Properties asenv = ASMainHelper.parseAsEnv(installRoot);
            for (String s : asenv.stringPropertyNames()) {
                System.setProperty(s, asenv.getProperty(s));
            }
            System.setProperty(com.sun.enterprise.glassfish.bootstrap.Constants.INSTALL_ROOT_URI_PROP_NAME, installRoot.toURI().toString());
        }
        final String instanceRootValue = properties.getProperty(com.sun.enterprise.glassfish.bootstrap.Constants.INSTANCE_ROOT_PROP_NAME);
        if (instanceRootValue != null && !instanceRootValue.isEmpty()) {
            File instanceRoot = new File(instanceRootValue);
            System.setProperty(com.sun.enterprise.glassfish.bootstrap.Constants.INSTANCE_ROOT_PROP_NAME, instanceRoot.getAbsolutePath());
            System.setProperty(com.sun.enterprise.glassfish.bootstrap.Constants.INSTANCE_ROOT_URI_PROP_NAME, instanceRoot.toURI().toString());
        }
    }

    protected GlassFish createGlassFish(ModuleStartup gfKernel, Habitat habitat, Properties gfProps) throws GlassFishException {
        return new GlassFishImpl(gfKernel, habitat, gfProps);
    }

    public BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(getClass()).getBundleContext();
    }
}
