/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.servermgmt;

/**
 * Exception for when there are problems accessing the domain or instance folder, files or config
 * @author  kebbs
 * @since Created on August 22, 2003, 11:21 AM
 */
public class RepositoryException extends java.lang.Exception {
    
       /** Constructs a new InstanceException object.
     * @param message
     */
    public RepositoryException(String message)
    {
        super(message);
    }

    /** Constructs a new InstanceException object.
     * @param cause
     */
    public RepositoryException(Throwable cause)
    {
        //When created without a message, we take the message of our cause
        this(cause.getLocalizedMessage(), cause);
    }

    /** Constructs a new InstanceException object.
     * @param message
     * @param cause
     */
    public RepositoryException(String message, Throwable cause)
    {
        super(message, cause);
    }

    private static final String PREFIX = "( ";
    private static final String POSTFIX = " )";
   
    private String format(String msg, String causeMsg, Throwable cause)
    {
        if (cause != null) {
            if (msg == null) {
                if (causeMsg != null) {
                    msg = causeMsg;
                } else {
                    msg = cause.toString();
                }
            } else if (causeMsg != null && !causeMsg.equals(msg)) {
                msg += PREFIX + causeMsg + POSTFIX;               
            } else {
                msg += PREFIX + cause.toString() + POSTFIX;
            }
        }
        return msg;
    }

    /** If there is a cause, appends the getCause().getMessage()
     *  to the original message.
     * @return the message
     */    
    @Override
    public String getMessage()
    {
        String msg = super.getMessage();
        Throwable cause = super.getCause();
        if (cause != null) {
            msg = format(msg, cause.getMessage(), cause);
        }
        return msg;
    } 
    
    /** If there is a cause, appends the getCause().getMessage()
     *  to the original message.
     * @return the message, localised
     */
    @Override
    public String getLocalizedMessage()
    {
        String msg = super.getLocalizedMessage();
        Throwable cause = super.getCause();
        if (cause != null) {
            msg = format(msg, cause.getLocalizedMessage(), cause);
        }
        return msg;
    }       
}
