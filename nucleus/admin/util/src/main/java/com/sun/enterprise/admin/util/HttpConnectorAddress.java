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
 */

// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.admin.util;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;
import com.sun.enterprise.universal.GFBase64Encoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;


public final class HttpConnectorAddress {
    static final String HTTP_CONNECTOR = "http";
    static final String HTTPS_CONNECTOR = "https";
    public static final String  AUTHORIZATION_KEY     = "Authorization";
    private static final String AUTHORIZATION_TYPE = "Basic ";
    private static final String DEFAULT_PROTOCOL = "TLSv1.2";

    private String host;
    private int    port;
    private String path;
    private boolean secure;
    private AuthenticationInfo  authInfo;
    private boolean interactive = true;

    private SSLSocketFactory sslSocketFactory;

    private static final Logger logger = AdminLoggerInfo.getLogger();
    
    public HttpConnectorAddress() {
    }

    public HttpConnectorAddress(String host, int port) {
        this(host, port, false);
    }
    
    /**
     * construct an address which indicates the host, port and
     * security attributes desired.
     * @param host a host address
     * @param port a port number
     * @param secure a boolean indication of whether the connection should be
     *  secure (i.e. confidential) or not
     */
    public HttpConnectorAddress(String host, int port, boolean secure) {
        this(host, port, secure, null);
    }

    public HttpConnectorAddress(String host, int port, boolean secure, String path) {
        this(host, port, secure, path, null);
    }

    public HttpConnectorAddress(String host, int port, SSLSocketFactory sslSocketFactory) {
        this(host, port, true /* secure */, null /* path */, sslSocketFactory);
    }

    public HttpConnectorAddress(String host, int port, boolean secure, String path,
            SSLSocketFactory sslSocketFactory) {
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.path = path;
        this.sslSocketFactory = sslSocketFactory;
    }


    /**
     * Open a connection using the reciever and the given path
     * @param path the path to the required resource (path here is
     * the portion after the <code>hostname:port</code> portion of a URL)
     * @return a connection to the required resource. The
     * connection returned may be a sub-class of
     * <code>URLConnection</code> including
     * <code>HttpsURLConnection</code>. If the sub-class is a
     * <code>HttpsURLConnection</code> then this connection will
     * accept any certificate from any server where the server's
     * name matches the host name of this object. Specifically we
     * allows the certificate <em>not</em> to contain the name of
     * the server. This is a potential security hole, but is also a
     * usability enhancement.
     * @throws IOException if there's a problem in connecting to the
     * resource
     */
    public URLConnection openConnection(String path) throws IOException {
        if (path == null || path.trim().length() == 0)
            path = this.path;
        final URLConnection cnx = this.openConnection(this.toURL(path));
        if (! (cnx instanceof HttpsURLConnection)) {
            return cnx;
        }

        configureSSL((HttpsURLConnection) cnx);
        
        return cnx;
    }

    private void configureSSL(final HttpsURLConnection httpsCnx) throws IOException {
        httpsCnx.setHostnameVerifier(new BasicHostnameVerifier(this.host));
        httpsCnx.setSSLSocketFactory(getOrCreateSSLSocketFactory());
    }

    private synchronized SSLSocketFactory getOrCreateSSLSocketFactory() {
        /*
         * The SSL socket factory will have been assigned a value if this
         * connection was made from the DAS or an instance...that code would have
         * used the constructor which accepts an SSLSocketFactory as an argument.
         * (That socket factory should provide client authentication.)
         *
         * If that value is null then this connection is originating from
         * somewhere else - such as asadmin - and the socket factory should be
         * the one which uses SSL but does not provide client auth.
         */
        if (sslSocketFactory == null) {
            sslSocketFactory = createAdminSSLSocketFactory(null, null);
        }
        return sslSocketFactory;
    }

    private SSLSocketFactory createAdminSSLSocketFactory(String alias, String protocol) {
        try {
            if (protocol == null) {
                
                /** 
                 * PAYARA-542
                 * Check if the system property has been set to determine the
                 * HTTPS protocol to use, and set the protocol to be used to
                 * this value if it has. If it hasn't, or an unrecognised 
                 * protocol is entered, log a message and use TLSv1.2
                 */
                String clientHttpsProtocol = System.getProperty("fish.payara.clientHttpsProtocol");
                if (clientHttpsProtocol != null) {
                    switch (clientHttpsProtocol) {
                        case "TLSv1": protocol = "TLSV1";
                                        logger.log(Level.FINE, 
                                                AdminLoggerInfo.settingHttpsProtocol,
                                                protocol);
                                        break;
                        
                        case "TLSv1.1": protocol = "TLSv1.1";
                                        logger.log(Level.FINE, 
                                                AdminLoggerInfo.settingHttpsProtocol,
                                                protocol);
                                        break;
                                        
                        case "TLSv1.2": protocol = "TLSv1.2";
                                        logger.log(Level.FINE, 
                                                AdminLoggerInfo.settingHttpsProtocol,
                                                protocol);
                                        break;
                        
                        default:        protocol = DEFAULT_PROTOCOL;
                                        String[] logParams = {protocol, clientHttpsProtocol};
                                        
                                        logger.log(Level.INFO, 
                                                AdminLoggerInfo.unrecognisedHttpsProtocol, 
                                                logParams);
                                        break;
                    }
                } else {
                    protocol = DEFAULT_PROTOCOL;
                    logger.log(Level.FINE, AdminLoggerInfo.usingDefaultHttpsProtocol, protocol);
                }
            }
            
            SSLContext cntxt = SSLContext.getInstance(protocol);
            /*
             * Pass null for the array of KeyManagers.  That uses the default
             * ones, so if the user has loaded client keys into the standard
             * Java SE keystore they will be found.
             */
            AsadminTrustManager atm = new AsadminTrustManager();
            atm.setInteractive(interactive);
            cntxt.init(null, new TrustManager[] {atm}, null);

            return cntxt.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get the protocol prefix to be used for a connection for the
     * receiver
     * @return the protocol prefix - one of <code>http</code> or
     *<code>https</code> depending upon the security setting.
     */
    public String getConnectorType() {
        return this.isSecure() ? HTTPS_CONNECTOR : HTTP_CONNECTOR;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path == null ? "/" : path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public AuthenticationInfo getAuthenticationInfo() {
        return authInfo;
    }

    public void setAuthenticationInfo(AuthenticationInfo authInfo) {
        this.authInfo = authInfo;
    }

    /**
     * Set the security attribute
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }
  

    /**
     * Indicate if the receiver represents a secure address
     */
    public boolean isSecure() {
        return secure;
    }
    
    /**
     * Set the interactive mode for the connection.
     */
    public void setInteractive(boolean mode) {
        interactive = mode;
    }

    public URL toURL(String path) throws MalformedURLException{
        return new URL(getConnectorType(), getHost(), getPort(), 
                path == null ? "" : path);
    }

    public synchronized SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }
  
    private String getUser() {
        return authInfo != null ? authInfo.getUser() : "";
    }

    private String getPassword() {
        return authInfo != null ? authInfo.getPassword() : "";
    }

    private URLConnection openConnection(URL url) throws IOException    {
        return this.setOptions(this.makeConnection(url));
    }

    private URLConnection makeConnection(URL url) throws IOException {
        return ( url.openConnection() );
    }

    private URLConnection setOptions(URLConnection uc) {
        uc.setDoOutput(true);
        uc.setUseCaches(false);
        //uc.setRequestProperty("Content-type", "application/octet-stream");
        uc.setRequestProperty("Connection", "Keep-Alive"); 
        return this.setAuthentication(uc);
    }

    private URLConnection setAuthentication(URLConnection uc) {
        if (authInfo != null) {
            uc.setRequestProperty(AUTHORIZATION_KEY, this.getBasicAuthString());
        }
        return uc;
    }

    public final String getBasicAuthString() {
        /*
         * taking care of the descripancies in the Base64Encoder, for very
         * large lengths of passwords and/or usernames.
         * Abhijit did the analysis and as per his suggestion, replacing
         * a newline in Base64 encoded String by newline followed by a space
         * should work for any length of password, independent of the
         * web server buffer length. That investigation is still on, but
         * in the meanwhile, it was found that the replacement of newline
         * character with empty string "" works. Hence implementing the same.
         * Date: 10/10/2003.
         */
        String user = getUser();
        String password = getPassword();
        
        return 
            AUTHORIZATION_TYPE + 
            getBase64Encoded(
                (user == null ? "" : user) + 
                ":" + 
                (password == null ? "" : password))
                .replaceAll(System.getProperty("line.separator"), "");
    }
  
    private String getBase64Encoded(String clearString) {
        return new GFBase64Encoder().encode(clearString.getBytes());
    }

    public static class BasicHostnameVerifier implements HostnameVerifier {
        private final String host;
        public BasicHostnameVerifier(String host) {
            if (host == null)
                throw new IllegalArgumentException("null host");
            this.host = host;
        }

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return host.equals(s);
        }
    }
}
