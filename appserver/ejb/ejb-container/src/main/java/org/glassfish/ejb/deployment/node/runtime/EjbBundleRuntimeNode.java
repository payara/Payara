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

package org.glassfish.ejb.deployment.node.runtime;

import java.util.List;
import java.util.Map;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.RuntimeBundleNode;
import com.sun.enterprise.deployment.node.runtime.common.SecurityRoleMappingNode;
import com.sun.enterprise.deployment.runtime.common.PrincipalNameDescriptor;
import com.sun.enterprise.deployment.runtime.common.SecurityRoleMapping;
import com.sun.enterprise.deployment.xml.DTDRegistry;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.glassfish.deployment.common.SecurityRoleMapper;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.node.EjbBundleNode;
import org.glassfish.security.common.Group;
import org.glassfish.security.common.Role;
import org.w3c.dom.Node;

/**
 * This node handles runtime deployment descriptors for ejb bundle
 * 
 * @author  Jerome Dochez
 * @version 
 */
public class EjbBundleRuntimeNode extends
        RuntimeBundleNode<EjbBundleDescriptorImpl> {

    public EjbBundleRuntimeNode(EjbBundleDescriptorImpl descriptor) {
        super(descriptor);
        //trigger registration in standard node, if it hasn't happened
        habitat.getService(EjbBundleNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.SECURITY_ROLE_MAPPING),
                SecurityRoleMappingNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.EJBS),
                EnterpriseBeansRuntimeNode.class);
    }

    public EjbBundleRuntimeNode() {
        this(null);
    }

    @Override
    public String getDocType() {
        return DTDRegistry.SUN_EJBJAR_310_DTD_PUBLIC_ID;
    }

    @Override
    public String getSystemID() {
        return DTDRegistry.SUN_EJBJAR_310_DTD_SYSTEM_ID;
    }

    /**
     * @return NULL for all runtime nodes.
     */
    @Override
    public List<String> getSystemIDs() {
        return null;
    }

    @Override
    protected XMLElement getXMLRootTag() {
        return new XMLElement(RuntimeTagNames.S1AS_EJB_RUNTIME_TAG);
    }

   /**
    * register this node as a root node capable of loading entire DD files
    * 
    * @param publicIDToDTD is a mapping between xml Public-ID to DTD 
    * @return the doctype tag name
    */
   public static String registerBundle(Map publicIDToDTD) {    
       publicIDToDTD.put(DTDRegistry.SUN_EJBJAR_200_DTD_PUBLIC_ID, DTDRegistry.SUN_EJBJAR_200_DTD_SYSTEM_ID);
       publicIDToDTD.put(DTDRegistry.SUN_EJBJAR_201_DTD_PUBLIC_ID, DTDRegistry.SUN_EJBJAR_201_DTD_SYSTEM_ID);
       publicIDToDTD.put(DTDRegistry.SUN_EJBJAR_210_DTD_PUBLIC_ID, DTDRegistry.SUN_EJBJAR_210_DTD_SYSTEM_ID);
       publicIDToDTD.put(DTDRegistry.SUN_EJBJAR_211_DTD_PUBLIC_ID, DTDRegistry.SUN_EJBJAR_211_DTD_SYSTEM_ID);
       publicIDToDTD.put(DTDRegistry.SUN_EJBJAR_300_DTD_PUBLIC_ID, DTDRegistry.SUN_EJBJAR_300_DTD_SYSTEM_ID);
       publicIDToDTD.put(DTDRegistry.SUN_EJBJAR_310_DTD_PUBLIC_ID, DTDRegistry.SUN_EJBJAR_310_DTD_SYSTEM_ID);

       if (!restrictDTDDeclarations()) {
           publicIDToDTD.put(DTDRegistry.SUN_EJBJAR_210beta_DTD_PUBLIC_ID, DTDRegistry.SUN_EJBJAR_210beta_DTD_SYSTEM_ID);
       }           
       return RuntimeTagNames.S1AS_EJB_RUNTIME_TAG;
   }

    @Override
    public EjbBundleDescriptorImpl getDescriptor() {
        return descriptor;
    }

    @Override
    public void setElementValue(XMLElement element, String value) {
        if (element.getQName().equals(RuntimeTagNames.COMPATIBILITY)) {
            descriptor.setCompatibility(value);
        } else if (element.getQName().equals(RuntimeTagNames.DISABLE_NONPORTABLE_JNDI_NAMES)) {
            descriptor.setDisableNonportableJndiNames(value);
        } else if (element.getQName().equals(RuntimeTagNames.KEEP_STATE)) {
            descriptor.setKeepState(value);
        } else if (element.getQName().equals(RuntimeTagNames.VERSION_IDENTIFIER)) {
        } else {
            super.setElementValue(element, value);
        }
    }

    @Override
    public void addDescriptor(Object newDescriptor) {
        if (newDescriptor instanceof SecurityRoleMapping) {
            SecurityRoleMapping roleMap = (SecurityRoleMapping)newDescriptor;
            descriptor.addSecurityRoleMapping(roleMap);
            Application app = descriptor.getApplication();
            if (app!=null) {
                Role role = new Role(roleMap.getRoleName());
                SecurityRoleMapper rm = app.getRoleMapper();
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
    }

    @Override
    public Node writeDescriptor(Node parent, EjbBundleDescriptorImpl bundleDescriptor) {
        Node ejbs = super.writeDescriptor(parent, bundleDescriptor);

        // security-role-mapping*
        List<SecurityRoleMapping> roleMappings = bundleDescriptor.getSecurityRoleMappings();
        for (int i = 0; i < roleMappings.size(); i++) { 
            SecurityRoleMappingNode srmn = new SecurityRoleMappingNode();
            srmn.writeDescriptor(ejbs, RuntimeTagNames.SECURITY_ROLE_MAPPING, roleMappings.get(i));
        }
	
	    // entreprise-beans
        EnterpriseBeansRuntimeNode ejbsNode = new EnterpriseBeansRuntimeNode();
        ejbsNode.writeDescriptor(ejbs, RuntimeTagNames.EJBS, bundleDescriptor);

        // compatibility
        appendTextChild(ejbs, RuntimeTagNames.COMPATIBILITY, bundleDescriptor.getCompatibility());

        //disable-nonportable-jndi-names
        Boolean djndi = bundleDescriptor.getDisableNonportableJndiNames();
        if (djndi != null) {
            appendTextChild(ejbs, RuntimeTagNames.DISABLE_NONPORTABLE_JNDI_NAMES, String.valueOf(djndi));
        }
 
        // keep-state
        appendTextChild(ejbs, RuntimeTagNames.KEEP_STATE, String.valueOf(bundleDescriptor.getKeepState()));

        return ejbs;
    }
}
