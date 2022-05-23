/*
 * Copyright (c) 2007, 2021 Oracle and/or its affiliates and others.
 * All rights reserved.
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

package com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletrequest;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.httpclient.Header;

import com.sun.javatest.Status;
import com.sun.ts.tests.common.webclient.http.HttpRequest;
import com.sun.ts.tests.common.webclient.http.HttpResponse;
import com.sun.ts.tests.servlet.api.common.request.HttpRequestClient;
import com.sun.ts.tests.servlet.common.util.Data;

public class URLClient extends HttpRequestClient {

  private static final String CONTEXT_ROOT = "/servlet_jsh_httpservletrequest_web";

  /**
   * Entry point for different-VM execution. It should delegate to method
   * run(String[], PrintWriter, PrintWriter), and this method should not contain
   * any test configuration.
   */
  public static void main(String[] args) {
    URLClient theTests = new URLClient();
    Status s = theTests.run(args, new PrintWriter(System.out),
        new PrintWriter(System.err));
    s.exit();
  }

  /**
   * Entry point for same-VM execution. In different-VM execution, the main
   * method delegates to this method.
   */
  public Status run(String args[], PrintWriter out, PrintWriter err) {

    setServletName("TestServlet");
    setContextRoot(CONTEXT_ROOT);

    return super.run(args, out, err);
  }

  /*
   * @class.setup_props: webServerHost; webServerPort; ts_home;
   *
   */

  /* Run test */

  // ------------------------------- ServletRequest
  // ------------------------------
  /*
   * @testName: getAttributeNamesTest
   * 
   * @assertion_ids: Servlet:JAVADOC:573
   * 
   * @test_Strategy: Servlet verifies attributes
   *
   */
  public void getAttributeNamesTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getAttributeNamesTest");
    invoke();
    TEST_PROPS.setProperty(APITEST, "getAttributeNamesEmptyEnumTest");
    invoke();
  }

  /*
   * @testName: getAttributeTest
   * 
   * @assertion_ids: Servlet:JAVADOC:572
   * 
   * @test_Strategy: Servlet verifies attribute
   *
   */
  public void getAttributeTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getAttributeTest");
    invoke();
    TEST_PROPS.setProperty(APITEST, "getAttributeDoesNotExistTest");
    invoke();
  }

  /*
   * @testName: getCharacterEncodingTest
   * 
   * @assertion_ids: Servlet:JAVADOC:574
   * 
   * @test_Strategy: Servlet verifies encoding
   */
  public void getCharacterEncodingTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getCharacterEncodingTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "Content-Type:text/plain; charset=ISO-8859-1");
    invoke();
    TEST_PROPS.setProperty(APITEST, "getCharacterEncodingNullTest");
    invoke();
  }

  /*
   * @testName: getContentLengthTest
   * 
   * @assertion_ids: Servlet:JAVADOC:575
   * 
   * @test_Strategy: Servlet compares this length to the actual length of the
   * content body read in using getInputStream
   *
   */

  /*
   * @testName: getContentTypeTest
   * 
   * @assertion_ids: Servlet:JAVADOC:576; Servlet:SPEC:34;
   * 
   * @test_Strategy: Client sets the content type and servlet reads it.
   *
   */

  /*
   * @testName: getInputStreamTest
   * 
   * @assertion_ids: Servlet:JAVADOC:577
   * 
   * @test_Strategy: Servlet tries to read the input stream.
   */

  /*
   * @testName: getInputStreamIllegalStateExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:579
   * 
   * @test_Strategy: Servlet gets a Reader object using
   * ServletRequest.getReader() then tries to get the inputStream Object
   *
   */

  /*
   * @testName: getLocaleTest
   * 
   * @assertion_ids: Servlet:JAVADOC:580
   * 
   * @test_Strategy: Servlet sends back locale to client.
   */
  public void getLocaleTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getLocaleTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS, "Accept-Language:en-US");
    invoke();
    TEST_PROPS.setProperty(APITEST, "getLocaleDefaultTest");
    invoke();
  }

  /*
   * @testName: getLocalesTest
   * 
   * @assertion_ids: Servlet:JAVADOC:581
   * 
   * @test_Strategy: Servlet sends back locale(s) to client.
   */
  public void getLocalesTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getLocalesTest");
    TEST_PROPS.setProperty(REQUEST_HEADERS, "Accept-Language:en-US,en-GB");
    invoke();
    TEST_PROPS.setProperty(APITEST, "getLocalesDefaultTest");
    invoke();
  }

  /*
   * @testName: getParameterMapTest
   * 
   * @assertion_ids: Servlet:JAVADOC:583
   * 
   * @test_Strategy: Client sets several parameters and the servlet attempts to
   * access them.
   */

  /*
   * @testName: getParameterNamesTest
   * 
   * @assertion_ids: Servlet:JAVADOC:584
   * 
   * @test_Strategy: Servlet attempts to access parameters.
   */
  public void getParameterNamesTest() throws Fault {
    String testName = "getParameterNamesTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + getServletName() + "?testname="
            + testName + "&parameter1=value1&parameter2=value2 HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING, Data.PASSED);
    invoke();
    testName = "getParameterNamesEmptyEnumTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + testName + " HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING, Data.PASSED);
    invoke();
  }

  /*
   * @testName: getParameterTest
   * 
   * @assertion_ids: Servlet:JAVADOC:582
   * 
   * @test_Strategy: Client sets a parameter and servlet retrieves it.
   */
  public void getParameterTest() throws Fault {
    String testName = "getParameterTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + getServletName() + "?testname="
            + testName + "&parameter1=value1 HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING, Data.PASSED);
    invoke();
    TEST_PROPS.setProperty(APITEST, "getParameterDoesNotExistTest");
    invoke();
  }

  /*
   * @testName: getParameterValuesTest
   * 
   * @assertion_ids: Servlet:JAVADOC:585
   * 
   * @test_Strategy: Servlet verifies values
   */
  public void getParameterValuesTest() throws Fault {
    String testName = "getParameterValuesTest";
    TEST_PROPS.setProperty(TEST_NAME, testName);
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/" + getServletName() + "?testname="
            + testName + "&Names=value1&Names=value2 HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING, Data.PASSED);
    invoke();
    TEST_PROPS.setProperty(APITEST, "getParameterValuesDoesNotExistTest");
    invoke();
  }

  /*
   * @testName: getProtocolTest
   * 
   * @assertion_ids: Servlet:JAVADOC:586
   * 
   * @test_Strategy: Servlet verifies the protocol used by the client
   */

  /*
   * @testName: getReaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:587
   * 
   * @test_Strategy: Client sets some content and servlet reads the content
   */

  /*
   * @testName: getReaderIllegalStateExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:590
   * 
   * @test_Strategy: Servlet gets an InputStream Object then tries to get a
   * Reader Object.
   */

  /*
   * @testName: getReaderUnsupportedEncodingExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:589
   * 
   * @test_Strategy: Client sets some content but with an invalid encoding,
   * servlet tries to read content.
   */

  /*
   * @testName: getRemoteAddrTest
   * 
   * @assertion_ids: Servlet:JAVADOC:592
   * 
   * @test_Strategy: Servlet reads and verifies where the request originated
   */

  /*
   * @testName: getLocalAddrTest
   * 
   * @assertion_ids: Servlet:JAVADOC:719
   * 
   * @test_Strategy: Servlet reads and verifies where the request originated
   */

  /*
   * @testName: getRemoteHostTest
   * 
   * @assertion_ids: Servlet:JAVADOC:593
   * 
   * @test_Strategy: Servlet reads and verifies where the request originated
   */

  /*
   * @testName: getRequestDispatcherTest
   * 
   * @assertion_ids: Servlet:JAVADOC:594
   * 
   * @test_Strategy: Servlet tries to get a dispatcher
   */

  /*
   * @testName: getSchemeTest
   * 
   * @assertion_ids: Servlet:JAVADOC:595
   * 
   * @test_Strategy: Servlet verifies the scheme of the url used in the request
   */

  /*
   * @testName: getServerNameTest
   * 
   * @assertion_ids: Servlet:JAVADOC:596
   * 
   * @test_Strategy: Servlet verifies the destination of the request
   */

  /*
   * @testName: getServerPortTest
   * 
   * @assertion_ids: Servlet:JAVADOC:597
   * 
   * @test_Strategy: Servlet verifies the destination port of the request
   */

  /*
   * @testName: isSecureTest
   * 
   * @assertion_ids: Servlet:JAVADOC:598
   * 
   * @test_Strategy: Servlet verifies the isSecure method for the non-secure
   * case.
   */

  /*
   * @testName: removeAttributeTest
   * 
   * @assertion_ids: Servlet:JAVADOC:599
   * 
   * @test_Strategy: Servlet adds then removes an attribute, then verifies it
   * was removed.
   */

  /*
   * @testName: setAttributeTest
   * 
   * @assertion_ids: Servlet:JAVADOC:600
   * 
   * @test_Strategy: Servlet adds an attribute, then verifies it was added
   */

  /*
   * @testName: setCharacterEncodingTest
   * 
   * @assertion_ids: Servlet:JAVADOC:601
   * 
   * @test_Strategy: Servlet sets a new encoding and tries to retrieve it.
   */

  /*
   * @testName: setCharacterEncodingTest1
   * 
   * @assertion_ids: Servlet:JAVADOC:601; Servlet:JAVADOC:574; Servlet:SPEC:28;
   * Servlet:SPEC:213;
   * 
   * @test_Strategy: HttpServletRequest calls getReader()first; then sets a new
   * encoding and tries to retrieve it. verifies that the new encoding is
   * ignored.
   */

  /*
   * @testName: setCharacterEncodingUnsupportedEncodingExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:602
   * 
   * @test_Strategy: Servlet tries to set an invalid encoding.
   *
   */

  // ---------------------------- END ServletRequest
  // -----------------------------

  // ---------------------------- HttpServletRequest
  // -----------------------------

  /*
   * @testName: getAuthTypeWithoutProtectionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:530
   * 
   * @test_Strategy: Servlet verifies correct result
   */

  /*
   * @testName: getContextPathTest
   * 
   * @assertion_ids: Servlet:JAVADOC:550
   * 
   * @test_Strategy: Client sets header and servlet verifies the result
   */

  /*
   * @testName: getCookiesNoCookiesTest
   * 
   * @assertion_ids: Servlet:JAVADOC:532
   * 
   * @test_Strategy: Servlet tries to get a cookie when none exist
   */

  /*
   * @testName: getCookiesTest
   * 
   * @assertion_ids: Servlet:JAVADOC:531
   * 
   * @test_Strategy:Client sets a cookie and servlet tries to read it
   */

  /*
   * @testName: getDateHeaderIllegalArgumentExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:535
   * 
   * @test_Strategy: Client set invalid date value, servlet tries to read it.
   */

  /*
   * @testName: getDateHeaderNoHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:534
   * 
   * @test_Strategy: Servlet tries to get a dateHeader when none exist
   */

  /*
   * @testName: getDateHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:533
   * 
   * @test_Strategy: client sets a dateheader and servlet tries to read it.
   */

  /*
   * @testName: getHeaderNamesTest
   * 
   * @assertion_ids: Servlet:JAVADOC:540
   * 
   * @test_Strategy: Client sets some headers and servlet tries to read them.
   */

  /*
   * @testName: getHeaderNoHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:537
   * 
   * @test_Strategy: Servlet tries to read a header when none exist
   */

  /*
   * @testName: getHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:536
   * 
   * @test_Strategy: Client sets a header and servlet tries to read it.
   */

  /*
   * @testName: getHeadersNoHeadersTest
   * 
   * @assertion_ids: Servlet:JAVADOC:539
   * 
   * @test_Strategy: Servlet tries to get all the headers when none have been
   * added
   */

  /*
   * @testName: getHeadersTest
   * 
   * @assertion_ids: Servlet:JAVADOC:538
   * 
   * @test_Strategy: Client sets some headers and servlet tries to read them
   */

  /*
   * @testName: getIntHeaderNoHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:543
   * 
   * @test_Strategy: Servlet tries to read a header when none exist.
   */

  /*
   * @testName: getIntHeaderNumberFoundExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:544
   * 
   * @test_Strategy: Client sets an invalid header and servlet tries to read it.
   */

  /*
   * @testName: getIntHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:542
   * 
   * @test_Strategy: Client sets a header and servlet reads it
   */

  /*
   * @testName: getMethodTest
   * 
   * @assertion_ids: Servlet:JAVADOC:545
   * 
   * @test_Strategy: Client makes 3 calls using GET/POST/HEAD
   */

  /*
   * @testName: getPathInfoNullTest
   * 
   * @assertion_ids: Servlet:JAVADOC:547
   * 
   * @test_Strategy:
   */

  /*
   * @testName: getPathInfoTest
   * 
   * @assertion_ids: Servlet:JAVADOC:546; Servlet:SPEC:25;
   * 
   * @test_Strategy: Servlet verifies path info
   */

  /*
   * @testName: getPathTranslatedNullTest
   * 
   * @assertion_ids: Servlet:JAVADOC:549
   * 
   * @test_Strategy: Servlet verifies result when there is no path info
   */

  /*
   * @testName: getPathTranslatedTest
   * 
   * @assertion_ids: Servlet:JAVADOC:548
   * 
   * @test_Strategy: client sets extra path info and servlet verifies it
   */

  /*
   * @testName: getQueryStringNullTest
   * 
   * @assertion_ids: Servlet:JAVADOC:553
   * 
   * @test_Strategy: Servlet verifies result when no query string exists
   */

  /*
   * @testName: getQueryStringTest
   * 
   * @assertion_ids: Servlet:JAVADOC:552
   * 
   * @test_Strategy: Client sets query string and servlet verifies it
   */

  /*
   * @testName: getRemoteUserTest
   * 
   * @assertion_ids: Servlet:JAVADOC:554
   * 
   * @test_Strategy: Servlet verifies the result of a non-authed user
   */

  /*
   * @testName: getRequestURITest
   * 
   * @assertion_ids: Servlet:JAVADOC:561
   * 
   * @test_Strategy: Servlet verifies URI data
   */

  /*
   * @testName: getRequestURLTest
   * 
   * @assertion_ids: Servlet:JAVADOC:562
   * 
   * @test_Strategy: Servlet verifies URL info
   */

  /*
   * @testName: getRequestedSessionIdNullTest
   * 
   * @assertion_ids: Servlet:JAVADOC:560
   * 
   * @test_Strategy: Servlet verifies null result
   */

  /*
   * @testName: getServletPathEmptyStringTest
   * 
   * @assertion_ids: Servlet:JAVADOC:563; Servlet:SPEC:23;
   * 
   * @test_Strategy: Servlet verifies empty string
   */

  /*
   * @testName: getServletPathTest
   * 
   * @assertion_ids: Servlet:JAVADOC:564; Servlet:SPEC:24;
   * 
   * @test_Strategy: Servlet verifies path info
   */

  /*
   * @testName: getSessionTrueTest
   * 
   * @assertion_ids: Servlet:JAVADOC:565
   * 
   * @test_Strategy: Servlet verifies getSession(true) call
   */

  /*
   * @testName: getSessionFalseTest
   * 
   * @assertion_ids: Servlet:JAVADOC:566
   * 
   * @test_Strategy: Servlet verifies getSession(false) call
   */

  /*
   * @testName: getSessionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:567
   * 
   * @test_Strategy: Servlet verifies getSession() call
   */

  /*
   * @testName: isRequestedSessionIdFromCookieTest
   * 
   * @assertion_ids: Servlet:JAVADOC:569
   * 
   * @test_Strategy: Servlet verifies correct result
   */

  /*
   * @testName: isRequestedSessionIdFromURLTest
   * 
   * @assertion_ids: Servlet:JAVADOC:570
   * 
   * @test_Strategy: Servlet verifies correct result
   */

  /*
   * @testName: isRequestedSessionIdValidTest
   * 
   * @assertion_ids: Servlet:JAVADOC:568; Servlet:SPEC:211;
   * 
   * @test_Strategy: Client sends request without session ID; Verifies
   * isRequestedSessionIdValid() returns false;
   */

  /*
   * @testName: getRequestedSessionIdTest1
   * 
   * @assertion_ids: Servlet:JAVADOC:559;
   * 
   * @test_Strategy: Client sends request with a session ID; Verifies
   * getRequestedSessionId() returns the same;
   */

  /*
   * @testName: getRequestedSessionIdTest2
   * 
   * @assertion_ids: Servlet:JAVADOC:559;
   * 
   * @test_Strategy: Client sends request to a servlet with a sesion ID; Servlet
   * start a sesison; Verifies getRequestedSessionId() returns the same;
   */

  /*
   * @testName: sessionTimeoutTest
   * 
   * @assertion_ids: Servlet:SPEC:67;
   * 
   * @test_Strategy: First set a HttpSession's timeout to 60 seconds; then sleep
   * 90 seconds in servlet; verify that the session is still valid after.
   */

  /*
   * @testName: getLocalPortTest
   * 
   * @assertion_ids: Servlet:JAVADOC:630;
   * 
   * @test_Strategy: Send an HttpServletRequest to server; Verify that
   * getLocalPort();
   */

  /*
   * @testName: getServletContextTest
   * 
   * @assertion_ids:
   * 
   * @test_Strategy: Send an HttpServletRequest to server; Verify that
   * getServletContext return the same as stored in ServletConfig
   */

  public void getServletContextTest() throws Fault {
    TEST_PROPS.setProperty(REQUEST,
        "GET " + getContextRoot() + "/getServletContextTest HTTP/1.1");
    TEST_PROPS.setProperty(UNEXPECTED_RESPONSE_MATCH, "Test FAILED");
    TEST_PROPS.setProperty(STATUS_CODE, OK);
    TEST_PROPS.setProperty(SEARCH_STRING, "Test PASSED");
    invoke();
  }

  /*
   * @testName: doHeadTest
   * 
   * @assertion_ids:
   * 
   * @test_Strategy: Perform a GET request and a HEAD request for the same
   * resource and confirm that a) HEAD response has no body and b) the header
   * values are the same.
   */
  public void doHeadTest() throws Fault {
    HttpRequest requestGet = new HttpRequest("GET " + getContextRoot() + "/doHeadTest HTTP/1.1", _hostname, _port);
    HttpRequest requestHead = new HttpRequest("HEAD " + getContextRoot() + "/doHeadTest HTTP/1.1", _hostname, _port);
    
    try {
      HttpResponse responseGet = requestGet.execute();
      HttpResponse responseHead = requestHead.execute();

      // Validate the response bodies
      String responseBodyGet = responseGet.getResponseBodyAsString(); 
      if (responseBodyGet == null || responseBodyGet.length() == 0) {
        throw new Fault("GET request did not include a response body");
      }
      InputStream responseBodyHead = responseHead.getResponseBodyAsRawStream(); 
      if (responseBodyHead != null) {
        throw new Fault("HEAD request included a response body");
      }

      // Validate the response headers
      Set<Header> headersToMatch = new HashSet<>();
      
      Header[] headersGet = responseGet.getResponseHeaders();
      for (Header header : headersGet) {
        switch (header.getName().toLowerCase(Locale.ENGLISH)) {
        case "date":
          // Ignore date header as it will change between requests
          break;
        default:
          headersToMatch.add(header);
        }
      }
      
      Header[] headersHead = responseHead.getResponseHeaders();
      for (Header header : headersHead) {
        if (header.getName().toLowerCase().equals("date")) {
          // Skip date header
          continue;
        }
        if (!headersToMatch.remove(header)) {
          throw new Fault("HEAD request contained header that was not present for GET: " + header);
        }
      }
      
      if (headersToMatch.size() > 0) {
        throw new Fault("HEAD request did not contain header that was present for GET:" + headersToMatch.iterator().next());
      }
    } catch (Throwable t) {
      throw new Fault("Exception occurred:" + t, t);
    }
  }
  
  // -------------------------- END HttpServletRequest
  // ---------------------------
}
