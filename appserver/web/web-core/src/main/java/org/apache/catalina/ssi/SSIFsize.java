/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.ssi;

import org.glassfish.web.util.HtmlEntityEncoder;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
/**
 * Implements the Server-side #fsize command
 * 
 * @author Bip Thelin
 * @author Paul Speed
 * @author Dan Sandberg
 * @author David Becker
 * @version $Revision: 1.4 $, $Date: 2007/05/05 05:32:20 $
 */
public final class SSIFsize implements SSICommand {
    private final static int ONE_KILOBYTE = 1024;
    private final static int ONE_MEGABYTE = 1024 * 1024;

    /**
     * @see SSICommand
     */
    public long process(SSIMediator ssiMediator, String commandName,
            String[] paramNames, String[] paramValues, PrintWriter writer) {
        long lastModified = 0;
        String configErrMsg = null;
        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i];
            String paramValue = paramValues[i];
            String substitutedValue = ssiMediator
                    .substituteVariables(paramValue);
            try {
                if (paramName.equalsIgnoreCase("file")
                        || paramName.equalsIgnoreCase("virtual")) {
                    boolean virtual = paramName.equalsIgnoreCase("virtual");
                    lastModified = ssiMediator.getFileLastModified(
                            substitutedValue, virtual);
                    long size = ssiMediator.getFileSize(substitutedValue,
                            virtual);
                    String configSizeFmt = ssiMediator.getConfigSizeFmt();
                    writer.write(formatSize(size, configSizeFmt));
                } else {
                    ssiMediator.log("#fsize--Invalid attribute: " + paramName);
                    if (configErrMsg == null) {
                        configErrMsg = getEncodedConfigErrorMessage(ssiMediator);
                    }
                    writer.write(configErrMsg);
                }
            } catch (IOException e) {
                ssiMediator.log("#fsize--Couldn't get size for file: "
                        + substitutedValue, e);
                if (configErrMsg == null) {
                    configErrMsg = getEncodedConfigErrorMessage(ssiMediator);
                }
                writer.write(configErrMsg);
            }
        }
        return lastModified;
    }


    public String repeat(char aChar, int numChars) {
        if (numChars < 0) {
            throw new IllegalArgumentException("Num chars can't be negative");
        }
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < numChars; i++) {
            buf.append(aChar);
        }
        return buf.toString();
    }


    public String padLeft(String str, int maxChars) {
        String result = str;
        int charsToAdd = maxChars - str.length();
        if (charsToAdd > 0) {
            result = repeat(' ', charsToAdd) + str;
        }
        return result;
    }


    //We try to mimic Apache here, as we do everywhere
    //All the 'magic' numbers are from the util_script.c Apache source file.
    protected String formatSize(long size, String format) {
        String retString = "";
        if (format.equalsIgnoreCase("bytes")) {
            DecimalFormat decimalFormat = new DecimalFormat("#,##0");
            retString = decimalFormat.format(size);
        } else {
            if (size < 0) {
                retString = "-1k";
            } else if (size == 0) {
                retString = "0k";
            } else if (size < ONE_KILOBYTE) {
                retString = "1k";
            } else if (size < ONE_MEGABYTE) {
                retString = Long.toString((size + 512) / ONE_KILOBYTE);
                retString += "k";
            } else if (size < 99 * ONE_MEGABYTE) {
                DecimalFormat decimalFormat = new DecimalFormat("0.0M");
                retString = decimalFormat.format(size / (double)ONE_MEGABYTE);
            } else {
                retString = Long.toString((size + (529 * ONE_KILOBYTE))
                        / ONE_MEGABYTE);
                retString += "M";
            }
            retString = padLeft(retString, 5);
        }
        return retString;
    }

    private String getEncodedConfigErrorMessage(SSIMediator ssiMediator) {
        String errorMessage = ssiMediator.getConfigErrMsg();
        return HtmlEntityEncoder.encodeXSS(errorMessage);
    }
}
