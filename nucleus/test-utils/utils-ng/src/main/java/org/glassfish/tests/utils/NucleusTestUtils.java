/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.tests.utils;

import com.sun.enterprise.universal.process.ProcessManager;
import com.sun.enterprise.universal.process.ProcessManagerException;
import com.sun.enterprise.universal.process.ProcessManagerTimeoutException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.testng.AssertJUnit.assertNotNull;

/**
 *
 * @author Tom Mueller
 */
public class NucleusTestUtils {
    protected static final int DEFAULT_TIMEOUT_MSEC = 480000; // 8 minutes
    private static boolean verbose = true;
    private static Map<String,String> env;
    private static String[] envp = null;
    private static String envProps[] = {
        "AS_ADMIN_PASSWORDFILE",
        "AS_LOGFILE",
        "AS_ADMIN_USER",
        "P_ADMIN_PASSWORDFILE",
        "P_ADMIN_USER",
        "P_ADMIN_READTIMEOUT",
        "C_ADMIN_READTIMEOUT",
    };
    protected static final File nucleusRoot = initNucleusRoot();

    private static File initNucleusRoot() {
        // Initialize the environment with environment variables that can be
        // used when running commands
        env = new HashMap<String,String>(System.getenv());
        for (String s : envProps) {
            String v = System.getProperty(s);
            if (v != null) {
                putEnv(s, v);
            }
        }

        String nucleusRootProp = System.getProperty("nucleus.home");
        if (nucleusRootProp == null) {
           String basedir = System.getProperty("basedir");
           assertNotNull(basedir);
           nucleusRootProp = basedir + "/../../distributions/nucleus/target/stage/nucleus";
        }
        System.out.println("nucleus.home=" + nucleusRootProp);
        return new File(nucleusRootProp);

    }

    // All methods are static, do not allow an object to be created.
    protected NucleusTestUtils() { }

    public static File getNucleusRoot() {
        return nucleusRoot;
    }

    public static void putEnv(String name, String value) {
        env.put(name, value);
        if (!env.isEmpty()) {
            envp = new String[env.size()];
            int i = 0;
            for (Map.Entry<String,String> me : env.entrySet()) {
                envp[i++] = me.getKey() + "=" + me.getValue();
            }
        } else {
            envp = null;
        }
    }
    /**
     * Runs the command with the args given
     *
     * @param args
     *
     * @return true if successful
     */
    public static boolean nadmin(int timeout, final String... args) {
        return nadminWithOutput(timeout, args).returnValue;
    }

    public static boolean nadmin(final String... args) {
        return nadmin(DEFAULT_TIMEOUT_MSEC, args);
    }

    /**
     * Runs the command with the args given
     * Returns the precious output strings for further processing.
     *
     * @param args
     *
     * @return true if successful
     */
    public static NadminReturn nadminWithOutput(final String... args) {
        return nadminWithOutput(DEFAULT_TIMEOUT_MSEC, args);
    }

    public static NadminReturn nadminWithOutput(final int timeout, final String... args) {
        File cmd = new File(nucleusRoot, isWindows() ? "bin/nadmin.bat" : "bin/nadmin");
        if (!cmd.canExecute()) {
            cmd = new File(nucleusRoot, isWindows() ? "bin/asadmin.bat" : "bin/asadmin");
                if (!cmd.canExecute()) {
                    cmd = new File(nucleusRoot, isWindows() ? "bin/padmin.bat" : "bin/padmin");
                }
        }
        return cmdWithOutput(cmd, timeout, args);
    }

    public static NadminReturn nadminDetachWithOutput( final String... args) {
            File cmd = new File(nucleusRoot, isWindows() ? "bin/nadmin.bat" : "bin/nadmin");
            if (!cmd.canExecute()) {
                cmd = new File(nucleusRoot, isWindows() ? "bin/asadmin.bat" : "bin/asadmin");
                if (!cmd.canExecute()) {
                    cmd = new File(nucleusRoot, isWindows() ? "bin/padmin.bat" : "bin/padmin");
                }
            }
            return cmdDetachWithOutput(cmd,DEFAULT_TIMEOUT_MSEC, args);
        }

    public static NadminReturn cmdWithOutput(final File cmd, final int timeout, final String... args) {
        List<String> command = new ArrayList<String>();
        command.add(cmd.toString());
        command.add("--echo");
        command.addAll(Arrays.asList(args));

        ProcessManager pm = new ProcessManager(command);

        // the tests may be running unattended -- don't wait forever!
        pm.setTimeoutMsec(timeout);
        pm.setEcho(false);
        pm.setEnvironment(envp);

        int exit;
        String myErr = "";
        try {
            exit = pm.execute();
        }
        catch (ProcessManagerTimeoutException tex) {
            myErr = "\nProcessManagerTimeoutException: command timed out after " + timeout + " ms.";
            exit = 1;
        }
        catch (ProcessManagerException ex) {
            ex.printStackTrace();
            myErr = "\n" + ex.getMessage();
            exit = 1;
        }

        NadminReturn ret = new NadminReturn(exit, pm.getStdout(), pm.getStderr() + myErr, args[0]);

        write(ret.outAndErr);
        return ret;
    }

    public static NadminReturn cmdDetachWithOutput(final File cmd, final int timeout, final String... args) {
            List<String> command = new ArrayList<String>();
            command.add(cmd.toString());
            command.add("--echo");
            command.add("--detach");
            command.addAll(Arrays.asList(args));

            ProcessManager pm = new ProcessManager(command);

            // the tests may be running unattended -- don't wait forever!
            pm.setTimeoutMsec(timeout);
            pm.setEcho(false);
            pm.setEnvironment(envp);

            int exit;
            String myErr = "";
            try {
                exit = pm.execute();
            }
            catch (ProcessManagerTimeoutException tex) {
                myErr = "\nProcessManagerTimeoutException: command timed out after " + timeout + " ms.";
                exit = 1;
            }
            catch (ProcessManagerException ex) {
                exit = 1;
            }

            NadminReturn ret = new NadminReturn(exit, pm.getStdout(), pm.getStderr() + myErr, args[0]);

            write(ret.outAndErr);
            return ret;
        }

    private static boolean validResults(String text, String... invalidResults) {
        for (String result : invalidResults) {
            if (text.contains(result)) {
                return false;
            }
        }
        return true;
    }

    private static void write(final String text) {
        if (verbose && !text.isEmpty())
            System.out.print(text);
    }


    protected static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    }

    /**
     * Returns true if String b contains String a.
     * Returns true if both strings are null.
     * Returns false if only one of the strings is null.
     *
     * @param a The possibly null string that must be contained
     * in b
     * @param b The possibly null string that must contain a
     * @return true if b contains a
     */
    public static boolean matchString(String a, String b) {
        if ((a == null) && (b == null)) return true;
        if (a == null) return false;
        if (b == null) return false;

        return b.indexOf(a) != -1;
    }

    /**
     * This methods opens a connection to the given URL and
     * returns the string that is returned from that URL.  This
     * is useful for simple servlet retrieval
     *
     * @param urlstr The URL to connect to
     * @return The string returned from that URL, or empty
     * string if there was a problem contacting the URL
     */
    public static String getURL(String urlstr) {
        // @todo Java SE 7 use try with resources
        StringWriter ow = null;
        BufferedReader ir = null;
        try {
            URL u = new URL(urlstr);
            URLConnection urlc = u.openConnection();
            ir = new BufferedReader(new InputStreamReader(urlc.getInputStream(),
                    "ISO-8859-1"));
            try {
                ow = new StringWriter();
                String line;
                while ((line = ir.readLine()) != null) {
                    ow.write(line);
                    ow.write("\n");
                }

                return ow.getBuffer().toString();
            }
            finally {
                if (ow != null) {
                    ow.close();
                }
            }
        }
        catch (IOException ex) {
            System.out.println("unable to fetch URL:" + urlstr + ", reason: " + ex.getMessage());
            return "";
        }
        finally {
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException ex) {
                    Logger.getLogger(NucleusTestUtils.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }


    // simple C-struct -- DIY
    public static class NadminReturn {
        NadminReturn(int exit, String out, String err, String cmd) {
            this.returnValue = exit == 0 && validResults(out,
                String.format("Command %s failed.", cmd));
            this.out = out;
            this.err = err;
            this.outAndErr = this.out + this.err;
        }

        public boolean returnValue;
        public String out;
        public String err;
        public String outAndErr;
    }

}
