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

package fish.payara.logging.jul;

import fish.payara.logging.jul.formatter.FormatterDelegate;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Formatter;
import java.util.logging.Level;

/**
 * @author David Matejcek
 */
public class PayaraLogHandlerConfiguration {

    public static final int DEFAULT_ROTATION_LIMIT_BYTES = 2_000_000;
    public static final int DEFAULT_BUFFER_CAPACITY = 10_000;
    public static final int DEFAULT_BUFFER_TIMEOUT = 0;

    private Level level = Level.INFO;
    private Charset encoding = StandardCharsets.UTF_8;

    private boolean logToFile = true;
    private File logFile;
    /** Count of flushed records in one batch, not a frequency at all */
    private int flushFrequency;
    private int maxHistoryFiles;

    private int bufferCapacity = DEFAULT_BUFFER_CAPACITY;
    private int bufferTimeout = DEFAULT_BUFFER_TIMEOUT;

    private boolean rotationOnDateChange;
    private int rotationTimeLimitValue;
    private long limitForFileRotation = DEFAULT_ROTATION_LIMIT_BYTES;
    private boolean compressionOnRotation;

    private boolean logStandardStreams;

    // FIXME: change to JulFormatterConfiguration and it's children
    private Formatter formatterConfiguration;
    private FormatterDelegate formatterDelegate;
    private String productId;

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


    public boolean isLogToFile() {
        return logToFile;
    }


    public void setLogToFile(final boolean logToFile) {
        this.logToFile = logToFile;
    }


    public File getLogFile() {
        return logFile;
    }


    public void setLogFile(final File logFile) {
        this.logFile = logFile;
    }


    public boolean isLogStandardStreams() {
        return logStandardStreams;
    }


    public void setLogStandardStreams(final boolean logStandardStreams) {
        this.logStandardStreams = logStandardStreams;
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


    public long getLimitForFileRotation() {
        return limitForFileRotation;
    }


    public void setLimitForFileRotation(final long limitForFileRotation) {
        this.limitForFileRotation = limitForFileRotation;
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
    public int getRotationTimeLimitValue() {
        return rotationTimeLimitValue;
    }


    /**
     * @param rotationTimeLimitValue minutes
     */
    public void setRotationTimeLimitValue(final int rotationTimeLimitValue) {
        this.rotationTimeLimitValue = rotationTimeLimitValue;
    }


    public int getMaxHistoryFiles() {
        return maxHistoryFiles;
    }


    public void setMaxHistoryFiles(final int maxHistoryFiles) {
        this.maxHistoryFiles = maxHistoryFiles;
    }


    public Formatter getFormatterConfiguration() {
        return formatterConfiguration;
    }


    public void setFormatterConfiguration(final Formatter formatterConfiguration) {
        this.formatterConfiguration = formatterConfiguration;
    }


    public FormatterDelegate getFormatterDelegate() {
        return formatterDelegate;
    }


    public void setFormatterDelegate(final FormatterDelegate formatterDelegate) {
        this.formatterDelegate = formatterDelegate;
    }


    public String getProductId() {
        return productId;
    }


    public void setProductId(final String productId) {
        this.productId = productId;
    }
}
