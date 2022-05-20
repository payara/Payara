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

package com.sun.ts.tests.servlet.common.servlets;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * GenericTCKServlet.java
 *
 * Any test that would normally extend GenericServlet will instead extend this
 * class. This will provide a simple framework from invoking various tests
 * defined as methods within the servlet that extends this class.
 *
 * Created: Wed Jul 31 20:57:16 2002
 *
 * @version %I%
 */

public abstract class HttpTCKServlet extends HttpServlet {

  private static final String TEXT_PLAIN = "text/plain";

  /**
   * <code>TEST_HEADER</code> is the constant for the <code>testname</code>
   * header.
   */
  private static final String TEST_HEADER = "testname";

  /**
   * <code>TEST_ARGS</code> is an array of Classes used during reflection.
   */
  private static final Class[] TEST_ARGS = { HttpServletRequest.class,
      HttpServletResponse.class };

  /**
   * <code>init</code> initializes the servlet.
   *
   * @param config
   *          - <code>ServletConfig</code>
   */
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  /**
   * <code>invokeTest</code> uses reflection to invoke test methods in child
   * classes of this particular class.
   *
   * @param req
   *          - <code>HttpServletRequest</code>
   * @param res
   *          - <code>HttpServletResponse</code>
   * @exception ServletException
   *              if an error occurs
   */
  protected void invokeTest(HttpServletRequest req, HttpServletResponse res)
      throws ServletException {
    res.setContentType(TEXT_PLAIN);
    char[] temp = req.getParameter(TEST_HEADER).toCharArray();
    temp[0] = Character.toLowerCase(temp[0]);
    String test = new String(temp);

    try {
      Method method = this.getClass().getMethod(test, TEST_ARGS);
      method.invoke(this, new Object[] { req, res });
    } catch (InvocationTargetException ite) {
      throw new ServletException(ite.getTargetException());
    } catch (NoSuchMethodException nsme) {
      throw new ServletException("Test: " + test + " does not exist");
    } catch (Throwable t) {
      throw new ServletException("Error executing test: " + test, t);
    }
  }

  /**
   * A basic implementation of the <code>doGet</code> method which will call
   * invokeTest.
   *
   * @param req
   *          - <code>HttpServletRequest</code>
   * @param res
   *          - <code>HttpServletResponse</code>
   * @exception ServletException
   *              if an error occurs
   * @exception IOException
   *              if an IO error occurs
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    invokeTest(req, res);
  }

  /**
   * A basic implementation of the <code>doPost</code> method which will call
   * invokeTest.
   *
   * @param req
   *          - <code>HttpServletRequest</code>
   * @param res
   *          - <code>HttpServletResponse</code>
   * @exception ServletException
   *              if an error occurs
   * @exception IOException
   *              if an IO error occurs
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    invokeTest(req, res);
  }

}// HttpTCKServlet
