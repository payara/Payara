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

package com.sun.gjc.spi;

import com.sun.logging.LogDomains;

import javax.resource.ResourceException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>ManagedConnectionMetaData</code> implementation for Generic JDBC Connector.
 *
 * @author Evani Sai Surya Kiran
 * @version 1.0, 02/08/03
 */
public class ManagedConnectionMetaDataImpl implements javax.resource.spi.ManagedConnectionMetaData {

    private java.sql.DatabaseMetaData dmd = null;
    private ManagedConnectionImpl mc;

    private static Logger _logger;

    static {
        _logger = LogDomains.getLogger(ManagedConnectionMetaDataImpl.class, LogDomains.RSR_LOGGER);
    }

    private boolean debug = false;

    /**
     * Constructor for <code>ManagedConnectionMetaDataImpl</code>
     *
     * @param mc <code>ManagedConnection</code>
     * @throws <code>ResourceException</code> if getting the DatabaseMetaData object fails
     */
    public ManagedConnectionMetaDataImpl(ManagedConnectionImpl mc) throws ResourceException {
        try {
            this.mc = mc;
            dmd = mc.getActualConnection().getMetaData();
        } catch (SQLException sqle) {
            _logger.log(Level.SEVERE, "jdbc.exc_md", sqle);
            throw new ResourceException(sqle.getMessage());
        }
    }

    /**
     * Returns product name of the underlying EIS instance connected
     * through the ManagedConnection.
     *
     * @return Product name of the EIS instance
     * @throws <code>ResourceException</code>
     */
    public String getEISProductName() throws ResourceException {
        try {
            return dmd.getDatabaseProductName();
        } catch (SQLException sqle) {
            _logger.log(Level.SEVERE, "jdbc.exc_eis_prodname", sqle);
            throw new ResourceException(sqle.getMessage());
        }
    }

    /**
     * Returns product version of the underlying EIS instance connected
     * through the ManagedConnection.
     *
     * @return Product version of the EIS instance
     * @throws <code>ResourceException</code>
     */
    public String getEISProductVersion() throws ResourceException {
        try {
            return dmd.getDatabaseProductVersion();
        } catch (SQLException sqle) {
            _logger.log(Level.SEVERE, "jdbc.exc_eis_prodvers", sqle);
            throw new ResourceException(sqle.getMessage(), sqle.getMessage());
        }
    }

    /**
     * Returns maximum limit on number of active concurrent connections
     * that an EIS instance can support across client processes.
     *
     * @return Maximum limit for number of active concurrent connections
     * @throws <code>ResourceException</code>
     */
    public int getMaxConnections() throws ResourceException {
        try {
            return dmd.getMaxConnections();
        } catch (SQLException sqle) {
            _logger.log(Level.SEVERE, "jdbc.exc_eis_maxconn");
            throw new ResourceException(sqle.getMessage());
        }
    }

    /**
     * Returns name of the user associated with the ManagedConnection instance. The name
     * corresponds to the resource principal under whose whose security context, a connection
     * to the EIS instance has been established.
     *
     * @return name of the user
     * @throws <code>ResourceException</code>
     */
    public String getUserName() throws ResourceException {
        javax.resource.spi.security.PasswordCredential pc = mc.getPasswordCredential();
        if (pc != null) {
            return pc.getUserName();
        }

        return mc.getManagedConnectionFactory().getUser();
    }
}
