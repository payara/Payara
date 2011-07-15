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
import com.sun.enterprise.admin.jmx.remote.StringManager;
import com.sun.enterprise.admin.jmx.remote.DefaultConfiguration;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;
import java.util.Hashtable;
import java.util.Map;

public class StringManagerFactory {

    /** cache for all the local string managers (per pkg) */
    private static Hashtable managers = new Hashtable();
    
    private static final Logger logger = Logger.getLogger(
        DefaultConfiguration.JMXCONNECTOR_LOGGER);/*, 
        DefaultConfiguration.LOGGER_RESOURCE_BUNDLE_NAME );
    
    /**
     * packageName stands for the input fully qualified class name against which 
     * a string manager is stored
     */
    public static IStringManager getServerStringManager(String packageName) {
        String stringMgrClassName = System.getProperty(DefaultConfiguration.STRING_MANAGER_CLASS_NAME);
        return getManager(packageName, stringMgrClassName);
    }

    public static IStringManager getClientStringManager(String packageName, Map env) {
        String stringMgrClassName = null;
        if (env != null) 
            stringMgrClassName = (String)env.get(DefaultConfiguration.STRING_MANAGER_CLASS_NAME);
        return getManager(packageName, stringMgrClassName);
    }

    /**
     * inputClass is the Class against whose package a string manager is stored
     */
    public static IStringManager getServerStringManager(Class inputClass) {
        String packageName = inputClass.getPackage().getName();
        String stringMgrClassName = System.getProperty(DefaultConfiguration.STRING_MANAGER_CLASS_NAME);
        return getManager(packageName, stringMgrClassName);
    }

    public static IStringManager getClientStringManager(Class inputClass, Map env) {
        String packageName = inputClass.getPackage().getName();
        String stringMgrClassName = null;
        if (env != null) 
            stringMgrClassName = (String)env.get(DefaultConfiguration.STRING_MANAGER_CLASS_NAME);
        return getManager(packageName, stringMgrClassName);
    }

    public static IStringManager getManager(String packageName, String stringMgrClassName) {
        
        IStringManager mgr = (IStringManager) managers.get(packageName);
        
        if (mgr != null) return mgr;
        
        if (stringMgrClassName == null) mgr = new StringManager(packageName);
        else {
            try {
                Class customClass = Class.forName(stringMgrClassName);
                Constructor constructor = 
                    customClass.getConstructor(new Class[] { String.class });
                mgr = (IStringManager) constructor.newInstance(new Object[] {packageName});
            } catch (Exception e) {
                logger.severe("StringManager could not be configured");
            }
        }
        
        if (mgr != null) managers.put(packageName, mgr);
        else logger.severe("Custom StringManager Class could not be instantiated");
        
        return mgr;
    }
}
