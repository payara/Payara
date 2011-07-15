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

package com.sun.enterprise.backup;

import java.util.ResourceBundle;
import java.text.MessageFormat;

/**
 *
 * @author  bnevins
 */
class StringHelper
{
	private StringHelper()
	{
	}
	
	/**
	 * @return the String from LocalStrings or the supplied String if it doesn't exist
	 */
	
	static String get(String s)
	{
		try 
		{
			return bundle.getString(s);
		} 
		catch (Exception e) 
		{
			// it is not an error to have no key...
			return s;
		}
	}

	/** 
	 * Convenience method which calls get(String, Object[])
	 * @return the String from LocalStrings or the supplied String if it doesn't exist --
	 * using the one supplied argument
	 * @see get(String, Object[])
	 */
	static String get(String s, Object o)
	{
		return get(s, new Object[] { o });
	}

	/** 
	 * Convenience method which calls get(String, Object[])
	 * @return the String from LocalStrings or the supplied String if it doesn't exist --
	 * using the two supplied arguments
	 * @see get(String, Object[])
	 */
	static String get(String s, Object o1, Object o2)
	{
		return get(s, new Object[] { o1, o2 });
	}
	
	/** 
	 * Convenience method which calls get(String, Object[])
	 * @return the String from LocalStrings or the supplied String if it doesn't exist --
	 * using the three supplied arguments
	 * @see get(String, Object[])
	 */
	static String get(String s, Object o1, Object o2, Object o3)
	{
		return get(s, new Object[] { o1, o2, o3 });
	}
	
	/**
	 * Get and format a String from LocalStrings.properties
	 * @return the String from LocalStrings or the supplied String if it doesn't exist --
	 * using the array of supplied Object arguments
	 */
	static String get(String s, Object[] objects)
	{
		s = get(s);
		
		try
		{
			MessageFormat mf = new MessageFormat(s);
			return mf.format(objects);
		}
		catch(Exception e)
		{
			return s;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	
	private static	ResourceBundle bundle;

	static
	{
		try 
		{   
			String props = StringHelper.class.getPackage().getName() + ".LocalStrings";
			bundle = ResourceBundle.getBundle(props);
		} 
		catch (Exception e) 
		{
			LoggerHelper.warning("No resource bundle found: " + Constants.exceptionResourceBundle, e);
			bundle = null;
		}
	}
	
	static void main(String[] notUsed)
	{
		System.out.println("key=backup-res.BadProjectBackupDir, value =" + get("backup-res.BadProjectBackupDir"));
	}
}


