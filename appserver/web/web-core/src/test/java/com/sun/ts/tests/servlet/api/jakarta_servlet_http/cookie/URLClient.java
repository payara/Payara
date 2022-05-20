/*
 * Copyright (c) 2006, 2021 Oracle and/or its affiliates and others.
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

package com.sun.ts.tests.servlet.api.jakarta_servlet_http.cookie;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpec;

import com.sun.javatest.Status;
import com.sun.ts.lib.util.TestUtil;
import com.sun.ts.tests.common.webclient.http.HttpRequest;
import com.sun.ts.tests.common.webclient.http.HttpResponse;
import com.sun.ts.tests.servlet.common.client.AbstractUrlClient;
import com.sun.ts.tests.servlet.common.util.Data;

public class URLClient extends AbstractUrlClient {
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
    setContextRoot("/servlet_jsh_cookie_web");

    return super.run(args, out, err);
  }

  /*
   * @class.setup_props: webServerHost; webServerPort; ts_home;
   *
   */
  private int findCookie(Cookie[] cookie, String name) {
    boolean found = false;
    int i = 0;
    if (cookie != null) {
      while ((!found) && (i < cookie.length)) {
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

  /* Run test */

  /*
   * @testName: cloneTest
   * 
   * @assertion_ids: Servlet:JAVADOC:453
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void cloneTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "cloneTest");
    invoke();
  }

  /*
   * @testName: constructorTest
   * 
   * @assertion_ids: Servlet:JAVADOC:434
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void constructorTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "constructorTest");
    invoke();
  }

  /*
   * @testName: constructorIllegalArgumentExceptionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:628
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void constructorIllegalArgumentExceptionTest() throws Fault {
    TEST_PROPS.setProperty(REQUEST,
        "GET /servlet_jsh_cookie_web/TestServlet?testname=constructorIllegalArgumentExceptionTest HTTP/1.1");
    TEST_PROPS.setProperty(UNEXPECTED_RESPONSE_MATCH, "Test FAILED");
    invoke();
  }

  /*
   * @testName: getCommentTest
   * 
   * @assertion_ids: Servlet:JAVADOC:436
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void getCommentTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getCommentTest");
    invoke();
  }

  /*
   * @testName: getCommentNullTest
   * 
   * @assertion_ids: Servlet:JAVADOC:437
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void getCommentNullTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getCommentNullTest");
    invoke();
  }

  /*
   * @testName: getDomainTest
   * 
   * @assertion_ids: Servlet:JAVADOC:439
   * 
   * @test_Strategy: Client sends a version 0 and 1 cookie to the servlet.
   * Servlet verifies values and returns result to client
   */
  public void getDomainTest() throws Fault {
    // version 1
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "Cookie: $Version=1; name1=value1; $Domain=" + _hostname
            + "; $Path=/servlet_jsh_cookie_web");
    TEST_PROPS.setProperty(APITEST, "getDomainTest");
    invoke();

  }

  /*
   * @testName: getMaxAgeTest
   * 
   * @assertion_ids: Servlet:JAVADOC:443
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void getMaxAgeTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getMaxAgeTest");
    invoke();
  }

  /*
   * @testName: getNameTest
   * 
   * @assertion_ids: Servlet:JAVADOC:448
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void getNameTest() throws Fault {
    // version 0
    TEST_PROPS.setProperty(REQUEST_HEADERS, "Cookie: name1=value1; Domain="
        + _hostname + "; Path=/servlet_jsh_cookie_web");
    TEST_PROPS.setProperty(APITEST, "getNameTest");
    invoke();
    // version 1
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "Cookie: $Version=1; name1=value1; $Domain=" + _hostname
            + "; $Path=/servlet_jsh_cookie_web");
    TEST_PROPS.setProperty(APITEST, "getNameTest");
    invoke();
  }

  /*
   * @testName: getPathTest
   * 
   * @assertion_ids: Servlet:JAVADOC:445
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void getPathTest() throws Fault {
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "Cookie: $Version=1; name1=value1; $Domain=" + _hostname
            + "; $Path=/servlet_jsh_cookie_web");
    TEST_PROPS.setProperty(APITEST, "getPathTest");
    invoke();
  }

  /*
   * @testName: getSecureTest
   * 
   * @assertion_ids: Servlet:JAVADOC:447
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void getSecureTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getSecureTest");
    invoke();
  }

  /*
   * @testName: getValueTest
   * 
   * @assertion_ids: Servlet:JAVADOC:450
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void getValueTest() throws Fault {
    // version 0
    TEST_PROPS.setProperty(REQUEST_HEADERS, "Cookie: name1=value1; Domain="
        + _hostname + "; Path=/servlet_jsh_cookie_web");
    TEST_PROPS.setProperty(APITEST, "getValueTest");
    invoke();
    // version 1
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "Cookie: $Version=1; name1=value1; $Domain=" + _hostname
            + "; $Path=/servlet_jsh_cookie_web");
    TEST_PROPS.setProperty(APITEST, "getValueTest");
    invoke();
  }

  /*
   * @testName: getVersionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:451
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void getVersionTest() throws Fault {
    // version 0
    TEST_PROPS.setProperty(REQUEST_HEADERS, "Cookie: name1=value1; Domain="
        + _hostname + "; Path=/servlet_jsh_cookie_web");
    TEST_PROPS.setProperty(APITEST, "getVersionVer0Test");
    invoke();
    // version 1
    TEST_PROPS.setProperty(REQUEST_HEADERS,
        "Cookie: $Version=1; name1=value1; $Domain=" + _hostname
            + "; $Path=/servlet_jsh_cookie_web");
    TEST_PROPS.setProperty(APITEST, "getVersionVer1Test");
    invoke();
  }

  /*
   * @testName: setDomainTest
   * 
   * @assertion_ids: Servlet:JAVADOC:438
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void setDomainTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "setDomainTest");
    invoke();
  }

  /*
   * @testName: setMaxAgePositiveTest
   * 
   * @assertion_ids: Servlet:JAVADOC:440
   * 
   * @test_Strategy: Servlet sets values and client verifies them
   */
  public void setMaxAgePositiveTest() throws Fault {
    String testName = "setMaxAgePositiveTest";
    HttpResponse response = null;
    String dateHeader = null;
    int index = -1;
    Date expiryDate = null;
    String body = null;

    HttpRequest request = new HttpRequest("GET " + getContextRoot() + "/"
        + getServletName() + "?testname=" + testName + " HTTP/1.1", _hostname,
        _port);

    try {
      response = request.execute();
      dateHeader = response.getResponseHeader("testDate").toString();
      CookieSpec spec = CookiePolicy.getCookieSpec(CookiePolicy.NETSCAPE);

      TestUtil
          .logTrace("Found " + response.getResponseHeaders("Set-Cookie").length
              + " set-cookie entry");

      boolean foundcookie = false;
      Header[] CookiesHeader = response.getResponseHeaders("Set-Cookie");
      int i = 0;
      while (i < CookiesHeader.length) {
        TestUtil.logTrace("Checking set-cookiei " + i + ":" + CookiesHeader[i]);
        Cookie[] cookies = spec.parse(".eng.com", _port, getServletName(),
            false, CookiesHeader[i]);
        index = findCookie(cookies, "name1");
        if (index >= 0) {
          expiryDate = cookies[index].getExpiryDate();
          body = response.getResponseBodyAsString();
          foundcookie = true;
          break;
        }
        i++;
      }

      if (!foundcookie)
        throw new Fault("The test cookie was not located in the response");
    } catch (Throwable t) {
      throw new Fault("Exception occurred:" + t, t);
    }

    // put expiry date into GMT
    SimpleDateFormat sdf = new SimpleDateFormat(TestServlet.CUSTOM_HEADER_DATE_FORMAT);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    String resultStringDate = sdf.format(expiryDate);
    try {
      Date resultDate = sdf.parse(resultStringDate);
      Date expectedDate = sdf
          .parse(dateHeader.substring(dateHeader.indexOf(": ") + 2).trim());
      if (resultDate.before(expectedDate)) {
        throw new Fault("The expiry date was incorrect, expected ="
            + expectedDate + ", result = " + resultDate);
      }
    } catch (Throwable t) {
      throw new Fault("Exception occurred: " + t);
    }

    if (body.indexOf(Data.PASSED) == -1) {
      throw new Fault("The string: " + Data.PASSED + " not found in response");
    }
  }

  /*
   * @testName: setMaxAgeZeroTest
   * 
   * @assertion_ids: Servlet:JAVADOC:442
   * 
   * @test_Strategy: Servlet sets values and client verifies them
   */
  public void setMaxAgeZeroTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "setMaxAgeZeroTest");
    TEST_PROPS.setProperty(EXPECTED_HEADERS, "Set-Cookie:name1=value1##Max-Age=0");
    invoke();
  }

  /*
   * @testName: setMaxAgeNegativeTest
   * 
   * @assertion_ids: Servlet:JAVADOC:441
   * 
   * @test_Strategy: Servlet sets values and client verifies them
   */
  public void setMaxAgeNegativeTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "setMaxAgeNegativeTest");
    TEST_PROPS.setProperty(EXPECTED_HEADERS,
        "Set-Cookie:name1=value1##!Expire##!Max-Age");
    invoke();
  }

  /*
   * @testName: setPathTest
   * 
   * @assertion_ids: Servlet:JAVADOC:444
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void setPathTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "setPathTest");
    TEST_PROPS.setProperty(EXPECTED_HEADERS,
        "Set-Cookie:Path=\"/servlet_jsh_cookie_web\"");
    invoke();
  }

  /*
   * @testName: setSecureTest
   * 
   * @assertion_ids: Servlet:JAVADOC:446
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void setSecureTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "setSecureVer0Test");
    invoke();
    TEST_PROPS.setProperty(APITEST, "setSecureVer1Test");
    invoke();
  }

  /*
   * @testName: setValueTest
   * 
   * @assertion_ids: Servlet:JAVADOC:449
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void setValueTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "setValueVer0Test");
    invoke();
    TEST_PROPS.setProperty(APITEST, "setValueVer1Test");
    invoke();
  }

  /*
   * @testName: setVersionTest
   * 
   * @assertion_ids: Servlet:JAVADOC:452
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void setVersionTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "setVersionVer0Test");
    invoke();
    TEST_PROPS.setProperty(APITEST, "setVersionVer1Test");
    invoke();
  }
  
  /*
   * @testName: setAttributeTest
   * 
   * @assertion_ids:
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void setAttributeTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "setAttributeTest");
    invoke();
  }
  
  /*
   * @testName: getAttributesTest
   * 
   * @assertion_ids:
   * 
   * @test_Strategy: Servlet tests method and returns result to client
   */
  public void getAttributesTest() throws Fault {
    TEST_PROPS.setProperty(APITEST, "getAttributesTest");
    invoke();
  }
}
