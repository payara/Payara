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

/* CVS information
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/https/HttpsUrlConnector.java,v 1.4 2005/12/25 04:26:32 tcfujii Exp $
 * $Revision: 1.4 $
 * $Date: 2005/12/25 04:26:32 $
 */

package com.sun.enterprise.admin.jmx.remote.https;

import java.util.logging.Logger;
import java.util.Map;
import javax.management.remote.JMXServiceURL;

import com.sun.enterprise.admin.jmx.remote.DefaultConfiguration;
import com.sun.enterprise.admin.jmx.remote.UrlConnector;
import com.sun.enterprise.admin.jmx.remote.https.SunOneBasicX509TrustManager;
import com.sun.enterprise.admin.jmx.remote.https.SunOneBasicHostNameVerifier;

import java.lang.reflect.Constructor;
import java.security.SecureRandom;
import java.security.GeneralSecurityException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.HostnameVerifier;

/** A Concrete implementation of UrlConnector that uses {@link java.net.URLConnection.openConnection} and
 * {@link javax.net.ssl.HttpsURLConnection} to communicate with the server. Sets up
 * the {@link SSLSocketFactory} and/or {@link SSLContext} and so that Trust Manager(s), Key Manager(s)
 * and Hostname Verifier can be customized. Refer to <a href = "http://java.sun.com/j2se/1.4.2/docs/guide/security/jsse/JSSERefGuide.html">
 * JSSE Guide </a> for more details.
 * <P>
 * The SSLContext is configurued for "SSLv3" protocol and the server is expected
 * to support that as the <a href="http://java.sun.com/j2se/1.4.2/docs/guide/security/jsse/JSSERefGuide.html#AppA">
 * appendix to JSSE guide </a> suggests that this is a standard protocol.
 *<P>
 * Following are additional configurations:
 * <ul>
 * <li> Default Trust Manager used is {@link SunOneBasicX509TrustManager} which checks the server's validity. </li>
 * <li> Key Manager allows selection of client's credentials to be sent tot he server. </li>
 * <li> Default Hostname Vetifier is {@link SunOneBasicHostNameVerifier} which has basic defense against spoofing attack. </li> * </ul>
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.0
 */

public class HttpsUrlConnector extends UrlConnector {
    
    private HostnameVerifier hv = null;
    private X509TrustManager[] tms = null;
    private X509KeyManager[] kms = null;
    private SSLSocketFactory ssf = null;
    
    public HttpsUrlConnector(JMXServiceURL serviceUrl, Map environment) {
        super(serviceUrl, environment);
        
        hv = (HostnameVerifier)environment.get(
                DefaultConfiguration.HOSTNAME_VERIFIER_PROPERTY_NAME);
        if (hv == null) 
            hv = new SunOneBasicHostNameVerifier(serviceUrl.getHost());

        //fetching any custom SSLSocketFactory passed through environment
        ssf = (SSLSocketFactory)environment.get(
                DefaultConfiguration.SSL_SOCKET_FACTORY);
        
        //No custom SSLScoketFactory passed. So now fetch the X509 based managers
        //to get the SSLSocketFactory configured using SSLContext
        if (ssf == null) {
            //fetching any trustmanagers passed through environment - default is 
            //SunOneBasicX509TrustManager
            Object tmgr = environment.get(DefaultConfiguration.TRUST_MANAGER_PROPERTY_NAME);
            if (tmgr instanceof X509TrustManager[]) 
                tms = (X509TrustManager[])tmgr;
            else if (tmgr instanceof X509TrustManager)
                tms = new X509TrustManager[] { (X509TrustManager)tmgr };
            else if (tmgr == null) {
                /*Class cls = Class.forName(DefaultConfiguration.DEFAULT_TRUST_MANAGER);        
                Constructor ctr = cls.getConstructor(new Class[] { String.class });
                X509TrustManager tm = (X509TrustManager) 
                    ctr.newInstance(new Object[] {serviceUrl} );
                tms = new X509TrustManager[] { tm };*/
                tms = new X509TrustManager[] { new SunOneBasicX509TrustManager(serviceUrl, environment) };
            }

            //fetching any keymanagers passed through environment - no defaults
            Object kmgr = environment.get(DefaultConfiguration.KEY_MANAGER_PROPERTY_NAME);
            if (kmgr instanceof X509KeyManager[]) 
                kms = (X509KeyManager[])kmgr;
            else if (kmgr instanceof X509KeyManager) 
                kms = new X509KeyManager[] { (X509KeyManager)kmgr };
        }

        initialize();
    }
    
    protected void validateJmxServiceUrl() throws RuntimeException {
        //additional validation
    }
    
    protected void validateEnvironment() throws RuntimeException {
        super.validateEnvironment();
    }
    
    private void initialize() {
        if (ssf == null) {
            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("SSLv3");
                sslContext.init(kms, tms, new SecureRandom());
            } catch(GeneralSecurityException e) {
                throw new RuntimeException(e);
            }

            if( sslContext != null ) 
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            
        } else HttpsURLConnection.setDefaultSSLSocketFactory(ssf);
        
        HttpsURLConnection.setDefaultHostnameVerifier( hv );
    }
}
