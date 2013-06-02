/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.enterprise.universal.process;

import java.io.*;
import java.lang.management.ManagementFactory;

import com.sun.enterprise.universal.io.*;
import com.sun.enterprise.util.*;
import java.util.*;

import static com.sun.enterprise.util.StringUtils.ok;

/**
 * Includes a somewhat kludgy way to get the pid for "me". Another casualty of
 * the JDK catering to the LEAST common denominator. Some obscure OS might not
 * have a pid! The name returned from the JMX method is like so: 12345
 *
 * @mycomputername where 12345 is the PID
 * @author bnevins
 */
public final class ProcessUtils {
    static final File jpsExe;
    static final String jpsName;
    static final File jstackExe;
    static final String jstackName;

    private ProcessUtils() {
        // all static class -- no instances allowed!!
    }

    // for informal testing.  Too difficult to make a unit test...
    public static void main(String[] args) {
        debug = true;
        for (String s : args) {
            String ret = killJvm(s);

            if (ret == null)
                ret = "SUCCESS!!";

            System.out.println(s + " ===> " + ret);
        }
    }

    /**
     * Look for <strong>name</strong> in the Path. If it is found and if it is
     * executable then return a File object pointing to it. Otherwise return nu
     *
     * @param name the name of the file with no path
     * @return the File object or null
     */
    public static File getExe(String name) {
        for (String path : paths) {
            File f = new File(path + "/" + name);

            if (f.canExecute()) {
                return SmartFile.sanitize(f);
            }
        }
        return null;
    }

    /**
     * Try and find the Process ID of "our" process.
     *
     * @return the process id or -1 if not known
     */
    public static int getPid() {
        return pid;
    }

    /**
     * Kill the process with the given Process ID.
     *
     * @param pid
     * @return a String if the process was not killed for any reason including
     * if it does not exist. Return null if it was killed.
     */
    public static String kill(int pid) {
        try {
            String pidString = Integer.toString(pid);
            ProcessManager pm = null;
            String cmdline;

            if (OS.isWindowsForSure()) {
                pm = new ProcessManager("taskkill", "/F", "/T", "/pid", pidString);
                cmdline = "taskkill /F /T /pid " + pidString;
            }
            else {
                pm = new ProcessManager("kill", "-9", "" + pidString);
                cmdline = "kill -9 " + pidString;
            }

            pm.setEcho(false);
            pm.execute();
            int exitValue = pm.getExitValue();

            if (exitValue == 0)
                return null;
            else
                return Strings.get("ProcessUtils.killerror", cmdline,
                        pm.getStderr() + pm.getStdout(), "" + exitValue);
        }
        catch (ProcessManagerException ex) {
            return ex.getMessage();
        }
    }

    /**
     * Kill the JVM with the given main classname. The classname can be
     * fully-qualified or just the classname (i.e. without the package name
     * prepended).
     *
     * @param pid
     * @return a String if the process was not killed for any reason including
     * if it does not exist. Return null if it was killed.
     */
    public static String killJvm(String classname) {
        List<Integer> pids = Jps.getPid(classname);
        StringBuilder sb = new StringBuilder();
        int numDead = 0;

        for (int p : pids) {
            String s = kill(p);
            if (s != null)
                sb.append(s).append('\n');
            else
                ++numDead;
        }
        String err = sb.toString();

        if (err.length() > 0 || numDead <= 0)
            return Strings.get("ProcessUtils.killjvmerror", err, numDead);
        return null;
    }

    /**
     * If we can determine it -- find out if the process that owns the given
     * process id is running.
     *
     * @param aPid
     * @return true if it's running, false if not and null if we don't know. I.e
     * the return value is a true tri-state Boolean.
     */
    public static Boolean isProcessRunning(int aPid) {
        try {
            if (OS.isWindowsForSure())
                return isProcessRunningWindows(aPid);
            else
                return isProcessRunningUnix(aPid);
        }
        catch (Exception e) {
            return null;
        }
    }
    //////////////////////////////////////////////////////////////////////////
    //////////     all private below     /////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////
    private static final int pid;
    private static final String[] paths;
   private static boolean debug;

   private static boolean isProcessRunningWindows(int aPid) throws ProcessManagerException {
        String pidString = Integer.toString(aPid);
        ProcessManager pm = new ProcessManager("tasklist", "/NH", "/FI", "\"pid eq " + pidString + "\"");
        pm.setEcho(false);
        pm.execute();
        String out = pm.getStdout() + pm.getStderr();

        /* output is either
         (1)
         INFO: No tasks running with the specified criteria.
         (2)
         java.exe                    3760 Console                 0     64,192 K
         */

        if (debug) {
            System.out.println("------------   Output from tasklist   ----------");
            System.out.println(out);
            System.out.println("------------------------------------------------");
        }

        if (ok(out)) {
            // check for java.exe because tasklist or some other command might
            // be reusing the pid. This isn't a guarantee because some other
            // java process might be reusing the pid.
            if (out.indexOf("java.exe") >= 0 && out.indexOf(pidString) >= 0)
                return true;
            else
                return false;
        }

        throw new ProcessManagerException("unknown");
    }

    private static Boolean isProcessRunningUnix(int aPid) throws ProcessManagerException {
        ProcessManager pm = new ProcessManager("kill", "-0", "" + aPid);
        pm.setEcho(false);
        pm.execute();
        int retval = pm.getExitValue();
        return retval == 0 ? Boolean.TRUE : Boolean.FALSE;
    }

    static {
        // variables named with 'temp' are here so that we can legally set the
        // 2 final variables above.

        int tempPid = -1;

        try {
            String pids = ManagementFactory.getRuntimeMXBean().getName();
            int index = -1;

            if (ok(pids) && (index = pids.indexOf('@')) >= 0) {
                tempPid = Integer.parseInt(pids.substring(0, index));
            }
        }
        catch (Exception e) {
            tempPid = -1;
        }
        // final assignment
        pid = tempPid;

        String tempPaths = null;

        if (OS.isWindows()) {
            tempPaths = System.getenv("Path");

            if (!ok(tempPaths))
                tempPaths = System.getenv("PATH"); // give it a try
        }
        else {
            tempPaths = System.getenv("PATH");
        }

        if (ok(tempPaths))
            paths = tempPaths.split(File.pathSeparator);
        else
            paths = new String[0];

        if (OS.isWindows()) {
            jpsName = "jps.exe";
            jstackName = "jstack.exe";
        }
        else {
            jpsName = "jps";
            jstackName = "jstack";
        }

        // byron sez:
        // looks VERY messy here.  Please feel free to clean up.  I just don't
        // want to invest the time to do it right now.

        final String javaroot = System.getProperty("java.home");
        final String relpath = "/bin";
        final File fhere1 = new File(javaroot + relpath + "/" + jpsName);
        final File fhere2 = new File(javaroot + relpath + "/" + jstackName);
        File fthere1 = new File(javaroot + "/.." + relpath + "/" + jpsName);
        File fthere2 = new File(javaroot + "/.." + relpath + "/" + jstackName);

        if (fhere1.isFile()) {
            jpsExe = SmartFile.sanitize(fhere1);
        }
        else if (fthere1.isFile()) {
            jpsExe = SmartFile.sanitize(fthere1);
        }
        else {
            jpsExe = null;
        }
        if (fhere2.isFile()) {
            jstackExe = SmartFile.sanitize(fhere2);
        }
        else if (fthere2.isFile()) {
            jstackExe = SmartFile.sanitize(fthere2);
        }
        else {
            jstackExe = null;
        }
    }
}
