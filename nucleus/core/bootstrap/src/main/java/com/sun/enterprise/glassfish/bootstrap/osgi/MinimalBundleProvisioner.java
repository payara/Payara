/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import org.osgi.framework.*;
import org.osgi.framework.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.glassfish.bootstrap.LogFacade;

/**
 * This is a specialized {@link BundleProvisioner} that installs only a minimum set of of bundles.
 * It derives the set of bundles to be included from the list of bundles to be started and all fragment bundles
 * available in the installation locations.
 *
 * @author sanjeeb.sahoo@oracle.com
 */
public class MinimalBundleProvisioner extends BundleProvisioner {
    private Logger logger = LogFacade.BOOTSTRAP_LOGGER;
    private List<Long> installedBundleIds;
    static class MinimalCustomizer extends DefaultCustomizer {
        private Logger logger = LogFacade.BOOTSTRAP_LOGGER;
        public MinimalCustomizer(Properties config) {
            super(config);
        }

        public Jar getLatestJar() {
            File latestFile = null;
            for (URI uri : getConfiguredAutoInstallLocations()) {
                File file = null;
                try {
                    file = new File(uri);
                } catch (Exception e) {
                    continue; // not a file, skip to next one
                }
                if (latestFile == null) {
                    latestFile = file;
                }
                if (file.lastModified() > latestFile.lastModified()) {
                    latestFile = file;
                }
                if (file.isDirectory()) {
                    // do only one-level search as configured auto install locations are not recursive.
                    for (File child : file.listFiles()) {
                        if (child.lastModified() > latestFile.lastModified()) {
                            latestFile = child;
                        }
                    }
                }
            }
            return latestFile != null ? new Jar(latestFile) : null;
        }

        @Override
        public List<URI> getAutoInstallLocations() {
            // We only install those bundles that are required to be started  or those bundles that are fragments
            List<URI> installLocations = getAutoStartLocations();
            List<URI> fragments = selectFragmentJars(super.getAutoInstallLocations());
            installLocations.addAll(fragments);
            logger.log(Level.INFO, LogFacade.SHOW_INSTALL_LOCATIONS, new Object[]{installLocations});
            return installLocations;
        }

        private List<URI> selectFragmentJars(List<URI> installLocations) {
            List<URI> fragments = new ArrayList<URI>();
            for (URI uri : installLocations) {
                InputStream is = null;
                JarInputStream jis = null;
                try {
                    is = uri.toURL().openStream();
                    jis = new JarInputStream(is);
                    Manifest m = jis.getManifest();
                    if (m != null && m.getMainAttributes().getValue(Constants.FRAGMENT_HOST) != null) {
                        logger.logp(Level.FINE, "MinimalBundleProvisioner$MinimalCustomizer", "selectFragmentJars",
                                "{0} is a fragment", new Object[]{uri});
                        fragments.add(uri);
                    }
                } catch (IOException e) {
                    LogFacade.log(logger, Level.INFO, LogFacade.CANT_TELL_IF_FRAGMENT, e, uri);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (jis != null) {
                            jis.close();
                        }
                    } catch (IOException e1) {
                        // ignore
                    }
                }
            }
            return fragments;
        }
    }

    public MinimalBundleProvisioner(BundleContext bundleContext, Properties config) {
        super(bundleContext, new MinimalCustomizer(config));
    }

    @Override
    public List<Long> installBundles() {
        BundleContext bctx = getBundleContext();
        final int n = bctx.getBundles().length;
        List<Long> bundleIds;
        if (n > 1) {
            // This is not the first run of the program, so don't do anything
            logger.logp(Level.FINE, "MinimalBundleProvisioner", "installBundles",
                    "Skipping installation of bundles as there are already {0} no. of bundles.", new Object[]{n});
            bundleIds = Collections.emptyList();
        } else {
            bundleIds = super.installBundles();
        }
        return installedBundleIds = bundleIds;
    }

    @Override
    public void startBundles() {
        if (installedBundleIds.isEmpty()) {
            logger.log(Level.INFO, LogFacade.SKIP_STARTING_ALREADY_PROVISIONED_BUNDLES);
        } else {
            super.startBundles();
        }
    }

    @Override
    public boolean hasAnyThingChanged() {
        long latestBundleTimestamp = -1;
        Bundle latestBundle = null;
        for (Bundle b : getBundleContext().getBundles()) {
            if (b.getLastModified() > latestBundleTimestamp) {
                latestBundleTimestamp = b.getLastModified();
                latestBundle = b;
            }
        }
        Jar latestJar = getCustomizer().getLatestJar();
        final boolean chnaged = latestJar.getLastModified() > latestBundle.getLastModified();
        logger.log(Level.INFO, LogFacade.LATEST_FILE_IN_INSTALL_LOCATION,
                new Object[]{chnaged, latestJar.getURI(), latestBundle.getLocation()});
        return chnaged;
    }

    @Override
    public void refresh() {
        // uninstall everything and start afresh
        for (Bundle b : getBundleContext().getBundles()) {
            // TODO(Sahoo): We should call getCustomizer().isManaged(new Jar(b)),
            // but obr gives us the ability to encode information in url
            if (b.getBundleId() != 0) {
                try {
                    b.uninstall();
                } catch (BundleException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        installBundles();
        super.refresh();
        setSystemBundleUpdationRequired(true);
    }

    @Override
    public MinimalCustomizer getCustomizer() {
        return (MinimalCustomizer) super.getCustomizer();
    }
}
