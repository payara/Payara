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
 * JmxServiceUrlFactory.java
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
import javax.management.remote.JMXServiceURL;
import java.net.MalformedURLException;
import java.util.logging.Logger;

/** A factory class to create the various JMXServiceURLs in various cases. This
 * is useful because there are many ways of creating the JMXServiceURL for
 * an intended connector server and client. The methods in this class can be 
 * used precisely for given case.
 */

public class JmxServiceUrlFactory {
    /** Field */
    public static final String RMI_JSR160_CS_JNDI_SUFFIX = "/management/rmi-jmx-connector";
    public static final String JCONSOLE_RMI_JSR160_CS_JNDI_SUFFIX = "/jmxrmi";    
    //hard-coding the logger name.
    private static final Logger logger      = Logger.getLogger("javax.enterprise.system.tools.admin");
    private JmxServiceUrlFactory() {
    }
    
    /** This API should be called when the <code> ConnectorServer </code> and 
     * following is true.
     * <ul>
     * <li> The protocol is RMI over JRMP. </li>
     * <li> There is no JNDI involved.
     * </ul>
     * 
     * After the ConnectorServer is created with this URL, its address will
     * contain the actual base-64 encoded stub that it can be used by ConnectorClients.
     *
     * In order to connect to such a ConnectorServer the client has to be provide
     * the stub to form the JMXServiceURL.
     * Bottom line: This API should be called when creating ConnectorServer.
     * @see JMXServiceURL
     */
    public static JMXServiceURL forRmiJrmpWithoutStub(final int port) {
        JMXServiceURL url;
        try {
            final String s = "service:jmx:rmi://" + localhost() + ":" + port;
            url = new JMXServiceURL(s);
            final String msg = "JmxServiceUrlFactory=>JMXServiceURL is: " + url;
            logger.fine(msg);
        }
        catch (MalformedURLException m) {
            url = null;
            //squelching the exception purposely, as I want to be sure.
        }
        assert (url != null): "Something seriously wrong, can't form the JMXServiceURL";
        return ( url );
    }

    /** This API should be called when the <code> ConnectorServer </code> and 
     * following is true.
     * <ul>
     * <li> The protocol is RMI over IIOP. </li>
     * <li> There is no JNDI involved.
     * </ul>
     * 
     * After the ConnectorServer is created with this URL, its address will
     * contain the actual base-64 encoded IOR that it can be used by ConnectorClients.

     * In order to connect to such a ConnectorServer the client has to be provide
     * the IIOP stub to form the JMXServiceURL.
     * Bottom line: This API should be called when creating ConnectorServer.
     * @see JMXServiceURL
     */

    public static JMXServiceURL forRmiIiopWithoutIor(final int port) {
        JMXServiceURL url;
        try {
            final String s = "service:jmx:iiop://" + localhost() + ":" + port;
            url = new JMXServiceURL(s);
            final String msg = "JMXServiceURL is: " + url;
            logger.fine(msg);
        }
        catch (MalformedURLException m) {
            url = null;
            //squelching the exception purposely, as I want to be sure.
        }
        assert (url != null): "Something seriously wrong, can't form the JMXServiceURL";
        return ( url );
    }
    
    /**  This is a method used by both JSR 160 connector server and connector clients
     * within the application server. <b> This API should be used by all the components within
     * the application server. </b> Note the following points about the returned JMXServiceURL.
     * <ul>
     * <li> Naming Service's host and port are provided. </li>
     * <li> The JNDI name of the stub is fixed. </li>
     * <li> This is the "ignored-host" kind of form. It always returns the URL which looks like
     * <code> service:jmx:rmi:///jndi/rmi://host:port/<Appserver's JNDI name></li>
     * </ul>
     * This is a symmetric form of URL.
     * @param host String representing the host name
     * @parm port int representing the port of naming service like rmi registry
     * @see JMXServiceUrlFactory#RMI_JSR160_CS_JNDI_SUFFIX
     */

    public static JMXServiceURL forRmiWithJndiInAppserver(final String host, final int port) {
        return ( forRmiWithJndi(host, port, RMI_JSR160_CS_JNDI_SUFFIX, false) );
    }
    
    public static JMXServiceURL forJconsoleOverRmiWithJndiInAppserver(final String host, final int port) {
        return ( forRmiWithJndi(host, port, JCONSOLE_RMI_JSR160_CS_JNDI_SUFFIX, false) );
    }
    
    /** A convenience method to create the JMXServiceURL for both Connector Client and
     * Connector Server. This API should be used when:
     * <ul>
     * <li> JNDI is used to obtain the stub. This must start with '/'. Must not be null. </li>
     * <li> Naming Service's host and port are known. </li>
     * <li> The JNDI name of the stub with which it is registered is known. </li>
     * </ul>
     * This is a symmetric form of URL.
     * @param host String representing the host name
     * @parm port int representing the port of naming service like rmi registry
     * @param jn String representing the JNDI name of the stub. This is known to the client
     * or server wants to specify it.
     * @param addHost boolean indicating the URL should have the host name or not. If
     * false what is returned is an an ignored-host form.
     */
    private static JMXServiceURL forRmiWithJndi(final String host, final int port, final String jn, final boolean addHost) {
        JMXServiceURL url;
        String hps = ""; //empty String
        if (host == null || jn == null) {
            throw new IllegalArgumentException("Null Argument");
        }
        if (! jn.startsWith("/"))
            throw new IllegalArgumentException("jndi-name must start with a /");
        if (addHost) {
            hps = host + ":" + port;
        }
        try {
            final String s = "service:jmx:rmi://" + hps + "/jndi/rmi://" + host + ":" + port  + jn;
            url = new JMXServiceURL(s);
            final String msg = "JMXServiceURL is: " + url;
            logger.fine(msg);
        }
        catch (MalformedURLException m) {
            url = null;
            //squelching the exception purposely, as I want to be sure. No real need to propagate.
        }
        assert (url != null): "Something seriously wrong, can't form the JMXServiceURL";
        return ( url );
    }
    /** A variant of forRmiWithJndi(String, int, String, boolean).
     * Returns the ignored host form with localhost as the naming host for
     * the given jndi name. 
     */
    private static JMXServiceURL forRmiWithJndi(final int port, final String jn) {
        return ( forRmiWithJndi(localhost(), port, jn, false) );
    }

    /** A variant of forRmiWithJndi(String, int, String, boolean).
     * Returns the System Jmx Connector address at the given port. The jndi
     * name is fixed by the field RMI_JSR160_CS_JNDI_SUFFIX.
     */
    private static JMXServiceURL forRmiWithJndi(final int port) {
        return ( forRmiWithJndi(port, RMI_JSR160_CS_JNDI_SUFFIX) );
    }
    /** Returns the JMXServiceURL for JMXMP and given host and port.
     * The host may not be null.
     */
    public static JMXServiceURL forJmxmp(final String host, final int port) {
        JMXServiceURL url;
        if (host == null)
            throw new IllegalArgumentException("Null Host");
        try {
            url = new JMXServiceURL("jmxmp", host, port);
            final String msg = "JMXServiceURL is: " + url;
            logger.fine(msg);
        }
        catch (MalformedURLException m) {
            url = null;
            //squelching the exception purposely, as I want to be sure.
        }
        assert (url != null): "Something seriously wrong, can't form the JMXServiceURL";
        return ( url );
    }
    
    public static JMXServiceURL forJmxmp(final int port) {
        return ( forJmxmp(localhost(), port) );
    }
    
    /*package private */static String localhost() throws RuntimeException {
        String h;
        try {
            h = java.net.InetAddress.getLocalHost().getCanonicalHostName();
        }
        catch (java.net.UnknownHostException e) {
            h = "localhost";
        }
        return ( h );
    }
    
    private static String concat(final String s1, final String s2, final String s3, final String s4) {
        final StringBuffer sb = new StringBuffer();
        if (s1 != null)
            sb.append(s1);
        if (s2 != null)
            sb.append(s2);
        if (s3 != null)
            sb.append(s3);
        if (s4 != null)
            sb.append(s4);
        return ( sb.toString() );
    }
}
