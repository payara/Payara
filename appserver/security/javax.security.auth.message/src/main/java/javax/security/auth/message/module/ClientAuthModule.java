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

package javax.security.auth.message.module;

import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import javax.security.auth.message.*;

// just for @see tag
import javax.security.auth.message.config.ClientAuthContext;

/**
 * A ClientAuthModule secures request messages, and validates received
 * response messages.
 *
 * <p> A module implementation should assume it may be used to secure
 * different requests as different clients. A module should also assume
 * it may be used concurrently by multiple callers.  It is the module
 * implementation's responsibility to properly save and restore any state
 * as necessary.  A module that does not need to do so
 * may remain completely stateless.
 *
 * <p> Every implementation of the interface must provide a public zero 
 * argument contructor.
 *
 * @version %I%, %G%
 *
 * @see ClientAuthContext
 */
public interface ClientAuthModule extends ClientAuth {

    /**
     * Initialize this module with request and response message policies
     * to enforce, a CallbackHandler, and any module-specific configuration
     * properties.
     *
     * <p> The request policy and the response policy must not both be null.
     *
     * @param requestPolicy The request policy this module must enforce,
     *		or null.
     *
     * @param responsePolicy The response policy this module must enforce,
     *		or null.
     *
     * @param handler CallbackHandler used to request information.
     *
     * @param options A Map of module-specific configuration properties.
     *
     * @exception AuthException If module initialization fails, including for
     * the case where the options argument contains elements that are not 
     * supported by the module.
     */

    void initialize(MessagePolicy requestPolicy,
	       MessagePolicy responsePolicy,
	       CallbackHandler handler,
	       Map options)
	throws AuthException;

    /**
     * Get the one or more Class objects representing the message types 
     * supported by the module.
     *
     * @return An array of Class objects where each element 
     * defines a message type supported by the module. 
     * A module should return an array containing at
     * least one element. An empty array indicates that the module
     * will attempt to support any message type. This method
     * never returns null. 
     */
    public Class[] getSupportedMessageTypes();

}


