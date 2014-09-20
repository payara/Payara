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

package org.glassfish.webservices.transport.tcp;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.tcp.util.ChannelContext;
import com.sun.xml.ws.transport.tcp.server.TCPAdapter;
import com.sun.xml.ws.transport.tcp.util.WSTCPException;
import java.io.IOException;

/**
 * General SOAP/TCP WebService Adapter for GFv3
 * 
 * @author Alexey Stashok
 */
public abstract class TCP109Adapter extends TCPAdapter {

    /**
     * Currently 109 deployed WS's pipeline relies on Servlet request and response
     * attributes. So its temporary workaround to make 109 work with TCP
     */
    private final ServletFakeArtifactSet servletFakeArtifactSet;
    private final boolean isEJB;

    public TCP109Adapter(
            @NotNull final String name,
            @NotNull final String urlPattern,
            @NotNull final WSEndpoint endpoint,
            @NotNull final ServletFakeArtifactSet servletFakeArtifactSet,
            final boolean isEJB) {
        super(name, urlPattern, endpoint);
        this.servletFakeArtifactSet = servletFakeArtifactSet;
        this.isEJB = isEJB;
    }


    @Override
    public void handle(@NotNull final ChannelContext channelContext) throws IOException, WSTCPException {
        try {
            beforeHandle();
            super.handle(channelContext);
        } finally {
            postHandle();
        }
    }

    public boolean isEJB() {
        return isEJB;
    }

    protected abstract void beforeHandle();

    protected abstract void postHandle();

    @Override
    protected TCPAdapter.TCPToolkit createToolkit() {
        return new TCP109Toolkit();
    }

    final class TCP109Toolkit extends TCPAdapter.TCPToolkit {
        // if its Adapter from 109 deployed WS - add fake Servlet artifacts
        @Override
        public void addCustomPacketSattellites(@NotNull final Packet packet) {
            super.addCustomPacketSattellites(packet);
            packet.addSatellite(servletFakeArtifactSet);
        }
    }
}
