/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.*;
//END OF IASRI 4660742


/**
 * Implementation of a local string manager.
 * Provides access to i18n messages for classes that need them.
 */

public class LocalStringManagerImpl implements LocalStringManager {

    // START OF IASRI 4660742
    //WARNING: _logger must be initialized upon demand in this case. The
    //reason is that this static init happens before the ServerContext
    //is initialized
    private static Logger _logger = null;
    // END OF IASRI 4660742

    private Class defaultClass;

    /**
     * Create a string manager that looks for LocalStrings.properties in
     * the package of the defaultClass.
     * @param defaultClass Class whose package has default localized strings
     */
    public LocalStringManagerImpl(Class defaultClass) {
	this.defaultClass = defaultClass;
    }

    /**
     * Get a localized string.
     * Strings are stored in a single property file per package named
     * LocalStrings[_locale].properties. Starting from the class of the
     * caller, we walk up the class hierarchy until we find a package
     * resource bundle that provides a value for the requested key.
     *
     * <p>This simplifies access to resources, at the cost of checking for
     * the resource bundle of several classes upon each call. However, due
     * to the caching performed by <tt>ResourceBundle</tt> this seems 
     * reasonable.
     *
     * <p>Due to that, sub-classes <strong>must</strong> make sure they don't
     * have conflicting resource naming.
     * @param callerClass The object making the call, to allow per-package
     * resource bundles
     * @param key The name of the resource to fetch
     * @param defaultValue The default return value if not found
     * @return The localized value for the resource
     */
    public String getLocalString(
	Class callerClass,
	String key,
	String defaultValue
    ) {
	Class stopClass  = defaultClass.getSuperclass();
	Class startClass = ((callerClass != null) ? callerClass : 
			    defaultClass);
	ResourceBundle resources  = null;
	boolean globalDone = false;
	for (Class c = startClass; 
	     c != stopClass && c != null;
	     c = c.getSuperclass()) {
	    globalDone = (c == defaultClass);
	    try {
		// Construct the bundle name as LocalStrings in the
		// caller class's package.
		StringBuffer resFileName = new StringBuffer(
		    c.getName().substring(0, c.getName().lastIndexOf(".")));
		resFileName.append(".LocalStrings");

		resources = ResourceBundle.getBundle(resFileName.toString(), Locale.getDefault(), c.getClassLoader());
		if ( resources != null ) {
		    String value = resources.getString(key);
		    if ( value != null )
			return value;
		}
	    } catch (Exception ex) {
	    }
	} 

	// Look for a global resource (defined by defaultClass)
	if ( ! globalDone ) {
	    return getLocalString(null, key, defaultValue);
	} else {
            CULoggerInfo.getLogger().log(Level.FINE, "No local string for", key);
	    return defaultValue;
	}
    }

    /**
     * Get a localized string from the package of the default class.
     * @param key The name of the resource to fetch
     * @param defaultValue The default return value if not found
     * @return The localized string
     */
    public String getLocalString(String key, String defaultValue) {
	return getLocalString(null, key, defaultValue);
    }

    /**
     * Get a local string for the caller and format the arguments accordingly.
     * @param callerClass The caller (to walk through its class hierarchy)
     * @param key The key to the local format string
     * @param fmt The default format if not found in the resources
     * @param arguments The set of arguments to provide to the formatter
     * @return A formatted localized string
     */

    public String getLocalString(
	Class callerClass,
	String key,
	String defaultFormat,
	Object... arguments
    ) {
	MessageFormat f = new MessageFormat(
	    getLocalString(callerClass, key, defaultFormat));
	for (int i = 0; i < arguments.length; i++) {
	    if ( arguments[i] == null ) {
		arguments[i] = "null";
	    } else if  ( !(arguments[i] instanceof String) &&
		 !(arguments[i] instanceof Number) &&
		 !(arguments[i] instanceof java.util.Date)) {
		arguments[i] = arguments[i].toString();
	    }
	}
	return f.format(arguments);
    }

    /**
     * Get a local string from the package of the default class and
     * format the arguments accordingly.
     * @param key The key to the local format string
     * @param fmt The default format if not found in the resources
     * @param arguments The set of arguments to provide to the formatter
     * @return A formatted localized string
     */
    public String getLocalString(
	String key,
	String defaultFormat,
	Object... arguments
    ) {
	return getLocalString(null, key, defaultFormat, arguments);
    }
}

