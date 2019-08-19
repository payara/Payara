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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.deployment.node.runtime.application.gf;

import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ResourcePropertyDescriptor;
import com.sun.enterprise.deployment.node.ApplicationNode;
import com.sun.enterprise.deployment.node.ResourcePropertyNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.*;
import com.sun.enterprise.deployment.node.runtime.common.SecurityRoleMappingNode;
import com.sun.enterprise.deployment.runtime.common.PrincipalNameDescriptor;
import com.sun.enterprise.deployment.runtime.common.SecurityRoleMapping;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.DTDRegistry;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.deployment.common.SecurityRoleMapper;
import org.glassfish.security.common.Group;
import org.glassfish.security.common.Role;
import org.w3c.dom.Node;

/**
 * This node handles all runtime-information pertinent to applications
 * The reading needs to be backward compatible with J2EE 1.2 and 1.3
 * where all runtime information was saved at the .ear file level in an
 * unique sun-ri.xml file. In J2EE 1.4, each sub archivist is responsible
 * for saving its runtime-info at his level.
 *
 * @author  Jerome Dochez
 * @version 
 */
public class ApplicationRuntimeNode extends RuntimeBundleNode<Application> {
    
    private String currentWebUri=null;
    
    public ApplicationRuntimeNode(Application descriptor) {
        super(descriptor);
        //trigger registration in standard node, if it hasn't happened
        serviceLocator.getService(ApplicationNode.class);
    }   
    
    /**
     * Initialize the child handlers
     */    
    protected void init() {     
        super.init();                          
        registerElementHandler(new XMLElement(RuntimeTagNames.SECURITY_ROLE_MAPPING), 
                               SecurityRoleMappingNode.class);              
        registerElementHandler(new XMLElement(RuntimeTagNames.RESOURCE_REFERENCE),
                               ResourceRefNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.EJB_REFERENCE),
                               EjbRefNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.RESOURCE_ENV_REFERENCE),
                               ResourceEnvRefNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.MESSAGE_DESTINATION_REFERENCE),
                               MessageDestinationRefNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.MESSAGE_DESTINATION),
             MessageDestinationRuntimeNode.class);
        registerElementHandler(new XMLElement(WebServicesTagNames.SERVICE_REF),
                               ServiceRefNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.PROPERTY),
                               ResourcePropertyNode.class);
    }
        
   /**
    * register this node as a root node capable of loading entire DD files
    * 
    * @param publicIDToDTD is a mapping between xml Public-ID to DTD 
    * @return the doctype tag name
    */
  public static String registerBundle(Map publicIDToDTD,
                                      Map<String, List<Class<?>>> versionUpgrades) {    
       publicIDToDTD.put(DTDRegistry.SUN_APPLICATION_130_DTD_PUBLIC_ID, DTDRegistry.SUN_APPLICATION_130_DTD_SYSTEM_ID);
       publicIDToDTD.put(DTDRegistry.SUN_APPLICATION_140_DTD_PUBLIC_ID, DTDRegistry.SUN_APPLICATION_140_DTD_SYSTEM_ID);       
       publicIDToDTD.put(DTDRegistry.SUN_APPLICATION_141_DTD_PUBLIC_ID, DTDRegistry.SUN_APPLICATION_141_DTD_SYSTEM_ID);       
       publicIDToDTD.put(DTDRegistry.SUN_APPLICATION_500_DTD_PUBLIC_ID, DTDRegistry.SUN_APPLICATION_500_DTD_SYSTEM_ID);       
       publicIDToDTD.put(DTDRegistry.SUN_APPLICATION_600_DTD_PUBLIC_ID, DTDRegistry.SUN_APPLICATION_600_DTD_SYSTEM_ID);       
       if (!restrictDTDDeclarations()) {
           publicIDToDTD.put(DTDRegistry.SUN_APPLICATION_140beta_DTD_PUBLIC_ID, DTDRegistry.SUN_APPLICATION_140beta_DTD_SYSTEM_ID);       
       }
       return RuntimeTagNames.S1AS_APPLICATION_RUNTIME_TAG;
   }   
    
    /**
     * @return the XML tag associated with this XMLNode
     */
    protected XMLElement getXMLRootTag() {
        return new XMLElement(RuntimeTagNames.S1AS_APPLICATION_RUNTIME_TAG);
    } 
    
    /** 
     * @return the DOCTYPE that should be written to the XML file
     */
    public String getDocType() {
	return DTDRegistry.SUN_APPLICATION_600_DTD_PUBLIC_ID;
    }
    
    /**
     * @return the SystemID of the XML file
     */
    public String getSystemID() {
	return DTDRegistry.SUN_APPLICATION_600_DTD_SYSTEM_ID;
    }

    /**
     * @return NULL for all runtime nodes.
     */
    public List<String> getSystemIDs() {
        return null;
    }
    
    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */    
    protected Map getDispatchTable() {    
        Map table = super.getDispatchTable();
        table.put(RuntimeTagNames.REALM, "setRealm");
        return table;
    }
    
    /**
     * receives notification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
	if (element.getQName().equals(RuntimeTagNames.PASS_BY_REFERENCE)) {
	    descriptor.setPassByReference("true".equalsIgnoreCase(value));
	} else
        if (element.getQName().equals(RuntimeTagNames.UNIQUE_ID)) {
            DOLUtils.getDefaultLogger().finer("Ignoring unique id");
            return;
        } else
	if (element.getQName().equals(RuntimeTagNames.ARCHIVE_NAME)) {
	    descriptor.setArchiveName(value);
	} else
	if (element.getQName().equals(RuntimeTagNames.COMPATIBILITY)) {
	    descriptor.setCompatibility(value);
	} else
	if (element.getQName().equals(RuntimeTagNames.PAYARA_CLASSLOADING_DELEGATE)) {
	    descriptor.setClassLoadingDelegate(value);
	} else 
	if (element.getQName().equals(RuntimeTagNames.PAYARA_ENABLE_IMPLICIT_CDI)) {
            // ignore, handled in EarHandler.java
	} else 
	if (element.getQName().equals(RuntimeTagNames.PAYARA_SCANNING_EXCLUDE)) {
            descriptor.addScanningExclusions(Collections.singletonList(value));
	} else
	if (element.getQName().equals(RuntimeTagNames.PAYARA_SCANNING_INCLUDE)) {
            descriptor.addScanningInclusions(Collections.singletonList(value));
	} else
	if (element.getQName().equals(RuntimeTagNames.PAYARA_WHITELIST_PACKAGE)) {
            descriptor.addWhitelistPackage(value);
	} else
	if (element.getQName().equals(RuntimeTagNames.WEB_URI)) {
	    currentWebUri=value;
	} else 
	if (element.getQName().equals(RuntimeTagNames.CONTEXT_ROOT)) {
	    if (currentWebUri!=null) {
		ModuleDescriptor md = descriptor.getModuleDescriptorByUri(currentWebUri);
                if (md==null) {
                    throw new RuntimeException("No bundle in application with uri " + currentWebUri);
                }
		currentWebUri=null;
		if (md.getModuleType().equals(DOLUtils.warType())) {
		    md.setContextRoot(value);
		} else {
		    throw new RuntimeException(currentWebUri + " uri does not point to a web bundle");
		} 
	    } else {
		throw new RuntimeException("No uri provided for this context-root " + value);
	    }
        } else if (element.getQName().equals(RuntimeTagNames.KEEP_STATE)) {
            descriptor.setKeepState(value);
        } else if (element.getQName().equals(RuntimeTagNames.VERSION_IDENTIFIER)) {
	} else super.setElementValue(element, value);
    }

    /**
     * Adds  a new DOL descriptor instance to the descriptor instance associated with 
     * this XMLNode
     *
     * @param newDescriptor the new descriptor
     */
    public void addDescriptor(Object newDescriptor) {
        if (newDescriptor instanceof SecurityRoleMapping) {
            SecurityRoleMapping roleMap = (SecurityRoleMapping) newDescriptor;
            if (descriptor!=null && !descriptor.isVirtual()) {
                descriptor.addSecurityRoleMapping(roleMap);
                Role role = new Role(roleMap.getRoleName());
                SecurityRoleMapper rm = descriptor.getRoleMapper();
                if (rm != null) {
                    List<PrincipalNameDescriptor> principals = roleMap.getPrincipalNames();
                    for (int i = 0; i < principals.size(); i++) {
                        rm.assignRole(principals.get(i).getPrincipal(),
                            role, descriptor);
                    }
                    List<String> groups = roleMap.getGroupNames();
                    for (int i = 0; i < groups.size(); i++) {
                        rm.assignRole(new Group(groups.get(i)),
                            role, descriptor);
                    }
                }
            }
        }
        else if(newDescriptor instanceof ResourcePropertyDescriptor) {
            ResourcePropertyDescriptor desc = (ResourcePropertyDescriptor)newDescriptor;
            if("default-role-mapping".equals(desc.getName())) {
                descriptor.setDefaultGroupPrincipalMapping(ConfigBeansUtilities.toBoolean(desc.getValue()));
            }
        }
    } 
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param nodeName the node name
     * @param application the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, String nodeName, Application application) {    
        Node appNode = super.writeDescriptor(parent, nodeName, application);
	
        // web*
	for (ModuleDescriptor module : application.getModules()) {
	    if (module.getModuleType().equals(DOLUtils.warType())) {
		Node web = appendChild(appNode, RuntimeTagNames.WEB);
		appendTextChild(web, RuntimeTagNames.WEB_URI, module.getArchiveUri());
		appendTextChild(web, RuntimeTagNames.CONTEXT_ROOT, module.getContextRoot());
	    }
	}
	
	// pass-by-reference ?
	if (application.isPassByReferenceDefined()) {
	    appendTextChild(appNode, RuntimeTagNames.PASS_BY_REFERENCE, String.valueOf(application.getPassByReference()));
	}

        // NOTE : unique-id is no longer written out to sun-ejb-jar.xml.  It is persisted via
        // domain.xml deployment context properties instead.
	
        // security-role-mapping*
        List<SecurityRoleMapping> roleMappings = application.getSecurityRoleMappings();
        for (int i = 0; i < roleMappings.size(); i++) { 
            SecurityRoleMappingNode srmn = new SecurityRoleMappingNode();
            srmn.writeDescriptor(appNode, RuntimeTagNames.SECURITY_ROLE_MAPPING, roleMappings.get(i));
        }
        
        // realm?
        appendTextChild(appNode, RuntimeTagNames.REALM, application.getRealm());

        // references
        RuntimeDescriptorNode.writeCommonComponentInfo(appNode, application);
        RuntimeDescriptorNode.writeMessageDestinationInfo(appNode, application);

        // archive-name
        appendTextChild(appNode, RuntimeTagNames.ARCHIVE_NAME, application.getArchiveName());

        // compatibility
        appendTextChild(appNode, RuntimeTagNames.COMPATIBILITY, application.getCompatibility());

        // keep-state
        appendTextChild(appNode, RuntimeTagNames.KEEP_STATE, String.valueOf(application.getKeepState()));

        return appNode;
    }
}
