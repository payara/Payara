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

package com.sun.enterprise.resource.listener;
import javax.resource.spi.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;

/**
 * This class is a ConnectionEventListener for handling the "close" of a
 * ManagedConnection that is not acquired through the appserver's pool.
 * The ManagedConnection is simply destroyed after close is called
 * Such an "unpooled" ManagedConnection is obtained for testConnectionPool
 * and the ConnectorRuntime.getConnection() APIs
 *
 * @author Aditya Gore
 * @since SJSAS 8.1
 */
public class UnpooledConnectionEventListener extends ConnectionEventListener {


    private static Logger _logger = LogDomains.getLogger(UnpooledConnectionEventListener.class,LogDomains.RSR_LOGGER);

    @Override
    public void connectionClosed(ConnectionEvent evt) {
        ManagedConnection mc = (ManagedConnection) evt.getSource();
        try {
            mc.destroy();
        } catch (Throwable re) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("error while destroying Unpooled Managed Connection");
            }
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("UnpooledConnectionEventListener: Connection closed");
        }
    }

    /**
     * Resource adapters will signal that the connection being closed is bad.
     * @param evt ConnectionEvent
     */
    @Override
    public void badConnectionClosed(ConnectionEvent evt){
        ManagedConnection mc = (ManagedConnection) evt.getSource();
        mc.removeConnectionEventListener(this);
        connectionClosed(evt);
    }

    @Override
    public void connectionErrorOccurred(ConnectionEvent evt) {
        //no-op
    }

    @Override
    public void localTransactionStarted(ConnectionEvent evt) {
            // no-op
    }

    @Override
    public void localTransactionCommitted(ConnectionEvent evt) {
         // no-op
    }

    @Override
    public void localTransactionRolledback(ConnectionEvent evt) {
        // no-op
    }

}

