/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.glassfish.bootstrap.Constants;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;

import java.io.File;
import java.net.URI;
import java.util.Properties;

import static com.sun.enterprise.glassfish.bootstrap.osgi.Constants.AUTO_INSTALL_PROP;
import static com.sun.enterprise.glassfish.bootstrap.osgi.Constants.AUTO_START_PROP;

/**
 * This activator is used when glassfish.jar is installed and started
 * in an existing OSGi runtime. It expects install root and instance root
 * to be set via framework context properties called com.sun.aas.installRoot and com.sun.aas.instanceRoot
 * respectively. The former one refers to the directory where glassfish is installed.
 * (e.g., /tmp/glassfish3/glassfish)
 * The latter one refers to the domain directory - this is a directory containing
 * configuration information and deployed applications, etc.
 * If instance root is not set, it defaults to $installRoot/domains/domain1/.
 * <p/>
 * Depending on the two context, it either builds a new GlassFishRuntime from scratch (i.e., by installing
 * necessary GlassFish bundles) or it just makes a new GlassFishRuntime and registers it in service registry.
 * Former is the case when user installs and starts just this bundle, where as latter is the case when this
 * bundle is invoked as part of GlassFish launching code as found in {@link OSGiGlassFishRuntimeBuilder}.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 * @see #prepareStartupContext(org.osgi.framework.BundleContext)
 */
public class GlassFishMainActivator implements BundleActivator {
    private BundleContext context;
    private String installRoot;
    private static volatile GlassFishRuntime gfr;
    private GlassFish gf;

    private static final String[] DEFAULT_INSTALLATION_LOCATIONS_RELATIVE = new String[]{
            "modules/endorsed/",
            "modules/",
            "modules/autostart/"
    };

    private static final String[] DEFAULT_START_LOCATIONS_RELATIVE = new String[]{
            "modules/endorsed/",
            "modules/osgi-adapter.jar",
            "modules/osgi-resource-locator.jar",
            "modules/org.apache.felix.configadmin.jar",
            "modules/org.apache.felix.fileinstall.jar",
            "modules/autostart/"
    };

    public void start(BundleContext context) throws Exception {
        this.context = context;
        if (nonEmbedded()) {
            Framework framework = (Framework) context.getBundle(0);
            gfr = new OSGiGlassFishRuntime(framework);
            context.registerService(GlassFishRuntime.class.getName(), gfr, null);
        } else {
            Properties properties = prepareStartupContext(context);
            final BootstrapProperties bsProperties = new BootstrapProperties(properties);
            if (gfr == null) {
                // Should we do the following in a separate thread?
                gfr = GlassFishRuntime.bootstrap(bsProperties, getClass().getClassLoader());
            }
            gf = gfr.newGlassFish(new GlassFishProperties(properties));
            gf.start();
        }
    }

    private boolean nonEmbedded() {
        return context.getProperty(Constants.BUILDER_NAME_PROPERTY) != null;
    }

    private Properties prepareStartupContext(BundleContext context) {
        Properties properties = new Properties();
        installRoot = context.getProperty(Constants.INSTALL_ROOT_PROP_NAME);

        if (installRoot == null) {
            installRoot = guessInstallRoot(context);
            if (installRoot == null) {
                throw new RuntimeException("Property named " + Constants.INSTALL_ROOT_PROP_NAME + " is not set.");
            } else {
                System.out.println("Deduced install root as : " + installRoot + " from location of bundle. " +
                        "If this is not correct, set correct value in a property called " +
                        Constants.INSTALL_ROOT_PROP_NAME);
            }
        }
        if (!new File(installRoot).exists()) {
            throw new RuntimeException("No such directory: [" + installRoot + "]");
        }
        properties.setProperty(Constants.INSTALL_ROOT_PROP_NAME,
                installRoot);
        String instanceRoot = context.getProperty(Constants.INSTANCE_ROOT_PROP_NAME);
        if (instanceRoot == null) {
            instanceRoot = new File(installRoot, "domains/domain1/").getAbsolutePath();
        }
        properties.setProperty(Constants.INSTANCE_ROOT_PROP_NAME,
                instanceRoot);

        properties.putAll(makeProvisioningOptions());

        // This property is understood by our corresponding builder.
        properties.setProperty(Constants.BUILDER_NAME_PROPERTY, EmbeddedOSGiGlassFishRuntimeBuilder.class.getName());
        return properties;
    }

    /**
     * This method tries to guess install root based on location of the bundle. Please note, because location of a
     * bundle is free form string, this method can come to very wrong conclusion if user wants to fool us.
     *
     * @param context
     * @return
     */
    private String guessInstallRoot(BundleContext context) {
        String location = context.getBundle().getLocation();
        try {
            final URI uri = URI.create(location);
            File f = new File(uri);
            if (f.exists() && f.isFile() && f.getParentFile().getCanonicalPath().endsWith("modules") &&
                    f.getParentFile().getParentFile().getCanonicalPath().endsWith("glassfish")) {
                return f.getParentFile().getParentFile().getAbsolutePath();
            }
        } catch (Exception e) {
        }
        return null;
    }

    private Properties makeProvisioningOptions() {
        Properties provisioningOptions = new Properties();
        URI installURI = new File(installRoot).toURI();
        String installLocations = getBundleContext().getProperty(AUTO_INSTALL_PROP);
        if (installLocations == null) {
            StringBuilder defaultInstallLocations = new StringBuilder();
            for (String entry : DEFAULT_INSTALLATION_LOCATIONS_RELATIVE) {
                defaultInstallLocations.append(installURI.resolve(entry).toString()).append(" ");
            }
            installLocations = defaultInstallLocations.toString();
        }
        provisioningOptions.setProperty(AUTO_INSTALL_PROP, installLocations);
        String startLocations = getBundleContext().getProperty(AUTO_START_PROP);
        if (startLocations == null) {
            StringBuilder deafultStartLocations = new StringBuilder();
            for (String entry : DEFAULT_START_LOCATIONS_RELATIVE) {
                deafultStartLocations.append(installURI.resolve(entry).toString()).append(" ");
            }
            startLocations = deafultStartLocations.toString();
        }
        provisioningOptions.setProperty(AUTO_START_PROP, startLocations);
        System.out.println("Provisioning options are " + provisioningOptions);
        return provisioningOptions;
    }

    public void stop(BundleContext context) throws Exception {
        if (gf != null) {
            gf.stop();
            gf.dispose();
            gf = null;
        }
    }

    public BundleContext getBundleContext() {
        return context;
    }

}