/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package javax.security.auth.message.config;

import java.util.Map;
import javax.security.auth.Subject;

import javax.security.auth.message.*;

/**
 * This interface encapsulates the configuration of ClientAuthContext objects
 * for a message layer and application context (for example, the messaging context of 
 * a specific application, or set of applications). 
 *
 * <p> Implementations of this interface are returned by an AuthConfigProvider.
 *
 * <p> Callers interact with a ClientAuthConfig to obtain ClientAuthContext
 * objects suitable for processing a given message exchange at the layer and
 * within the application context of the ClientAuthConfig.
 *
 * Each ClientAuthContext object is responsible for instantiating, 
 * initializing, and invoking the one or more ClientAuthModules 
 * encapsulated in the ClientAuthContext.
 *
 * <p> After having acquired a ClientAuthContext, a caller operates on the
 * context to cause it to invoke the encapsulated ClientAuthModules to
 * secure client requests and to validate server responses.
 *
 * @version %I%, %G%
 * @see AuthConfigProvider
 */
public interface ClientAuthConfig extends AuthConfig {

    /**
     * Get a ClientAuthContext instance from this ClientAuthConfig.
     *
     * <p> The implementation of this method returns a ClientAuthContext
     * instance that encapsulates the ClientAuthModules used to
     * secure and validate requests/responses associated
     * with the given <i>authContextID</i>.
     *
     * <p> Specifically, this method accesses this ClientAuthConfig
     * object with the argument <i>authContextID</i> to determine the
     * ClientAuthModules that are to be encapsulated in the returned
     * ClientAuthContext instance.
     * 
     * <P> The ClientAuthConfig object establishes the request 
     * and response MessagePolicy objects that are passed to the encapsulated 
     * modules when they are initialized by the returned ClientAuthContext 
     * instance. It is the modules' responsibility to enforce these policies 
     * when invoked.
     * 
     * @param authContextID An String identifier used to index
     *		the provided <i>config</i>, or null.
     *		This value must be identical to the value returned by
     *		the <code>getAuthContextID</code> method for all
     *		<code>MessageInfo</code> objects passed to the
     *		<code>secureRequest</code>
     *		method of the returned ClientAuthContext.
     *
     * @param clientSubject A Subject that represents the source of the 
     *          service request to be secured by the acquired authentication
     *          context. The principals and credentials of
     *          the Subject may be used to select or acquire the 
     *          authentication context. If the Subject is not null, 
     *          additional Principals or credentials (pertaining to the source 
     *          of the request) may be added to the Subject. A null value may 
     *          be passed for this parameter.
     *
     * @param properties A Map object that may be used by
     *          the caller to augment the properties that will be passed 
     *          to the encapsulated modules at module initialization.
     *          The null value may be passed for this parameter.
     *
     * @return A ClientAuthContext instance that encapsulates the
     *		ClientAuthModules used to secure and validate
     *		requests/responses associated with the given 
     *          <i>authContextID</i>,
     *		or null (indicating that no modules are configured).
     *
     * @exception AuthException If this method fails.
     */
    public abstract ClientAuthContext 
    getAuthContext(String authContextID, Subject clientSubject, Map properties) 
	throws AuthException;

}
