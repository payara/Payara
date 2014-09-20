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

package org.glassfish.internal.api;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import org.glassfish.grizzly.http.server.Request;
import org.jvnet.hk2.annotations.Contract;

/** Determines the behavior of administrative access to GlassFish v3. It should be enhanced to take into account
 *  Role-based Access Control. As of GlassFish v3, this takes care of authentication alone.
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 */
@Contract
public interface AdminAccessController {

    /**
     * Represents the possible types of access granted as the result of
     * logging in as an admin user.
     * <p>
     * <ul>
     * <li>FULL - the connection should be permitted full admin access, including
     * the ability to change the configuration
     * <li>READONLY - the connection should be permitted read but not write access
     * <li>FORBIDDEN - the connection is rejected because it is remote, secure admin
     * is not enabled, and the connection is not from the DAS to an instance
     * <li>NONE - no access permitted
     * </ul>
     * The calling logic is responsible for enforcing any restrictions as to
     * what access should be allowed vs. prohibited based on the returned Access value.
     * <p>
     * Some parts of the authentication logic throw an exception if the user cannot
     * be authenticated but there are some places where it just returns.
     * Hence the NONE case.
     */
    public static enum Access {
        FULL,
        READONLY,
        FORBIDDEN,
        NONE;
        
        public boolean isOK() {
            return this == FULL || this == READONLY;
        }
    }

    /** Authenticates the admin user by delegating to the underlying realm. The implementing classes
     *  should use the GlassFish security infrastructure constructs like LoginContextDriver. This method assumes that
     *  the realm infrastructure is available in both the configuration and runtime of the server.
     *  <p>
     *  Like the name suggests the method also ensures that the admin group membership is satisfied.
     * @param user String representing the user name of the user doing an admin opearation
     * @param password String representing clear-text password of the user doing an admin operation
     * @param realm String representing the name of the admin realm for given server
     * @param originHost the host from which the request was sent
     * @throws LoginException if the credentials do not authenticate
     * @throws RemoteAdminAccessException if the request is remote but remote access is disabled
     * @return Subject for an admin user
     */
    Subject loginAsAdmin(String user, String password,
            String realm, String originHost) throws LoginException;

    /** Authenticates the admin user by delegating to the underlying realm. The implementing classes
     *  should use the GlassFish security infrastructure constructs like LoginContextDriver. This method assumes that
     *  the realm infrastructure is available in both the configuration and runtime of the server.
     *  <p>
     *  This variant also logs the requester in as an admin if the specified Principal
     *  matches the Principal from the certificate in the truststore associated with
     *  the alias configured in the domain configuration.
     *
     *  Typically, methods invoking
     *  this variant should pass the Principal associated with the request as
     *  reported by the secure transport and the value from the X-GlassFish-admin header
     *  (null if no such header exists).
     * @Param request The Grizzly request containing the admin request
     * @throws LoginException if the credentials do not authenticate
     * @throws RemoteAdminAccessException if the request is remote but remote access is disabled
     * @return Subject for an admin user
     */
    Subject loginAsAdmin(
            Request request) throws LoginException;
    
    /** Authenticates the admin user by delegating to the underlying realm. The implementing classes
     *  should use the GlassFish security infrastructure constructs like LoginContextDriver. This method assumes that
     *  the realm infrastructure is available in both the configuration and runtime of the server.
     *  <p>
     *  This variant also logs the requester in as an admin if the specified Principal
     *  matches the Principal from the certificate in the truststore associated with
     *  the alias configured in the domain configuration.
     *
     *  Typically, methods invoking
     *  this variant should pass the Principal associated with the request as
     *  reported by the secure transport and the value from the X-GlassFish-admin header
     *  (null if no such header exists).
     * @Param request The Grizzly request containing the admin request
     * @param hostname the originating host
     * @throws LoginException if the credentials do not authenticate
     * @throws RemoteAdminAccessException if the request is remote but remote access is disabled
     * @return Subject for an admin user
     */
    Subject loginAsAdmin(
            Request request, String hostname) throws LoginException;
}
