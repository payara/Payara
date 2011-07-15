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

package javax.security.auth.message.callback;

import java.security.Principal;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;

/**
 * Callback for setting the container's caller (or Remote user) principal.
 * This callback is intended to be called by a <code>serverAuthModule</code> 
 * during its <code>validateRequest</code> processing. 
 *
 * @version %I%, %G%
 */
public class CallerPrincipalCallback implements Callback {

    private Subject subject;
    private Principal principal;
    private String name;

    /**
     * Create a CallerPrincipalCallback to set the container's 
     * representation of the caller principal
     *
     * @param s The Subject in which the container will distinguish the
     * caller identity.
     *
     * @param p The Principal that will be distinguished as the caller
     * principal. This value may be null.
     * <p> 
     * The CallbackHandler must use the argument Principal to establish the caller
     * principal associated with the invocation being processed by the container.
     * When the argument Principal is null, the handler must establish the 
     * container's representation of the unauthenticated caller principal. The 
     * handler may perform principal mapping of non-null argument Principal 
     * values, but it must be possible to configure the handler such that it 
     * establishes the non-null argument Principal as the caller principal.
     */
    
    
    public CallerPrincipalCallback(Subject s, Principal p) { 
	subject = s;
	principal = p;
        name = null;
    }

    /**
     * Create a CallerPrincipalCallback to set the container's 
     * representation of the caller principal.
     *
     * @param s The Subject in which the container will distinguish the
     * caller identity.
     *
     * @param n The String value that will be returned when getName() is
     * called on the principal established as the caller principal or null.
     * <p> 
     *  The CallbackHandler must use the n argument to establish the caller 
     * principal associated with the invocation being processed by the container.
     * When the n argument is null, the handler must establish the container's
     * representation of the unauthenticated caller principal (which may or may 
     * not be equal to null, depending on the requirements of the container type
     * ). The handler may perform principal mapping of non-null values of n, but
     * it must be possible to configure the handler such that it establishes the
     * non-null argument value as the value returned when getName is called on 
     * the established principal.
     */
    public CallerPrincipalCallback(Subject s, String n) { 
	subject = s;
	principal = null;
        name = n;
    }

    /**
     * Get the Subject in which the handler will distinguish the caller 
     * principal
     *
     * @return The subject.
     */
    public Subject getSubject() {
	return subject;
    }

    /**
     * Get the caller principal.
     *
     * @return The principal or null.
     * <p> 
     * When the values returned by this method and the getName methods 
     * are null, the handler must establish the container's representation 
     * of the unauthenticated caller principal within the Subject.
     */
    public Principal getPrincipal() {
	return principal;
    }

    /**
     * Get the caller principal name.
     *
     * @return The principal name or null.
     * <p> 
     * When the values returned by this method and the getPrincipal methods 
     * are null, the handler must establish the container's representation 
     * of the unauthenticated caller principal within the Subject.
     */
    public String getName() {
	return name;
    }
}
