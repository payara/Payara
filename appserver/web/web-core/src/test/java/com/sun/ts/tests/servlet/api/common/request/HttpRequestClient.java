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

import com.sun.ts.tests.servlet.common.util.Data;

public class HttpRequestClient extends RequestClient {

  public void getAuthTypeWithoutProtectionTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getAuthTypeWithoutProtectionTest");
    invoke();
  }

  public void getAuthTypeTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getAuthTypeTest");
    invoke();
  }

  public void getContextPathTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getContextPathTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS, "result:" + getContextRoot());
    invoke();
  }

  public void getCookiesTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getCookiesTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "Cookie: cookie=value; Domain=" + _hostname + "; Path=/");
    invoke();
  }

  public void getCookiesNoCookiesTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getCookiesNoCookiesTest");
    invoke();
  }

  public void getDateHeaderTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getDateHeaderTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "If-Modified-Since:Sat, 01 Jan 2000 00:00:01 GMT");
    invoke();
  }

  public void getDateHeaderNoHeaderTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getDateHeaderNoHeaderTest");
    invoke();
  }

  public void getDateHeaderIllegalArgumentExceptionTest() throws Fault {
    TEST_PROPS.setProperty(APITEST,
        "getDateHeaderIllegalArgumentExceptionTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS, "If-Modified-Since:java");
    invoke();
  }

  public void getHeaderTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getHeaderTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS, "User-Agent: Mozilla/4.0");
    invoke();
  }

  public void getHeaderNoHeaderTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getHeaderNoHeaderTest");
    invoke();
  }

  public void getHeaderNamesTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getHeaderNamesTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "If-Modified-Since:Sat, 01 Jan 2000 00:00:01 GMT|Cookie:cookie=value");
    invoke();
  }

  public void getHeaderNamesNoHeaderTest() throws Fault {
    String testName = "getHeaderNamesNoHeaderTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING, Data.PASSED);
    invoke();
  }

  public void getHeadersTest() throws Fault {
    String testName = "getHeadersTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(REQUEST_HEADERS, "Accept-Language:en-us, ga-us");
    TEST_PROPS.setProperty(SEARCH_STRING, Data.PASSED);
    invoke();

    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "Accept-Language:en-us|Accept-Language:ga-us");
    TEST_PROPS.setProperty(SEARCH_STRING, Data.PASSED);
    invoke();

  }

  public void getHeadersNoHeadersTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getHeadersNoHeadersTest");
    invoke();
  }

  public void getIntHeaderTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getIntHeaderTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS, "MyIntHeader:123");
    invoke();
  }

  public void getIntHeaderNumberFoundExceptionTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getIntHeaderNumberFoundExceptionTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS, "MyNonIntHeader:Java");
    invoke();
  }

  public void getIntHeaderNoHeaderTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getIntHeaderNoHeaderTest");
    invoke();
  }

  public void getMethodTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getMethod_GETTest");
    invoke();
    String testName = "getMethod_POSTTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "POST " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    invoke();
    testName = "getMethod_HEADTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "HEAD " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(EXPECTED_HEADERS, "status:" + Data.PASSED);
    TEST_PROPS.setProperty(IGNORE_BODY, "true");
    invoke();
  }

  public void getPathInfoTest() throws Fault {
    String testName = "getPathInfoTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + getServletName()
            + "/pathinfostring1/pathinfostring2?testname=" + testName
            + " HTTP/1.1");
    invoke();
  }

  public void getPathInfoNullTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getPathInfoNullTest");
    invoke();
  }

  public void getPathTranslatedTest() throws Fault {
    String testName = "getPathTranslatedTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + getServletName()
            + "/jakarta_servlet?testname=" + testName + " HTTP/1.1");
    invoke();
  }

  public void getPathTranslatedNullTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getPathTranslatedNullTest");
    invoke();
  }

  public void getQueryStringTest() throws Fault {
    String testName = "getQueryStringTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + "&qs=value1 HTTP/1.1");
    invoke();
  }

  public void getQueryStringNullTest() throws Fault {
    String testName = "getQueryStringNullTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + testName + " HTTP/1.1");
    invoke();
  }

  public void getRequestedSessionIdNullTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getRequestedSessionIdNullTest");
    invoke();
  }

  public void getRequestedSessionIdTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getSessionTrueNoSessionTest");
    TEST_PROPS.setProperty(SAVE_STATE, "true");
    invoke();
    TEST_PROPS.setProperty(APITEST, "invalidateSessionId");
    TEST_PROPS.setProperty(SAVE_STATE, "true");
    TEST_PROPS.setProperty(USE_SAVED_STATE, "true");
    invoke();
    TEST_PROPS.setProperty(APITEST, "getRequestedSessionIdTest");
    TEST_PROPS.setProperty(SAVE_STATE, "true");
    TEST_PROPS.setProperty(USE_SAVED_STATE, "true");
    invoke();
  }

  public void getRequestedSessionIdTest1() throws Fault {
    String testName = "getRequestedSessionIdTest1";
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + getServletName()
            + ";jsessionid=c0o7fszeb1" + "?testname=" + testName
            + "&TCKidsetto=c0o7fszeb1 HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING,
        "Test returned with RequestdSessionId=c0o7fszeb1");
    invoke();
  }

  public void getRequestedSessionIdTest2() throws Fault {
    String testName = "getRequestedSessionIdTest2";
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + getServletName()
            + ";jsessionid=xyzc0o7fszeb1" + "?testname=" + testName
            + "&TCKidsetto=xyzc0o7fszeb1 HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING,
        "Test returned with RequestdSessionId=xyzc0o7fszeb1");
    invoke();
  }

  public void getRemoteUserTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getRemoteUserTest");
    invoke();
  }

  public void getRequestURITest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getRequestURITest");
    TEST_PROPS.setProperty(REQUEST_HEADERS, "result:" + getContextRoot());
    invoke();
  }

  public void getServletPathTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getServletPathTest");
    invoke();
  }

  public void getServletPathEmptyStringTest() throws Fault {
    String testName = "getServletPathEmptyStringTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING, Data.PASSED);
    invoke();
  }

  public void isRequestedSessionIdFromCookieTest() throws Fault {
    String testName = "isRequestedSessionIdFromCookieTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING, "Actual result = false");
    invoke();
  }

  public void isRequestedSessionIdFromCookieTest1() throws Fault {

    TEST_PROPS.setProperty(APITEST, "getSessionTest");
    TEST_PROPS.setProperty(SAVE_STATE, "true");
    TEST_PROPS.setProperty(USE_SAVED_STATE, "true");
    invoke();

    String testName = "isRequestedSessionIdFromCookieTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST, "GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING, "Actual result = true");
    TEST_PROPS.setProperty(USE_SAVED_STATE, "true");
    TEST_PROPS.setProperty(SAVE_STATE, "true");
    invoke();
  }

  public void isRequestedSessionIdFromURLTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "isRequestedSessionIdFromURLTest");
    invoke();
  }

  public void isRequestedSessionIdValidTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "isRequestedSessionIdValidTest");
    invoke();
  }

  public void getSessionTrueTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getSessionTrueNoSessionTest");
    TEST_PROPS.setProperty(SAVE_STATE, "true");
    invoke();
    TEST_PROPS.setProperty(APITEST, "getSessionTrueSessionTest");
    TEST_PROPS.setProperty(USE_SAVED_STATE, "true");
    invoke();
  }

  public void getSessionFalseTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getSessionFalseTest");
    invoke();
  }

  public void getSessionTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getSessionTest");
    invoke();
  }

  public void getRequestURLTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getRequestURLTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "scheme:http|server:" + _hostname + "|port:" + _port + "|servletpath:"
            + "-" + getContextRoot().substring(1, getContextRoot().length())
            + "-" + getServletName());
    invoke();
  }

  public void sessionTimeoutTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "sessionTimeoutTest");
    invoke();
  }

  public void changeSessionIDTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "changeSessionIDTest");
    invoke();
  }

  public void changeSessionIDTest1() throws Fault {
    TEST_PROPS.setProperty(APITEST, "changeSessionIDTest1");
    invoke();
  }
}
