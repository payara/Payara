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

package com.sun.ts.tests.common.webclient.validation;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.Header;

import com.sun.ts.lib.util.TestUtil;
import com.sun.ts.tests.common.webclient.WebTestCase;
import com.sun.ts.tests.common.webclient.handler.Handler;
import com.sun.ts.tests.common.webclient.handler.HandlerFactory;
import com.sun.ts.tests.common.webclient.http.HttpRequest;
import com.sun.ts.tests.common.webclient.http.HttpResponse;

/**
 * Base abstract class for WebTestCase validation.
 */
public abstract class WebValidatorBase implements ValidationStrategy {

  /**
   * Used to detect 4xx class HTTP errors to allow fail fast situations when 4xx
   * errors are not expected.
   */
  protected static final char CLIENT_ERROR = '4';

  /**
   * Used to detect 5xx class HTTP errors to allows fail fast situations when
   * 5xx errors are not expected.
   */
  protected static final char SERVER_ERROR = '5';

  /**
   * This test case's HttpResponse
   */
  protected HttpResponse _res = null;

  /**
   * This test case's HttpRequest
   */
  protected HttpRequest _req = null;

  /**
   * The test case being validated
   */
  protected WebTestCase _case = null;

  /**
   * <tt>validate</tt> Will validate the response against the configured
   * TestCase.
   *
   *
   * <ul>
   * <li>Check the HTTP status-code</li>
   * <li>Check the HTTP reason-phrase</li>
   * <li>Check for expected headers</li>
   * <li>Check from unexpected headers</li>
   * <li>Check expected search strings</li>
   * <li>Check unexpected search strings</li>
   * <li>Check the goldenfile</li>
   * </ul>
   */
  public boolean validate(WebTestCase testCase) {
    _res = testCase.getResponse();
    _req = testCase.getRequest();
    _case = testCase;

    // begin the check
    try {
      if (!checkStatusCode() || !checkReasonPhrase() || !checkExpectedHeaders()
          || !checkUnexpectedHeaders() || !checkSearchStrings()
          || !checkSearchStringsNoCase() || !checkUnorderedSearchStrings()
          || !checkUnexpectedSearchStrings() || !checkGoldenfile()) {
        return false;
      }
    } catch (IOException ioe) {
      TestUtil
          .logErr("[WebValidatorBase] Unexpected Exception: " + ioe.toString());
      return false;
    }
    return true;
  }

  /**
   * <code>checkStatusCode</code> will perform status code comparisons based on
   * the algorithm below
   * <ul>
   * <li>Check the HTTP status code</li>
   * <ul>
   * <li>
   * <p>
   * If status code is -1, then return true
   * </p>
   * </li>
   * <li>
   * <p>
   * If test case status code null and response 4xx, return failure, print
   * error; return false
   * </p>
   * </li>
   * <li>
   * <p>
   * If test case status code null and response 5xx, return failure include
   * response body; return false
   * <p>
   * </li>
   * <li>
   * <p>
   * If test case status code null, and response not 4xx or 5xx, return true
   * </p>
   * </li>
   * <li>
   * <p>
   * Test case status code not null, compare it with the response status code;
   * return true if equal
   * <p>
   * <li>
   * </ul>
   * </ul>
   *
   * @return boolen result of check
   * @throws IOException
   *           if an IO error occurs during validation
   */
  protected boolean checkStatusCode() throws IOException {
    String sCode = _case.getStatusCode();
    String resCode = _res.getStatusCode();
    if ("-1".equals(sCode))
      return true;

    if (sCode == null && resCode.charAt(0) == CLIENT_ERROR) {
      TestUtil
          .logErr("[WebValidatorBase] Unexpected " + resCode + " received from "
              + "target server!  Request path: " + _req.getRequestPath());
      return false;
    }

    if (sCode == null && (resCode.charAt(0) == SERVER_ERROR)) {
      String resBody = _res.getResponseBodyAsRawString();
      StringBuffer sb = new StringBuffer(75 + resBody.length());
      sb.append("[WebValidatorBase] Unexpected '");
      sb.append(resCode).append("' received from target server!\n");
      sb.append("Error response recieved from server:\n");
      sb.append("------------------------------------------------\n");
      sb.append(resBody != null ? resBody : "NO RESPONSE");
      TestUtil.logErr(sb.toString());
      return false;
    }

    if (sCode == null) {
      return true;
    }

    /*
     * Take sCode as a comma separated list of status codes.
     *
     * If prefixed by "!" the response status code must not match any in the list.
     *
     * Otherwise matching any in the list is accepted.
     */

    boolean exclusions = sCode.charAt(0) == '!';
    String[] sCodes = exclusions ? sCode.substring(1).split(",") : sCode.split(",");

    if (exclusions) {
        for (String current : sCodes) {
            if (current.equals(resCode)) {
                TestUtil.logErr("[WebValidatorBase] Unexpected Status Code "
                    + "recieved from server.  Expected any value except '" + sCode
                    + "', received '" + resCode + "'");
                return false;
            }
        }
    } else {
        boolean found = false;
        for (String current : sCodes) {
            if (current.equals(resCode)) {
                TestUtil.logTrace("[WebValidatorBase] Expected Status Code '" + current
                        + "' found in response line!");
                found = true;
                break;
            }
        }

        if (!found) {
            TestUtil.logTrace("[WebValidatorBase] Status Code '" + sCode
                      + "' not found in response line!");

            return false;
        }
    }

    return true;
  }

  /**
   * <code>checkSearchStrings</code> will scan the response for the configured
   * strings that are to be expected in the response.
   * <ul>
   * <li>Check search strings</li>
   * <ul>
   * <li>
   * <p>
   * If list of Strings is null, return true.
   * </p>
   * </li>
   * <li>
   * <p>
   * If list of Strings is not null, scan response body. If string is found,
   * return true, otherwise display error and return false.
   * </p>
   * </li>
   * </ul>
   * </ul>
   * <em>NOTE:</em> If there are multiple search strings, the search will be
   * performed as such to preserve the order. For example, if the list of search
   * strings contains two entities, the search for the second entity will be
   * started after the index if the first match.
   *
   * @return boolen result of check
   * @throws IOException
   *           if an IO error occurs during validation
   */
  protected boolean checkSearchStrings() throws IOException {
    List list = _case.getSearchStrings();
    boolean found = true;
    if (list != null && !list.isEmpty()) {
      String responseBody = _res.getResponseBodyAsRawString();

      String search = null;

      for (int i = 0, n = list.size(), startIdx = 0, bodyLength = responseBody
          .length(); i < n; i++) {

        // set the startIdx to the same value as the body length
        // and let the test fail (prevents index based runtime
        // exceptions).
        if (startIdx >= bodyLength) {
          startIdx = bodyLength;
        }

        search = (String) list.get(i);
        int searchIdx = responseBody.indexOf(search, startIdx);

        TestUtil.logTrace(
            "[WebValidatorBase] Scanning response for " + "search string: '"
                + search + "' starting at index " + "location: " + startIdx);
        if (searchIdx < 0) {
          found = false;
          StringBuffer sb = new StringBuffer(255);
          sb.append("[WebValidatorBase] Unable to find the following ");
          sb.append("search string in the server's ");
          sb.append("response: '").append(search).append("' at index: ");
          sb.append(startIdx);
          sb.append("\n[WebValidatorBase] Server's response:\n");
          sb.append("-------------------------------------------\n");
          sb.append(responseBody);
          sb.append("\n-------------------------------------------\n");
          TestUtil.logErr(sb.toString());
          break;
        }

        TestUtil.logTrace("[WebValidatorBase] Found search string: '" + search
            + "' at index '" + searchIdx + "' in the server's " + "response");
        // the new searchIdx is the old index plus the lenght of the
        // search string.
        startIdx = searchIdx + search.length();
      }
    }
    return found;
  }

  /**
   * <code>checkSearchStringsNoCase</code> will scan the response for the
   * configured case insensitive strings that are to be expected in the
   * response.
   * <ul>
   * <li>Check search strings</li>
   * <ul>
   * <li>
   * <p>
   * If list of Strings is null, return true.
   * </p>
   * </li>
   * <li>
   * <p>
   * If list of Strings is not null, scan response body. If string is found,
   * return true, otherwise display error and return false.
   * </p>
   * </li>
   * </ul>
   * </ul>
   * <em>NOTE:</em> If there are multiple search strings, the search will be
   * performed as such to preserve the order. For example, if the list of search
   * strings contains two entities, the search for the second entity will be
   * started after the index if the first match.
   *
   * @return boolen result of check
   * @throws IOException
   *           if an IO error occurs during validation
   */
  protected boolean checkSearchStringsNoCase() throws IOException {
    List list = _case.getSearchStringsNoCase();
    boolean found = true;
    if (list != null && !list.isEmpty()) {
      String responseBody = _res.getResponseBodyAsRawString();

      String search = null;

      for (int i = 0, n = list.size(), startIdx = 0, bodyLength = responseBody
          .length(); i < n; i++) {

        // set the startIdx to the same value as the body length
        // and let the test fail (prevents index based runtime
        // exceptions).
        if (startIdx >= bodyLength) {
          startIdx = bodyLength;
        }

        search = (String) list.get(i);
        int searchIdx = responseBody.toLowerCase().indexOf(search.toLowerCase(),
            startIdx);

        TestUtil.logTrace(
            "[WebValidatorBase] Scanning response for " + "search string: '"
                + search + "' starting at index " + "location: " + startIdx);
        if (searchIdx < 0) {
          found = false;
          StringBuffer sb = new StringBuffer(255);
          sb.append("[WebValidatorBase] Unable to find the following ");
          sb.append("search string in the server's ");
          sb.append("response: '").append(search).append("' at index: ");
          sb.append(startIdx);
          sb.append("\n[WebValidatorBase] Server's response:\n");
          sb.append("-------------------------------------------\n");
          sb.append(responseBody);
          sb.append("\n-------------------------------------------\n");
          TestUtil.logErr(sb.toString());
          break;
        }

        TestUtil.logTrace("[WebValidatorBase] Found search string: '" + search
            + "' at index '" + searchIdx + "' in the server's " + "response");
        // the new searchIdx is the old index plus the lenght of the
        // search string.
        startIdx = searchIdx + search.length();
      }
    }
    return found;
  }

  /**
   * <code>checkUnorderedSearchStrings</code> will scan the response for the
   * configured strings that are to be expected in the response.
   * <ul>
   * <li>Check search strings</li>
   * <ul>
   * <li>
   * <p>
   * If list of Strings is null, return true.
   * </p>
   * </li>
   * <li>
   * <p>
   * If list of Strings is not null, scan response body. If string is found,
   * return true, otherwise display error and return false.
   * </p>
   * </li>
   * </ul>
   * </ul>
   *
   * @return boolen result of check
   * @throws IOException
   *           if an IO error occurs during validation
   */
  protected boolean checkUnorderedSearchStrings() throws IOException {
    List list = _case.getUnorderedSearchStrings();
    boolean found = true;
    if (list != null && !list.isEmpty()) {
      String responseBody = _res.getResponseBodyAsRawString();

      String search = null;

      for (int i = 0, n = list.size(); i < n; i++) {

        search = (String) list.get(i);
        int searchIdx = responseBody.indexOf(search);

        TestUtil.logTrace("[WebValidatorBase] Scanning response for "
            + "search string: '" + search + "'...");
        if (searchIdx < 0) {
          found = false;
          StringBuffer sb = new StringBuffer(255);
          sb.append("[WebValidatorBase] Unable to find the following ");
          sb.append("search string in the server's ");
          sb.append("response: '").append(search);
          sb.append("\n[WebValidatorBase] Server's response:\n");
          sb.append("-------------------------------------------\n");
          sb.append(responseBody);
          sb.append("\n-------------------------------------------\n");
          TestUtil.logErr(sb.toString());
          break;
        }

        TestUtil.logTrace("[WebValidatorBase] Found search string: '" + search
            + "' at index '" + searchIdx + "' in the server's " + "response");
      }
    }
    return found;
  }

  /**
   * <code>checkUnexpectedSearchStrings</code> will scan the response for the
   * configured strings that are not expected in the response.
   * <ul>
   * <li>Check unexpected search strings</li>
   * <ul>
   * <li>
   * <p>
   * If list of Strings is null, return true.
   * </p>
   * </li>
   * <li>
   * <p>
   * If list of Strings is not null, scan response body. If string is not found,
   * return true, otherwise display error and return false.
   * <p>
   * </li>
   * </ul>
   * </ul>
   *
   * @return boolen result of check
   * @throws IOException
   *           if an IO error occurs during validation
   */
  protected boolean checkUnexpectedSearchStrings() throws IOException {
    List list = _case.getUnexpectedSearchStrings();
    if (list != null && !list.isEmpty()) {
      String responseBody = _res.getResponseBodyAsRawString();
      Iterator iter = list.iterator();
      while (iter.hasNext()) {
        String search = (String) iter.next();
        TestUtil.logTrace("[WebValidatorBase] Scanning response.  The following"
            + " string should not be present in the response: '" + search
            + "'");
        if (responseBody.indexOf(search) > -1) {
          StringBuffer sb = new StringBuffer(255);
          sb.append("[WebValidatorBase] Found the following unexpected ");
          sb.append("search string in the server's ");
          sb.append("response: '").append(search).append("'");
          sb.append("\n[WebValidatorBase] Server's response:\n");
          sb.append("-------------------------------------------\n");
          sb.append(responseBody);
          sb.append("\n-------------------------------------------\n");
          TestUtil.logErr(sb.toString());
          return false;
        }
      }
    }
    return true;
  }

  /**
   * <code>checkGoldenFile</code> compare the server's response with the
   * configured goldenfile
   * <ul>
   * <li>Check the goldenfile</li>
   * <ul>
   * <li>
   * <p>
   * If goldenfile is null, return true.
   * </p>
   * </li>
   * <li>
   * <p>
   * If goldenfile is not null, compare the goldenfile with the response. If
   * equal, return true, otherwise display error and return false.
   * <p>
   * </li>
   * </ul>
   * </ul>
   *
   * @return boolen result of check
   * @throws IOException
   *           if an IO error occurs during validation
   */
  protected abstract boolean checkGoldenfile() throws IOException;

  /**
   * <code>checkReasonPhrase</code> will perform comparisons between the
   * configued reason-phrase and that of the response.
   * <ul>
   * <li>Check reason-phrase</li>
   * <ul>
   * <li>
   * <p>
   * If configured reason-phrase is null, return true.
   * </p>
   * </li>
   * <li>
   * <p>
   * If configured reason-phrase is not null, compare the reason-phrases with
   * the response. If equal, return true, otherwise display error and return
   * false.
   * <p>
   * </li>
   * </ul>
   * </ul>
   *
   * @return boolen result of check
   */
  protected boolean checkReasonPhrase() {
    String sReason = _case.getReasonPhrase();
    String resReason = _res.getReasonPhrase();

    if (sReason == null) {
      return true;
    } else if (sReason.equalsIgnoreCase(resReason)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * <code>checkExpectedHeaders</code> will check the response for the
   * configured expected headers.
   * <ul>
   * <li>Check expected headers</li>
   * <ul>
   * <li>
   * <p>
   * If there are no expected headers, return true.
   * </p>
   * </li>
   * <li>
   * <p>
   * If there are expected headers, scan the response for the expected headers.
   * If all expected headers are found, return true, otherwise display an error
   * and return false.
   * <p>
   * </li>
   * </ul>
   * </ul>
   *
   * @return boolen result of check
   */
  protected boolean checkExpectedHeaders() {
    Header[] expected = _case.getExpectedHeaders();
    if (isEmpty(expected)) {
      return true;
    } else {
      boolean found = true;
      Header currentHeader = null;
      for (int i = 0; i < expected.length; i++) {
        currentHeader = expected[i];
        Header resHeader = _res.getResponseHeader(currentHeader.getName());
        if (resHeader != null) {
          Handler handler = HandlerFactory.getHandler(currentHeader.getName());
          if (!handler.invoke(currentHeader, resHeader)) {
            found = false;
            break;
          }
        } else {
          found = false;
          break;
        }
      }
      if (!found) {
        StringBuffer sb = new StringBuffer(255);
        sb.append("[WebValidatorBase] Unable to find the following header");
        sb.append(" in the server's response: ");
        sb.append(currentHeader.toExternalForm()).append("\n");
        sb.append("[WebValidatorBase] Response headers recieved from");
        sb.append(" server:");

        Header[] resHeaders = _res.getResponseHeaders();
        for (int i = 0; i < resHeaders.length; i++) {
          sb.append("\n\tResponseHeader -> ");
          sb.append(resHeaders[i].toExternalForm());
        }
        sb.append("\n");
        TestUtil.logErr(sb.toString());

        return false;
      } else {
        TestUtil.logTrace("[WebValidatorBase] Found expected header: "
            + currentHeader.toExternalForm());
        return true;
      }
    }
  }

  /**
   * <code>checkUnexpectedHeaders</code> will check the response for the
   * configured unexpected expected headers.
   * <ul>
   * <li>Check unexpected headers</li>
   * <ul>
   * <li>
   * <p>
   * If there are no configured unexpected headers, return true.
   * </p>
   * </li>
   * <li>
   * <p>
   * If there are configured unexpected headers, scan the response for the
   * unexpected headers. If the headers are not found, return true, otherwise
   * display an error and return false.
   * <p>
   * </li>
   * </ul>
   * </ul>
   *
   * @return boolen result of check
   */
  protected boolean checkUnexpectedHeaders() {
    Header[] unexpected = _case.getUnexpectedHeaders();
    if (isEmpty(unexpected)) {
      return true;
    } else {
      Header currentHeader = null;
      for (int i = 0; i < unexpected.length; i++) {
        currentHeader = unexpected[i];
        String currName = currentHeader.getName();
        String currValue = currentHeader.getValue();
        Header resHeader = _res.getResponseHeader(currName);
        if (resHeader != null) {
          if (resHeader.getValue().equals(currValue)) {
            StringBuffer sb = new StringBuffer(255);
            sb.append("[WebValidatorBase] Unexpected header found in the ");
            sb.append("server's response: ");
            sb.append(currentHeader.toExternalForm()).append("\n");
            sb.append("[WebValidatorBase] Response headers recieved from");
            sb.append("server:");

            Header[] resHeaders = _res.getResponseHeaders();
            for (int j = 0; j < resHeaders.length; j++) {
              sb.append("\n\tResponseHeader -> ");
              sb.append(resHeaders[j].toExternalForm());
            }
            sb.append("\n");
            TestUtil.logErr(sb.toString());

            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Utility method to determine of the expected or unexpected headers are empty
   * or not.
   */
  protected boolean isEmpty(Header[] headers) {
    if (headers == null || headers.length == 0) {
      return true;
    } else {
      return false;
    }
  }
}
