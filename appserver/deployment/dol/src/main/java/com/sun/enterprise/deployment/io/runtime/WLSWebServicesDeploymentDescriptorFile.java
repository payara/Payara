/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.io.runtime;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.node.RootXMLNode;
import com.sun.enterprise.deployment.node.ws.WLDescriptorConstants;
import com.sun.enterprise.deployment.node.ws.WLWebServicesDescriptorNode;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.RootDeploymentDescriptor;

import java.util.Vector;
import java.io.OutputStream;
import java.io.IOException;

/**
 * This class is responsible for handling the WebLogic webservices deployment descriptor.
 * This file weblogic-webservices.xml complements JSR-109 defined webservices.xml
 * to define extra configuration.
 *
 * @author Rama Pulavarthi
 */
public class WLSWebServicesDeploymentDescriptorFile extends ConfigurationDeploymentDescriptorFile {
    private String descriptorPath;

    public WLSWebServicesDeploymentDescriptorFile(RootDeploymentDescriptor desc) {
        if (desc instanceof WebServicesDescriptor) {
            descriptorPath = (((WebServicesDescriptor)desc).getBundleDescriptor().getModuleType().equals(DOLUtils.warType())) ?
                WLDescriptorConstants.WL_WEB_WEBSERVICES_JAR_ENTRY : WLDescriptorConstants.WL_EJB_WEBSERVICES_JAR_ENTRY;
        } else if (desc instanceof WebBundleDescriptor) {
            descriptorPath = WLDescriptorConstants.WL_WEB_WEBSERVICES_JAR_ENTRY;
        } else if (desc instanceof EjbBundleDescriptor) {
            descriptorPath = WLDescriptorConstants.WL_EJB_WEBSERVICES_JAR_ENTRY;
        }
    }

    @Override
    public String getDeploymentDescriptorPath() {
        return descriptorPath;
    }

    public static Vector getAllDescriptorPaths() {
        Vector allDescPaths = new Vector();
        allDescPaths.add(WLDescriptorConstants.WL_WEB_WEBSERVICES_JAR_ENTRY);
        allDescPaths.add(WLDescriptorConstants.WL_EJB_WEBSERVICES_JAR_ENTRY);

        return allDescPaths;
    }

    @Override
    public RootXMLNode getRootXMLNode(Descriptor descriptor) {
        if (descriptor instanceof WebServicesDescriptor) {
            return new WLWebServicesDescriptorNode((WebServicesDescriptor) descriptor);
        }
        return null;
    }

    /**
     * writes the descriptor to an output stream
     *
     * @param descriptor the descriptor
     * @param os the output stream
     */
    @Override
    public void write(Descriptor descriptor, OutputStream os) throws IOException {
        if (descriptor instanceof BundleDescriptor) {
            BundleDescriptor bundleDesc = (BundleDescriptor)descriptor;
            if (bundleDesc.hasWebServices()) {
                super.write(bundleDesc.getWebServices(), os);
            }
        }
    }
    
  /**
   * Return whether this configuration file can be validated.
   * @return whether this configuration file can be validated.
   */
  public boolean isValidating() {
    return true;
  }
}
