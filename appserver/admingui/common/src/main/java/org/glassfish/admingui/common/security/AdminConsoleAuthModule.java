/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2024] [Payara Foundation and/or its affiliates]

package org.glassfish.admingui.common.security;

import static java.util.logging.Level.INFO;
import static jakarta.security.auth.message.AuthStatus.SEND_CONTINUE;
import static jakarta.security.auth.message.AuthStatus.SEND_FAILURE;
import static jakarta.security.auth.message.AuthStatus.SUCCESS;
import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Map;
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
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.RestResponse;
import org.glassfish.admingui.common.util.RestUtil;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.security.SecurityServicesUtil;

/**
 * <p>
 * This class is responsible for providing the Authentication support needed by the admin console to both access the
 * admin console pages as well as invoke REST requests.
 * </p>
 */
public class AdminConsoleAuthModule implements ServerAuthModule {

    private static final Logger logger = GuiUtil.getLogger();
    
    private static final Class<?>[] SUPPORTED_MESSAGE_TYPES = { HttpServletRequest.class, HttpServletResponse.class };

    private static final String SAVED_SUBJECT = "Saved_Subject";
    private static final String USER_NAME = "userName";
    private static final String ORIG_REQUEST_PATH = "__origRequestPath";
    private static final String RESPONSE_TYPE = "application/json";

    /**
     * The Session key for the REST Server Name.
     */
    public static final String REST_SERVER_NAME = "serverName";

    /**
     * The Session key for the REST Server Port.
     */
    public static final String REST_SERVER_PORT = "serverPort";

    /**
     * The Session key for the REST authentication token.
     */
    public static final String REST_TOKEN = "__rTkn__";

    private CallbackHandler handler;
    private String restURL;
    private String loginPage;
    private String loginErrorPage;

    /**
     * <p>
     * This method configures this AuthModule and makes sure all the information needed to continue is present.
     * </p>
     */
    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, @SuppressWarnings("rawtypes") Map options) throws AuthException {
        this.handler = handler;
        
        if (options != null) {
            loginPage = (String) options.get("loginPage");
            if (loginPage == null) {
                throw new AuthException(
                        "'loginPage' " + "must be supplied as a property in the provider-config " + "in the domain.xml file!");
            }
            
            loginErrorPage = (String) options.get("loginErrorPage");
            if (loginErrorPage == null) {
                throw new AuthException(
                        "'loginErrorPage' " + "must be supplied as a property in the provider-config " + "in the domain.xml file!");
            }
            
            // Save the REST URL we need to authenticate the user.
            restURL = getAuthenticationURL();
        }
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public Class[] getSupportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }

    /**
     * <p>
     * This is where the validation happens...
     * </p>
     */
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
        
        if (!isMandatory(messageInfo) && !request.getRequestURI().endsWith("/j_security_check")) {
            return doNothing(clientSubject);
        }

        HttpSession session = request.getSession(true);

        Subject savedClientSubject = (Subject) session.getAttribute(SAVED_SUBJECT);
        String savedUsername = (String) session.getAttribute(USER_NAME);
        
        if (savedClientSubject != null && savedUsername != null) {
            
            // Caller authenticated before, re-apply authentication for this request
            return notifyContainerAboutLogin(clientSubject, savedUsername);
        }

        // See if we've already calculated the serverName / serverPort
        if (session.getAttribute(REST_SERVER_NAME) == null) {
            saveServerHostPort(session);
        }

        // See if the username / password has been passed in...
        String username = request.getParameter("j_username");
        char[] password = request.getParameter("j_password") != null
                ? request.getParameter("j_password").toCharArray() : null;
        
        if (username == null || password == null || !request.getMethod().equalsIgnoreCase("post")) {
            
            // Credentials not passed in, show the login page
            return saveRequestAndForwardToLogin(session, request, response);
        }

        // Credentials provided, validte them via a REST based identity store
        RestResponse validationResult = validateCredentials(request, username, password);

        // Check to see if successful
        if (validationResult.isSuccess()) {
            
            notifyContainerAboutLogin(clientSubject, username);

            request.changeSessionId();

            // Get the "extraProperties" section from the validation result
            @SuppressWarnings("rawtypes")
            Map extraProperties = getExtraProperties(validationResult);

            // Save the Rest Token...
            if (extraProperties != null) {
                session.setAttribute(REST_TOKEN, extraProperties.get("token"));
            }

            // Save the Subject...
            session.setAttribute(SAVED_SUBJECT, clientSubject);

            // Save the userName
            session.setAttribute(USER_NAME, username);

            return redirectBack(session, request, response);
        } 
        
        // If we reach this location an error has occurred
        return forwardToErrorPage(validationResult, request, response);
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
    }
    
    
    
    // ### Private methods
    
    private AuthStatus doNothing(Subject clientSubject) throws AuthException {
        try {
            handler.handle(new Callback[] { new CallerPrincipalCallback(clientSubject, (Principal) null) });
            return SUCCESS;
        } catch (IOException | UnsupportedCallbackException e) {
            throw (AuthException) new AuthException().initCause(e);
        }
    }
    
    private void saveServerHostPort(HttpSession session) {
        try {
            URL url = new URL(restURL);
            
            session.setAttribute(REST_SERVER_NAME, url.getHost());
            session.setAttribute(REST_SERVER_PORT, url.getPort());
            
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Unable to parse REST URL: (" + restURL + ")", ex);
        }
    }
    
    /**
     * Compute the rest URL needed to authenticate a user
     * @return
     */
    private String getAuthenticationURL() {
        ServiceLocator habitat = SecurityServicesUtil.getInstance().getHabitat();
        
        Domain domain = habitat.getService(Domain.class);
        SecureAdmin secureAdmin = habitat.getService(SecureAdmin.class);
        
        NetworkListener adminListener = domain.getServerNamed("server")
                                              .getConfig()
                                              .getNetworkConfig()
                                              .getNetworkListener("admin-listener");

        String host = adminListener.getAddress();
        String port = adminListener.getPort();
        
        return
            (SecureAdmin.Util.isEnabled(secureAdmin) ? "https://" : "http://") + 
            (host.equals("0.0.0.0") ? "localhost" : host) + ":" + port + "/management/sessions";
    }
    
    private RestResponse validateCredentials(HttpServletRequest request, String username, char[] password) {
        WebTarget target = RestUtil.initialize(ClientBuilder.newBuilder())
                                   .build()
                                   .target(restURL)
                                   .register(HttpAuthenticationFeature.basic(username, new String(password)));
        
        MultivaluedMap<String, String> payLoad = new MultivaluedHashMap<>();
        payLoad.putSingle("remoteHostName", request.getRemoteHost());

        // Username and Password sent in... validate them!
        
        return RestResponse.getRestResponse(
                target.request(RESPONSE_TYPE)
                      .post(Entity.entity(payLoad, APPLICATION_FORM_URLENCODED), Response.class));
    }
    
    @SuppressWarnings("rawtypes")
    private Map getExtraProperties(RestResponse validationResult) {
        Object obj = validationResult.getResponse().get("data");
        Map extraProperties = null;
        if (obj instanceof Map) {
            obj = ((Map) obj).get("extraProperties");
            if (obj instanceof Map) {
                extraProperties = (Map) obj;
            }
        }
        
        return extraProperties;
    }
    
    private AuthStatus saveRequestAndForwardToLogin(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws AuthException {
        
        // Save original request path
        String originalPath = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            originalPath += "?" + queryString;
        }
        session.setAttribute(ORIG_REQUEST_PATH, originalPath);
        
        // Forward to login page
        try {
            request.getRequestDispatcher(loginPage)
                   .forward(request, response);
            
            return SEND_CONTINUE;
        } catch (Exception ex) {
            throw (AuthException) new AuthException().initCause(ex);
        }
    }
    
    private AuthStatus forwardToErrorPage(RestResponse validationResult, HttpServletRequest request, HttpServletResponse response) throws AuthException {
        if (validationResult.getResponseCode() == 403) {
            request.setAttribute("errorText", GuiUtil.getMessage("alert.ConfigurationError"));
            request.setAttribute("messageText", GuiUtil.getMessage("alert.EnableSecureAdmin"));
        }
        
        try {
            request.getRequestDispatcher(loginErrorPage)
                   .forward(request, response);
            
            return SEND_FAILURE;
        } catch (Exception ex) {
            throw (AuthException) new AuthException().initCause(ex);
        }
        
    }
    
    private AuthStatus notifyContainerAboutLogin(Subject clientSubject, String username) throws AuthException {
        try {
            handler.handle(new Callback[] { new CallerPrincipalCallback(clientSubject, username) });
            return SUCCESS;
        } catch (Exception ex) {
            throw (AuthException) new AuthException().initCause(ex);
        }
    }
    
    private AuthStatus redirectBack(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws AuthException {
        try {
            // Redirect...
            String origRequest = (String) session.getAttribute(ORIG_REQUEST_PATH);
            //clear session attribute for security reason
            session.removeAttribute(ORIG_REQUEST_PATH);

            // Explicitly test for favicon.ico, as Firefox seems to ask for this on
            // every page
            if (origRequest == null || "/favicon.ico".equals(origRequest)) {
                origRequest = "/index.jsf";
            }
            logger.log(INFO, "Redirecting to {0}", origRequest);
            response.sendRedirect(response.encodeRedirectURL(origRequest));
            
            return SEND_CONTINUE;
        } catch (Exception ex) {
            throw (AuthException) new AuthException().initCause(ex);
        }
    }

    private boolean isMandatory(MessageInfo messageInfo) {
        return Boolean.valueOf((String) messageInfo.getMap().get("jakarta.security.auth.message.MessagePolicy.isMandatory"));
    }
    
    
}
