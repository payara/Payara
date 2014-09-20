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

package com.sun.jdo.spi.persistence.utility.logging;

import java.util.logging.Level;
import java.util.logging.LogManager;

import java.lang.StringBuffer;

/**
 * This class is used with JDK 1.4 (and higher) programs to log messages from
 * jdo components.  It extends a java.util.logging.Logger which does
 * the actual logging.  
 *
 * @author  Craig Russell
 * @version 1.0
 */
public class LoggerJDK14 extends java.util.logging.Logger implements Logger {

    /** Class that issued logging call; set by inferCaller. */
    protected String sourceClassName;

    /** Method that issued logging call; set by inferCaller. */
    protected String sourceMethodName;

    /** Creates new LoggerJDK14.  The Thread context class loader or the
     * loader which loaded this class must be able to load the bundle.
     * @param loggerName the full domain name of this logger
     * @param bundleName the bundle name for message translation
     */
    public LoggerJDK14(String loggerName, String bundleName) {
        super (loggerName, bundleName);
    }

    /** Return whether logging is enabled
     * at the FINE level.  This method
     * is not exact because to make it
     * accurately reflect the logging level
     * we would have to include the JDK 1.4
     * java.util.logging.Level class.
     * @return whether logging is enabled at the fine level.
     */
    public boolean isLoggable() {
        return isLoggable(Level.FINE);
    }
    
    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o      Objects to be inserted into the message
     */
    public void fine(String msg, Object[] o) {
        if (isLoggable(Level.FINE)) {
            inferCaller();
            logp(Level.FINE, sourceClassName, sourceMethodName, msg, o);
        }
    }

    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o1      A parameter to be inserted into the message
     */
    public void fine(String msg, Object o1) {
        if (isLoggable(Level.FINE)) {
            inferCaller();
            logp(Level.FINE, sourceClassName, sourceMethodName, msg, o1);
        }
    }
    

    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o1      A parameter to be inserted into the message
     * @param   o2      A parameter to be inserted into the message
     */
    public void fine(String msg, Object o1, Object o2) {
        if (isLoggable(Level.FINE)) {
            inferCaller();
            logp(Level.FINE, sourceClassName, sourceMethodName, msg, new Object[]{o1, o2});
        }
    }
    
    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o1      A parameter to be inserted into the message
     * @param   o2      A parameter to be inserted into the message
     * @param   o3      A parameter to be inserted into the message
     */
    public void fine(String msg, Object o1, Object o2, Object o3) {
        if (isLoggable(Level.FINE)) {
            inferCaller();
            logp(Level.FINE, sourceClassName, sourceMethodName, msg, new Object[]{o1, o2, o3});
        }
    }
    
    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o      Objects to be inserted into the message
     */
    public void finer(String msg, Object[] o) {
        if (isLoggable(Level.FINER)) {
            inferCaller();
            logp(Level.FINER, sourceClassName, sourceMethodName, msg, o);
        }
    }

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o1      A parameter to be inserted into the message
     */
    public void finer(String msg, Object o1) {
        if (isLoggable(Level.FINER)) {
            inferCaller();
            logp(Level.FINER, sourceClassName, sourceMethodName, msg, o1);
        }
    }
    
    
    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o1      A parameter to be inserted into the message
     * @param   o2      A parameter to be inserted into the message
     */
    public void finer(String msg, Object o1, Object o2) {
        if (isLoggable(Level.FINER)) {
            inferCaller();
            logp(Level.FINER, sourceClassName, sourceMethodName, msg, 
                 new Object[]{o1, o2});
        }
    }
    
    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o1      A parameter to be inserted into the message
     * @param   o2      A parameter to be inserted into the message
     * @param   o3      A parameter to be inserted into the message
     */
    public void finer(String msg, Object o1, Object o2, Object o3) {
        if (isLoggable(Level.FINER)) {
            inferCaller();
            logp(Level.FINER, sourceClassName, sourceMethodName, msg, 
                 new Object[]{o1, o2, o3});
        }
    }
    

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o      Objects to be inserted into the message
     */
    public void finest(String msg, Object[] o) {
        if (isLoggable(Level.FINEST)) {
            inferCaller();
            logp(Level.FINEST, sourceClassName, sourceMethodName, msg, o);
        }
    }

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o1      A parameter to be inserted into the message
     */
    public void finest(String msg, Object o1) {
        if (isLoggable(Level.FINEST)) {
            inferCaller();
            logp(Level.FINEST, sourceClassName, sourceMethodName, msg, o1);
        }
    }
    
    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o1      A parameter to be inserted into the message
     * @param   o2      A parameter to be inserted into the message
     */
    public void finest(String msg, Object o1, Object o2) {
        if (isLoggable(Level.FINEST)) {
            inferCaller();
            logp(Level.FINEST, sourceClassName, sourceMethodName, msg, 
                 new Object[]{o1, o2});
        }
    }
    
    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o1      A parameter to be inserted into the message
     * @param   o2      A parameter to be inserted into the message
     * @param   o3      A parameter to be inserted into the message
     */
    public void finest(String msg, Object o1, Object o2, Object o3) {
        if (isLoggable(Level.FINEST)) {
            inferCaller();
            logp(Level.FINEST, sourceClassName, sourceMethodName, msg, 
                 new Object[]{o1, o2, o3});
        }
    }
    
    /** Prepare a printable version of this instance.
     * @return the String representation of this object
     */    
    public String toString() {
        StringBuffer buf = new StringBuffer ("LoggerJDK14: ");  //NOI18N
        buf.append (" name: "); buf.append (getName()); //NOI18N
        buf.append (", super: "); buf.append (super.toString());  //NOI18N
        buf.append (", logging level: "); buf.append (getLevel()); //NOI18N
        return buf.toString();
    }
    
    /**
     * Log a message.
     * <p>
     * If the logger is currently enabled for the message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   level   The level for this message
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o1      A parameter to be inserted into the message
     */
    public void log(int level, String msg, Object o1) {
        Level lvl = convertLevel(level);
        if (isLoggable(lvl)) {
            inferCaller();
            logp(lvl, sourceClassName, sourceMethodName, msg, o1);
        }
    }
    
    /**
     * Log a message.
     * <p>
     * If the logger is currently enabled for the  message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   level   The level for this message
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o1      A parameter to be inserted into the message
     * @param   o2      A parameter to be inserted into the message
     */
    public void log(int level, String msg, Object o1, Object o2) {
        Level lvl = convertLevel(level);
        if (isLoggable(lvl)) {
            inferCaller();
            logp(lvl, sourceClassName, sourceMethodName, msg, new Object[]{o1, o2});
        }
    }
    
    /**
     * Log a  message.
     * <p>
     * If the logger is currently enabled for the  message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   level   The level for this message
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o1      A parameter to be inserted into the message
     * @param   o2      A parameter to be inserted into the message
     * @param   o3      A parameter to be inserted into the message
     */
    public void log(int level, String msg, Object o1, Object o2, Object o3) {
        Level lvl = convertLevel(level);
        if (isLoggable(lvl)) {
            inferCaller();
            logp(lvl, sourceClassName, sourceMethodName, msg, 
                 new Object[] {o1, o2, o3});
        }
    }
    
    /**
     * Log a  message.
     * <p>
     * If the logger is currently enabled for the  message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   level   The level for this message
     * @param   msg	The string message (or a key in the message catalog)
     * @param   o      Objects to be inserted into the message
     */
    public void log(int level, String msg, Object[] o) {
        Level lvl = convertLevel(level);
        if (isLoggable(lvl)) {
            inferCaller();
            logp(lvl, sourceClassName, sourceMethodName, msg, o);
        }
    }
    
    /**
     * Log a message.
     * <p>
     * If the logger is currently enabled for the message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   level   The level for this message
     * @param   msg	The string message (or a key in the message catalog)
     */
    public void log(int level, String msg) {
        Level lvl = convertLevel(level);
        if (isLoggable(lvl)) {
            inferCaller();
            logp(lvl, sourceClassName, sourceMethodName, msg);
        }
    }
    

    /**
     * Log a message.
     * <p>
     * If the logger is currently enabled for the message 
     * level then the given message, and the exception dump, 
	 * is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   level   The level for this message
     * @param   msg	The string message (or a key in the message catalog)
     * @param   thrown	The exception to log
     */
    public void log(int level, String msg, Throwable thrown ){
        Level lvl = convertLevel(level);
        if (isLoggable(lvl)) {
            inferCaller();
            logp(lvl, sourceClassName, sourceMethodName, msg, thrown);
        }
    }

    /**
     * Check if a message of the given level would actually be logged
     * by this logger.  This check is based on the Loggers effective level,
     * which may be inherited from its parent.
     *
     * @return true if the given message level is currently being logged.
     * @param levelValue  the level to check
     */
    public boolean isLoggable(int levelValue) {
        return isLoggable(convertLevel(levelValue));
    }
    
    /** Convert an int level used by jdo logger to the Level instance used
     * by JDK 1.4 logger.
     * This is done to allow components to use logging outside the JDK 1.4
     * environment.
     * @param level the level to convert
     * @return  the Level instance corresponding to the int level
     */
    protected Level convertLevel(int level) {
        switch (level) {
            case 300: return Level.FINEST;
            case 400: return Level.FINER;
            case 500: return Level.FINE;
            case 700: return Level.CONFIG;
            case 800: return Level.INFO;
            case 900: return Level.WARNING;
            case 1000: return Level.SEVERE;
            default: return Level.CONFIG;
        }
    }

    /** 
     * Method to infer the caller's class name and method name. 
     * The method analyses the current stack trace, to find the method that
     * issued the logger call. It stores the callers class and method name
     * into fields sourceClassName and sourceMethodName.
     */
    protected void inferCaller() {
        // Get the stack trace.
        StackTraceElement[] stack = (new Throwable()).getStackTrace();
        // Search for the first frame before the "Logger" class.
        for(int ix = 0; ix < stack.length; ix++) {
            StackTraceElement frame = stack[ix];
            String cname = frame.getClassName();
            if (!isLoggerClass(cname)) {
                // We've found the relevant frame.
                sourceClassName = cname;
                sourceMethodName = frame.getMethodName();
                return;
            }
        }
    }

    /**
     * This method is a helper method for {@link #inferCaller}. It returns 
     * <code>true</code> if the specified class name denotes a logger class
     * that should be ignored when analysing the stack trace to infer the 
     * caller of a log message.
     * @param className the class name to be checked.
     * @return <code>true</code> if the specified name denotes a logger class;
     * <code>false</code> otherwise.
     */
    protected boolean isLoggerClass(String className)
    {
        return "com.sun.jdo.spi.persistence.utility.logging.LoggerJDK14". //NOI18N
            equals(className);
    }
}
