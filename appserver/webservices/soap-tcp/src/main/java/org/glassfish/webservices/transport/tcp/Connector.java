/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.webservices.transport.tcp;

import com.sun.xml.ws.transport.tcp.server.IncomeMessageProcessor;
import com.sun.xml.ws.transport.tcp.server.TCPMessageListener;
import com.sun.xml.ws.transport.tcp.server.WSTCPConnector;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Properties;

/**
 *
 * @author oleksiys
 */
public class Connector implements WSTCPConnector {
    private final String host;
    private final int port;
    private final TCPMessageListener listener;

    private final Properties properties;
    
    private final IncomeMessageProcessor processor;
    
    public Connector(String host, int port, TCPMessageListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
        
        properties = new Properties();

        processor = IncomeMessageProcessor.registerListener(port, listener, properties);
    }

    public void listen() throws IOException {
    }

    public void process(ByteBuffer buffer, SocketChannel channel) throws IOException {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader oldClassLoader = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(processor.getClass().getClassLoader());
            processor.process(buffer, channel);
        } finally {
            currentThread.setContextClassLoader(oldClassLoader);
        }
    }

    public void notifyConnectionClosed(SocketChannel channel) {
        processor.notifyClosed(channel);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
    }

    public TCPMessageListener getListener() {
        return listener;
    }

    public void setListener(TCPMessageListener arg0) {
    }

    public void setFrameSize(int frameSize) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getFrameSize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void close() {
        IncomeMessageProcessor.releaseListener(port);
    }
}
