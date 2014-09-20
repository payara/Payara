/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.deployment.io;

import org.glassfish.deployment.common.Descriptor;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.node.runtime.GFEjbBundleRuntimeNode;
import org.glassfish.hk2.api.PerLookup;

import org.jvnet.hk2.annotations.Service;

import com.sun.ejb.containers.EjbContainerUtil;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFileFor;
import com.sun.enterprise.deployment.io.DescriptorConstants;
import com.sun.enterprise.deployment.node.RootXMLNode;
import com.sun.enterprise.deployment.util.DOLUtils;

/**
 * This class is responsible for handling the XML configuration information
 * for the Glassfish EJB Container
 */
@ConfigurationDeploymentDescriptorFileFor(EjbContainerUtil.EJB_CONTAINER_NAME)
@Service
@PerLookup
public class GFEjbRuntimeDDFile extends ConfigurationDeploymentDescriptorFile {
    /**
     * @return the location of the DeploymentDescriptor file for a
     *         particular type of J2EE Archive
     */
    public String getDeploymentDescriptorPath() {
        return DOLUtils.warType().equals(getArchiveType()) ?
        		DescriptorConstants.GF_EJB_IN_WAR_ENTRY : DescriptorConstants.GF_EJB_JAR_ENTRY;
    }

    /**
     * @param descriptor the descriptor for which we need the node
     * @return a RootXMLNode responsible for handling the deployment
     *         descriptors associated with this J2EE module
     */
    public RootXMLNode<EjbBundleDescriptorImpl> getRootXMLNode(Descriptor descriptor) {
        if (descriptor instanceof EjbBundleDescriptorImpl) {
            return new GFEjbBundleRuntimeNode((EjbBundleDescriptorImpl) descriptor);
        }
        return null;
    }
}
