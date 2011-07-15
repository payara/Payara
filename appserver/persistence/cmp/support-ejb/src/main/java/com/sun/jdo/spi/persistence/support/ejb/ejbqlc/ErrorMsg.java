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
 * ErrorMsg.java
 *
 * Created on November 12, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbqlc;

import java.util.ResourceBundle;
import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;

/** 
 * This is a helper class to report error messages from the EJBQL compiler.
 * @author  Michael Bouschen
 * @author  Shing Wai Chan
 */
public class ErrorMsg
{
    /** I18N support. */
    private final static ResourceBundle msgs = I18NHelper.loadBundle(
        ErrorMsg.class);
    
    /** The logger */
    private static Logger logger = LogHelperQueryCompilerEJB.getLogger();
    
    /**
     * This method throws an EJBQLException indicating an user error.
     * @param line line number
     * @param col column number
     * @param text error message
     * @exception EJBQLException describes the user error.
     */
    public static void error(int line, int col, String text)
        throws EJBQLException
    {
        EJBQLException ex = null;
        if (line > 1) {
            // include line and column info
            Object args[] = {new Integer(line), new Integer(col), text};
            ex = new EJBQLException(I18NHelper.getMessage(
                msgs, "EXC_PositionInfoMsgLineColumn", args)); //NOI18N
        }
        else if (col > 0) {
            // include column info
            Object args[] = {new Integer(col), text};
            ex = new EJBQLException(I18NHelper.getMessage(
                msgs, "EXC_PositionInfoMsgColumn", args)); //NOI18N
        }
        else {
            ex = new EJBQLException(I18NHelper.getMessage(
                msgs, "EXC_PositionInfoMsg", text)); //NOI18N
        }
        throw ex;
    }
    
    /**
     * This method throws an EJBQLException indicating an user error.
     * @param text error message
     * @param cause the cause of the error
     * @exception EJBQLException describes the user error.
     */
    public static void error(String text, Throwable cause)
        throws EJBQLException
    {
        throw new EJBQLException(text, cause);
    }
    
    /**
     * This method throws an EJBQLException indicating an user error.
     * @param text error message
     * @exception EJBQLException describes the user error.
     */
    public static void error(String text)
        throws EJBQLException
    {
        throw new EJBQLException(text);
    }
    
    /**
     * This method throws an UnsupportedOperationException indicating an 
     * unsupported feature.
     * @param line line number
     * @param col column number
     * @param text message
     * @exception UnsupportedOperationException describes the unsupported 
     * feature.
     */
    public static void unsupported(int line, int col, String text)
        throws UnsupportedOperationException
    {
        UnsupportedOperationException ex;
        if (line > 1)
        {
            // include line and column info
            Object args[] = {new Integer(line), new Integer(col), text};
            ex = new UnsupportedOperationException(I18NHelper.getMessage(
                msgs, "EXC_PositionInfoMsgLineColumn", args)); //NOI18N
        }
        else if (col > 0) {
            // include column info
            Object args[] = {new Integer(col), text};
            ex = new UnsupportedOperationException(I18NHelper.getMessage(
                msgs, "EXC_PositionInfoMsgColumn", args)); //NOI18N
        }
        else {
            Object args[] = {text};
            ex = new UnsupportedOperationException(I18NHelper.getMessage(
                msgs, "EXC_PositionInfoMsg", args)); //NOI18N
        }   
        throw ex;
    }
    
    /**
     * This method is called in the case of an fatal internal error.
     * @param text error message
     * @exception EJBQLException describes the fatal internal error.
     */
    public static void fatal(String text)
        throws EJBQLException
    {
        throw new EJBQLException(I18NHelper.getMessage(
            msgs, "ERR_FatalInternalError", text)); //NOI18N
    }

    /**
     * This method is called in the case of an fatal internal error.
     * @param text error message
     * @param nested the cause of the error
     * @exception EJBQLException describes the fatal internal error.
     */
    public static void fatal(String text, Throwable nested)
        throws EJBQLException
    {
        throw new EJBQLException(I18NHelper.getMessage(
            msgs, "ERR_FatalInternalError", text), nested); //NOI18N
    }

    /**
     * This method is called when we want to log an exception in a given level.
     * Note that all other methods in this class do not log a stack trace.
     * @param level log level
     * @param text error message
     * @param nested the cause of the error
     * @exception EJBQLException describes the fatal internal error.
     */
    public static void log(int level, String text, Throwable nested)
        throws EJBQLException
    {
        logger.log(level, text, nested);
        throw new EJBQLException(text, nested);
    }
}
