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
 * OptPkgRef.java
 *
 * Created on August 13, 2004, 4:26 PM
 */

package com.sun.enterprise.tools.verifier.apiscan.packaging;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class holds the information for each installed optional package
 * reference. It is used to select the optional package actually referenced.
 * Refer to http://java.sun.com/j2se/1.4.2/docs/guide/extensions/versioning.html#packages
 * for more info.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 * @see Archive
 */
public class ExtensionRef {
    private static String resourceBundleName = "com.sun.enterprise.tools.verifier.apiscan.LocalStrings";
    public static final Logger logger = Logger.getLogger("apiscan.packaging", resourceBundleName); // NOI18N
    private static final String myClassName = "ExtensionRef"; // NOI18N
    // a client can specify dependency using atleast the name or name and any other attr
    private String name, implVendorId = "";
    private String implVer;//See javadocs of java.lang.Package. implVer is a string and not DeweyDecimal
    private DeweyDecimal specVer;

    /**
     * @param manifest Manifest file to be read.
     * @param extName  Name of the extension reference. It is the string that is
     *                 mentioned in the Extension-List manifest attribute.
     */
    public ExtensionRef(Manifest manifest, String extName) {
        Attributes attrs = manifest.getMainAttributes();
        name = attrs.getValue(extName + "-" + Attributes.Name.EXTENSION_NAME); // NOI18N
        String s = attrs.getValue(
                extName + "-" + Attributes.Name.SPECIFICATION_VERSION); // NOI18N
        if (s != null) {
            try {
                specVer = new DeweyDecimal(s);
            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE, getClass().getName() + ".exception1", new Object[]{e.getMessage()});
                logger.log(Level.SEVERE, "", e);
                throw e;
            }
        }
        implVendorId =
                attrs.getValue(
                        extName + "-" + Attributes.Name.IMPLEMENTATION_VENDOR_ID); // NOI18N
        implVer =
                attrs.getValue(
                        extName + "-" + Attributes.Name.IMPLEMENTATION_VERSION); // NOI18N
        validate();
    }

    private void validate() {
        if (name == null || name.length() <= 0) {
            throw new IllegalArgumentException("Extension-Name can not be empty.");
        }
    }

    /**
     * @param another Archive whose specifications will be used for matching.
     * @return true if the other archive meets the specifications of this
     *         extensionRef, else returns false.
     */
    public boolean isSatisfiedBy(Archive another) throws IOException {
        logger.entering(myClassName, "isSatisfiedBy", another); // NOI18N
        Attributes attrs = another.getManifest().getMainAttributes();
        String name = attrs.getValue(Attributes.Name.EXTENSION_NAME);
        String s = attrs.getValue(Attributes.Name.SPECIFICATION_VERSION);
        DeweyDecimal specVer = null;
        try {
            specVer = s != null ? new DeweyDecimal(s) : null;
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, getClass().getName() + ".warning1", new Object[]{e.getMessage(), another.toString()});
            return false;
        }
        String implVendorId = attrs.getValue(
                Attributes.Name.IMPLEMENTATION_VENDOR_ID);
        String implVer = attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        //implVendor is not used for comparision because it is not supposed to be used.
        //See optional package versioning.
        //The order of comparision is very well defined in 
        //http://java.sun.com/j2se/1.4.2/docs/guide/extensions/versioning.html
        //Although that is specified for Java plugins for applets, it is equally
        //applicable for J2EE.
        //See J2EE 1.4 section#8.2
        return this.name.equals(name) &&
                (this.specVer == null || this.specVer.isCompatible(specVer)) &&
                (this.implVendorId == null ||
                this.implVendorId.equals(implVendorId)) &&
                (this.implVer == null || this.implVer.equals(implVer));
    }

    /**
     * Used for pretty printing.
     */
    public String toString() {
        return "Extension-Name: " + name + "\n" + // NOI18N
                (specVer != null ? "Specification-Version: " + specVer + "\n" : "") + // NOI18N
                (implVendorId != null ?
                "Implementation-Vendor-Id: " + implVendorId + "\n" : "") + // NOI18N
                (implVer != null ? "Implementation-Version: " + implVer + "\n" : ""); // NOI18N
    }
}
