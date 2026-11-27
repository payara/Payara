/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package com.sun.enterprise.server.logging.logviewer.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.Test;

import java.util.logging.Logger;

public class LogFilterForInstanceTest {

    private static Node node(String type, boolean local) {
        Node node = mock(Node.class);
        when(node.getType()).thenReturn(type);
        when(node.isLocal()).thenReturn(local);
        return node;
    }

    private static Server serverOn(String nodeRef) {
        Server server = mock(Server.class);
        when(server.getNodeRef()).thenReturn(nodeRef);
        return server;
    }

    private static Domain domainWith(String nodeRef, Node node) {
        Nodes nodes = mock(Nodes.class);
        when(nodes.getNode(nodeRef)).thenReturn(node);
        Domain domain = mock(Domain.class);
        when(domain.getNodes()).thenReturn(nodes);
        return domain;
    }

    @Test
    public void testLogViewingSupported() {
        Node localNode = node("CONFIG", true);
        assertTrue(LogFilterForInstance.isLogViewingSupported(localNode));

        Node sshNode = node("SSH", false);
        assertTrue(LogFilterForInstance.isLogViewingSupported(sshNode));
    }

    @Test
    public void testLogViewingNotSupported() {
        String[] unSupportedTypes = new String[]{"DOCKER", "TEMP", "CONFIG"};
        for (String unSupportedType : unSupportedTypes) {
            Node remoteNode = node(unSupportedType, false);
            assertFalse("node type " + unSupportedType, LogFilterForInstance.isLogViewingSupported(remoteNode));
        }
    }

    @Test
    public void testDownloadGivenInstanceLogFile() {
        Node dockerNode = node("DOCKER", false);

        LogViewingNotSupportedException exception = assertThrows(LogViewingNotSupportedException.class,
                () -> new LogFilterForInstance().downloadGivenInstanceLogFile(
                        mock(ServiceLocator.class),
                        serverOn("dockernode"),
                        domainWith("dockernode", dockerNode),
                        Logger.getLogger("test"),
                        "dockerinst1", "/tmp", "server.log", "server.log"));

        assertEquals("DOCKER", exception.getNodeType());
    }

    @Test
    public void testGetInstanceLogFileNames() {
        Node dockerNode = node("DOCKER", false);
        LogViewingNotSupportedException exception = assertThrows(LogViewingNotSupportedException.class,
                () -> new LogFilterForInstance().getInstanceLogFileNames(
                        mock(ServiceLocator.class),
                        serverOn("dockernode"),
                        domainWith("dockernode", dockerNode),
                        Logger.getLogger("test"),
                        "dockerinst1", "server.log"));
        assertEquals("DOCKER", exception.getNodeType());
    }
}
