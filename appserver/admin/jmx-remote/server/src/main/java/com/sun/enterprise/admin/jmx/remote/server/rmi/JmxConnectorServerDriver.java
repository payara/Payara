/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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
 * JmxConnectorServerDriver.java
 * Indentation Information:
 * 0. Please (try to) preserve these settings.
 * 1. No tabs are used, all spaces.
 * 2. In vi/vim -
 *      :set tabstop=4 :set shiftwidth=4 :set softtabstop=4
 * 3. In S1 Studio -
 *      1. Tools->Options->Editor Settings->Java Editor->Tab Size = 4
 *      2. Tools->Options->Indentation Engines->Java Indentation Engine->Expand Tabs to Spaces = True.
 *      3. Tools->Options->Indentation Engines->Java Indentation Engine->Number of Spaces per Tab = 4.
 * Unit Testing Information:
 * 0. Is Standard Unit Test Written (y/n):
 * 1. Unit Test Location: (The instructions should be in the Unit Test Class itself).
 */

package com.sun.enterprise.admin.jmx.remote.server.rmi;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.HashMap;
import java.rmi.RemoteException;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMIClientSocketFactory;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.management.MBeanServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import com.sun.enterprise.admin.jmx.remote.IStringManager;
import com.sun.enterprise.admin.jmx.remote.StringManagerFactory;
import com.sun.enterprise.admin.jmx.remote.server.rmi.RemoteJmxProtocol;
import com.sun.enterprise.admin.jmx.remote.server.rmi.JmxServiceUrlFactory;

/** A Class to initialize the JMX Connector Servers in the system. It has its own
 * configuration that determines the characteristics of the connector server end
 * that will be started. It is important to note the various configurable
 * parameters provided by this class.
 * Implementation Note: This class is <code> not </code> thread safe. Callers
 * have to take care of serialization of calls when necessary.
 * @author  kedar
 * @since Sun Java System Application Server 8.1
 */

public class JmxConnectorServerDriver {
    private static IStringManager sm = StringManagerFactory.getServerStringManager( JmxConnectorServerDriver.class );
    private final Map env;
    private JMXServiceURL url;
    private JMXServiceURL jconsoleurl;
    private int port;
    private RemoteJmxProtocol protocol;
    private boolean secureRegistry;
    private boolean ssl;
    private RMIServerSocketFactory rmissf;
    private RMIClientSocketFactory rmicsf;
    private boolean auth;
    private JMXAuthenticator authenticator;
    private Logger logger;
    private MBeanServer mbs;
    
    /** Creates a JMXConnectorServerDriver instance with default values for
     * the various parameters. The protocol defaults to "rmi/jrmp". The rmi
     registry is not secure by default. An available port is selected. The 
     authentication is turned off by default. */
    
    public JmxConnectorServerDriver() {
        this.env = new HashMap();
        this.protocol = RemoteJmxProtocol.RMIJRMP;
        this.secureRegistry = false;
        this.port = getFreePort();
        this.ssl = false;
        this.auth = false;
        this.authenticator = null;
        logger = Logger.getLogger(this.getClass().getName());
    }
    
    /** Sets the protocol to one of the values in RemoteJmxProtocol.
     * @see RemoteJmxProtocol
     */
    public void setProtocol(final RemoteJmxProtocol protocol) {
        this.protocol = protocol;
    }
    /** Sets the port to the given value.
     * Note that NO checks whatsoever made for the validity of port 
     */
    public void setPort(final int port) {
        // not going to check for port validity
        this.port = port;
    }
    /** Sets if Transport Layer Security is on. Additional configuration is needed when ssl is on.
     */
    public void setSsl(final boolean ssl) {
        this.ssl = ssl;
    }
    
    /** Sets the RMIServerSocketFactory. This is the custom server socket factory that
     * RMI Connector Server would use. Really useful only if ssl is set to true by
     * using setSsl(true). If ssl is false, this value is not passed on to the
     * environmental map of connector server. If ssl is true then this value can not be null.
     */
    public void setRmiServerSocketFactory(final RMIServerSocketFactory f) {
        if (ssl && f == null)
            throw new IllegalArgumentException("Internal: null server socket factory passed with ssl ON");
        this.rmissf = f;
    }
    /** Sets the RMIClientSocketFactory. This is the custom client socket factory that
     * RMI Connector Server would use. Really useful only if ssl is set to true by
     * using setSsl(true). If ssl is false, this value is not passed on to the
     * environmental map of connector server. If ssl is true this value can not be null.
     */
    public void setRmiClientSocketFactory(final RMIClientSocketFactory f) {
        if (ssl && f == null)
            throw new IllegalArgumentException("Internal: null client socket factory passed with ssl ON");
        this.rmicsf = f;
    }
    /** Sets Authentication value. If true, then every connection establishment
     * should be authenticated. This is generally done by implementation of 
     * JMXAuthenticator interface.
     */
    public void setAuthentication(final boolean auth) {
        this.auth = auth;
    }
    /** Sets the Logger */
    public void setLogger (final Logger logger) {
        if (logger == null)
            throw new IllegalArgumentException("Internal: null logger");
        this.logger = logger;
    }
    /** Sets the Authenticator Object. This Object is responsible for
     * authenticating the connection.
     */
    public void setAuthenticator(final JMXAuthenticator authenticator) {
        //TODO
        //if (authenticator == null)
            //throw new IllegalArgumentException ("null authenticator");
        this.authenticator = authenticator;
    }
    /** Sets if the rmi registry is secure.
     */
    public void setRmiRegistrySecureFlag(final boolean secure) {
        this.secureRegistry = secure;
    }
    /** Sets the MBeanServer that will be associated with the created Connector Server.
        May not be null.
     * @throws IllegalArgumentException in case the argument is null.
     */
    public void setMBeanServer(final MBeanServer mbs) {
        if (mbs == null)
            throw new IllegalArgumentException ("null mbs");
        this.mbs = mbs;
    }
    /** Starts the configured ConnectorServer. Note that the same
     * connector server is returned. Internally the naming service may be
     * started if need be. In case of RMI Connector, as of June 2004 the
     * rmi registry is started.
     * @throws IOException if the connector server could not be started.
     */
    public JMXConnectorServer startConnectorServer() throws IOException {
        //using the jndi form everywhere as stub-form is not usable.
        prepare();
        formJmxServiceUrl();
        createEnvironment();
        
        final JMXConnectorServer cs = 
            JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);

        cs.start();
        logStartup(cs);
        return ( cs );
    }
    
    public JMXConnectorServer startJconsoleConnectorServer() throws IOException {
        // This env is ditto with the System JMX Connector Server, except SSL ClientSocketFactory.
        final RMIClientSocketFactory cf = new SslRMIClientSocketFactory();
        final Map jconsoleenv = new HashMap(env);
        if (ssl) 
            jconsoleenv.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, cf);
        final JMXConnectorServer jconsolecs = 
            JMXConnectorServerFactory.newJMXConnectorServer(jconsoleurl, jconsoleenv, mbs);
        jconsolecs.start();
        logJconsoleStartup(jconsolecs);
        return ( jconsolecs );
    }

    /** A wrapper to shutdown the passed connector server. This method, in that
     * regard just behaves like a static method. <code> Note that ref may not
     * be null. </code>
     * @param cs instance of JMXConnectorServer that needs to be stopped
     * @throws IOException in case the stopping process throws any exception
     */
    public void stopConnectorServer(final JMXConnectorServer cs) throws IOException {
        final String cad = cs.getAddress().toString();
        if (cs.isActive()) {
            logger.log(Level.FINE, "rjmx.lc.stopping", cad);
            cs.stop();
        }
        else {
            final String msg = "JMX Connector Server: " + cad + " is not active";
            logger.fine(msg);
        }
    }
    /* Private Methods - Start */
    private void logStartup(final JMXConnectorServer cs) {
        logger.log(Level.FINE, "rjmx.lc.address", cs.getAddress().toString());
        logger.log(Level.FINE, "rjmx.lc.status", "" + cs.isActive());
    }
    
    
    private void logJconsoleStartup(final JMXConnectorServer cs) {
        logger.log(Level.INFO, "rjmx.std.address", cs.getAddress().toString());
        logger.log(Level.INFO, "rjmx.std.status", "" + cs.isActive());
    }
    
    private void formJmxServiceUrl() {
        //Note that the Connector Server can only be started on the host where this method is called
        if (protocol == RemoteJmxProtocol.RMIJRMP) {
            this.url = JmxServiceUrlFactory.forRmiWithJndiInAppserver(
                JmxServiceUrlFactory.localhost(), this.port);
            this.jconsoleurl = JmxServiceUrlFactory.forJconsoleOverRmiWithJndiInAppserver(
                JmxServiceUrlFactory.localhost(), this.port);
        }
    }
    private void prepare() {
        if (protocol == RemoteJmxProtocol.RMIJRMP) {
            new RmiStubRegistryHandler(port, secureRegistry, logger);
        }
    }
    
    private void createEnvironment() {
        env.clear();
        handleSsl();        
        handleAuth();
    }
    
    private void handleSsl () {
        if (protocol == RemoteJmxProtocol.RMIJRMP)
            env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, rmissf);
        if (ssl) env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, rmicsf);
    }
    private void handleAuth() {
        if (protocol == RemoteJmxProtocol.RMIJRMP || 
            protocol == RemoteJmxProtocol.RMIIIOP) {
            if (auth) {
                if (authenticator == null) {
                    String msg = "Internal: The authentication is on, but the authenticator is null";
                    throw new IllegalArgumentException("msg");
                }
                env.put(JMXConnectorServer.AUTHENTICATOR, authenticator);
            }
        }
    }
    
    /**
        Gets a free port at the time of call to this method. 
        The logic leverages the built in java.net.ServerSocket implementation
        which binds a server socket to a free port when instantiated with
        a port <code> 0 </code>.
        <P> Note that this method guarantees the availability of the port
        only at the time of call. The method does not bind to this port.
        <p> Checking for free port can fail for several reasons which may
        indicate potential problems with the system. This method acknowledges
        the fact and following is the general contract:
        <li> Best effort is made to find a port which can be bound to. All 
        the exceptional conditions in the due course are considered SEVERE.
        <li> If any exceptional condition is experienced, <code> 0 </code>
        is returned, indicating that the method failed for some reasons and
        the callers should take the corrective action. (The method need not
        always throw an exception for this).
        <li> Method is synchronized on this class.
        @return integer depicting the free port number available at this time
        0 otherwise.
    */
    public static int getFreePort() {
        int                             freePort        = 0;
        boolean                         portFound       = false;
        ServerSocket                    serverSocket    = null;

        synchronized (JmxConnectorServerDriver.class) {
            try {
                /*following call normally returns the free port,
                  to which the ServerSocket is bound. */
                serverSocket = new ServerSocket(0);
                freePort = serverSocket.getLocalPort();
                portFound = true;
            } catch(Exception e) {
                //squelch the exception
            } finally {
                if (!portFound) freePort = 0;
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                        if (! serverSocket.isClosed())
                            throw new Exception("local exception ...");
                    }
                } catch(Exception e) {
                    //squelch the exception
                    freePort = 0;
                }
            }
            return freePort;
        }
    }
    
    /* Private Methods - End */
}
