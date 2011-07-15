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

import javax.security.auth.message.*;

/**
 * This interface defines the common functionality implemented by
 * Authentication context configuration objects.
 *
 * @version %I%, %G%
 * @see ClientAuthContext
 * @see ServerAuthContext
 */
public interface AuthConfig {
    /**
     * Get the message layer name of this authentication context configuration 
     * object.
     *
     * @return The message layer name of this configuration object, or null if 
     * the configuration object pertains to an unspecified message layer.
     */
    String getMessageLayer();


    /**
     * Get the application context identifier of this authentication 
     * context configuration object.
     *
     * @return The String identifying the application context of this
     * configuration object, or null if the configuration object pertains
     * to an unspecified application context.
     */
    String getAppContext();

    /**
     * Get the authentication context identifier corresponding to the
     * request and response objects encapsulated in messageInfo.
     *
     * @param messageInfo A contextual Object that encapsulates the
     *          client request and server response objects.
     *
     * @return The authentication context identifier corresponding to the 
     *          encapsulated request and response objects, or null.
     *
     * @throws IllegalArgumentException If the type of the message
     * objects incorporated in messageInfo are not compatible with
     * the message types supported by this 
     * authentication context configuration object.
     */

    public String getAuthContextID(MessageInfo messageInfo);

    /**
     * Causes a dynamic anthentication context configuration object to 
     * update the internal state that it uses to process calls to its
     * <code>getAuthContext</code> method.
     *
     * @exception AuthException If an error occured during the update.
     *
     * @exception SecurityException If the caller does not have permission
     *		to refresh the configuration object.
     */
    public void refresh();

    /**
     * Used to determine whether the authentication context configuration 
     * object encapsulates any protected authentication contexts.
     *
     * @return True if the configuration object encapsulates at least one
     *        protected authentication context. Otherwise, this method 
     *        returns false.
     */
    public boolean isProtected();
}

