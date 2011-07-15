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
 * StringHelper.java
 *
 * Created on March 3, 2000
 */

package com.sun.jdo.spi.persistence.utility;

import java.util.*;

/**
 * NOTE: These utilities have been moved from another (more specific 
 * package's) utility class so that more classes can have access to them.  
 * There can be some refactoring work to combine these with some of the 
 * methods in the StringScanner class.
 *
 */

public class StringHelper
{
	/**
	 * constants for escape
	 */
	private static final char BACKSLASH = '\\';
	private static final char QUOTE = '"';

	/** Convert an array of objects into a separated string.  This method 
	 * assumes there is no instance of the separator in the strings of list.
	 * @param list The list of objects to be expanded.
	 * @param beginIndex The index of the first element in the list to be used.
	 * @param endIndex The index of the last element in the list to be used.
	 * @param separator The separator to be used between strings.
	 * @return a string representing the expanded list.
	 */
	public static String arrayToSeparatedList (List list, int beginIndex,
		int endIndex, String separator)
	{
		StringBuffer result = new StringBuffer();

		if (list != null)
		{
			int i, count = (endIndex + 1);

			if ((count > beginIndex) && (list.size() >= count))
				result.append(list.get(beginIndex));

			for (i = beginIndex + 1; i < count; i++)
				result.append(separator + list.get(i));
		}

		return result.toString();
	}

	/** Convert an array of objects into a separated string using the default 
	 * separator.  This method assumes there is no instance of the separator 
	 * in the strings of list.
	 * @param list The list of objects to be expanded.
	 * @param beginIndex The index of the first element in the list to be used.
	 * @param endIndex The index of the last element in the list to be used.
	 * @return a string representing the expanded list.
	 */
	public static String arrayToSeparatedList (List list, int beginIndex,
		int endIndex)
	{
		return arrayToSeparatedList(list, beginIndex, endIndex, ","); // NOI18N
	}

	/** Convert an array of objects into a separated string using the specified 
	 * separator and the entire array.  This method assumes there is no 
	 * instance of the separator in the strings of list.
	 * @param list The list of objects to be expanded.
	 * @param separator The separator to be used between strings.
	 * @return a string representing the expanded list.
	 */
	public static String arrayToSeparatedList (List list, String separator)
	{
		return arrayToSeparatedList(list, 0, 
			((list != null) ? (list.size() - 1) : 0), separator);
	}

	/** Convert an array of objects into a separated string using the default 
	 * separator and the entire array.  This method assumes there is no 
	 * instance of the separator in the strings of list.
	 * @param list The list of objects to be expanded.
	 * @return a string representing the expanded list.
	 */
	public static String arrayToSeparatedList (List list)
	{
		return arrayToSeparatedList(list, 0, 
			((list != null) ? (list.size() - 1) : 0));
	}

	/** Convert a separated string to an array of strings
	 * @param list The string representing the list of objects.
	 * @param separator The separator to be used to tokenize strings.
	 * @return an array representing the tokenized list.
	 */
	public static List separatedListToArray (String list, String separator)
	{
		ArrayList result = new ArrayList();

		if (list != null)
		{
			StringTokenizer st = new StringTokenizer(list, separator);
			int i, size = st.countTokens();

			for (i = 0; i < size; i++)
				result.add(st.nextToken());
		}

		return result;
	}

	/** Convert a separated string to an array of strings using the default 
	 * separator.
	 * @param list The string representing the list of objects.
	 * @return an array representing the tokenized list.
	 */
	public static List separatedListToArray (String list)
	{
		return separatedListToArray(list, ","); // NOI18N
	}

	/** Convert an array of int values into a separated string.  
	 * @param intArray The array of int values to be expanded.
	 * @param separator The separator to be used between strings.
	 * @return a string representing the expanded array.
	 */
	public static String intArrayToSeparatedList(int[] intArray, 
		String separator)
	{
		return intArrayToSeparatedList(intArray, 0, 
			((intArray != null) ? (intArray.length - 1) : 0), separator);
	}
	
	/** Convert an array of int values into a separated string.  
	 * @param intArray The array of int values to be expanded.
	 * @param beginIndex The index of the first element in the array to be used.
	 * @param endIndex The index of the last element in the array to be used.
	 * @param separator The separator to be used between strings.
	 * @return a string representing the expanded array.
	 */
	public static String intArrayToSeparatedList(int[] intArray, int beginIndex,
		int endIndex, String separator)
	{
		StringBuffer result = new StringBuffer();

		if (intArray != null) 
		{
			int count = (endIndex + 1);
			if ((count > beginIndex) && (intArray.length >= count))
				result.append(intArray[beginIndex]);

			for (int i = beginIndex + 1; i < count; i++) {
				result.append(separator);
				result.append(intArray[i]);
			}
		}

		return result.toString();
	}
	
	/** Checks if a string is null or empty.
	 * @return <code>true</code> if the string is null or empty after trim, 
	 * <code>false</code> otherwirse.
	 */
	public static boolean isEmpty (String aString)
	{
		return ((aString == null) || (aString.trim().length() == 0));
	}

	/** Gets a version of the specified string with the first letter 
	 * capitalized.  This can be used to convert a field name to get and set 
	 * method names.
	 * @param aString the string to be capitalized
	 * @return a capitalized for the specified string
	 */
	public static String getCapitalizedString (String aString)
	{
		if (isEmpty(aString))
			return aString;

		return Character.toUpperCase(aString.charAt(0)) + aString.substring(1);
	}

	/** Replaces the first occurence of <code>oldString</code> in <code>string</code>
	 * with <code>newString</code>. The methods returns either a new string 
	 * instance (in the case <code>oldString</code> is included in the string)
	 * or the origial string itself (in the case <code>oldString</code> is not 
	 * included).
	 * @param string	the original string.
	 * @param oldString the string to be replaced.
	 * @param newString the string the old value is replaced with.
	 * @return a string derived from the specified this string by replacing the 
	 * first occurence oldString with newString.
	 */
	public static String replaceFirst (String string, String oldString, String newString)
	{
		int index = string.indexOf(oldString);
		if (index != -1) {
			StringBuffer sb = new StringBuffer(string.length());
			sb.append(string.substring(0, index));
			sb.append(newString);
			sb.append(string.substring(index + oldString.length()));
			return sb.toString();
		}
		return string;
	}

	/** Replaces all occurences of <code>oldString</code> in <code>string</code>
	 * with <code>newString</code>. The methods returns either a new string
	 * instance (in the case <code>oldString</code> is included in the string)
	 * or the origial string itself (in the case <code>oldString</code> is not
	 * included).
	 * @param string	the original string.
	 * @param oldString the string to be replaced.
	 * @param newString the string the old value is replaced with.
	 * @return a string derived from the specified this string by replacing
	 * every occurrence of oldString with newString.
	 */
	public static String replace (String string, String oldString, 
		String newString)
	{
		StringBuffer sb = null;
		final int l = oldString.length();
		int	beginIndex = 0;
		int	index;

		while ((index = string.indexOf(oldString, beginIndex)) > -1)
		{
			// only create the StringBuffer if there's an occurence of oldString
			if (sb == null)
			{
				sb = new StringBuffer(string.length());
			}
			sb.append(string.substring(beginIndex, index));
			sb.append(newString);
			beginIndex = index + l;
		}

		// append the rest if the old value was found at least once
		if (sb != null)
		{
			sb.append(string.substring(beginIndex));
		}

		return (sb != null ? sb.toString() : string);	
	}

	/**
	 * Trims trailing spaces from input.
	 * @param input The input string.
	 * @return A new string with trailing spaces trimmed. If there are no 
	 * trailing spaces, returns <CODE>input</CODE>.
	 */
	public static String rtrim (String input)
	{
		String retVal = input;

		if (input != null)
		{
			int lastCharIndex = input.length() - 1;
			int originalLastCharIndex = lastCharIndex;

			while ((lastCharIndex >= 0) && 
				Character.isSpaceChar(input.charAt(lastCharIndex)))
			{
				lastCharIndex--;
			}
			if (lastCharIndex != originalLastCharIndex)
			{
				//We have characters to trim. 
				retVal = input.substring(0,lastCharIndex + 1);
			}
		}

		return retVal;
	}

	/**
	 * Escaping given string by " and \.
	 * @param str String to be escaped
	 * @return string escaped by " and \.
	 */
	public static String escape(String str)
	{
		if (str == null)
		{
			return str;
		}
		else
		{
			int indS = str.indexOf(BACKSLASH);
			int indQ = str.indexOf(QUOTE);
			if (indS == -1 && indQ == -1)
			{
				return str;
			}
			else
			{
				StringBuffer buf = new StringBuffer();
				char data[] = str.toCharArray();
				for (int i = 0; i < data.length; i++)
				{
					if (BACKSLASH == data[i] || QUOTE == data[i])
					{
						buf.append(BACKSLASH);
					}
					buf.append(data[i]);
				}
				return buf.toString();
			}
		}
	}
}
