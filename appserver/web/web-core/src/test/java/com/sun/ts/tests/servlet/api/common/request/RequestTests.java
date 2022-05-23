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
package com.sun.ts.tests.servlet.api.common.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import com.sun.ts.tests.servlet.common.util.Data;
import com.sun.ts.tests.servlet.common.util.ServletTestUtil;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class RequestTests {

  static String savedSessionId = null;

  static String savedRequestedSessionId = null;

  static HttpSession savedSession = null;

  // ------------------------ jakarta_servlet.RequestRequest
  // -------------------------------
  public static void requestWrapperConstructorTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {
    ServletRequestWrapper srw = new ServletRequestWrapper(request);
    if (srw != null) {
      ServletTestUtil.printResult(pw, true);
    } else {
      ServletTestUtil.printResult(pw, false);
    }
  }

  public static void requestWrapperGetRequestTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    ServletRequestWrapper srw = new ServletRequestWrapper(request);
    if (srw != null) {
      ServletRequest sr = srw.getRequest();
      if (!request.equals(sr)) {
        passed = false;
        pw.println("getRequest failed to return the same request object");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("constructor returned a null");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void requestWrapperSetRequestTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = true;
    ServletRequestWrapper srw = new ServletRequestWrapper(request);
    if (srw != null) {
      try {
        srw.setRequest(request);
      } catch (Throwable t) {
        passed = false;
        pw.println("Error: setRequest generated a Throwable");
        pw.println("Exception:" + t.getMessage());
      }
      if (passed) {
        ServletRequest sr = srw.getRequest();
        if (!request.equals(sr)) {
          passed = false;
          pw.println("getRequest failed to return the same request object");
        }
      }
    } else {
      passed = false;
      pw.println("constructor returned a null");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void requestWrapperSetRequestIllegalArgumentExceptionTest(
      PrintWriter pw, ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    ServletRequestWrapper srw = new ServletRequestWrapper(request);
    if (srw != null) {
      try {
        srw.setRequest(null);
        passed = false;
        pw.println(
            "Error: an IllegalArgumentException should have been generated");
      } catch (Throwable t) {
        if (t instanceof IllegalArgumentException) {
          passed = true;
        } else {
          passed = false;
          pw.println(
              "Exception thrown, but was not an instance of IllegalArgumentException.");
          pw.println("instead received: " + t.getClass().getName());
        }
      }
    } else {
      passed = false;
      pw.println("constructor returned a null");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getAttributeNamesTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    ;

    Enumeration e = request.getAttributeNames();
    Object[] attr = ServletTestUtil.getAsArray(e);
    for (int i = 0, len = attr.length; i < len; i++) {
      request.removeAttribute((String) attr[i]);
    }

    String attribute1 = "attribute1";
    String attribute2 = "attribute2";

    request.setAttribute(attribute1, "value1");
    request.setAttribute(attribute2, "value2");
    String[] expected = { attribute1, attribute2 };

    e = request.getAttributeNames();

    if (!ServletTestUtil.checkEnumeration(e, expected)) {
      passed = false;
      ServletTestUtil.printFailureData(pw, e, expected);
    } else {
      passed = true;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getAttributeNamesEmptyEnumTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    Enumeration e = request.getAttributeNames();
    Object[] attr = ServletTestUtil.getAsArray(e);
    for (int i = 0, len = attr.length; i < len; i++) {
      request.removeAttribute((String) attr[i]);
    }
    e = request.getAttributeNames();
    if (!e.hasMoreElements()) {
      passed = true;
    } else {
      passed = false;
      pw.println(
          "getAttributeNames() returned a non empty enumeration after all attributes were removed");
      pw.println("The values returned were:");
      while (e.hasMoreElements()) {
        pw.println(" " + (String) e.nextElement());
      }
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getAttributeTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;

    request.setAttribute("attribute1", "value1");

    Object o = request.getAttribute("attribute1");

    if (o != null) {
      if (o instanceof String) {
        String attr = (String) o;
        if (!attr.equals("value1")) {
          passed = false;
          pw.println("getAttribute() returned incorrect value");
          pw.println("Expected Attribute Value -> value1");
          pw.println("Actual Attribute value returned -> + attr ");
        } else {
          passed = true;
        }
      } else {
        passed = false;
        pw.println("getAttribute() returned an Object of type String");
      }
    } else {
      passed = false;
      pw.println("getAttribute() returned a null attribute");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getAttributeDoesNotExistTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    Object o = request.getAttribute("doesnotexist");

    // No attribute was set. Expecting null value
    if (o != null) {
      passed = false;
      pw.println("getAttribute() returned incorrect value");
      pw.println("Expected Attribute Value -> null");
      pw.println("Actual Attribute value returned -> + o ");
    } else {
      passed = true;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getCharacterEncodingTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    // getting char encoding
    String expectedResult = "ISO-8859-1";
    String encoding = request.getCharacterEncoding();

    if (encoding != null) {
      if (!encoding.equals(expectedResult)) {
        passed = false;
        pw.println(
            "getCharacterEncoding() did not receive the proper encoding");
        pw.println("Expected result = " + expectedResult);
        pw.println("Actual result = |" + encoding + "|");

      } else {
        passed = true;
      }

    } else {
      passed = false;
      pw.println("getCharacterEncoding() returned a null result");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getContentLengthTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    // get the content length
    int contentLength = request.getContentLength();

    if (contentLength > 0) {
      int len = 0;

      // getting input stream
      ServletInputStream sin = request.getInputStream();
      // read from the stream
      while (sin.read() != -1) {
        len++;
      }

      // did we get what we wrote
      if ((contentLength != len) && (contentLength != -1)) {
        passed = false;
        pw.println("getContentLength() method FAILED");
        pw.println("Expected Value returned ->" + contentLength);
        pw.println("Actual Value returned -> " + len);
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getContentLength() returned an incorrect length");
      pw.println("Expected length =  > 0");
      pw.println("Actual length = " + contentLength);
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getContentTypeTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;
    String expectedResult = "text/plain";
    String contentType = request.getContentType();

    if (contentType != null) {
      if (!expectedResult.equals(contentType)) {
        passed = false;
        pw.println("getContentType() did not receive the proper content type");
        pw.println("Expected result=" + expectedResult);
        pw.println("Actual result=" + contentType);
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getContentType() returned a null");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getContentTypeNullTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    String contentType = request.getContentType();

    if (contentType != null) {
      passed = false;
      pw.println("getContentType() did not a return a null");
      pw.println("Actual result=" + contentType);
    } else {
      passed = true;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getInputStreamTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;
    String expected = "calling getInputStreamTest";

    StringBuffer sb = new StringBuffer();

    ServletInputStream sin = request.getInputStream();

    int tmp = 0;
    try {
      tmp = sin.read();
      while (tmp != -1) {
        sb.append((char) tmp);
        tmp = sin.read();
      }
      String actual = sb.toString();
      if (actual != null) {
        if (!actual.equals(expected)) {
          passed = false;
          pw.println("getInputStream returned the wrong body content");
          pw.println("expected =" + expected);
          pw.println("actual =" + actual);
        } else {
          passed = true;
        }
      } else {
        passed = false;
        pw.println("getInputStream returned null body content");
      }
    } catch (Throwable t) {
      passed = false;
      pw.println("read() generated an exception");
      t.printStackTrace(pw);
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getInputStreamIllegalStateExceptionTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    // getting Reader object
    request.getReader();

    try {
      // we already got reader object
      // IllegalStateException should be thrown
      ServletInputStream sin = request.getInputStream();
      passed = false;
      pw.println(
          "getInputStream should have thrown an IllegalStateException exception");
    } catch (Throwable t) {
      if (t instanceof IllegalStateException) {
        passed = true;
      } else {
        passed = false;
        pw.println(
            "Exception thrown, but was not an instance of IllegalStateException .");
        pw.println("instead received: " + t.getClass().getName());
      }
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getLocaleTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    boolean passed = false;
    Locale expectedResult = new Locale("en", "US");
    Locale actual = request.getLocale();

    if (actual != null) {
      if (!actual.equals(expectedResult)) {
        passed = false;
        pw.println("getLocale() did not receive the proper locale");
        pw.println("Expected result = " + expectedResult.getLanguage() + "-"
            + expectedResult.getCountry());
        pw.println("Actual result = |" + actual.getLanguage() + "-"
            + actual.getCountry() + "|");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getLocale() returned a null result");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getLocaleDefaultTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    Locale actual = request.getLocale();

    if (actual != null) {
      if ((actual.getLanguage() == null) || (actual.getCountry() == null)) {
        if (actual.getLanguage() == null) {
          passed = false;
          pw.println("Locale.getLanguage() returned a null");
        }
        if (actual.getCountry() == null) {
          passed = false;
          pw.println("Locale.getCountry() returned a null");
        }
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getLocale() returned a null result");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getLocalesDefaultTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    Enumeration e = request.getLocales();

    Object[] result = ServletTestUtil.getAsArray(e);
    if (result.length == 1) {
      Locale locale = ((Locale) result[0]);
      if (locale != null) {
        if ((locale.getLanguage() == null) || (locale.getCountry() == null)) {
          if (locale.getLanguage() == null) {
            passed = false;
            pw.println("Locale.getLanguage() returned a null");
          }
          if (locale.getCountry() == null) {
            passed = false;
            pw.println("Locale.getCountry() returned a null");
          }
        } else {
          passed = true;
        }
      } else {
        passed = false;
        pw.println("getLocales() returned a null result");
      }
    } else if (result.length <= 0) {
      passed = false;
      pw.println("getLocales() did not return any locales");
    } else {
      passed = false;
      pw.println("getLocales() returned more than 1 locale");
      pw.println("The locales received were :");

      for (int i = 0; i < result.length; i++) {
        pw.println(" " + ((Locale) result[i]).getLanguage() + "-"
            + ((Locale) result[i]).getCountry());
      }
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getLocalesTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = true;

    Enumeration e = request.getLocales();
    Locale expectedResult1 = new Locale("en", "US");
    boolean expectedResult1Found = false;
    int expectedResult1order = 1;
    Locale expectedResult2 = new Locale("en", "GB");
    boolean expectedResult2Found = false;
    int expectedResult2order = 2;
    int expectedCount = 2;
    int count = 0;

    if (e.hasMoreElements()) {
      Vector v = new Vector();

      while (e.hasMoreElements()) {
        Locale result = ((Locale) e.nextElement());

        if (result.equals(expectedResult1)) {
          if (!expectedResult1Found) {
            count++;
            expectedResult1Found = true;
            if (count != expectedResult1order) {
              passed = false;
              pw.println(
                  "getLocales() did not return the locale in the correct order");
              pw.println("expected order=" + expectedResult1order);
              pw.println("actual order=" + count);
            }
          } else {
            passed = false;
            pw.println("getLocales() method return the same locale name twice");
            pw.println("The locale already received was "
                + expectedResult1.getLanguage() + "-"
                + expectedResult1.getCountry());
          }
        } else if (result.equals(expectedResult2)) {
          if (!expectedResult2Found) {
            count++;
            expectedResult2Found = true;
            if (count != expectedResult2order) {
              passed = false;
              pw.println(
                  "getLocales() did not return the locale in the correct order");
              pw.println("expected order=" + expectedResult2order);
              pw.println("actual order=" + count);
            }
          } else {
            passed = false;
            pw.println("getLocales() method return the same locale name twice");
            pw.println("The locale already received was "
                + expectedResult2.getLanguage() + "-"
                + expectedResult2.getCountry());
          }
        } else {
          v.add(result);
        }

      }

      if (count != expectedCount) {
        passed = false;
        pw.println("getLocales() did not return the proper number of locales");
        pw.println("Expected count = " + expectedCount);
        pw.println("Actual count = |" + count + "|");
        pw.println("The expected locales received were :");

        if (expectedResult1Found) {
          pw.println(" " + expectedResult1.getLanguage() + "-"
              + expectedResult1.getCountry());
        }

        if (expectedResult2Found) {
          pw.println(" " + expectedResult2.getLanguage() + "-"
              + expectedResult2.getCountry());
        }

        pw.println("Other locales received were :");

        for (int i = 0; i < v.size(); i++) {
          pw.println(" " + ((Locale) v.elementAt(i)).getLanguage() + "-"
              + ((Locale) v.elementAt(i)).getCountry());
        }
      }
    } else {
      passed = false;
      pw.println("getLocales() returned an empty enumeration");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getParameterMapTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = true;

    Map map = request.getParameterMap();

    int count = 0;
    int expectedCount = 2;
    String expectedResult1 = "parameter1";
    String expectedResult1A = "value1";
    int expectedCount1 = 1;
    String expectedResult2 = "parameter2";
    String expectedResult2A = "value2";
    int expectedCount2 = 1;

    if (map.containsKey(expectedResult1)) {
      String result[] = (String[]) map.get(expectedResult1);
      int count2 = result.length;

      if (count2 == expectedCount1) {
        if (result[0].equals(expectedResult1A)) {
          count++;
        } else {
          passed = false;
          pw.println(
              "getParameterMap() did not return the proper value for the specified key");
          pw.println("Expected result = " + expectedResult1A);
          pw.println("Actual result = " + result[0]);
        }
      } else {
        passed = false;
        pw.println(
            "Map.get() returned the wrong number of parameter values for key "
                + expectedResult1);
        pw.println("Expected number = " + expectedCount1);
        pw.println("Actual number = " + count2);
      }
    }

    if (map.containsKey(expectedResult2)) {
      String result[] = (String[]) map.get(expectedResult2);
      int count2 = result.length;

      if (count2 == expectedCount1) {
        if (result[0].equals(expectedResult2A)) {
          count++;
        } else {
          passed = false;
          pw.println(
              "getParameterMap() did not return the proper value for the specified key");
          pw.println("Expected result = " + expectedResult2A);
          pw.println("Actual result = " + result[0]);
        }
      } else {
        passed = false;
        pw.println(
            "Map.get() returned the wrong number of parameter values for key "
                + expectedResult2);
        pw.println("Expected number = " + expectedCount2);
        pw.println("Actual number = " + count2);
      }

    }

    if (count != expectedCount) {
      passed = false;
      pw.println("getParameterMap() return the proper keys and values ");
      pw.println("Expected count = " + expectedCount);
      pw.println("Actual count = |" + count + "|");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getParameterNamesTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    String parameter1 = "parameter1";
    String parameter2 = "parameter2";

    String[] expected = { parameter1, parameter2 };

    Enumeration e = request.getParameterNames();

    if (!ServletTestUtil.checkEnumeration(e, expected, false, false)) {
      passed = false;
      ServletTestUtil.printFailureData(pw, e, expected);
    } else {
      passed = true;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getParameterNamesEmptyEnumTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    // no parameter was set in the client side
    Enumeration e = request.getParameterNames();

    if (e.hasMoreElements()) {
      passed = false;
      pw.println(
          "getParameterNames() return an enumerated list when it should have been empty");
      pw.println("The list contains the following items:");
      ServletTestUtil.getAsString(e);
    } else {
      passed = true;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getParameterTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;

    // getting the request parameter
    String param = "parameter1";
    String actual = request.getParameter(param);
    String expectedResult = "value1";

    if (actual != null) {
      // is param an instance of java.lang.String
      if (!actual.equals(expectedResult)) {
        passed = false;
        pw.println("getParameter() did not return the correct value");
        pw.println("Expected result = " + expectedResult);
        pw.println("Actual result = |" + actual + "|");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getParameter() did not return the correct value");
      pw.println("Expected result = " + expectedResult);
      pw.println("Actual result = |null|");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getParameterValuesTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = true;

    String[] names = request.getParameterValues("Names");
    int enumlength = names.length;
    String expectedResult1 = "value1";
    String expectedResult2 = "value2";
    boolean expectedResult1Found = false;
    boolean expectedResult2Found = false;
    int expectedCount = 2;
    int count = 0;
    int arraycount = 0;

    if (enumlength > 0) {
      Vector v = new Vector();

      while (arraycount < enumlength) {
        String result = names[arraycount++];

        if (result.equals(expectedResult1)) {
          if (!expectedResult1Found) {
            count++;
            expectedResult1Found = true;
          } else {
            passed = false;
            pw.println("getParameterValues() return the same value twice ");
            pw.println("The value already received was " + expectedResult1);
          }
        }

        if (result.equals(expectedResult2)) {
          if (!expectedResult2Found) {
            count++;
            expectedResult2Found = true;
          } else {
            passed = false;
            pw.println("getParameterValues() return the same value twice ");
            pw.println("The value already received was " + expectedResult2);
          }
        } else {
          v.add(result);
        }

      }

      if (count != expectedCount) {
        passed = false;
        pw.println(
            "getParameterValues() did not return the proper number of parameter values");
        pw.println("Expected count = " + expectedCount);
        pw.println("Actual count = |" + count + "|");
        pw.println("The expected parameter values received were :");

        if (expectedResult1Found) {
          pw.println(expectedResult1);
        }

        if (expectedResult2Found) {
          pw.println(expectedResult2);
        }

        pw.println("Other parameter values received were :");

        for (int i = 0; i < v.size(); i++) {
          pw.println(" " + v.elementAt(i).toString());
        }
      }

    } else {
      passed = false;
      pw.println("getParameterValues() returned an empty array");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getParameterValuesDoesNotExistTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    // no parameter was set with name zero

    String[] vals = request.getParameterValues("doesnotexit");

    if (vals != null) {
      passed = false;
      pw.println(
          "getParameterValues() returned a null list of parameter values");
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getParameterDoesNotExistTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    String expectedResult = null;
    String actual = request.getParameter("doesnotexist");
    // we are not settting any parameter in the client side so we should get
    // null
    if (actual != expectedResult) {
      passed = false;
      pw.println("getParameter() did not return the correct result");
      pw.println("Expected result = " + expectedResult);
      pw.println("Actual result = |" + actual + "|");
    } else {
      passed = true;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getProtocolTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;

    String proto = request.getProtocol();
    String expectedResult = "HTTP/1.";
    // looking for HTTP
    int actual = proto.indexOf(expectedResult);

    if (actual == -1) {
      passed = false;
      pw.println("getProtocol() did not return the correct value");
      pw.println("Expected result = " + expectedResult);
      pw.println("Actual count = |" + proto + "|");
    } else {
      passed = true;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getReaderTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;
    String expected = "calling getReaderTest";
    BufferedReader br = request.getReader();

    StringBuffer sb = new StringBuffer();

    try {
      String tmp = br.readLine();
      while (tmp != null) {
        sb.append(tmp);
        tmp = br.readLine();
      }
      String actual = sb.toString();
      if (actual != null) {
        if (!actual.equals(expected)) {
          passed = false;
          pw.println("getReaderTest returned the wrong body content");
          pw.println("expected =" + expected);
          pw.println("actual =" + actual);
        } else {
          passed = true;
        }
      } else {
        passed = false;
        pw.println("getReaderTest returned null body content");
      }
    } catch (Throwable t) {
      passed = false;
      pw.println("readLine() generated an exception");
      t.printStackTrace(pw);
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getReaderIllegalStateExceptionTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    request.getInputStream();

    try {
      BufferedReader br = request.getReader();
      passed = false;
      pw.println("getReader() method did not throw IllegalStateException");
    } catch (Throwable t) {
      if (t instanceof IllegalStateException) {
        passed = true;
      } else {
        passed = false;
        pw.println(
            "Exception thrown, but was not an instance of IllegalStateException.");
        pw.println("instead received: " + t.getClass().getName());
      }
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getReaderUnsupportedEncodingExceptionTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    try {
      BufferedReader br = request.getReader();
      passed = false;
      pw.println(
          "getReader() method did not throw UnsupportedEncodingException");
    } catch (Throwable t) {
      if (t instanceof UnsupportedEncodingException) {
        passed = true;
      } else {
        passed = false;
        pw.println(
            "Exception thrown, but was not an instance of java.io.UnsupportedEncodingException.");
        pw.println("instead received: " + t.getClass().getName());
      }
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getRemoteAddrTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;

    String expResult = request.getParameter("Address");
    String[] expectedResults = ServletTestUtil.getAsArray(expResult);
    Arrays.sort(expectedResults);
    String actual = request.getRemoteAddr();

    if (Arrays.binarySearch(expectedResults, actual) < 0) {
      passed = false;
      pw.println("getRemoteAddr() returned an incorrect result");
      pw.println("Expected result = [ " + expResult + " ]");
      pw.println("Actual result = |" + actual + "|");
    } else {
      passed = true;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getLocalAddrTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    boolean passed = false;

    String expected_results = null;
    InetAddress[] _addrs = null;

    try {
      _addrs = InetAddress
          .getAllByName(InetAddress.getLocalHost().getCanonicalHostName());

      if (_addrs.length != 0) {
        StringBuffer sb = new StringBuffer(32);

        for (int i = 0; i < _addrs.length; i++) {
          String ip = _addrs[i].getHostAddress();
          if (!ip.equals("127.0.0.1")) {
            if (ip.contains("%")) {
              int scope_id = ip.indexOf("%");
              ip = ip.substring(0, scope_id);
            }
            sb.append(ip);
          }
          sb.append(",");
        }
        sb.append("127.0.0.1");
        expected_results = sb.toString();
        pw.println("Local Interface info: " + expected_results);
      }

      String[] expectedResults = ServletTestUtil.getAsArray(expected_results);
      Arrays.sort(expectedResults);
      String actual = request.getLocalAddr();

      if (Arrays.binarySearch(expectedResults, actual) < 0) {
        pw.println("getLocalAddr() returned an incorrect result");
        pw.println("Expected result = [ " + expected_results + " ]");
        pw.println("Actual result = |" + actual + "|");
      } else {
        passed = true;
      }
    } catch (UnknownHostException uhe) {
      pw.println("Unable to obtain local host information.");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getRemoteHostTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;

    String expResult = request.getParameter("Address");
    String[] expectedResults = ServletTestUtil.getAsArray(expResult);
    String actual = request.getRemoteHost();

    for (int i = 0; i < expectedResults.length; i++) {
      if (actual.indexOf(expectedResults[i]) >= 0) {
        passed = true;
        break;
      }
    }

    if (passed == false) {
      pw.println("getRemoteHost() returned an incorrect result");
      pw.println("The expected result could be one of the following: [ "
          + expResult + " ]");

      pw.println("Actual result = |" + actual + "|");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getRequestDispatcherTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    String path = "/WEB-INF/web.xml";

    RequestDispatcher rd = request.getRequestDispatcher(path);

    if (rd == null) {
      passed = false;
      pw.println("getRequestDispatcher() returned a null");
    } else {
      passed = true;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getSchemeTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;

    String expectedResult = "http";
    // check for some value
    String actual = request.getScheme();

    if (actual != null) {
      if (!actual.equals(expectedResult)) {
        passed = false;
        pw.println("getScheme() returned an incorrect result");
        pw.println("Expected result = " + expectedResult);
        pw.println("Actual result = |" + actual + "|");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getScheme() returned a null");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getServerNameTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;

    String actual = request.getServerName();
    String expectedResult = null;
    expectedResult = request.getParameter("hostname");

    if (actual != null) {
      if (expectedResult != null) {
        InetAddress thisHost = null;
        InetAddress expectedHost = null;
        try {
          thisHost = InetAddress.getByName(actual);
          expectedHost = InetAddress.getByName(expectedResult);
        } catch (Throwable t) {
          throw new ServletException(
              "Unexpected problem with Test: " + t.getMessage());
        }

        if (!thisHost.equals(expectedHost)) {
          passed = false;
          pw.println("getServerName() returned an incorrect result");
          pw.println("Expected result (as IP) = " + expectedHost.toString());
          pw.println("Actual result (as IP) = " + thisHost.toString());
        } else {
          passed = true;
        }
      } else {
        passed = false;
        pw.println("getParameter(hostname) returned a null result");
      }

    } else {
      passed = false;
      pw.println("getServerName() returned a null");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getServerPortTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;

    String expectedResult = request.getParameter("port");
    int actual = 0;
    actual = request.getServerPort();

    if (actual != 0) {
      try {
        int sexpectedResult = Integer.parseInt(expectedResult);
        if (actual != sexpectedResult) {
          passed = false;
          pw.println("getServerPort() returned an incorrect result");
          pw.println("Expected result = " + expectedResult);
          pw.println("Actual result = |" + actual + "|");
        } else {
          passed = true;
        }
      } catch (Throwable t) {
        passed = false;
        t.printStackTrace(pw);
      }
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void isSecureTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;

    // check for some value
    boolean expectedResult = false;
    boolean actual = request.isSecure();

    if (actual != expectedResult) {
      passed = false;
      pw.println("isSecure() did not return the correct result");
      pw.println("Expected result = " + expectedResult);
      pw.println("Actual result = |" + actual + "|");
    } else {
      passed = true;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void removeAttributeTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;

    request.setAttribute("attribute1", "value1");
    request.removeAttribute("attribute1");
    String attr = (String) request.getAttribute("attribute1");

    if (attr != null) {
      passed = false;
      pw.println("removeAttribute() did not remove the indicated attribute");
    } else {
      passed = true;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void setAttributeTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;

    String expectedResult = "value1";
    request.setAttribute("attribute1", expectedResult);
    String attr = (String) request.getAttribute("attribute1");

    if (attr != null) {
      if (!attr.equals(expectedResult)) {
        passed = false;
        pw.println("setAttribute() did not set the attribute properly");
        pw.println("Expected result = " + expectedResult);
        pw.println("Actual result = |" + attr + "|");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("setAttribute() did not set the attribute properly");
      pw.println(
          "An attempt to get the attribute resulted in a null being returned");

    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getCharacterEncodingNullTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    String actual = request.getCharacterEncoding();

    if (actual != null) {
      passed = false;
      pw.println("getCharacterEncoding() returned the wrong result");
      pw.println("Expected result = null");
      pw.println("Actual result = |" + actual + "|");
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void setCharacterEncodingUnsupportedEncodingExceptionTest(
      PrintWriter pw, ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    try {
      request.setCharacterEncoding("doesnotexist");
      passed = false;
      pw.println(
          "The exception UnsupportedEncodingException should have been thrown");
    } catch (Throwable t) {
      if (t instanceof UnsupportedEncodingException) {
        passed = true;
      } else {
        passed = false;
        pw.println(
            "Exception thrown, but was not an instance of IllegalStateException.");
        pw.println("instead received: " + t.getClass().getName());
      }
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void setCharacterEncodingTest(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    String expected = "ISO-8859-7";
    try {
      request.setCharacterEncoding(expected);
      String encoding = request.getCharacterEncoding();

      if (encoding != null) {
        if (!encoding.equals(expected)) {
          passed = false;
          pw.println(
              "getCharacterEncoding() did not receive the proper encoding");
          pw.println("Expected result = " + expected);
          pw.println("Actual result = |" + encoding + "|");
        } else {
          passed = true;
        }
      } else {
        passed = false;
        pw.println("getCharacterEncoding() returned a null result");
      }
    } catch (Throwable t) {
      passed = false;
      pw.println("Exception thrown:" + t.getClass().getName());
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void setCharacterEncodingTest1(PrintWriter pw,
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;
    String expected = "ISO-8859-7";

    try {
      BufferedReader br = request.getReader();
      request.setCharacterEncoding(expected);
      String encoding = request.getCharacterEncoding();

      if (encoding == null) {
        pw.println("getCharacterEncoding() returned null as expected");
        passed = true;
      } else {
        pw.println(
            "getCharacterEncoding() returned a non-null result: " + encoding);
      }
    } catch (Throwable t) {
      pw.println("Exception thrown:" + t.getClass().getName());
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getLocalNameTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    boolean passed = false;

    String tmp = request.getLocalName();
    String expected = request.getParameter("hostname");

    if ((expected == null) || (tmp == null)) {
      pw.println("Either answer or expected is null");
      pw.println("Incorrect answer returned = " + tmp);
      pw.println("Expecting " + expected);
    } else {
      tmp = tmp.toLowerCase();
      // Check the cases when hostname and hostname.domain are used to send
      // request
      // Check the cases when localhost or 127.0.0.1 are used to send request
      if (tmp.equals("localhost") || tmp.equals("127.0.0.1")
          || tmp.startsWith(expected.toLowerCase())
          || expected.toLowerCase().startsWith(tmp)
          || expected.toLowerCase().equals("127.0.0.1")
          || expected.toLowerCase().equals("localhost")) {
        passed = true;
        pw.println("Correct answer returned = " + tmp);
        pw.println("Expecting " + expected);
      } else {
        // Checkthe case when ip address is used to send request
        String thisHost = null;
        String expectedHost = null;
        try {
          thisHost = InetAddress.getByName(tmp).getHostAddress();
          expectedHost = InetAddress.getByName(expected).getHostAddress();

          if (thisHost.equals(expected) || thisHost.equals(expectedHost)) {
            pw.println("Test used an IP address to send request");
            pw.println("Correct answer returned = " + tmp);
            pw.println("Got result (as IP) = " + thisHost);
            pw.println("expected " + expected);
            pw.println("expected IP address " + expectedHost);
            passed = true;
          } else {
            pw.println("getLocalName() returned an incorrect result");
            pw.println("Expected result (as hostname) = " + expected);
            pw.println("Expected result (as IP) = " + expectedHost);
            pw.println("Got result (as IP) = " + thisHost);
            pw.println("Incorrect answer returned = " + tmp);
          }
        } catch (java.net.UnknownHostException t) {
          pw.println("Incorrect answer returned = " + tmp);
          pw.println("Not an correct IP address neither");
          pw.println("Expecting " + expected);
        }
      }
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getLocalPortTest(PrintWriter pw, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    boolean passed = false;

    int tmp = request.getLocalPort();

    if (String.valueOf(tmp).equals(request.getParameter("hostport"))) {
      passed = true;
      pw.println("Correct answer returned = " + tmp);
    } else {
      pw.println("Incorrect answer returned = " + tmp);
      pw.println("Expecting " + request.getParameter("hostport"));
    }

    ServletTestUtil.printResult(pw, passed);
  }
  // --------------- END jakarta_servlet.ServletRequest
  // ----------------------------

  // --------------- jakarta_servlet_http.HttpServletRequest
  // -----------------------
  public static void httpRequestWrapperConstructorIllegalArgumentExceptionTest(
      PrintWriter pw, HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;

    try {
      new HttpServletRequestWrapper(null);
      passed = false;
      pw.println("IllegalArgumentException should have been thrown");
    } catch (Throwable t) {
      if (t instanceof IllegalArgumentException) {
        passed = true;
      } else {
        passed = false;
        pw.println(
            "Exception thrown, but was not an instance of IllegalArgumentException.");
        pw.println("instead received: " + t.getClass().getName());
      }
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void httpRequestWrapperConstructorTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    HttpServletRequestWrapper hsrw = new HttpServletRequestWrapper(request);
    if (hsrw != null) {
      ServletTestUtil.printResult(pw, true);
    } else {
      ServletTestUtil.printResult(pw, false);
    }
  }

  public static void httpRequestWrapperGetRequestTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = true;
    HttpServletRequestWrapper hsrw = new HttpServletRequestWrapper(request);
    if (hsrw != null) {
      ServletRequest sr = hsrw.getRequest();
      if (!((ServletRequest) request).equals(sr)) {
        passed = false;
        pw.println("getRequest failed to return the same request object");
      }
    } else {
      passed = false;
      pw.println("constructor returned a null");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void httpRequestWrapperSetRequestTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = true;
    HttpServletRequestWrapper hsrw = new HttpServletRequestWrapper(request);
    if (hsrw != null) {
      try {
        hsrw.setRequest(request);
      } catch (Throwable t) {
        passed = false;
        pw.println("Error: setRequest generated a Throwable");
        t.printStackTrace(pw);
      }
      if (passed) {
        ServletRequest sr = hsrw.getRequest();
        if (!((ServletRequest) request).equals(sr)) {
          passed = false;
          pw.println("getRequest failed to return the same request object");
        }
      }
    } else {
      passed = false;
      pw.println("constructor returned a null");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void httpRequestWrapperSetRequestIllegalArgumentExceptionTest(
      PrintWriter pw, HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    HttpServletRequestWrapper hsrw = new HttpServletRequestWrapper(request);
    if (hsrw != null) {
      try {
        hsrw.setRequest(null);
        passed = false;
        pw.println(
            "Error: an IllegalArgumentException should have been generated");
      } catch (Throwable t) {
        if (t instanceof IllegalArgumentException) {
          passed = true;
        } else {
          passed = false;
          pw.println(
              "Exception thrown, but was not an instance of IllegalArgumentException.");
          pw.println("instead received: " + t.getClass().getName());
        }
      }
    } else {
      passed = false;
      pw.println("constructor returned a null");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getAuthTypeWithoutProtectionTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    String result = request.getAuthType();

    if (result != null) {
      passed = false;
      pw.println(
          "getAuthType() returned a non-null result, even though Servlet is not protected");
      pw.println("Actual result = |" + result + "|");
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);

  }

  public static void getContextPathTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    String expectedResult = request.getHeader("result");
    String result = request.getContextPath();

    if (result != null) {
      if (!result.equals(expectedResult)) {
        passed = false;
        pw.println("getContextPath() returned an incorrect result");
        pw.println("Expected result = " + expectedResult);
        pw.println("Actual result = |" + result + "|");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getContextPath() returned a null result");
    }
    ServletTestUtil.printResult(pw, passed);

  }

  public static void getCookiesTest(PrintWriter pw, HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    boolean passed = true;
    String expectedName = "cookie";
    String expectedValue = "value";
    boolean expectedResultFound = false;
    Cookie cookies[] = null;
    int count = 0;
    int expectedCount = 1;
    boolean gotCookie = false;
    cookies = request.getCookies();
    int len = cookies.length;

    for (int i = 0; i < len; i++) {
      String name = cookies[i].getName();
      String value = cookies[i].getValue();
      if (name.equals(expectedName) && value.equals(expectedValue)) {
        if (!expectedResultFound) {
          count++;
          expectedResultFound = true;
        } else {
          passed = false;
          pw.println("getCookies() method return the same cookie twice");
          pw.println(
              "The cookie already received was " + name + ", value=" + value);
        }
      } else {
        passed = false;
        pw.println("getCookies() method return the wrong cookie");
        pw.println("actual=" + name + ", value=" + value);
      }
    }

    if (count != expectedCount) {
      passed = false;
      pw.println(
          "getCookies() method did not return the correct number of cookies");
      pw.println("Expected count = " + expectedCount);
      pw.println("Actual count = " + count);
    }
    ServletTestUtil.printResult(pw, passed);

  }

  public static void getCookiesNoCookiesTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;
    Cookie[] cook = request.getCookies();

    if (cook != null) {
      passed = false;
      pw.println(
          "getCookies() returning non null value even though client is not sending any cookies.");
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getDateHeaderTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = true;

    pw.println("lower case");
    long expected = 946684801000L;
    String param = "if-modified-since";
    if (!testDateHeader(pw, request, expected, param)) {
      passed = false;
    }

    pw.println("mix case");
    param = "If-Modified-Since";
    if (!testDateHeader(pw, request, expected, param)) {
      passed = false;
    }

    pw.println("upper case");
    param = "IF-MODIFIED-SINCE";
    if (!testDateHeader(pw, request, expected, param)) {
      passed = false;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  private static boolean testDateHeader(PrintWriter pw,
      HttpServletRequest request, long expected, String param) {
    boolean passed = false;

    try {
      long result = request.getDateHeader(param);

      if (result != expected) {
        passed = false;
        pw.println("getDateHeader(" + param + ") returned an incorrect result");
        pw.println("Expected result = " + expected);
        pw.println("Actual result = |" + result + "|");
      } else {
        passed = true;
      }
    } catch (Throwable t) {
      passed = false;
      pw.println("getDateHeader(" + param
          + ") Can't convert the sent header value to Date");
      pw.println("getDateHeader(" + param + ") threw exception");
      t.printStackTrace(pw);
    }
    return passed;
  }

  public static void getDateHeaderNoHeaderTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    long result = request.getDateHeader("If-Modified-Since");

    if (result != -1) {
      passed = false;
      pw.println("getDateHeader didn't return -1 for a NonExistent header");
      pw.println("Actual result = |" + result + "|");
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getDateHeaderIllegalArgumentExceptionTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    String param = "If-Modified-Since";
    try {
      long result = request.getDateHeader("If-Modified-Since");
      passed = false;
      pw.println("getDateHeader(" + param
          + ") did not throw an IllegalArgumentExcpetion");
    } catch (Throwable t) {
      if (t instanceof IllegalArgumentException) {
        passed = true;
      } else {
        passed = false;
        pw.println(
            "Exception thrown, but was not an instance of IllegalArgumentException .");
        pw.println("instead received: " + t.getClass().getName());
      }
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getHeaderTest(PrintWriter pw, HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    boolean passed = true;

    String expected = "Mozilla/4.0";

    pw.println("lower case");
    String param = "user-agent";
    if (!testGetHeader(pw, request, expected, param)) {
      passed = false;
    }

    pw.println("mixed case");
    param = "User-Agent";
    if (!testGetHeader(pw, request, expected, param)) {
      passed = false;
    }

    pw.println("upper case");
    param = "USER-AGENT";
    if (!testGetHeader(pw, request, expected, param)) {
      passed = false;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  // This is a private utility test method
  private static boolean testGetHeader(PrintWriter pw,
      HttpServletRequest request, String expected, String param) {

    boolean passed = false;

    String result = request.getHeader(param);
    if (!expected.equals(result)) {
      passed = false;
      pw.println("getHeader() returned an incorrect result");
      pw.println("Expected result = " + expected);
      pw.println("Actual result = |" + result + "|");
    } else {
      passed = true;
    }
    return passed;
  }

  public static void getHeaderNoHeaderTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    String result = request.getHeader("doesnotexist");
    if (result != null) {
      passed = false;
      pw.println("getHeader didn't return a null for a NonExistent header");
      pw.println("Actual result = |" + result + "|");
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getHeaderNamesTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = true;

    String expected1 = "If-Modified-Since";
    String expected2 = "Cookie";
    String[] expected = { expected1, expected2 };

    Enumeration e = request.getHeaderNames();
    if (e == null) {
      // Some servlet containers won't allow servlets to use
      // this method and in that case it returns null
    } else {
      if (!ServletTestUtil.checkEnumeration(e, expected, false, false)) {
        passed = false;
        ServletTestUtil.printFailureData(pw, e, expected);
      }
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getHeadersTest(PrintWriter pw, HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    boolean passed = true;

    final String[] expected = { "en-us, ga-us" };

    final String[] expected1 = { "en-us", "ga-us" };

    pw.println("lower case");
    String param = "accept-language";
    if (!testGetHeaders(pw, request, expected, param)) {
      passed = testGetHeaders(pw, request, expected1, param);
    }

    pw.println("mixed case");
    param = "Accept-Language";
    if (!testGetHeaders(pw, request, expected, param)) {
      passed = testGetHeaders(pw, request, expected1, param);
    }

    pw.println("upper case");
    param = "ACCEPT-LANGUAGE";
    if (!testGetHeaders(pw, request, expected, param)) {
      passed = testGetHeaders(pw, request, expected1, param);
    }

    ServletTestUtil.printResult(pw, passed);
  }

  private static boolean testGetHeaders(PrintWriter pw,
      HttpServletRequest request, String[] expected, String param) {

    boolean passed = false;

    Enumeration e = request.getHeaders(param);
    if (e != null) {
      if (!ServletTestUtil.checkEnumeration(e, expected)) {
        passed = false;
        ServletTestUtil.printFailureData(pw, e, expected);
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getHeaders(" + param + ") returned a null");
    }
    return passed;
  }

  public static void getHeadersNoHeadersTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;

    Enumeration e = request.getHeaders("doesnotexist");

    if (e.hasMoreElements()) {
      passed = false;
      pw.println("getHeaders(doesnotexist) returned a Non-Empty enumeration");
      pw.print("The headers received were:");
      ServletTestUtil.getAsString(e);
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getIntHeaderTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = true;
    final int expected = 123;

    pw.println("lower case");
    String param = "myintheader";
    if (!testGetIntHeader(pw, request, expected, param)) {
      passed = false;
    }

    pw.println("mixed case");
    param = "MyIntHeader";
    if (!testGetIntHeader(pw, request, expected, param)) {
      passed = false;
    }
    pw.println("upper case");
    param = "MYINTHEADER";
    if (!testGetIntHeader(pw, request, expected, param)) {
      passed = false;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  private static boolean testGetIntHeader(PrintWriter pw,
      HttpServletRequest request, int expected, String param) {

    boolean passed = false;

    try {
      int result = request.getIntHeader(param);

      if (result != expected) {
        passed = false;
        pw.println("getIntHeader(" + param + ") return an incorrect result");
        pw.println("Expected result = " + expected);
        pw.println("Actual result = " + result);
      } else {
        passed = true;
      }
    } catch (Throwable t) {
      passed = false;
      pw.println("getIntHeader(" + param + ") generated an exception");
      t.printStackTrace(pw);
    }
    return passed;
  }

  public static void getIntHeaderNumberFoundExceptionTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    try {

      String param = "MyNonIntHeader";
      int result = request.getIntHeader(param);
      passed = false;
      pw.println("getIntHeader(" + param
          + ") did not throw a NumberFormatException for a NonInt Header");
      pw.println("Actual result = |" + result + "|");
    } catch (Throwable t) {
      if (t instanceof NumberFormatException) {
        passed = true;
      } else {
        passed = false;
        pw.println(
            "Exception thrown, but was not an instance of NumberFormatException .");
        pw.println("instead received: " + t.getClass().getName());
      }
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getIntHeaderNoHeaderTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;
    String param = "doesnotexist";

    try {
      int result = request.getIntHeader(param);

      if (result != -1) {
        passed = false;
        pw.println("getIntHeader(" + param
            + ") did not return a -1 for non-existent header");
        pw.println("Actual result = |" + result + "|");
      } else {
        passed = true;
      }

    } catch (Throwable t) {
      passed = false;
      pw.println("getIntHeader(" + param
          + ") generated an exception instead of returning -1");
      t.printStackTrace(pw);
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getMethod_GETTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;

    String expectedResult = "GET";
    String result = request.getMethod();

    if (result != null) {
      if (!result.equalsIgnoreCase(expectedResult)) {
        passed = false;
        pw.println("getMethod() returned an incorrect result");
        pw.println("Expected result = " + expectedResult);
        pw.println("Actual result = |" + result + "|");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getMethod() returned a null result");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getMethod_HEADTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String expectedResult = "HEAD";
    String result = request.getMethod();
    pw.write("Arbitrary Text");

    if (result != null) {
      if (result.equalsIgnoreCase(expectedResult)) {
        response.addHeader("status", Data.PASSED);
      } else {
        response.addHeader("status", Data.FAILED);
      }
    }

  }

  public static void getMethod_POSTTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;
    String expectedResult = "POST";
    String result = request.getMethod();

    if (result != null) {
      if (!result.equalsIgnoreCase(expectedResult)) {
        passed = false;
        pw.println("getMethod() returned an incorrect result");
        pw.println("Expected result = " + expectedResult);
        pw.println("Actual result = |" + result + "|");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getMethod() returned a null result");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getPathInfoTest(PrintWriter pw, HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    boolean passed = false;
    String expectedResult = "/pathinfostring1/pathinfostring2";
    String result = request.getPathInfo();

    if (result != null) {
      if (!result.equals(expectedResult)) {
        passed = false;
        pw.println("getPathInfo() returned an incorrect result");
        pw.println("Expected result = " + expectedResult);
        pw.println("Actual result = |" + result + "|");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getMethod() returned a null result");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getPathInfoNullTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;
    // shouldn't retrieve any extra path info coz nothing was provided
    String result = request.getPathInfo();

    if (result != null) {
      passed = false;
      pw.println("getPathInfo() returned a non-null result");
      pw.println("Actual result = |" + result + "|");
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getPathTranslatedNullTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    String result = request.getPathTranslated();

    // null will be returned if running out of a jar
    if (result != null) {
      passed = false;
      pw.println("getPathTranslated() returned an incorrect result");
      pw.println("Expected result = null");
      pw.println("Actual result = |" + result + "|");
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getPathTranslatedTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = true;
    ServletContext sc = (ServletContext) request.getAttribute("servletContext");
    String expectedResult = sc.getRealPath(request.getPathInfo());

    String result = request.getPathTranslated();
    // null will be returned if running out of a jar
    if (expectedResult == null) {
      if (result != null) {
        passed = false;
        pw.println("getPathTranslated() returned an incorrect result");
        pw.println("Expected result = Null");
        pw.println("Actual result = |" + result + "|");
      }
    } else if (!expectedResult.equals(result)) {
      passed = false;
      pw.println("getPathTranslated() returned an incorrect result");
      pw.println("Expected result = " + expectedResult);
      pw.println("Actual result = |" + result + "|");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getQueryStringTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    String expected = "qs=value1";
    // we should get a not null value
    String result = request.getQueryString();

    if (result != null) {
      if (!expected.equals(result)) {
        passed = false;
        pw.println("getQueryString() returned an incorrect result");
        pw.println("Expected result = " + expected);
        pw.println("Actual result = |" + result + "|");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getQueryString() returned a null result");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getQueryStringNullTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    String result = request.getQueryString();

    if (result != null) {
      passed = false;
      pw.println(
          " HttpServletRequest.getQueryString() returned a Non-Null result");
      pw.println(" Actual result = |" + result + "|");
    } else {
      passed = true;
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getRequestURITest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;

    String expectedResult = request.getHeader("result") + "/TestServlet";
    String result = request.getRequestURI();
    // not null value expected
    if (result != null) {
      if (!result.equals(expectedResult)) {
        passed = false;
        pw.println("getRequestURI() returned an incorrect result");
        pw.println("Expected result = " + expectedResult);
        pw.println("Actual result = |" + result + "|");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getRequestURI() returned a null result");
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getServletPathTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    String expectedResult = "/TestServlet";
    String result = request.getServletPath();

    if (result != null) {
      if (!result.equals(expectedResult)) {
        passed = false;
        pw.println("getServletPath() returned an incorrect result");
        pw.println("Expected result = " + expectedResult);
        pw.println("Actual result = |" + result + "|");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getServletPath() returned a null result");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getServletPathEmptyStringTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    String expectedResult = "";
    String result = request.getServletPath();

    if (result != null) {
      if (!result.equals(expectedResult)) {
        passed = false;
        pw.println("getServletPath() returned an incorrect result");
        pw.println("Expected result = ''");
        pw.println("Actual result = |" + result + "|");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getServletPath() returned a null result");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getSessionTest(PrintWriter pw, HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    boolean passed = false;
    HttpSession sess = request.getSession();

    if (sess == null) {
      passed = false;
      pw.println("getSession() returned a Null result");
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getSessionTrueSessionTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    HttpSession expected = savedSession;
    HttpSession actual = request.getSession(true);
    if (actual != null) {
      if (!actual.getId().equals(expected.getId())) {
        passed = false;
        pw.println("getSession(true) did not return the correct session id");
        pw.println("expected=" + expected.getId());
        pw.println("actual=" + actual.getId());
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("getSession(true) returned a Null result");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getSessionTrueNoSessionTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    HttpSession sess = request.getSession(true);
    if (sess != null) {
      savedSession = sess;
      savedSessionId = sess.getId();
      passed = true;
    } else {
      passed = false;
      pw.println("getSession(true) returned a Null result");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getSessionFalseTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    HttpSession sess = request.getSession(false);

    if (sess != null) {
      if (sess.isNew()) {
        passed = true;
        pw.println("getSession() did not returned a Null result");
        pw.println("Actual=" + sess.toString());
      } else {
        passed = false;
        pw.println("getSession() did not returned a Null result");
        pw.println("Actual=" + sess.toString());
      }
    } else {
      passed = true;
      pw.println("getSession() returned a Null");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void isRequestedSessionIdFromCookieTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean result = request.isRequestedSessionIdFromCookie();

    pw.println("Actual result = " + result);
  }

  public static void isRequestedSessionIdFromURLTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    boolean expectedResult = false;
    boolean result = request.isRequestedSessionIdFromURL();

    if (result != expectedResult) {
      passed = false;
      pw.println("isRequestedSessionIdFromURL returned incorrect result");
      pw.println("Expected result = " + expectedResult);
      pw.println("Actual result = " + result);
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void isRequestedSessionIdValidTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    boolean expectedResult = false;
    boolean result = request.isRequestedSessionIdValid();

    if (result != expectedResult) {
      passed = false;
      pw.println("isRequestedSessionIdValid() returned incorrect result");
      pw.println("Expected result = " + expectedResult);
      pw.println("Actual result = " + result);

    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getRequestURLTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;
    String expResult1 = null;
    String expResult2 = null;
    String result = request.getRequestURL().toString();

    String scheme = request.getHeader("scheme");
    String path = request.getHeader("servletPath").replace('-', '/');

    expResult1 = scheme + "://";
    expResult2 = path;

    if (!result.startsWith(expResult1) && !result.endsWith(expResult2)) {
      passed = false;
      pw.println("Unexpected result returned from getRequestURL().");
      pw.println("Expected " + expResult1 + "<host.domain:port>" + expResult2);
      pw.println("Received: " + result);
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getRemoteUserTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;
    String result = request.getRemoteUser();
    if (result != null) {
      passed = false;
      pw.println("A non-null value was returned from getQueryString().");
      pw.println("Received: " + result + "'");
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getRequestedSessionIdNullTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;
    String result = request.getRequestedSessionId();
    if (result != null) {
      passed = false;
      pw.println("A non-null value was returned from getRequestedSessionId()");
      pw.println("Received: " + result);
    } else {
      passed = true;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void invalidateSessionId(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    HttpSession sess = request.getSession();
    savedRequestedSessionId = request.getRequestedSessionId();
    sess.invalidate();
    ServletTestUtil.printResult(pw, true);
  }

  public static void getRequestedSessionIdTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    HttpSession sess = request.getSession(true);
    if (sess != null) {
      String actual = request.getRequestedSessionId();
      if (actual != null) {
        if (actual.equals(savedRequestedSessionId)) {
          pw.println(
              "getRequestedSessionId() returned correct requesteSessionId="
                  + savedRequestedSessionId);
          passed = true;
        } else {
          passed = false;
          pw.println(
              "getRequestedSessionId() returned the incorrect request session id");
          pw.println("Correct RequestedSessionId=" + savedRequestedSessionId);
          pw.println("getRequestedSessionId     =" + actual);
        }
      } else {
        passed = false;
        pw.println("getRequestedSessionId() returned a Null result");
      }
    } else {
      passed = false;
      pw.println("getSession(true) returned a Null result");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getRequestedSessionIdTest1(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;

    String actual = request.getRequestedSessionId();
    String expected = request.getParameter("TCKidsetto");
    if (actual != null) {
      if (actual.equals(expected)) {
        passed = true;
        pw.println("Test returned with RequestdSessionId=" + actual);
      } else {
        pw.println(
            "getRequestedSessionId() returned the incorrect request session id");
        pw.println("Correct RequestedSessionId=" + expected);
        pw.println("getRequestedSessionId     =" + actual);
      }
    } else {
      pw.println("getRequestedSessionId() returned a Null result");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getRequestedSessionIdTest2(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean passed = false;

    HttpSession sess = request.getSession(true);
    if (sess != null) {
      String actual = request.getRequestedSessionId();
      String expected = request.getParameter("TCKidsetto");
      if (actual != null) {
        if (actual.equals(expected)) {
          passed = true;
          pw.println("Test returned with RequestdSessionId=" + actual);
        } else {
          pw.println(
              "getRequestedSessionId() returned the incorrect request session id");
          pw.println("Correct RequestedSessionId=" + expected);
          pw.println("getRequestedSessionId     =" + actual);
        }
      } else {
        pw.println("getRequestedSessionId() returned a Null result");
      }
    } else {
      passed = false;
      pw.println("getSession(true) returned a Null result");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void sessionTimeoutTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;

    HttpSession sess = request.getSession();
    // timeouts shortened to spare us time
    if (sess != null) {
      sess.setMaxInactiveInterval(3);

      try {
        Thread.sleep(5000);
      } catch (java.lang.InterruptedException ex) {
        pw.println("Sleep interupted - " + ex.getMessage());
      }

      if (request.getSession(false) != null) {
        passed = true;
        pw.println("Session is still alive after timeout - 90 seconds");
      } else {
        passed = false;
        pw.println("Session is expired after timeout - 90 seconds");
      }
    } else {
      passed = false;
      pw.println("getSession() returned a Null result");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void changeSessionIDTest(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Boolean passed = true;

    try {
      request.changeSessionId();
      passed = false;
      pw.println("Test Failed. ");
      pw.println("Expected ServletException not thrown");
    } catch (IllegalStateException lex) {
      pw.println("Test Passed.");
      pw.println("Expected IllegalStateException thrown: " + lex.getMessage());
    } catch (Exception oex) {
      passed = false;
      pw.println("Test Failed.");
      pw.print("Unexpected Exception thrown: " + oex.getMessage());
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void changeSessionIDTest1(PrintWriter pw,
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Boolean pass = true;
    String attrName_OLD = "OLD";
    String attrName_NEW = "NEW";

    HttpSession session = request.getSession(true);
    String sessionId_old = session.getId();
    String sessionId_new = request.changeSessionId();

    if (sessionId_new.equals(sessionId_old)) {
      pass = false;
      pw.append(
          "HttpServletRequest.changeSessionId didn't return new Session ID,"
              + "it returns the original sesison ID " + sessionId_old);
    }

    if (!((String) request.getSession(false).getAttribute(attrName_OLD))
        .equals(sessionId_old)) {
      pass = false;
      pw.append(
          "Original Session ID does not sync up. Before ChangeSessionId is called: "
              + sessionId_old + " From TCKHttpSessionIDListener "
              + request.getSession(false).getAttribute(attrName_OLD));
    }

    if (((String) request.getSession(false).getAttribute(attrName_NEW))
        .equals(sessionId_old)) {
      pass = false;
      pw.append("Session ID didn't change: " + sessionId_old);
    }

    if (!((String) request.getSession(false).getAttribute(attrName_NEW))
        .equals(sessionId_new)) {
      pass = false;
      pw.append("New Session ID does not sync up. ChangeSessionId returned  "
          + sessionId_new + " TCKHttpSessionIDListener returned "
          + request.getSession(false).getAttribute(attrName_NEW));
    }

    pw.append("Original before changeSessionId is called =" + sessionId_old
        + "=changeSessionId returned=" + sessionId_new
        + "=oroginal from TCKHttpSessionIDListener="
        + request.getSession(false).getAttribute(attrName_OLD)
        + "=new from TCKHttpSessionIDListener="
        + request.getSession(false).getAttribute(attrName_NEW));
    ServletTestUtil.printResult(pw, pass);
  }
  // --------------- END jakarta_servlet_http.HttpServletRequest
  // -------------------
}
