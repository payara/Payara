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

package com.sun.ts.lib.implementation.sun.common;

import java.net.*;
import com.sun.ts.lib.util.*;
import com.sun.ts.lib.porting.*;

/**
 * This is a J2EE Reference specific implementation of the TSURLInterface which
 * is to be used for J2EE-TS testing. TS tests use this interface to obtain the
 * URL String to use to access a given web component. If a given J2EE Server
 * implmentation requires that URLs be created in a different manner, then this
 * implementation can be replaced.
 *
 * @author Kyle Grucci
 */
public class SunRIURL implements TSURLInterface {
  private URL url = null;

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
      throws MalformedURLException {
    try {
      url = new URL(protocol, host, port, file);
    } catch (MalformedURLException e) {
      TestUtil.logErr("Failed during URL creation", e);
      throw e;
    }
    return url;
  }

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
      String file) {
    if (file.startsWith("/"))
      return protocol + "://" + host + ":" + port + file;
    else
      return protocol + "://" + host + ":" + port + "/" + file;
  }

  /**
   * This method is called by TS tests to get the request string to use to
   * access a given web component.
   *
   * @param request
   *          - the request file.
   * @return a valid String object.
   */
  public String getRequest(String request) {
    return request;
  }
}
