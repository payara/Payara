/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.util;

//JDK imports
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.lang.IllegalArgumentException;


/**
 * @author Kedar
 * @version 1.0
 * @deprecated Use ProcessManager instead
 */
public class ProcessExecutor {
    public static final long kDefaultTimeoutMillis = 600000;
    public static final long kSleepTime = 2000;
    private static final long DEFAULT_TIMEOUT_SEC = 600;
    private static final String NEWLINE = System.getProperty("line.separator");
    private long mTimeoutMilliseconds = 0;
    protected String[] mCmdStrings = null;
    protected File mOutFile = null;
    protected File mErrFile = null;
    private OutputStream mOutStream = null;
    private OutputStream mErrStream = null;
    private File mWorkingDir = null; //working directory
    private String[] mEnv = null; //environment
    private String[] mInputLines = null; // strings to set in process's InputStream (like from redirection)
    private int mExitValue = -1;
    private Process mSubProcess = null; // used to get handle to child process for ProcessManager funtionality
    private boolean mVerboseMode = false;
    private boolean retainExecutionLogs = false;
    private String lastExecutionOutputString = null;
    private String lastExecutionErrorString = null;
    private boolean bDebug = false;

    /**
     * Creates new ProcessExecutor
     */
    public ProcessExecutor(String[] cmd) {
        this(cmd, DEFAULT_TIMEOUT_SEC, null);
    }

    /**
     * Creates new ProcessExecutor
     */
    public ProcessExecutor(String[] cmd, String[] inputLines) {
        this(cmd, DEFAULT_TIMEOUT_SEC, inputLines);
    }

    /**
     * Creates new ProcessExecutor
     */
    public ProcessExecutor(String[] cmd, long timeoutSeconds) {
        this(cmd, timeoutSeconds, null);
    }

    public ProcessExecutor(String[] cmd, long timeoutSeconds, String[] inputLines) {
        this(cmd, timeoutSeconds, inputLines, null, null);
    }

    /**
     * Creates a new
     * <code> ProcessExecutor </code> that executes the given command.
     *
     * @param cmd String that has command name and its command line arguments
     * @param timeoutSeconds long integer timeout to be applied in seconds.
     * After this time if the process to execute does not end, it will be
     * destroyed.
     */
    public ProcessExecutor(String[] cmd, long timeoutSeconds, String[] inputLines,
            String[] env, File workingDir) {
        mCmdStrings = cmd;
        mInputLines = inputLines;
        mEnv = env;
        mWorkingDir = workingDir;
        char fwdSlashChar = '/';
        char backSlashChar = '\\';

        if (System.getProperty("Debug") != null) {
            // turn on debug, this option was added to help developers
            // debug the their code
            bDebug = true;
        }

        for (int i = 0; i < mCmdStrings.length; i++) {
            if (OS.isUnix()) {
                mCmdStrings[i] = mCmdStrings[i].replace(backSlashChar, fwdSlashChar);
            }
            else {
                mCmdStrings[i] = mCmdStrings[i].replace(fwdSlashChar, backSlashChar);
            }
        }
        mTimeoutMilliseconds = (long) timeoutSeconds * 1000;
    }

    /**
     * This is the setting after the fact that an instance of ProcessExecutor is
     * created. This is to be used in case the output and error of the last
     * execute call has to be retained for latter analysis.
     *
     * @param s boolean representing whether to retain, true means the buffers
     * will be retained, false otherwise.
     */
    public void setExecutionRetentionFlag(final boolean s) {
        this.retainExecutionLogs = s;
    }

    public boolean getExecutionRetentionFlag() {
        return (this.retainExecutionLogs);
    }

    /**
     * Returns the last LAST_BYTES bytes in the error stream of last execution
     * as a String, if the ProcessExecutor was configured properly. It may
     * return null if the retentionFlag is set to false.
     */
    public String getLastExecutionError() {
        return (this.lastExecutionErrorString);
    }

    /**
     * Returns the last LAST_BYTES bytes in the output stream of last execution
     * as a String, if the ProcessExecutor was configured properly. It may
     * return null if the retentionFlag is set to false.
     */
    public String getLastExecutionOutput() {
        return (this.lastExecutionOutputString);
    }

    private void init() throws ExecException {
        try {
            mOutFile = File.createTempFile("stdout", null);
            mOutFile.deleteOnExit();
            mErrFile = File.createTempFile("stderr", null);
            mErrFile.deleteOnExit();
        }
        catch (IllegalArgumentException iae) {
            deleteTempFiles();
            throw new ExecException("Internal error (util.ProcessExecutor.init()): " + iae.getMessage());
        }
        catch (IOException ioe) {
            deleteTempFiles();
            throw new ExecException(cannotCreateTempFiles());
        }
    }

    private final static String cannotCreateTempFiles() {
        return "Could not create temporary files - check "
                + System.getProperty("java.io.tmpdir")
                + " to see if its writeable and not-full";
    }

    private void deleteTempFiles() {
        if (mOutStream != null) {
            try {
                mOutStream.flush();
                mOutStream.close();
            }
            catch (IOException ioe) {
                // Ignore
            }
        }

        if (mErrStream != null) {
            try {
                mErrStream.flush();
                mErrStream.close();
            }
            catch (IOException ioe) {
                // Ignore
            }
        }
        if (mOutFile != null)
            delete(mOutFile);
        if (mErrFile != null)
            delete(mErrFile);
    }

    public void execute() throws ExecException {
        execute(false);
    }

    /*
     * Executes the command. Redirects the standard output and error streams
     * safely to files. This makes the subprocess NOT block or wait on buffers
     * getting flushed. This is done in a threaded manner. Note that the
     * subprocess will be killed if it does not end in given timeout.
     *
     * @throws ExecException if anything goes wrong in subprocess, or subprocess
     * terminates abruptly.
     */
    public String[] execute(boolean bReturnOutputLines) throws ExecException {
        return execute(bReturnOutputLines, true);
    }

    /**
     * Allows a subclass to control the error message returned when a non-zero
     * exit code is returned from a failed execution
     *
     * @return
     */
    protected String getExceptionMessage() {
        /*
         * read the error message from error file
         */
        String errorMessage = getFileBuffer(mErrFile);
        if (errorMessage.length() == 0) {
            errorMessage = "The Process Output: " + getLatestOutput(mOutFile);
        }
        return "abnormal subprocess termination: Detailed Message:" + errorMessage;
    }

    /*
     * Executes the command. Redirects the standard output and error streams
     * safely to files. This makes the subprocess NOT block or wait on buffers
     * getting flushed. This is done in a threaded manner. Note that the
     * subprocess will be killed if it does not end in given timeout.
     *
     * @throws ExecException if anything goes wrong in subprocess, or subprocess
     * terminates abruptly.
     */
    public String[] execute(boolean bReturnOutputLines, boolean bStartUpTimeLimit) throws ExecException {
        init();
        InputStream inputStream = null;
        try {

            if (bDebug) {
                System.out.println("\n**** Executing command:");
                for (int ii = 0; ii < mCmdStrings.length; ii++) {
                    System.out.println(mCmdStrings[ii]);
                }
            }

            mSubProcess = Runtime.getRuntime().exec(mCmdStrings, mEnv, mWorkingDir);
            if (mInputLines != null)
                addInputLinesToProcessInput(mSubProcess);
            if (!bReturnOutputLines)
                mOutStream = redirectProcessOutput(mSubProcess);
            else
                inputStream = mSubProcess.getInputStream(); //attach to input stream for later reading
            mErrStream = redirectProcessError(mSubProcess);

            // see if process should startup in a limited ammount of time
            // processes used by ProcessManager don't return
            if (bStartUpTimeLimit) {
                long timeBefore = System.currentTimeMillis();
                boolean timeoutReached = false;
                boolean isSubProcessFinished = false;
                boolean shouldBeDone = false;
                while (!shouldBeDone) {
                    sleep(kSleepTime);
                    long timeAfter = System.currentTimeMillis();
                    timeoutReached = (timeAfter - timeBefore) >= mTimeoutMilliseconds;
                    try {
                        mExitValue = mSubProcess.exitValue();
                        isSubProcessFinished = true;
                    }
                    catch (IllegalThreadStateException itse) {
                        isSubProcessFinished = false;
                        //ignore exception
                    }
                    shouldBeDone = timeoutReached || isSubProcessFinished;
                }
                if (!isSubProcessFinished) {
                    mSubProcess.destroy();
                    mExitValue = -255;
                    throw new ExecException("Subprocess timed out after " + mTimeoutMilliseconds + "mS");
                }
                else {
                    mExitValue = mSubProcess.exitValue();
                    if (debug()) {
                        System.out.println("Subprocess command line = " + a2s(mCmdStrings));
                        System.out.println("Subprocess exit value = " + mExitValue);
                    }
                    if (mExitValue != 0) {
                        mExitValue = mSubProcess.exitValue();
                        if (mExitValue != 0) {
                            throw new ExecException(getExceptionMessage());
                        }
                    }
                }
            }
        }
        catch (SecurityException se) {
            throw new ExecException(se.getMessage());
        }
        catch (IOException ioe) {
            throw new ExecException(ioe.getMessage());
        }
        finally {

            // retain buffers before deleting them
            retainBuffers();

            // only delete files if the time is limited
            // for processes that don't return, the temp files will remain
            if (bStartUpTimeLimit) {
                deleteTempFiles();
            }
        }

        if (bReturnOutputLines) {
            return getInputStrings(inputStream);
        }
        else {
            return null;
        }

    }

    /**
     * Get the exit value of the process executed. If this method is called
     * before process execution is complete (i.e. before execute() method has
     * returned, it will return -1. If sub process is terminated at timeout, the
     * method will return -255
     */
    public int getProcessExitValue() {
        return mExitValue;
    }

    private void addInputLinesToProcessInput(Process subProcess) throws ExecException {
        if (mInputLines == null)
            return;

        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(subProcess.getOutputStream())));

            for (int i = 0; i < mInputLines.length; i++) {
                if (bDebug) {
                    System.out.println("InputLine ->" + mInputLines[i] + "<-");
                }
                out.println(mInputLines[i]);
            }
            out.flush();
        }
        catch (Exception e) {
            throw new ExecException(e.getMessage());
        }
        finally {
            try {
                out.close();
            }
            catch (Throwable t) {
            }
        }
    }

    private String[] getInputStrings(InputStream inputStream) throws ExecException {
        if (inputStream == null)
            return null;
        BufferedReader in = null;
        ArrayList list = new ArrayList();
        String str;
        try {
            in = new BufferedReader(new InputStreamReader(inputStream));
            while ((str = in.readLine()) != null)
                list.add(str);
            if (list.size() < 1)
                return null;
            return (String[]) list.toArray(new String[list.size()]);

        }
        catch (Exception e) {
            throw new ExecException(e.getMessage());
        }
        finally {
            try {
                in.close();
            }
            catch (Throwable t) {
            }
        }
    }

    private OutputStream redirectProcessOutput(Process subProcess) throws ExecException {
        OutputStream out = null;
        try {
            InputStream in = subProcess.getInputStream();
            // Redirect stderr for verbose mode
            if (mVerboseMode) {
                // send output to stderr
                out = System.err;
            }
            else {
                // send to temp file
                out = new FileOutputStream(mOutFile);
            }

            new FlusherThread(in, out).start();
        }
        catch (Exception e) {
            throw new ExecException(e.getMessage());
        }
        return out;
    }

    private OutputStream redirectProcessError(Process subProcess) throws ExecException {
        OutputStream out = null;
        try {
            InputStream in = subProcess.getErrorStream();
            // Redirect stderr for verbose mode
            if (mVerboseMode) {
                // send output to stderr
                out = System.err;
            }
            else {
                // send to temp file
                out = new FileOutputStream(mErrFile);
            }
            new FlusherThread(in, out).start();
        }
        catch (Exception e) {
            throw new ExecException(e.getMessage());
        }
        return out;
    }

    public void setVerbose(boolean verbose) {
        mVerboseMode = verbose;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException ie) {
            //ignore exception
        }
    }

    /**
     * Returns the contents of a file as a String. It never returns a null. If
     * the file is empty, an empty string is returned.
     *
     * @param file the file to read
     */
    protected String getFileBuffer(File file) {
        final StringBuffer sb = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(NEWLINE);
            }
        }
        catch (Exception e) {
            //squelch the exception
        }
        finally {
            try {
                reader.close();
            }
            catch (Exception e) {
            }
        }
        return (sb.toString());
    }

    protected String getLatestOutput(final File f) {
        return (new RAFileReader(f).readLastBytesAsString());
    }

    public void retainBuffers() {
        if (this.retainExecutionLogs) {
            this.lastExecutionErrorString = this.getLatestOutput(this.mErrFile);
            this.lastExecutionOutputString = this.getLatestOutput(this.mOutFile);
        }
    }

    private boolean debug() {
        final String td = System.getProperty("java.io.tmpdir");
        final String n = "as_debug_process_executor"; // a debug hook
        final File f = new File(td, n);
        return (f.exists());
    }

    private String a2s(String[] a) {
        final StringBuffer s = new StringBuffer();
        if (a != null) {
            for (int i = 0; i < a.length; i++) {
                s.append(a[i]);
                s.append(" ");
            }
        }
        return (s.toString());
    }

    /*
     * bnevins, April 2012
     * I added this method to solve a FindBugs low-level issue about ignoring the
     * return value.
     */
    private static void delete(File f) {
        if(f != null && !f.delete()) {
            f.deleteOnExit();
        }
    }

    private static class RAFileReader {
        final File file;
        final static int LAST_BYTES = 16384;
        final static String RMODE = "r"; //read

        RAFileReader(final File file) {
            this.file = file;
        }

        String readLastBytesAsString() {
            final int n = getNumberOfBytes(LAST_BYTES);
            final StringBuffer sb = new StringBuffer();
            final long ln = file.length(); //if SecurityManager is not present, this is safe.
            if (ln == 0)
                return (sb.toString()); //nothing to read, file may not exist, is protected, is a directory etc.
            assert (n <= ln) : ("Asked to read number of bytes more than size of file");
            final long s = ln - n;
            return (readWithoutCheck(s));
        }

        private String readWithoutCheck(final long seekPos) {
            final StringBuffer sb = new StringBuffer();
            RandomAccessFile rf = null;
            int lines = 0;
            try {
                rf = new RandomAccessFile(file, RMODE);
                rf.seek(seekPos);
                String tmp = rf.readLine();
                while (tmp != null) {
                    lines++;
                    sb.append(tmp);
                    //sb.append(Character.LINE_SEPARATOR);
                    sb.append('\n'); // adding a newline character is going to add one extra byte
                    tmp = rf.readLine();
                }
            }
            catch (Exception e) {
                //e.printStackTrace(); //ignore
            }
            finally {
                try {
                    if (rf != null)
                        rf.close();
                }
                catch (Exception e) {
                }//ignore;
            }
            //System.out.println("ln-seekPos = " + (ln - seekPos) );
            //System.out.println("bytes = " + sb.toString().getBytes().length);
            //System.out.println("lines = " + lines);
            //assert ((ln - seekPos) == (sb.toString().getBytes().length + lines)) : "Wrong number of bytes read";
            return (sb.toString());
        }

        private int getNumberOfBytes(final int max) {
            final long ln = file.length();
            return (max >= ln ? (int) ln : max);
        }
    }

    // used for ProcessManager to watchdog subProcess
    public Process getSubProcess() {
        return mSubProcess;
    }

    public static void main(String args[]) {
        testProcessError();
    }

    /*
     * This method tests the condition of process throwing an error. On Unixlike
     * systems this throws an error in error file. On non-unixlike Systems it
     * will throw IOException for CreateProcess, which is desired
     */
    private static void testProcessError() {
        ProcessExecutor executor = new ProcessExecutor(
                new String[]{"/usr/bin/ls", "-wrongPARAMS123"});
        try {
            executor.execute();
        }
        catch (ExecException ee) {
            System.out.println(ee.getMessage());
        }
    }
}
/**
 * inner class to flush runtime.exec process so it doesn't hang
 */
class FlusherThread extends Thread {
    InputStream mInStream = null;
    OutputStream mOutStream = null;
    public static final int kSize = 1024;

    FlusherThread(InputStream in, OutputStream out) {
        mInStream = in;
        mOutStream = out;
    }

    public void run() {
        // check for null stream
        if (mInStream == null)
            return;

        // transfer bytes from input to output stream
        try {
            int byteCnt = 0;
            byte[] buffer = new byte[4096];
            while ((byteCnt = mInStream.read(buffer)) != -1) {
                if (mOutStream != null && byteCnt > 0) {
                    mOutStream.write(buffer, 0, byteCnt);
                    mOutStream.flush();
                }
                yield();
            }
        }
        catch (IOException e) {
            // ignore
        }
        finally {
            try {
                mOutStream.close();
            }
            catch (IOException ioe) {
                // ignore
            }
        }
    }
}
