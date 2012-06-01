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

import java.util.Map;

import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbCMPFinder;
import org.w3c.dom.Node;

public class FinderNode extends DeploymentDescriptorNode<IASEjbCMPFinder> {

    private IASEjbCMPFinder descriptor;

    @Override
    public IASEjbCMPFinder getDescriptor() {
        if (descriptor==null) descriptor = new IASEjbCMPFinder();
        return descriptor;
    }

    @Override
    protected Map getDispatchTable() {  
	Map dispatchTable = super.getDispatchTable();
	dispatchTable.put(RuntimeTagNames.METHOD_NAME, "setMethodName");
	dispatchTable.put(RuntimeTagNames.QUERY_PARAMS, "setQueryParameterDeclaration");
	dispatchTable.put(RuntimeTagNames.QUERY_FILTER, "setQueryFilter");
	dispatchTable.put(RuntimeTagNames.QUERY_VARIABLES, "setQueryVariables");
	dispatchTable.put(RuntimeTagNames.QUERY_ORDERING, "setQueryOrdering");
	return dispatchTable;
    }

    @Override
    public Node writeDescriptor(Node parent, String nodeName, IASEjbCMPFinder finder) {    
	Node finderNode = super.writeDescriptor(parent, nodeName, finder);
	appendTextChild(finderNode, RuntimeTagNames.METHOD_NAME, finder.getMethodName());
	appendTextChild(finderNode, RuntimeTagNames.QUERY_PARAMS, finder.getQueryParameterDeclaration());
	appendTextChild(finderNode, RuntimeTagNames.QUERY_FILTER, finder.getQueryFilter());
	appendTextChild(finderNode, RuntimeTagNames.QUERY_VARIABLES, finder.getQueryVariables());
	appendTextChild(finderNode, RuntimeTagNames.QUERY_ORDERING, finder.getQueryOrdering());
	return finderNode;
    }
}
