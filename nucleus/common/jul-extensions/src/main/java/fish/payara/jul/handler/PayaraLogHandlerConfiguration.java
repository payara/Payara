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

package fish.payara.jul.handler;

import fish.payara.jul.cfg.LogProperty;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static fish.payara.jul.handler.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.DEFAULT_BUFFER_CAPACITY;
import static fish.payara.jul.handler.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.DEFAULT_BUFFER_TIMEOUT;
import static fish.payara.jul.handler.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.DEFAULT_ROTATION_LIMIT_MB;

/**
 * @author David Matejcek
 */
public class PayaraLogHandlerConfiguration implements Cloneable {

    public static final long BYTES_PER_MEGABYTES = 1_000_000;
    private Level level = Level.INFO;
    private Charset encoding = StandardCharsets.UTF_8;

    private boolean enabled = true;
    private File logFile;
    /** Count of flushed records in one batch, not a frequency at all */
    private int flushFrequency;
    private int maxArchiveFiles;

    private int bufferCapacity = DEFAULT_BUFFER_CAPACITY;
    private int bufferTimeout = DEFAULT_BUFFER_TIMEOUT;

    private boolean rotationOnDateChange;
    private int rotationTimeLimitMinutes;
    private long rotationSizeLimitBytes = DEFAULT_ROTATION_LIMIT_MB * BYTES_PER_MEGABYTES;
    private boolean compressionOnRotation;

    private boolean redirectStandardStreams;

    private Formatter formatterConfiguration;

    public Level getLevel() {
        return level;
    }


    public void setLevel(final Level level) {
        this.level = level;
    }


    public Charset getEncoding() {
        return encoding;
    }


    public void setEncoding(final Charset encoding) {
        this.encoding = encoding;
    }


    public boolean isEnabled() {
        return enabled;
    }


    public void setEnabled(final boolean logToFile) {
        this.enabled = logToFile;
    }


    public File getLogFile() {
        return logFile;
    }


    public void setLogFile(final File logFile) {
        this.logFile = logFile;
    }


    public boolean isRedirectStandardStreams() {
        return redirectStandardStreams;
    }


    public void setRedirectStandardStreams(final boolean logStandardStreams) {
        this.redirectStandardStreams = logStandardStreams;
    }


    public int getFlushFrequency() {
        return flushFrequency;
    }


    public void setFlushFrequency(final int flushFrequency) {
        this.flushFrequency = flushFrequency;
    }


    public int getBufferCapacity() {
        return bufferCapacity;
    }


    public void setBufferCapacity(final int bufferCapacity) {
        this.bufferCapacity = bufferCapacity;
    }


    public int getBufferTimeout() {
        return bufferTimeout;
    }


    public void setBufferTimeout(final int bufferTimeout) {
        this.bufferTimeout = bufferTimeout;
    }


    public long getRotationSizeLimitBytes() {
        return rotationSizeLimitBytes;
    }


    public void setRotationSizeLimitMB(final long megabytes) {
        this.rotationSizeLimitBytes = megabytes * BYTES_PER_MEGABYTES;
    }


    public void setRotationSizeLimitBytes(final long bytes) {
        this.rotationSizeLimitBytes = bytes;
    }


    public boolean isCompressionOnRotation() {
        return compressionOnRotation;
    }


    public void setCompressionOnRotation(final boolean compressionOnRotation) {
        this.compressionOnRotation = compressionOnRotation;
    }


    public boolean isRotationOnDateChange() {
        return rotationOnDateChange;
    }


    public void setRotationOnDateChange(final boolean rotationOnDateChange) {
        this.rotationOnDateChange = rotationOnDateChange;
    }


    /**
     * @return minutes
     */
    public int getRotationTimeLimitMinutes() {
        return rotationTimeLimitMinutes;
    }


    /**
     * @param rotationTimeLimitValue minutes
     */
    public void setRotationTimeLimitMinutes(final int rotationTimeLimitValue) {
        this.rotationTimeLimitMinutes = rotationTimeLimitValue;
    }


    public int getMaxArchiveFiles() {
        return maxArchiveFiles;
    }


    public void setMaxArchiveFiles(final int maxHistoryFiles) {
        this.maxArchiveFiles = maxHistoryFiles;
    }


    public Formatter getFormatterConfiguration() {
        return formatterConfiguration;
    }


    public void setFormatterConfiguration(final Formatter formatterConfiguration) {
        this.formatterConfiguration = formatterConfiguration;
    }


    @Override
    public PayaraLogHandlerConfiguration clone() {
        try {
            return (PayaraLogHandlerConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone failed.", e);
        }
    }


    /**
     * Configuration property set of this handler.
     */
    public enum PayaraLogHandlerProperty implements LogProperty {

        /** False means that handler will stay in logging structure, but will ignore incoming records */
        ENABLED("enabled"),
        /** Minimal acceptable level of the incoming log record */
        LEVEL("level"),
        /** Absolute path to the output file */
        OUTPUT_FILE("file"),
        /** Charset */
        ENCODING("encoding"),
        /** Class of the {@link Formatter} used with this handler */
        FORMATTER(HandlerConfigurationHelper.FORMATTER.getPropertyName()),
        /**
         * LogRecord buffer size. If the buffer is full and it is not possible to add new record for
         * {@link #BUFFER_TIMEOUT} seconds, buffer will reset and replace all records with just one
         * severe {@link LogRecord} explaining what happened.
         */
        BUFFER_CAPACITY("buffer.capacity"),
        /**
         * LogRecord buffer timeout for adding new records if the buffer is full.
         * If the buffer is full and it is not possible to add new record for
         * this count of seconds, buffer will reset and replace all records with just one
         * severe {@link LogRecord} explaining what happened.
         * <p>
         * 0 means wait forever.
         */
        BUFFER_TIMEOUT("buffer.timeoutInSeconds"),
        /** Count of records processed until handler flushes the output */
        FLUSH_FREQUENCY("flushFrequency"),
        /** Log STDOUT and STDERR to the log file too */
        REDIRECT_STANDARD_STREAMS("redirectStandardStreams"),
        /** Compress rolled file to a zio file */
        ROTATION_COMPRESS("rotation.compress"),
        /** File will be rotated after mignight */
        ROTATION_ON_DATE_CHANGE("rotation.rollOnDateChange"),
        /** File containing more megabytes (1 000 000 B) will be rotated */
        ROTATION_LIMIT_SIZE("rotation.limit.megabytes"),
        /** File will be rotated after given count of minutes */
        ROTATION_LIMIT_TIME("rotation.limit.minutes"),
        /** Maximal count of archived files */
        ROTATION_MAX_HISTORY("rotation.maxArchiveFiles"),
        ;
        public static final int MINIMUM_ROTATION_LIMIT_MB = 1;
        public static final int DEFAULT_ROTATION_LIMIT_MB = 2;
        public static final int DEFAULT_BUFFER_CAPACITY = 10_000;
        public static final int DEFAULT_BUFFER_TIMEOUT = 0;

        private final String propertyName;

        PayaraLogHandlerProperty(final String propertyName) {
            this.propertyName = propertyName;
        }

        @Override
        public String getPropertyName() {
            return propertyName;
        }

        /**
         * @return full name using the {@link PayaraLogHandler} class.
         */
        public String getPropertyFullName() {
            return getPropertyFullName(PayaraLogHandler.class);
        }

    }
}
