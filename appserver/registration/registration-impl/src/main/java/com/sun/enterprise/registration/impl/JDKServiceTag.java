/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.registration.impl;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import java.io.IOException;

/**
 * JDKServiceTag class is defined for Glassfish to register JDK 6u4
 * which does not have the product registration support. 
 * 
 * Glassfish 9.1 U1 will include the product registration support
 * but the JDK product registration support is not released in time
 * so that the JDK 6u4 bundled in Glassfish 9.1 U1 doesn't have 
 * the interface for Glassfish to use.
 * 
 * This class is created to be included in the Glassfish source
 * as an interim solution until the JDK supports the product registration
 * and Glassfish will make the change to use the exported JDK interface.
 */
public class JDKServiceTag {

    // Service Tag Node names
    private final static String ST_NODE_SERVICE_TAG = "service_tag";
    private final static String ST_NODE_INSTANCE_URN = "instance_urn";
    private final static String ST_NODE_PRODUCT_NAME = "product_name";
    private final static String ST_NODE_PRODUCT_VERSION = "product_version";
    private final static String ST_NODE_PRODUCT_URN = "product_urn";
    private final static String ST_NODE_PRODUCT_PARENT_URN = "product_parent_urn";
    private final static String ST_NODE_PRODUCT_PARENT = "product_parent";
    private final static String ST_NODE_PRODUCT_DEFINED_INST_ID = "product_defined_inst_id";
    private final static String ST_NODE_PRODUCT_VENDOR = "product_vendor";
    private final static String ST_NODE_PLATFORM_ARCH = "platform_arch";
    private final static String ST_NODE_CONTAINER = "container";
    private final static String ST_NODE_SOURCE = "source";

    // Swordfish entry for JDK 6
    private final static String JDK_6_URN = "urn:uuid:b58ef9a8-5ae8-11db-a023-080020a9ed93";
    private final static String JDK_6_NAME = "Java SE 6 Development Kit";
    private final static String JRE_6_URN = "urn:uuid:92d1de8c-1e59-42c6-a280-1c379526bcbc";
    private final static String JRE_6_NAME = "Java SE 6 Runtime Environment";
    private final static String PARENT_6_URN = "urn:uuid:fdc90b21-018d-4cab-b866-612c7c119ed3";
    private final static String PARENT_6_NAME = "Java Platform Standard Edition 6 (Java SE 6)";

    // Swordfish entry for JDK 5
    private final static String JDK_5_URN = "urn:uuid:d5bed446-05f2-42ed-ba0a-153105a52413";
    private final static String JDK_5_NAME = "J2SE 5.0 Development Kit";
    private final static String JRE_5_URN = "urn:uuid:5c6686aa-fd05-46a6-ba3e-700e2d5f7043";
    private final static String JRE_5_NAME = "J2SE 5.0 Runtime Environment";
    private final static String PARENT_5_URN = "urn:uuid:f3c20172-557a-11d7-93d0-d6a41ea318df";
    private final static String PARENT_5_NAME = "Java 2 Platform, Standard Edition 5.0";

    private JDKServiceTag() {
    }

    /**
     * Returns a (@code Properties) object containing the required fields
     * for creating the service tag for Java SE.  The key will be the 
     * element tag name defined in the Service Tags schema.
     * This method returns {@code null} if not supported.
     * 
     * @param bundleOwner the product name of the bundle owner
     * 
     * @throw NullPointerException if bundleOwner is null.
     * @throw IllegalArgumentException if the input arugment is null or empty.
     */
    public static Properties getServiceTagProperties(String bundleOwner) {
        if (bundleOwner.length() == 0) {
            throw new IllegalArgumentException("Empty bundleOwner value");
        }
        // only support JDK 5 and JDK 6
        int version = getJdkVersion();
        if (version != 5 && version != 6) {
            return null;
        }

        Properties svcTagProps = new Properties();

        String javaHome = System.getProperty("java.home");
        boolean isJdk;
        String jrePath;
        // Determine the JRE path by checking the existence of
        // <HOME>/jre/lib and <HOME>/lib.
        jrePath = javaHome + File.separator + "jre";
        File f = new File(jrePath, "lib");
        if (!f.exists()) {
            // java.home usually points to the JRE path
            jrePath = javaHome;
        }

        // <HOME>/jre exists which implies it's a JDK
        isJdk = jrePath.endsWith(File.separator + "jre");

        // Determine the product URN and name
        String productURN;
        String productName;
        if (isJdk) {
            // <HOME>/jre exists which implies it's a JDK
            productURN = (version == 5 ? JDK_5_URN : JDK_6_URN);
            productName = (version == 5 ? JDK_5_NAME : JDK_6_NAME);
        } else {
            // Otherwise, it's a JRE
            productURN = (version == 5 ? JRE_5_URN : JRE_6_URN);
            productName = (version == 5 ? JRE_5_NAME : JRE_6_NAME);
        }
        String parentURN = (version == 5 ? PARENT_5_URN : PARENT_6_URN);
        String parentName = (version == 5 ? PARENT_5_NAME : PARENT_6_NAME);

        // The product defined instance ID has a 256 characters limit
        String definedId = getProductDefinedId(jrePath);
        if (definedId.length() > 256) {
            System.err.println("Product defined instance ID exceeds the field limit:");
            System.err.println(definedId);
        }
        svcTagProps.setProperty(ST_NODE_PRODUCT_NAME, productName);
        svcTagProps.setProperty(ST_NODE_PRODUCT_VERSION,
                                System.getProperty("java.version"));
        svcTagProps.setProperty(ST_NODE_PRODUCT_URN, productURN);
        svcTagProps.setProperty(ST_NODE_PRODUCT_PARENT_URN, parentURN);
        svcTagProps.setProperty(ST_NODE_PRODUCT_PARENT, parentName);
        svcTagProps.setProperty(ST_NODE_PRODUCT_DEFINED_INST_ID, definedId);
        svcTagProps.setProperty(ST_NODE_PRODUCT_VENDOR, "Sun Microsystems");
        svcTagProps.setProperty(ST_NODE_PLATFORM_ARCH,
                                System.getProperty("os.arch"));
        // Glassfish 9.1 U1 sets the container to "global"
        // So JDK uses the same for simplicity.
        svcTagProps.setProperty(ST_NODE_CONTAINER, "global");
        svcTagProps.setProperty(ST_NODE_SOURCE, bundleOwner);
        return svcTagProps;
    }


    /**
     * Returns the product defined instance ID for Java SE.
     * It is a list of comma-separated name/value pairs:
     *    "id=<full-version>  <arch> [<arch>]*"
     *    "dir=<java.home system property value>"
     *
     * where <full-version> is the full version string of the JRE,
     *       <arch> is the architecture that the runtime supports
     *       (i.e. "sparc", "sparcv9", "i386", "amd64" (ISA list))
     *
     * For Solaris, it can be dual mode that can support both
     * 32-bit and 64-bit. the "id" will be set to
     *     "1.6.0_03-b02 sparc sparcv9"
     * 
     * The "dir" property is included in the service tag to enable
     * the Service Tag software to determine if a service tag for 
     * Java SE is invalid and perform appropriate service tag
     * cleanup if necessary.  See RFE# 6574781 Service Tags Enhancement. 
     *
     */
    private static String getProductDefinedId(String jrePath) {
        StringBuilder definedId = new StringBuilder();
        definedId.append("id=");
        definedId.append(System.getProperty("java.runtime.version"));

        // Traverse the directories under <JRE>/lib. 
        // If <JRE>/lib/<arch>/libjava.so exists, add <arch>
        // to the product defined ID 
        File dir = new File(jrePath + File.separator + "lib");
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String name : children) {
                File f = new File(dir, name + File.separator + "libjava.so");
                if (f.exists()) {
                    definedId.append(" " + name);
                }

            }
        }

        definedId.append(",dir=");
        definedId.append(System.getProperty("java.home"));
        return definedId.toString();
    }

    private static int jdkVersion = 0;
    private static synchronized int getJdkVersion() {
        if (jdkVersion > 0) {
            return jdkVersion;
        }

        // parse java.runtime.version
        // valid format of the version string is:
        // n.n.n[_uu[c]][-<identifer>]-bxx
        CharSequence cs = System.getProperty("java.runtime.version");
        if (cs.length() >= 5 &&
            Character.isDigit(cs.charAt(0)) && cs.charAt(1) == '.' &&
            Character.isDigit(cs.charAt(2)) && cs.charAt(3) == '.' &&
            Character.isDigit(cs.charAt(4))) {
            jdkVersion = Character.digit(cs.charAt(2), 10);
        }
        return jdkVersion;
    }
}
