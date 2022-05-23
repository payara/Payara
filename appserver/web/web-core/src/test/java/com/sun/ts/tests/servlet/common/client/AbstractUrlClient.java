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

package com.sun.ts.tests.servlet.common.client;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.sun.javatest.Status;
import com.sun.ts.lib.util.TestUtil;
import com.sun.ts.tests.common.webclient.BaseUrlClient;
import com.sun.ts.tests.common.webclient.WebTestCase;
import com.sun.ts.tests.common.webclient.http.HttpRequest;
import com.sun.ts.tests.servlet.common.util.Data;

/**
 * Base client for Servlet tests.
 */

public abstract class AbstractUrlClient extends BaseUrlClient {

  protected static final String APITEST = "apitest";

  protected static final String DONOTUSEServletName = "NoServletName";

  private InetAddress[] _addrs = null;

  private String _servlet = null;

  protected AbstractUrlClient() {
    // Construct a default context root based on the class name of
    // the concrete subclass of this class.
    String cname = this.getClass().getName();
    String prefix = "com.sun.ts.tests.";
    if (cname.startsWith(prefix))
      cname = cname.substring(prefix.length());
    String suffix = ".URLClient";
    if (cname.endsWith(suffix))
      cname = cname.substring(0, cname.length() - suffix.length());
    cname = cname.replace('.', '_');
    cname = "/" + cname + "_web";
    setContextRoot(cname);
  }

  /**
   * Convenience method for the common use case.
   */
  public void run(String args[]) {
    Status s = super.run(args, new PrintWriter(System.out),
        new PrintWriter(System.err));
    s.exit();
  }

  protected void setTestProperties(WebTestCase testCase) {

    setStandardProperties(TEST_PROPS.getProperty(STANDARD), testCase);
    setApiTestProperties(TEST_PROPS.getProperty(APITEST), testCase);
    super.setTestProperties(testCase);
  }

  /**
   * Sets the request, testname, and a search string for test passed. A search
   * is also added for test failure. If found, the test will fail.
   *
   * @param testValue
   *          - a logical test identifier
   * @param testCase
   *          - the current test case
   */
  private void setApiTestProperties(String testValue, WebTestCase testCase) {
    if (testValue == null) {
      return;
    }

    // An API test consists of a request with a request parameter of
    // testname, a search string of Test PASSED, and a logical test name.

    // set the testname
    _testName = testValue;

    // set the request
    StringBuffer sb = new StringBuffer(50);
    if ((_servlet != null)
        && (TEST_PROPS.getProperty(DONOTUSEServletName) == null)) {
      sb.append(GET).append(_contextRoot).append(SL);
      sb.append(_servlet).append("?testname=").append(testValue);
      sb.append(HTTP11);
    } else {
      sb.append(GET).append(_contextRoot).append(SL);
      sb.append(testValue).append(HTTP10);
    }
    System.out.println("REQUEST LINE: " + sb.toString());

    HttpRequest req = new HttpRequest(sb.toString(), _hostname, _port);
    testCase.setRequest(req);

    if ((TEST_PROPS.getProperty(SEARCH_STRING) == null)
        || ((TEST_PROPS.getProperty(SEARCH_STRING)).equals(""))) {
      testCase.setResponseSearchString(Data.PASSED);
      testCase.setUnexpectedResponseSearchString(Data.FAILED);
    }

  }

  /**
   * Consists of a test name, a request, and a goldenfile.
   * 
   * @param testValue
   *          - a logical test identifier
   * @param testCase
   *          - the current test case
   */
  private void setStandardProperties(String testValue, WebTestCase testCase) {

    if (testValue == null) {
      return;
    }
    // A standard test sets consists of a testname
    // a request, and a goldenfile. The URI is not used
    // in this case since the JSP's are assumed to be located
    // at the top of the contextRoot
    StringBuffer sb = new StringBuffer(50);

    // set the testname
    _testName = testValue;

    // set the request
    // sb.append(GET).append(_contextRoot).append(SL);
    // sb.append(testValue).append(JSP_SUFFIX).append(HTTP10);
    // setRequest(sb.toString());
    // HttpRequest req = new HttpRequest(sb.toString(), _hostname, _port);
    // testCase.setRequest(req);

    if (_servlet != null) {
      sb.append(GET).append(_contextRoot).append(SL);
      sb.append(_servlet).append("?testname=").append(testValue);
      sb.append(HTTP11);
    } else {
      sb.append(GET).append(_contextRoot).append(SL);
      sb.append(testValue).append(HTTP10);
    }
    System.out.println("REQUEST LINE: " + sb.toString());
    HttpRequest req = new HttpRequest(sb.toString(), _hostname, _port);
    testCase.setRequest(req);

    // set the goldenfile
    sb = new StringBuffer(50);
    sb.append(_tsHome).append(GOLDENFILEDIR);
    sb.append(_generalURI).append(SL);
    sb.append(testValue).append(GF_SUFFIX);
    testCase.setGoldenFilePath(sb.toString());
  }

  /**
   * Sets the name of the servlet to use when building a request for a single
   * servlet API test.
   * 
   * @param servlet
   *          - the name of the servlet
   */
  protected void setServletName(String servlet) {
    _servlet = servlet;
  }

  protected String getServletName() {
    return _servlet;
  }

  protected String getLocalInterfaceInfo(boolean returnAddresses) {
    String result = null;
    initInetAddress();
    if (_addrs.length != 0) {
      StringBuffer sb = new StringBuffer(32);
      if (!returnAddresses) {
        // localhost might not show up if aliased
        sb.append("localhost,");
      } else {
        // add 127.0.0.1
        sb.append("127.0.0.1,");
      }

      for (int i = 0; i < _addrs.length; i++) {
        if (returnAddresses) {
          String ip = _addrs[i].getHostAddress();
          if (!ip.equals("127.0.0.1")) {
            if (ip.contains("%")) {
              int scope_id = ip.indexOf("%");
              ip = ip.substring(0, scope_id);
            }
            sb.append(ip);
          }
        } else {
          String host = _addrs[i].getCanonicalHostName();
          if (!host.equals("localhost")) {
            sb.append(host);
          }
        }
        if (i + 1 != _addrs.length) {
          sb.append(",");
        }
      }
      result = sb.toString();
      TestUtil.logTrace("[AbstractUrlClient] Interface info: " + result);
    }
    return result;
  }

  private void initInetAddress() {
    if (_addrs == null) {
      try {
        _addrs = InetAddress
            .getAllByName(InetAddress.getLocalHost().getCanonicalHostName());
      } catch (UnknownHostException uhe) {
        TestUtil.logMsg(
            "[AbstractUrlClient][WARNING] Unable to obtain local host information.");
      }
    }
  }
}
