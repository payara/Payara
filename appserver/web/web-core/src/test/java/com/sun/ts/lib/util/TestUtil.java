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

package com.sun.ts.lib.util;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.SimpleDateFormat;

/**
 * TestUtil is a final utility class responsible for implementing logging across
 * multiple VMs. It also contains many convenience methods for logging property
 * object contents, stacktraces, and header lines.
 *
 * @author Kyle Grucci
 *
 */
public final class TestUtil {
  public static boolean traceflag = true;

  // this can be set in TestUtil's start logging method!!
  public static String sTestName;

  public static final String[] EMPTY_STRING_ARRAY = new String[0];

  public static final int VM_UNDER_TEST = 0;

  public static final int VM_HARNESS = 1; // this is really the test client VM

  public static final int VM_JAVATEST = 2;

  public static final int DEBUG_OUTPUT_LEVEL = 2;

  public static final int NORMAL_OUTPUT_LEVEL = 3;

  public static final int ERROR_STREAM = 4;

  public static final int OUTPUT_STREAM = 5;

  public static String NEW_LINE = System.getProperty("line.separator", "\n");

  // by default so the testers don't have to do anything
  public static int iWhereAreWe = VM_UNDER_TEST;

  private static PrintWriter out = null;

  private static PrintWriter err = null;

  private static PrintWriter additionalWriter = null;

  private static ObjectOutputStream objectOutputStream = null;

  private static ObjectOutputStream objectInputStream = null;

  private static Socket socketOnRemoteVM = null;

  private static boolean bAlreadyInitialized = false;

  private static Object socketMutex = new Object();

  private static int portOfHarness = 2000;

  private static String hostOfHarness = "unset host";

  private static Vector vBuffereredOutput = new Vector();

  // Transaction Attribute Value Mapping Table
  private static final String UNRECOGNIZED_STATUS = "UNRECOGNIZED_STATUS";

  private static final String transactionTable[] = { "STATUS_ACTIVE", // 0
      "STATUS_MARKED_ROLLBACK", // 1
      "STATUS_PREPARED", // 2
      "STATUS_COMMITTED", // 3
      "STATUS_ROLLEDBACK", // 4
      "STATUS_UNKNOWN", // 5
      "STATUS_NO_TRANSACTION", // 6
      "STATUS_PREPARING", // 7
      "STATUS_COMMITTING", // 8
      "STATUS_ROLLING_BACK", // 9
  };

  // debug flag for printing TS harness debug output
  public static boolean harnessDebug;

  // hang onto the props that are passed in during logging init calls
  private static Properties testProps = null;

  private static SimpleDateFormat df = new SimpleDateFormat(
      "MM-dd-yyyy HH:mm:ss");

  static {
    harnessDebug = Boolean.getBoolean("cts.harness.debug");
  }

  /**
   * used by harness to log debug output to the standard output stream
   * 
   * @param s
   *          the output string
   */
  public static void logHarnessDebug(String s) {
    if (harnessDebug) {
      logHarness(s, null);
    }
  }

  /**
   * used by TSTestFinder and TSScript to log output to the standard output
   * stream
   *
   * @param s
   *          the output string
   * @param t
   *          a Throwable whose stacktrace gets printed
   */
  public static void logHarness(String s, Throwable t) {
    synchronized (System.out) {
      System.out.println(df.format(new Date()) + ":  Harness - " + s);
      logToAdditionalWriter(s, t);
      if (t != null) {
        t.printStackTrace();
      }
    }
  }

  public static void logHarness(String s) {
    logHarness(s, null);
  }

  private static void logToAdditionalWriter(String s) {
    logToAdditionalWriter(s, null);
  }

  private static void logToAdditionalWriter(String s, Throwable t) {
    if (additionalWriter != null)
      additionalWriter.println(s);
    if (t != null) {
      t.printStackTrace(additionalWriter);
    }
  }

  /**
   * This method returns the properties object
   *
   * @return the properties object
   */
  public static Properties getProperties() {
    if (testProps == null) {
      testProps = getPropsFromFile();
    }
    return testProps;
  }

  /**
   * This method returns the property value for the appropriate property key
   *
   * @param s
   *          the property name
   * @return the property value
   */
  public static String getProperty(String s) {
    Properties p = getProperties();
    return p.getProperty(s);
  }

  /**
   * This method returns the property value for the appropriate property key
   * 
   * @param s
   *          the property name
   * @return the property value
   */
  public static String getProperty(String s, String defaultValue) {
    Properties p = getProperties();
    return p.getProperty(s, defaultValue);
  }

  /**
   * returns the transaction status value as a String given its integer
   * representation
   *
   * @param status
   *          integer representation of a transaction status
   * @return string representation of a transaction status
   */
  public static String getTransactionStatus(int status) {
    if (status < 0 || status > transactionTable.length - 1)
      return UNRECOGNIZED_STATUS;
    else
      return transactionTable[status];
  }

  /**
   * prints the transaction status value as a String given its integer
   * representation
   *
   * @param status
   *          integer representation of a transaction status
   */
  public static void printTransactionStatus(int status) {
    logMsg("TRANSACTION_STATUS: " + getTransactionStatus(status));
  }

  // MilliSeconds Multiple
  public static final int MILLI = 1000;

  /**
   * pauses the calling thread for the specified number of seconds
   *
   * @param s
   *          number of seconds
   */
  public static void sleepSec(int s) {
    logTrace("Sleeping " + s + " seconds");
    try {
      Thread.sleep(s * MILLI);
    } catch (InterruptedException e) {
      logErr("Exception: " + e);
    }
  }

  /**
   * pauses the calling thread for the specified number of milliseconds
   *
   * @param s
   *          number of milliseconds
   */
  public static void sleep(int s) {
    sleepMsec(s);
  }

  /**
   * pauses the calling thread for the specified number of milliseconds
   *
   * @param s
   *          number of milliseconds
   */
  public static void sleepMsec(int s) {
    logTrace("Sleeping " + s + " milliseconds");
    try {
      Thread.sleep(s);
    } catch (InterruptedException e) {
      logErr("Exception: " + e);
    }
  }

  public static void flushStream() {
    synchronized (socketMutex) {
      try {
        objectOutputStream.flush();
      } catch (Throwable t) {
        // Ignore
        // System.out.println("EXCEPTION WHILE FLUSHING");
      }
    }
  }

  public static void writeObject(TestReportInfo info) {
    synchronized (socketMutex) {
      flushStream();
      try {
        objectOutputStream.writeObject(info);
        // System.out.println("WROTE: " + info.sOutput);
      } catch (Exception e) {
        // System.out.println("EXCEPTION WHILE WRITING: " + info.sOutput);
        synchronized (vBuffereredOutput) {
          vBuffereredOutput.addElement(info);
          // System.out.println("ADDED THIS STRING TO BUFFERED OUT: " +
          // info.sOutput);
        }
      }
      flushStream();
    }
  }

  private static void sendBufferedData() throws Exception {
    TestReportInfo tri = null;
    synchronized (vBuffereredOutput) {
      try {
        // logMsg("vBuffereredOutput size = " + vBuffereredOutput.size());
        synchronized (socketMutex) {
          for (int ii = 0; ii < vBuffereredOutput.size(); ii++) {
            tri = (TestReportInfo) vBuffereredOutput.elementAt(ii);
            writeObject(tri);
            // System.out.println("WRITING_bufferedoutput: " + tri.sOutput);
            // objectOutputStream.writeObject(tri);
            // objectOutputStream.flush();
            // logMsg("writing: " + ii);
            // logMsg("writing: " + tri.sOutput);
          }
        }
        // logMsg("wrote buffered output");
      } catch (Exception e) {
        throw e;
      } finally {
        vBuffereredOutput.removeAllElements();
        // logMsg("reinititialized buffered output vector");
      }
    }
  }

  private static final String PROPS_FILE_NAME = "-cts-props.txt";

  private static final String PROPS_FILE;

  static {
    String userName = System.getProperty("user.name");
    String tmpDir = System.getProperty("java.io.tmpdir",
        File.separator + "tmp");
    if (tmpDir.endsWith(File.separator)) {
      PROPS_FILE = tmpDir + userName + PROPS_FILE_NAME;
    } else {
      PROPS_FILE = tmpDir + File.separator + userName + PROPS_FILE_NAME;
    }
    System.out.println(
        "************************************************************");
    System.out.println("* props file set to \"" + PROPS_FILE + "\"");
    System.out.println(
        "************************************************************");
  }

  private static Properties getPropsFromFile() {
    FileInputStream in = null;
    Properties p = new Properties();
    try {
      in = new FileInputStream(PROPS_FILE);
      p.load(in);
    } catch (Exception e) {
      logErr("Error reading the Properties object", e);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception e) {
        }
      }
    }
    return p;
  }

  private static void savePropsToFile(Properties p) {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(PROPS_FILE);
      p.store(out, "CTS Test Properties File");
    } catch (Exception e) {
      logErr("Error saving the Properties object", e);
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (Exception e) {
        }
      }
    }
  }

  /**
   * This static method must be called once by each new remote VM. Once called,
   * a socket connection is created back to the host running the test harness.
   * All calls to logMsg, logErr, and logTrace are immediately sent back to the
   * harness host.
   *
   * @param p
   *          properties containing harness host, port, and trace flag
   * @exception RemoteLoggingInitException
   *              if an exception occurs while the server side is setting up the
   *              socket connection back to the client host
   */
  public static void init(Properties p) throws RemoteLoggingInitException {
    savePropsToFile(p); // persist properties to a disk file
    // System.out.println("INIT_CALLED");
    synchronized (socketMutex) {
      try {
        // TSPropertyManager.createTSPropertyManager(p);
        testProps = p;
        if (p.isEmpty()) {
          throw new RemoteLoggingInitException(
              "Init: Error - Empty properties object passed to TestUtil.init");
        }
        NEW_LINE = p.getProperty("line.separator");
        if (socketOnRemoteVM != null) {
          socketOnRemoteVM.close();
        }
        if (objectOutputStream != null) {
          objectOutputStream.close();
        }
        if (true) {
          // System.out.println("INIT_CALLED AND SOCKET = NULL");
          traceflag = Boolean
              .valueOf(p.getProperty("harness.log.traceflag", "true"))
              .booleanValue();
          hostOfHarness = p.getProperty("harness.host");
          portOfHarness = Integer
              .parseInt(p.getProperty("harness.log.port", "2000"));
          if (hostOfHarness == null) {
            throw new RemoteLoggingInitException(
                "Init: Error while trying to getProperty(harness.host) - returned null");
          }
          socketOnRemoteVM = new Socket(hostOfHarness, portOfHarness);
          objectOutputStream = new ObjectOutputStream(
              socketOnRemoteVM.getOutputStream());
          sendBufferedData();
          // logMsg("socketOnRemoteVM=null, renewed everything");
        } else {
          // we'll never get here now...
          // call flush to make sure that we still have an open connection.
          // if the client went away and a new client is being run, we will
          // get an IOException, in which case we will reconnect.
          // logMsg("socketOnRemoteVM != null, calling flush");
          // objectOutputStream.flush();
          // if this fails, then the connection is gone
          // this is better than flush, because flush seems to always fail
          TestReportInfo tri = new TestReportInfo("SVR: " + "Logging check",
              OUTPUT_STREAM, DEBUG_OUTPUT_LEVEL, null);
          objectOutputStream.writeObject(tri);
          // System.out.println("WROTE TEST OBJECT");
        }
      } catch (UnknownHostException e) {
        throw new RemoteLoggingInitException(
            "You must pass a valid host string to init()");
      } catch (IOException e) {
        // System.out.println("EXCEPTION WHILE WRITING TEST OBJECT");
        // the client VM may have shutdown, so establish a new connection
        try {
          // System.out.println("INIT_CALLED AND TRYING TO ESABLISH CONN. AFTER
          // IOEXCEPTION");
          traceflag = Boolean
              .valueOf(p.getProperty("harness.log.traceflag", "true"))
              .booleanValue();
          hostOfHarness = p.getProperty("harness.host");
          portOfHarness = Integer
              .parseInt(p.getProperty("harness.log.port", "2000"));
          if (hostOfHarness == null) {
            throw new RemoteLoggingInitException(
                "Init: Error while trying to getProperty(harness.host) - returned null");
          }
          socketOnRemoteVM.close();
          socketOnRemoteVM = new Socket(hostOfHarness, portOfHarness);
          objectOutputStream = new ObjectOutputStream(
              socketOnRemoteVM.getOutputStream());
          sendBufferedData();
          // logMsg("caught IOException from flush(), renewed everything");
        } catch (IOException e2) {
          e2.printStackTrace();
          throw new RemoteLoggingInitException(
              "IOException in TestUtil.init()");
        } catch (Exception e2) {
          e2.printStackTrace();
          throw new RemoteLoggingInitException(
              "got a random exception in init()");
        }
      } catch (NumberFormatException e) {
        throw new RemoteLoggingInitException(
            "You must pass a valid port number string to init()");
      } catch (Exception e) {
        e.printStackTrace();
        throw new RemoteLoggingInitException(
            "got a random exception in init()");
      }
    }
  }

  /*
   * This method is called by our harness code to allow code that is shared
   * between the harness and the tests and calls TestUtil logMsg and logTrace to
   * do the right thing inside of the JavaTest VM. These calls will call to our
   * logHarness methods.
   */
  public static void initJavaTest() {
    iWhereAreWe = VM_JAVATEST;
  }

  public static void setAdditionalWriter(PrintWriter pw) {
    iWhereAreWe = VM_JAVATEST;
    additionalWriter = pw;
  }

  /**
   * This static method must be called once by a VM which does not rely upon any
   * remote logging.
   *
   * param p properties containing harness trace flag
   */
  public static void initNoLogging(Properties p) {
    if (bAlreadyInitialized)
      return;

    testProps = p;
    NEW_LINE = p.getProperty("line.separator");
    traceflag = Boolean.valueOf(p.getProperty("harness.log.traceflag", "true"))
        .booleanValue();
    iWhereAreWe = VM_HARNESS;
    bAlreadyInitialized = true;
  }

  /**
   * This static method must be called once by the harness VM. Once called, a
   * serversocket begins listening for Remote VMs to connect on the port
   * specified by harness.log.port.
   *
   * @param p
   *          properties containing harness trace flag
   */
  public static void initClient(Properties p) {
    if (bAlreadyInitialized)
      return;
    // start listener thread
    try {
      testProps = p;
      NEW_LINE = p.getProperty("line.separator");
      traceflag = Boolean
          .valueOf(p.getProperty("harness.log.traceflag", "true"))
          .booleanValue();
      iWhereAreWe = VM_HARNESS;
      ServerSocket ss = getServerSocket(p);
      new Acceptor(ss);
      bAlreadyInitialized = true;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static ServerSocket getServerSocket(Properties p) throws IOException {
    ServerSocket result = null;
    int port = 2000;
    int retry = 10;
    final int delaySeconds = 1;
    try {
      port = Integer.parseInt(p.getProperty("harness.log.port", "2000"));
    } catch (NumberFormatException e1) {
      e1.printStackTrace();
      System.err.println("Invalid value for harness.log.port,"
          + " using default harness.log.port of " + port);
    }
    try {
      retry = Integer
          .parseInt(p.getProperty("harness.socket.retry.count", "10"));
    } catch (NumberFormatException e2) {
      e2.printStackTrace();
      System.err.println("Invalid value for harness.socket.retry.count,"
          + " using default harness.socket.retry.count of " + retry);
    }
    logTrace(
        "#######  Value of harness.socket.retry.count is \"" + retry + "\"");
    logTrace("#######  Value of harness.log.port is \"" + port + "\"");
    result = getServerSocket0(port, retry, delaySeconds);
    while (result == null) {
      port++;
      result = getServerSocket0(port, retry, delaySeconds);
    }
    p.setProperty("harness.log.port", Integer.toString(port));
    logTrace(
        "#######  Actual bind value of harness.log.port is \"" + port + "\"");
    return result;
  }

  private static ServerSocket getServerSocket0(int port, int retry,
      int delaySeconds) {
    ServerSocket result = null;
    for (int i = 0; i < retry; i++) {
      try {
        result = new ServerSocket(port);
        break;
      } catch (IOException e3) {
        try {
          Thread.sleep(delaySeconds * 1000);
        } catch (InterruptedException e4) {
        }
      }
    }
    return result;
  }

  /**
   * This static method must be called once by the harness VM in order to set
   * the output and error streams and the name of the current test.
   *
   * @param testName
   *          the currently running testname as specified in the source code
   *          tags
   * @param outStream
   *          stream printed to by the logMsg and logTrace methods
   * @param errStream
   *          stream printed to by the logErr methods
   */
  public static void setCurrentTest(String testName, PrintWriter outStream,
      PrintWriter errStream) {
    sTestName = testName;
    out = outStream;
    err = outStream;
  }

  /**
   * prints a string to the log stream. All tests should use this method for
   * standard logging messages
   *
   * @param s
   *          string to print to the log stream
   */
  public static void logMsg(String s) {
    if (iWhereAreWe == VM_JAVATEST) {
      logHarness(s);
    } else if (iWhereAreWe == VM_HARNESS) {
      synchronized (out) {
        // just print to the appropriate stream
        out.println(df.format(new Date()) + ":  " + s);
        out.flush();
      }
    } else {
      TestReportInfo tri = new TestReportInfo("SVR: " + s, OUTPUT_STREAM,
          NORMAL_OUTPUT_LEVEL, null);
      writeObject(
          tri); /*
                 * try { synchronized(socketMutex) { objectOutputStream.flush();
                 * objectOutputStream.writeObject(tri);
                 * objectOutputStream.flush(); //System.out.
                 * println("successfully wrote to objectOutputStream"); } }
                 * catch(Exception ex) { //System.out.
                 * println("got exception trying to write to objectOutputStream"
                 * ); //if we have any problem, buffer the data
                 * synchronized(vBuffereredOutput) {
                 * vBuffereredOutput.addElement(tri); } }
                 */

    }
  }

  /**
   * prints a string as well as the provided Throwable's stacktrace to the log
   * stream. All tests should use this method for standard logging messages
   *
   * @param s
   *          string to print to the log stream
   * @param t
   *          - throwable whose stacktrace gets printed*
   *
   */
  public static void logMsg(String s, Throwable t) {
    if (iWhereAreWe == VM_JAVATEST) {
      logHarnessDebug(s);
      if (t != null) {
        t.printStackTrace();
      }
    } else {
      if (iWhereAreWe == VM_HARNESS) {
        synchronized (out) {
          // just print to the appropriate stream
          out.println(df.format(new Date()) + ":  " + s);
          out.flush();
        }
      } else {
        TestReportInfo tri = new TestReportInfo("SVR: " + s, OUTPUT_STREAM,
            NORMAL_OUTPUT_LEVEL, null);
        writeObject(tri);
      }
      if (t != null) {
        printStackTrace(t);
      }
    }

  }

  /**
   * turns on/off debugging. Once on, all calls to the logTrace method result in
   * messages being printed to the log stream. If off, all logTrace calls are
   * not printed.
   *
   * @param b
   *          If <code>true</code>, debugging is on. If false, debugging is
   *          turned off.
   */
  public static void setTrace(boolean b) {
    traceflag = b;
  }

  /**
   * prints a debug string to the log stream. All tests should use this method
   * for verbose logging messages. Whether or not the string is printed is
   * determined by the last call to the setTrace method.
   *
   * @param s
   *          string to print to the log stream
   */
  public static void logTrace(String s) {
    logTrace(s, null);
  }

  /**
   * Prints a debug string as well as the provided Throwable's stacktrace. Use
   * this if certain exceptions are only desired while tracing.
   *
   * @param s
   *          - string to print to the log stream
   * @param t
   *          - throwable whose stactrace gets printed
   */
  public static void logTrace(String s, Throwable t) {
    if (traceflag) {
      if (iWhereAreWe == VM_JAVATEST) {
        logHarnessDebug(s);
      } else {
        if (iWhereAreWe == VM_HARNESS) {
          synchronized (out) {
            // just print to the appropriate stream
            if (s != null && s.startsWith("SVR-TRACE"))
              out.println(df.format(new Date()) + ":  " + s);
            else
              out.println(df.format(new Date()) + ":  TRACE: " + s);
          }
        } else {
          TestReportInfo tri = new TestReportInfo("SVR-TRACE: " + s,
              OUTPUT_STREAM, DEBUG_OUTPUT_LEVEL, null);
          writeObject(tri);
        }
      }
      if (t != null) {
        t.printStackTrace();
      }
    }
  }

  /**
   * prints an error string to the error stream. All tests should use this
   * method for error messages.
   *
   * @param s
   *          string to print to the error stream
   * @param e
   *          a Throwable whose stacktrace gets printed
   */
  public static void logErr(String s, Throwable e) {
    if (iWhereAreWe == VM_JAVATEST) {
      logHarness(s);
      if (e != null) {
        e.printStackTrace();
      }
    } else {
      if (iWhereAreWe == VM_HARNESS) {
        synchronized (err) {
          // just print to the appropriate stream
          if (s != null && s.startsWith("SVR-ERROR"))
            err.println(df.format(new Date()) + ":  " + s);
          else
            err.println(df.format(new Date()) + ":  ERROR: " + s);
        }
      } else {
        TestReportInfo tri = new TestReportInfo("SVR-ERROR: " + s, ERROR_STREAM,
            NORMAL_OUTPUT_LEVEL, null);
        writeObject(tri);
      }
      if (e != null) {
        printStackTrace(e);
      }
    }
  }

  /**
   * prints an error string to the error stream. All tests should use this
   * method for error messages.
   *
   * @param s
   *          string to print to the error stream
   */
  public static void logErr(String s) {
    logErr(s, null);
  }

  /**
   * prints the contents of a properties object to the logging stream
   *
   * @param p
   *          properties to print
   */
  public static void list(Properties p) {
    StringBuffer sb = new StringBuffer();
    if (p == null)
      return;
    sb.append("--- Property Listing ---").append(TestUtil.NEW_LINE);
    Enumeration e = p.propertyNames();
    String key = null;
    while (e.hasMoreElements()) {
      key = (String) e.nextElement();
      sb.append(key).append("=").append(p.getProperty(key))
          .append(TestUtil.NEW_LINE);
    }
    sb.append("--- End Property Listing ---").append(TestUtil.NEW_LINE);
    logTrace(new String(sb));
  }

  /**
   * prints the stacktrace of a Throwable to the logging stream
   *
   * @param e
   *          exception to print the stacktrace of
   */
  public static void printStackTrace(Throwable e) {
    if (e == null) {
      return;
    }
    try {
      StringWriter sw = new StringWriter();
      PrintWriter writer = new PrintWriter(sw);
      e.printStackTrace(writer);
      logErr(sw.toString());
      writer.close();
    } catch (Exception E) {
    }
  }

  /**
   * prints the stacktrace of a Throwable to a string
   *
   * @param e
   *          exception to print the stacktrace of
   */
  public static String printStackTraceToString(Throwable e) {
    String sTrace = "";
    if (e == null)
      return "";
    try {
      StringWriter sw = new StringWriter();
      PrintWriter writer = new PrintWriter(sw);
      e.printStackTrace(writer);
      sTrace = sw.toString();
      writer.close();
    } catch (Exception E) {
    }
    return sTrace;
  }

  /**
   * prints a line of asterisks to the logging stream
   */
  public static void separator2() {
    logMsg("**************************************************"
        + "******************************");
  }

  /**
   * prints a line of dashes to the logging stream
   */
  public static void separator1() {
    logMsg("--------------------------------------------------"
        + "------------------------------");
  }

  /**
   * Convience method to handle sucking in the data from a connection.
   */
  public static String getResponse(URLConnection connection)
      throws IOException {
    StringBuffer content;
    BufferedReader in;
    // set up the streams / readers
    InputStream instream = connection.getInputStream();
    InputStreamReader inreader = new InputStreamReader(instream);
    in = new BufferedReader(inreader);
    // data structures
    content = new StringBuffer(1024);
    char[] chars = new char[1024];
    int length = 0;
    // pull the data into the content buffer
    while (length != -1) {
      content.append(chars, 0, length);
      length = in.read(chars, 0, chars.length);
    }
    // return
    instream.close(); // john feb 16
    inreader.close(); // john feb 16
    in.close(); // john feb 16
    return content.toString();
  }

  /**
   * Loads any properties that might be in a given String.
   */
  public static Properties getResponseProperties(String string)
      throws IOException {
    Properties props;
    ByteArrayInputStream in;
    byte[] bytes;
    props = new Properties();
    bytes = string.getBytes();
    in = new ByteArrayInputStream(bytes);
    props.load(in);
    in.close();
    return props;
  }

  /**
   * One shot method to get Properties directly from a URLConnection.
   */
  public static Properties getResponseProperties(URLConnection connection)
      throws IOException {
    Properties props;
    String input;
    input = getResponse(connection);
    props = getResponseProperties(input);
    return props;
  }

  public static String toEncodedString(Properties args) {
    StringBuffer buf = new StringBuffer();
    Enumeration names = args.propertyNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      String value = args.getProperty(name);
      buf.append(URLEncoder.encode(name)).append("=")
          .append(URLEncoder.encode(value));
      if (names.hasMoreElements())
        buf.append("&");
    }
    return buf.toString();
  }

  public static URLConnection sendPostData(Properties p, URL url)
      throws IOException {
    TestUtil.logMsg("Openning url connection to: " + url.toString());
    URLConnection urlConn = url.openConnection();
    // Begin POST of properties to SERVLET
    String argString = TestUtil.toEncodedString(p);
    urlConn.setDoOutput(true);
    urlConn.setDoInput(true);
    urlConn.setUseCaches(false);
    DataOutputStream out = new DataOutputStream(urlConn.getOutputStream());
    out.writeBytes(argString);
    out.flush();
    out.close();
    // End POST
    return urlConn;
  }

  /**
   * Parse a the table name from the ddl string such as: "create table foo" or
   * "delete from foo"
   * 
   * @param value
   *          buffer to parse
   * @return The name of the table
   */
  public static String getTableName(String value) {
    String tableName = "";
    if (value != null) {
      tableName = value.trim();
      int pos = tableName.lastIndexOf(" ");
      tableName = tableName.substring(pos + 1);
    } else {
      TestUtil.logMsg("Error: Null value passed for table Name");
    }
    return (tableName);
  } // END -- getTableName

  public static String srcToDist(String src) {
    return replaceLastSrc(src, "dist");
  }

  public static String replaceLastSrc(String src, String replacement) {
    // find last index of /src/, remove "src", then replace it with replacement
    StringBuffer sbToConvert = new StringBuffer(src);
    int iStart = src.lastIndexOf("src");
    if (iStart != -1) {
      if (harnessDebug) {
        TestUtil.logHarnessDebug("Pre-converted src dir = " + sbToConvert);
      }
      sbToConvert.replace(iStart, iStart + 3, replacement);

      if (harnessDebug) {
        TestUtil.logHarnessDebug(
            "Converted " + replacement + " dir = " + sbToConvert);
      }
    }
    return sbToConvert.toString();
  }

  public static String getDistString() {
    // we may need to default to src until we are ready to convert for good
    return "dist";
  }

  public static String getRelativePath(String oldVal) {
    if (oldVal == null) {
      return oldVal;
    }
    String result = oldVal;
    oldVal = oldVal.replace('\\', '/');
    while (oldVal.endsWith("/")) {
      oldVal = oldVal.substring(0, oldVal.length() - 1);
    }
    if (oldVal.endsWith("/src")) {
      return result;
    }
    int pos = oldVal.indexOf("/src/");
    if (pos == -1) {
      pos = oldVal.indexOf("/dist/");
      if (pos == -1) {
        result = oldVal;
      } else {
        result = oldVal.substring(pos + 6); // len of '/dist/'
      }
    } else {
      result = oldVal.substring(pos + 5);
    }
    return result;
  }

  // Convert the given string of key-value-pair into a properties object
  //
  // for example :
  // DatabaseName=derbyDB:user=cts1:password=cts1:serverName=localhost:portNumber=1527
  //
  public static Properties strToProps(String strProps) {

    logTrace("Props String = " + strProps);
    Properties props = new Properties();
    String strArray[] = strProps.split(":"); // Split the given string into
                                             // array of key value pairs

    for (String keyValuePair : strArray) {
      String strArray2[] = keyValuePair.split("="); // Take the key value pair
                                                    // and store it into
                                                    // properties
      logTrace("Setting property " + strArray2[0] + " = " + strArray2[1]);
      props.setProperty(strArray2[0], strArray2[1]);
    }

    return props;

  }

  public static void printProperties(Properties props) {
    Set<String> propertyNames = props.stringPropertyNames();
    for (String key : propertyNames) {
      logTrace(key + " = " + props.getProperty(key));
    }
  }

}

// ======================= end of class TestUtil ======================

class Acceptor extends Thread {
  ServerSocket serverSocket;

  private Socket outputSocket = null;

  public Acceptor(ServerSocket ss) {
    serverSocket = ss;
    this.start();
  }

  public void run() {
    while (true) {
      try {
        outputSocket = serverSocket.accept();
        new SocketReader(outputSocket);
        // System.out.println("new connection!!!!!");
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }
}

class TestReportInfo implements Serializable {
  public int iDebugLevel = TestUtil.NORMAL_OUTPUT_LEVEL;

  public String sOutput = ""; // Constants.EMPTY_STRING;

  public Throwable exception = null;

  public int iStream = TestUtil.OUTPUT_STREAM;

  public TestReportInfo(String output, int stream, int level, Throwable e) {
    if (sOutput != null)
      sOutput = output;
    iDebugLevel = level;
    exception = e;
    iStream = stream;
  }
}

class SocketReader extends Thread {
  private Socket outputSocket = null;

  public SocketReader(Socket s) {
    outputSocket = s;
    this.start();
  }

  public void run() {
    ObjectInputStream objIn;
    TestReportInfo tri = null;
    try {
      objIn = new ObjectInputStream(outputSocket.getInputStream());
      // while((tri = (TestReportInfo)objIn.readObject()) != null)
      while (true) {
        tri = (TestReportInfo) objIn.readObject();
        if (tri.iDebugLevel == TestUtil.DEBUG_OUTPUT_LEVEL) {
          // System.out.println("about to call logTrace");
          TestUtil.logTrace(tri.sOutput);
        } else {
          if (tri.iStream == TestUtil.ERROR_STREAM) {
            if (tri.exception == null)
              TestUtil.logErr(tri.sOutput);
            else
              TestUtil.logErr(tri.sOutput, tri.exception);
            // System.out.println("about to call logErr");
          } else // assume outputstream
          {
            // System.out.println("about to call logMsg");
            TestUtil.logMsg(tri.sOutput);
          }
        }
      }
    } catch (EOFException e) {
      // do nothing since the eof broke us out of the loop
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    // cleanup socket no matter what happens
    /*
     * try { outputSocket.close(); outputSocket = null; } catch(IOException e) {
     * 
     * }
     */
  }
}
