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

package com.sun.jdo.spi.persistence.support.sqlstore.ejb;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.sun.jdo.spi.persistence.utility.logging.LoggerFactoryJDK14;
import com.sun.jdo.spi.persistence.utility.logging.LoggerJDK14;


/**
 *
 * @author  Craig Russell
 * @version 1.0
 */

public class LoggerFactoryiAS extends LoggerFactoryJDK14 {

    /** The top level of the logger domain for application server.
     */
    protected String DOMAIN_ROOT = "javax.enterprise.resource.jdo."; //NOI18N

    /** Creates new LoggerFactory */
    public LoggerFactoryiAS() {
    }

    
    protected String getDomainRoot() {
        return DOMAIN_ROOT;
    }
    
    /** Create a new Logger.  Create a logger for the named component.
     * The bundle name is passed to allow the implementation
     * to properly find and construct the internationalization bundle.
     *
     * This operation is executed as a privileged action to allow
     * permission access for the following operations:
     * ServerLogManager.initializeServerLogger
     *
     * @param absoluteLoggerName the absolute name of this logger
     * @param bundleName the fully qualified name of the resource bundle
     * @return the logger
     */  
    protected LoggerJDK14 createLogger (final String absoluteLoggerName, 
                                        final String bundleName) {
        return (LoggerJDK14) AccessController.doPrivileged ( 
            new PrivilegedAction () {
                public Object run () {
                    LoggerJDK14 result = new LoggerJDK14(absoluteLoggerName, bundleName);
                    //Handlers and Formatters will be set in addLogger().
                    //ServerLogManager.initializeServerLogger(result);
                    
                    return result;
                } 
            } 
        );
    }
    
    /**
     * This method is a no-op in the Sun ONE Application server.
     */
    protected void configureFileHandler(LoggerJDK14 logger) {
    }    

}

