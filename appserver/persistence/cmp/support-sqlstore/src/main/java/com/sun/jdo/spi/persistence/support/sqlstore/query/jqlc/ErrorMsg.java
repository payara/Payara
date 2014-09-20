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
 * Created on April 3, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc;

import java.util.ResourceBundle;

import com.sun.jdo.api.persistence.support.JDOQueryException;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.api.persistence.support.JDOUnsupportedOptionException;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

/** 
 *
 * @author  Michael Bouschen
 * @version 0.1
 */
public class ErrorMsg
{
    /**
     *
     */
    protected String context = null;
    
    /**
     * I18N support
     */
	protected final static ResourceBundle messages = 
      I18NHelper.loadBundle(ErrorMsg.class);

    /** The logger */
    private static Logger logger = LogHelperQueryCompilerJDO.getLogger();
    
    /**
     *
     */
    public String getContext()
    {
        return context;
    }
    
    /**
     *
     */
    public void setContext(String name)
    {
        context = name;
    }

    /**
	 * Indicates an error situation. 
     * @param line line number
     * @param col column number
     * @param msg error message
	 */
    public void error(int line, int col, String msg)
        throws JDOQueryException
	{
        JDOQueryException ex;
        if (line > 1)
        {
            // include line and column info
            Object args[] = {context, new Integer(line), new Integer(col), msg};
            ex = new JDOQueryException(I18NHelper.getMessage(
                messages, "jqlc.errormsg.generic.msglinecolumn", args)); //NOI18N
        }
        else if (col > 0)
        {
            // include column info
            Object args[] = {context, new Integer(col), msg};
            ex = new JDOQueryException(I18NHelper.getMessage(
                messages, "jqlc.errormsg.generic.msgcolumn", args)); //NOI18N
        }
        else 
        {
            Object args[] = {context, msg};
            ex = new JDOQueryException(I18NHelper.getMessage(
                messages, "jqlc.errormsg.generic.msg", args)); //NOI18N
        }
        logger.throwing("jqlc.ErrorMsg", "error", ex);
        throw ex;
	}
    
    /**
	 * Indicates that a feature is not supported by the current release. 
     * @param line line number
     * @param col column number
     * @param msg message
	 */
    public void unsupported(int line, int col, String msg)
        throws JDOUnsupportedOptionException
	{
        JDOUnsupportedOptionException ex;
        if (line > 1)
        {
            // include line and column info
            Object args[] = {context, new Integer(line), new Integer(col), msg};
            ex = new JDOUnsupportedOptionException(I18NHelper.getMessage(
                messages, "jqlc.errormsg.generic.msglinecolumn", args)); //NOI18N
        }
        else if (col > 0)
        {
            // include column info
            Object args[] = {context, new Integer(col), msg};
            ex = new JDOUnsupportedOptionException(I18NHelper.getMessage(
                messages, "jqlc.errormsg.generic.msgcolumn", args)); //NOI18N
                                                                         
        }
        else 
        {
            Object args[] = {context, msg};
            ex = new JDOUnsupportedOptionException(I18NHelper.getMessage(
                messages, "jqlc.errormsg.generic.msg", args)); //NOI18N
        }	
        logger.throwing("jqlc.ErrorMsg", "unsupported", ex);
        throw ex;
    }
    
    /**
	 * Indicates a fatal situation (implementation error).
     * @param msg error message
	 */
	public void fatal(String msg)
        throws JDOFatalInternalException
	{
        JDOFatalInternalException ex = new JDOFatalInternalException(msg);
        logger.throwing("jqlc.ErrorMsg", "fatal", ex);
        throw ex;
	}

    /**
	 * Indicates a fatal situation (implementation error).
     * @param msg error message
	 */
	public void fatal(String msg, Exception nested)
        throws JDOFatalInternalException
	{
        JDOFatalInternalException ex = new JDOFatalInternalException(msg, nested);
        logger.throwing("jqlc.ErrorMsg", "fatal", ex);
        throw ex;
	}
}


