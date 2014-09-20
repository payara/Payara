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

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import com.sun.enterprise.deployment.NameValuePairDescriptor;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.node.PropertiesNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.DefaultResourcePrincipalNode;
import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import com.sun.enterprise.deployment.node.runtime.common.RuntimeNameValuePairNode;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.w3c.dom.Node;

/**
 * This node handles the cmp-resource runtime xml tag
 *
 * @author  Jerome Dochez
 * @version 
 */
public class CmpResourceNode extends RuntimeDescriptorNode<ResourceReferenceDescriptor> {

    private ResourceReferenceDescriptor descriptor;

    public CmpResourceNode() {
        registerElementHandler(new XMLElement(RuntimeTagNames.DEFAULT_RESOURCE_PRINCIPAL), 
                               DefaultResourcePrincipalNode.class, "setResourcePrincipal");
        registerElementHandler(new XMLElement(RuntimeTagNames.PROPERTY), 
                               RuntimeNameValuePairNode.class, "addProperty");		
        registerElementHandler(new XMLElement(RuntimeTagNames.SCHEMA_GENERATOR_PROPERTIES), 
                                PropertiesNode.class, "setSchemaGeneratorProperties");
    }

    @Override
    public ResourceReferenceDescriptor getDescriptor() {
        if (descriptor == null) descriptor = new ResourceReferenceDescriptor();
        return descriptor;
    }

    @Override
    protected Map getDispatchTable() {    
        Map table = super.getDispatchTable();
        table.put(RuntimeTagNames.JNDI_NAME, "setJndiName");
        table.put(RuntimeTagNames.CREATE_TABLES_AT_DEPLOY, "setCreateTablesAtDeploy");
        table.put(RuntimeTagNames.DROP_TABLES_AT_UNDEPLOY, "setDropTablesAtUndeploy");
        table.put(RuntimeTagNames.DATABASE_VENDOR_NAME, "setDatabaseVendorName");
        return table;
    } 

    @Override
    public void postParsing() {
        EjbBundleDescriptorImpl bd = (EjbBundleDescriptorImpl) getParentNode().getDescriptor();
        if (bd==null) {
            DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.addDescriptorFailure",
                    new Object[]{descriptor});
            return;
        }
        bd.setCMPResourceReference(descriptor);
    }

    @Override
    public Node writeDescriptor(Node parent, String nodeName, 
                                ResourceReferenceDescriptor descriptor) {     
        Node cmp = super.writeDescriptor(parent, nodeName, descriptor);
        appendTextChild(cmp, RuntimeTagNames.JNDI_NAME, descriptor.getJndiName());
        if (descriptor.getResourcePrincipal() != null) {
            DefaultResourcePrincipalNode drpNode = new DefaultResourcePrincipalNode();
            drpNode.writeDescriptor(cmp, RuntimeTagNames.DEFAULT_RESOURCE_PRINCIPAL, 
                descriptor.getResourcePrincipal());
        }
        // properties*
        Iterator properties = descriptor.getProperties();
	if (properties!=null) {
	    RuntimeNameValuePairNode propNode = new RuntimeNameValuePairNode();        
	    while (properties.hasNext()) {
		NameValuePairDescriptor aProp = (NameValuePairDescriptor) properties.next();
		propNode.writeDescriptor(cmp, RuntimeTagNames.PROPERTY, aProp);
	    }
	}
        
        // createTableAtDeploy, dropTableAtUndeploy
        if (descriptor.isCreateTablesAtDeploy()) {
            appendTextChild(cmp, RuntimeTagNames.CREATE_TABLES_AT_DEPLOY, RuntimeTagNames.TRUE);
        }
        if (descriptor.isDropTablesAtUndeploy()) {
            appendTextChild(cmp, RuntimeTagNames.DROP_TABLES_AT_UNDEPLOY, RuntimeTagNames.TRUE);
        }        
        // database vendor name
        appendTextChild(cmp, RuntimeTagNames.DATABASE_VENDOR_NAME, descriptor.getDatabaseVendorName());
        
        // schema-generator-properties?
        Properties schemaGeneratorProps = descriptor.getSchemaGeneratorProperties();
        if (schemaGeneratorProps!=null) {
            PropertiesNode pn = new PropertiesNode();
            pn.writeDescriptor(cmp, RuntimeTagNames.SCHEMA_GENERATOR_PROPERTIES, schemaGeneratorProps);
        }
        
        return cmp;
    }
}
