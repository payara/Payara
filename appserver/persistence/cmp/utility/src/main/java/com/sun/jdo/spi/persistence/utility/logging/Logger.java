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

import java.util.ResourceBundle;
import java.security.SecurityPermission;

/** This interface provides an isolation layer between the JDO components
 * that need logging services and the implementation of those services.
 * <P>
 * The JDO Logger interface contains a small number of convenience methods
 * in addition to a subset of the 
 * methods in the java.util.logging.Logger class, and can therefore
 * be implemented in a very straightforward way by a subclass of the 
 * java.util.logging.Logger class.
 * There is one instance of the implementing class for each subsystem
 * in JDO.
 * <P>
 * This interface has no JDK 1.4 dependencies.
 */

public interface Logger {
    
    /** Levels are defined as ints to avoid including the java.util.logging.Level
     * class in this package.
     */
    
    /**
     * OFF is a special level that can be used to turn off logging.
     */
    public static final int OFF = Integer.MAX_VALUE;

    /**
     * SEVERE is a message level indicating a serious failure.
     * <p>
     * In general SEVERE messages should describe events that are
     * of considerable importance and which will prevent normal
     * program execution.   They should be reasonably intelligible
     * to end users and to system administrators.
     */
    public static final int SEVERE = 1000;

    /**
     * WARNING is a message level indicating a potential problem.
     * <p>
     * In general WARNING messages should describe events that will
     * be of interest to end users or system managers, or which
     * indicate potential problems.
     */
    public static final int WARNING = 900;

    /**
     * INFO is a message level for informational messages.
     * <p>
     * Typically INFO messages will be written to the console
     * or its equivalent.  So the INFO level should only be 
     * used for reasonably significant messages that will
     * make sense to end users and system admins.
     */
    public static final int INFO = 800;

    /**
     * CONFIG is a message level for static configuration messages.
     * <p>
     * CONFIG messages are intended to provide a variety of static
     * configuration information, to assist in debugging problems
     * that may be associated with particular configurations.
     * For example, CONFIG message might include the CPU type,
     * the graphics depth, the GUI look-and-feel, etc.
     */
    public static final int CONFIG = 700;

    /**
     * FINE is a message level providing tracing information.
     * <p>
     * All of FINE, FINER, and FINEST are intended for relatively
     * detailed tracing.  The exact meaning of the three levels will
     * vary between subsystems, but in general, FINEST should be used
     * for the most voluminous detailed output, FINER for somewhat
     * less detailed output, and FINE for the  lowest volume (and
     * most important) messages.
     * <p>
     * In general the FINE level should be used for information
     * that will be broadly interesting to developers who do not have
     * a specialized interest in the specific subsystem.
     * <p>
     * FINE messages might include things like minor (recoverable)
     * failures.  Issues indicating potential performance problems
     * are also worth logging as FINE.
     */
    public static final int FINE = 500;

    /**
     * FINER indicates a fairly detailed tracing message.
     * By default logging calls for entering, returning, or throwing
     * an exception are traced at this level.
     */
    public static final int FINER = 400;

    /**
     * FINEST indicates a highly detailed tracing message
     */
    public static final int FINEST = 300;

    /**
     * ALL indicates that all messages should be logged.
     */
    public static final int ALL = Integer.MIN_VALUE;
    
    /** Test if this logger is logging messages.  This is a test for
     * log level FINE, FINER, or FINEST.  If the log message is expensive to construct,
     * this method should be used to determine whether it is a waste of time.
     * We don't expose isLoggable(Level) because Level is not available in
     * JDK 1.3.  Once this is not an issue, we can add isLoggable(Level).
     * @return if FINE, FINER, or FINEST is currently being logged
     */
    public boolean isLoggable();
    
    /** Test if this logger is logging messages at the specific level.
     * If the log message is expensive to construct,
     * this method should be used to determine whether it is a waste of time.
     * We don't expose isLoggable(Level) because Level is not available in
     * JDK 1.3.  Once this is not an issue, we can add isLoggable(Level).
     * @return if this level is currently being logged
     * @param level The level to be tested */
    public boolean isLoggable(int level);
    

    //======================================================================
    // Start of convenience methods for logging.
    //======================================================================

    /**
     * Log a method entry.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY", log level
     * FINER, and the given sourceMethod and sourceClass is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     */
    public void entering(String sourceClass, String sourceMethod);

    /**
     * Log a method entry, with one parameter.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY {0}", log level
     * FINER, and the given sourceMethod, sourceClass, and parameter
     * is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     * @param   param1	       parameter to the method being entered
     */
    public void entering(String sourceClass, String sourceMethod, Object param1);

    /**
     * Log a method entry, with an array of parameters.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY" (followed by a 
     * format {N} indicator for each entry in the parameter array), 
     * log level FINER, and the given sourceMethod, sourceClass, and 
     * parameters is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     * @param   params	       array of parameters to the method being entered
     */
    public void entering(String sourceClass, String sourceMethod, Object params[]);

    /**
     * Log a method return.
     * <p>
     * This is a convenience method that can be used to log returning
     * from a method.  A LogRecord with message "RETURN", log level
     * FINER, and the given sourceMethod and sourceClass is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of the method 
     */
    public void exiting(String sourceClass, String sourceMethod);


    /**
     * Log a method return, with result object.
     * <p>
     * This is a convenience method that can be used to log returning
     * from a method.  A LogRecord with message "RETURN {0}", log level
     * FINER, and the gives sourceMethod, sourceClass, and result
     * object is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of the method 
     * @param   result  Object that is being returned
     */
    public void exiting(String sourceClass, String sourceMethod, Object result);

    /**
     * Log throwing an exception.
     * <p>
     * This is a convenience method to log that a method is
     * terminating by throwing an exception.  The logging is done 
     * using the FINER level.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.  The
     * LogRecord's message is set to "THROW".
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus is it
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod  name of the method.
     * @param   thrown  The Throwable that is being thrown.
     */
    public void throwing(String sourceClass, String sourceMethod, Throwable thrown);

    /**
     * Log a SEVERE message.
     * <p>
     * If the logger is currently enabled for the SEVERE message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     */
    public void severe(String msg);

    /**
     * Log a WARNING message.
     * <p>
     * If the logger is currently enabled for the WARNING message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     */
    public void warning(String msg);

    /**
     * Log an INFO message.
     * <p>
     * If the logger is currently enabled for the INFO message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     */
    public void info(String msg);

    /**
     * Log a CONFIG message.
     * <p>
     * If the logger is currently enabled for the CONFIG message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     */
    public void config(String msg);

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
    public void log(int level, String msg);

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
    public void log(int level, String msg, Object o1);

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
    public void log(int level, String msg, Object[] o);

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
    public void log(int level, String msg, Object o1, Object o2);

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
    public void log(int level, String msg, Object o1, Object o2, Object o3);

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
    public void log(int level, String msg, Throwable thrown );

    /**
     * Log a  message.
     * <p>
     * If the logger is currently enabled for the  message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     */
    public void fine(String msg);

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
    public void fine(String msg, Object o1);

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
    public void fine(String msg, Object[] o);

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
    public void fine(String msg, Object o1, Object o2);

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
    public void fine(String msg, Object o1, Object o2, Object o3);

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     */
    public void finer(String msg);

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
    public void finer(String msg, Object[] o);

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
    public void finer(String msg, Object o1);

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
    public void finer(String msg, Object o1, Object o2);

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
    public void finer(String msg, Object o1, Object o2, Object o3);

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg	The string message (or a key in the message catalog)
     */
    public void finest(String msg);

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
    public void finest(String msg, Object[] o);

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
    public void finest(String msg, Object o1);

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
    public void finest(String msg, Object o1, Object o2);

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
    public void finest(String msg, Object o1, Object o2, Object o3);

    //================================================================
    // End of convenience methods 
    //================================================================

    /**
     * Get the name for this logger.
     * @return logger name.  Will be null for anonymous Loggers.
     */
    public String getName();

}
