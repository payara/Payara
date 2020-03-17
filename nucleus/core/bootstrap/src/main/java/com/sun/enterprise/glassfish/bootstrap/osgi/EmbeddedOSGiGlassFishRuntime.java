/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.glassfish.bootstrap.GlassFishImpl;
import com.sun.enterprise.glassfish.bootstrap.GlassfishUrlClassLoader;
import com.sun.enterprise.glassfish.bootstrap.MainHelper;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.bootstrap.BootException;
import com.sun.enterprise.module.bootstrap.Main;
import com.sun.enterprise.module.bootstrap.ModuleStartup;
import com.sun.enterprise.module.bootstrap.StartupContext;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.hk2.api.ServiceLocator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import static com.sun.enterprise.glassfish.bootstrap.Constants.INSTALL_ROOT_PROP_NAME;
import static com.sun.enterprise.glassfish.bootstrap.Constants.INSTALL_ROOT_URI_PROP_NAME;
import static com.sun.enterprise.glassfish.bootstrap.Constants.INSTANCE_ROOT_PROP_NAME;
import static com.sun.enterprise.glassfish.bootstrap.Constants.INSTANCE_ROOT_URI_PROP_NAME;

/**
 * Implementation of GlassFishRuntime in an OSGi environment.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class EmbeddedOSGiGlassFishRuntime extends GlassFishRuntime {

    // TODO(Sahoo): Merge with StaticGlassFishRuntime and elevate to higher package level.
    // This can be achieved by modelling this as a GlassFishRuntimeDecorator taking StaticGlassFishRuntime
    // as the decorated object.

    private final List<GlassFish> gfs = new ArrayList<>();
    private final BundleContext context;

    public EmbeddedOSGiGlassFishRuntime(BundleContext context) {
        this.context = context;
    }

    @Override
    public synchronized GlassFish newGlassFish(GlassFishProperties gfProps) throws GlassFishException {
        try {
            // set env props before updating config, because configuration update may actually trigger
            // some code to be executed which may be depending on the environment variable values.
            setEnv(gfProps.getProperties());
            final StartupContext startupContext = new StartupContext(gfProps.getProperties());
            final BundleContext bundleCtx = getBundleContext();
            final ServiceTracker<Main, ?> hk2Tracker = new ServiceTracker<>(bundleCtx, Main.class, null);
            hk2Tracker.open();
            final Main hk2Main = (Main) hk2Tracker.waitForService(0);
            hk2Tracker.close();
            final ServiceReference<ModulesRegistry> mrServiceRef = bundleCtx.getServiceReference(ModulesRegistry.class);
            final ModulesRegistry mr = bundleCtx.getService(mrServiceRef);
            logClassLoaders(mr);
            final ServiceLocator serviceLocator = hk2Main.createServiceLocator(mr, startupContext, null, null);
            final ModuleStartup gfKernel = hk2Main.findStartupService(mr, serviceLocator, null, startupContext);
            final GlassFish glassFish = createGlassFish(gfKernel, serviceLocator, gfProps.getProperties());
            gfs.add(glassFish);
            return glassFish;
        } catch (BootException ex) {
            throw new GlassFishException(ex);
        } catch (InterruptedException ex) {
            throw new GlassFishException(ex);
        }
    }

    @Override
    public synchronized void shutdown() throws GlassFishException {
        // make a copy to avoid ConcurrentModificationException
        for (GlassFish gf : new ArrayList<>(gfs)) {
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
    }

    protected GlassFish createGlassFish(ModuleStartup gfKernel, ServiceLocator habitat, Properties gfProps) throws GlassFishException {
        GlassFish gf = new GlassFishImpl(gfKernel, habitat, gfProps);
        return new EmbeddedOSGiGlassFishImpl(gf, getBundleContext());
    }

    private void setEnv(Properties properties) {
        final String installRootValue = properties.getProperty(INSTALL_ROOT_PROP_NAME);
        if (installRootValue != null && !installRootValue.isEmpty()) {
            File installRoot = new File(installRootValue);
            System.setProperty(INSTALL_ROOT_PROP_NAME, installRoot.getAbsolutePath());
            final Properties asenv = MainHelper.parseAsEnv(installRoot);
            for (String s : asenv.stringPropertyNames()) {
                System.setProperty(s, asenv.getProperty(s));
            }
            System.setProperty(INSTALL_ROOT_URI_PROP_NAME, installRoot.toURI().toString());
        }
        final String instanceRootValue = properties.getProperty(INSTANCE_ROOT_PROP_NAME);
        if (instanceRootValue != null && !instanceRootValue.isEmpty()) {
            File instanceRoot = new File(instanceRootValue);
            System.setProperty(INSTANCE_ROOT_PROP_NAME, instanceRoot.getAbsolutePath());
            System.setProperty(INSTANCE_ROOT_URI_PROP_NAME, instanceRoot.toURI().toString());
        }
    }

    private BundleContext getBundleContext() {
        return context;
    }

    private void logClassLoaders(final ModulesRegistry moduleRegistry) {
        final Logger logger = getLogger();
        if (!logger.isLoggable(Level.FINEST)) {
            return;
        }
        logCL(logger, "currentThread.contextClassLoader:       ", Thread.currentThread().getContextClassLoader());
        logCL(logger, "this.class.classLoader:                 ", getClass().getClassLoader());
        logCL(logger, "this.class.classLoader.parent:          ", getClass().getClassLoader().getParent());
        logCL(logger, "moduleRegistry.parentClassLoader:       ", moduleRegistry.getParentClassLoader());
        logCL(logger, "moduleRegistry.parentClassLoader.parent ", moduleRegistry.getParentClassLoader().getParent());
    }

    private void logCL(final Logger logger, final String label, final ClassLoader classLoader) {
        // don't use supplier here, the message must be resolved in current state, not later.
        logger.finest(label + toString(classLoader));
    }

    private String toString(final ClassLoader cl) {
        if (cl instanceof GlassfishUrlClassLoader) {
            return cl.toString();
        }
        if (cl instanceof URLClassLoader) {
            URLClassLoader ucl = URLClassLoader.class.cast(cl);
            return ucl + ": " + Arrays.stream(ucl.getURLs()).collect(Collectors.toList());
        }
        return cl.toString();
    }
}
