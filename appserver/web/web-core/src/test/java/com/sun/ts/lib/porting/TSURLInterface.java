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

package com.sun.ts.lib.porting;

import java.net.*;

/**
 * An implementation of the TSURLInterface is to be used for Java EE TCK
 * testing. TS tests use this interface to obtain the URL String to use to
 * access a given web component. If a given Java EE Server implmentation
 * requires that URLs be created in a different manner, then this implementation
 * can be replaced.
 *
 * @author Kyle Grucci
 */
public interface TSURLInterface {
  /**
   * This method is called by TS tests to get the URL to use to access a given
   * web component.
   *
   * @param protocol
   *          - the name of the protocol.
   * @param host
   *          - the name of the host.
   * @param port
   *          - the port number.
   * @param file
   *          - the host file.
   * @return a valid URL object.
   */
  public URL getURL(String protocol, String host, int port, String file)
      throws MalformedURLException;

  /**
   * This method is called by TS tests to get the URL to use to access a given
   * web component.
   *
   * @param protocol
   *          - the name of the protocol.
   * @param host
   *          - the name of the host.
   * @param port
   *          - the port number.
   * @param file
   *          - the host file.
   * @return a valid URL as a String.
   */
  public String getURLString(String protocol, String host, int port,
      String file);

  /**
   * This method is called by TS tests to get the request to use to access a
   * given web component.
   *
   * @param request
   *          - the request file.
   * @return a valid String object.
   */
  public String getRequest(String request);
}
