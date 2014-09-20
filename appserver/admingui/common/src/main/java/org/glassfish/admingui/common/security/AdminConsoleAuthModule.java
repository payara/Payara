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
package org.glassfish.admingui.common.security;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.RestResponse;
import org.glassfish.admingui.common.util.RestUtil;
import org.glassfish.grizzly.config.dom.NetworkListener;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.security.SecurityServicesUtil;

/**
 *  <p>	This class is responsible for providing the Authentication support
 *	needed by the admin console to both access the admin console pages
 *	as well as invoke REST requests.</p>
 */
public class AdminConsoleAuthModule implements ServerAuthModule {
    //public static final String TOKEN_ADMIN_LISTENER_PORT = "${ADMIN_LISTENER_PORT}";

    private CallbackHandler handler = null;

    private String restURL = null;

    private String loginPage = null;

    private String loginErrorPage = null;

    private static final Class[] SUPPORTED_MESSAGE_TYPES = new Class[]{HttpServletRequest.class, HttpServletResponse.class};

    private static final String SAVED_SUBJECT = "Saved_Subject";

    private static final String USER_NAME = "userName";

    private static final String ORIG_REQUEST_PATH = "origRequestPath";

    private static final String RESPONSE_TYPE = "application/json";

    /**
     *	The Session key for the REST Server Name.
     */
    public static final String REST_SERVER_NAME = "serverName";

    /**
     *	The Session key for the REST Server Port.
     */
    public static final String REST_SERVER_PORT = "serverPort";

    /**
     *	The Session key for the REST authentication token.
     */
    public static final String REST_TOKEN = "__rTkn__";

    private static final Logger logger = GuiUtil.getLogger();

    /**
     *	<p> This method configures this AuthModule and makes sure all the
     *	    information needed to continue is present.</p>
     */
    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options) throws AuthException {
        this.handler = handler;
        if (options != null) {
            this.loginPage = (String) options.get("loginPage");
            if (loginPage == null) {
                throw new AuthException("'loginPage' "
                        + "must be supplied as a property in the provider-config "
                        + "in the domain.xml file!");
            }
            this.loginErrorPage = (String) options.get("loginErrorPage");
            if (loginErrorPage == null) {
                throw new AuthException("'loginErrorPage' "
                        + "must be supplied as a property in the provider-config "
                        + "in the domain.xml file!");
            }
            ServiceLocator habitat = SecurityServicesUtil.getInstance().getHabitat();
            Domain domain = habitat.getService(Domain.class);
            NetworkListener adminListener = domain.getServerNamed("server").getConfig().getNetworkConfig().getNetworkListener("admin-listener");
            SecureAdmin secureAdmin = habitat.getService(SecureAdmin.class);

            final String host = adminListener.getAddress();
            // Save the REST URL we need to authenticate the user.
            this.restURL =  (SecureAdmin.Util.isEnabled(secureAdmin) ? "https://" : "http://") +
                    (host.equals("0.0.0.0") ? "localhost" : host) + ":" + adminListener.getPort() + "/management/sessions";
        }
    }

    /**
     *
     */
    @Override
    public Class[] getSupportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }

    /**
     *	<p> This is where the validation happens...</p>
     */
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        // Make sure we need to check...
        HttpServletRequest request =
                (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response =
                (HttpServletResponse) messageInfo.getResponseMessage();
        if (!isMandatory(messageInfo)
                && !request.getRequestURI().endsWith("/j_security_check")) {
            return AuthStatus.SUCCESS;
        }

        // See if we've already checked...
        HttpSession session = request.getSession(true);
        if (session == null) {
            return AuthStatus.FAILURE;
        }

        Subject savedClientSubject = (Subject) session.getValue(SAVED_SUBJECT);
        if (savedClientSubject != null) {
            // Copy all principals...
            clientSubject.getPrincipals().addAll(savedClientSubject.getPrincipals());
            clientSubject.getPublicCredentials().addAll(savedClientSubject.getPublicCredentials());
            clientSubject.getPrivateCredentials().addAll(savedClientSubject.getPrivateCredentials());
            return AuthStatus.SUCCESS;
        }

        // See if we've already calculated the serverName / serverPort
        if (session.getValue(REST_SERVER_NAME) == null) {
            // Save this for use later...
            URL url = null;
            try {
                url = new URL(restURL);
            } catch (MalformedURLException ex) {
                throw new IllegalArgumentException(
                        "Unable to parse REST URL: (" + restURL + ")", ex);
            }
            session.putValue(REST_SERVER_NAME, url.getHost());
            session.putValue(REST_SERVER_PORT, url.getPort());
        }

        // See if the username / password has been passed in...
        String username = request.getParameter("j_username");
        String password = request.getParameter("j_password");
        if ((username == null) || (password == null) || !request.getMethod().equalsIgnoreCase("post")) {
            // Not passed in, show the login page...
            String origPath = request.getRequestURI();
            String qs = request.getQueryString();
            if ((qs != null) && (!qs.isEmpty())) {
                origPath += "?" + qs;
            }
            session.setAttribute(ORIG_REQUEST_PATH, origPath);
            RequestDispatcher rd = request.getRequestDispatcher(loginPage);
            try {
                rd.forward(request, response);
            } catch (Exception ex) {
                AuthException ae = new AuthException();
                ae.initCause(ex);
                throw ae;
            }
            return AuthStatus.SEND_CONTINUE;
        }

// Don't use the PasswordValidationCallback... use REST authorization instead.
//	char[] pwd = new char[password.length()];
//	password.getChars(0, password.length(), pwd, 0);
//	PasswordValidationCallback pwdCallback =
//	    new PasswordValidationCallback(clientSubject, username, pwd);

        // Make REST Request

        Client client2 = RestUtil.initialize(ClientBuilder.newBuilder()).build();
        WebTarget target = client2.target(restURL);
        target.register(HttpAuthenticationFeature.basic(username, password));
        MultivaluedMap payLoad = new MultivaluedHashMap();
        payLoad.putSingle("remoteHostName", request.getRemoteHost());

        Response resp = target.request(RESPONSE_TYPE).post(Entity.entity(payLoad, MediaType.APPLICATION_FORM_URLENCODED), Response.class);
        RestResponse restResp = RestResponse.getRestResponse(resp);

        // Check to see if successful..
        if (restResp.isSuccess()) {
            // Username and Password sent in... validate them!
            CallerPrincipalCallback cpCallback =
                    new CallerPrincipalCallback(clientSubject, username);
            try {
                handler.handle(new Callback[]{ /*pwdCallback,*/cpCallback});
            } catch (Exception ex) {
                AuthException ae = new AuthException();
                ae.initCause(ex);
                throw ae;
            }

            request.changeSessionId();

                // Get the "extraProperties" section of the response...
            Object obj = restResp.getResponse().get("data");
            Map extraProperties = null;
            if ((obj != null) && (obj instanceof Map)) {
                obj = ((Map) obj).get("extraProperties");
                if ((obj != null) && (obj instanceof Map)) {
                    extraProperties = (Map) obj;
                }
            }

            // Save the Rest Token...
            if (extraProperties != null) {
                session.putValue(REST_TOKEN, extraProperties.get("token"));
            }

            // Save the Subject...
            session.putValue(SAVED_SUBJECT, clientSubject);

            // Save the userName
            session.putValue(USER_NAME, username);

            try {
                // Redirect...
                String origRequest = (String)session.getAttribute(ORIG_REQUEST_PATH);
                // Explicitly test for favicon.ico, as Firefox seems to ask for this on
                // every page
                if ((origRequest == null) || "/favicon.ico".equals(origRequest)) {
                    origRequest = "/index.jsf";
                }
                logger.log(Level.INFO, "Redirecting to {0}", origRequest);
                response.sendRedirect(response.encodeRedirectURL(origRequest));
            } catch (Exception ex) {
                AuthException ae = new AuthException();
                ae.initCause(ex);
                throw ae;
            }

            // Continue...
            return AuthStatus.SEND_CONTINUE;
        } else {
            int status = restResp.getResponseCode();
            if (status == 403) {
                request.setAttribute("errorText", GuiUtil.getMessage("alert.ConfigurationError"));
                request.setAttribute("messageText", GuiUtil.getMessage("alert.EnableSecureAdmin"));
            }
            RequestDispatcher rd = request.getRequestDispatcher(this.loginErrorPage);
            try {
                rd.forward(request, response);
            } catch (Exception ex) {
                AuthException ae = new AuthException();
                ae.initCause(ex);
                throw ae;
            }
            return AuthStatus.SEND_FAILURE;
        }
    }

    /**
     *
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return AuthStatus.SUCCESS;
    }

    /**
     *
     */
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        // FIXME: Cleanup...
    }

    /**
     *
     */
    private boolean isMandatory(MessageInfo messageInfo) {
        return Boolean.valueOf((String) messageInfo.getMap().get(
                "javax.security.auth.message.MessagePolicy.isMandatory"));
    }
}
