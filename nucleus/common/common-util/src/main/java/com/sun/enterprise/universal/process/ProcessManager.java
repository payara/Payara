/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2024] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.universal.process;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * ProcessManager.java
 * Use this class for painless process spawning.
 * This class was specifically written to be compatable with 1.4
 * 
 * @author bnevins
 * Created on October 28, 2005, 10:08 PM
 */
public class ProcessManager {
    public ProcessManager(String... cmds) {
        cmdline = cmds;
        sb_out = new StringBuffer();
        sb_err = new StringBuffer();
    }

    ////////////////////////////////////////////////////////////////////////////
    public ProcessManager(List<String> Cmdline) {
        cmdline = new String[Cmdline.size()];
        cmdline = Cmdline.toArray(cmdline);
        sb_out = new StringBuffer();
        sb_err = new StringBuffer();
    }

    ////////////////////////////////////////////////////////////////////////////
    public final void setTimeoutMsec(int num) {
        if (num > 0) {
            timeout = num;
        }
    }

    public final void setEnvironment(String[] env) {
        this.env = env;
    }

    ////////////////////////////////////////////////////////////////////////////
    public final void setStdinLines(List<String> list) {
        if (list != null && !list.isEmpty()) {
            stdinLines = list.toArray(new String[0]);
        }
    }

    public final void waitForReaderThreads(boolean b) {
        waitForReaderThreads = b;
    }

    ////////////////////////////////////////////////////////////////////////////
    /** Should the output of the process be echoed to stdout?
     *
     * @param newEcho
     */
    public final void setEcho(boolean newEcho) {
        echo = newEcho;
    }

    ////////////////////////////////////////////////////////////////////////////
    public final int execute() throws ProcessManagerException {
        try {
            Runtime rt = Runtime.getRuntime();
            process = rt.exec(cmdline, env);
            readStream("stderr", process.getErrorStream(), sb_err);
            readStream("stdout", process.getInputStream(), sb_out);
            writeStdin();
            await();

            try {
                exit = process.exitValue();
            }
            catch (IllegalThreadStateException tse) {
                // this means that the process is still running...
                process.destroy();
                throw new ProcessManagerTimeoutException(tse);
            }
        }
        catch (ProcessManagerException pme) {
            throw pme;
        }
        catch (Exception e) {
            if (process != null) {
                process.destroy();
            }

            throw new ProcessManagerException(e);
        }
        finally {
            // Always wait for reader threads -- unless the boolean flag is false.
            // note that this won't block when there was a timeout because the process
            // has been forcibly destroyed above.
            doWaitForReaderThreads();
        }

        return exit;
    }

    ////////////////////////////////////////////////////////////////////////////
    public final String getStdout() {
        return sb_out.toString();
    }

    ////////////////////////////////////////////////////////////////////////////
    public final String getStderr() {
        return sb_err.toString();
    }

    ////////////////////////////////////////////////////////////////////////////
    public final int getExitValue() {
        return exit;
    }

    ////////////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        return Arrays.toString(cmdline);
    }

    ////////////////////////////////////////////////////////////////////////////
    private void writeStdin() throws ProcessManagerException {
        if (stdinLines == null || stdinLines.length <= 0) {
            return;
        }


        if(process == null) {
            throw new ProcessManagerException(Strings.get("null.process"));
        }

        try (PrintWriter pipe = new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)))) {
            for (String stdinLine : stdinLines) {
                debug("InputLine ->" + stdinLine + "<-");
                pipe.println(stdinLine);
            }
            pipe.flush();
        }
        catch (Exception e) {
            throw new ProcessManagerException(e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private void readStream(String name, InputStream stream, StringBuffer sb) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        Thread thread = new Thread(new ReaderThread(reader, sb, echo), name);
        threads.add(thread);
        thread.start();
    }

    ////////////////////////////////////////////////////////////////////////////
    private void await() throws InterruptedException, ProcessManagerException {
        if (timeout <= 0) {
            waitForever();
        }
        else {
            waitAwhile();
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private void waitForever() throws InterruptedException, ProcessManagerException {
        if(process == null) {
            throw new ProcessManagerException(Strings.get("null.process"));
        }

        process.waitFor();
    }

    ////////////////////////////////////////////////////////////////////////////
    private void waitAwhile() throws InterruptedException {
        {
            Thread processWaiter = new Thread(new TimeoutThread(process));
            processWaiter.start();
            processWaiter.join(timeout);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private void doWaitForReaderThreads() {
        if (waitForReaderThreads) {
            // wait for stdin and stderr to finish up
            for (Thread t : threads) {
                try {
                    t.join();
                }
                catch (InterruptedException ex) {
                    // nothing to do
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private static void debug(String s) {
        if (debugOn) {
            System.out.println(s);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    public static void main(String[] args) {
        try {
            if (args.length <= 0) {
                System.out.println("Usage: ProcessManager cmd arg1 arg2 ... argn");
                System.exit(1);
            }

            List<String> cmds = new ArrayList<>(Arrays.asList(args));

            ProcessManager pm = new ProcessManager(cmds);
            pm.execute();

            System.out.println("*********** STDOUT ***********\n" + pm.getStdout());
            System.out.println("*********** STDERR ***********\n" + pm.getStderr());
            System.out.println("*********** EXIT VALUE: " + pm.getExitValue());
        }
        catch (ProcessManagerException pme) {
            pme.printStackTrace();
        }
    }
    ////////////////////////////////////////////////////////////////////////////
    private String[] cmdline;
    private String[] env = null;
    private final StringBuffer sb_out;
    private final StringBuffer sb_err;
    private int exit = -1;
    private int timeout;
    private Process process;
    private boolean echo = true;
    private static final boolean debugOn = false;
    private String[] stdinLines;
    private List<Thread> threads = new ArrayList<>(2);
    private boolean waitForReaderThreads = true;
    ////////////////////////////////////////////////////////////////////////////

    static class ReaderThread implements Runnable {
        ReaderThread(BufferedReader Reader, StringBuffer SB, boolean echo) {
            reader = Reader;
            sb = SB;
            this.echo = echo;
        }

        @Override
        public void run() {
            try {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    sb.append(line).append('\n');

                    if (echo) {
                        System.out.println(line);
                    }
                }
            }
            catch (Exception e) {
            }
            ProcessManager.debug("ReaderThread exiting...");
        }
        private final BufferedReader reader;
        private final StringBuffer sb;
        private final boolean echo;
    }

    static class TimeoutThread implements Runnable {
        TimeoutThread(Process p) {
            process = p;
        }

        @Override
        public void run() {
            try {
                process.waitFor();
            }
            catch (Exception e) {
            }
            ProcessManager.debug("TimeoutThread exiting...");
        }
        private Process process;
    }
}
