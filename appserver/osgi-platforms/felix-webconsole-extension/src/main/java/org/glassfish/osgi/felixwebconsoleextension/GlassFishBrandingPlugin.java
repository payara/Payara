/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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


package org.glassfish.osgi.felixwebconsoleextension;

import org.apache.felix.webconsole.BrandingPlugin;
import org.apache.felix.webconsole.DefaultBrandingPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a customization of {@link BrandingPlugin} for GlassFish.
 *
 * If a properties file <code>META-INF/webconsole.properties</code> is available
 * through the class loader of this class, the properties overwrite the default
 * settings according to the property names listed in {@link BrandingPlugin}.
 * The easiest way to add such a properties file is to provide a fragment bundle with the file.
 *
 * @author sanjeeb.sahoo@oracle.com
 */
public class GlassFishBrandingPlugin implements BrandingPlugin {
    private Logger logger = Logger.getLogger(getClass().getPackage().getName());

    private final String brandName;
    private final String prouctName;
    private final String productImage;
    private final String productUrl;
    private final String vendorName;
    private final String vendorUrl;
    private final String vendorImage;
    private final String favIcon;
    private final String mainStyleSheet;

    // default values
    private static final String NAME = "GlassFish OSGi Administration Console";
    private static final String PROD_NAME = "GlassFish Server";
    private static final String PROD_IMAGE = "http://java.net/projects/glassfish/sources/svn/content/trunk/main/appserver/osgi-platforms/felix-webconsole-extension/src/main/resources/res/glassfish/logo.png";
    private static final String PROD_URL = "http://GlassFish.org";
    private static final String VENDOR = "GlassFish community";
    private static final String VENDOR_URL = PROD_URL;
    private static final String VENDOR_IMAGE = PROD_IMAGE;

    // This is where we look for any custom/localized branding information Must be made available via a fragment
    private String path = "/META-INF/webconsole.properties";
    Properties branding = new Properties();

    public GlassFishBrandingPlugin() {
        InputStream inStream = getClass().getResourceAsStream(path);
        if (inStream != null) {
            try {
                branding.load(inStream);
                logger.logp(Level.INFO, "GlassFishBrandingPlugin", "GlassFishBrandingPlugin", "branding = {0}",
                        new Object[]{branding});
            } catch (IOException e) {
                logger.logp(Level.INFO, "GlassFishBrandingPlugin", "GlassFishBrandingPlugin",
                        "Failed to read properties file", e);
                // we will use defaults if we fail here
            } finally {
                try {
                    inStream.close();
                } catch (IOException e) {
                }
            }
        }

        brandName = getBranding().getProperty("webconsole.brand.name", NAME);
        prouctName = getBranding().getProperty("webconsole.product.name", PROD_NAME);
        productImage = getBranding().getProperty("webconsole.product.image", PROD_IMAGE);
        productUrl = getBranding().getProperty("webconsole.product.url", PROD_URL);
        vendorName = getBranding().getProperty("webconsole.vendor.name", VENDOR);
        vendorUrl = getBranding().getProperty("webconsole.vendor.url", VENDOR_URL);
        vendorImage = getBranding().getProperty("webconsole.vendor.image", VENDOR_IMAGE);
        // we don't have our own default
        favIcon = getBranding().getProperty("webconsole.favicon", getDefaultPlugin().getFavIcon());
        // we don't have our own default
        mainStyleSheet = getBranding().getProperty("webconsole.stylesheet", getDefaultPlugin().getMainStyleSheet());
    }

    private DefaultBrandingPlugin getDefaultPlugin() {
        return DefaultBrandingPlugin.getInstance();
    }

    @Override
    public String getBrandName() {
        return brandName;
    }

    @Override
    public String getProductName() {
        return prouctName;
    }

    @Override
    public String getProductURL() {
        return productUrl;
    }

    @Override
    public String getProductImage() {
        return productImage;
    }

    @Override
    public String getVendorName() {
        return vendorName;
    }

    @Override
    public String getVendorURL() {
        return vendorUrl;
    }

    @Override
    public String getVendorImage() {
        return vendorImage;
    }

    @Override
    public String getFavIcon() {
        return favIcon;
    }

    @Override
    public String getMainStyleSheet() {
        return mainStyleSheet;
    }

    public Properties getBranding() {
        return branding;
    }
}
