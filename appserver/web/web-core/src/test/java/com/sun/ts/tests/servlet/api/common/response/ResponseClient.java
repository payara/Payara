/*
 * Copyright (c) 2007, 2018 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ts.tests.servlet.api.common.response;

import com.sun.ts.tests.servlet.common.client.AbstractUrlClient;
import com.sun.ts.tests.servlet.common.util.Data;

public class ResponseClient extends AbstractUrlClient {

  public void flushBufferTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "flushBufferTest");
    invoke();
  }

  public void getBufferSizeTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getBufferSizeTest");
    invoke();
  }

  public void getLocaleDefaultTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getLocaleDefaultTest");
    invoke();
  }

  public void getLocaleTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getLocaleTest");
    invoke();
  }

  public void getOutputStreamTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getOutputStreamTest");
    invoke();
  }

  public void getOutputStreamFlushTest() throws Fault {
    String testName = "getOutputStreamFlushTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING,
        "in getOutputStreamFlushTest|" + Data.PASSED);
    invoke();
  }

  public void getOutputStreamIllegalStateExceptionTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getOutputStreamIllegalStateExceptionTest");
    invoke();
  }

  public void getWriterTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getWriterTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "Content-Type: text/html;charset=ISO-8859-1");
    TEST_PROPS.setProperty(EXPECTED_HEADERS, "Content-Type: text/html");
    TEST_PROPS.setProperty(SEARCH_STRING, Data.PASSED);
    invoke();
  }

  public void getWriterFlushTest() throws Fault {
    String testName = "getWriterFlushTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING, "in test|" + Data.PASSED);
    invoke();
  }

  public void getWriterAfterTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getWriterAfterTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "Content-Type: text/html;charset=ISO-8859-1");
    TEST_PROPS.setProperty(EXPECTED_HEADERS,
        "Content-Type: text/html;charset=ISO-8859-7");
    invoke();
  }

  public void getWriterIllegalStateExceptionTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getWriterIllegalStateExceptionTest");
    invoke();
  }

  public void getWriterUnsupportedEncodingExceptionTest() throws Fault {
    String testName = "getWriterUnsupportedEncodingExceptionTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + testName + " HTTP/1.1");
    invoke();
    testName = "checkTestResult";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING, Data.PASSED);
    invoke();
  }

  public void isCommittedTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "isCommittedTest");
    invoke();
  }

  public void resetBufferTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "resetBufferTest");
    invoke();
  }

  public void resetTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "resetTest");
    invoke();
  }

  public void resetTest1() throws Fault {
    TEST_PROPS.setProperty(APITEST, "resetTest1");
    TEST_PROPS.setProperty(UNEXPECTED_HEADERS,
        "Content-Type: application/java-archive; charset=Shift_Jis");
    TEST_PROPS.setProperty(UNEXPECTED_RESPONSE_MATCH, "BigNoNo");
    TEST_PROPS.setProperty(SEARCH_STRING, "YesPlease");
    invoke();
  }

  public void resetIllegalStateExceptionTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "resetIllegalStateExceptionTest");
    invoke();
  }

  public void getCharacterEncodingTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getCharacterEncodingTest");
    invoke();
  }

  public void getCharacterEncodingDefaultTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getCharacterEncodingDefaultTest");
    invoke();
  }

  public void setCharacterEncodingTest() throws Fault {
    String testName = "setCharacterEncodingTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "Content-Type: text/html;charset=ISO-8859-1");
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(EXPECTED_HEADERS,
        "Content-Type: text/html;charset=ISO-8859-7");
    TEST_PROPS.setProperty(SEARCH_STRING, Data.PASSED);
    invoke();
  }

  public void setBufferSizeTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "setBufferSizeTest");
    invoke();
  }

  public void setBufferSizeIllegalStateExceptionTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "setBufferSizeIllegalStateExceptionTest");
    invoke();
  }

  public void setContentLengthTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "setContentLengthTest");
    int lenn = Data.PASSED.length();
    TEST_PROPS.setProperty(EXPECTED_HEADERS, "Content-Length:" + lenn);
    invoke();
  }

  public void getContentTypeTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getContentTypeTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS, "Content-Type:text/html");
    TEST_PROPS.setProperty(EXPECTED_HEADERS,
        "Content-Type:text/html;charset=ISO-8859-1");
    invoke();
  }

  public void getContentType1Test() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getContentType1Test");
    TEST_PROPS.setProperty(EXPECTED_HEADERS, "Content-Type:text/html");
    invoke();
  }

  public void getContentTypeNullTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getContentTypeNullTest");
    invoke();
  }

  public void getContentTypeNull1Test() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getContentTypeNull1Test");
    invoke();
  }

  public void getContentTypeNull2Test() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getContentTypeNull2Test");
    invoke();
  }

  public void setContentTypeTest() throws Fault {
    String testName = "setContentTypeTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(EXPECTED_HEADERS, "Content-Type:text/html");
    TEST_PROPS.setProperty(IGNORE_BODY, "true");
    invoke();
  }

  public void setContentType1Test() throws Fault {
    TEST_PROPS.setProperty(APITEST, "setContentType1Test");
    invoke();
  }

  public void setContentType2Test() throws Fault {
    String testName = "setContentType2Test";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(EXPECTED_HEADERS, "Content-Type:text/html");
    TEST_PROPS.setProperty(UNEXPECTED_HEADERS, "Content-Type:text/plain");
    TEST_PROPS.setProperty(IGNORE_BODY, "true");
    invoke();
  }

  public void setLocaleTest() throws Fault {
    String testName = "setLocaleTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(EXPECTED_HEADERS, "Content-Language:en-US");
    TEST_PROPS.setProperty(IGNORE_BODY, "true");
    invoke();
  }

  public void setLocale1Test() throws Fault {
    String testName = "setLocale1Test";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(EXPECTED_HEADERS,
        "Content-Type:text/html;charset=Shift_Jis");
    TEST_PROPS.setProperty(IGNORE_BODY, "true");
    invoke();
  }
}
