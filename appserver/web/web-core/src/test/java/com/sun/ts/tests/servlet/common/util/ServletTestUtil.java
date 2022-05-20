/*
 * Copyright (c) 2007, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

/*
 * $Id$
 */

package com.sun.ts.tests.servlet.common.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;

/**
 * A set of useful utility methods to help perform test functions.
 */
public class ServletTestUtil {

  /**
   * Flag to enabled the printing of debug statements.
   */
  public final static boolean DEBUG = true;

  /**
   * Private as this class contains only public static methods.
   */
  private ServletTestUtil() {
  }

  /**
   * Compares the String values in an Enumeration against the provides String
   * array of values. The number of elements in the enumeration must be the same
   * as the size of the array, or false will be returned. False will also be
   * returned if the provided Enumeration or String array is null.
   *
   * If all values are found, true will be returned.
   *
   * <em>Note:</em> This method isn't concerned with the presence of duplicate
   * values contained in the enumeration.
   *
   * The comparison is performed in a case sensitive manner.
   * 
   * @param e
   *          - Enumeration to validate
   * @param values
   *          - the values expected to be found in the Enumeration
   *
   * @return true if all the expected values are found, otherwise false.
   */
  public static boolean checkEnumeration(Enumeration e, String[] values) {
    return checkEnumeration(e, values, true, true);
  }

  /**
   * Compares the String values in an Enumeration against the provides String
   * array of values. The number of elements in the enumeration must be the same
   * as the size of the array, or false will be returned. False will also be
   * returned if the provided Enumeration or String array is null.
   *
   * If all values are found, true will be returned.
   *
   * <em>Note:</em> This method isn't concerned with the presence of duplicate
   * values contained in the enumeration.
   *
   * The comparison is performed in a case sensitive manner.
   * 
   * @param e
   *          - Enumeration to validate
   * @param values
   *          - the values expected to be found in the Enumeration
   * @param enforceSizes
   *          - ensures that the number of elements in the Enumeration matches
   *          the number of elements in the array of values
   * @param allowDuplicates
   *          - If true, the method will true if duplicate elements are found in
   *          the Enumeration, if false, then false will be return if duplicate
   *          elements have been found.
   *
   * @return true if all the expected values are found, otherwise false.
   */
  public static boolean checkEnumeration(Enumeration e, String[] values,
      boolean enforceSizes, boolean allowDuplicates) {
    List foundValues = null;

    if (e == null || !e.hasMoreElements() || values == null) {
      return false;
    }

    if (!allowDuplicates) {
      foundValues = new ArrayList();
    }

    boolean valuesFound = true;
    Arrays.sort(values);
    int count = 0;
    while (e.hasMoreElements()) {
      Object val = null;
      try {
        val = e.nextElement();
        count++;
        if (!allowDuplicates) {
          if (foundValues.contains(val)) {
            debug("[ServletTestUtil] Duplicate values found in "
                + "Enumeration when duplicates are not allowed."
                + "Values found in the Enumeration: " + getAsString(e));
            valuesFound = false;
            break;
          }
          foundValues.add(val);
        }

      } catch (NoSuchElementException nsee) {
        debug("[ServletTestUtil] There were less elements in the "
            + "Enumeration than expected");
        valuesFound = false;
        break;
      }
      debug("[ServletTestUtil] Looking for '" + val + "' in values: "
          + getAsString(values));
      if ((Arrays.binarySearch(values, val) < 0) && (enforceSizes)) {
        debug("[ServletTestUtil] Value '" + val + "' not found.");
        valuesFound = false;
        continue;
      }
    }

    if (enforceSizes) {
      if (e.hasMoreElements()) {
        // more elements than should have been.
        debug("[ServletTestUtil] There were more elements in the Enumeration "
            + "than expected.");
        valuesFound = false;
      }
      if (count != values.length) {
        debug("[ServletTestUtil] There number of elements in the Enumeration "
            + "did not match number of expected values."
            + "Expected number of Values=" + values.length
            + ", Actual number of Enumeration elements=" + count);

        valuesFound = false;
      }
    }
    return valuesFound;
  }

  public static boolean checkArrayList(ArrayList al, String[] values,
      boolean enforceSizes, boolean allowDuplicates) {
    List foundValues = null;

    if (al == null || al.isEmpty() || values == null) {
      return false;
    }

    if (!allowDuplicates) {
      foundValues = new ArrayList();
    }

    al.trimToSize();
    boolean valuesFound = true;
    Arrays.sort(values);
    int len = al.size();
    for (int i = 0; i < len; i++) {
      Object val = null;
      val = (String) al.get(i);
      debug("[ServletTestUtil] val=" + val);
      if (!allowDuplicates) {
        if (foundValues.contains(val)) {
          debug("[ServletTestUtil] Duplicate values found in "
              + "ArrayList when duplicates are not allowed."
              + "Values found in the ArrayList: " + getAsString(al));
          valuesFound = false;
          break;
        }
        foundValues.add(val);
      }
      debug("[ServletTestUtil] Looking for '" + val + "' in values: "
          + getAsString(values));
      if ((Arrays.binarySearch(values, val) < 0) && (enforceSizes)) {
        debug("[ServletTestUtil] Value '" + val + "' not found.");
        valuesFound = false;
        continue;
      }
    }

    if (enforceSizes) {
      if (len != values.length) {
        debug("[ServletTestUtil] There number of elements in the ArrayList "
            + "did not match number of expected values."
            + "Expected number of Values=" + values.length
            + ", Actual number of ArrayList elements=" + len);

        valuesFound = false;
      }
    }
    return valuesFound;
  }

  public static boolean compareString(String expected, String actual) {
    String[] list_expected = expected.split("[|]");
    boolean found = true;
    for (int i = 0, n = list_expected.length, startIdx = 0, bodyLength = actual
        .length(); i < n; i++) {

      String search = list_expected[i];
      if (startIdx >= bodyLength) {
        startIdx = bodyLength;
      }

      int searchIdx = actual.toLowerCase().indexOf(search.toLowerCase(),
          startIdx);

      debug("[ServletTestUtil] Scanning response for " + "search string: '"
          + search + "' starting at index " + "location: " + startIdx);
      if (searchIdx < 0) {
        found = false;
        StringBuffer sb = new StringBuffer(255);
        sb.append("[ServletTestUtil] Unable to find the following ");
        sb.append("search string in the server's ");
        sb.append("response: '").append(search).append("' at index: ");
        sb.append(startIdx);
        sb.append("\n[ServletTestUtil] Server's response:\n");
        sb.append("-------------------------------------------\n");
        sb.append(actual);
        sb.append("\n-------------------------------------------\n");
        debug(sb.toString());
        break;
      }

      debug("[ServletTestUtil] Found search string: '" + search + "' at index '"
          + searchIdx + "' in the server's " + "response");
      // the new searchIdx is the old index plus the lenght of the
      // search string.
      startIdx = searchIdx + search.length();
    }
    return found;
  }

  /**
   * Returns the provided String array in the following format:
   * <tt>[n1,n2,n...]</tt>
   * 
   * @param sArray
   *          - an array of Objects
   * @return - a String based off the values in the array
   */
  public static String getAsString(Object[] sArray) {
    if (sArray == null) {
      return null;
    }
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    for (int i = 0; i < sArray.length; i++) {
      buf.append(sArray[i].toString());
      if ((i + 1) != sArray.length) {
        buf.append(",");
      }
    }
    buf.append("]");
    return buf.toString();
  }

  public static String getAsString(ArrayList al) {
    if (al == null) {
      return null;
    }
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    al.trimToSize();
    for (int i = 0, len = al.size(); i < len; i++) {
      buf.append((String) al.get(i));
      if ((i + 1) != len) {
        buf.append(",");
      }
    }
    buf.append("]");
    return buf.toString();
  }

  /**
   * Returns the provided Enumeration as a String in the following format:
   * <tt>[n1,n2,n...]</tt>
   * 
   * @param e
   *          - an Enumeration
   * @return - a printable version of the contents of the Enumeration
   */
  public static String getAsString(Enumeration e) {
    return getAsString(getAsArray(e));
  }

  /**
   * Returnes the provides Enumeration as an Array of String Arguments.
   * 
   * @param e
   *          - an Enumeration
   * @return - the elements of the Enumeration as an array of Objects
   */
  public static Object[] getAsArray(Enumeration e) {
    List list = new ArrayList();
    while (e.hasMoreElements()) {
      list.add(e.nextElement());
    }
    return list.toArray(new Object[list.size()]);
  }

  /**
   * Returnes the provided string as an Array of Strings.
   * 
   * @param e
   *          - a String
   * @return - the elements of the String as an array of Strings
   */
  public static String[] getAsArray(String value) {
    StringTokenizer st = new StringTokenizer(value, ",");
    String[] retValues = new String[st.countTokens()];
    for (int i = 0; st.hasMoreTokens(); i++) {
      retValues[i] = st.nextToken();
    }
    return retValues;
  }

  /**
   * Writes the provided message to System.out when the <tt>debug</tt> is set.
   * 
   * @param message
   *          - the message to write to System.out
   */
  public static void debug(String message) {
    if (DEBUG) {
      System.out.println(message);
    }
  }

  public static void printResult(PrintWriter pw, String s) {

    // if string is null or empty, then it passed
    if (s == null || s.equals("")) {
      pw.println(Data.PASSED);
    } else {
      pw.println(Data.FAILED + ": " + s);
    }
  }

  public static void printResult(PrintWriter pw, boolean b) {
    if (b) {
      pw.println(Data.PASSED);
    } else {
      pw.println(Data.FAILED);
    }
  }

  public static void printResult(ServletOutputStream pw, boolean b)
      throws IOException {
    if (b) {
      pw.println(Data.PASSED);
    } else {
      pw.println(Data.FAILED);
    }
  }

  public static void printFailureData(PrintWriter pw, ArrayList result,
      Object[] expected) {
    pw.println("Unable to find the expected values:\n " + "   "
        + ServletTestUtil.getAsString(expected)
        + "\nin the results returned by the test which were:\n" + "   "
        + ServletTestUtil.getAsString(result));
  }

  public static void printFailureData(PrintWriter pw, Enumeration result,
      Object[] expected) {
    pw.println("Unable to find the expected values:\n " + "   "
        + ServletTestUtil.getAsString(expected)
        + "\nin the results returned by the test which were:\n" + "   "
        + ServletTestUtil.getAsString(result));
  }

  public static int findCookie(Cookie[] cookie, String name) {
    boolean found = false;
    int i = 0;
    if (cookie != null) {
      while ((!found) && (i < cookie.length)) {
        // System.out.println("cookie["+i+"]="+cookie[i].getName());
        if (cookie[i].getName().equals(name)) {
          found = true;
        } else {
          i++;
        }
      }
    } else {
      found = false;
    }
    if (found) {
      return i;
    } else {
      return -1;
    }
  }
}
