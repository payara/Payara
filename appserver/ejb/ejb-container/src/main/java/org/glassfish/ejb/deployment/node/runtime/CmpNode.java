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
import java.util.logging.Level;

import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.glassfish.ejb.deployment.descriptor.IASEjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbCMPFinder;
import org.glassfish.ejb.deployment.descriptor.runtime.PrefetchDisabledDescriptor;
import org.w3c.dom.Node;

/**
 * This node handles the cmp runtime deployment descriptors 
 *
 * @author  Jerome Dochez
 * @version 
 */
public class CmpNode extends DeploymentDescriptorNode<IASEjbCMPEntityDescriptor> {

    protected IASEjbCMPEntityDescriptor descriptor;

    public CmpNode() {
        registerElementHandler(new XMLElement(RuntimeTagNames.FINDER), FinderNode.class);   
        registerElementHandler(new XMLElement(RuntimeTagNames.PREFETCH_DISABLED), PrefetchDisabledNode.class);   
    }

    @Override
    public IASEjbCMPEntityDescriptor getDescriptor() {
        if (descriptor == null) {
            Object desc = getParentNode().getDescriptor();
            if (desc instanceof IASEjbCMPEntityDescriptor) {
                descriptor = (IASEjbCMPEntityDescriptor) desc;
            }
        }
        return descriptor;
    }

    @Override
    protected Map getDispatchTable() {  
	Map dispatchTable = super.getDispatchTable();
	dispatchTable.put(RuntimeTagNames.MAPPING_PROPERTIES, "setMappingProperties");
        // deprecated element, will be ignored at reading
        dispatchTable.put(RuntimeTagNames.IS_ONE_ONE_CMP, null);
	return dispatchTable;
    }

    @Override
    public void addDescriptor(Object newDescriptor) {    
        getDescriptor();
        if (descriptor == null) {
            DOLUtils.getDefaultLogger().log(Level.WARNING, "enterprise.deployment.backend.addDescriptorFailure",
                new Object[] {newDescriptor, this});
            return;
        }
        if (newDescriptor instanceof IASEjbCMPFinder ) {
            descriptor.addOneOneFinder((IASEjbCMPFinder) newDescriptor);
        }
         else if (newDescriptor instanceof PrefetchDisabledDescriptor) {
            descriptor.setPrefetchDisabledDescriptor((PrefetchDisabledDescriptor)newDescriptor);
        } else super.addDescriptor(descriptor);        
    }

    @Override
    public Node writeDescriptor(Node parent, String nodeName, IASEjbCMPEntityDescriptor ejbDescriptor) {    
        Node cmpNode = super.writeDescriptor(parent, nodeName, ejbDescriptor);
        appendTextChild(cmpNode, RuntimeTagNames.MAPPING_PROPERTIES, ejbDescriptor.getMappingProperties());
        Map finders = ejbDescriptor.getOneOneFinders();
        if (!finders.isEmpty()) {
            Node findersNode = appendChild(cmpNode, RuntimeTagNames.ONE_ONE_FINDERS);
            FinderNode fn = new FinderNode();
            for (Iterator finderIterator = finders.values().iterator();finderIterator.hasNext();) {
                IASEjbCMPFinder aFinder = (IASEjbCMPFinder) finderIterator.next();
                fn.writeDescriptor(findersNode, RuntimeTagNames.FINDER, aFinder);
            }
        }

        // prefetch-disabled
        PrefetchDisabledDescriptor prefetchDisabledDesc =  ejbDescriptor.getPrefetchDisabledDescriptor();
        if (prefetchDisabledDesc != null) {
            PrefetchDisabledNode prefetchDisabledNode = new PrefetchDisabledNode();
            prefetchDisabledNode.writeDescriptor(cmpNode, 
                RuntimeTagNames.PREFETCH_DISABLED, prefetchDisabledDesc);
        }

        return cmpNode;
    }
}
