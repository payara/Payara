/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import static org.testng.AssertJUnit.assertNotNull;

/**
 *
 * @author Tom Mueller
 */
public class NucleusTestUtils {
    private static final int DEFAULT_TIMEOUT_MSEC = 480000; // 8 minutes
    private static boolean verbose = true;
    protected static final File nucleusRoot = initNucleusRoot();
    
    private static File initNucleusRoot() {
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
    private NucleusTestUtils() {
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
        List<String> command = new ArrayList<String>();
        command.add(cmd.toString());
        command.add("--echo");
        command.addAll(Arrays.asList(args));

        ProcessManager pm = new ProcessManager(command);

        // the tests may be running unattended -- don't wait forever!
        pm.setTimeoutMsec(timeout);
        pm.setEcho(false);
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

        NadminReturn ret = new NadminReturn();
        ret.out = pm.getStdout();
        ret.err = pm.getStderr() + myErr;
        ret.outAndErr = ret.out + ret.err;
        ret.returnValue = exit == 0 && validResults(ret.out,
                String.format("Command %s failed.", args[0]));
        write(ret.out);
        write(ret.err);
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


    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
    
    /*
     * Returns true if String b contains String a.
     */
    public static boolean matchString(String a, String b) {
        return b.indexOf(a) != -1;
    }

    public static String getURL(String urlstr) {
        try {
            URL u = new URL(urlstr);
            URLConnection urlc = u.openConnection();
            BufferedReader ir = new BufferedReader(new InputStreamReader(urlc.getInputStream(),
                    "ISO-8859-1"));
            StringWriter ow = new StringWriter();
            String line;
            while ((line = ir.readLine()) != null) {
                ow.write(line);
                ow.write("\n");
            }
            ir.close();
            ow.close();
            return ow.getBuffer().toString();
        }
        catch (IOException ex) {
            System.out.println("unable to fetch URL:" + urlstr + ", reason: " + ex.getMessage());
            return "";
        }
    }

    
    // simple C-struct -- DIY
    public static class NadminReturn {
        public boolean returnValue;
        public String out;
        public String err;
        public String outAndErr;
    }

}
