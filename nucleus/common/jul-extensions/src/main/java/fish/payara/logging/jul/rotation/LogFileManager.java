/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package fish.payara.logging.jul.rotation;

import fish.payara.logging.jul.internal.MeteredStream;
import fish.payara.logging.jul.internal.PayaraLoggingTracer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.zip.GZIPOutputStream;

import static java.util.logging.Level.FINE;

/**
 * Manages the logging file, it's rotations, packing of rolled log file, etc.
 * <p>
 * Note about logging in this class - it is mixed JUL logging and private {@link PayaraLoggingTracer}
 * which prints errors to the original standard error output. The reason is practical - if everything
 * works fine, JUL logging and all it's handlers work properly. If they don't, you would not see
 * errors in any log.
 * So if this class cannot delete or pack log files, it is probably a sign of really serious problem
 * - then is standard output more reliable way to see logs.
 *
 * @author David Matejcek
 */
public class LogFileManager {
    private static final Logger LOG = Logger.getLogger(LogFileManager.class.getName());

    private static final DateTimeFormatter SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");
    private static final String GZIP_EXTENSION = ".gz";

    private final File logFile;
    private final long maxFileSize;
    private final boolean compressOldLogs;
    private final int maxCountOfOldLogs;
    private final HandlerSetStreamMethod streamSetter;
    private final HandlerCloseStreamMethod streamCloser;

    private MeteredStream meter;


    /**
     * Creates the manager and initializes it with given parameters. It only creates the manager but
     * does not enable the output. Call {@link #enableOutput()} for that.
     *
     * @param logFile - output logging file path
     * @param maxFileSize - if the size of the file crosses this value, the file is renamed to the
     *            logFile name with added suffix ie. <code>server.log_2020-05-01T16-28-27</code>
     * @param compressOldLogs - if true, rolled file is packed to GZIP (so the file will have a name
     *            ie. <code>server.log_2020-05-01T21-50-09.gz</code>)
     * @param maxCountOfOldLogs - if the count of rolled files with logFile's file name prefix
     *            crosses this value, old files will be permanently deleted.
     * @param streamSetter - this should be a {@link StreamHandler#setOutputStream} method. This
     *            method will be called when we enable ouput.
     * @param streamCloser - this should be a {@link StreamHandler#close()} method. This method will
     *            be called when we disable output.
     */
    public LogFileManager(final File logFile, //
        final long maxFileSize, final boolean compressOldLogs, final int maxCountOfOldLogs, //
        final HandlerSetStreamMethod streamSetter, final HandlerCloseStreamMethod streamCloser //
    ) {
        this.logFile = logFile;
        this.maxFileSize = maxFileSize;
        this.compressOldLogs = compressOldLogs;
        this.maxCountOfOldLogs = maxCountOfOldLogs;
        this.streamSetter = streamSetter;
        this.streamCloser = streamCloser;
    }


    /**
     * @return the size of the logFile in bytes. The value is obtained from the outputstream, only
     *         if the output stream is closed, this method will check the file system.
     */
    public long getFileSize() {
        return this.meter == null ? this.logFile.length() : this.meter.getBytesWritten();
    }


    /**
     * Calls {@link #roll()} if the file is bigger than limit given in constructor.
     */
    public void rollIfFileTooBig() {
        if (isRollFileSizeLimitReached()) {
            roll();
        }
    }


    /**
     * Calls {@link #roll()} if the file is not empty.
     */
    public void rollIfFileNotEmpty() {
        if (getFileSize() > 0) {
            roll();
        }
    }


    /**
     * Rolls the file regardless of it's size and if it is currently used for output.
     * <p>
     * But if the output was enabled, the output is suspended first, then file rolls
     * and finally if the output was enabled before this method call, it is enabled
     * again.
     */
    public synchronized void roll() {
        LOG.log(FINE, "roll(); {0}", this.logFile);
        final boolean wasOutputEnabled = isOutputEnabled();
        disableOutput();
        final File archivedFile = rollToNewFile();
        // There is no need to block processing of new log records with this time consuming action.
        final Runnable cleanup = () -> cleanUpHistoryLogFiles(archivedFile);
        new Thread(cleanup, "old-log-files-cleanup-" + this.logFile.getName()).start();
        if (wasOutputEnabled) {
            enableOutput();
        }
    }


    /**
     * @return true if the handler owning this instance can write to the outputstream.
     */
    public synchronized boolean isOutputEnabled() {
        return this.meter != null;
    }


    /**
     * Creates the file, initializes the MeteredStream and calls the stream setter given in
     * constructor.
     * <p>
     * Redundant calls do nothing.
     */
    public synchronized void enableOutput() {
        if (isOutputEnabled()) {
            return;
        }
        // check that the parent directory exists.
        final File parent = this.logFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create the parent directory " + parent.getAbsolutePath());
        }
        try {
            final FileOutputStream fout = new FileOutputStream(this.logFile, true);
            final BufferedOutputStream bout = new BufferedOutputStream(fout);
            this.meter = new MeteredStream(bout, this.logFile.length());
            this.streamSetter.setStream(this.meter);
        } catch (Exception e) {
            throw new IllegalStateException("Could not open the log file for writing: " + this.logFile, e);
        }
    }


    /**
     * Calls the close method given in constructor, then closes the output stream.
     * <p>
     * Redundant calls do nothing.
     */
    public synchronized void disableOutput() {
        if (isOutputEnabled()) {
            this.streamCloser.close();
            try {
                this.meter.close();
            } catch (final IOException e) {
                PayaraLoggingTracer.error(getClass(), "Could not close the output stream.", e);
            }
            this.meter = null;
        }
    }


    private boolean isRollFileSizeLimitReached() {
        if (this.maxFileSize <= 0) {
            return false;
        }
        final long fileSize = getFileSize();
        return fileSize >= this.maxFileSize;
    }


    private File rollToNewFile() {
        try {
            if (!this.logFile.exists()) {
                this.logFile.createNewFile();
                return null;
            }
            LOG.log(FINE, "Rolling log file: {0}", this.logFile);
            final File archivedLogFile = prepareAchivedLogFileTarget();
            moveFile(logFile, archivedLogFile);
            forceOSFilesync(logFile);
            return archivedLogFile;
        } catch (final Exception e) {
            logError("Error, could not rotate log file", e);
            return null;
        }
    }


    private File prepareAchivedLogFileTarget() {
        final String archivedFileNameBase = logFile.getName() + "_" + SUFFIX_FORMATTER.format(LocalDateTime.now());
        int counter = 1;
        String archivedFileName = archivedFileNameBase;
        while (true) {
            final File archivedLogFile = new File(logFile.getParentFile(), archivedFileName);
            if (!archivedLogFile.exists()) {
                return archivedLogFile;
            }
            counter++;
            archivedFileName = archivedFileNameBase + "_" + counter;
        }
    }


    /**
     * Make sure that server.log contents are flushed out to start from a clean file again after
     * the rename...
     *
     * @param file
     * @throws IOException
     */
    private void forceOSFilesync(final File file) throws IOException {
        new FileOutputStream(file).close();
    }


    private void moveFile(final File logFileToArchive, final File target) throws IOException {
        LOG.log(FINE, () -> String.format("moveFile(logFileToArchive=%s, target=%s)", logFileToArchive, target));
        final boolean renameSuccess = logFileToArchive.renameTo(target);
        if (!renameSuccess) {
            logError(String.format(
                "File %s could not be renamed to %s trying to copy and delete it with NIO.",
                logFileToArchive, target));
            // If we don't succeed with file rename which most likely can happen on
            // Windows because of multiple file handles opened. We go through Plan B to
            // copy bytes explicitly to a renamed file.
            Files.copy(logFileToArchive.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
        }
    }


    // synchronized - it is executed in separate thread!
    private synchronized void cleanUpHistoryLogFiles(final File rotatedFile) {
        if (this.compressOldLogs) {
            compressFile(rotatedFile);
        }
        deleteOldLogFiles();
    }


    private void compressFile(final File rotatedFile) {
        final long start = System.currentTimeMillis();
        final File outFile = new File(rotatedFile.getParentFile(), rotatedFile.getName() + GZIP_EXTENSION);
        final boolean compressed = gzipFile(rotatedFile, outFile);
        if (compressed) {
            final long time = System.currentTimeMillis() - start;
            LOG.log(FINE, "File {0} of size {1} has been archived to file {2} of size {3} in {4} ms",
                new Object[] {rotatedFile, rotatedFile.length(), outFile, outFile.length(), time});
            final boolean deleted = rotatedFile.delete();
            if (!deleted) {
                logError("Could not delete uncompressed log file: " + rotatedFile.getAbsolutePath());
            }
        } else {
            logError("Could not compress log file: " + rotatedFile.getAbsolutePath());
        }
    }


    private void deleteOldLogFiles() {
        if (this.maxCountOfOldLogs == 0) {
            return;
        }

        final File dir = this.logFile.getParentFile();
        final String logFileName = this.logFile.getName();
        if (dir == null) {
            return;
        }
        final FileFilter filter = f -> f.isFile() && !f.getName().equals(logFileName)
            && f.getName().startsWith(logFileName);
        Arrays.stream(dir.listFiles(filter)).sorted(Comparator.comparing(File::getName).reversed())
            .skip(this.maxCountOfOldLogs).forEach(this::deleteFile);
    }


    private void deleteFile(final File file) {
        final boolean delFile = file.delete();
        if (!delFile) {
            logError("Could not delete the log file: " + file);
        }
    }


    private boolean gzipFile(final File inputFile, final File outputFile) {
        try ( //
            FileInputStream fis = new FileInputStream(inputFile); //
            FileOutputStream fos = new FileOutputStream(outputFile); //
            GZIPOutputStream gzos = new GZIPOutputStream(fos) //
        ) { //
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, len);
            }
            gzos.finish();
            return true;
        } catch (IOException ix) {
            logError("Error gzipping log file " + inputFile, ix);
            return false;
        }
    }


    private void logError(final String message) {
        PayaraLoggingTracer.error(getClass(), message);
        LOG.log(Level.SEVERE, message);
    }


    private void logError(final String message, final Throwable t) {
        PayaraLoggingTracer.error(getClass(), message, t);
        LOG.log(Level.SEVERE, message, t);
    }

    /**
     * Method which sets the handler's output stream. <br>
     * After this call handler is able to write to the output file.
     */
    @FunctionalInterface
    public interface HandlerSetStreamMethod {

        /**
         * Method which sets the handler's output stream. <br>
         * After this call handler is able to write to the output file.
         *
         * @param outputStream target output stream
         */
        void setStream(final OutputStream outputStream);
    }

    /**
     * Method which flushes and closes the handler's output stream. <br>
     * After this call is not possible to write to the file.
     */
    @FunctionalInterface
    public interface HandlerCloseStreamMethod {

        /**
         * Method which flushes and closes the handler's output stream. <br>
         * After this call is not possible to write to the file.
         */
        void close();
    }
}
