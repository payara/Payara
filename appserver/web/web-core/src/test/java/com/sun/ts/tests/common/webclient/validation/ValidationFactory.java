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

package com.sun.ts.tests.common.webclient.validation;

import com.sun.ts.lib.util.TestUtil;

/**
 * Returns a ValidationStrategy instance used to validate a response against a
 * particular WebTestCase
 *
 * @author Ryan Lubke
 * @version %I%
 */
public class ValidationFactory {

  /**
   * Private constructor as all interaction with the class is through the
   * getInstance() method.
   */
  private ValidationFactory() {
  }

  /*
   * public methods
   * ========================================================================
   */

  /**
   * Returns a ValidationStrategy instance based on the available factory types.
   *
   * @param validator
   *          Validator instance to obtain
   * @return a ValidationStrategy instance or null if the instance could not be
   *         obtained.
   */
  public static ValidationStrategy getInstance(String validator) {
    try {
      Object o = Thread.currentThread().getContextClassLoader()
          .loadClass(validator).newInstance();
      if (o instanceof ValidationStrategy) {
        return (ValidationStrategy) o;
      }
    } catch (Throwable t) {
      TestUtil.logMsg("[ValidationFactory] Unable to obtain "
          + "ValidationStrategy instance: " + validator);
    }
    return null;
  }
}
