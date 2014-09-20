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

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.gjc.common.DataSourceObjectBuilder;
import com.sun.gjc.util.SecurityUtils;
import com.sun.logging.LogDomains;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ResourceAllocationException;
import javax.resource.spi.security.PasswordCredential;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.spi.ConnectionDefinition;

/**
 * Data Source <code>ManagedConnectionFactory</code> implementation for Generic JDBC Connector.
 *
 * @author Evani Sai Surya Kiran
 * @version 1.0, 02/07/30
 */

@ConnectionDefinition(
    connectionFactory = javax.sql.DataSource.class,
    connectionFactoryImpl = com.sun.gjc.spi.base.AbstractDataSource.class,
    connection = java.sql.Connection.class,
    connectionImpl = com.sun.gjc.spi.base.ConnectionHolder.class
)
public class DSManagedConnectionFactory extends ManagedConnectionFactoryImpl {

    private transient javax.sql.DataSource dataSourceObj;

    private static Logger _logger;

    static {
        _logger = LogDomains.getLogger(DSManagedConnectionFactory.class, LogDomains.RSR_LOGGER);
    }

    /**
     * Creates a new physical connection to the underlying EIS resource
     * manager.
     *
     * @param subject       <code>Subject</code> instance passed by the application server
     * @param cxRequestInfo <code>ConnectionRequestInfo</code> which may be created
     *                      as a result of the invocation <code>getConnection(user, password)</code>
     *                      on the <code>DataSource</code> object
     * @return <code>ManagedConnection</code> object created
     * @throws ResourceException           if there is an error in instantiating the
     *                                     <code>DataSource</code> object used for the
     *                                     creation of the <code>ManagedConnection</code> object
     * @throws SecurityException           if there ino <code>PasswordCredential</code> object
     *                                     satisfying this request
     * @throws ResourceAllocationException if there is an error in allocating the
     *                                     physical connection
     */
    public javax.resource.spi.ManagedConnection createManagedConnection(javax.security.auth.Subject subject,
                                                                        ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        logFine("In createManagedConnection");
        PasswordCredential pc = SecurityUtils.getPasswordCredential(this, subject, cxRequestInfo);

        javax.sql.DataSource dataSource = getDataSource();

        java.sql.Connection dsConn = null;
        ManagedConnectionImpl mc = null;

        try {
            /* For the case where the user/passwd of the connection pool is
            * equal to the PasswordCredential for the connection request
            * get a connection from this pool directly.
            * for all other conditions go create a new connection
            */

            if (isEqual(pc, getUser(), getPassword())) {
                dsConn = dataSource.getConnection();
            } else {
                dsConn = dataSource.getConnection(pc.getUserName(),
                        new String(pc.getPassword()));
            }
        } catch (java.sql.SQLException sqle) {
            //_logger.log(Level.WARNING, "jdbc.exc_create_conn", sqle.getMessage());
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "jdbc.exc_create_conn", sqle.getMessage());
            }
            StringManager localStrings =
                    StringManager.getManager(DataSourceObjectBuilder.class);
            String msg = localStrings.getString("jdbc.cannot_allocate_connection"
                    , sqle.getMessage());
            ResourceAllocationException rae = new ResourceAllocationException(msg);
            rae.initCause(sqle);
            throw rae;
        }

        try {
            mc = constructManagedConnection(null, dsConn, pc, this);

            //GJCINT
            validateAndSetIsolation(mc);
        } finally {
            if (mc == null) {
                if (dsConn != null) {
                    try {
                        dsConn.close();
                    } catch (SQLException e) {
                        _logger.log(Level.FINEST, "Exception while closing connection : " +
                                "createManagedConnection" + dsConn);
                    }
                }
            }
        }
        return mc;
    }

    /**
     * Returns the underlying datasource
     *
     * @return DataSource of jdbc vendor
     * @throws ResourceException
     */
    public javax.sql.DataSource getDataSource() throws ResourceException {
        if (dataSourceObj == null) {
            try {
                dataSourceObj = (javax.sql.DataSource) super.getDataSource();
            } catch (ClassCastException cce) {
                _logger.log(Level.SEVERE, "jdbc.exc_cce", cce);
                throw new ResourceException(cce.getMessage());
            }
        }
        return dataSourceObj;
    }

    /**
     * Check if this <code>ManagedConnectionFactory</code> is equal to
     * another <code>ManagedConnectionFactory</code>.
     *
     * @param other <code>ManagedConnectionFactory</code> object for checking equality with
     * @return true    if the property sets of both the
     *         <code>ManagedConnectionFactory</code> objects are the same
     *         false	otherwise
     */
    public boolean equals(Object other) {
        logFine("In equals");
        /**
         * The check below means that two ManagedConnectionFactory objects are equal
         * if and only if their properties are the same.
         */
        if (other instanceof com.sun.gjc.spi.DSManagedConnectionFactory) {
            com.sun.gjc.spi.DSManagedConnectionFactory otherMCF =
                    (com.sun.gjc.spi.DSManagedConnectionFactory) other;
            return this.spec.equals(otherMCF.spec);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * 7 + (spec.hashCode());
    }

}
