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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.valves;


import org.apache.catalina.HttpResponse;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Request;
import org.apache.catalina.Response;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;


/**
 * <p>
 * This Tomcat extension logs server access directly to a database, and can 
 * be used instead of the regular file-based access log implemented in 
 * AccessLogValve.
 * To use, copy into the server/classes directory of the Tomcat installation
 * and configure in server.xml as:
 * <pre>
 * 		&lt;Valve className="AccessLogDBValve"
 *        	driverName="<i>your_jdbc_driver</i>"
 *        	connectionURL="<i>your_jdbc_url</i>"
 *        	pattern="combined" resolveHosts="false"
 * 		/&gt;
 * </pre>
 * </p>
 * <p>
 * Many parameters can be configured, such as the database connection (with
 * <code>driverName</code> and <code>connectionURL</code>),
 * the table name (<code>tableName</code>)
 * and the field names (corresponding to the get/set method names).
 * The same options as AccessLogValve are supported, such as
 * <code>resolveHosts</code> and <code>pattern</code> ("common" or "combined" 
 * only).
 * </p>
 * <p>
 * When Tomcat is started, a database connection (with autoReconnect option)
 * is created and used for all the log activity. When Tomcat is shutdown, the
 * database connection is closed.
 * This logger can be used at the level of the Engine context (being shared
 * by all the defined hosts) or the Host context (one instance of the logger 
 * per host, possibly using different databases).
 * </p>
 * <p>
 * The database table can be created with the following command:
 * </p>
 * <pre>
 * CREATE TABLE access (
 * id INT UNSIGNED AUTO_INCREMENT NOT NULL,
 * ts TIMESTAMP NOT NULL,
 * remoteHost CHAR(15) NOT NULL,
 * user CHAR(15),
 * timestamp TIMESTAMP NOT NULL,
 * virtualHost VARCHAR(64) NOT NULL,
 * method VARCHAR(8) NOT NULL,
 * query VARCHAR(255) NOT NULL,
 * status SMALLINT UNSIGNED NOT NULL,
 * bytes INT UNSIGNED NOT NULL,
 * referer VARCHAR(128),
 * userAgent VARCHAR(128),
 * PRIMARY KEY (id),
 * INDEX (ts),
 * INDEX (remoteHost),
 * INDEX (virtualHost),
 * INDEX (query),
 * INDEX (userAgent)
 * );
 * </pre>
 * <p>
 * If the table is created as above, its name and the field names don't need 
 * to be defined.
 * </p>
 * <p>
 * If the request method is "common", only these fields are used:
 * <code>remoteHost, user, timeStamp, query, status, bytes</code>
 * </p>
 * <p>
 * <i>TO DO: provide option for excluding logging of certain MIME types.</i>
 * </p>
 * 
 * @version 1.0
 * @author Andre de Jesus
 */

public final class JDBCAccessLogValve 
    /** CR 6411114 (Lifecycle implementation moved to ValveBase)
    extends ValveBase 
    implements Lifecycle {
    */
    // START CR 6411114
    extends ValveBase {
    // END CR 6411114


    // ----------------------------------------------------------- Constructors


    /**
     * Class constructor. Initializes the fields with the default values.
     * The defaults are:
     * <pre>
     * 		driverName = null;
     * 		connectionURL = null;
     * 		tableName = "access";
     * 		remoteHostField = "remoteHost";
     * 		userField = "user";
     * 		timestampField = "timestamp";
     * 		virtualHostField = "virtualHost";
     * 		methodField = "method";
     * 		queryField = "query";
     * 		statusField = "status";
     * 		bytesField = "bytes";
     * 		refererField = "referer";
     * 		userAgentField = "userAgent";
     * 		pattern = "common";
     * 		resolveHosts = false;
     * </pre>
     */
    public JDBCAccessLogValve() {
        super();
        driverName = null;
        connectionURL = null;
        tableName = "access";
        remoteHostField = "remoteHost";
        userField = "user";
        timestampField = "timestamp";
        virtualHostField = "virtualHost";
        methodField = "method";
        queryField = "query";
        statusField = "status";
        bytesField = "bytes";
        refererField = "referer";
        userAgentField = "userAgent";
        pattern = "common";
        resolveHosts = false;
        conn = null;
        ps = null;
        currentTimeMillis = new java.util.Date().getTime();
    }


    // ----------------------------------------------------- Instance Variables


    private String driverName;
    private String connectionURL;
    private String tableName;
    private String remoteHostField;
    private String userField;
    private String timestampField;
    private String virtualHostField;
    private String methodField;
    private String queryField;
    private String statusField;
    private String bytesField;
    private String refererField;
    private String userAgentField;
    private String pattern;
    private boolean resolveHosts;


    private Connection conn;
    private PreparedStatement ps;


    private long currentTimeMillis;


    /**
     * The descriptive information about this implementation.
     */
    private static final String info = 
        "org.apache.catalina.valves.JDBCAccessLogValve/1.0";


    /**
     * The lifecycle event support for this component.
     */
    /** CR 6411114 (Lifecycle implementation moved to ValveBase)
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);
    */


    /**
     * Has this component been started yet?
     */
    /** CR 6411114 (Lifecycle implementation moved to ValveBase)
    private boolean started = false;
    */


    // ------------------------------------------------------------- Properties


    /**
     * Sets the database driver name.
     * 
     * @param driverName The complete name of the database driver class.
     */
    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    /**
     * Sets the JDBC URL for the database where the log is stored.
     * 
     * @param connectionURL The JDBC URL of the database.
     */
    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }


    /**
     * Sets the name of the table where the logs are stored.
     * 
     * @param tableName The name of the table.
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }


    /**
     * Sets the name of the field containing the remote host.
     * 
     * @param remoteHostField The name of the remote host field.
     */
    public void setRemoteHostField(String remoteHostField) {
        this.remoteHostField = remoteHostField;
    }


    /**
     * Sets the name of the field containing the remote user name.
     * 
     * @param userField The name of the remote user field.
     */
    public void setUserField(String userField) {
        this.userField = userField;
    }


    /**
     * Sets the name of the field containing the server-determined timestamp.
     * 
     * @param timestampField The name of the server-determined timestamp field.
     */
    public void setTimestampField(String timestampField) {
        this.timestampField = timestampField;
    }


    /**
     * Sets the name of the field containing the virtual host information 
     * (this is in fact the server name).
     * 
     * @param virtualHostField The name of the virtual host field.
     */
    public void setVirtualHostField(String virtualHostField) {
        this.virtualHostField = virtualHostField;
    }


    /**
     * Sets the name of the field containing the HTTP request method.
     * 
     * @param methodField The name of the HTTP request method field.
     */
    public void setMethodField(String methodField) {
        this.methodField = methodField;
    }


    /**
     * Sets the name of the field containing the URL part of the HTTP query.
     * 
     * @param queryField The name of the field containing the URL part of 
     * the HTTP query.
     */
    public void setQueryField(String queryField) {
        this.queryField = queryField;
    }


  /**
   * Sets the name of the field containing the HTTP response status code.
   * 
   * @param statusField The name of the HTTP response status code field.
   */  
    public void setStatusField(String statusField) {
        this.statusField = statusField;
    }


    /**
     * Sets the name of the field containing the number of bytes returned.
     * 
     * @param bytesField The name of the returned bytes field.
     */
    public void setBytesField(String bytesField) {
        this.bytesField = bytesField;
    }


    /**
     * Sets the name of the field containing the referer.
     * 
     * @param refererField The referer field name.
     */
    public void setRefererField(String refererField) {
        this.refererField = refererField;
    }


    /**
     * Sets the name of the field containing the user agent.
     * 
     * @param userAgentField The name of the user agent field.
     */
    public void setUserAgentField(String userAgentField) {
        this.userAgentField = userAgentField;
    }


    /**
     * Sets the logging pattern. The patterns supported correspond to the 
     * file-based "common" and "combined". These are translated into the use 
     * of tables containing either set of fields.
     * <P><I>TO DO: more flexible field choices.</I></P>
     * 
     * @param pattern The name of the logging pattern.
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }


    /**
     * Determines whether IP host name resolution is done.
     * 
     * @param resolveHosts "true" or "false", if host IP resolution 
     * is desired or not.
     */
    public void setResolveHosts(String resolveHosts) {
        this.resolveHosts = Boolean.valueOf(resolveHosts).booleanValue();
    }


    // --------------------------------------------------------- Public Methods


    /**
     * This method is invoked by Tomcat on each query.
     * 
     * @param request The Request object.
     * @param response The Response object.
     * @exception IOException Should not be thrown.
     * @exception ServletException Database SQLException is wrapped 
     * in a ServletException.
     */    
     public int invoke(Request request, Response response)
         throws IOException, ServletException {

        return INVOKE_NEXT;
    }


    public void postInvoke(Request request, Response response)
                                    throws IOException, ServletException{

        ServletRequest req = request.getRequest();
        HttpServletRequest hreq = null;
        if(req instanceof HttpServletRequest) 
            hreq = (HttpServletRequest) req;
        String remoteHost = "";
        if(resolveHosts)
            remoteHost = req.getRemoteHost();
        else
            remoteHost = req.getRemoteAddr();
        String user = "";
        if(hreq != null)
            user = hreq.getRemoteUser();
        String query="";
        if(hreq != null)
            query = hreq.getRequestURI();
        int bytes = response.getContentCount();
        if(bytes < 0)
            bytes = 0;
        int status = ((HttpResponse)response).getStatus();

        synchronized (ps) {
            try {
                ps.setString(1, remoteHost);
                ps.setString(2, user);
                ps.setTimestamp(3, new Timestamp(getCurrentTimeMillis()));
                ps.setString(4, query);
                ps.setInt(5, status);
                ps.setInt(6, bytes);
            } catch(SQLException e) {
                throw new ServletException(e);
            }
            if (pattern.equals("common")) {
                try {
                    ps.executeUpdate();
                } catch(SQLException e) {
                    throw new ServletException(e);
                }
            } else if (pattern.equals("combined")) {
                String virtualHost = "";
                if(hreq != null)
                    virtualHost = hreq.getServerName();
                String method = "";
                if(hreq != null)
                    method = hreq.getMethod();
                String referer = "";
                if(hreq != null)
                    referer = hreq.getHeader("referer");
                String userAgent = "";
                if(hreq != null)
                    userAgent = hreq.getHeader("user-agent");
                try {
                    ps.setString(7, virtualHost);
                    ps.setString(8, method);
                    ps.setString(9, referer);
                    ps.setString(10, userAgent);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    throw new ServletException(e);
                }
            }
        }

    }	


    /**
     * Adds a Lifecycle listener.
     * 
     * @param listener The listener to add.
     */  
    /** CR 6411114 (Lifecycle implementation moved to ValveBase)
    public void addLifecycleListener(LifecycleListener listener) {

        lifecycle.addLifecycleListener(listener);

    }
    */


    /**
     * Get the lifecycle listeners associated with this lifecycle. If this 
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    /** CR 6411114 (Lifecycle implementation moved to ValveBase)
    public LifecycleListener[] findLifecycleListeners() {

        return lifecycle.findLifecycleListeners();

    }
    */


    /**
     * Removes a Lifecycle listener.
     * 
     * @param listener The listener to remove.
     */
    /** CR 6411114 (Lifecycle implementation moved to ValveBase)
    public void removeLifecycleListener(LifecycleListener listener) {

        lifecycle.removeLifecycleListener(listener);

    }
    */


    /**
     * Invoked by Tomcat on startup. The database connection is set here.
     * 
     * @exception LifecycleException Can be thrown on lifecycle 
     * inconsistencies or on database errors (as a wrapped SQLException).
     */
    public void start() throws LifecycleException {

        // START CR 6411114
        if (started)            // Ignore multiple starts
            return;
        super.start();
        // END CR 6411114

        try {
            Class.forName(driverName).newInstance(); 
        } catch (ClassNotFoundException e) {
            throw new LifecycleException(e);
        } catch (InstantiationException e) {
            throw new LifecycleException(e);
        } catch (IllegalAccessException e) {
            throw new LifecycleException(e);
        }
        Properties info = new Properties();
        info.setProperty("autoReconnect", "true");
        try {
            conn = DriverManager.getConnection(connectionURL, info);
            if (pattern.equals("common")) {
                ps = conn.prepareStatement
                    ("INSERT INTO " + tableName + " (" 
                     + remoteHostField + ", " + userField + ", "
                     + timestampField +", " + queryField + ", "
                     + statusField + ", " + bytesField 
                     + ") VALUES(?, ?, ?, ?, ?, ?)");
            } else if (pattern.equals("combined")) {
                ps = conn.prepareStatement
                    ("INSERT INTO " + tableName + " (" 
                     + remoteHostField + ", " + userField + ", "
                     + timestampField + ", " + queryField + ", " 
                     + statusField + ", " + bytesField + ", " 
                     + virtualHostField + ", " + methodField + ", "
                     + refererField + ", " + userAgentField
                     + ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            }
        } catch (SQLException e) {
            throw new LifecycleException(e);
        }

    }


    /**
     * Invoked by tomcat on shutdown. The database connection is closed here.
     * 
     * @exception LifecycleException Can be thrown on lifecycle 
     * inconsistencies or on database errors (as a wrapped SQLException).
     */
    public void stop() throws LifecycleException {

        // START CR 6411114
        if (!started)       // Ignore stop if not started
            return;
        // END CR 6411114

        try {
            if (ps != null)
                ps.close();
            if (conn != null)
                conn.close();
    	} catch (SQLException e) {
            throw new LifecycleException(e);	
        }
        // START CR 6411114
        super.stop();
        // END CR 6411114

    }


    public long getCurrentTimeMillis() {
        long systime  =  System.currentTimeMillis();
        if ((systime - currentTimeMillis) > 1000) {
            currentTimeMillis  =  new java.util.Date(systime).getTime();
        }
        return currentTimeMillis;
    }


}
