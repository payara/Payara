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

package org.glassfish.uberjar.bootstrap;

import com.sun.enterprise.glassfish.bootstrap.OSGiFrameworkLauncher;
import java.net.URISyntaxException;

import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.launch.Framework;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.spi.RuntimeBuilder;

/**
 * @author bhavanishankar@dev.java.net
 */

public class UberJarOSGiGlassFishRuntimeBuilder implements RuntimeBuilder {

    private Framework framework;

    public boolean handles(BootstrapProperties bsOptions) {
        // default is Felix
        /* Constants.Platform platform =
                Constants.Platform.valueOf(props.getProperty(
                        Constants.PLATFORM_PROPERTY_KEY, Constants.Platform.Felix.name())); */
        BootstrapProperties.Platform platform = BootstrapProperties.Platform.valueOf(bsOptions.getPlatform());
        if(platform == null) {
            platform = BootstrapProperties.Platform.valueOf(BootstrapProperties.Platform.Felix.name());
        }
        logger.finer("platform = " + platform);
        // TODO(Sahoo): Add support for generic OSGi platform
        switch (platform) {
            case Felix:
            case Equinox:
            case Knopflerfish:
                return true;
        }
        return false;
    }

    public void destroy() throws GlassFishException {

        if (framework != null) {
            try {
                framework.stop();
                framework.waitForStop(0);
                logger.info("EmbeddedOSGIRuntimeBuilder.destroy, stopped framework " + framework);
            } catch (InterruptedException ex) {
                throw new GlassFishException(ex);
            } catch (BundleException ex) {
                throw new GlassFishException(ex);
            }
        } else {
            logger.finer("EmbeddedOSGIRuntimeBuilder.destroy called");
        }
    }

    private static Logger logger = Logger.getLogger("embedded-glassfish");

    public static final String AUTO_START_BUNDLES_PROP =
            "org.glassfish.embedded.osgimain.autostartBundles";


    private static final String UBER_JAR_URI = "org.glassfish.embedded.osgimain.jarURI";

    public GlassFishRuntime build(BootstrapProperties bsOptions) throws GlassFishException {
        // Get all the properties in the Bootstrap options and then manipulate the Properties object.
        Properties props = bsOptions.getProperties();

        String uberJarURI = bsOptions.getProperties().getProperty(UBER_JAR_URI);
        logger.finer("EmbeddedOSGIRuntimeBuilder.build, uberJarUri = " + uberJarURI);

        URI jar = null;
        try {
            jar = uberJarURI != null ? new URI(uberJarURI) : Util.whichJar(GlassFishRuntime.class);
        } catch (URISyntaxException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
         
        // XXX : Commented out by Prasad , we are again looking for instance root here. Why ?
        // String instanceRoot = props.getProperty(Constants.INSTALL_ROOT_PROP_NAME);
        String installRoot = bsOptions.getInstallRoot();

        if (installRoot == null) {
            installRoot = getDefaultInstallRoot();
            props.setProperty(BootstrapProperties.INSTALL_ROOT_PROP_NAME, installRoot);
            props.setProperty(BootstrapProperties.INSTALL_ROOT_URI_PROP_NAME,
                    new File(installRoot).toURI().toString());
        }

        // XXX : Assuming that this property will be set along with Bootstrap options.
        // This is a temporary hack, we need to separate the properties out between bootstrap and newGlassfish methods clearly
        // and not mix them in the code.
        String instanceRoot = props.getProperty(GlassFishProperties.INSTANCE_ROOT_PROP_NAME);
        if (instanceRoot == null) {
            instanceRoot = getDefaultInstanceRoot();
            props.setProperty(GlassFishProperties.INSTANCE_ROOT_PROP_NAME, instanceRoot);
            props.setProperty(GlassFishProperties.INSTANCE_ROOT_URI_PROP_NAME,
                    new File(instanceRoot).toURI().toString());
        }
        try {
            copyConfigFile(props.getProperty(GlassFishProperties.CONFIG_FILE_URI_PROP_NAME), instanceRoot);
        } catch (Exception ex) {
            throw new GlassFishException(ex);
        }

        String platform = props.getProperty(BootstrapProperties.PLATFORM_PROPERTY_KEY);
        if (platform == null) {
            platform = BootstrapProperties.Platform.Felix.toString();
            props.setProperty(BootstrapProperties.PLATFORM_PROPERTY_KEY, platform);
        }

       // readConfigProperties(installRoot, props);

        System.setProperty(UBER_JAR_URI, jar.toString()); // embedded-osgi-main module will need this to extract the modules.

        String osgiMainModule = "jar:" + jar.toString() + "!/uber-osgi-main.jar";
        props.setProperty("glassfish.auto.start", osgiMainModule);

        String autoStartBundleLocation = "jar:" + jar.toString() + "!/modules/installroot-builder_jar/," +
                "jar:" + jar.toString() + "!/modules/instanceroot-builder_jar/," +
                "jar:" + jar.toString() + "!/modules/kernel_jar/"; // TODO :: was modules/glassfish_jar

        if (isOSGiEnv()) {
            autoStartBundleLocation = autoStartBundleLocation +
                    ",jar:" + jar.toString() + "!/modules/osgi-modules-uninstaller_jar/";
        }

        props.setProperty(AUTO_START_BUNDLES_PROP, autoStartBundleLocation);
        System.setProperty(AUTO_START_BUNDLES_PROP, autoStartBundleLocation);

        System.setProperty(BootstrapProperties.INSTALL_ROOT_PROP_NAME, installRoot);
        System.setProperty(GlassFishProperties.INSTANCE_ROOT_PROP_NAME, instanceRoot);

        props.setProperty("org.osgi.framework.system.packages.extra",
                "org.glassfish.simpleglassfishapi; version=3.1");

//        props.setProperty(org.osgi.framework.Constants.FRAMEWORK_BUNDLE_PARENT,
//                org.osgi.framework.Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);
//        props.setProperty("org.osgi.framework.bootdelegation", "org.jvnet.hk2.component, " +
//                "org.jvnet.hk2.component.*," +
//                "org.jvnet.hk2.annotations," +
//                "org.jvnet.hk2.annotations.*");
//        props.setProperty("org.osgi.framework.bootdelegation", "*");

        props.setProperty("org.osgi.framework.storage", instanceRoot + "/osgi-cache/Felix");
//        }

        logger.logp(Level.FINER, "EmbeddedOSGIRuntimeBuilder", "build",
                "Building file system {0}", props);

        try {
            if (!isOSGiEnv()) {
                final OSGiFrameworkLauncher fwLauncher = new OSGiFrameworkLauncher(props);
                framework = fwLauncher.launchOSGiFrameWork();
                return fwLauncher.getService(GlassFishRuntime.class);
            } else {
                BundleContext context = ((BundleReference) (getClass().getClassLoader())).
                        getBundle().getBundleContext();
                Bundle autostartBundle = context.installBundle(props.getProperty("glassfish.auto.start"));
                autostartBundle.start(Bundle.START_TRANSIENT);
                logger.finer("Started autostartBundle " + autostartBundle);
                return getService(GlassFishRuntime.class, context);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw new GlassFishException(new Exception(t));
//            return null;
        }
    }

    private String getDefaultInstallRoot() {
        String userDir = System.getProperty("user.home");
        return new File(userDir, ".glassfish4-embedded").getAbsolutePath();
    }

    private String getDefaultInstanceRoot() {
        String userDir = System.getProperty("user.home");
        String fs = File.separator;
        return new File(userDir, ".glassfish4-embedded" + fs + "domains" + fs + "domain1").getAbsolutePath();
    }

    private boolean isOSGiEnv() {
        return (getClass().getClassLoader() instanceof BundleReference);
    }

    public <T> T getService(Class<T> type, BundleContext context) throws Exception {
        ServiceTracker tracker = new ServiceTracker(context, type.getName(), null);
        try {
            tracker.open(true);
            return type.cast(tracker.waitForService(0));
        } finally {
            tracker.close(); // no need to track further
        }
    }

    private void copyConfigFile(String configFileURI, String instanceRoot) throws Exception {
        if (configFileURI != null && instanceRoot != null) {
            URI configFile = URI.create(configFileURI);
            InputStream stream = configFile.toURL().openConnection().getInputStream();
            File domainXml = new File(instanceRoot, "config/domain.xml");
            logger.finer("domainXML uri = " + configFileURI + ", size = " + stream.available());
            if (!domainXml.toURI().equals(configFile)) {
                domainXml.getParentFile().mkdirs();
                Util.copy(stream, new FileOutputStream(domainXml), stream.available());
                logger.finer("Created " + domainXml);
            } else {
                logger.finer("Skipped creation of " + domainXml);
            }

        }
    }

}
