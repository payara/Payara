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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.deployment.io.runtime;

import org.glassfish.deployment.common.Descriptor;
import org.glassfish.hk2.api.PerLookup;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFileFor;
import com.sun.enterprise.deployment.io.DescriptorConstants;
import com.sun.enterprise.deployment.node.RootXMLNode;
import com.sun.enterprise.deployment.node.runtime.application.gf.GFApplicationRuntimeNode;
import com.sun.enterprise.deployment.EarType;
import org.jvnet.hk2.annotations.Service;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for handling the XML configuration information for the Glassfish Application Container
 */
@ConfigurationDeploymentDescriptorFileFor(EarType.ARCHIVE_TYPE)
@PerLookup
@Service
public class GFApplicationRuntimeDDFile extends ConfigurationDeploymentDescriptorFile {

    /**
     * @return the location of the DeploymentDescriptor file for a particular type of EE Archive
     */
    public String getDeploymentDescriptorPath() {
        return DescriptorConstants.GF_APPLICATION_JAR_ENTRY;
    }

    /**
     * @return a RootXMLNode responsible for handling the deployment descriptors associated with this J2EE module
     *
     * @param the descriptor for which we need the node
     */
    public RootXMLNode getRootXMLNode(Descriptor descriptor) {
        if (descriptor instanceof Application) {
            return new GFApplicationRuntimeNode((Application) descriptor);
        }

        return null;
    }

    /**
     * Register the root node for this runtime deployment descriptor file in the root nodes map, and also in the dtd map
     * which will be used for dtd validation.
     *
     * @param rootNodesMap the map for storing all the root nodes
     * @param publicIDToDTDMap the map for storing public id to dtd mapping
     * @param versionUpgrades The list of upgrades from older versions
     */
    public void registerBundle(Map<String, Class<?>> rootNodesMap, Map<String, String> publicIDToDTDMap, Map<String, List<Class<?>>> versionUpgrades) {
        rootNodesMap.put(GFApplicationRuntimeNode.registerBundle(publicIDToDTDMap), GFApplicationRuntimeNode.class);
    }
}
