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

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.*;

/**
 * This interface is implemented by objects that can be used to obtain
 * authentication context configuration objects, that is, 
 * <code>ClientAuthConfig</code> or <code>ServerAuthConfig</code> objects.
 *
 * <p> Authentication context configuration objects serve as sources of 
 * the authentication context objects, that is, <code>ClientAuthContext</code> or
 * <code>ServerAuthContext</code> objects, for a specific message layer
 * and messaging context.
 * 
 * <p> Authentication context objects encapsulate the initialization, 
 * configuration, and invocation of authentication modules, that is,
 * <code>ClientAuthModule</code> or <code>ServerAuthModule</code> objects, for
 * a specific message exchange within a specific message layer and 
 * messaging context.
 * 
 * <p> Callers do not directly operate on authentication modules.
 * Instead, they rely on a ClientAuthContext or ServerAuthContext
 * to manage the invocation of modules. A caller obtains an instance
 * of ClientAuthContext or ServerAuthContext by calling the respective
 * <code>getAuthContext</code> method on a <code>ClientAuthConfig</code>
 * or <code>ServerAuthConfig</code> object obtained from an 
 * AuthConfigProvider.
 *
 * <p> The following represents a typical sequence of calls for obtaining
 * a client authentication context object, and then using it to secure 
 * a request.
 * <ol>
 * <li>AuthConfigProvider provider;
 * <li>ClientAuthConfig config = provider.getClientAuthConfig(layer,appID,cbh);
 * <li>String authContextID = config.getAuthContextID(messageInfo);
 * <li>ClientAuthContext context = config.getAuthContext(authContextID,subject,properties);
 * <li>context.secureRequest(messageInfo,subject);
 * </ol>
 *
* <p> Every implementation of this interface must offer a public,
 * two argument constructor with the following signature:
 * <pre>
 * <code>
 * public AuthConfigProviderImpl(Map properties, AuthConfigFactory factory);
 * </code>
 *</pre>
 * where the properties argument may be null, and where all values and 
 * keys occurring in a non-null properties argument must be of type String.
 * When the factory argument is not null, it indicates that the
 * provider is to self-register at the factory by calling the following
 * method on the factory:
 * <pre>
 * <code>
 * public String 
 * registerConfigProvider(AuthConfigProvider provider, String layer, 
 *                        String appContext, String description);
 * </code>
 * </pre>
 * @version %I%, %G%
 *
 * @see ClientAuthContext
 * @see ServerAuthContext
 * @see AuthConfigFactory
 */
public interface AuthConfigProvider {

    /**
     * Get an instance of ClientAuthConfig from this provider.
     *
     * <p> The implementation of this method returns a ClientAuthConfig
     * instance that describes the configuration of ClientAuthModules
     * at a given message layer, and for use in an identified application
     * context.
     *
     * @param layer A String identifying the message layer
     *		for the returned ClientAuthConfig object. 
     *          This argument must not be null.
     *
     * @param appContext A String that identifies the messaging context 
     *          for the returned ClientAuthConfig object.
     *          This argument must not be null.
     *
     * @param handler A CallbackHandler to be passed to the ClientAuthModules
     *		encapsulated by ClientAuthContext objects derived from the
     *		returned ClientAuthConfig. This argument may be null,
     *		in which case the implementation may assign a default handler
     *		to the configuration. 
     *          The CallbackHandler assigned to the configuration must support 
     *          the Callback objects required to be supported by the profile
     *          of this specification being followed by the messaging runtime. 
     *          The CallbackHandler instance must be initialized with any 
     *          application context needed to process the required callbacks 
     *          on behalf of the corresponding application.
     *
     * @return A ClientAuthConfig Object that describes the configuration
     *		of ClientAuthModules at the message layer and messaging context
     *          identified by the layer and appContext arguments.
     *		This method does not return null.
     *
     * @exception AuthException If this provider does not support the 
     *          assignment of a default CallbackHandler to the returned 
     *          ClientAuthConfig.
     *
     * @exception SecurityException If the caller does not have permission
     *		to retrieve the configuration.
     */
    public ClientAuthConfig getClientAuthConfig
    (String layer, String appContext, CallbackHandler handler) 
	throws AuthException;

    /**
     * Get an instance of ServerAuthConfig from this provider.
     *
     * <p> The implementation of this method returns a ServerAuthConfig
     * instance that describes the configuration of ServerAuthModules
     * at a given message layer, and for a particular application context.
     *
     * @param layer A String identifying the message layer
     *		for the returned ServerAuthConfig object.
     *          This argument must not be null.
     *
     * @param appContext A String that identifies the messaging context 
     *          for the returned ServerAuthConfig object.
     *          This argument must not be null.
     *
     * @param handler A CallbackHandler to be passed to the ServerAuthModules
     *		encapsulated by ServerAuthContext objects derived from the
     *		returned ServerAuthConfig. This argument may be null,
     *		in which case the implementation may assign a default handler
     *		to the configuration.
     *          The CallbackHandler assigned to the configuration must support 
     *          the Callback objects required to be supported by the profile
     *          of this specification being followed by the messaging runtime. 
     *          The CallbackHandler instance must be initialized with any 
     *          application context needed to process the required callbacks 
     *          on behalf of the corresponding application.
     *
     * @return A ServerAuthConfig Object that describes the configuration
     *		of ServerAuthModules at a given message layer,
     *		and for a particular application context. 
     *          This method does not return null.
     *
     * @exception AuthException If this provider does not support the 
     *          assignment of a default CallbackHandler to the returned
     *          ServerAuthConfig.
     *
     * @exception SecurityException If the caller does not have permission
     *		to retrieve the configuration.
     */
    public ServerAuthConfig getServerAuthConfig	
    (String layer, String appContext, CallbackHandler handler)
	throws AuthException;

   /**
     * Causes a dynamic configuration provider to update its internal 
     * state such that any resulting change to its state is reflected in
     * the corresponding authentication context configuration objects 
     * previously created by the provider within the current process context. 
     *
     * @exception AuthException If an error occured during the refresh.
     *
     * @exception SecurityException If the caller does not have permission
     *		to refresh the provider.
     */
    public void refresh();

}



















