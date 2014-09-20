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


import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.MethodNode;
import com.sun.enterprise.deployment.node.XMLElement;
import org.glassfish.ejb.deployment.EjbTagNames;
import org.glassfish.ejb.deployment.descriptor.ConcurrentMethodDescriptor;
import org.glassfish.ejb.deployment.descriptor.TimeoutValueDescriptor;
import org.w3c.dom.Node;

public class ConcurrentMethodNode extends DeploymentDescriptorNode<ConcurrentMethodDescriptor> {

    private static final String WRITE_LOCK = "Write";

    private ConcurrentMethodDescriptor descriptor = null;

    public ConcurrentMethodNode() {
        super();

        registerElementHandler(new XMLElement(EjbTagNames.METHOD), MethodNode.class,
                "setConcurrentMethod");
        registerElementHandler(new XMLElement(EjbTagNames.CONCURRENT_ACCESS_TIMEOUT),
                TimeoutValueNode.class, "setAccessTimeout");

    }

    @Override
    public ConcurrentMethodDescriptor getDescriptor() {
        if (descriptor == null) descriptor = new ConcurrentMethodDescriptor();
        return descriptor;
    }

    @Override
    public void setElementValue(XMLElement element, String value) {
        if (EjbTagNames.CONCURRENT_LOCK.equals(element.getQName())) {
            descriptor.setWriteLock(value.equals(WRITE_LOCK));
        } else {
            super.setElementValue(element, value);
        }
    }

    @Override
    public Node writeDescriptor(Node parent, String nodeName, ConcurrentMethodDescriptor desc) {
        Node concurrentNode = super.writeDescriptor(parent, nodeName, descriptor);

        MethodNode methodNode = new MethodNode();

        methodNode.writeJavaMethodDescriptor(concurrentNode, EjbTagNames.METHOD,
                desc.getConcurrentMethod());

        if( desc.hasLockMetadata() ) {
            String lockType = desc.isWriteLocked() ? "Write" : "Read";
            appendTextChild(concurrentNode, EjbTagNames.CONCURRENT_LOCK, lockType);
        }

        if( desc.hasAccessTimeout() ) {
            TimeoutValueNode timeoutValueNode = new TimeoutValueNode();
            TimeoutValueDescriptor timeoutDesc = new TimeoutValueDescriptor();
            timeoutDesc.setValue(desc.getAccessTimeoutValue());
            timeoutDesc.setUnit(desc.getAccessTimeoutUnit());
            timeoutValueNode.writeDescriptor(concurrentNode, EjbTagNames.CONCURRENT_ACCESS_TIMEOUT,
                timeoutDesc);
        }

        return concurrentNode;
     }

}
