/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.glassfish.bootstrap.Constants;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.embeddable.spi.RuntimeBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.enterprise.glassfish.bootstrap.Constants.HK2_CACHE_DIR;
import static com.sun.enterprise.glassfish.bootstrap.Constants.INHABITANTS_CACHE;
import static com.sun.enterprise.glassfish.bootstrap.osgi.Constants.*;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT;

/**
 * This RuntimeBuilder can only handle GlassFish_Platform of following types:
 * <p/>
 * {@link Constants.Platform#Felix},
 * {@link Constants.Platform#Equinox},
 * and {@link Constants.Platform#Knopflerfish}.
 * <p/>
 * <p/>It can't handle GenericOSGi platform,
 * because it reads framework configuration from a framework specific file when it calls
 * {@link ASMainHelper#buildStartupContext(java.util.Properties)}.
 * <p/>
 * This class is responsible for
 * a) setting up OSGi framework,
 * b) installing glassfish bundles,
 * c) starting a configured list of bundles
 * d) obtaining a reference to GlassFishRuntime OSGi service.
 * <p/>
 * Steps #b & #c are handled via {@link BundleProvisioner}.
 * We specify our provisioning bundle details in the properties object that's used to boostrap
 * the system. BundleProvisioner installs and starts such bundles,
 * <p/>
 * If caller does not pass in a properly populated properties object, we assume that we are
 * running against an existing installation of glassfish and set appropriate default values.
 * <p/>
 * <p/>
 * This class is registered as a provider of RuntimeBuilder using META-INF/services file.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public final class OSGiGlassFishRuntimeBuilder implements RuntimeBuilder {

    /*
     * Not a thread safe class.
     */

    private Framework framework;
    private Properties properties;

    private Logger logger = Logger.getLogger(getClass().getPackage().getName());

    private Properties oldProvisioningOptions;
    private Properties newProvisioningOptions;

    // These two should be a part of an external interface of HK2, but they are not, so we have to duplicate them here.
    private OSGiFrameworkLauncher fwLauncher;

    /**
     * Default constructor needed for meta-inf/service lookup to work
     */
    public OSGiGlassFishRuntimeBuilder() {}

    public GlassFishRuntime build(BootstrapProperties bsProps) throws GlassFishException {
        try {
            ASMainHelper.buildStartupContext(bsProps.getProperties());
            properties = bsProps.getProperties();

            // Set the builder name so that when we check for nonEmbedded() inside GlassFishMainActivator,
            // we can identify the environment.
            properties.setProperty(Constants.BUILDER_NAME_PROPERTY, getClass().getName());
            // Step 0: Locate and launch a framework
            long t0 = System.currentTimeMillis();
            fwLauncher = new OSGiFrameworkLauncher(properties);
            framework = fwLauncher.launchOSGiFrameWork();
            long t1 = System.currentTimeMillis();
            logger.logp(Level.FINE, "OSGiGlassFishRuntimeBuilder", "build", "Launched {0}", new Object[]{framework});

            // Step 1: install/update/delete bundles
            if (newFramework()) {
                storeProvisioningOptions();
            } else {
                reconfigure(); // this will reconfigure if any provisioning options have changed.
            }
            BundleProvisioner bundleProvisioner = new BundleProvisioner(framework.getBundleContext(), properties);
            List<Long> bundleIds = bundleProvisioner.installBundles();

            if (bundleProvisioner.hasAnyThingChanged()) {
                bundleProvisioner.refresh();
                deleteHK2Cache(); // clean hk2 cache so that updated bundle details will go in there.
                // Save the bundle ids for use during restart.
                storeBundleIds(bundleIds.toArray(new Long[bundleIds.size()]));
            }
            if (bundleProvisioner.isSystemBundleUpdationRequired()) {
                logger.logp(Level.INFO, "OSGiFrameworkLauncher", "launchOSGiFrameWork", "Updating system bundle");
                framework.update();
            }

            // Step 2: Start bundles
            bundleProvisioner.startBundles();
            long t2 = System.currentTimeMillis();

            // Step 3: Start the framework, so bundles will get activated as per their start levels
            framework.start();
            long t3 = System.currentTimeMillis();
            printStats(bundleProvisioner, t0, t1, t2, t3);

            // Step 4: Obtain reference to GlassFishRuntime and return the same
            return getGlassFishRuntime();
        } catch (Exception e) {
            throw new GlassFishException(e);
        }
    }

    public boolean handles(BootstrapProperties bsProps) {
        // See GLASSFISH-16743 for the reason behind additional check
        final String builderName = bsProps.getProperty(Constants.BUILDER_NAME_PROPERTY);
        if (builderName != null && !builderName.equals(getClass().getName())) {
            return false;
        }
        /*
         * This builder can't handle GOSGi platform, because we read framework configuration from a framework
         * specific file in ASMainHelper.buildStartupContext(properties);
         */
        String platformStr = bsProps.getProperty(Constants.PLATFORM_PROPERTY_KEY);
        if (platformStr != null && platformStr.trim().length() != 0) {
            try {
                Constants.Platform platform = Constants.Platform.valueOf(platformStr);
                switch (platform) {
                    case Felix:
                    case Equinox:
                    case Knopflerfish:
                        return true;
                }
            } catch (IllegalArgumentException ex) {
                // might be a plugged-in custom platform.
            }
        }
        return false;
    }

    private GlassFishRuntime getGlassFishRuntime() throws GlassFishException {
        final ServiceReference reference =
                framework.getBundleContext().getServiceReference(GlassFishRuntime.class.getName());
        if (reference != null) {
            GlassFishRuntime gfr = (GlassFishRuntime) framework.getBundleContext().getService(reference);
            return gfr;
        }
        throw new GlassFishException("No GlassFishRuntime available");
    }

    private void deleteHK2Cache() {
        // This is a HACK - thanks to some weired optimization trick
        // done for GlassFish. HK2 maintains a cache of inhabitants and
        // that needs  to be recreated when there is a change in modules dir.
        final String cacheDir = properties.getProperty(HK2_CACHE_DIR);
        if (cacheDir != null) {
            File inhabitantsCache = new File(cacheDir, INHABITANTS_CACHE);
            if (inhabitantsCache.exists()) inhabitantsCache.delete();
        }
    }

    private void printStats(BundleProvisioner bundleProvisioner, long t0, long t1, long t2, long t3) {
        logger.logp(Level.FINE, "OSGiGlassFishRuntimeBuilder", "build",
                "installed = {0}, updated = {1}, uninstalled = {2}",
                new Object[]{bundleProvisioner.getNoOfInstalledBundles(),
                        bundleProvisioner.getNoOfUpdatedBundles(),
                        bundleProvisioner.getNoOfUninstalledBundles()});
        logger.logp(Level.FINE, "OSGiGlassFishRuntimeBuilder", "build",
                "Total time taken (in ms) to initialize framework = {0}, " +
                        "to install/update/delete/start bundles = {1}, " +
                        "to start framework= {2}",
                new Object[]{t1 - t0, t2 - t1, t3 - t2});
    }

    private boolean newFramework() {
        return framework.getBundleContext().getBundles().length == 1;
    }

    /**
     * This method helps in situations where glassfish installation directory has been moved or
     * certain initial provisoning options have changed, etc. If such thing has happened, it uninstalls
     * all the bundles that were installed from GlassFish installation location.
     */
    private void reconfigure() throws Exception {
        if (hasBeenReconfigured()) {
            // uninstallOldBundles() will also uninstall framework extension bundles.
            // This in turn requires a framework to be updated, but as of Felix 3.0.8,
            // Felix framework can't be updated. It leads to some NPE in resolver somewhere.
            // As a work around, we create a new framework instance with a clean cache.
            // uninstallOldBundles();
            logger.logp(Level.INFO, "OSGiGlassFishRuntimeBuilder", "reconfigure",
                    "Provisioning options have changed, recreating the framework");
            framework.stop();
            framework.waitForStop(0);
            properties.setProperty(FRAMEWORK_STORAGE_CLEAN,
                    FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
            fwLauncher = new OSGiFrameworkLauncher(properties);
            framework = fwLauncher.launchOSGiFrameWork();
            logger.logp(Level.FINE, "OSGiGlassFishRuntimeBuilder", "reconfigure", "Launched {0}",
                    new Object[]{framework});
            storeProvisioningOptions();
        }
    }

    /**
     * Uninstall bundles that were installed as part of an earlier provisoning process.
     */
    private void uninstallOldBundles() {
        final Long[] ids = readBundleIds();
        if (ids.length == 0) {
            for (Bundle b : framework.getBundleContext().getBundles()) {
                if (b != framework) uninstallBundle(b);
            }
        }
        for (Long id : ids) {
            uninstallBundle(id);
        }
    }

    private void uninstallBundle(Long id) {
        Bundle b = framework.getBundleContext().getBundle(id);
        if (b != null) {
            uninstallBundle(b);
        } else {
            logger.logp(Level.WARNING, "OSGiGlassFishRuntimeBuilder", "uninstallBundle",
                    "Unable to locate bundle {0}", new Object[]{id});
        }
    }

    private void uninstallBundle(Bundle b) {
        try {
            b.uninstall();
            logger.logp(Level.FINE, "OSGiGlassFishRuntimeBuilder", "uninstallBundle",
                    "Uninstalled {0}", new Object[]{b});
        } catch (BundleException e) {
            e.printStackTrace();
        }
    }

    private boolean hasBeenReconfigured() {
        try {
            logger.logp(Level.FINE, "OSGiGlassFishRuntimeBuilder", "hasBeenReconfigured", "oldProvisioningOptions = {0}",
                    new Object[]{getOldProvisioningOptions()});
            logger.logp(Level.FINE, "OSGiGlassFishRuntimeBuilder", "hasBeenReconfigured", "newProvisioningOptions = {0}",
                    new Object[]{getNewProvisioningOptions()});
            return !getNewProvisioningOptions().equals(getOldProvisioningOptions());
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    /**
     * @return properties used by BundleProvisioner
     */
    private Properties getNewProvisioningOptions() {
        if (newProvisioningOptions == null) {
            Properties props = new Properties();
            for (String key : properties.stringPropertyNames()) {
                if (key.equals(AUTO_INSTALL_PROP) ||
                        key.equals(AUTO_START_PROP) ||
                        key.startsWith(AUTO_START_LEVEL_PROP)) {
                    props.setProperty(key, properties.getProperty(key));
                }
                // Should we also include default start level of bundles?
            }
            newProvisioningOptions = props;
        }
        return newProvisioningOptions;
    }

    private void storeBundleIds(Long[] bundleIds) {
        try {
            File f = framework.getBundleContext().getDataFile(BUNDLEIDS_FILENAME);
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(f));
            os.writeObject(bundleIds);
            os.flush();
            os.close();
            logger.logp(Level.FINE, "OSGiGlassFishRuntimeBuilder", "storeBundleIds", "Stored bundle ids in {0}",
                    new Object[]{f.getAbsolutePath()});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Long[] readBundleIds() {
        try {
            File f = framework.getBundleContext().getDataFile(BUNDLEIDS_FILENAME);
            if (!f.exists()) return new Long[0];
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(f));
            Long[] result;
            try {
                result = (Long[]) is.readObject();
            } finally {
                is.close();
            }
            logger.logp(Level.FINE, "OSGiGlassFishRuntimeBuilder", "readBundleIds", "Read bundle ids from {0}",
                    new Object[]{f.getAbsolutePath()});
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new Long[0];
        }
    }

    private void storeProvisioningOptions() {
        try {
            File f = framework.getBundleContext().getDataFile(PROVISIONING_OPTIONS_FILENAME);
            final FileOutputStream os = new FileOutputStream(f);
            getNewProvisioningOptions().store(os, "");
            os.flush();
            os.close();
            logger.logp(Level.FINE, "OSGiGlassFishRuntimeBuilder", "storeProvisioningOptions", "Stored provisioning options in {0}",
                    new Object[]{f.getAbsolutePath()});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Properties getOldProvisioningOptions() throws IOException {
        if (oldProvisioningOptions == null) {
            Properties options = new Properties();
            try {
                File f = framework.getBundleContext().getDataFile(PROVISIONING_OPTIONS_FILENAME);
                if (f.exists()) {
                    options.load(new FileInputStream(f));
                    logger.logp(Level.FINE, "OSGiGlassFishRuntimeBuilder", "getOldProvisioningOptions",
                            "Read provisioning options from {0}", new Object[]{f.getAbsolutePath()});
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            oldProvisioningOptions = options;
        }
        return oldProvisioningOptions;
    }

}
