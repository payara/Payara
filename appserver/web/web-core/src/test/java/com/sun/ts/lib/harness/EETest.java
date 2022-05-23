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

package com.sun.ts.lib.harness;

import com.sun.ts.lib.util.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import com.sun.javatest.Status;
import com.sun.javatest.*;
import java.lang.annotation.Annotation;

/**
 * This abstract class must be extended by all clients of all J2EE-TS tests. All
 * implementations of this class must define a setup, cleanup, and
 * runtest(method names of runtest methods must match the 'testname' tag. EETest
 * uses reflection to invoke these methods which in turn, run the test(s) to
 * completion. Tests are assumed to pass, unless a Fault is thrown.
 *
 * @author Kyle Grucci
 */
public abstract class EETest implements Serializable {

  /*
   * Please do NOT change this class in an incompatible manner with respect to
   * serialization. Please see the serialization specification to determine what
   * is a compatible change versus incompatible. If you do need to change this
   * class in an incompatible manner you will need to rebuild the compat tests.
   * You should also increment the serialVersionUID field to denote that this
   * class is incompatible with older versions.
   */
  static final long serialVersionUID = -4235235600918875382L;

  transient protected PrintStream log;

  transient protected PrintStream err;

  protected String sTestCase;

  transient Status sTestStatus = Status.passed("");

  Class testClass = this.getClass();

  Object testClInst;

  // nCl is true if the test class is different from the client class.
  boolean nCl;

  boolean bUtilAlreadyInitialized;

  Vector vLeftOverTestArgs;

  protected int iLogDelaySeconds;

  // get the props from the args
  protected Properties getTestPropsFromArgs(String[] argv) {
    Properties p = new Properties();
    Properties ap;
    String sProp;
    String sVal;
    boolean bRunIndividualTest = false;
    vLeftOverTestArgs = new Vector();
    // load a props object if used with -p
    for (int ii = 0; ii < argv.length; ii++) {
      if (argv[ii].startsWith("-p") || argv[ii].startsWith("-ap")) {
        ap = initializeProperties(argv[++ii]);
        // add additional props to "p"
        Enumeration e = ap.propertyNames();
        String key;
        while (e.hasMoreElements()) {
          key = (String) e.nextElement();
          p.put(key, ap.getProperty(key));
        }
      } else if (argv[ii].startsWith("-d")) {
        sProp = argv[ii].substring(2, argv[ii].indexOf('='));
        sVal = argv[ii].substring(argv[ii].indexOf('=') + 1);
        p.put(sProp, sVal);
      } else if (argv[ii].equalsIgnoreCase("-t")) {
        sTestCase = argv[++ii];
        bRunIndividualTest = true;
      } else {
        // there must be args that the test needs,
        // so pass these on
        vLeftOverTestArgs.addElement(argv[ii]);
      }
    }
    if (bRunIndividualTest)
      p.setProperty("testName", sTestCase);
    return p;
  }

  private Method getSetupMethod(Class testClass, Method runMethod) {
    String[] s = {};
    Class[] setupParameterTypes = { s.getClass(),
        (new Properties()).getClass() };
    Method setupMethod = null;

    // first check for @SetupMethod annotation on run method
    Annotation annotation = runMethod.getAnnotation(SetupMethod.class);

    if (annotation != null) {
      try {
        TestUtil
            .logTrace("getSetupMethod - getting setup method from annotation");
        SetupMethod setupAnnotation = (SetupMethod) annotation;
        setupMethod = testClass.getMethod(setupAnnotation.name(),
            setupParameterTypes);
        TestUtil.logTrace(
            "getSetupMethod - setup method name: " + setupAnnotation.name());
      } catch (NoSuchMethodException e) {
        setTestStatus(Status.failed(
            "Could not find annotation defined setup method for testcase: "
                + sTestCase),
            e);
      }
    } else {
      TestUtil.logTrace("No setupMethod annotation present");
      try {
        // get setup method
        TestUtil.logTrace(
            "getSetupMethod - checking for testcase specific setup method:  "
                + sTestCase + "_setup");
        setupMethod = testClass.getMethod(sTestCase + "_setup",
            setupParameterTypes);
      } catch (NoSuchMethodException e2) {
        TestUtil.logTrace(
            "getSetupMethod - checking for default class specific setup method");

        // try calling the generic setup method
        try {
          setupMethod = testClass.getMethod("setup", setupParameterTypes);

        } catch (NoSuchMethodException e3) {
          setTestStatus(
              Status.failed(
                  "Could not find setup method for" + "testcase: " + sTestCase),
              e3);
        }
      } catch (RuntimeException re) {
        setTestStatus(
            Status.failed("Could not access the test case: " + sTestCase), re);
      } catch (ThreadDeath t) {
        throw t;
      } catch (Throwable t) {
        setTestStatus(Status.failed("Unexpected Throwable: " + t), t);
      }
    }
    return setupMethod;
  }

  private Method getRunMethod(Class testClass) {
    Method runMethod = null;
    try {
      // get run method
      TestUtil.logTrace("** IN getRunMethod: testClass=" + testClass.getName());
      TestUtil.logTrace("** IN getRunMethod: testname=" + sTestCase);
      runMethod = testClass.getMethod(sTestCase, (java.lang.Class[]) null);
    } catch (NoSuchMethodException e) {
      setTestStatus(
          Status.failed(
              "Could not find the run method" + "for test case: " + sTestCase),
          e);
    } catch (RuntimeException e) {
      setTestStatus(
          Status.failed("Could not access the test case: " + sTestCase), e);
    } catch (ThreadDeath t) {
      throw t;
    } catch (Throwable t) {
      setTestStatus(Status.failed("Unexpected Throwable: " + t), t);
    }
    return runMethod;
  }

  private Method getCleanupMethod(Class testClass, Method runMethod) {
    Method cleanupMethod = null;

    // first check for @CleanupMethod annotation on run method
    Annotation annotation = runMethod.getAnnotation(CleanupMethod.class);

    if (annotation != null) {
      try {
        TestUtil.logTrace(
            "getCleanupMethod - getting cleanup method from annotation");
        CleanupMethod cleanupAnnotation = (CleanupMethod) annotation;
        cleanupMethod = testClass.getMethod(cleanupAnnotation.name(),
            (java.lang.Class[]) null);
        TestUtil.logTrace("getCleanupMethod - cleanup method name: "
            + cleanupAnnotation.name());
      } catch (NoSuchMethodException e) {
        setTestStatus(Status.failed(
            "Could not find annotation defined cleanup method for testcase: "
                + sTestCase),
            e);
      }
    } else {
      TestUtil.logTrace("No cleanupMethod annotation present");
      try {
        // get cleanup method
        TestUtil.logTrace(
            "getCleanupMethod - checking for testcase specific cleanup method:  "
                + sTestCase + "_cleanup");
        cleanupMethod = testClass.getMethod(sTestCase + "_cleanup",
            (java.lang.Class[]) null);
      } catch (NoSuchMethodException e2) {
        TestUtil.logTrace(
            "getCleanupMethod - checking for default class specific cleanup method");

        // try calling the generic cleanup method
        try {
          cleanupMethod = testClass.getMethod("cleanup",
              (java.lang.Class[]) null);

        } catch (NoSuchMethodException e3) {
          setTestStatus(Status.failed(
              "Could not find cleanup method for" + "testcase: " + sTestCase),
              e3);
        }
      } catch (RuntimeException re) {
        setTestStatus(
            Status.failed("Could not access the test case: " + sTestCase), re);
      } catch (ThreadDeath t) {
        throw t;
      } catch (Throwable t) {
        setTestStatus(Status.failed("Unexpected Throwable: " + t), t);
      }
    }
    return cleanupMethod;
  }

  protected Properties initializeProperties(String sPropertiesFile) {
    FileInputStream propertyFileInputStream = null;
    Properties props = null;
    try {
      propertyFileInputStream = new FileInputStream(sPropertiesFile);
      props = new Properties();
      props.load(propertyFileInputStream);
    } catch (FileNotFoundException e) {
      TestUtil.logHarness("Could not find specified props file", e);
    } catch (IOException e) {
      TestUtil.logHarness("IOException while reading props file", e);
    } catch (Exception e) {
      TestUtil.logHarness("Exception while reading props file", e);
    } finally {
      try {
        if (propertyFileInputStream != null)
          propertyFileInputStream.close();
      } catch (IOException ex) {
        TestUtil.logHarness("IOException while closing props file", ex);
      }
    }
    return props;
  }

  /**
   * <p>
   * This method is only called when test are run outside of JavaTest. If a
   * testcase name is passed within argv, then that testcase is run. Otherwise,
   * all testcases within this implementation of EETest are run.
   * </p>
   * 
   * @param argv
   *          an array of arguments that a test may use
   * @param log
   *          Stream passed to TestUtil for standard loggin
   * @param err
   *          Writer passed to TestUtil for error logging
   * @return a Javatest {@link Status} object (passed or failed)
   */
  public Status run(String[] argv, PrintStream log, PrintStream err) {
    return run(argv, new PrintWriter(log, true), new PrintWriter(err, true));
  }

  /**
   * This method is only called when tests are run outside of JavaTest or if the
   * test is being run in the same VM as the harness. If a testcase name is
   * passed within argv, then that testcase is run. Otherwise, all testcases
   * within this implementation of EETest are run.
   *
   * @param argv
   *          an array of arguments that a test may use
   * @param log
   *          Writer passed to TestUtil for standard loggin
   * @param err
   *          Writer passed to TestUtil for error logging
   * @return a Javatest {@link Status} object (passed or failed)
   */
  public Status run(String[] argv, PrintWriter log, PrintWriter err) {
    Properties props;
    Status retStatus = Status.failed("No status set yet");
    // assign log and reference output streams
    // this.err = err;
    // this.log = log;
    props = getTestPropsFromArgs(argv);
    // get the # of secs we should delay to allow reporting to finish
    try {
      iLogDelaySeconds = Integer
          .parseInt(props.getProperty("harness.log.delayseconds", "0")) * 1000;
    } catch (NumberFormatException e) {
      // set the default if a number was not set
      iLogDelaySeconds = 0;
    }
    if (props.isEmpty())
      return Status.failed(
          "FAILED:  An error occurred while trying to load the test properties");
    // copy leftover args to an array and pass them on
    int iSize = vLeftOverTestArgs.size();
    if (iSize == 0) {
      argv = null;
    } else {
      argv = new String[iSize];
      for (int ii = 0; ii < iSize; ii++) {
        argv[ii] = (String) vLeftOverTestArgs.elementAt(ii);
      }
    }
    props.put("line.separator", System.getProperty("line.separator"));
    if (sTestCase == null)
      return runAllTestCases(argv, props, log, err);
    else {
      // need to pass these streams to the Local Reporter
      TestUtil.setCurrentTest(sTestCase, log, err);
      TestUtil.initClient(props);
      retStatus = getPropsReady(argv, props);
      try {
        Thread.sleep(iLogDelaySeconds);
      } catch (InterruptedException e) {
        logErr("Exception: " + e);
      }
      return retStatus;
    }
  }

  protected void setTestStatus(Status s, Throwable t) {
    // only set the status for the first failure
    if (sTestStatus.getType() == Status.PASSED)
      sTestStatus = s;
    if (t != null) {
      TestUtil.logErr(s.getReason());
      TestUtil.logErr("Exception at:  ", t);
    }
  }

  protected Status runAllTestCases(String[] argv, Properties p, PrintWriter log,
      PrintWriter err) {
    String[] sTestCases;
    int iPassedCount = 0;
    int iFailedCount = 0;
    TestUtil.initClient(p);
    try {
      sTestCases = getAllTestCases(p);
    } catch (Exception e) {
      e.printStackTrace();
      return Status
          .failed("An error occurred trying to get all" + "testcase methods.");
    }
    for (int ii = 0; ii < sTestCases.length; ii++) {
      sTestCase = sTestCases[ii];
      p.setProperty("testName", sTestCase);
      TestUtil.setCurrentTest(sTestCase, log, err);
      bUtilAlreadyInitialized = true;
      sTestStatus = Status.passed("");
      TestUtil.separator2();
      TestUtil.logMsg("Beginning Test:  " + sTestCases[ii]);
      TestUtil.separator2();
      sTestStatus = getPropsReady(argv, p);
      try {
        Thread.sleep(iLogDelaySeconds);
      } catch (InterruptedException e) {
        logErr("Exception: " + e);
      }
      if (sTestStatus.getType() == Status.PASSED) {
        sTestCases[ii] += "...........PASSED";
        iPassedCount++;
      } else {
        TestUtil.logMsg(sTestStatus.getReason());
        sTestCases[ii] += "...........FAILED";
        iFailedCount++;
      }
      TestUtil.separator2();
      TestUtil.logMsg("End Test:  " + sTestCases[ii]);
      TestUtil.separator2();
    }
    TestUtil.separator2();
    TestUtil.logMsg("Completed running " + sTestCases.length + " tests.");
    TestUtil.logMsg("Number of Tests Passed = " + iPassedCount);
    TestUtil.logMsg("Number of Tests Failed = " + iFailedCount);
    TestUtil.separator2();
    for (int ii = 0; ii < sTestCases.length; ii++) {
      TestUtil.logMsg(sTestCases[ii]);
    }
    if (iFailedCount > 0)
      return Status.failed("FAILED");
    else
      return Status.passed("PASSED");
  }

  /**
   * This method is only called from JavaTest to run a single testcase. All
   * properties are determined from the source code tags.
   *
   * @param argv
   *          an array of arguments that a test may use
   * @param p
   *          properties that are used by the testcase
   * @param log
   *          stream passed to TestUtil for standard logging
   * @param err
   *          stream passed to TestUtil for error logging
   * @return a Javatest Status object (passed or failed)
   */
  public Status run(String[] argv, Properties p, PrintWriter log,
      PrintWriter err) {
    // need to pass these streams to the Local Reporter
    sTestCase = p.getProperty("testName");
    TestUtil.setCurrentTest(sTestCase, log, err);
    TestUtil.initClient(p);
    return getPropsReady(argv, p);
  }

  protected Status getPropsReady(String[] argv, Properties p) {
    // we only do this if we're in the Harness VM. If we're on the server,
    // that means that we're executing a service test within a generic
    // vehicle. In that case, we just invoke the setup, run, and cleanup
    // methods.
    // trim any whitespace around the property values
    Enumeration enum1 = p.propertyNames();
    String key;
    String value;
    while (enum1.hasMoreElements()) {
      key = (String) enum1.nextElement();
      value = p.getProperty(key);
      if (value != null) {
        p.put(key, value.trim());
      }
      // TestUtil.logTrace("Trimming prop: " + key + ". Value = " + value);
    }
    // set testname just to be sure
    sTestCase = p.getProperty("testName");
    // The code below is to allow JCK service tests to be run from
    // ejb vehicles that have been bundled with an appclient
    // Need to get the setup(), run() and cleanup() methods in the
    // individual test classes -- not from the APIClient.class.
    // TestUtil.logTrace("**Got current testclass : " +
    // this.getClass().getName());
    // TestUtil.logTrace("*** Got testclass property : " +
    // p.getProperty("test_classname"));
    String sClass_name = p.getProperty("test_classname");
    if (sClass_name != null) {
      if ((p.getProperty("finder").trim()).equals("jck")) {
        if (!((this.getClass().getName()).equals((sClass_name.trim())))) {
          try {
            testClInst = Class.forName(sClass_name).newInstance();
            testClass = testClInst.getClass();
            nCl = true;
          } catch (Exception te) {
            te.printStackTrace();
          }
        }
      }
    }
    // set the harness.host prop so the server can initialize the
    // the TestUtil logging
    try {
      p.setProperty("harness.host",
          InetAddress.getLocalHost().getHostAddress());
    } catch (UnknownHostException e) {
      TestUtil.logErr("Could not get our hostname to send to the "
          + "server for testcase: " + sTestCase);
      return Status.failed("Could not get our hostname to send to the "
          + "server for testcase: " + sTestCase);
    }
    return run(argv, p);
  }

  // public RemoteStatus run(String[] argv, Properties p, int iDummy)
  // {
  // return new RemoteStatus(run(argv, p));
  // }
  /**
   * This run method is the one that actually invokes reflection to figure out
   * and invoke the testcase methods.
   *
   * @param argv
   *          an array of arguments that a test may use
   * @param p
   *          properties that are used by the testcase
   * @return a Javatest Status object (passed or failed)
   */
  public Status run(String[] argv, Properties p) {
    sTestStatus = Status.passed("");
    // Make sure we set the testname if we're in a generic vehicle
    sTestCase = p.getProperty("testName");
    // Class testClass = this.getClass();
    Method setupMethod, runMethod, cleanupMethod;

    // commented out as it's not currently used
    // Class testArgTypes[] = {};
    Object testArgs[] = { argv, p };
    TestUtil.logTrace("*** in EETest.run(argv,p)");
    if (sTestCase == null || sTestCase.equals("")) {
      // TestUtil.logTrace("*** in EETestrun(): testCase=null)");
      return Status.failed("Invalid test case name: " + sTestCase);
    } else {
      // The code below is to allow JCK service tests to be run from
      // ejb vehicles that have been bundled with an appclient
      // Need to get the setup(), run() and cleanup() methods in the
      // individual test classes -- not from the APIClient.class.
      // TestUtil.logTrace("**Got current testclass : " +
      // this.getClass().getName());
      // TestUtil.logTrace("*** Got testclass property : " +
      // p.getProperty("test_classname"));
      // if ( p.getProperty("test_classname") != null )
      // if (
      // !((this.getClass().getName()).equals((p.getProperty("test_classname").trim()))))
      // {
      // try {
      // testClInst=Class.forName(p.getProperty("test_classname")).newInstance();
      // testClass=testClInst.getClass();
      // nCl=true;
      // }catch ( Exception te ) {
      // te.printStackTrace();
      // }
      // }
      TestUtil.logTrace("TESTCLASS=" + testClass.getName());
      runMethod = getRunMethod(testClass);
      if (runMethod == null)
        TestUtil.logTrace("* RUN METHOD is null and is not found");
      else {
        TestUtil.logTrace("** GOT RUN METHOD!");
        TestUtil.logTrace("**runmethod=" + runMethod.getName());
      }
      TestUtil.logTrace("ABOUT TO GET SETUP METHOD!");
      setupMethod = getSetupMethod(testClass, runMethod);
      if (setupMethod == null)
        TestUtil.logTrace("SETUP METHOD not found");
      else
        TestUtil.logTrace("GOT SETUP METHOD!");

      cleanupMethod = getCleanupMethod(testClass, runMethod);
      if (cleanupMethod == null)
        TestUtil.logTrace("CLEANUP METHOD not found");
      else
        TestUtil.logTrace("GOT CLEANUP METHOD!");
      // if anything went wrong while getting our methods, return
      if (setupMethod == null || runMethod == null || cleanupMethod == null)
        return Status.failed("One of the test methods could not be"
            + "found for testcase: " + sTestCase);
      try {
        TestUtil.logTrace("ABOUT TO INVOKE SETUP METHOD!");
        // if new classname is true, use that class name instead of
        // "this" class.
        if (nCl)
          setupMethod.invoke(testClInst, testArgs);
        else
          setupMethod.invoke(this, testArgs);
        TestUtil.logTrace("INVOKED SETUP METHOD!");
      } catch (IllegalAccessException e) {
        setTestStatus(Status.failed(
            "Could not execute setup method" + "for test case: " + sTestCase),
            e);
      } catch (InvocationTargetException e) {
        setTestStatus(Status.failed(
            "Test case throws exception: " + e.getTargetException().toString()),
            e.getTargetException());
      } catch (RuntimeException e) {
        setTestStatus(
            Status.failed("Could not access the test case: " + e.toString()),
            e);
      } catch (ThreadDeath t) {
        throw t;
      } catch (Throwable t) {
        setTestStatus(Status.failed("Unexpected Throwable: " + t), t);
      }
      if (sTestStatus.getType() == Status.PASSED) {
        TestUtil.logTrace("ABOUT TO INVOKE EETEST RUN METHOD!");
        try {
          // if new classname is true, use that class name instead of
          // "this" class.
          if (nCl)
            runMethod.invoke(testClInst, (java.lang.Object[]) null);
          else
            runMethod.invoke(this, (java.lang.Object[]) null);
        } catch (IllegalAccessException e) {
          setTestStatus(Status.failed(
              "Could not execute run method" + "for test case: " + sTestCase),
              e);
        } catch (InvocationTargetException e) {
          setTestStatus(
              Status.failed("Test case throws exception: "
                  + e.getTargetException().getMessage()),
              e.getTargetException());
        } catch (RuntimeException e) {
          setTestStatus(
              Status.failed("Could not access the test case: " + e.toString()),
              e);
        } catch (ThreadDeath t) {
          throw t;
        } catch (Throwable t) {
          setTestStatus(Status.failed("Unexpected Throwable: " + t), t);
        }
      }
      // call cleanup no matter what
      try {
        // if new classname is true, use that class name instead of
        // "this" class.
        if (nCl)
          cleanupMethod.invoke(testClInst, (java.lang.Object[]) null);
        else
          cleanupMethod.invoke(this, (java.lang.Object[]) null);
      } catch (IllegalAccessException e) {
        setTestStatus(
            Status.failed(
                "Could not execute cleanup method for test case: " + sTestCase),
            e);
      } catch (InvocationTargetException e) {
        setTestStatus(Status.failed("Test case throws exception: "
            + e.getTargetException().getMessage()), e.getTargetException());
        // Throwable t = e.getTargetException();
      } catch (RuntimeException e) {
        setTestStatus(
            Status.failed("Could not access the test case: " + e.toString()),
            e);
      } catch (ThreadDeath t) {
        throw t;
      } catch (Throwable t) {
        setTestStatus(Status.failed("Unexpected Throwable: " + t), t);
      }
    }
    return sTestStatus;
  }

  private String[] getAllTestCases(Properties p) throws SetupException {
    Vector tests = new Vector();
    String[] testMethods;
    try {
      // read in exclude list once per VM
      ExcludeListProcessor.readExcludeList(System.getProperty("exclude.list"));
      // setup a testname in a format that will macth the exclude list
      String sJavaTestName = "";
      String sVehicle;
      sVehicle = p.getProperty("vehicle");
      if (sVehicle != null) {
        // tack on "_from_<vehicle-name>"
        sVehicle = "_from_" + sVehicle;
      }
      // for all tests, prepend the relative path and
      // .java file
      String sClientClassName = this.getClass().getName();
      String sClientJavaName = sClientClassName
          .substring(sClientClassName.lastIndexOf('.') + 1) + ".java";
      String sCurrentDir = System.getProperty("current.dir");
      sCurrentDir = sCurrentDir.replace(File.separatorChar, '/');
      String sRelativeTestDir = sCurrentDir
          .substring(sCurrentDir.indexOf("tests/"));
      sRelativeTestDir = sRelativeTestDir
          .substring(sRelativeTestDir.indexOf("/") + 1);
      // make sure we have a trailing "/"
      if (!sRelativeTestDir.endsWith("/"))
        sRelativeTestDir += "/"; /*
                                  * Get public methods for this class Loop
                                  * through them to get methods that return
                                  * void, have no parameters, and contain "Test"
                                  * in their name.
                                  */

      Method[] methods = testClass.getMethods();
      for (int ii = 0; ii < methods.length; ii++) {
        Class[] paramTypes = methods[ii].getParameterTypes();

        // commented out as this is not currently used
        // Class returnType = methods[ii].getReturnType();

        // test that the parameter types match
        if ((paramTypes.length == 0))
        // &&
        // Void.class.isAssignableFrom(returnType))
        {
          String sName = methods[ii].getName();
          // test for our name requirements
          if ((sName.indexOf("Test") != -1 || sName.indexOf("test") != -1)
              && (sName.indexOf("Setup") == -1 && sName.indexOf("setup") == -1)
              && (sName.indexOf("Cleanup") == -1
                  && sName.indexOf("cleanup") == -1)) {
            // check here for excluded tests when running
            // outside of JavaTest
            sJavaTestName = sName + sVehicle;
            // construct the JavaTest recognizable testname
            sJavaTestName = sRelativeTestDir + sClientJavaName + "#"
                + sJavaTestName;
            // for all tests, check to see if it's excluded
            if (!ExcludeListProcessor.isTestExcluded(sJavaTestName))
              tests.addElement(sName);
            else
              System.out.println(sJavaTestName + " is excluded.");
            sJavaTestName = "";
          }
        }
      }
    } catch (SecurityException e) {
      throw new SetupException("Failed while getting all test methods: ", e);
    }
    /*
     * Check size of vector, if <= 0, no methods match signature if > 0, copy
     * values into testMethods array
     */
    if (tests.size() <= 0)
      throw new SetupException(
          "No methods match signature: " + "\"public void methodName()\"");
    testMethods = new String[tests.size()];
    for (int ii = 0; ii < testMethods.length; ii++) {
      testMethods[ii] = (String) tests.elementAt(ii);
    }
    return testMethods;
  }

  /**
   * prints a string to the TestUtil log stream. All tests should use this
   * method for standard logging messages
   *
   * @param msg
   *          string to print to the log stream
   */
  public void logMsg(String msg) {
    TestUtil.logMsg(msg);
  }

  /**
   * prints a debug string to the TestUtil log stream. All tests should use this
   * method for verbose logging messages. Whether or not the string is printed
   * is determined by the last call to the TestUtil setTrace method.
   *
   * @param msg
   *          string to print to the log stream
   */
  public void logTrace(String msg) {
    TestUtil.logTrace(msg);
  }

  /**
   * prints a string to the TestUtil error stream. All tests should use this
   * method for error messages
   *
   * @param msg
   *          string to print to the error stream
   */
  public void logErr(String msg) {
    TestUtil.logErr(msg);
  }

  /**
   * prints a string to the TestUtil error stream. All tests should use this
   * method for error messages
   *
   * @param msg
   *          string to print to the error stream
   * @param e
   *          a Throwable whose stacktrace gets printed
   */
  public void logErr(String msg, Throwable e) {
    TestUtil.logErr(msg, e);
  }

  /**
   * This exception must be thrown by all implentations of EETest to signify a
   * test failure. Overrides 3 printStackTrace methods to preserver the original
   * stack trace. Using setStackTraceElement() would be more elegant but it is
   * not available prior to j2se 1.4.
   *
   * @author Kyle Grucci
   */
  public static class Fault extends Exception {
    private static final long serialVersionUID = -1574745208867827913L;

    public Throwable t;

    /**
     * creates a Fault with a message
     */
    public Fault(String msg) {
      super(msg);
      TestUtil.logErr(msg);
    }

    /**
     * creates a Fault with a message.
     *
     * @param msg
     *          the message
     * @param t
     *          prints this exception's stacktrace
     */
    public Fault(String msg, Throwable t) {
      super(msg);
      this.t = t;
      // TestUtil.logErr(msg, t);
    }

    /**
     * creates a Fault with a Throwable.
     *
     * @param t
     *          the Throwable
     */
    public Fault(Throwable t) {
      super(t);
      this.t = t;
    }

    /**
     * Prints this Throwable and its backtrace to the standard error stream.
     *
     */
    public void printStackTrace() {
      if (this.t != null) {
        this.t.printStackTrace();
      } else {
        super.printStackTrace();
      }
    }

    /**
     * Prints this throwable and its backtrace to the specified print stream.
     *
     * @param s
     *          <code>PrintStream</code> to use for output
     */
    public void printStackTrace(PrintStream s) {
      if (this.t != null) {
        this.t.printStackTrace(s);
      } else {
        super.printStackTrace(s);
      }
    }

    /**
     * Prints this throwable and its backtrace to the specified print writer.
     *
     * @param s
     *          <code>PrintWriter</code> to use for output
     */
    public void printStackTrace(PrintWriter s) {
      if (this.t != null) {
        this.t.printStackTrace(s);
      } else {
        super.printStackTrace(s);
      }
    }

    @Override
    public Throwable getCause() {
      return t;
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {
      if (t != null)
        throw new IllegalStateException("Can't overwrite cause");
      if (!Exception.class.isInstance(cause))
        throw new IllegalArgumentException("Cause not permitted");
      this.t = (Exception) cause;
      return this;
    }
  }

  /**
   * This exception is used only by EETest. Overrides 3 printStackTrace methods
   * to preserver the original stack trace. Using setStackTraceElement() would
   * be more elegant but it is not available prior to j2se 1.4.
   *
   * @author Kyle Grucci
   */
  public static class SetupException extends Exception {
    private static final long serialVersionUID = -7616313680616499158L;

    public Exception e;

    /**
     * creates a Fault with a message
     */
    public SetupException(String msg) {
      super(msg);
    }

    /**
     * creates a SetupException with a message
     *
     * @param msg
     *          the message
     * @param e
     *          prints this exception's stacktrace
     */
    public SetupException(String msg, Exception e) {
      super(msg);
      this.e = e;
    }

    /**
     * Prints this Throwable and its backtrace to the standard error stream.
     *
     */
    public void printStackTrace() {
      if (this.e != null) {
        this.e.printStackTrace();
      } else {
        super.printStackTrace();
      }
    }

    /**
     * Prints this throwable and its backtrace to the specified print stream.
     *
     * @param s
     *          <code>PrintStream</code> to use for output
     */
    public void printStackTrace(PrintStream s) {
      if (this.e != null) {
        this.e.printStackTrace(s);
      } else {
        super.printStackTrace(s);
      }
    }

    /**
     * Prints this throwable and its backtrace to the specified print writer.
     *
     * @param s
     *          <code>PrintWriter</code> to use for output
     */
    public void printStackTrace(PrintWriter s) {
      if (this.e != null) {
        this.e.printStackTrace(s);
      } else {
        super.printStackTrace(s);
      }
    }

    @Override
    public Throwable getCause() {
      return e;
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {
      if (e != null)
        throw new IllegalStateException("Can't overwrite cause");
      if (!Exception.class.isInstance(cause))
        throw new IllegalArgumentException("Cause not permitted");
      this.e = (Exception) cause;
      return this;
    }
  }

}
