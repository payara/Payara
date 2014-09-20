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

package org.glassfish.resources.admin.cli;

import com.sun.enterprise.config.serverbeans.Resources;
import org.glassfish.resources.api.Resource;
import org.glassfish.resourcebase.resources.api.ResourceStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Properties;
import java.io.File;

/**
 * This class serves as the API to creating new resources when an xml file 
 * is supplied containing the resource definitions
 * 
 * @author PRASHANTH ABBAGANI
 */
public class ResourcesManager {

     /**
     * Creating resources from sun-resources.xml file. This method is used by 
     * the admin framework when the add-resources command is used to create
     * resources
     */
    public static ArrayList createResources(Resources resources, File resourceXMLFile,
            String target, org.glassfish.resources.admin.cli.ResourceFactory resourceFactory) throws Exception {
        ArrayList results = new ArrayList();
        org.glassfish.resources.admin.cli.ResourcesXMLParser resourcesParser =
            new org.glassfish.resources.admin.cli.ResourcesXMLParser(resourceXMLFile);
        List<Resource> vResources = resourcesParser.getResourcesList();
        //First add all non connector resources.
        Iterator<Resource> nonConnectorResources = org.glassfish.resources.admin.cli.ResourcesXMLParser.getNonConnectorResourcesList(vResources, false, false).iterator();
        while (nonConnectorResources.hasNext()) {
            Resource resource = (Resource) nonConnectorResources.next();
            HashMap attrList = resource.getAttributes();
            String desc = resource.getDescription();
            if (desc != null)
                attrList.put("description", desc);

            Properties props = resource.getProperties();

            ResourceStatus rs;
            try {
                org.glassfish.resources.admin.cli.ResourceManager rm = resourceFactory.getResourceManager(resource);
                rs = rm.create(resources, attrList, props, target);
            } catch (Exception e) {
                String msg = e.getMessage();
                rs = new ResourceStatus(ResourceStatus.FAILURE, msg);
            }
            results.add(rs);
        }

        //Now add all connector resources
        Iterator connectorResources = org.glassfish.resources.admin.cli.ResourcesXMLParser.getConnectorResourcesList(vResources, false, false).iterator();
        while (connectorResources.hasNext()) {
            Resource resource = (Resource) connectorResources.next();
            HashMap attrList = resource.getAttributes();
            String desc = resource.getDescription();
            if (desc != null)
                attrList.put("description", desc);

            Properties props = resource.getProperties();

            ResourceStatus rs;
            try {
                org.glassfish.resources.admin.cli.ResourceManager rm = resourceFactory.getResourceManager(resource);
                rs = rm.create(resources, attrList, props, target);
            } catch (Exception e) {
                String msg = e.getMessage();
                rs = new ResourceStatus(ResourceStatus.FAILURE, msg);
            }
            results.add(rs);
        }

        return results;
    }

}
