/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.appserver.monitoring.rest.service.security;

import com.sun.enterprise.security.SecurityServicesUtil;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.callback.CallerPrincipalCallback;
import jakarta.security.auth.message.callback.PasswordValidationCallback;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.glassfish.hk2.api.ServiceLocator;
import fish.payara.appserver.monitoring.rest.service.configuration.RestMonitoringConfiguration;

/**
 *
 * @author Andrew Pielage
 */
public class RestMonitoringAuthModule implements ServerAuthModule {

    private String contextRoot = null;
    private CallbackHandler handler = null;
    private boolean securityEnabled = false;

    private static final String DEFAULT_USER_NAME = "payara";
    private static final String ORIG_REQUEST_PATH = "origRequestPath";
    private static final String LOGIN_PAGE = "/login.xhtml";
    private static final Class[] SUPPORTED_MESSAGE_TYPES = new Class[] {HttpServletRequest.class, 
            HttpServletResponse.class };
    
    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options)
            throws AuthException {
        this.handler = handler;
        ServiceLocator habitat = SecurityServicesUtil.getInstance().getHabitat();
        RestMonitoringConfiguration restMonitoringConfiguration = habitat.getService(RestMonitoringConfiguration.class);

        contextRoot = restMonitoringConfiguration.getContextRoot();
        securityEnabled = Boolean.parseBoolean(restMonitoringConfiguration.getSecurityEnabled());
    }

    @Override
    public Class[] getSupportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) 
            throws AuthException {
        if (securityEnabled) {
            HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
            HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
            HttpSession session = request.getSession();
            
            // Check if our session has already been authenticated
            Principal userPrincipal = request.getUserPrincipal();
            if (userPrincipal != null) {
                try {
                    handler.handle(new Callback[] { new CallerPrincipalCallback(clientSubject, userPrincipal) });
                    
                    return AuthStatus.SUCCESS;
                } catch (IOException | UnsupportedCallbackException ex) {
                    AuthException ae = new AuthException();
                    ae.initCause(ex);
                    throw ae;
                }
            }
            
            // See if the username / password has been passed in...
            String username = request.getParameter("j_username");
            String password = request.getParameter("j_password");
            if ((username == null) || (password == null) || !request.getMethod().equalsIgnoreCase("post")) {
                // Not passed in, show the login page...
                String origPath = request.getRequestURI();
                String queryString = request.getQueryString();
                
                if ((queryString != null) && (!queryString.isEmpty())) {
                    origPath += "?" + queryString;
                }
                
                session.setAttribute(ORIG_REQUEST_PATH, origPath);
                RequestDispatcher rd = request.getRequestDispatcher(LOGIN_PAGE);
                
                try {
                    rd.forward(request, response);
                } catch (Exception ex) {
                    AuthException authException = new AuthException();
                    authException.initCause(ex);
                    throw authException;
                }
                
                return AuthStatus.SEND_CONTINUE;
            }
            
            // Authenticate the details
            PasswordValidationCallback pvCallback = new PasswordValidationCallback(clientSubject, username, 
                    password.toCharArray());
            
            try {
                handler.handle(new Callback[]{pvCallback});
            } catch (Exception ex) {
                AuthException ae = new AuthException();
                ae.initCause(ex);
                throw ae;
            }
            
            // Register the session as authenticated
            messageInfo.getMap().put("jakarta.servlet.http.registerSession", Boolean.TRUE.toString());
            
            // Redirect to original path
            try {
                String origRequest = (String) session.getAttribute(ORIG_REQUEST_PATH);

                if ((origRequest == null)) {
                    origRequest = contextRoot;
                }

                response.sendRedirect(response.encodeRedirectURL(origRequest));
            } catch (Exception ex) {
                AuthException ae = new AuthException();
                ae.initCause(ex);
                throw ae;
            }
            
            // Continue...
            return AuthStatus.SUCCESS;
        } else {
            Callback[] callbacks = new Callback[] { new CallerPrincipalCallback(clientSubject, DEFAULT_USER_NAME) };

            try {
                handler.handle(callbacks);
            } catch (IOException | UnsupportedCallbackException ex) {
                Logger.getLogger(RestMonitoringAuthModule.class.getName()).log(Level.SEVERE, null, ex);
            }

            return AuthStatus.SUCCESS;
        }  
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return AuthStatus.SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        if (subject != null) {
            subject.getPrincipals().clear();
        }
    }  
}
