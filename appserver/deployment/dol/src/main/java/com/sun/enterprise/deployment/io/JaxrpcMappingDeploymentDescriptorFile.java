/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.io;

import com.sun.enterprise.deployment.JaxrpcMappingDescriptor;
import com.sun.enterprise.deployment.node.JaxrpcMappingDescriptorNode;
import com.sun.enterprise.deployment.node.RootXMLNode;

/**
 * This class is responsible for handling the 
 * JSR 109 jaxrpc mapping deployment descriptor
 *
 * @author Kenneth Saks
 */
public class JaxrpcMappingDeploymentDescriptorFile extends 
    DeploymentDescriptorFile<JaxrpcMappingDescriptor> {

    String mappingFilePath = null;
    
    public JaxrpcMappingDeploymentDescriptorFile() {
    }
    

    /** 
     * @return the location of the DeploymentDescriptor file for a 
     * particular type of J2EE Archive
     */
    public String getDeploymentDescriptorPath() {
        // writing not supported.  always copied from input jar.
        return mappingFilePath;
    }
    
    /**
     * Sets the mapping file location in the source archive
     */
    public void setDeploymentDescriptorPath(String path) {
        this.mappingFilePath = path;
    }

    /**
     * @return a RootXMLNode responsible for handling the deployment
     * descriptors associated with this J2EE module
     *
     * @param descriptor ignored
     */
    public RootXMLNode getRootXMLNode(JaxrpcMappingDescriptor descriptor) {
        return new JaxrpcMappingDescriptorNode();
    }    
}
