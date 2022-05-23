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

package com.sun.ts.tests.common.webclient.log;

import org.apache.commons.logging.impl.SimpleLog;

import com.sun.ts.lib.util.TestUtil;

public class WebLog extends SimpleLog {

  /**
   * Construct a simple log with given name.
   *
   * @param name
   *          log name
   */
  public WebLog(String name) {
    super(name);
  }

  /**
   * <p>
   * Do the actual logging. This method assembles the message and then prints to
   * <code>System.err</code>.
   * </p>
   */
  protected void log(int type, Object message, Throwable t) {
    StringBuffer buf = new StringBuffer(64);
    // append log type
    buf.append("[WIRE] - ");

    // append the message
    buf.append(String.valueOf(message));

    if (t == null) {
      TestUtil.logTrace(buf.toString());
    } else {
      TestUtil.logTrace(buf.toString(), t);
    }
  }
}
