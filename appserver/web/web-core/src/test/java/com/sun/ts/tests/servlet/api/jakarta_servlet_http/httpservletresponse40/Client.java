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

/*
 * $Id:$
 */
package com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletresponse40;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.Properties;

import com.sun.javatest.Status;
import com.sun.ts.lib.harness.EETest;
import com.sun.ts.lib.porting.TSURL;
import com.sun.ts.lib.util.TestUtil;
import com.sun.ts.lib.util.WebUtil;

public class Client extends EETest {

  private static final String CONTEXT_ROOT = "/servlet_jsh_httpservletresponse40_web";

  private static final String PROTOCOL = "http";

  private static final String WEBSERVERHOSTPROP = "webServerHost";

  private static final String WEBSERVERPORTPROP = "webServerPort";

  public static final String DELIMITER = "\r\n";

  public static final String ENCODING = "ISO-8859-1";

  private String hostname;

  private int portnum;

  private WebUtil.Response response = null;

  private String request = null;

  private TSURL tsurl = new TSURL();

  public static void main(String[] args) {
    Client theTests = new Client();
    Status s = theTests.run(args, System.out, System.err);
    s.exit();
  }

  /*
   * @class.setup_props: webServerHost; webServerPort;
   */
  public void setup(String[] args, Properties p) throws Fault {
    boolean pass = true;

    try {
      hostname = p.getProperty(WEBSERVERHOSTPROP);
      if (hostname == null)
        pass = false;
      else if (hostname.equals(""))
        pass = false;
      try {
        portnum = Integer.parseInt(p.getProperty(WEBSERVERPORTPROP));
      } catch (Exception e) {
        pass = false;
      }
    } catch (Exception e) {
      throw new Fault("setup failed:", e);
    }
    if (!pass) {
      TestUtil.logErr(
          "Please specify host & port of web server " + "in config properties: "
              + WEBSERVERHOSTPROP + ", " + WEBSERVERPORTPROP);
      throw new Fault("setup failed:");
    }

    System.out.println(hostname);
    System.out.println(portnum);
    logMsg("setup ok");
  }

  public void cleanup() throws Fault {
    TestUtil.logTrace("cleanup");
  }

  /*
   * @testName: TrailerTestWithHTTP10
   * 
   * @assertion_ids: servlet40:TrailerTestWithHTTP10;
   * 
   * @test_Strategy:
   */
  public void TrailerTestWithHTTP10() throws Fault {

    String response = simpleTest("TrailerTestWithHTTP10", "HTTP/1.0",
        "/TrailerTestServlet");
    if (response
        .indexOf("Get IllegalStateException when call setTrailerFields") < 0) {
      TestUtil.logErr(
          "The underlying protocol is HTTP 1.0, the IllegalStateException should be thrown");
      throw new Fault("TrailerTestWithHTTP10 failed.");
    }

  }

  /*
   * @testName: TrailerTestResponseCommitted
   * 
   * @assertion_ids: servlet40:TrailerTestResponseCommitted;
   * 
   * @test_Strategy:
   */
  public void TrailerTestResponseCommitted() throws Fault {

    String response = simpleTest("TrailerTestResponseCommitted", "HTTP/1.1",
        "/TrailerTestServlet2");
    if (response
        .indexOf("Get IllegalStateException when call setTrailerFields") < 0) {
      TestUtil.logErr(
          "The response has been committed, the IllegalStateException should be thrown");
      throw new Fault("TrailerTestResponseCommitted failed.");
    }

  }

  /*
   * @testName: TrailerTest
   * 
   * @assertion_ids: servlet40:TrailerTest;
   * 
   * @test_Strategy:
   */
  public void TrailerTest() throws Fault {
    String content = simpleTest("TrailerTest", "HTTP/1.1",
        "/TrailerTestServlet");
    // if (content.indexOf("Trailer: myTrailer") < 0) {
    // TestUtil.logErr("Can not find header, \"Trailer: myTrailer\"");
    // throw new Fault("TrailerTest failed.");
    // }
    int i = content.indexOf("Current trailer field: ");
    if (i < 0) {
      throw new Fault("TrailerTest failed.");
    }
    content = content.substring(i + "Current trailer field: ".length());
    String[] ss = content.split("\r\n");
    if (ss.length != 3) {
      throw new Fault("TrailerTest failed.");
    }
    int lastChunkSize = Integer.parseInt(ss[1], 16);
    if (lastChunkSize != 0 || !ss[0].trim().equals("myTrailer:foo")) {
      TestUtil.logErr("The current getTrailerFields is " + ss[0].trim() + 
          ", But expected getTrailerFields should be myTrailer:foo");
      throw new Fault("TrailerTest failed.");
    }
    String[] trailer = ss[2].split(":");
    if (trailer.length != 2 || !trailer[0].trim().equals("myTrailer")
        || !trailer[1].trim().equals("foo")) {
      TestUtil.logErr("Expected tailer should be myTrailer:foo");
      throw new Fault("TrailerTest failed.");
    }

  }

  private String simpleTest(String testName, String protocol,
      String servletPath) throws Fault {
    URL url;
    Socket socket = null;
    OutputStream output;
    InputStream input;

    try {
      url = new URL(
          "http://" + hostname + ":" + portnum + CONTEXT_ROOT + servletPath);
      TestUtil.logMsg("access " + url.toString());
      socket = new Socket(url.getHost(), url.getPort());
      socket.setKeepAlive(true);
      output = socket.getOutputStream();

      String path = url.getPath();
      StringBuffer outputBuffer = new StringBuffer();
      outputBuffer.append("POST " + path + " " + protocol + DELIMITER);
      outputBuffer.append("Host: " + url.getHost() + DELIMITER);
      outputBuffer.append("Content-Type: text/plain" + DELIMITER);
      outputBuffer.append("Content-Length: 3" + DELIMITER);
      outputBuffer.append(DELIMITER);
      outputBuffer.append("ABC");

      byte[] outputBytes = outputBuffer.toString().getBytes(ENCODING);
      output.write(outputBytes);
      output.flush();

      input = socket.getInputStream();
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      int read = 0;
      while ((read = input.read()) >= 0) {
        bytes.write(read);
      }
      String response = new String(bytes.toByteArray());
      TestUtil.logMsg(response);
      return response;
    } catch (Exception e) {
      TestUtil.logErr("Caught exception: " + e.getMessage());
      e.printStackTrace();
      throw new Fault(testName + " failed: ", e);
    } finally {
      try {
        if (socket != null)
          socket.close();
      } catch (Exception e) {
      }
    }
  }
}
