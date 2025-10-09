/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.jaspic.config;

import static com.sun.enterprise.deployment.web.LoginConfiguration.CLIENT_CERTIFICATION_AUTHENTICATION;
import static com.sun.enterprise.security.jaspic.config.GFServerConfigProvider.HTTPSERVLET;
import static com.sun.enterprise.security.jaspic.config.HttpServletConstants.POLICY_CONTEXT;
import static com.sun.enterprise.security.jaspic.config.HttpServletConstants.WEB_BUNDLE;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.config.AuthConfigProvider;

import org.glassfish.internal.api.Globals;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.security.auth.realm.certificate.CertificateRealm;
import com.sun.enterprise.security.jacc.JaccWebAuthorizationManager;
import com.sun.enterprise.security.jaspic.WebServicesDelegate;

public class PayaraJaspicServletServices extends PayaraJaspicServices {
    
    public static final String AUTH_TYPE = "jakarta.servlet.http.authType";
    
    private String realmName;

    public PayaraJaspicServletServices(String appContext, Map<String, Object> map, CallbackHandler callbackHandler, String realmName, boolean isSystemApp, String defaultSystemProviderID) {

        WebBundleDescriptor webBundle = null;
        
        if (map != null) {
            webBundle = (WebBundleDescriptor) map.get(WEB_BUNDLE);
            if (webBundle != null) {
                LoginConfiguration loginConfig = webBundle.getLoginConfiguration();
                if (loginConfig != null
                        && CLIENT_CERTIFICATION_AUTHENTICATION.equals(loginConfig.getAuthenticationMethod())) {
                    this.realmName = CertificateRealm.AUTH_TYPE;
                } else {
                    this.realmName = realmName;
                }
            }
        }

        // Set realmName before init
        init(HTTPSERVLET, appContext, map, callbackHandler, Globals.get(WebServicesDelegate.class));

        if (webBundle != null) {
            String policyContextId = JaccWebAuthorizationManager.getContextID(webBundle);
            map.put(POLICY_CONTEXT, policyContextId);

            SunWebApp sunWebApp = webBundle.getSunDescriptor();
            String pid = (sunWebApp != null ? sunWebApp.getAttributeValue(sunWebApp.HTTPSERVLET_SECURITY_PROVIDER) : null);
            boolean nullConfigProvider = false;

            if (isSystemApp && (pid == null || pid.length() == 0)) {
                pid = defaultSystemProviderID;
                if (pid == null || pid.length() == 0) {
                    nullConfigProvider = true;
                }
            }

            if (((pid != null && pid.length() > 0) || nullConfigProvider) && !hasExactMatchAuthProvider()) {
                AuthConfigProvider configProvider = nullConfigProvider ? null : new GFServerConfigProvider(new HashMap<>(), null);
                
                // Register the Payara JASPIC provider
                
                String jaspicRegistrationId = factory.registerConfigProvider(
                    configProvider, HTTPSERVLET, appContext,
                    "Payara provider: " + HTTPSERVLET + ":" + appContext);
                
                setRegistrationId(jaspicRegistrationId);
            }
        }

    }

    // realmName must be set first and this is invoked inside the init()
    protected HandlerContext getHandlerContext(Map map) {
        return new HandlerContext() {
            public String getRealmName() {
                return realmName;
            }
        };
    }
}
