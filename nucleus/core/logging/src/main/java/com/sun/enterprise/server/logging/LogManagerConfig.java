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

package com.sun.enterprise.server.logging;

import java.io.File;

/**
 * @author David Matejcek
 */
public class LogManagerConfig {

    private String handlers;
    private String handlerServices;

    private String chFormatterClass;

    private String fhFileNamePattern;
    private String fhOutputFileSizeLimit;
    private String fhMaxCountOfFiles;
    private String fhFormatterClass;

    private File gfhOutputFile;
    private String gfhLogToFile;
    private String gfhLogStandardStreams;
    private boolean gfMultiLineMode;
    private String gfExcludedFields;
    private String gfhEnableRotationOnDateChange;
    private String gfhRotationOnTimeLimit;
    private String gfhOutputFileSizeLimit;
    private String gfhMaxCountOfFiles;
    private String gfhCompressOnRotation;
    private String gfhBatchSize;
    private String gfhFormatterClass;
    private String gfhTimeStampPattern;
    private String gfhRetainErrorsStatictics;

    private String pnOutputFile;
    private String pnLogToFile;
    private String pnRotationOnTimeLimit;
    private String pnEnableRotationOnDateChange;
    private String pnOutputFileSizeLimit;
    private String pnMaxCountOfFiles;
    private String pnCompressOnRotation;
    private String pnFormatterClass;

    private String enableSysLogHandler;

    public String getHandlers() {
        return handlers;
    }


    public void setHandlers(String handlers) {
        this.handlers = handlers;
    }


    public String getHandlerServices() {
        return handlerServices;
    }


    public void setHandlerServices(String handlerServices) {
        this.handlerServices = handlerServices;
    }


    public String getChFormatterClass() {
        return chFormatterClass;
    }


    public void setChFormatterClass(String chFormatterClass) {
        this.chFormatterClass = chFormatterClass;
    }


    public String getFhFileNamePattern() {
        return fhFileNamePattern;
    }


    public void setFhFileNamePattern(String fhFileNamePattern) {
        this.fhFileNamePattern = fhFileNamePattern;
    }


    public String getFhOutputFileSizeLimit() {
        return fhOutputFileSizeLimit;
    }


    public void setFhOutputFileSizeLimit(String fhOutputFileSizeLimit) {
        this.fhOutputFileSizeLimit = fhOutputFileSizeLimit;
    }


    public String getFhMaxCountOfFiles() {
        return fhMaxCountOfFiles;
    }


    public void setFhMaxCountOfFiles(String fhCount) {
        this.fhMaxCountOfFiles = fhCount;
    }


    public String getFhFormatterClass() {
        return fhFormatterClass;
    }


    public void setFhFormatterClass(String fhFormatterClass) {
        this.fhFormatterClass = fhFormatterClass;
    }


    public File getGfhOutputFile() {
        return gfhOutputFile;
    }


    public void setGfhOutputFile(File fhOutputFile) {
        this.gfhOutputFile = fhOutputFile;
    }


    public String getGfhLogToFile() {
        return gfhLogToFile;
    }


    public void setGfhLogToFile(String fhLogToFile) {
        this.gfhLogToFile = fhLogToFile;
    }


    public String getGfhLogStandardStreams() {
        return gfhLogStandardStreams;
    }


    public void setGfhLogStandardStreams(String gfhLogStandardStreams) {
        this.gfhLogStandardStreams = gfhLogStandardStreams;
    }



    public boolean isGfMultiLineMode() {
        return gfMultiLineMode;
    }



    public void setGfMultiLineMode(boolean gfMultiLineMode) {
        this.gfMultiLineMode = gfMultiLineMode;
    }



    public String getGfExcludedFields() {
        return gfExcludedFields;
    }



    public void setGfExcludedFields(String gfExcludedFields) {
        this.gfExcludedFields = gfExcludedFields;
    }


    public String getGfhEnableRotationOnDateChange() {
        return gfhEnableRotationOnDateChange;
    }


    public void setGfhEnableRotationOnDateChange(String gfhEnableRotationOnDateChange) {
        this.gfhEnableRotationOnDateChange = gfhEnableRotationOnDateChange;
    }


    /**
     * @return limit in minutes
     */
    public String getGfhRotationOnTimeLimit() {
        return gfhRotationOnTimeLimit;
    }


    public void setGfhRotationOnTimeLimit(String minutes) {
        this.gfhRotationOnTimeLimit = minutes;
    }


    public String getGfhOutputFileSizeLimit() {
        return gfhOutputFileSizeLimit;
    }


    public void setGfhOutputFileSizeLimit(String gfhOutputFileSizeLimit) {
        this.gfhOutputFileSizeLimit = gfhOutputFileSizeLimit;
    }


    public String getGfhMaxCountOfFiles() {
        return gfhMaxCountOfFiles;
    }


    public void setGfhMaxCountOfFiles(String gfhMaxCountOfFiles) {
        this.gfhMaxCountOfFiles = gfhMaxCountOfFiles;
    }


    public String getGfhCompressOnRotation() {
        return gfhCompressOnRotation;
    }


    public void setGfhCompressOnRotation(String gfhCompressOnRotation) {
        this.gfhCompressOnRotation = gfhCompressOnRotation;
    }


    public String getGfhBatchSize() {
        return gfhBatchSize;
    }


    public void setGfhBatchSize(String fhBatchSize) {
        this.gfhBatchSize = fhBatchSize;
    }


    public String getGfhFormatterClass() {
        return gfhFormatterClass;
    }


    public void setGfhFormatterClass(String fhFormatterClass) {
        this.gfhFormatterClass = fhFormatterClass;
    }


    public String getGfhTimeStampPattern() {
        return gfhTimeStampPattern;
    }


    public void setGfhTimeStampPattern(String gfhTimeStampPattern) {
        this.gfhTimeStampPattern = gfhTimeStampPattern;
    }


    public String getGfhRetainErrorsStatictics() {
        return gfhRetainErrorsStatictics;
    }


    public void setGfhRetainErrorsStatictics(String gfhRetainErrorsStatictics) {
        this.gfhRetainErrorsStatictics = gfhRetainErrorsStatictics;
    }


    public String getEnableSysLogHandler() {
        return enableSysLogHandler;
    }


    public void setEnableSysLogHandler(String enableSysLogHandler) {
        this.enableSysLogHandler = enableSysLogHandler;
    }


    public String getPnOutputFile() {
        return pnOutputFile;
    }


    public void setPnOutputFile(String pnOutputFile) {
        this.pnOutputFile = pnOutputFile;
    }


    public String getPnLogToFile() {
        return pnLogToFile;
    }


    public void setPnLogToFile(String pnLogToFile) {
        this.pnLogToFile = pnLogToFile;
    }


    public String getPnRotationOnTimeLimit() {
        return pnRotationOnTimeLimit;
    }


    public void setPnRotationOnTimeLimit(String pnhRotationOnTimeLimit) {
        this.pnRotationOnTimeLimit = pnhRotationOnTimeLimit;
    }


    public String getPnEnableRotationOnDateChange() {
        return pnEnableRotationOnDateChange;
    }


    public void setPnEnableRotationOnDateChange(String pnEnableRotationOnDateChange) {
        this.pnEnableRotationOnDateChange = pnEnableRotationOnDateChange;
    }


    public String getPnOutputFileSizeLimit() {
        return pnOutputFileSizeLimit;
    }


    public void setPnOutputFileSizeLimit(String pnhOutputFileSizeLimit) {
        this.pnOutputFileSizeLimit = pnhOutputFileSizeLimit;
    }


    public String getPnMaxCountOfFiles() {
        return pnMaxCountOfFiles;
    }


    public void setPnMaxCountOfFiles(String pnMaxCountOfFiles) {
        this.pnMaxCountOfFiles = pnMaxCountOfFiles;
    }


    public String getPnCompressOnRotation() {
        return pnCompressOnRotation;
    }


    public void setPnCompressOnRotation(String pnCompressOnRotation) {
        this.pnCompressOnRotation = pnCompressOnRotation;
    }


    public String getPnFormatterClass() {
        return pnFormatterClass;
    }


    public void setPnFormatterClass(String pnFormatterClass) {
        this.pnFormatterClass = pnFormatterClass;
    }
}
