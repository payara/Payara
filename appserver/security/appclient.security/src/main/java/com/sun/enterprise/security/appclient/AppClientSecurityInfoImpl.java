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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019-2024] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.appclient;

import com.sun.enterprise.security.SecurityServicesUtil;
import com.sun.enterprise.security.UsernamePasswordStore;
import com.sun.enterprise.security.appclient.integration.AppClientSecurityInfo;
import com.sun.enterprise.security.auth.login.LoginCallbackHandler;
import com.sun.enterprise.security.auth.login.LoginContextDriver;
import com.sun.enterprise.security.common.ClientSecurityContext;
import com.sun.enterprise.security.common.SecurityConstants;
import com.sun.enterprise.security.common.Util;
import com.sun.enterprise.security.integration.AppClientSSL;
import com.sun.enterprise.security.ssl.SSLUtils;
import com.sun.logging.LogDomains;
import jakarta.inject.Inject;
import org.glassfish.appclient.client.acc.config.MessageSecurityConfig;
import org.glassfish.appclient.client.acc.config.Security;
import org.glassfish.appclient.client.acc.config.Ssl;
import org.glassfish.appclient.client.acc.config.TargetServer;
import org.glassfish.enterprise.iiop.api.IIOPSSLUtil;
import org.glassfish.epicyro.config.factory.file.AuthConfigFileFactory;
import org.jvnet.hk2.annotations.Service;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.util.List;

import java.util.logging.Logger;

import static jakarta.security.auth.message.config.AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Level.FINE;

/**
 *
 * @author Kumar
 */

@Service
public class AppClientSecurityInfoImpl implements AppClientSecurityInfo {

    private static Logger logger = null;

    static {
        logger = LogDomains.getLogger(AppClientSecurityInfoImpl.class, LogDomains.SECURITY_LOGGER);
    }
    
     private static final String DEFAULT_PARSER_CLASS =
        "com.sun.enterprise.security.appclient.ConfigXMLParser";
     
    private CallbackHandler callbackHandler;
    boolean isJWS;
    boolean useGUIAuth;
    private List<TargetServer> targetServers;
    private List<MessageSecurityConfig> msgSecConfigs;
    
    @Inject
    protected SSLUtils sslUtils;
    
    @Inject
    private SecurityServicesUtil secServUtil; 
    @Inject
    private Util util;
    @Inject
    private IIOPSSLUtil appClientSSLUtil;

    public void initializeSecurity(List<TargetServer> tServers, List<MessageSecurityConfig> configs, 
                                   CallbackHandler handler, String username, char[] password, 
                                   boolean isJWS, boolean useGUIAuth) {
       
           /* security init */
        this.isJWS = isJWS;
        this.useGUIAuth = useGUIAuth;
        if (handler != null) {
            this.callbackHandler = handler;
        } else {
            this.callbackHandler = new LoginCallbackHandler(useGUIAuth);
        }
        this.targetServers = tServers;
        this.msgSecConfigs = configs;
        
        //set the parser to ConfigXMLParser
        System.setProperty("config.parser", DEFAULT_PARSER_CLASS);
        util.setAppClientMsgSecConfigs(msgSecConfigs);
        try {
            // Setup Jakarta Authentication factory define default factory if it is not already defined
            String defaultFactory = java.security.Security.getProperty(DEFAULT_FACTORY_SECURITY_PROPERTY);
            logger.log(FINE, "AuthConfigFactory obtained from java.security.Security.getProperty(\"authconfigprovider.factory\"): {0}",
                    defaultFactory);
            if (defaultFactory == null) {
                java.security.Security.setProperty(DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFileFactory.class.getName());
            }
        } catch (Exception e) {
            logger.log(WARNING, "SEC9001: ACC: Error in initializing Jakarta Authentication Default Factory", e);
        }
        
        setSSLData(this.getTargetServers());
        if (username != null || password != null) {
            UsernamePasswordStore.set(username, password);
        }

        //why am i setting both?.
        secServUtil.setCallbackHandler(callbackHandler);
        util.setCallbackHandler(callbackHandler);
    }

    public int getCredentialEncoding(CredentialType type) {
        switch(type) {
            case USERNAME_PASSWORD :
                return SecurityConstants.USERNAME_PASSWORD;
            case CERTIFICATE :
                return SecurityConstants.CERTIFICATE;
            case ALL :
                return SecurityConstants.ALL;
            default :
                throw new RuntimeException("Unknown CredentialType");
        }
        
    }

    public Subject doClientLogin(CredentialType credType) {
        return LoginContextDriver.doClientLogin(this.getCredentialEncoding(credType), callbackHandler);
    }
    
    private AppClientSSL convert(Ssl ssl) {
        AppClientSSL appSSL = new AppClientSSL();
        appSSL.setCertNickname(ssl.getCertNickname());
        appSSL.setSsl3TlsCiphers(ssl.getSsl3TlsCiphers());
        appSSL.setTlsRollbackEnabled(ssl.isTlsRollbackEnabled());
        
        return appSSL;
    }
    
    private void setSSLData(List<TargetServer> tServers) {
        try {
            // Set the SSL related properties for ORB
            TargetServer tServer = tServers.get(0);
            // TargetServer is required.
	    //temp solution to target-server+ change in DTD
            // assuming that multiple servers can be specified but only 1st
	    // first one will be used.
	    Security security = tServer.getSecurity();
	    if (security == null) {
		logger.log(FINE,"No Security input set in ClientContainer.xml");
		// do nothing
		return;
	    }
	    Ssl ssl = security.getSsl();
	    if (ssl == null) {
		logger.log(FINE,"No SSL input set in ClientContainer.xml");
		// do nothing
		return;
		
	    }
	    //XXX do not use NSS in this release
	    //CertDb   certDB  = security.getCertDb();
	    sslUtils.setAppclientSsl(convert(ssl));	
            this.appClientSSLUtil.setAppClientSSL(convert(ssl));
	} catch (Exception ex) {

        }
    }

    public List<TargetServer> getTargetServers() {
        return targetServers;
    }

    public List<MessageSecurityConfig> getMsgSecConfigs() {
        return msgSecConfigs;
    }

    @Override
    public void clearClientSecurityContext() {
        ClientSecurityContext.setCurrent(null);
    }

    @Override
    public boolean isLoginCancelled() {
        boolean isCancelled = false;
        if(callbackHandler instanceof LoginCallbackHandler){
            isCancelled=((LoginCallbackHandler) callbackHandler).getCancelStatus();
        }
        return isCancelled;
    }
}
