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

package com.sun.gjc.spi.jdbc40;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.gjc.common.DataSourceObjectBuilder;
import com.sun.gjc.spi.ManagedConnectionFactoryImpl;
import com.sun.gjc.spi.base.AbstractDataSource;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import java.util.logging.Level;
import java.sql.*;
import java.util.logging.Logger;

/**
 * Holds the <code>java.sql.Connection</code> object, which is to be
 * passed to the application program.
 *
 * @author Binod P.G
 * @version 1.0, 02/07/31
 */
public class DataSource40 extends AbstractDataSource {


    protected final static StringManager localStrings =
            StringManager.getManager(ManagedConnectionFactoryImpl.class);

    /**
     * Constructs <code>DataSource</code> object. This is created by the
     * <code>ManagedConnectionFactory</code> object.
     *
     * @param mcf <code>ManagedConnectionFactory</code> object
     *            creating this object.
     * @param cm  <code>ConnectionManager</code> object either associated
     *            with Application server or Resource Adapter.
     */
    public DataSource40(ManagedConnectionFactoryImpl mcf, ConnectionManager cm) {
        super(mcf, cm);
    }

    /**
     * Returns an object that implements the given interface to allow access to
     * non-standard methods, or standard methods not exposed by the proxy.
     * <p/>
     * If the receiver implements the interface then the result is the receiver
     * or a proxy for the receiver. If the receiver is a wrapper
     * and the wrapped object implements the interface then the result is the
     * wrapped object or a proxy for the wrapped object. Otherwise return the
     * the result of calling <code>unwrap</code> recursively on the wrapped object
     * or a proxy for that result. If the receiver is not a
     * wrapper and does not implement the interface, then an <code>SQLException</code> is thrown.
     *
     * @param iface A Class defining an interface that the result must implement.
     * @return an object that implements the interface. May be a proxy for the actual implementing object.
     * @throws java.sql.SQLException If no object found that implements the interface
     * @since 1.6
     */
    public <T> T unwrap(Class<T> iface) throws SQLException {
        T result;
        try {
            Object cds = mcf.getDataSource();

            if (iface.isInstance(cds)) {
                result = iface.cast(cds);
            } else if (cds instanceof java.sql.Wrapper) {
                result = ((java.sql.Wrapper) cds).unwrap(iface);
            } else {
                String msg = localStrings.getString("jdbc.feature_not_supported");
                throw new SQLException(msg);
            }
        } catch (ResourceException e) {
            _logger.log(Level.WARNING, "jdbc.exc_unwrap", e);
            throw new SQLException(e);
        }
        return result;
    }

    /**
     * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
     * for an object that does. Returns false otherwise. If this implements the interface then return true,
     * else if this is a wrapper then return the result of recursively calling <code>isWrapperFor</code> on the wrapped
     * object. If this does not implement the interface and is not a wrapper, return false.
     * This method should be implemented as a low-cost operation compared to <code>unwrap</code> so that
     * callers can use this method to avoid expensive <code>unwrap</code> calls that may fail. If this method
     * returns true then calling <code>unwrap</code> with the same argument should succeed.
     *
     * @param iface a Class defining an interface.
     * @return true if this implements the interface or directly or indirectly wraps an object that does.
     * @throws java.sql.SQLException if an error occurs while determining whether this is a wrapper
     *                               for an object with the given interface.
     * @since 1.6
     */
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        boolean result = false;
        try {
            Object cds = mcf.getDataSource();

            if (iface.isInstance(cds)) {
                result = true;
            } else if (cds instanceof java.sql.Wrapper) {
                result = ((java.sql.Wrapper) cds).isWrapperFor(iface);
            }
        } catch (ResourceException e) {
            _logger.log(Level.WARNING, "jdbc.exc_is_wrapper", e);
            throw new SQLException(e);
        }
        return result;
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        if(DataSourceObjectBuilder.isJDBC41()) {
            try {
                return (Logger) executor.invokeMethod(mcf.getDataSource().getClass(),
                    "getParentLogger", null);
            } catch (ResourceException ex) {
                _logger.log(Level.SEVERE, "jdbc.ex_get_parent_logger", ex);
                throw new SQLFeatureNotSupportedException(ex);
            }
        }
        throw new UnsupportedOperationException("Operation not supported in this runtime.");
    }
}
