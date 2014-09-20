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

/*
 * EJBQLException.java
 *
 * Created on November 12, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbqlc;

/** 
 * This class represents errors reported by the EJBQL compiler.
 * 
 * @author  Michael Bouschen
 */
public class EJBQLException 
    extends RuntimeException
{
    /** The Throwable that caused this EJBQLException. */
    Throwable cause;

    /**
     * Creates a new <code>EJBQLException</code> without detail message.
     */
    public EJBQLException() 
    {
    }

    /**
     * Constructs a new <code>EJBQLException</code> with the specified 
     * detail message.
     * @param msg the detail message.
     */
    public EJBQLException(String msg) 
    {
        super(msg);
    }
    
    /**
      * Constructs a new <code>EJBQLException</code> with the specified 
      * detail message and cause.
      * @param msg the detail message.
      * @param cause the cause <code>Throwable</code>.
      */
    public EJBQLException(String msg, Throwable cause) 
    {
        super(msg);
        this.cause = cause;
    }
    
    /**
     * Returns the cause of this <code>EJBQLException</code> or 
     * <code>null</code> if the cause is nonexistent or unknown.  
     * @return the cause of this or <code>null</code> if the
     * cause is nonexistent or unknown.
     */
    public Throwable getCause()
    {
        return cause;
    }

    /** 
     * The <code>String</code> representation includes the name of the class,
     * the descriptive comment (if any),
     * and the <code>String</code> representation of the cause 
     * <code>Throwable</code> (if any).
     * @return the <code>String</code>.
     */
    public String toString() {
        // calculate approximate size of the String to return
        StringBuffer sb = new StringBuffer();
        sb.append (super.toString());
        // include cause Throwable information
        if (cause != null) {
            sb.append("\n");  //NOI18N
            sb.append("Nested exception"); //NOI18N
            sb.append("\n");  //NOI18N
            sb.append(cause.toString());
        }
        return sb.toString();
    }    
}
