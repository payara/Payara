/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletresponse40;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class TrailerTestServlet extends HttpServlet {
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    Writer writer = resp.getWriter();
    if (!req.getProtocol().equals("HTTP/1.0")) {
      resp.setHeader("Transfer-Encoding", "chunked");
    }

    try {
      resp.setTrailerFields(() -> {
        Map m = new HashMap();
        m.put("myTrailer", "foo");
        return m;
      });
      writer.write("Current trailer field: ");
      resp.getTrailerFields().get().forEach((key, value) -> {
        try {
          writer.write(key + ":" + value);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (IllegalStateException e) {
      String s = "Get IllegalStateException when call setTrailerFields";
      resp.setHeader("Content-Length", String.valueOf(s.length()));
      writer.write(s);
    }
  }
}
