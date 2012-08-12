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

/*
 * DeploymentHelper.java
 *
 * Created on September 30, 2003.
 */

package com.sun.jdo.spi.persistence.support.sqlstore.ejb;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.sql.DataSource;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.jdo.api.persistence.support.JDOFatalUserException;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperPersistenceManager;
import com.sun.jdo.spi.persistence.utility.StringHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.persistence.common.DatabaseConstants;
import org.glassfish.persistence.common.I18NHelper;
import org.glassfish.persistence.common.Java2DBProcessorHelper;

/** 
 * This class is used for static method invocations to avoid unnecessary
 * registration requirements to use EJBHelper and/or CMPHelper from 
 * deploytool, verifier, or any other stand-alone client.
 * 
 */
public class DeploymentHelper
    {

    /** I18N message handler */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
        "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
        DeploymentHelper.class.getClassLoader());

    /** The logger */
    private static Logger logger = LogHelperPersistenceManager.getLogger();

    /** 
     * Returns name prefix for DDL files extracted from the info instance by the
     * Sun-specific code.
     *   
     * @param info the instance to use for the name generation.
     * @return name prefix as String. 
     */   
    public static String getDDLNamePrefix(Object info) { 
        return Java2DBProcessorHelper.getDDLNamePrefix(info);
    }

    /**
     * Returns boolean value for the <code>DatabaseConstants.JAVA_TO_DB_FLAG</code>
     * flag in this Properties object.
     * @param prop a Properties object where flag is located
     * @return true if there is a property value that contains "true" as
     * the value for the <code>DatabaseConstants.JAVA_TO_DB_FLAG</code>
     * key.
     */  
    public static boolean isJavaToDatabase(Properties prop) {
        if (prop != null) {
            String value = prop.getProperty(DatabaseConstants.JAVA_TO_DB_FLAG);
            if (! StringHelper.isEmpty(value)) {
                 if (logger.isLoggable(Logger.FINE))
                     logger.fine(DatabaseConstants.JAVA_TO_DB_FLAG + " property is set."); // NOI18N
                 return Boolean.valueOf(value).booleanValue();
            }
        }
        return false;
    }

    /** Get a Connection from the resource specified by the JNDI name 
     * of a CMP resource.
     * This connection is aquired from a non-transactional resource which does not 
     * go through transaction enlistment/delistment.
     * The deployment processing is required to use only those connections.
     *
     * @param name JNDI name of a cmp-resource for the connection.
     * @return a Connection.
     * @throws JDOFatalUserException if name cannot be looked up, or we
     * cannot get a connection based on the name.
     * @throws SQLException if can not get a Connection.
     */  
    public static Connection getConnection(String name) throws SQLException {
        if (logger.isLoggable(logger.FINE)) {
            logger.fine("ejb.DeploymentHelper.getconnection", name); //NOI18N
        }

        // TODO - pass Habitat or ConnectorRuntime as an argument.

        ServiceLocator habitat = Globals.getDefaultHabitat();
        DataSource ds = null;
        try {
            ConnectorRuntime connectorRuntime = habitat.getService(ConnectorRuntime.class);
            ds = DataSource.class.cast(connectorRuntime.lookupNonTxResource(name, true));
        } catch (Exception e) { 
            throw new JDOFatalUserException(
                I18NHelper.getMessage(messages,
                        "ejb.jndi.lookupfailed", name)); //NOI18N
        }
        return ds.getConnection();
    }    

    /** Create a RuntimeException for unexpected instance returned
     * from JNDI lookup.
     *
     * @param name the JNDI name that had been looked up.
     * @param value the value returned from the JNDI lookup.
     * @throws JDOFatalUserException.
     */
    private static void handleUnexpectedInstance(String name, Object value) {
        RuntimeException e = new JDOFatalUserException(
                I18NHelper.getMessage(messages,
                        "ejb.jndi.unexpectedinstance", //NOI18N
                        name, value.getClass().getName()));
        logger.severe(e.toString());
 
        throw e;
 
    }
}
