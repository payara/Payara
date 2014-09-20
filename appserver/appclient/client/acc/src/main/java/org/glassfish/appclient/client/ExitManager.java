/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates the logic related to intercepting exceptions
 * and VM exits in a test environment and creating a disk file which records
 * the results.  Typically a test script would wait for the file to appear
 * and then read it to find out the results of the Java Web Start launch.
 * <p>
 * The report file format contains one line with the exit status value, then
 * possibly additional lines containing stack trace information.
 * <p>
 * The status is written to a temp file, then that file is renamed to the
 * selected status file.  This prevents a script waiting for the status file to
 * appear from seeing the file appear before it has been written and closed and
 * thereby not being able to read the status file's contents correctly.
 *
 * @author Tim Quinn
 */
class ExitManager implements Runnable {

    private final File tempStatusFile;
    private final File statusFile;

    private PrintWriter reportWriter;
    private CommentWriter commentWriter;
    private Throwable reportedFailure = null;
    private AtomicInteger reportedStatus = new AtomicInteger(0);

    private static final Logger logger = Logger.getLogger(ExitManager.class.getName());

    ExitManager(final String testReportLocation) {
        statusFile = new File(statusFileName(testReportLocation));
        tempStatusFile = new File(statusTempFileName(testReportLocation));

        prepareReportWriter(tempStatusFile);
        Runtime.getRuntime().addShutdownHook(new Thread(this));
        logger.log(Level.FINE, "ExitManager initialized");
    }

    /**
     * Executes when shutdown is in progress.  Writes any recorded exit status
     * to the status file and, if a failure has been reported, writes the
     * stack trace to the file as well.
     * <p>
     * The file is readable as a properties file.  (The stack trace is written
     * as comments.)
     */
    @Override
    public void run() {
        logger.log(Level.FINE, "ExitManager writing output");
        reportWriter.println("jws.exit.status=" + reportedStatus);
        if (reportedFailure != null) {
            reportedFailure.printStackTrace(commentWriter);
        }
        reportWriter.close();
        if ( ! tempStatusFile.renameTo(statusFile)) {
            throw new RuntimeException("Could not rename temp status file from " + 
                    tempStatusFile.getAbsolutePath() + " to " + 
                    statusFile.getAbsolutePath());
        }
    }


    private void prepareReportWriter(final File tempTestReportFile) {
        try {
            reportWriter = new PrintWriter(tempTestReportFile);
            commentWriter = new CommentWriter(reportWriter);
            logger.log(Level.FINE, "PrintWriter for temp exit file {0} ready",
                    tempTestReportFile.getAbsolutePath());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String statusFileName(final String testReportLocation) {
        return testReportLocation + ".status";
    }

    private String statusTempFileName(final String testReportLocation) {
        return statusFileName(testReportLocation) + ".tmp";
    }

    void recordFailure(final Throwable t) {
        logger.log(Level.FINE, "Recording failure", t);
        reportedFailure = t;
        recordExit(1);
    }
    
    void recordExit(final int status) {
        logger.log(Level.FINE, "Recording exit {0}", status);
        reportedStatus.set(status);
        
    }

    private static class CommentWriter extends PrintWriter {

        private CommentWriter(final PrintWriter delegate) {
            super(delegate);
        }

        @Override
        public void println() {
            super.println();
            print("#");
        }
    }
}
