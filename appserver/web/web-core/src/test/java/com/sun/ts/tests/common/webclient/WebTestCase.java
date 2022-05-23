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
 * $Id$
 */

package com.sun.ts.tests.common.webclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpState;

import com.sun.ts.lib.util.TestUtil;
import com.sun.ts.tests.common.webclient.http.HttpRequest;
import com.sun.ts.tests.common.webclient.http.HttpResponse;
import com.sun.ts.tests.common.webclient.validation.ValidationFactory;
import com.sun.ts.tests.common.webclient.validation.ValidationStrategy;
// used to force the class to be compiled
import com.sun.ts.tests.common.webclient.validation.TokenizedValidator;

/**
 * A TestCase implementation for HTTP-based testing. This allows the user to set
 * criteria for test success which will be compared against the response from
 * the server.
 */
public class WebTestCase implements TestCase {

  /**
   * Tokenized response validation strategy
   */
  public static final String TOKENIZED_STRATEGY = "com.sun.ts.tests.common.webclient.validation.TokenizedValidator";

  /**
   * Whitespace response validation strategy
   */
  public static final String WHITESPACE_STRATEGY = "com.sun.ts.tests.common.webclient.validation.WhitespaceValidator";

  /**
   * The request for this case.
   */
  private HttpRequest _request = null;

  /**
   * The server's response.
   */
  private HttpResponse _response = null;

  // use HttpMethod instances to store test
  // case headers that are expected or not
  // expected.

  /**
   * Storage for headers that are expected in the response
   */
  private Map<String, Header> _expected = null;

  /**
   * Storage for headers that are not expected in the response
   */
  private Map<String, Header> _unexpected = null;

  /**
   * Expected response status code.
   */
  private String _statusCode = null;

  /**
   * Expected response reason phrase.
   */
  private String _reasonPhrase = null;

  /**
   * Path to goldenfile.
   */
  private String _goldenfilePath = null;

  /**
   * A List of strings that will be searched for in the response in the order
   * they appear in the list.
   */
  private List<String> _searchStrings = null;

  /**
   * A List of case insensitive strings that will be searched for in the
   * response in the order they appear in the list.
   */
  private List<String> _searchStringsNoCase = null;

  /**
   * A List of strings that will be search for in the response with no attention
   * paid to the order they appear.
   */
  private List<String> _unorderedSearchStrings = null;

  /**
   * A List of strings that should not be in the server's response.
   */
  private List<String> _uSearchStrings = null;

  /**
   * Indicates whether a response body should be expected or not.
   */
  private boolean _expectResponseBody = true;

  /**
   * Strategy to use when validating the test case against the server's
   * response.
   */
  private ValidationStrategy _strategy = null;

  /**
   * Logical name for test case.
   */
  private String _name = "Test Case";

  /**
   * Creates a new instance of WebTestCase By default, a new WebTestCase
   * instance will use the TokenizedValidator to verify the response with the
   * configured properties of the test case.
   */
  public WebTestCase() {
    _strategy = ValidationFactory.getInstance(TOKENIZED_STRATEGY);
  }

  /*
   * public methods
   * ========================================================================
   */
  /**
   * Executes the test case.
   *
   * @throws TestFailureException
   *           if the test fails for any reason.
   * @throws IllegalStateException
   *           if no request was configured or if no Validator is available at
   *           runtime.
   */
  public void execute() throws TestFailureException {

    // If no request was created, we can't run.
    if (_request == null) {
      throw new IllegalStateException("[FATAL] HttpRequest is null.");
    }

    // If no stragey instance is available (strange, but has happened),
    // fail.
    if (_strategy == null) {
      throw new IllegalStateException("[FATAL] No Validator available.");
    }

    try {
      _response = _request.execute();
    } catch (Throwable t) {
      String message = t.getMessage();
      throw new TestFailureException("[FATAL] Unexpected failure during "
          + "test execution." + (message == null ? t.toString() : message), t);
    }

    // Validate this test case instance
    if (!_strategy.validate(this)) {
      throw new TestFailureException("Test FAILED!");
    }

  }

  /**
   * Sets the status code to be expected in the response from the server, i.e.
   * 500 or 404, etc.
   *
   * @param statusCode
   *          the expected status code
   */
  public void setExpectedStatusCode(String statusCode) {
    _statusCode = statusCode;
  }

  /**
   * Sets the reason phrase to be expected in the response from the server.
   *
   * @param reasonPhrase
   *          the expected reason-phrase
   */
  public void setExpectedReasonPhrase(String reasonPhrase) {
    _reasonPhrase = reasonPhrase;
  }

  /**
   * Adds a header that is to be expected in the response from the server.
   *
   * @param header
   *          in the format of <headername>:<value> (test:foo)
   */
  public void addExpectedHeader(String header) {
    if (_expected == null) {
      _expected = new HashMap<String, Header>();
    }
    addHeader(_expected, header);
  }

  /**
   * Sets the path to the goldenfile the test case should use.
   *
   * @param gfPath
   *          a fully qualified path including filename.
   */
  public void setGoldenFilePath(String gfPath) {
    _goldenfilePath = gfPath;
  }

  /**
   * Sets the request that should be dispatched by this test case.
   *
   * @param request
   *          the HTTP request used for this test case
   */
  public void setRequest(HttpRequest request) {
    _request = request;
  }

  /**
   * Adds a header that is should not be in the server's response.
   *
   * @param header
   *          in the format of <headername>:<value> (test:foo)
   */
  public void addUnexpectedHeader(String header) {
    if (_unexpected == null) {
      _unexpected = new HashMap<String, Header>();
    }
    addHeader(_unexpected, header);
  }

  /**
   * Enables/Disables an assertion that a response body is present.
   *
   * @param value
   *          a value of true will enable the assertion.
   */
  public void setAssertNoResponseBody(boolean value) {
  }

  /**
   * Sets a string that will be scanned for and expected in the response body
   * from the server.
   *
   * If multiple search strings are required, one can either call this method
   * for each string, or pass in one string with pipe <code>|<code> delimiting
   * the individual search strings within the large string.
   *
   * @param searchString
   *          a string expected in the server's response body
   */
  public void setResponseSearchString(String searchString) {
    if (_searchStrings == null) {
      _searchStrings = new ArrayList<String>();
    }
    addSearchStrings(_searchStrings, searchString);
  }

  /**
   * Sets a string that will be scanned for and expected in the response body
   * from the server.
   *
   * If multiple search strings are required, one can either call this method
   * for each string, or pass in one string with pipe <code>|<code> delimiting
   * the individual search strings within the large string.
   *
   * @param searchString
   *          a case insensitive string expected in the server's response body
   */
  public void setResponseSearchStringIgnoreCase(String searchString) {
    if (_searchStringsNoCase == null) {
      _searchStringsNoCase = new ArrayList<String>();
    }
    addSearchStrings(_searchStringsNoCase, searchString);
  }

  /**
   * Sets a string that will be scanned for and should not be present in the
   * response body from the server.
   *
   * If multiple search strings are required, one can either call this method
   * for each string, or pass in one string with pipe <code>|<code> delimiting
   * the individual search strings within the large string.
   *
   * @param searchString
   *          a string that is not expected in the server's response body
   */
  public void setUnexpectedResponseSearchString(String searchString) {
    if (_uSearchStrings == null) {
      _uSearchStrings = new ArrayList<String>();
    }
    addSearchStrings(_uSearchStrings, searchString);
  }

  /**
   * Sets a string or series of strings that will be searched for in the
   * response. If multiple search strings are required, one can either call this
   * method for each string, or pass in one string with pipe <code>|<code>
   * delimiting the individual search strings within the large string.
   *
   * @param searchString
   *          a string that is not expected in the server's response body
   */
  public void setUnorderedSearchString(String searchString) {
    if (_unorderedSearchStrings == null) {
      _unorderedSearchStrings = new ArrayList<String>();
    }
    addSearchStrings(_unorderedSearchStrings, searchString);
  }

  /**
   * Returns the list of search strings.
   * 
   * @return the list of search strings.
   */
  public List<String> getUnorderedSearchStrings() {
    return _unorderedSearchStrings;
  }

  /**
   * Returns the response for this particular test case.
   *
   * @return an HttpResponse object
   */
  public HttpResponse getResponse() {
    return _response;
  }

  /**
   * Returns an array of Header objects that are expected to be found in the
   * responst.
   *
   * @return an array of headers
   */
  public Header[] getExpectedHeaders() {
    if (_expected == null) {
      return null;
    }
    return _expected.values().toArray(new Header[_expected.size()]);
  }

  /**
   * Returns an array of Header objects that are not expected to be found in the
   * responst.
   *
   * @return an array of headers
   */
  public Header[] getUnexpectedHeaders() {
    if (_unexpected == null) {
      return null;
    }
    return _unexpected.values().toArray(new Header[_unexpected.size()]);
  }

  /**
   * Returns the status code expected to be found in the server's response
   *
   * @return status code
   */
  public String getStatusCode() {
    return _statusCode;
  }

  /**
   * Returns the reason phrase that is expected to be found in the server's
   * response.
   *
   * @return reason phrase
   */
  public String getReasonPhrase() {
    return _reasonPhrase;
  }

  /**
   * Returns the configured list of strings that will be used when scanning the
   * server's response.
   *
   * @return list of Strings
   */
  public List<String> getSearchStrings() {
    if (_searchStrings == null) {
      return null;
    }
    return _searchStrings;
  }

  /**
   * Returns the configured list of strings that will be used when scanning the
   * server's response.
   *
   * @return list of case insensitive Strings
   */
  public List<String> getSearchStringsNoCase() {
    if (_searchStringsNoCase == null) {
      return null;
    }
    return _searchStringsNoCase;
  }

  /**
   * Returns the configured list of strings that will be used when scanning the
   * server's response (these strings are not expected in the response).
   *
   * @return list of Strings
   */
  public List<String> getUnexpectedSearchStrings() {
    if (_uSearchStrings == null) {
      return null;
    }
    return _uSearchStrings;
  }

  /**
   * Returns an indicator on whether a response body is expected or not.
   *
   * @return boolean value
   */
  public boolean getExpectResponseBody() {
    return _expectResponseBody;
  }

  /**
   * Returns the HttpRequest for this particular test case.
   *
   * @return HttpRequest of this test case
   */
  public HttpRequest getRequest() {
    return _request;
  }

  /**
   * Returns the path to the goldenfile.
   *
   * @return path to the goldenfile
   */
  public String getGoldenfilePath() {
    return _goldenfilePath;
  }

  /**
   * Returns the state for this particular test case.
   *
   * @return test state
   */
  public Object getState() {
    if (_response != null) {
      return _response.getState();
    } else {
      // an initial request for state
      return _request.getState();
    }
  }

  /**
   * Sets the state for this test case.
   *
   * @param state
   *          test state
   */
  public void setState(Object state) {
    _request.setState((HttpState) state);
  }

  /**
   * Sets a logical name for this test case.
   *
   * @param name
   *          the logical name for this test
   */
  public void setName(String name) {
    _name = name;
  }

  /**
   * Returns the logical name for this test case.
   *
   * @return test name
   */
  public String getName() {
    return _name;
  }

  /**
   * Sets the validation strategy for this test case instance.
   *
   * @param validator
   *          - the fully qualified class name of the response validator to use.
   */
  public void setStrategy(String validator) {
    ValidationStrategy strat = ValidationFactory.getInstance(validator);
    if (strat != null) {
      _strategy = strat;
    } else {
      TestUtil.logMsg("[WebTestCase][WARNING] An attempt was made to use a "
          + "non-existing validator (" + validator + ")"
          + ".  Falling back to the TokenizedValidator");
    }
  }

  /**
   * Returns the class name of the response validator used.
   *
   * @return the fully qualified class of the validator used
   */
  public String getStrategy() {
    return _strategy.getClass().getName();
  }

  /*
   * Private Methods
   * ===========================================================================
   */

  /**
   * Utility method to add headers to test case holder objects.
   *
   * @param map
   *          the object in which to add header to
   * @param headerString
   *          String representation of a header in the form of
   *          <headername>:<value>
   */
  private void addHeader(Map<String, Header> map, String headerString) {
    TestUtil.logTrace(
        "[WebTestCase] addHeader utility method called: " + headerString);
    StringTokenizer st = new StringTokenizer(headerString, "|");
    while (st.hasMoreTokens()) {
      String head = st.nextToken();
      int colIdx = head.indexOf(':');
      String name = head.substring(0, colIdx).trim();
      String value = head.substring(colIdx + 1).trim();
      TestUtil
          .logTrace("[WebTestCase] Adding test header: " + name + ", " + value);
      Header header = map.get(name);
      if (header != null) {
        map.put(name, createNewHeader(value, header));
      } else {
        map.put(name, new Header(name, value));
      }
    }
  }

  /**
   * Creates a new header based of the provided header and value.
   * 
   * @param newValue
   *          - the new value to add to an existing header
   * @param header
   *          - the original header
   * @return new Header
   */
  private Header createNewHeader(String newValue, Header header) {
    String oldValue = header.getValue();
    return new Header(header.getName(), oldValue + ", " + newValue);
  }

  /**
   * Adds a search string to the provided list
   * 
   * @param stringList
   *          - list to add the string to
   * @param s
   *          - the search string
   */
  private void addSearchStrings(List<String> stringList, String s) {
    StringTokenizer st = new StringTokenizer(s, "|");
    while (st.hasMoreTokens()) {
      stringList.add(st.nextToken());
    }
  }
}
