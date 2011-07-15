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
 * This checked exception is thrown by implementations of the 
 * <code>javax.security.jacc.PolicyConfiguration</code> Interface, the
 * <code>javax.security.jacc.PolicyConfigurationFactory</code> abstract class,
 * the <code>javax.security.jacc.PolicyContext</code> utility class, and 
 * implementations of the 
 * <code>javax.security.jacc.PolicyContextException</code> Interface.
 * <P>
 * This exception is used by javax.security.jacc implementation 
 * classes to rethrow checked exceptions ocurring within an
 * implementation that are not declared by the interface or class 
 * being implemented.
 *
 * @see java.lang.Exception
 * @see javax.security.jacc.PolicyConfiguration
 * @see javax.security.jacc.PolicyConfigurationFactory
 * @see javax.security.jacc.PolicyContext
 * @see javax.security.jacc.PolicyContextHandler
 *
 * @author Ron Monzillo
 * @author Gary Ellison
 */

public class PolicyContextException extends java.lang.Exception {

   /** 
    * Constructs a new PolicyContextException with 
    * <code>null</code> as its detail message.
    * describing the cause of the exception.
    */
    public PolicyContextException()
    {
        super();
    }

   /** 
    * Constructs a new PolicyContextException with the specified detail message
    * @param msg - a <code>String</code> containing a detail message 
    * describing the cause of the exception.
    */

    public PolicyContextException(String msg)
    {
        super(msg);
    }

   /** 
    * Constructs a new PolicyContextException with the specified detail message
    * and cause. The cause will be encapsulated in the constructed exception.
    * @param msg - a <code>String containing a detail message describing the 
    * cause of the exception.
    * @param cause - the Throwable that is "causing" this exception to be 
    * constructed. A null value is permitted, and the value passed through
    * this parameter may subsequently be retrieved by calling 
    * <code>getCause()</code> on the constructed exception.
    */

    public PolicyContextException(String msg, Throwable cause)
    {
        super(msg,cause);
    }

   /** 
    * Constructs a new PolicyContextException with the specified cause.
    * The cause will be encapsulated in the constructed exception.
    *
    * @param cause - the Throwable that is "causing" this exception to be 
    * constructed. A null value is permitted, and the value passed through
    * this parameter may subsequently be retrieved by calling 
    * <code>getCause()</code> on the constructed exception.
    */

    public PolicyContextException(Throwable cause)
    {
        super(cause);
    }
}





