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

package javax.security.jacc;

/**
 * This interface defines the methods that must be implemented by handlers 
 * that are to be registered and activated by the <code>PolicyContext</code>
 * class. The <code>PolicyContext</code> class provides methods for containers
 * to register and activate container-specific <code>PolicyContext</code>
 * handlers. <code>Policy</code> providers use the <code>PolicyContext</code>
 * class to activate handlers to obtain (from the container) additional policy
 * relevant context to apply in their access decisions. All handlers
 * registered and activated via the <code>PolicyContext</code> class must
 * implement the <code>PolicyContextHandler</code> interface.
 *
 * @see javax.security.jacc.PolicyContext
 * @see javax.security.jacc.PolicyContextException
 *
 * @author Ron Monzillo
 * @author Gary Ellison
 */

public interface PolicyContextHandler {
	
    /** This public method returns a boolean result indicating whether or
     * not the handler supports the context object identified by the
     * (case-sensitive) key value.
     * @param key a <code>String</code> value identifying a context object
     * that could be supported by the handler. The value of this parameter
     * must not be null.
     * @return a boolean indicating whether or not the context object
     * corresponding to the argument key is handled by the handler.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the method signature. The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException
     */
    
    public boolean supports(String key)
	throws javax.security.jacc.PolicyContextException;

    /** This public method returns the keys identifying the context objects
     * supported by the handler. The value of each key supported by a handler
     * must be a non-null <code>String</code> value.
     * @return an array containing <code>String</code> values
     * identifing the context objects supported by the handler. The array
     * must not contain duplicate key values. In the
     * unlikely case that the Handler supports no keys, the handler must
     * return a zero length array. The value null must never be returned by
     * this method.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the method signature. The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException
     */

    public String[] getKeys()
	throws javax.security.jacc.PolicyContextException;

    /** This public method is used by the <code>PolicyContext</code> class to
     * activate the handler and obtain from it the context object
     * identified by the (case-sensitive) key. In addition to the key,
     * the handler will be activated with the handler data value associated
     * within the <code>PolicyContext</code> class
     * with the thread on which the call to this method is made.
     * <P>
     * Note that the policy context identifier associated with a thread 
     * is available to the handler by calling PolicyContext.getContextID(). 
     * <P>
     * @param key a String that identifies the context object to be returned
     * by the handler. The value of this paramter must not be null.
     * @param data the handler data <code>Object</code> associated with the
     * thread on which the call to this method has been made. Note that
     * the value passed through this parameter may be <code>null</code>.
     * @return The container and handler specific <code>Object</code>
     * containing the desired context. A <code>null</code> value may 
     * be returned if the value of the corresponding context is null.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the method signature. The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException
     */

    public Object getContext(String key, Object data) 
	throws javax.security.jacc.PolicyContextException;

}

