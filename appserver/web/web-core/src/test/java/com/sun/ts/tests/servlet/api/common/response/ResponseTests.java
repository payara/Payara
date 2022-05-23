/*
 * Copyright (c) 2007, 2022 Oracle and/or its affiliates. All rights reserved.
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
 * $URL$ $LastChangedDate$
 */

package com.sun.ts.tests.servlet.api.common.response;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import com.sun.ts.tests.servlet.common.util.Data;
import com.sun.ts.tests.servlet.common.util.ServletTestUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ServletResponseWrapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public class ResponseTests {

  // --------------------------- ServletResponse
  // ---------------------------------

  public static void responseWrapperConstructorTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    PrintWriter pw = response.getWriter();
    ServletResponseWrapper srw = new ServletResponseWrapper(response);
    if (srw != null) {
      ServletTestUtil.printResult(pw, true);
    } else {
      ServletTestUtil.printResult(pw, false);
    }
  }

  public static void responseWrapperGetResponseTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    PrintWriter pw = response.getWriter();
    boolean passed = false;
    ServletResponseWrapper srw = new ServletResponseWrapper(response);
    if (srw != null) {
      ServletResponse sr = srw.getResponse();
      if (!response.equals(sr)) {
        passed = false;
        pw.println("getResponse failed to return the same response object");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("constructor failed");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void responseWrapperSetResponseTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    PrintWriter pw = response.getWriter();
    boolean passed = true;
    ServletResponseWrapper srw = new ServletResponseWrapper(response);
    if (srw != null) {
      try {
        srw.setResponse(response);
      } catch (Throwable t) {
        passed = false;
        pw.println("Error: setResponse generated a Throwable");
        t.printStackTrace(pw);
      }
      if (passed) {
        ServletResponse sr = srw.getResponse();
        if (!response.equals(sr)) {
          passed = false;
          pw.println("getResponse failed to return the same response object");
        }
      }
    } else {
      passed = false;
      pw.println("constructor failed");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void responseWrapperSetResponseIllegalArgumentExceptionTest(
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    PrintWriter pw = response.getWriter();
    boolean passed = false;
    ServletResponseWrapper srw = new ServletResponseWrapper(response);
    if (srw != null) {
      try {
        srw.setResponse(null);
        passed = false;
        pw.println(
            "Error: an IllegalArgumentException should have been generated");
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
    } else {
      passed = false;
      pw.println("constructor failed");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void flushBufferTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    ServletOutputStream sos = null;

    try {
      sos = response.getOutputStream();
      response.setBufferSize(13);
      response.setContentType("text/html");
      ServletTestUtil.printResult(sos, true);

      // after flushing the client should get this
      response.flushBuffer();

      sos.close();
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  public static void getBufferSizeTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    final String IN_TEST = "in test";
    int bSize = IN_TEST.length() * 2;
    boolean passed = false;
    PrintWriter pw = response.getWriter();

    response.setBufferSize(bSize);
    pw.println(IN_TEST);
    response.flushBuffer();
    int result = response.getBufferSize();
    // needs to be greater than or equal to SetBufferSize value or zero .
    if ((result >= bSize) || (result == 0)) {
      passed = true;
    } else {
      passed = false;
      pw.println("ServletRequest.getBufferSize() returned incorrect result");
      pw.println("Expected result -> >= " + bSize + " or 0");
      pw.println("Actual result   -> " + result);
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getOutputStreamFlushTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;
    ServletOutputStream sos = response.getOutputStream();
    sos.println("in getOutputStreamFlushTest");
    sos.flush();
    if (response.isCommitted()) {
      passed = true;
    } else {
      passed = false;
    }
    ServletTestUtil.printResult(sos, passed);
  }

  public static void getOutputStreamTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    ServletOutputStream sos = response.getOutputStream();
    ServletTestUtil.printResult(sos, true);
  }

  public static void getOutputStreamIllegalStateExceptionTest(
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    PrintWriter pw = response.getWriter();
    try {
      response.getOutputStream();
      passed = false;
      pw.println("getOutputStream() did not throw IllegalStateException ");
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

  public static void setCharacterEncodingTest(ServletRequest request,
      ServletResponse response) throws IOException {
    boolean pass = true;
    StringBuilder report = new StringBuilder();
    
    // First need to know the default
    String defaultEncoding = response.getCharacterEncoding();
    
    report.append("Test 1: Direct UTF-8 then null:\n");
    response.setCharacterEncoding("UTF-8");
    if ("UTF-8".equalsIgnoreCase(response.getCharacterEncoding())) {
      report.append(" Set with UTF-8 Pass\n");
    } else {
      pass = false;
      report.append(" Set with UTF-8 Fail\n");
    }
    response.setCharacterEncoding(null);
    if ((defaultEncoding == null && response.getCharacterEncoding() == null) ||
        defaultEncoding != null && defaultEncoding.equalsIgnoreCase(response.getCharacterEncoding())) {
      report.append(" Set with null Pass\n");
    } else {
      pass = false;
      report.append(" Set with null Fail\n");
    }
    response.reset();
    
    report.append("Test 2: Content-Type UTF-8 then null:\n");
    response.setContentType("text/plain; charset=UTF-8");
    if ("UTF-8".equalsIgnoreCase(response.getCharacterEncoding())) {
      report.append(" Set via Content-Type Pass\n");
    } else {
      pass = false;
      report.append(" Set via Content-Type Fail\n");
    }
    response.setCharacterEncoding(null);
    if ((defaultEncoding == null && response.getCharacterEncoding() == null) ||
        defaultEncoding != null && defaultEncoding.equalsIgnoreCase(response.getCharacterEncoding())) {
      report.append(" Set with null Pass\n");
    } else {
      pass = false;
      report.append(" Set with null Fail\n");
    }
    response.reset();
    
    report.append("Test 3: Locale Shift_Jis then null:\n");
    response.setLocale(new Locale("ja"));
    if ("Shift_Jis".equalsIgnoreCase(response.getCharacterEncoding())) {
      report.append(" Set via Locale Pass\n");
    } else {
      pass = false;
      report.append(" Set via Locale Fail\n");
    }
    response.setCharacterEncoding(null);
    if ((defaultEncoding == null && response.getCharacterEncoding() == null) ||
        defaultEncoding != null && defaultEncoding.equalsIgnoreCase(response.getCharacterEncoding())) {
      report.append(" Set with null Pass\n");
    } else {
      pass = false;
      report.append(" Set with null Fail\n");
    }
    response.reset();
    
    report.append("Test 4: Invalid then getWriter():\n");
    response.setCharacterEncoding("does-not-exist");
    if ("does-not-exist".equalsIgnoreCase(response.getCharacterEncoding())) {
      report.append(" Set with invalid Pass\n");
    } else {
      pass = false;
      report.append(" Set with invalid Fail\n");
    }
    try {
      response.getWriter();
      pass = false;
      report.append(" getWriter() did not throw UnsupportedEncodingException Fail\n");
    } catch (UnsupportedEncodingException uee) {
      report.append(" getWriter() throw UnsupportedEncodingException Pass\n");
    }
    response.reset();
    
    report.append("Test 5: Check getContentType():\n");
    final String ENCODING = "ISO-8859-7";
    response.setContentType("text/html");
    response.setCharacterEncoding(ENCODING);
    String type = response.getContentType();

    if (type != null) {
      if ((type.toLowerCase().indexOf("text/html") > -1)
          && (type.toLowerCase().indexOf("charset") > -1)
          && (type.toLowerCase().indexOf("iso-8859-7") > -1)) {
        report.append(" getContentType returns correct type\n");
      } else {
        pass = false;
        report.append(" Expecting text/html; charset=ISO-8859-7");
        report.append(" getContentType returns incorrect type: " + type);
      }
    } else {
      pass = false;
      report.append(" getContentType return null");
    }

    PrintWriter pw = response.getWriter();
    pw.print(report.toString());
    ServletTestUtil.printResult(pw, pass);
  }

  public static void getWriterTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    boolean pass = false;

    PrintWriter pw = response.getWriter();
    response.setContentType("text/html;charset=ISO-8859-7");
    String type = response.getContentType();
    if (type != null) {
      if ((type.toLowerCase().indexOf("text/html") > -1)
          && (type.toLowerCase().indexOf("charset") > -1)
          && (type.toLowerCase().indexOf("iso-8859-1") > -1)) {
        pass = true;
      } else {
        pw.println("Expecting text/html; charset=ISO-8859-1");
        pw.println("getContentType returns incorrect type: " + type);
      }
    } else {
      pw.println("getContentType return null");
    }
    ServletTestUtil.printResult(pw, pass);
  }

  public static void getWriterFlushTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    boolean passed = false;
    PrintWriter pw = response.getWriter();
    response.setContentType("text/html;charset=ISO-8859-7");
    pw.println("in test");
    pw.flush();
    if (response.isCommitted()) {
      passed = true;
    } else {
      passed = false;
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getWriterAfterTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    boolean pass = false;
    response.setContentType("text/xml;charset=ISO-8859-7");
    PrintWriter pw = response.getWriter();
    response.setContentType("text/html;charset=UTF-8");

    String type = response.getContentType();
    if (type != null) {
      if ((type.toLowerCase().indexOf("text/html") > -1)
          && (type.toLowerCase().indexOf("charset") > -1)
          && (type.toLowerCase().indexOf("iso-8859-7") > -1)) {
        pass = true;
      } else {
        pw.println("Expecting text/html; charset=ISO-8859-7");
        pw.println("getContentType returns incorrect type: " + type);
      }
    } else {
      pw.println("getContentType return null");
    }
    ServletTestUtil.printResult(pw, pass);
  }

  public static void getWriterIllegalStateExceptionTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;
    ServletOutputStream sos = response.getOutputStream();

    try {
      PrintWriter pw = response.getWriter();
      passed = false;
      sos.println("getWriter() did not throw IllegalStateException ");
    } catch (Throwable t) {
      if (t instanceof IllegalStateException) {
        passed = true;
      } else {
        passed = false;
        sos.println(
            "Exception thrown, but was not an instance of IllegalStateException.");
        sos.println("instead received: " + t.getClass().getName());
      }
    }
    ServletTestUtil.printResult(sos, passed);
  }

  public static void isCommittedTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;
    ServletOutputStream sos = null;

    try {
      boolean notYet = false;
      sos = response.getOutputStream();
      // set buffer size
      response.setBufferSize(50);

      // commit the response
      if (response.isCommitted() == false)
        notYet = true;

      response.flushBuffer();

      if (notYet && (response.isCommitted() == true)) {
        passed = true;
      } else {
        passed = false;
        sos.println(
            "IsCommitted did not detect that flushBuffer was called already");
      }
    } catch (Throwable t) {
      throw new ServletException(t);
    }
    ServletTestUtil.printResult(sos, true);
  }

  public static void resetTest(ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    ServletOutputStream sos = null;

    try {
      sos = response.getOutputStream();

      // set buffer size
      response.setBufferSize(Data.FAILED.length() * 2);

      // Write some data to the stream
      ServletTestUtil.printResult(sos, false);

      // Reset the response
      response.reset();
      ServletTestUtil.printResult(sos, true);
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  public static void resetTest1(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    ServletOutputStream sos = null;

    String ct = "application/java-archive";
    String enc = "Shift_Jis";

    response.setLocale(new Locale("ja"));
    response.setCharacterEncoding(enc);
    response.setContentType(ct);

    try {
      sos = response.getOutputStream();

      // Write some data to the stream
      sos.println("BigNoNo");
      sos.println("Test FAILED");

      // Reset the response
      response.reset();

      // Write something else to the stream
      sos.println("YesPlease");
      sos.println("Test PASSED");

      response.flushBuffer();
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  public static void resetIllegalStateExceptionTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;
    PrintWriter pw = response.getWriter();
    response.setBufferSize(11);
    pw.println("in test");
    response.flushBuffer();

    try {
      response.reset();
      passed = false;
      pw.println("reset() did not throw IllegalStateException ");
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
    ServletTestUtil.printResult(pw, true);
  }

  public static void getCharacterEncodingDefaultTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    final String ENCODING = "ISO-8859-1";
    boolean passed = false;

    PrintWriter pw = response.getWriter();

    String result = response.getCharacterEncoding();

    if (result != null) {
      if (result.equalsIgnoreCase(ENCODING)) {
        passed = true;
      } else {
        passed = false;
        pw.println("getCharacterEncoding() returned an incorrect result ");
        pw.println("Expected result = " + ENCODING);
        pw.println("Actual result = |" + result + "|");
      }
    } else {
      passed = false;
      pw.println("getCharacterEncoding() returned a null result ");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getCharacterEncodingTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;
    final String ENCODING = "ISO-8859-7";
    response.setCharacterEncoding(ENCODING);

    PrintWriter pw = response.getWriter();

    String result = response.getCharacterEncoding();
    if (result != null) {
      if (result.equalsIgnoreCase(ENCODING)) {
        passed = true;
      } else {
        passed = false;
        pw.println("getCharacterEncoding() returned an incorrect result ");
        pw.println("Expected result = " + ENCODING);
        pw.println("Actual result = |" + result + "|");
      }
    } else {
      passed = false;
      pw.println("getCharacterEncoding() returned a null result ");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void setBufferSizeTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    boolean passed = false;
    PrintWriter pw = response.getWriter();
    response.setBufferSize(1000);
    int result = response.getBufferSize();
    // needs to be greater than or equal to SetBufferSize value or zero
    if ((result >= 1000) || (result == 0)) {
      passed = true;
    } else {
      passed = false;
      pw.println("getBufferSize() returned incorrect result ");
      pw.println("Expected result -> >= 1000 or 0");
      pw.println("Actual  result = |" + result + "|");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void setBufferSizeIllegalStateExceptionTest(
      ServletRequest request, ServletResponse response)
      throws ServletException, IOException {

    ServletOutputStream sos = null;
    boolean passed = false;

    try {
      sos = response.getOutputStream();
      sos.println("in test");
      sos.flush();

      try {
        // should IllegalStateException
        response.setBufferSize(20);
        passed = false;
        sos.println(
            "setBufferSize(20) should have thrown IllegalStateException ");
      } catch (Throwable t) {
        if (t instanceof IllegalStateException) {
          passed = true;
        } else {
          passed = false;
          sos.println(
              "Exception thrown, but was not an instance of IllegalStateException.");
          sos.println("instead received: " + t.getClass().getName());
        }
      }
    } catch (Throwable t) {
      throw new ServletException(t);
    }
    ServletTestUtil.printResult(sos, passed);
  }

  public static void setContentLengthTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    String expected = "text/html";
    response.setContentType(expected);
    ServletOutputStream sos = response.getOutputStream();

    response.setContentLength(Data.PASSED.length());
    sos.print(Data.PASSED);

  }

  public static void setContentTypeTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    response.setContentType("text/html");
  }

  public static void setContentType1Test(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    boolean passed = false;
    String expected = "text/html";
    response.setContentType(expected);

    String actual = response.getContentType();
    PrintWriter pw = response.getWriter();
    if (actual != null) {
      if (actual.toLowerCase().indexOf(expected) >= 0) {
        passed = true;
      } else {
        pw.println(
            "The value returned by getContentType() did not contain the expected result="
                + expected);
        pw.println("actual=" + actual);
      }
    } else {
      pw.println("Null value returned by getContentType() ");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void setContentType2Test(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    String expected = "text/html";
    String unexpected = "text/plain";

    response.setContentType(expected);
    ServletOutputStream sos = response.getOutputStream();
    sos.println(response.getContentType() + "returned by getContentType()");
    sos.flush();

    response.setContentType(unexpected);
    sos.println(response.getContentType() + "returned by getContentType()");
    sos.flush();
  }

  public static void getContentTypeTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;
    PrintWriter pw = response.getWriter();

    String expected = "text/html";
    response.setContentType(expected);

    String actual = response.getContentType();
    if (actual == null) {
      pw.println("null value returned by getContentType()");
    } else if (actual.toLowerCase().indexOf(expected) >= 0) {
      passed = true;
    } else {
      passed = false;
      pw.println(
          "The value returned by getContentType() did not contain the expected result="
              + expected);
      pw.println("actual=" + actual);
    }
    ServletTestUtil.printResult(pw, passed);

  }

  public static void getContentType1Test(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    boolean passed = false;
    String expected = "text/html";
    response.setContentType(expected);
    ServletOutputStream sos = response.getOutputStream();

    String actual = response.getContentType();
    if (actual == null) {
      sos.println("null value returned by getContentType()");
    } else if (actual.toLowerCase().indexOf(expected) >= 0) {
      passed = true;
    } else if (actual.toLowerCase().indexOf("char=") >= 0) {
      sos.println(
          "The value returned by getContentType() contains the unexpected encoding");
      sos.println("actual=" + actual + "   expected=" + expected);
    } else {
      sos.println(
          "The value returned by getContentType() did not contain the expected result="
              + expected);
      sos.println("actual=" + actual);
    }
    ServletTestUtil.printResult(sos, passed);

  }

  public static void getContentTypeNullTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    PrintWriter pw = response.getWriter();
    String actual = response.getContentType();

    if (actual != null) {
      pw.println("Test FAILED.");
      pw.println("getContentType() did not return a null");
      pw.println("actual=" + actual);
    } else {
      pw.println("Test PASSED");
    }
  }

  public static void getContentTypeNull1Test(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    response.setCharacterEncoding("Shift_Jis");
    String actual = response.getContentType();
    PrintWriter pw = response.getWriter();

    if (actual != null) {
      pw.println("Test FAILED.");
      pw.println("getContentType() did not return a null");
      pw.println("actual=" + actual);
    } else {
      pw.println("Test PASSED");
    }
  }

  public static void getContentTypeNull2Test(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    String expected = "Shift_Jis";

    response.setContentType("text/html;charset=Shift_Jis");
    response.setContentType("text/xml");
    String actual_encoding = response.getCharacterEncoding();
    String actual_type = response.getContentType();

    PrintWriter pw = response.getWriter();

    if (!actual_type.replace(" ", "")
        .equalsIgnoreCase("text/xml;charset=Shift_Jis")) {
      pw.println("getContentType() did not return text/xml; charset=Shift_Jis");
      pw.println("actual=" + actual_type);
    } else if (actual_encoding.toLowerCase()
        .indexOf(expected.toLowerCase()) < 0) {
      pw.println("getCharacterEncoding() did not return correct encoding");
      pw.println("actual=" + actual_encoding);
      pw.println("expected=" + expected);
    } else {
      pw.println("Test PASSED");
    }
  }

  public static void setLocaleTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    Locale loc = new Locale("en", "US");
    response.setLocale(loc);
    response.getWriter().println("Arbitrary text");
  }

  public static void setLocale1Test(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    boolean passed = false;
    String expected1 = "text/html";
    String expected2 = "charset=Shift_Jis";
    Locale loc = new Locale("ja");
    response.setLocale(loc);
    response.setContentType("text/html");

    String actual = response.getContentType();
    PrintWriter pw = response.getWriter();

    if (actual == null) {
      pw.println("Null value returned by getContentType()");
    } else if ((actual.toLowerCase().indexOf(expected1) >= 0)
        && (actual.toLowerCase().indexOf(expected2.toLowerCase()) >= 0)) {
      passed = true;
    } else {
      pw.println(
          "The value returned by getContentType() did not contain the expected result="
              + expected1 + ";" + expected2);
      pw.println("actual=" + actual);
    }
    ServletTestUtil.printResult(pw, passed);

  }

  public static void getLocaleDefaultTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    Locale defaultLocale = Locale.getDefault();
    Locale containerLocale = response.getLocale();

    PrintWriter pw = response.getWriter();
    if (defaultLocale.equals(containerLocale)) {
      pw.println("Test PASSED");
    } else {
      pw.println(
          "Test FAILED.  Expected ServletResponse.getLocale() to return the "
              + "default locale of the VM if the locale was not explicily set.");
      pw.println("VM default locale: " + defaultLocale);
      pw.println(
          "Locale returned by ServletResponse.getLocale(): " + containerLocale);
    }

  }

  public static void getLocaleTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    boolean passed = false;
    PrintWriter pw = response.getWriter();

    Locale expectedResult = new Locale("en", "US");
    response.setLocale(expectedResult);
    Locale result = response.getLocale();

    if (result.equals(expectedResult)) {
      passed = true;
    } else {
      passed = false;
      pw.println("getLocale() did not receive the proper locale");
      pw.println("Expected result = " + expectedResult);
      pw.println("Actual result = " + result);
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void resetBufferTest(ServletRequest request,
      ServletResponse response) throws ServletException, IOException {

    ServletOutputStream sos = null;
    try {
      sos = response.getOutputStream();

      // Write some data to the stream
      response.setContentType("text/html");

      ServletTestUtil.printResult(sos, false);
      sos.println("resetBuffer() did not reset the buffer");
      // Reset the response
      response.resetBuffer();
      ServletTestUtil.printResult(sos, true);
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  // --------------------------- END ServletResponse
  // -----------------------------

  // ---------------------------- HttpServletResponse
  // ----------------------------

  public static void httpResponseWrapperConstructorTest(
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    PrintWriter pw = response.getWriter();
    HttpServletResponseWrapper hsrw = new HttpServletResponseWrapper(response);
    if (hsrw != null) {
      ServletTestUtil.printResult(pw, true);
    } else {
      ServletTestUtil.printResult(pw, false);
    }
  }

  public static void httpResponseWrapperGetResponseTest(
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    PrintWriter pw = response.getWriter();
    boolean passed = false;
    HttpServletResponseWrapper srw = new HttpServletResponseWrapper(response);
    if (srw != null) {
      ServletResponse sr = srw.getResponse();
      if (!response.equals(sr)) {
        passed = false;
        pw.println("getResponse failed to return the same response object");
      } else {
        passed = true;
      }
    } else {
      passed = false;
      pw.println("constructor failed");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void httpResponseWrapperSetResponseTest(
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    PrintWriter pw = response.getWriter();
    boolean passed = true;
    HttpServletResponseWrapper srw = new HttpServletResponseWrapper(response);
    if (srw != null) {
      try {
        srw.setResponse(response);
      } catch (Throwable t) {
        passed = false;
        pw.println("Error: setResponse generated a Throwable");
        t.printStackTrace(pw);
      }
      if (passed) {
        ServletResponse sr = srw.getResponse();
        if (!response.equals(sr)) {
          passed = false;
          pw.println("getResponse failed to return the same response object");
        }
      }
    } else {
      passed = false;
      pw.println("constructor failed");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void httpResponseWrapperSetResponseIllegalArgumentExceptionTest(
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    PrintWriter pw = response.getWriter();
    boolean passed = false;
    HttpServletResponseWrapper srw = new HttpServletResponseWrapper(response);
    if (srw != null) {
      try {
        srw.setResponse(null);
        passed = false;
        pw.println(
            "Error: an IllegalArgumentException should have been generated");
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
    } else {
      passed = false;
      pw.println("constructor failed");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void addCookieTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    // check for this in the client side
    response.addCookie(new Cookie("cookie1", "value1"));
    response.addCookie(new Cookie("cookie2", "value2"));
  }

  public static void addDateHeaderTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    long date = 9876543210L;
    response.addDateHeader("DateInfo", date);
  }

  public static void addHeaderTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    response.addHeader("header1", "value1");
    response.addHeader("header1", "value11");
    response.addHeader("header2", "value2");
  }

  public static void addIntHeaderTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    response.addIntHeader("intHeader1", 1);
    response.addIntHeader("intHeader1", 11);
    response.addIntHeader("intHeader2", 2);
  }

  public static void containsHeaderTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    boolean passed = true;
    PrintWriter pw = response.getWriter();
    pw.println("positive case");
    String param = "header";
    response.setHeader(param, "value1"); // set a Header
    boolean expectedResult = true;
    boolean result = response.containsHeader(param);

    if (result != expectedResult) {
      passed = false;
      pw.println("HttpServletResponse.containsHeader(" + param
          + ") returned incorrect results");
      pw.println("Expected result = " + expectedResult);
      pw.println("Actual result = |" + result);
    }

    pw.println("negative case");
    param = "doesnotexist";
    expectedResult = false;
    result = response.containsHeader(param);

    if (result != expectedResult) {
      passed = false;
      pw.println("HttpServletResponse.containsHeader(" + param
          + ") gave incorrect results");
      pw.println("Expected result = " + expectedResult);
      pw.println("Actual result = |" + result);
    }
    ServletTestUtil.printResult(pw, passed);

  }

  public static void sendErrorClearBufferTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    PrintWriter pw = response.getWriter();
    pw.println("THIS TEXT SHOULD NOT APPEAR");
    response.sendError(HttpServletResponse.SC_GONE);
  }

  public static void sendErrorIllegalStateExceptionTest(
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    PrintWriter pw = response.getWriter();
    pw.println("THIS TEXT SHOULD APPEAR");
    response.flushBuffer();
    try {
      response.sendError(HttpServletResponse.SC_GONE);
      passed = false;
      pw.println("IllegalStateException exception should have been thrown");
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

  public static void sendError_StringTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    PrintWriter pw = response.getWriter();
    pw.println("THIS TEXT SHOULD NOT APPEAR");
    response.addHeader("header", "sendError_StringTest");
    response.addCookie(new Cookie("cookie1", "value1"));
    response.sendError(HttpServletResponse.SC_GONE,
        "in sendError_StringTest servlet");
  }

  public static void sendError_StringIllegalStateExceptionTest(
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    PrintWriter pw = response.getWriter();
    pw.println("THIS TEXT SHOULD APPEAR");
    response.flushBuffer();
    try {
      response.sendError(HttpServletResponse.SC_GONE,
          "in sendError_StringIllegalStateExceptionTest servlet");
      ServletTestUtil.printResult(pw, false);
      pw.println("IllegalStateException exception should have been thrown");
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

  public static void sendError_StringErrorPageTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    PrintWriter pw = response.getWriter();
    pw.println("THIS TEXT SHOULD NOT APPEAR");
    response.addHeader("header", "sendError_StringTest");
    response.addCookie(new Cookie("cookie1", "value1"));
    response.sendError(HttpServletResponse.SC_LENGTH_REQUIRED,
        "in sendError_StringErrorPageTest servlet");
  }

  public static void sendRedirectWithoutLeadingSlashTest(
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    response.sendRedirect("RedirectedTest");
  }

  public static void sendRedirectWithLeadingSlashTest(
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    response.sendRedirect("/RedirectedTest");
  }

  public static void sendRedirectIllegalStateExceptionTest(
      HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    boolean passed = false;
    PrintWriter pw = response.getWriter();
    response.setBufferSize(60);
    response.setContentType("text/html");
    pw.println("in sendRedirect_1Test servlet");
    response.flushBuffer();

    try {
      response.sendRedirect("/RedirectedTest");
      passed = false;
      pw.println("IllegalStateException exception should have been thrown");
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

  public static void setDateHeaderTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    long date = 9876543210L;
    response.setDateHeader("DateInfo", date);
  }

  public static void setDateHeaderOverrideTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    long date = 9876543210L;
    response.setDateHeader("DateInfo", date);
    date = 9876544210L;
    response.setDateHeader("DateInfo", date);
  }

  public static void setHeaderTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    response.setHeader("header", "value1");
  }

  public static void setHeaderOverrideTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    response.setHeader("header", "value1");
    response.setHeader("header", "value2");
  }

  public static void setMultiHeaderTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    response.setHeader("header", "value1");
    response.addHeader("header", "value2");
    response.setHeader("header", "value3");
  }

  public static void setIntHeaderTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    response.addIntHeader("intHeader", 1);
    response.setIntHeader("intHeader", 2);
  }

  public static void setStatusTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    response.setStatus(HttpServletResponse.SC_OK);
  }

  public static void setStatusTest1(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  public static void getHeadersTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    PrintWriter pw = response.getWriter();
    Boolean passed = true;
    String name = "TestheadersUnique";
    String[] values = { "first", "second", "third" };

    response.setHeader(name, values[0]);
    response.addHeader(name, values[1]);
    response.addHeader(name, values[2]);

    Collection<String> headers = response.getHeaders(name);

    for (int i = 0; i < 3; i++) {
      if (headers.contains(values[i])) {
        headers.remove(values[i]);
      } else {
        passed = false;
        pw.println("Header value " + values[i] + " is set but not present.");
      }
    }

    if (!headers.isEmpty()) {
      passed = false;
      pw.println("Unexpected header value(s) is present:");
      Iterator left = headers.iterator();
      while (left.hasNext()) {
        pw.println(left.next());
      }
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getHeaderTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    PrintWriter pw = response.getWriter();
    Boolean passed = false;
    String name = "TestheadersUnique";
    String[] values = { "first", "second", "third" };

    response.setHeader(name, values[0]);
    response.addHeader(name, values[1]);
    response.addHeader(name, values[2]);

    String header = response.getHeader(name);

    if (values[0].equals(header)) {
      passed = true;
      pw.println("Expected header value " + values[0] + " is set.");
    } else {
      pw.println("Test FAILED");
      pw.println("Expected header value " + values[0] + " is not set.");
      pw.println("but unexpected header " + header + " present.");
    }
    ServletTestUtil.printResult(pw, passed);
  }

  public static void getHeaderNamesTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    PrintWriter pw = response.getWriter();
    Boolean passed = true;
    String[] names = { "TestheadersUnique", "TestheadersUniqueInt",
        "TestheadersUniqueDate" };
    String[] values = { "first", "second", "third" };
    int[] values1 = { 1, 2, 3 };
    long[] values2 = { 11L, 22L, 33L };

    response.setHeader(names[0], values[0]);
    response.addHeader(names[0], values[1]);
    response.addHeader(names[0], values[2]);

    response.setIntHeader(names[1], values1[0]);
    response.addIntHeader(names[1], values1[1]);
    response.addIntHeader(names[1], values1[2]);

    response.setDateHeader(names[2], values2[0]);
    response.addDateHeader(names[2], values2[1]);
    response.addDateHeader(names[2], values2[2]);

    Collection<String> headers = response.getHeaderNames();

    for (int i = 0; i < 3; i++) {
      if (!headers.contains(names[i])) {
        passed = false;
        pw.println("Header name " + names[i] + " is set but not present.");
      }
    }

    ServletTestUtil.printResult(pw, passed);
  }

  public static void getStatusTest(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    Boolean passed = true;
    PrintWriter pw = response.getWriter();

    int[] status_codes = { HttpServletResponse.SC_ACCEPTED,
        HttpServletResponse.SC_BAD_GATEWAY, HttpServletResponse.SC_BAD_REQUEST,
        HttpServletResponse.SC_CONFLICT, HttpServletResponse.SC_CONTINUE,
        HttpServletResponse.SC_CREATED,
        HttpServletResponse.SC_EXPECTATION_FAILED,
        HttpServletResponse.SC_FORBIDDEN, HttpServletResponse.SC_FOUND,
        HttpServletResponse.SC_GATEWAY_TIMEOUT, HttpServletResponse.SC_GONE,
        HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED,
        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
        HttpServletResponse.SC_LENGTH_REQUIRED,
        HttpServletResponse.SC_METHOD_NOT_ALLOWED,
        HttpServletResponse.SC_MOVED_PERMANENTLY,
        HttpServletResponse.SC_MOVED_TEMPORARILY,
        HttpServletResponse.SC_MULTIPLE_CHOICES,
        HttpServletResponse.SC_NO_CONTENT,
        HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION,
        HttpServletResponse.SC_NOT_ACCEPTABLE, HttpServletResponse.SC_NOT_FOUND,
        HttpServletResponse.SC_NOT_IMPLEMENTED,
        HttpServletResponse.SC_NOT_MODIFIED, HttpServletResponse.SC_OK,
        HttpServletResponse.SC_PARTIAL_CONTENT,
        HttpServletResponse.SC_PAYMENT_REQUIRED,
        HttpServletResponse.SC_PRECONDITION_FAILED,
        HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED,
        HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
        HttpServletResponse.SC_REQUEST_TIMEOUT,
        HttpServletResponse.SC_REQUEST_URI_TOO_LONG,
        HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
        HttpServletResponse.SC_RESET_CONTENT, HttpServletResponse.SC_SEE_OTHER,
        HttpServletResponse.SC_SERVICE_UNAVAILABLE,
        HttpServletResponse.SC_SWITCHING_PROTOCOLS,
        HttpServletResponse.SC_TEMPORARY_REDIRECT,
        HttpServletResponse.SC_UNAUTHORIZED,
        HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
        HttpServletResponse.SC_USE_PROXY };

    for (int i = 0; i < status_codes.length; i++) {
      response.setStatus(status_codes[i]);
      if (response.getStatus() != status_codes[i]) {
        pw.println("Failed to set/getStatus " + status_codes[i]);
        passed = false;
      }
    }
    ServletTestUtil.printResult(pw, passed);
  }
  // -------------------------- END HttpServletResponse
  // --------------------------
}
