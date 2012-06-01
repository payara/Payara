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

import java.util.HashMap;
import java.util.Map;

import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.glassfish.ejb.deployment.descriptor.runtime.PersistenceManagerInUse;
import org.w3c.dom.Node;

/**
 * This node handles the pm-inuse runtime xml element
 *
 * @author  Jerome Dochez
 * @version 
 */

public class PMInUseNode extends RuntimeDescriptorNode<PersistenceManagerInUse> {

    private PersistenceManagerInUse descriptor;

    @Override
    public PersistenceManagerInUse getDescriptor() {
        if (descriptor == null) descriptor = new PersistenceManagerInUse();
        return descriptor;
    }

    @Override
    protected Map getDispatchTable() {
        Map table = new HashMap();
        table.put(RuntimeTagNames.PM_IDENTIFIER, "set_pm_identifier");
        table.put(RuntimeTagNames.PM_VERSION, "set_pm_version");
        return table;
    }


    @Override
    public Node writeDescriptor(Node parent, String nodeName, PersistenceManagerInUse descriptor) {
        Node pmInUse = super.writeDescriptor(parent, nodeName, descriptor);
        appendTextChild(pmInUse, RuntimeTagNames.PM_IDENTIFIER, descriptor.get_pm_identifier());
        appendTextChild(pmInUse, RuntimeTagNames.PM_VERSION, descriptor.get_pm_version());
        return pmInUse;
    }
}
