/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.nio.NIOConnection;

/**
 *
 * @author Alexey Stashok
 */
public class WSTCPProtocolFilter extends BaseFilter {

    private static final Logger LOGGER = LogUtils.getLogger();

    private volatile Connector connector;
    
    private final Object sync = new Object();

    private static final V3Module module = new V3Module();

    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final NIOConnection connection = (NIOConnection) ctx.getConnection();

        if (connector == null) {
            synchronized (sync) {
                if (connector == null) {

                    final InetSocketAddress socketAddress = (InetSocketAddress) connection.getPeerAddress();
                    final String host = socketAddress.getHostName();
                    final int port = socketAddress.getPort();

                    LOGGER.log(Level.INFO, LogUtils.SOAPTCP_PROTOCOL_INITIALIZED, port);

                    connector = new Connector(host, port, module.getDelegate());
                }
            }
        }
        
        final Buffer buffer = ctx.getMessage();
        final ByteBuffer byteBuffer = buffer.toByteBuffer();
                
        final SocketChannel channel = (SocketChannel) connection.getChannel();
        connector.process(byteBuffer, channel);

        return ctx.getStopAction();
    }

    @Override
    public NextAction handleClose(final FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        final SelectionKey selectionKey = ((NIOConnection) connection).getSelectionKey();

        try {
            if (connector != null) {
                connector.notifyConnectionClosed((SocketChannel) selectionKey.channel());
            } else {
                synchronized (sync) {
                    if (connector != null) {
                        connector.notifyConnectionClosed((SocketChannel) selectionKey.channel());
                    }
                }
            }
        } catch (Exception e) {
        }

        return ctx.getInvokeAction();
    }
}
