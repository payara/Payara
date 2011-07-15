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

package com.sun.enterprise.deployment.node.appclient;

import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.node.*;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.ApplicationClientTagNames;
import com.sun.enterprise.deployment.xml.EjbTagNames;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

import java.util.*;

/**
 * This class is responsible for handling app clients
 *
 * @author  Sheetal Vartak
 * @version 
 */
public class AppClientNode extends BundleNode<ApplicationClientDescriptor> {

     // Descriptor class we are using   
    private ApplicationClientDescriptor descriptor; 

    //app client 1.2
    public final static String PUBLIC_DTD_ID_12 = "-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.2//EN";
    public final static String SYSTEM_ID_12 = "http://java.sun.com/dtd/application-client_1_2.dtd";
    
    //app client 1.3
    public final static String PUBLIC_DTD_ID = "-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.3//EN";
    public final static String SYSTEM_ID = "http://java.sun.com/dtd/application-client_1_3.dtd";
    
    public final static String SCHEMA_ID_14 = "application-client_1_4.xsd";
    
    public final static String SCHEMA_ID_15 = "application-client_5.xsd";
    public final static String SCHEMA_ID = "application-client_6.xsd";
    public final static String SPEC_VERSION = "6";
    private final static List<String> systemIDs = initSystemIDs();
 
    public final static XMLElement tag = new XMLElement(ApplicationClientTagNames.APPLICATION_CLIENT_TAG);

    private static List<String> initSystemIDs() {
        final ArrayList<String> systemIDs = new ArrayList<String>();
        systemIDs.add(SCHEMA_ID);
        systemIDs.add(SCHEMA_ID_14);
        systemIDs.add(SCHEMA_ID_15);
        return Collections.unmodifiableList(systemIDs);
    }
    
    /** Creates new AppClientNode */
    public AppClientNode() {
	registerElementHandler(new XMLElement(TagNames.ENVIRONMENT_PROPERTY), 
                                                             EnvEntryNode.class, "addEnvironmentProperty");     
        registerElementHandler(new XMLElement(EjbTagNames.EJB_REFERENCE), EjbReferenceNode.class);     
        registerElementHandler(new XMLElement(EjbTagNames.EJB_LOCAL_REFERENCE), EjbLocalReferenceNode.class);     
        registerElementHandler(new XMLElement(WebServicesTagNames.SERVICE_REF), ServiceReferenceNode.class, "addServiceReferenceDescriptor");     
        registerElementHandler(new XMLElement(EjbTagNames.RESOURCE_REFERENCE), 
                                                             ResourceRefNode.class, "addResourceReferenceDescriptor");   
	registerElementHandler(new XMLElement(TagNames.RESOURCE_ENV_REFERENCE), 
                                                            ResourceEnvRefNode.class, "addJmsDestinationReferenceDescriptor");               
        registerElementHandler(new XMLElement(TagNames.MESSAGE_DESTINATION_REFERENCE), MessageDestinationRefNode.class, "addMessageDestinationReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.PERSISTENCE_UNIT_REF), EntityManagerFactoryReferenceNode.class, "addEntityManagerFactoryReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.MESSAGE_DESTINATION),
                               MessageDestinationNode.class,
                               "addMessageDestination");
        registerElementHandler(new XMLElement(TagNames.POST_CONSTRUCT), LifecycleCallbackNode.class, "addPostConstructDescriptor");
        registerElementHandler(new XMLElement(TagNames.PRE_DESTROY), LifecycleCallbackNode.class, "addPreDestroyDescriptor");
        registerElementHandler(new XMLElement(TagNames.DATA_SOURCE), DataSourceDefinitionNode.class, "addDataSourceDefinitionDescriptor");
    }

    /**
     * register this node as a root node capable of loading entire DD files
     * 
     * @param publicIDToDTD is a mapping between xml Public-ID to DTD 
     * @return the doctype tag name
     */
    public static String registerBundle(Map publicIDToDTD) {
        publicIDToDTD.put(PUBLIC_DTD_ID, SYSTEM_ID);
        publicIDToDTD.put(PUBLIC_DTD_ID_12, SYSTEM_ID_12);
        return tag.getQName();
   }

    /**
     * Adds  a new DOL descriptor instance to the descriptor instance associated with 
     * this XMLNode
     *
     * @param descriptor the new descriptor
     */    
    public void addDescriptor(Object  newDescriptor) {       
        if (newDescriptor instanceof EjbReference) {            
            DOLUtils.getDefaultLogger().fine("Adding ejb ref " + newDescriptor);
            (getDescriptor()).addEjbReferenceDescriptor(
                        (EjbReference) newDescriptor);
        } else {
            super.addDescriptor(newDescriptor);
        }
    }      
    
    public ApplicationClientDescriptor getDescriptor() {
        if (descriptor == null) {
            descriptor = (ApplicationClientDescriptor) DescriptorFactory.getDescriptor(getXMLPath());
        } 
        return descriptor;
    }
    
    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */    
    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();
	table.put(ApplicationClientTagNames.CALLBACK_HANDLER, "setCallbackHandler");        
        return table;
    }    
        
    /**
     * @return the XML tag associated with this XMLNode
     */
    protected XMLElement getXMLRootTag() {
        return tag;
    }

     /**
     * @return the DOCTYPE of the XML file
     */
    public String getDocType() {
        return null;
    }
    
    /**
     * @return the SystemID of the XML file
     */
    public String getSystemID() {
        return SCHEMA_ID;
    }

    /**
     * @return the list of SystemID of the XML schema supported
     */
    public List<String> getSystemIDs() {
        return systemIDs;
    }

    public void setElementValue(XMLElement element, String value) {
        if (TagNames.MODULE_NAME.equals(element.getQName())) {
            getDescriptor().getModuleDescriptor().setModuleName(value);
        } else {
            super.setElementValue(element, value);
        }
    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, 
        ApplicationClientDescriptor appclientDesc) {
        Node appclientNode = super.writeDescriptor(parent, appclientDesc);      

	// env-entry*
        writeEnvEntryDescriptors(appclientNode, appclientDesc.getEnvironmentProperties().iterator());
        
        // ejb-ref * and ejb-local-ref*
        writeEjbReferenceDescriptors(appclientNode, appclientDesc.getEjbReferenceDescriptors().iterator());

        // service-ref*
        writeServiceReferenceDescriptors(appclientNode, appclientDesc.getServiceReferenceDescriptors().iterator());

        // resource-ref*
        writeResourceRefDescriptors(appclientNode, appclientDesc.getResourceReferenceDescriptors().iterator());
        
        // resource-env-ref*
        writeResourceEnvRefDescriptors(appclientNode, appclientDesc.getJmsDestinationReferenceDescriptors().iterator());

        // message-destination-ref*
        writeMessageDestinationRefDescriptors(appclientNode, appclientDesc.getMessageDestinationReferenceDescriptors().iterator());

        // persistence-unit-ref*
        writeEntityManagerFactoryReferenceDescriptors(appclientNode, appclientDesc.getEntityManagerFactoryReferenceDescriptors().iterator());

        // post-construct
        writePostConstructDescriptors(appclientNode, appclientDesc.getPostConstructDescriptors().iterator());
        
        // pre-destroy
        writePreDestroyDescriptors(appclientNode, appclientDesc.getPreDestroyDescriptors().iterator());

        // datasource-definition*
        writeDataSourceDefinitionDescriptors(appclientNode, appclientDesc.getDataSourceDefinitionDescriptors().iterator());
        
        appendTextChild(appclientNode, ApplicationClientTagNames.CALLBACK_HANDLER, appclientDesc.getCallbackHandler());

         // message-destination*
        writeMessageDestinations
           (appclientNode, appclientDesc.getMessageDestinations().iterator());      
        
	return appclientNode;
              
    }
    /**
     * @return the default spec version level this node complies to
     */
    public String getSpecVersion() {
        return SPEC_VERSION;
    }
    
}
