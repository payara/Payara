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

/*
 * $URL$ $LastChangedDate$
 */
package com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletresponse;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple Servlet-based error page that displays error related req attributes.
 */

public class ServletErrorPage extends HttpServlet {

  private static final String STATUS_CODE = RequestDispatcher.ERROR_STATUS_CODE; // "jakarta.servlet.error.status_code";

  private static final String EXCEPTION_TYPE = RequestDispatcher.ERROR_EXCEPTION_TYPE; //"jakarta.servlet.error.exception_type";

  private static final String MESSAGE = RequestDispatcher.ERROR_MESSAGE; //"jakarta.servlet.error.message";

  private static final String EXCEPTION = RequestDispatcher.ERROR_EXCEPTION;  //"jakarta.servlet.error.exception"

  private static final String REQUEST_URI = RequestDispatcher.ERROR_REQUEST_URI; //"jakarta.servlet.error.request_uri";

  private static final String SERVLET_NAME = RequestDispatcher.ERROR_SERVLET_NAME; //"jakarta.servlet.error.servlet_name"

  private static final String EXP_MESSAGE = "error page invoked";

  /**
   * Invoked by container
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    PrintWriter pw = res.getWriter();
    // display req attributes
    pw.println("Servlet Name: " + req.getAttribute(SERVLET_NAME));
    pw.println("Request URI: " + req.getAttribute(REQUEST_URI));
    pw.println("Status Code: " + req.getAttribute(STATUS_CODE));
    pw.println("Exception Type: " + req.getAttribute(EXCEPTION_TYPE));
    pw.println("Exception: " + req.getAttribute(EXCEPTION));
    pw.print("Message: ");
    if (((String) req.getAttribute(MESSAGE)).indexOf(EXP_MESSAGE) > -1) {
      pw.println(EXP_MESSAGE);
    }

  }
}
