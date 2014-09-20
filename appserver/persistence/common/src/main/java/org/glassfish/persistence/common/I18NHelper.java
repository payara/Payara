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

package org.glassfish.persistence.common;

import java.util.*;
import java.text.MessageFormat;

public class I18NHelper {
	private static final String bundleSuffix = ".Bundle";	// NOI18N
	private static Hashtable	bundles = new Hashtable();
  	private static Locale 		locale = Locale.getDefault();

	/**
	 * Constructor
	 */
  	public I18NHelper() {
  	}

	/**
	 * Load ResourceBundle by bundle name
	 */
  	public static ResourceBundle loadBundle(String bundleName) {
		return loadBundle(bundleName, I18NHelper.class.getClassLoader());
  	}

	/**
	 * Load ResourceBundle by bundle name and class loader
	 */
  	public static ResourceBundle loadBundle(String bundleName, ClassLoader loader) {
      		ResourceBundle messages = (ResourceBundle)bundles.get(bundleName);

    		if (messages == null) //not found as loaded - add
		{
      			messages = ResourceBundle.getBundle(bundleName, locale, loader);
			bundles.put(bundleName, messages);
    		}
		return messages;
  	}

	/**
	 * Load ResourceBundle by class object - figure out the bundle name
	 * for the class object's package and use the class' class loader.
	 */
  	public static ResourceBundle loadBundle(Class classObject) {
		return loadBundle(
    			getPackageName(classObject.getName()) + bundleSuffix,
    				classObject.getClassLoader());
	}

  
	/**
	 * Returns message as String
	 */
	final public static String getMessage(ResourceBundle messages, String messageKey) 
	{
    		return messages.getString(messageKey);
  	}

  	/**
	 * Formats message by adding Array of arguments
	 */
	final public static String getMessage(ResourceBundle messages, String messageKey, Object msgArgs[]) 
	{
    		for (int i=0; i<msgArgs.length; i++) {
        		if (msgArgs[i] == null) msgArgs[i] = ""; // NOI18N
    		}
    		MessageFormat formatter = new MessageFormat(messages.getString(messageKey));
    		return formatter.format(msgArgs);
  	}
  	/**
	 * Formats message by adding a String argument
	 */
	final public static String getMessage(ResourceBundle messages, String messageKey, String arg) 
	{
    		Object []args = {arg};
    		return getMessage(messages, messageKey, args);
  	}
  	/**
	 * Formats message by adding two String arguments
	 */
	final public static String getMessage(ResourceBundle messages, String messageKey, String arg1,
				       String arg2) 
	{
    		Object []args = {arg1, arg2};
    		return getMessage(messages, messageKey, args);
  	}
  	/**
	 * Formats message by adding three String arguments
	 */
	final public static String getMessage(ResourceBundle messages, String messageKey, String arg1,
				       String arg2, String arg3) 
	{
    		Object []args = {arg1, arg2, arg3};
    		return getMessage(messages, messageKey, args);
  	}
  	/**
	 *
	 * Formats message by adding an Object as an argument
	 */
	final public static String getMessage(ResourceBundle messages, String messageKey, Object arg) 
	{
    		Object []args = {arg};
    		return getMessage(messages, messageKey, args);
  	}
  	/**
	 * Formats message by adding an int as an argument
	 */
	final public static String getMessage(ResourceBundle messages, String messageKey, int arg) 
	{
    		Object []args = {arg};
    		return getMessage(messages, messageKey, args);
  	}
  	/**
	 * Formats message by adding a boolean as an argument
	 */
	final public static String getMessage(ResourceBundle messages, String messageKey, boolean arg) 
	{
    		Object []args = {String.valueOf(arg)};
    		return getMessage(messages, messageKey, args);
  	}

        /**
         * Returns the package portion of the specified class
         * @param className the name of the class from which to extract the
         * package
         * @return package portion of the specified class
         */
        private static String getPackageName (final String className)
        {
                if (className != null)
                {
                        final int index = className.lastIndexOf('.');

                        return ((index != -1) ?
                                className.substring(0, index) : ""); // NOI18N
                }

                return null;
    	}

}
