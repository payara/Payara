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

import javax.security.auth.message.*;

// just for @see tag
import javax.security.auth.message.module.ServerAuthModule;

/**
 * This ServerAuthContext class encapsulates ServerAuthModules that are used
 * to validate service requests received from clients, and to secure any 
 * response returned for those requests.  A caller typically uses this class
 * in the following manner:
 *
 * <ol>
 * <li> Retrieve an instance of this class via
 *      ServerAuthConfig.getAuthContext.
 * <li> Invoke <i>validateRequest</i>.
 *      <br>
 *	ServerAuthContext implementation invokes validateRequest of 
 *      one or more encapsulated
 *	ServerAuthModules.  Modules validate credentials present in request
 *	(for example, decrypt and verify a signature).
 * <li> If credentials valid and sufficient, authentication complete.	
 *      <br>
 *      Perform authorization check on authenticated identity and,
 *	if successful, dispatch to requested service application.
 * <li> Service application finished.
 * <li> Invoke <i>secureResponse</i>.
 *      <br>
 *	ServerAuthContext implementation invokes secureResponse of 
 *      one or more encapsulated
 *	ServerAuthModules.  Modules secure response
 *	(sign and encrypt response, for example), and prepare response message.
 * <li> Send secured response to client.
 * <li> Invoke <i>cleanSubject</i> (as necessary)
 *	to clean up any authentication state in Subject(s).
 * </ol>
 *
 * <p> A ServerAuthContext instance may be used concurrently by multiple
 * callers.
 *
 * <p> Implementations of this interface are responsible for constructing
 * and initializing the encapsulated modules.  The initialization step
 * includes passing the relevant request and response MessagePolicy objects
 * to the encapsulated modules. The MessagePolicy objects are obtained
 * by the ServerAuthConfig instance used to ontain the ServerAuthContext 
 * object.
 * See <code>ServerAuthConfig.getAuthContext</code> for more information.
 *
 * <p> Implementations of this interface are instantiated by their associated
 * configuration object such that they know which modules to invoke, in what 
 * order, and how results returned by preceding modules are to influence 
 * subsequent module invocations.
 *
 * <p> Calls to the inherited methods of this interface delegate to the
 * corresponding methods of the encapsulated authentication modules.
 *
 * @version %I%, %G%
 * @see ServerAuthConfig
 * @see ServerAuthModule
 */
public interface ServerAuthContext extends ServerAuth {

}








