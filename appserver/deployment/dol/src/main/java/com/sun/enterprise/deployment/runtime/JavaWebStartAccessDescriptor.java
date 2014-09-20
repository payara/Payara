/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.runtime;

import com.sun.enterprise.deployment.BundleDescriptor;
import org.glassfish.deployment.common.Descriptor;

/**
 *Records information about Java Web Start access to an app client.
 * @author tjquinn
 */
public class JavaWebStartAccessDescriptor extends Descriptor {
    
    private BundleDescriptor bundleDescriptor;
    
    /** Creates a new instance of JavaWebStartAccessDescriptor */
    public JavaWebStartAccessDescriptor() {
    }

    /**
     * Holds value of property eligible.
     */
    private boolean eligible = true;

    /**
     * Getter for property eligible.
     * @return Value of property eligible.
     */
    public boolean isEligible() {

        return this.eligible;
    }

    /**
     * Setter for property eligible.
     * @param eligible New value of property eligible.
     */
    public void setEligible(boolean eligible) {

        this.eligible = eligible;
    }

    /**
     * Holds value of property contextRoot.
     */
    private String contextRoot = null;

    /**
     * Getter for property contextRoot.
     * @return Value of property contextRoot.
     */
    public String getContextRoot() {

        return this.contextRoot;
    }

    /**
     * Setter for property contextRoot.
     * @param contextRoot New value of property contextRoot.
     */
    public void setContextRoot(String contextRoot) {

        this.contextRoot = contextRoot;
    }
    
    /**
     * Holds value of property vendor.
     */
    private String vendor = null;

    /**
     * Getter for property vendor.
     * @return Value of property vendor.
     */
    public String getVendor() {

        return this.vendor;
    }

    /**
     * Setter for property vendor.
     * @param contextRoot New value of property vendor.
     */
    public void setVendor(String vendor) {

        this.vendor = vendor;
    }

     public void setBundleDescriptor(BundleDescriptor bundle) {
        bundleDescriptor = bundle;
    }

    public BundleDescriptor getBundleDescriptor() {
        return bundleDescriptor;
    }

    /**
     * Declaration and methods for jnlp-doc subelement.
     */
    private String jnlpDoc = null;

    public String getJnlpDocument() {
        return jnlpDoc;
    }

    public void setJnlpDocument(String jnlpDoc) {
        this.jnlpDoc = jnlpDoc;
    }
    

}
