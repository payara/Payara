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

package com.sun.enterprise.deployment.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EarType;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.ApplicationTagNames;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFile;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.jvnet.hk2.annotations.Service;
import org.w3c.dom.Node;

/**
 * This class is responsible for loading and saving XML elements
 *
 * @author  Jerome Dochez
 * @version
 */
@Service
public class ApplicationNode extends AbstractBundleNode<Application> {

   /** 
    * The public ID.
    */
    public final static String PUBLIC_DTD_ID = "-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN";
    public final static String PUBLIC_DTD_ID_12 = "-//Sun Microsystems, Inc.//DTD J2EE Application 1.2//EN";
    /** 
     * The system ID.
     */
    public final static String SYSTEM_ID = "http://java.sun.com/dtd/application_1_3.dtd";
    public final static String SYSTEM_ID_12 = "http://java.sun.com/dtd/application_1_2.dtd";
    
    public final static String SCHEMA_ID_14 = "application_1_4.xsd";

    public final static String SCHEMA_ID_15 = "application_5.xsd";
    public final static String SCHEMA_ID_16 = "application_6.xsd";
    public final static String SCHEMA_ID = "application_7.xsd";
    public final static String SPEC_VERSION = "7";
    private final static List<String> systemIDs = initSystemIDs();
     
    // The XML tag associated with this Node
    public final static XMLElement tag = new XMLElement(ApplicationTagNames.APPLICATION);

    private final static List<String> initSystemIDs() {
        List<String> systemIDs = new ArrayList<String>();
        systemIDs.add(SCHEMA_ID);
        systemIDs.add(SCHEMA_ID_14);
        systemIDs.add(SCHEMA_ID_15);
        systemIDs.add(SCHEMA_ID_16);
        return Collections.unmodifiableList(systemIDs);
    }
    
    // The DOL Descriptor we are working for
    private Application descriptor;
    
    /**
     * register this node as a root node capable of loading entire DD files
     * 
     * @param publicIDToDTD is a mapping between xml Public-ID to DTD 
     * @return the doctype tag name
     */
    public String registerBundle(Map publicIDToDTD) {
        publicIDToDTD.put(PUBLIC_DTD_ID, SYSTEM_ID);
        publicIDToDTD.put(PUBLIC_DTD_ID_12, SYSTEM_ID_12);
        return tag.getQName();
    }

    @Override
    public Map<String,Class> registerRuntimeBundle(final Map<String,String> publicIDToDTD, Map<String, List<Class>> versionUpgrades) {
        final Map<String,Class> result = new HashMap<String,Class>();
        for (ConfigurationDeploymentDescriptorFile confDD : DOLUtils.getConfigurationDeploymentDescriptorFiles(habitat, EarType.ARCHIVE_TYPE)) {
          confDD.registerBundle(result, publicIDToDTD, versionUpgrades);
        }
        return result;
    }

    @Override
    public Collection<String> elementsAllowingEmptyValue() {
        final Set<String> result = new HashSet<String>();
        result.add(ApplicationTagNames.LIBRARY_DIRECTORY);
        return result;
    }

    @Override
    protected String topLevelTagName() {
        return ApplicationTagNames.APPLICATION_NAME;
    }

    @Override
    protected String topLevelTagValue(Application descriptor) {
        return descriptor.getAppName();
    }
    
    /** Creates new ApplicationNode */
    public ApplicationNode() {
        super();	
        registerElementHandler(new XMLElement(ApplicationTagNames.MODULE), ModuleNode.class, "addModule");    
        registerElementHandler(new XMLElement(ApplicationTagNames.ROLE), SecurityRoleNode.class, "addAppRole");            
        registerElementHandler(new XMLElement(TagNames.ENVIRONMENT_PROPERTY), EnvEntryNode.class, "addEnvironmentProperty");
        registerElementHandler(new XMLElement(TagNames.EJB_REFERENCE), EjbReferenceNode.class);
        registerElementHandler(new XMLElement(TagNames.EJB_LOCAL_REFERENCE), EjbLocalReferenceNode.class);
        JndiEnvRefNode serviceRefNode = habitat.getService(JndiEnvRefNode.class, WebServicesTagNames.SERVICE_REF); 
        if (serviceRefNode != null) {
            registerElementHandler(new XMLElement(WebServicesTagNames.SERVICE_REF), serviceRefNode.getClass(),"addServiceReferenceDescriptor");
        }

        registerElementHandler(new XMLElement(TagNames.RESOURCE_REFERENCE),
                                                             ResourceRefNode.class, "addResourceReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.RESOURCE_ENV_REFERENCE),
                                                            ResourceEnvRefNode.class, "addResourceEnvReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.MESSAGE_DESTINATION_REFERENCE), MessageDestinationRefNode.class, "addMessageDestinationReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.PERSISTENCE_CONTEXT_REF), EntityManagerReferenceNode.class, "addEntityManagerReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.PERSISTENCE_UNIT_REF), EntityManagerFactoryReferenceNode.class, "addEntityManagerFactoryReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.MESSAGE_DESTINATION),
                               MessageDestinationNode.class,
                               "addMessageDestination");
        registerElementHandler(new XMLElement(TagNames.DATA_SOURCE), DataSourceDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.MAIL_SESSION), MailSessionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.CONNECTION_FACTORY), ConnectionFactoryDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.ADMINISTERED_OBJECT), AdministeredObjectDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.JMS_CONNECTION_FACTORY), JMSConnectionFactoryDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.JMS_DESTINATION), JMSDestinationDefinitionNode.class, "addResourceDescriptor");

        SaxParserHandler.registerBundleNode(this, ApplicationTagNames.APPLICATION);
    }
    
    /**
     * @return the XML tag associated with this XMLNode
     */
    protected XMLElement getXMLRootTag() {
        return tag;
    }    

   /**
     * receives notiification of the value for a particular tag
     *
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
        Application application = getDescriptor();
        if (element.getQName().equals(
            ApplicationTagNames.LIBRARY_DIRECTORY)) {          
            application.setLibraryDirectory(value);
        } else if(element.getQName().equals(
            ApplicationTagNames.APPLICATION_NAME)) {
            application.setAppName(value);
        } else if (element.getQName().equals(
            ApplicationTagNames.INITIALIZE_IN_ORDER)) {
            application.setInitializeInOrder(Boolean.valueOf(value));
        } else super.setElementValue(element, value);
    }   

    
    /**
     * Adds  a new DOL descriptor instance to the descriptor instance associated with 
     * this XMLNode
     *
     * @param newDescriptor the new descriptor
     */
    public void addDescriptor(Object newDescriptor) {
        if (newDescriptor instanceof BundleDescriptor) {
	        if(DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
		    DOLUtils.getDefaultLogger().fine("In  " + toString() +
		        " adding descriptor " + newDescriptor);
	        }
           descriptor.addBundleDescriptor((BundleDescriptor) newDescriptor);
        } else if (newDescriptor instanceof EjbReference) {
            descriptor.addEjbReferenceDescriptor(
                        (EjbReference) newDescriptor);
        }
    }   
   
   /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public Application getDescriptor() {
        if (descriptor==null) {        	
            descriptor = Application.createApplication();
        }
        return descriptor;
    }

    /**
     * @return the DOCTYPE  of the XML file
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
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent parent node
     * @param application  to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, Application application) {
        Node appNode = super.writeDescriptor(parent, application);

        // initialize-in-order
        appendTextChild(appNode, ApplicationTagNames.INITIALIZE_IN_ORDER, String.valueOf(application.isInitializeInOrder()));

        // module
        ModuleNode moduleNode = new ModuleNode();
        for (ModuleDescriptor md :  application.getModules()) {
            moduleNode.writeDescriptor(appNode, ApplicationTagNames.MODULE, md);
        }

        // security-role* 
        // this information is not written out since it's already included
        // in the sub module deployment descriptors


        // library-directory
        if (application.getLibraryDirectoryRawValue() != null) {
            appendTextChild(appNode, ApplicationTagNames.LIBRARY_DIRECTORY, 
                application.getLibraryDirectoryRawValue());
        }

       // env-entry*
        writeEnvEntryDescriptors(appNode, application.getEnvironmentProperties().iterator());

        // ejb-ref * and ejb-local-ref*
        writeEjbReferenceDescriptors(appNode, application.getEjbReferenceDescriptors().iterator());

        // service-ref*
        writeServiceReferenceDescriptors(appNode, application.getServiceReferenceDescriptors().iterator());

        // resource-ref*
        writeResourceRefDescriptors(appNode, application.getResourceReferenceDescriptors().iterator());

        // resource-env-ref*
        writeResourceEnvRefDescriptors(appNode, application.getResourceEnvReferenceDescriptors().iterator());

        // message-destination-ref*
        writeMessageDestinationRefDescriptors(appNode, application.getMessageDestinationReferenceDescriptors().iterator());

        // persistence-context-ref*
        writeEntityManagerReferenceDescriptors(appNode, application.getEntityManagerReferenceDescriptors().iterator());

        // persistence-unit-ref*
        writeEntityManagerFactoryReferenceDescriptors(appNode, application.getEntityManagerFactoryReferenceDescriptors().iterator());

        // message-destination*
        writeMessageDestinations(appNode, application.getMessageDestinations().iterator());

        // all descriptors (includes DSD, MSD, JMSCFD, JMSDD,AOD, CFD)*
        writeResourceDescriptors(appNode, application.getAllResourcesDescriptors().iterator());

        return appNode;
    }
        
    /**
     * @return the default spec version level this node complies to
     */
    public String getSpecVersion() {
        return SPEC_VERSION;
    }
    
}
