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

package com.sun.ts.tests.servlet.api.common.response;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HttpResponseTestServlet extends HttpServlet {

  private static final String TEST_HEADER = "testname";

  private static final Class[] HTTP_TEST_ARGS = { HttpServletRequest.class,
      HttpServletResponse.class };

  private static final Class[] TEST_ARGS = { ServletRequest.class,
      ServletResponse.class, };

  private static final Class[][] ALL_TYPES = { TEST_ARGS, HTTP_TEST_ARGS };

  public void init(ServletConfig servletConfig) throws ServletException {
    super.init(servletConfig);
  }

  public void service(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    String test = servletRequest.getParameter(TEST_HEADER).trim();
    Method method = null;
    for (int i = 0; i < ALL_TYPES.length; i++) {
      try {
        method = ResponseTests.class.getDeclaredMethod(test, ALL_TYPES[i]);
        break;
      } catch (NoSuchMethodException nsme) {
        ; // do nothing
      }
    }

    if (method != null) {
      invokeTest(method, new Object[] { servletRequest, servletResponse });
    } else {
      throw new ServletException("No such test: " + test);
    }

  }

  private void invokeTest(Method toBeInvoked, Object[] paramValues)
      throws ServletException {
    try {
      toBeInvoked.invoke(null, paramValues);
    } catch (InvocationTargetException ite) {
      throw new ServletException(ite.getTargetException());
    } catch (Throwable t) {
      throw new ServletException(
          "Error executing test: " + toBeInvoked.getName(), t);
    }
  }
}
