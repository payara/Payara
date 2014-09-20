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

package com.sun.enterprise.v3.services.impl.monitor;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ConnectionProbe;
import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.Transport;

/**
 *
 * @author oleksiys
 */
public class ConnectionMonitor implements ConnectionProbe {
    private final GrizzlyMonitoring grizzlyMonitoring;
    private final String monitoringId;

    public ConnectionMonitor(GrizzlyMonitoring grizzlyMonitoring,
            String monitoringId, Transport transport) {
        this.grizzlyMonitoring = grizzlyMonitoring;
        this.monitoringId = monitoringId;
    }
    
    @Override
    public void onAcceptEvent(final Connection serverConnection,
            final Connection clientConnection) {
        final Object peerAddress = clientConnection.getPeerAddress();

        grizzlyMonitoring.getConnectionQueueProbeProvider().connectionAcceptedEvent(
                monitoringId, clientConnection.hashCode(),
                peerAddress.toString());
    }

    @Override
    public void onConnectEvent(final Connection connection) {
        grizzlyMonitoring.getConnectionQueueProbeProvider().connectionConnectedEvent(
                monitoringId, connection.hashCode(),
                connection.getPeerAddress().toString());
    }

    @Override
    public void onCloseEvent(Connection connection) {
        grizzlyMonitoring.getConnectionQueueProbeProvider().connectionClosedEvent(
                monitoringId, connection.hashCode());
    }

    @Override
    public void onBindEvent(Connection connection) {
    }

    @Override
    public void onReadEvent(Connection connection, Buffer data, int size) {
    }

    @Override
    public void onWriteEvent(Connection connection, Buffer data, long size) {
    }

    @Override
    public void onErrorEvent(Connection connection, Throwable error) {
    }

    @Override
    public void onIOEventReadyEvent(Connection connection, IOEvent ioEvent) {
    }

    @Override
    public void onIOEventEnableEvent(Connection connection, IOEvent ioEvent) {
    }

    @Override
    public void onIOEventDisableEvent(Connection connection, IOEvent ioEvent) {
    }

}
