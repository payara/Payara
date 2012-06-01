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

package org.glassfish.ejb.deployment.node;

import java.util.Map;

import com.sun.enterprise.deployment.node.SecurityRoleRefNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.TagNames;
import org.glassfish.ejb.deployment.EjbTagNames;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.w3c.dom.Node;

/**
 * This class is responsible for reading/writing all information
 * common to all EJB which are interfaces based (entity, session)
 *
 * @author  Jerome Dochez
 * @version 
 */
public abstract class InterfaceBasedEjbNode<S extends EjbDescriptor> extends EjbNode<S> {

   public InterfaceBasedEjbNode() {
       super();
       // register sub XMLNodes
       registerElementHandler(new XMLElement(TagNames.ROLE_REFERENCE), SecurityRoleRefNode.class, "addRoleReference");
    }

    @Override
    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();
        table.put(EjbTagNames.HOME, "setHomeClassName");
        table.put(EjbTagNames.REMOTE, "setRemoteClassName");        
        table.put(EjbTagNames.LOCAL_HOME, "setLocalHomeClassName");
        table.put(EjbTagNames.LOCAL, "setLocalClassName");        
        table.put(EjbTagNames.BUSINESS_LOCAL, "addLocalBusinessClassName");
        table.put(EjbTagNames.BUSINESS_REMOTE, "addRemoteBusinessClassName");
        table.put(EjbTagNames.SERVICE_ENDPOINT_INTERFACE,
                  "setWebServiceEndpointInterfaceName");
        return table;
    }

    @Override
    protected void writeCommonHeaderEjbDescriptor(Node ejbNode, EjbDescriptor descriptor) {    
        super.writeCommonHeaderEjbDescriptor(ejbNode, descriptor);
        appendTextChild(ejbNode, EjbTagNames.HOME, descriptor.getHomeClassName());               
        appendTextChild(ejbNode, EjbTagNames.REMOTE, descriptor.getRemoteClassName());               
        appendTextChild(ejbNode, EjbTagNames.LOCAL_HOME, descriptor.getLocalHomeClassName());               
        appendTextChild(ejbNode, EjbTagNames.LOCAL, descriptor.getLocalClassName());                   

        for(String next : descriptor.getLocalBusinessClassNames()) {
            appendTextChild(ejbNode, EjbTagNames.BUSINESS_LOCAL, next);
        }

        for(String next : descriptor.getRemoteBusinessClassNames()) {
            appendTextChild(ejbNode, EjbTagNames.BUSINESS_REMOTE, next);
        }

        if( descriptor.isLocalBean()) {
            appendChild(ejbNode, EjbTagNames.LOCAL_BEAN);
        }

        appendTextChild(ejbNode, EjbTagNames.SERVICE_ENDPOINT_INTERFACE,
                        descriptor.getWebServiceEndpointInterfaceName());
        appendTextChild(ejbNode, EjbTagNames.EJB_CLASS, descriptor.getEjbClassName());
    }
}
