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

package com.sun.enterprise.deployment.io;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.node.SaxParserHandlerFactory;
import com.sun.enterprise.deployment.xml.*;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.deployment.common.XModuleType;
import org.glassfish.api.ContractProvider;
import org.jvnet.hk2.annotations.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * Factory for DeploymentDescriptorFile implementations
 *
 * @author Jerome Dochez
 */
@Service
public class DeploymentDescriptorFileFactory implements ContractProvider {
   

    /**
     * Creates and return an appropriate DeploymentDescriptorFile
     * capable of handling the passed descriptor
     *
     * @param descriptor used to identify the associated DeploymentDescriptorFile
     * @return the created DeploymentDescriptorFile
     */
    public static DeploymentDescriptorFile getDDFileFor(RootDeploymentDescriptor descriptor) {
        if (descriptor instanceof Application) {
            return new ApplicationDeploymentDescriptorFile();
        }
        if (descriptor instanceof EjbBundleDescriptor) {
            return new EjbDeploymentDescriptorFile();
        }
        if (descriptor instanceof WebBundleDescriptor) {
            return new WebDeploymentDescriptorFile();
        }
        if (descriptor instanceof ConnectorDescriptor) {
            return new ConnectorDeploymentDescriptorFile();
        } 
        if (descriptor instanceof ApplicationClientDescriptor) {
            return new AppClientDeploymentDescriptorFile();
        }
        return null;
    }
    
    /**
     * Creates and return an appropriate DeploymentDescriptorFile
     * capable of handling the passed descriptor
     *
     * @param descriptor used to identify the associated DeploymentDescriptorFile
     * @return the created DeploymentDescriptorFile
     */
    public static DeploymentDescriptorFile getDDFileFor(XModuleType type) {
        if (type==null) {
            return null;
        }
        if (type.equals(XModuleType.EAR)) {
            return new ApplicationDeploymentDescriptorFile();
        }
        if (type.equals(XModuleType.EJB)) {
            return new EjbDeploymentDescriptorFile();
        }
        if (type.equals(XModuleType.WAR)) {
            return new WebDeploymentDescriptorFile();
        }
        if (type.equals(XModuleType.RAR)) {
            return new ConnectorDeploymentDescriptorFile();
        } 
        if (type.equals(XModuleType.CAR)) {
            return new AppClientDeploymentDescriptorFile();
        }
        return null;
    } 
    
    /**
     * Creates and return the appropriate DeploymentDescriptorFile 
     * depending on the XML file passed.
     * 
     * @param input xml file
     * @return the DeploymentDescriptorFile responsible for handling
     * the xml file
     */
    public static DeploymentDescriptorFile getDDFileFor(File xmlFile) 
        throws Exception {
            
        // this is higly unefficient but we read the xml file as a DOM
        // tree, figure out the top xml element name and return the 
        // appropriate DeploymentDescriptorFile
        
        // always use system default to parse DD, see IT 8229
        ClassLoader currentLoader =
            Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(
            DeploymentDescriptorFileFactory.class.getClassLoader());
        DocumentBuilderFactory factory = null;
        try {
            factory = DocumentBuilderFactory.newInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(currentLoader);
        }

        factory.setValidating(false);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        docBuilder.setEntityResolver(SaxParserHandlerFactory.newInstance());
        Document document = docBuilder.parse(xmlFile);
        Element element = document.getDocumentElement();
        if (element.getTagName().equals(ApplicationTagNames.APPLICATION)) {
            return new ApplicationDeploymentDescriptorFile();
        }
        if (element.getTagName().equals(EjbTagNames.EJB_BUNDLE_TAG)) {
            return new EjbDeploymentDescriptorFile();
        }
        if (element.getTagName().equals(WebTagNames.WEB_BUNDLE)) {
            return new WebDeploymentDescriptorFile();
        }
        if (element.getTagName().equals(ConnectorTagNames.CONNECTOR)) {
            return new ConnectorDeploymentDescriptorFile();
        } 
        if (element.getTagName().equals(ApplicationClientTagNames.APPLICATION_CLIENT_TAG)) {
            return new AppClientDeploymentDescriptorFile();
        }
        if (element.getTagName().equals(PersistenceTagNames.PERSISTENCE)) {
            return new PersistenceDeploymentDescriptorFile();
        }
        return null;
    }
}
