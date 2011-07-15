/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.jmx.remote;

import com.sun.enterprise.admin.jmx.remote.IStringManager;

import java.lang.reflect.Method;
//import java.lang.reflect.Constructor;
import java.util.logging.Logger;

/**
 * This class acts as an adapter to use the appserver internal StringManager. 
 * It implements the IStringManager interface that is defined for an 
 * jmx-connector string-manager. At the same time it composes a delegatee
 * com.sun.enterprise.util.i18n.StringManager object.
 * The method invocation on this delegatee is through reflection to avoid any 
 * compile time dependencies
 */
public class StringManager implements IStringManager {
    
    private static final Logger logger = Logger.getLogger(
        DefaultConfiguration.JMXCONNECTOR_LOGGER);/*, 
        DefaultConfiguration.LOGGER_RESOURCE_BUNDLE_NAME );*/
    
    private static Class asStringManagerClass = null;
    private static Method getStr = null;
    private static Method getStrDef = null;
    private static Method getStrGeneric = null;
    private static Method getManager = null;
    
    //this is the composed AS9.0 internal default StringManager
    private Object asStringManager = null;
    
    public StringManager(String packageName) {
        try {
            if (asStringManagerClass == null) {
                asStringManagerClass = Class.forName("com.sun.enterprise.util.i18n.StringManager");
                getStr = asStringManagerClass.getMethod("getString", new Class[] { String.class});
                getStrDef = asStringManagerClass.getMethod("getString", new Class[] { String.class, Object.class });
                getStrGeneric = asStringManagerClass.getMethod("getString", new Class[] { String.class, Object[].class });
                getManager = asStringManagerClass.getMethod("getManager", new Class[] { String.class });
            }
            
            asStringManager = getManager.invoke(asStringManagerClass, new Object[] { packageName });
        } catch (Throwable e) {
            e.printStackTrace();
            StackTraceElement[] ste = e.getStackTrace();
            for (int i =0; i<ste.length; i++) logger.severe(ste[i].toString());
            logger.severe("StringManager could not be configured");
        }
    }

    public String getString(String key) {
        try {
            return (String) getStr.invoke(asStringManager, new Object[] { key });
        } catch(Exception ex) {
            logger.severe("Method invocation failed on com.sun.enterprise.util.i18n.StringManager");
            return null;
        }
    }
    
    public String getString(String key, Object arg) {
        try {
            return (String) getStrDef.invoke(asStringManager, new Object[] { key, arg });
        } catch(Exception ex) {
            logger.severe("Method invocation failed on com.sun.enterprise.util.i18n.StringManager");
            return null;
        }
    }
    
    public String getString(String key, Object[] args) {
        try {
            return (String) getStrGeneric.invoke(asStringManager, new Object[] { key, args });
        } catch(Exception ex) {
            logger.severe("Method invocation failed on com.sun.enterprise.util.i18n.StringManager");
            return null;
        }
    }    
}
