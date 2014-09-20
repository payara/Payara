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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Formatter;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.glassfish.persistence.common.I18NHelper;

/**
 *
 * @author  Craig Russell
 * @version 1.0
 */

public class LoggerFactoryJDK14 extends AbstractLoggerFactory {

    /** I18N message handler for this class */
    private static final ResourceBundle _messages =
      I18NHelper.loadBundle(LoggerFactoryJDK14.class);

    /** Get the message bundle for the AbstractLogger itself.
     */
    protected static ResourceBundle getMessages () { return _messages; }

    /** Creates new LoggerFactory */
    public LoggerFactoryJDK14() {
    }

    protected LoggerJDK14 findLogger(String absoluteLoggerName) {
        return (LoggerJDK14)
        LogManager.getLogManager().getLogger(absoluteLoggerName);
    }

    /** Create a new Logger.  create a logger for the named component.  
     * The bundle name and class loader are passed to allow the implementation 
     * to properly find and construct the internationalization bundle.
     * This operation is executed as a privileged action to allow
     * permission access for the following operations:
     *
     * LogManager.getLogManager().addLogger - this might do checkAccess.
     * new FileHandler
     * FileHandler.setLevel
     * FileHandler.setFormatter
     * Logger.addHandler
     *
     * @param absoluteLoggerName the absolute name of this logger
     * @param bundleName the fully qualified name of the resource bundle
     * @param loader the class loader used to load the resource bundle, or null
     * @return the logger
     */
    protected Logger createLogger (final String absoluteLoggerName,
        final String bundleName, final ClassLoader loader) {
        return (Logger) AccessController.doPrivileged ( 
            new PrivilegedAction () { 
                public Object run () { 
                    LoggerJDK14 logger = null;
                    ClassLoader pushed = Thread.currentThread().getContextClassLoader();
                    if(loader!=null) {
                        setContextClassLoader (loader);
                    }
                    try {
                        logger = createLogger(absoluteLoggerName, bundleName);
                        LogManager.getLogManager().addLogger(logger);
                        configureFileHandler(logger);

                        return logger;
                    } catch (Exception ex) {
                          MessageFormat messageFormat = new MessageFormat(
                              getMessages().getString("errorlogger.create.exception"));

                          getErrorLogger().log(Logger.SEVERE, messageFormat.format(
                              new String[]{absoluteLoggerName}), ex);
                    } finally {
                        setContextClassLoader (pushed);
                    }

                    return logger;
                } 
            } 
        );
    }

    /**
     * This method throws  SecurityException  if a security manager exists and if
     * the caller does not have <tt>LoggingPermission("control"))</tt> or the
     * calling code is not placed in the doPrivileged() block.
     */
    protected void setContextClassLoader (final ClassLoader loader) {
        if (loader != null) {
            Thread.currentThread().setContextClassLoader (loader);
        }
    } //setContextClassLoader
 

    protected LoggerJDK14 createLogger (String absoluteLoggerName, String
        bundleName) {
            LoggerJDK14 result = new LoggerJDK14(absoluteLoggerName, bundleName);
            return result;
    }
    
    /**
     * This method throws  SecurityException  if a security manager exists and if
     * the caller does not have <tt>LoggingPermission("control"))</tt> or the
     * calling code is not placed in the doPrivileged() block.
     */
    protected void configureFileHandler(LoggerJDK14 logger) {
        String name = logger.getName();
        String baseName = name + ".FileHandler"; //NOI18N
        LogManager logManager = LogManager.getLogManager();
        
        String pattern = logManager.getProperty(baseName + ".pattern"); //NOI18N
        if(pattern != null) {
            //If pattern != null, create and attach a FileHandler to logger. 
            //Look various properties . If not found, fall back to 
            //defaults
            
            int defaultLimit = 0;
            String limit = logManager.getProperty(baseName + ".limit"); //NOI18N
            if(limit != null) {
                try {
                    defaultLimit = Integer.parseInt(limit);
                    if(defaultLimit < 0)
                        defaultLimit = 0;
                }
                catch (NumberFormatException e) {
                }
            }

            int defaultCount = 1;
            String count = logManager.getProperty(baseName + ".count"); //NOI18N
            if(count != null) {
                try {
                    defaultCount = Integer.parseInt(count);
                    if(defaultCount < 1)
                        defaultCount = 1;
                }
                catch (NumberFormatException e) {
                }
            }
            
            boolean defaultAppend = false;
            String append = logManager.getProperty(baseName + ".append"); //NOI18N
            if(append != null) {
                defaultAppend = Boolean.valueOf(append).booleanValue();
            }
            
            FileHandler fileHandler = null;
            try {
                fileHandler = new FileHandler(pattern, defaultLimit,
                            defaultCount, defaultAppend);
            }
            catch(Exception e) {
                MessageFormat messageFormat = new MessageFormat( getMessages().getString(
                    "errorlogger.filehandler.initialize.exception")); //NOI18N

                getErrorLogger().log(Logger.WARNING, 
                    messageFormat.format(new String[]{name}), e);
            }

            if(fileHandler != null) {
                //Initialize various attributes for the new fileHandler
                //--Level
                String level = logManager.getProperty(baseName + ".level"); //NOI18N
                if (level != null) {
                    try {
                        fileHandler.setLevel(Level.parse(level) );
                    }
                    catch(IllegalArgumentException e) {
                    }
                }

                //--Formatter
                Formatter defaultFormatter = null;
                //Initialize various attributes for the new fileHandler
                String formatter = logManager.getProperty(baseName + ".formatter"); //NOI18N
                if(formatter != null) {
                    try {
                        Class clz = ClassLoader.getSystemClassLoader().loadClass(formatter);
                            defaultFormatter = (Formatter) clz.newInstance();
                    } catch (Exception e) {
                      // We got one of a variety of exceptions in creating the
                      // class or creating an instance.
                      // Drop through.
                      MessageFormat messageFormat = new MessageFormat(
                         getMessages().getString("errorlogger.formatter.initialize.exception"));

                      getErrorLogger().log(Logger.WARNING, messageFormat.format(new String[]{name}), e);
                    }
                
                }
                
                if (defaultFormatter == null) {
                    defaultFormatter = new SimpleFormatter();
                }
                
                try {
                   fileHandler.setFormatter(defaultFormatter);
                }
                catch(IllegalArgumentException e) {
                }

                logger.addHandler(fileHandler);

            }   //if(fileHandler != null) 
            
        }   //if(pattern != null) 

    }
}
