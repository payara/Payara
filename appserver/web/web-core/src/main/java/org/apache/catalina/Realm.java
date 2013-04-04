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

package org.apache.catalina;

import org.apache.catalina.deploy.SecurityConstraint;
import org.jvnet.hk2.annotations.Contract;

import org.glassfish.hk2.api.PerLookup;

import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
/**
 * A <b>Realm</b> is a read-only facade for an underlying security realm
 * used to authenticate individual users, and identify the security roles
 * associated with those users.  Realms can be attached at any Container
 * level, but will typically only be attached to a Context, or higher level,
 * Container.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.6 $ $Date: 2007/04/18 17:27:22 $
 */

@Contract
@PerLookup
public interface Realm {

    // ------------------------------------------------------------- Constants

    //START SJSAS 6202703
    /**
     * Flag indicating authentication is needed for current request.  Used by
     * preAuthenticateCheck method.
     */
    public static final int AUTHENTICATE_NEEDED = 1;
    
    /**
     * Flag indicating authentication is not needed for current request. Used
     * by preAuthenticateCheck method.
     */
    public static final int AUTHENTICATE_NOT_NEEDED = 0;
    
    /**
     * Flag indicating the user has been authenticated but been denied access
     * to the requested resource.
     */
    public static final int AUTHENTICATED_NOT_AUTHORIZED = -1;

    //END SJSAS 6202703
    
    // ------------------------------------------------------------- Properties


    /**
     * Return the Container with which this Realm has been associated.
     */
    public Container getContainer();


    /**
     * Set the Container with which this Realm has been associated.
     *
     * @param container The associated Container
     */
    public void setContainer(Container container);


    /**
     * Return descriptive information about this Realm implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo();


    // --------------------------------------------------------- Public Methods

    
    /**
     * Add a property change listener to this component.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);


    /**
     * Return the Principal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     *
     * @param username Username of the Principal to look up
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     */
    public Principal authenticate(String username, char[] credentials);


    /**
     * Return the Principal associated with the specified username, which
     * matches the digest calculated using the given parameters using the
     * method described in RFC 2069; otherwise return <code>null</code>.
     *
     * @param username Username of the Principal to look up
     * @param digest Digest which has been submitted by the client
     * @param nonce Unique (or supposedly unique) token which has been used
     * for this request
     * @param realm Realm name
     * @param md5a2 Second MD5 digest used to calculate the digest :
     * MD5(Method + ":" + uri)
     */
    public Principal authenticate(String username, char[] digest,
                                  String nonce, String nc, String cnonce,
                                  String qop, String realm,
                                  char[] md5a2);


    /**
     * Return the Principal associated with the specified chain of X509
     * client certificates.  If there is none, return <code>null</code>.
     *
     * @param certs Array of client certificates, with the first one in
     *  the array being the certificate of the client itself.
     */
    public Principal authenticate(X509Certificate certs[]);
    
    /**
     * Return the SecurityConstraints configured to guard the request URI for
     * this request, or <code>null</code> if there is no such constraint.
     *
     * @param request Request we are processing
     */
    public SecurityConstraint[] findSecurityConstraints(HttpRequest request,
                                                        Context context);

    /**
     * Gets the security constraints configured by the given context
     * for the given request URI and method.
     *
     * @param uri the request URI
     * @param method the request method
     * @param context the context
     *
     * @return the security constraints configured by the given context
     * for the given request URI and method, or null
     */
    public SecurityConstraint[] findSecurityConstraints(String uri, 
                                                        String method,
                                                        Context context);

    /**
     * Perform access control based on the specified authorization constraint.
     * Return <code>true</code> if this constraint is satisfied and processing
     * should continue, or <code>false</code> otherwise.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraint Security constraint we are enforcing
     * @param context Context to which client of this class is attached.
     *
     * @exception IOException if an input/output error occurs
     */
    public boolean hasResourcePermission(HttpRequest request,
                                         HttpResponse response,
                                         SecurityConstraint[] constraint,
                                         Context context)
        throws IOException;
    
    
    /**
     * Return <code>true</code> if the specified Principal has the specified
     * security role, within the context of this Realm; otherwise return
     * <code>false</code>.
     *
     * @param principal Principal for whom the role is to be checked
     * @param role Security role to be checked
     */
    public boolean hasRole(Principal principal, String role);

    //START SJSAS 6232464
    /**
     * Return <code>true</code> if the specified Principal has the specified
     * security role, within the context of this Realm; otherwise return
     * <code>false</code>.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param principal Principal for whom the role is to be checked
     * @param role Security role to be checked
     */
    public boolean hasRole(HttpRequest request, 
                           HttpResponse response, 
                           Principal principal, 
                           String role);
    //END SJSAS 6232464
    
    //START SJSAS 6202703
    /**
     * Checks whether or not authentication is needed.
     * Returns an int, one of AUTHENTICATE_NOT_NEEDED, AUTHENTICATE_NEEDED,
     * or AUTHENTICATED_NOT_AUTHORIZED.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraints Security constraint we are enforcing
     * @param disableProxyCaching whether or not to disable proxy caching for
     *        protected resources.
     * @param securePagesWithPragma true if we add headers which 
     * are incompatible with downloading office documents in IE under SSL but
     * which fix a caching problem in Mozill
     * @param ssoEnabled true if sso is enabled
     * @exception IOException if an input/output error occurs
     */
    public int preAuthenticateCheck(HttpRequest request,
                                    HttpResponse response,
                                    SecurityConstraint[] constraints,
                                    boolean disableProxyCaching,
                                    boolean securePagesWithPragma,
                                    boolean ssoEnabled)
        throws IOException;    
    
    
    /**
     * Authenticates the user making this request, based on the specified
     * login configuration.  Return <code>true</code> if any specified
     * requirements have been satisfied, or <code>false</code> if we have
     * created a response challenge already.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param context The Context to which client of this class is attached.
     * @param authenticator the current authenticator.
     * @param calledFromAuthenticate true if the call originates from
     *                               HttpServletRequest.authenticate
     * @exception IOException if an input/output error occurs
     */
    public boolean invokeAuthenticateDelegate(HttpRequest request,
                                              HttpResponse response,
                                              Context context,
                                              Authenticator authenticator,
                                              boolean calledFromAuthenticate)
          throws IOException;

    /**
     * Post authentication for given request and response.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param context The Context to which client of this class is attached.
     * @exception IOException if an input/output error occurs
     */
    public boolean invokePostAuthenticateDelegate(HttpRequest request,
                                              HttpResponse response,
                                              Context context)
           throws IOException;
    
    //END SJSAS 6202703

        /**
     * Enforce any user data constraint required by the security constraint
     * guarding this request URI.  Return <code>true</code> if this constraint
     * was not violated and processing should continue, or <code>false</code>
     * if we have created a response already.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraint Security constraint being checked
     *
     * @exception IOException if an input/output error occurs
     */
    public boolean hasUserDataPermission(HttpRequest request,
        HttpResponse response, SecurityConstraint[] constraint)
            throws IOException;
    
    /**
     * Checks if the given request URI and method are the target of any
     * user-data-constraint with a transport-guarantee of CONFIDENTIAL,
     * and whether any such constraint is already satisfied.
     * 
     * If <tt>uri</tt> and <tt>method</tt> are null, then the URI and method
     * of the given <tt>request</tt> are checked.
     *
     * If a user-data-constraint exists that is not satisfied, then the 
     * given <tt>request</tt> will be redirected to HTTPS.
     *
     * @param request the request that may be redirected
     * @param response the response that may be redirected
     * @param constraints the security constraints to check against
     * @param uri the request URI (minus the context path) to check
     * @param method the request method to check
     *
     * @return true if the request URI and method are not the target of any
     * unsatisfied user-data-constraint with a transport-guarantee of
     * CONFIDENTIAL, and false if they are (in which case the given request
     * will have been redirected to HTTPS)
     */
    public boolean hasUserDataPermission(HttpRequest request,
                                         HttpResponse response,
                                         SecurityConstraint[] constraints,
                                         String uri, String method)
        throws IOException;

    /**
     * Remove a property change listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);


    // BEGIN IASRI 4808401, 4934562
    /**
     * Return an alternate principal from the request if available.
     *
     * @param req The request object.
     * @return Alternate principal or null.
     */
    public Principal getAlternatePrincipal(HttpRequest req);


    /**
     * Return an alternate auth type from the request if available.
     *
     * @param req The request object.
     * @return Alternate auth type or null.
     */
    public String getAlternateAuthType(HttpRequest req);
    // END IASRI 4808401


    // BEGIN IASRI 4856062,4918627,4874504
    /**
     * Set the name of the associated realm.
     *
     * @param name the name of the realm.
     */
    public void setRealmName(String name, String authMethod);
 
 
    /**
     * Returns the name of the associated realm.
     *
     * @return realm name or null if not set.
     */
    public String getRealmName();
    // END IASRI 4856062,4918627,4874504
    /**
     * Does digest authentication and returns the Principal associated with the username in the 
     * HTTP header.
     *
     * @param hreq HTTP servlet request.
     */
    public Principal authenticate(HttpServletRequest hreq);
    
    /**
     * Returns whether the specified ServletContext indicates that security
     * extension is enabled.
     * 
     * @param servletContext the ServletContext
     * @return true if security extension is enabled; false otherwise
     */
    public boolean isSecurityExtensionEnabled(ServletContext servletContext);

    /**
     * Logs out.
     * 
     * @param hreq the HttpRequest
     */
    public void logout(HttpRequest hreq);

}
