/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.MailSessionDescriptor;
import com.sun.enterprise.deployment.xml.TagNames;
import org.w3c.dom.Node;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: naman mehta
 * Date: 16/4/12
 * Time: 5:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class MailSessionNode extends DeploymentDescriptorNode<MailSessionDescriptor> {

    public final static XMLElement tag = new XMLElement(TagNames.MAIL_SESSION);
    private MailSessionDescriptor descriptor = null;

    public MailSessionNode() {
        registerElementHandler(new XMLElement(TagNames.RESOURCE_PROPERTY), ResourcePropertyNode.class,
                "addMailSessionPropertyDescriptor");
    }

    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();

        table.put(TagNames.MAIL_SESSION_NAME, "setName");
        table.put(TagNames.MAIL_SESSION_STORE_PROTOCOL, "setStoreProtocol");
        table.put(TagNames.MAIL_SESSION_TRANSPORT_PROTOCOL, "setTransportProtocol");
        table.put(TagNames.MAIL_SESSION_HOST, "setHost");
        table.put(TagNames.MAIL_SESSION_USER, "setUser");
        table.put(TagNames.MAIL_SESSION_PASSWORD, "setPassword");
        table.put(TagNames.MAIL_SESSION_FROM, "setFrom");

        return table;
    }


    public Node writeDescriptor(Node parent, String nodeName, MailSessionDescriptor mailSessionDesc) {

        Node node = appendChild(parent, nodeName);
        appendTextChild(node, TagNames.MAIL_SESSION_NAME, mailSessionDesc.getName());
        appendTextChild(node, TagNames.MAIL_SESSION_STORE_PROTOCOL, mailSessionDesc.getStoreProtocol());
        appendTextChild(node, TagNames.MAIL_SESSION_TRANSPORT_PROTOCOL, mailSessionDesc.getTransportProtocol());
        appendTextChild(node, TagNames.MAIL_SESSION_HOST, mailSessionDesc.getHost());
        appendTextChild(node, TagNames.MAIL_SESSION_USER, mailSessionDesc.getUser());
        appendTextChild(node, TagNames.MAIL_SESSION_PASSWORD, mailSessionDesc.getPassword());
        appendTextChild(node, TagNames.MAIL_SESSION_FROM, mailSessionDesc.getFrom());

        ResourcePropertyNode propertyNode = new ResourcePropertyNode();
        propertyNode.writeDescriptor(node, mailSessionDesc);

        return node;
    }

    public MailSessionDescriptor getDescriptor() {
        if (descriptor == null) {
            descriptor = new MailSessionDescriptor();
        }
        return descriptor;
    }
}
