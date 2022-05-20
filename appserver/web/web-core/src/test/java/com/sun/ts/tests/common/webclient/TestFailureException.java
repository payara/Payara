/*
 * Copyright (c) 2007, 2018 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Signifies a failure at some point during a test cycle.
 */
public class TestFailureException extends java.lang.Exception {

  private static final long serialVersionUID = -4651996590051941456L;

  /**
   * Creates a new instance of <code>TestFailureException</code> without a
   * detailed message.
   */
  public TestFailureException() {
  }

  /**
   * Creates a new instance of <code>TestFailureException</code> containing the
   * root cause of the test failure.
   * 
   * @param t
   *          - root cause
   */
  public TestFailureException(Throwable t) {
    super(t);
  }

  /**
   * Creates a new instance of <code>TestFailureException</code> with the
   * specified detail message.
   * 
   * @param msg
   *          - the detail message.
   */
  public TestFailureException(String msg) {
    super(msg);
  }

  /**
   * Creates a new instance of <code>TestFailureException</code> with the
   * specified detail message, and the root cause of the test failure
   * 
   * @param msg
   *          - the detail message
   * @param t
   *          - root cause
   */
  public TestFailureException(String msg, Throwable t) {
    super(msg, t);
  }

  /**
   * Returns, if any, the root cause of this Exception.
   * 
   * @return the root cause of this exception, or null
   */
  public Throwable getRootCause() {
    return getCause();
  }

}
