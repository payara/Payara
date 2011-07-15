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

package com.sun.enterprise.deployment.node.ws;

import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.runtime.ws.ReliabilityConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Map;

/**
 * This node represents reliability-config in weblogic-webservices.xml
 *
 * @author Rama Pulavarthi
 */
public class ReliabilityConfigNode extends DeploymentDescriptorNode {
    private final XMLElement tag =
            new XMLElement(WLWebServicesTagNames.RELIABILITY_CONFIG);

    ReliabilityConfig rmConfig = new ReliabilityConfig();
    /*
    public ReliabilityConfigNode(WebServiceEndpoint endpoint) {
        this.endpoint = endpoint;
    }
     */
    protected XMLElement getXMLRootTag() {
        return tag;
    }

    public Object getDescriptor() {
        return rmConfig;
    }

    protected Map getDispatchTable() {
        Map table = super.getDispatchTable();
        table.put(WLWebServicesTagNames.INACTIVITY_TIMEOUT, "setInactivityTimeout");
        table.put(WLWebServicesTagNames.BASE_RETRANSMISSION_INTERVAL, "setBaseRetransmissionInterval");
        table.put(WLWebServicesTagNames.RETRANSMISSION_EXPONENTIAL_BACKOFF, "setRetransmissionExponentialBackoff");
        table.put(WLWebServicesTagNames.ACKNOWLEDGEMENT_INTERVAL, "setAcknowledgementInterval");
        table.put(WLWebServicesTagNames.SEQUENCE_EXPIRATION, "setSequenceExpiration");
        table.put(WLWebServicesTagNames.BUFFER_RETRY_COUNT, "setBufferRetryCount");
        table.put(WLWebServicesTagNames.BUFFER_RETRY_DELAY, "setBufferRetryDelay");
        return table;
    }

    public Node writeDescriptor(Node parent, ReliabilityConfig descriptor) {
        if (descriptor != null) {
            Document doc = getOwnerDocument(parent);
            Element reliablityConfig = doc.createElement(WLWebServicesTagNames.RELIABILITY_CONFIG);
            addElementIfNonNull(doc,reliablityConfig, WLWebServicesTagNames.INACTIVITY_TIMEOUT, descriptor.getInactivityTimeout());
            addElementIfNonNull(doc,reliablityConfig, WLWebServicesTagNames.BASE_RETRANSMISSION_INTERVAL, descriptor.getBaseRetransmissionInterval());
            addElementIfNonNull(doc,reliablityConfig, WLWebServicesTagNames.RETRANSMISSION_EXPONENTIAL_BACKOFF, descriptor.getRetransmissionExponentialBackoff());
            addElementIfNonNull(doc,reliablityConfig, WLWebServicesTagNames.ACKNOWLEDGEMENT_INTERVAL, descriptor.getAcknowledgementInterval());
            addElementIfNonNull(doc,reliablityConfig, WLWebServicesTagNames.SEQUENCE_EXPIRATION, descriptor.getSequenceExpiration());
            addElementIfNonNull(doc,reliablityConfig, WLWebServicesTagNames.BUFFER_RETRY_COUNT, descriptor.getBufferRetryCount());
            addElementIfNonNull(doc,reliablityConfig, WLWebServicesTagNames.BUFFER_RETRY_DELAY, descriptor.getBufferRetryDelay());
            parent.appendChild(reliablityConfig);
            return reliablityConfig;
        }
        return null;
    }

    private void addElementIfNonNull(Document doc, Node parentNode, String tagName, String value) {
        if (value != null) {
            Element tag = doc.createElement(tagName);
            tag.appendChild(doc.createTextNode(value));
            parentNode.appendChild(tag);
        }
    }
}
