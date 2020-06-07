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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.apache.catalina.util;


import java.util.StringTokenizer;


/**
 * Utility class that represents either an available "Optional Package"
 * (formerly known as "Standard Extension") as described in the manifest
 * of a JAR file, or the requirement for such an optional package.  It is
 * used to support the requirements of the Servlet Specification, version
 * 2.3, related to providing shared extensions to all webapps.
 * <p>
 * In addition, static utility methods are available to scan a manifest
 * and return an array of either available or required optional modules
 * documented in that manifest.
 * <p>
 * For more information about optional packages, see the document
 * <em>Optional Package Versioning</em> in the documentation bundle for your
 * Java2 Standard Edition package, in file
 * <code>guide/extensions/versioning.html</code>.
 *
 * @author Craig McClanahan
 * @author Justyna Horwat
 * @author Greg Murray
 * @version $Revision: 1.3 $ $Date: 2006/11/06 20:14:21 $
 */

public final class Extension {


    // ------------------------------------------------------------- Properties


    /**
     * The name of the optional package being made available, or required.
     */
    private String extensionName = null;
    

    public String getExtensionName() {
        return (this.extensionName);
    }

    public void setExtensionName(String extensionName) {
        if (extensionName != null) {
            this.extensionName = extensionName.trim();
        }
    }

    /**
     * UniqueId created by combining the extension name and implementation
     * version. 
     */
    public String getUniqueId() {
        return this.extensionName + this.implementationVersion;
    }

    /**
     * The URL from which the most recent version of this optional package
     * can be obtained if it is not already installed.
     */
    private String implementationURL = null;

    public String getImplementationURL() {
        return (this.implementationURL);
    }

    public void setImplementationURL(String implementationURL) {
        this.implementationURL = implementationURL;
    }


    /**
     * The name of the company or organization that produced this
     * implementation of this optional package.
     */
    private String implementationVendor = null;

    public String getImplementationVendor() {
        return (this.implementationVendor);
    }

    public void setImplementationVendor(String implementationVendor) {
        this.implementationVendor = implementationVendor;
    }


    /**
     * The unique identifier of the company that produced the optional
     * package contained in this JAR file.
     */
    private String implementationVendorId = null;

    public String getImplementationVendorId() {
        return (this.implementationVendorId);
    }

    public void setImplementationVendorId(String implementationVendorId) {
        this.implementationVendorId = implementationVendorId;
    }


    /**
     * The version number (dotted decimal notation) for this implementation
     * of the optional package.
     */
    private String implementationVersion = null;

    public String getImplementationVersion() {
        return (this.implementationVersion);
    }

    public void setImplementationVersion(String implementationVersion) {
        if (implementationVersion != null) {
            this.implementationVersion = implementationVersion.trim();
        }
    }


    /**
     * The name of the company or organization that originated the
     * specification to which this optional package conforms.
     */
    private String specificationVendor = null;

    public String getSpecificationVendor() {
        return (this.specificationVendor);
    }

    public void setSpecificationVendor(String specificationVendor) {
        this.specificationVendor = specificationVendor;
    }


    /**
     * The version number (dotted decimal notation) of the specification
     * to which this optional package conforms.
     */
    private String specificationVersion = null;

    public String getSpecificationVersion() {
        return (this.specificationVersion);
    }

    public void setSpecificationVersion(String specificationVersion) {
        this.specificationVersion = specificationVersion;
    }


    /**
     * fulfilled is true if all the required extension dependencies have been
     * satisfied
     */
    private boolean fulfilled = false;

    public void setFulfilled(boolean fulfilled) {
        this.fulfilled = fulfilled;
    }
    
    public boolean isFulfilled() {
        return fulfilled;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Return <code>true</code> if the specified <code>Extension</code>
     * (which represents an optional package required by this application)
     * is satisfied by this <code>Extension</code> (which represents an
     * optional package that is already installed.  Otherwise, return
     * <code>false</code>.
     *
     * @param required Extension of the required optional package
     */
    public boolean isCompatibleWith(Extension required) {

        // Extension Name must match
        if (extensionName == null)
            return (false);
        if (!extensionName.equals(required.getExtensionName()))
            return (false);

        // Available specification version must be >= required
        if (!isNewer(specificationVersion, required.getSpecificationVersion()))
            return (false);

        // Implementation Vendor ID must match
        if (implementationVendorId == null)
            return (false);
        if (!implementationVendorId.equals(required.getImplementationVendorId()))
            return (false);

        // Implementation version must be >= required
        if (!isNewer(implementationVersion, required.getImplementationVersion()))
            return (false);

        // This available optional package satisfies the requirements
        return (true);

    }

    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("Extension[");
        sb.append(extensionName);
        if (implementationURL != null) {
            sb.append(", implementationURL=");
            sb.append(implementationURL);
        }
        if (implementationVendor != null) {
            sb.append(", implementationVendor=");
            sb.append(implementationVendor);
        }
        if (implementationVendorId != null) {
            sb.append(", implementationVendorId=");
            sb.append(implementationVendorId);
        }
        if (implementationVersion != null) {
            sb.append(", implementationVersion=");
            sb.append(implementationVersion);
        }
        if (specificationVendor != null) {
            sb.append(", specificationVendor=");
            sb.append(specificationVendor);
        }
        if (specificationVersion != null) {
            sb.append(", specificationVersion=");
            sb.append(specificationVersion);
        }
        sb.append("]");
        return (sb.toString());

    }


    // -------------------------------------------------------- Private Methods



    /**
     * Return <code>true</code> if the first version number is greater than
     * or equal to the second; otherwise return <code>false</code>.
     *
     * @param first First version number (dotted decimal)
     * @param second Second version number (dotted decimal)
     *
     * @exception NumberFormatException on a malformed version number
     */
    private boolean isNewer(String first, String second)
        throws NumberFormatException {

        if ((first == null) || (second == null))
            return (false);
        if (first.equals(second))
            return (true);

        StringTokenizer fTok = new StringTokenizer(first, ".", true);
        StringTokenizer sTok = new StringTokenizer(second, ".", true);
        int fVersion = 0;
        int sVersion = 0;
        while (fTok.hasMoreTokens() || sTok.hasMoreTokens()) {
            if (fTok.hasMoreTokens())
                fVersion = Integer.parseInt(fTok.nextToken());
            else
                fVersion = 0;
            if (sTok.hasMoreTokens())
                sVersion = Integer.parseInt(sTok.nextToken());
            else
                sVersion = 0;
            if (fVersion < sVersion)
                return (false);
            else if (fVersion > sVersion)
                return (true);
            if (fTok.hasMoreTokens())   // Swallow the periods
                fTok.nextToken();
            if (sTok.hasMoreTokens())
                sTok.nextToken();
        }

        return (true);  // Exact match

    }


}
