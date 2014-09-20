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

package com.sun.appserv.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The <code>javax.sql.DataSource</code> implementation of SunONE application 
 * server will implement this interface. An application program would be able
 * to use this interface to do the extended functionality exposed by SunONE 
 * application server.
 * <p>A sample code for getting driver's connection implementation would like 
 * the following.
 * <pre>
     InitialContext ic = new InitialContext();
     com.sun.appserv.DataSource ds = (com.sun.appserv.DataSOurce) ic.lookup("jdbc/PointBase"); 
     Connection con = ds.getConnection();
     Connection drivercon = ds.getConnection(con);

     // Do db operations.

     con.close();
   </pre>
 * 
 * @author Binod P.G
 */
public interface DataSource extends javax.sql.DataSource {

    /**
     * Retrieves the actual SQLConnection from the Connection wrapper 
     * implementation of SunONE application server. If an actual connection is
     * supplied as argument, then it will be just returned.
     *
     * @param con Connection obtained from <code>Datasource.getConnection()</code>
     * @return <code>java.sql.Connection</code> implementation of the driver.
     * @throws <code>java.sql.SQLException</code> If connection cannot be obtained.
     */
    public Connection getConnection(Connection con) throws SQLException;

    /**
     * Gets a connection that is not in the scope of any transaction. This
     * can be used to save performance overhead incurred on enlisting/delisting
     * each connection got, irrespective of whether its required or not.
     * Note here that this meethod does not fit in the connector contract
     * per se.
     *
     * @return <code>java.sql.Connection</code>
     * @throws <code>java.sql.SQLException</code> If connection cannot be obtained
     */
    public Connection getNonTxConnection() throws SQLException;

    /**
     * Gets a connection that is not in the scope of any transaction. This
     * can be used to save performance overhead incurred on enlisting/delisting
     * each connection got, irrespective of whether its required or not.
     * Note here that this meethod does not fit in the connector contract
     * per se.
     *
     * @param user User name for authenticating the connection
     * @param password Password for authenticating the connection
     * @return <code>java.sql.Connection</code>
     * @throws <code>java.sql.SQLException</code> If connection cannot be obtained
     */
    public Connection getNonTxConnection( String userName, String password ) 
        throws SQLException;


     /**
     * API to mark a connection as bad. If the application can determine that the connection
     * is bad, using this api, it can notify the resource-adapter which inturn will notify the
     * connection-pool. Connection-pool will drop and create a new connection.
     * eg:
     * <pre>
        com.sun.appserv.jdbc.DataSource ds=
           (com.sun.appserv.jdbc.DataSource)context.lookup("dataSource");
	  	Connection con = ds.getConnection();
	  	Statement stmt = null;
	  	try{
	 		 stmt = con.createStatement();
	 		 stmt.executeUpdate("Update");
	 	}catch(BadConnectionException e){
	 		dataSource.markConnectionAsBad(con) //marking it as bad for removal
	 	}finally{
	 		stmt.close();
	 		con.close(); //Connection will be destroyed while close or Tx completion
	    }
     * </pre>
     * @param conn <code>java.sql.Connection</code>
     */
    public void markConnectionAsBad(Connection conn);

}
