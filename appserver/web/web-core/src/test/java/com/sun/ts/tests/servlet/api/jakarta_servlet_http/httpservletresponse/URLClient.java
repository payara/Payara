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

package com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletresponse;

import java.io.PrintWriter;

import com.sun.javatest.Status;
import com.sun.ts.tests.servlet.api.common.response.HttpResponseClient;

public class URLClient extends HttpResponseClient {

  private static final String CONTEXT_ROOT = "/servlet_jsh_httpservletresponse_web";

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

  // --------------------------- ServletResponse
  // ----------------------------------
  /*
   * @testName: flushBufferTest
   * 
   * @assertion_ids: Servlet:JAVADOC:603
   * 
   * @test_Strategy: Servlet writes data in the buffer and flushes it
   */

  /*
   * @testName: getBufferSizeTest
   * 
   * @assertion_ids: Servlet:JAVADOC:605
   * 
   * @test_Strategy: Servlet flushes buffer and verifies the size of the buffer
   */

  /*
   * @testName: getLocaleDefaultTest
   * 
   * @assertion_ids: Servlet:JAVADOC:625;
   * 
   * @test_Strategy: Validate that getLocale() will return the default locale of
   * the VM that the container is running in when setLocale() is not called.
   */

  /*
   * @testName: getLocaleTest
   * 
   * @assertion_ids: Servlet:JAVADOC:608
   * 
   * @test_Strategy: Servlet set Locale and then verifies it
   *
   */

  /*
   * @testName: getOutputStreamTest
   * 
   * @assertion_ids: Servlet:JAVADOC:609
   * 
   * @test_Strategy: Servlet gets an output stream and writes to it.
   */

  /*
   * @testName: getOutputStreamIllegalStateExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:611
   * 
   * @test_Strategy: Servlet tries to get an stream object after calling
   * getWriter
   */

  /*
   * @testName: getWriterTest
   * 
   * @assertion_ids: Servlet:JAVADOC:612; Servlet:JAVADOC:151.2;
   * Servlet:JAVADOC:151.3;
   * 
   * @test_Strategy: Servlet gets a Writer object and writes data; sets content
   * type by calling ServletResponse.setContentType; Verify that content type
   * gets set, not encoding
   */

  /*
   * @testName: getWriterIllegalStateExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:615
   * 
   * @test_Strategy: Servlet tries to get a Writer object after calling
   * getOutputStream
   */

  /*
   * @testName: isCommittedTest
   * 
   * @assertion_ids: Servlet:JAVADOC:616
   * 
   * @test_Strategy: Servlet checks before and after response is flushed
   *
   */

  /*
   * @testName: resetBufferTest
   * 
   * @assertion_ids: Servlet:JAVADOC:619; Servlet:SPEC:31;
   * 
   * @test_Strategy: Servlet writes data to the response, resets the buffer and
   * then writes new data
   */

  /*
   * @testName: resetTest
   * 
   * @assertion_ids: Servlet:JAVADOC:617; Servlet:SPEC:31;
   * 
   * @test_Strategy: Servlet writes data to the response, does a reset, then
   * writes new data
   */

  /*
   * @testName: resetTest1
   * 
   * @assertion_ids: Servlet:JAVADOC:617; Servlet:SPEC:31;
   * 
   * @test_Strategy: Servlet writes data to the response, set the Headers, does
   * a reset, then writes new data, set the new Header
   */

  /*
   * @testName: resetIllegalStateExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:618; Servlet:SPEC:31;
   * 
   * @test_Strategy: Servlet writes data, flushes the buffer then tries to do a
   * reset
   */

  /*
   * @testName: getCharacterEncodingDefaultTest
   * 
   * @assertion_ids: Servlet:JAVADOC:606
   * 
   * @test_Strategy: Servlet checks for the default encoding
   */

  /*
   * @testName: getCharacterEncodingTest
   * 
   * @assertion_ids: Servlet:JAVADOC:606
   * 
   * @test_Strategy: Servlet sets encoding then checks it.
   */

  /*
   * @testName: setCharacterEncodingTest
   * 
   * @assertion_ids: Servlet:JAVADOC:622; Servlet:JAVADOC:151.3;
   * 
   * @test_Strategy: Servlet set encoding by calling
   * ServletResponse.setCharcaterEncoding; client verifies it is set
   */

  /*
   * @testName: setBufferSizeTest
   * 
   * @assertion_ids: Servlet:JAVADOC:620
   * 
   * @test_Strategy: Servlet sets the buffer size then verifies it was set
   */

  /*
   * @testName: setBufferSizeIllegalStateExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:621
   * 
   * @test_Strategy: Servlet writes data and flushes buffer then tries to get
   * the buffer size
   */

  /*
   * @testName: setContentLengthTest
   * 
   * @assertion_ids: Servlet:JAVADOC:623
   * 
   * @test_Strategy: Servlet sets the content length
   */

  /*
   * @testName: getContentTypeTest
   * 
   * @assertion_ids: Servlet:JAVADOC:607; Servlet:SPEC:34;
   * 
   * @test_Strategy: Servlet verifies the content type
   */
  public void getContentTypeTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getContentTypeTest");
    invoke();
    TEST_PROPS.setProperty(REQUEST,
        "GET /servlet_jsh_httpservletresponse_web/GetContentTypeNullTestServlet HTTP/1.1");
    TEST_PROPS.setProperty(SEARCH_STRING, "Test PASSED");
    invoke();
  }

  /*
   * @testName: setContentTypeTest
   * 
   * @assertion_ids: Servlet:JAVADOC:624; Servlet:JAVADOC:151.3;
   * Servlet:SPEC:34;
   * 
   * @test_Strategy: Servlet sets the content type; Verify Content-Type is set
   * in Client
   */

  /*
   * @testName: setContentType1Test
   * 
   * @assertion_ids: Servlet:JAVADOC:151; Servlet:SPEC:34;
   * 
   * @test_Strategy: Servlet sets the content-type, and verifies it with
   * getContentType()
   */

  /*
   * @testName: setContentType2Test
   * 
   * @assertion_ids: Servlet:JAVADOC:151.2; Servlet:JAVADOC:151.3;
   * Servlet:SPEC:34;
   * 
   * @test_Strategy: Servlet sets the content-type, Commit the response; Set the
   * content-type again Verifies that content-type is set the first time The
   * second setting is ignored.
   */

  /*
   * @testName: setLocaleTest
   * 
   * @assertion_ids: Servlet:JAVADOC:625
   * 
   * @test_Strategy: Servlet sets the Locale
   */

  // ---------------------------- END ServletResponse
  // -----------------------------

  // ------------------------ HttpServletResponse
  // ---------------------------------

  /*
   * @testName: addCookieTest
   * 
   * @assertion_ids: Servlet:JAVADOC:502
   * 
   * @test_Strategy: Servlet adds 2 cookies, client verifies them
   */

  /*
   * @testName: addDateHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:522
   * 
   * @test_Strategy: Servlet adds a date header and client verifies it
   */

  /*
   * @testName: addHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:525
   * 
   * @test_Strategy: Servlet adds 2 headers with 3 values and client verifies
   * them
   */

  /*
   * @testName: addIntHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:527
   * 
   * @test_Strategy: Servlet adds 2 int headers with 3 values and client
   * verifies them
   */

  /*
   * @testName: containsHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:503
   * 
   * @test_Strategy: Servlet sets a header and verifies it exists, then the
   * servlet tries to verify that a header does not exist.
   */

  /*
   * @testName: sendErrorIllegalStateExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:514
   * 
   * @test_Strategy: Servlet adds a header, a cookie, and content, then flushes
   * the buffer. Servlet verifies exception is generated
   */

  /*
   * @testName: sendError_StringIllegalStateExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:511
   * 
   * @test_Strategy: Servlet adds a header, a cookie, and content, then flushes
   * the buffer. Servlet verifies exception is generated
   */

  /*
   * @testName: sendErrorClearBufferTest
   * 
   * @assertion_ids: Servlet:JAVADOC:512; Servlet:SPEC:39;
   * 
   * @test_Strategy: Servlet adds content and an error, client verifies the
   * error and that the content was cleared
   */

  /*
   * @testName: sendError_StringTest
   * 
   * @assertion_ids: Servlet:JAVADOC:508
   * 
   * @test_Strategy: Servlet adds a header, a cookie and an error, client
   * verifies the error and that the header still exists
   */

  /*
   * @testName: sendError_StringErrorPageTest
   * 
   * @assertion_ids: Servlet:JAVADOC:509
   * 
   * @test_Strategy: Servlet adds a header, a cookie and content and an error.
   * There also is an error page configured to catch error. Client verifies the
   * error.
   */

  /*
   * @testName: sendRedirectWithLeadingSlashTest
   * 
   * @assertion_ids: Servlet:JAVADOC:516
   * 
   * @test_Strategy: Servlet redirects to another servlet
   */

  /*
   * @testName: sendRedirectWithoutLeadingSlashTest
   * 
   * @assertion_ids: Servlet:JAVADOC:515
   * 
   * @test_Strategy: Servlet redirects to another servlet
   */

  /*
   * @testName: sendRedirectIllegalStateExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:519
   * 
   * @test_Strategy: Servlet flushes the buffer then tries to redirect
   */

  /*
   * @testName: setDateHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:520
   * 
   * @test_Strategy: Servlet sets a date header and client verifies it
   */

  /*
   * @testName: setDateHeaderOverrideTest
   * 
   * @assertion_ids: Servlet:JAVADOC:521
   * 
   * @test_Strategy: Servlet sets the same date header twice and client verifies
   * it
   */

  /*
   * @testName: setHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:523
   * 
   * @test_Strategy: Servlet sets a header and client verifies it
   */

  /*
   * @testName: setHeaderOverrideTest
   * 
   * @assertion_ids: Servlet:JAVADOC:524
   * 
   * @test_Strategy: Servlet sets the same header twice and client verifies it
   */

  /*
   * @testName: setMultiHeaderTest
   *
   * @assertion_ids: Servlet:SPEC:183; Servlet:JAVADOC:523; Servlet:JAVADOC:525;
   * Servlet:JAVADOC:524
   *
   * @test_Strategy: Servlet sets the multivalues for the same header; verify
   * that setHeader clear all with new value
   */

  /*
   * @testName: setIntHeaderTest
   * 
   * @assertion_ids: Servlet:JAVADOC:526
   * 
   * @test_Strategy: Servlet sets an int header and client verifies it
   */

  /*
   * @testName: setStatusTest
   * 
   * @assertion_ids: Servlet:JAVADOC:528
   * 
   * @test_Strategy: Servlet sets a status and client verifies it
   */

  // ----------------------- END HttpServletResponse
  // ------------------------------
}
