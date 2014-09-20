/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * RuntimeVersion.java
 *
 * Created on March 14, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore;

import com.sun.jdo.api.persistence.support.JDOException;
import org.glassfish.persistence.common.I18NHelper;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.ResourceBundle;

public class RuntimeVersion {
    private static Properties _properties = new Properties();
    private final static ResourceBundle vendor_info = I18NHelper.loadBundle(
            RuntimeVersion.class);


    private static String product_version = "product.version.number"; // NOI18N
    private static String build_time = "product.build.time"; // NOI18N
    private static String runtime_version = "runtime.version.number"; // NOI18N
    private static String vendor_name = "VendorName"; // NOI18N
    private static String version_number = "VersionNumber"; // NOI18N
    private static String vendor = I18NHelper.getMessage(vendor_info, "vendor"); // NOI18N

    public static void main(String[] args) {
        if (args == null || args.length == 0 ||
                (args.length == 1 && args[0].equals("-version"))) // NOI18N
        {
            RuntimeVersion rt = new RuntimeVersion();
            rt.loadProperties("/com/sun/jdo/spi/persistence/support/sqlstore/sys.properties"); // NOI18N
            System.out.println(parse_version());
        }
        System.exit(0);
    }

    /**
     * Constructor without parameters
     */
    public RuntimeVersion() {
    }

    /**
     * Constructor without parameters
     */
    public RuntimeVersion(String fileName) {
        loadProperties(fileName);
    }

    /**
     * Load properties file
     */
    public static void loadProperties(String fileName) {
        try {
            InputStream in = RuntimeVersion.class.getResourceAsStream(fileName);
            if (in == null)
                throw new FileNotFoundException(fileName);

            _properties.load(in);
            in.close();
        } catch (java.io.IOException e) {
            throw new JDOException(null, e);
        }
    }

    /**
     * Return Vendor properties for a given file name
     */
    public static Properties getVendorProperties(String fileName) {
        loadProperties(fileName);
        return getVendorProperties();
    }

    /**
     * Return Vendor properties
     */
    public static Properties getVendorProperties() {
        if (_properties == null)
            return null;

        Properties _vendorProperties = new Properties();
        _vendorProperties.setProperty(vendor_name, vendor);
        _vendorProperties.setProperty(version_number, parse_version());

        return _vendorProperties;
    }

    /**
     * Parse the build date and create a localized version
     * return version as String
     */
    private static String parse_version() {
        if (_properties == null)
            return null;

        String majorVersion = _properties.getProperty(product_version);
        String minorVersion = _properties.getProperty(runtime_version);
        String buildTime = _properties.getProperty(build_time);

        // Parse the build date and create a localized version
        String s = null;
        try {
            DateFormat dateFormatter = DateFormat.getDateTimeInstance();
            SimpleDateFormat propertyFormat = new SimpleDateFormat("MM/dd/yy hh:mm:ss"); // NOI18N
            s = dateFormatter.format(propertyFormat.parse(buildTime));
        } catch (Exception e) {
            s = ""; // NOI18N
        }

        return I18NHelper.getMessage(vendor_info, "fullVersion", majorVersion, minorVersion, s); // NOI18N

    }
}
