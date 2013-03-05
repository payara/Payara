/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.remote.reader;

import com.sun.enterprise.admin.remote.ParamsWithPayload;
import com.sun.enterprise.admin.remote.RestPayloadImpl;
import com.sun.enterprise.util.StringUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Properties;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ParameterMap;
import org.jvnet.mimepull.MIMEConfig;
import org.jvnet.mimepull.MIMEMessage;
import org.jvnet.mimepull.MIMEPart;

/**
 *
 * @author martinmares
 */
public class MultipartProprietaryReader implements ProprietaryReader<ParamsWithPayload> {
    
    private final ActionReportJsonProprietaryReader actionReportReader;

    public MultipartProprietaryReader() {
        this(new ActionReportJsonProprietaryReader());
    }

    public MultipartProprietaryReader(ActionReportJsonProprietaryReader actionReportReader) {
        this.actionReportReader = actionReportReader;
    }
    
    @Override
    public boolean isReadable(final Class<?> type,
                               final String mimetype) {
        if (mimetype == null || mimetype.startsWith("*/") || mimetype.startsWith("multipart/")) {
            return ParamsWithPayload.class.isAssignableFrom(type);
        }
        return false;
    }
    
    public ParamsWithPayload readFrom(final HttpURLConnection urlConnection) throws IOException {
        return readFrom(urlConnection.getInputStream(), urlConnection.getContentType());
    }
    
    @Override
    public ParamsWithPayload readFrom(final InputStream is, final String contentType) throws IOException {
        RestPayloadImpl.Inbound payload = null;
        ActionReport actionReport  = null;
        ParameterMap parameters  = null;
        Properties mtProps = parseHeaderParams(contentType);
        final String boundary = mtProps.getProperty("boundary");
        if (!StringUtils.ok(boundary)) {
            throw new IOException("ContentType does not define boundary");
        }
        final MIMEMessage mimeMessage = new MIMEMessage(is, boundary, new MIMEConfig());
        //Parse
        for (MIMEPart mimePart : mimeMessage.getAttachments()) {
            String cd = getFirst(mimePart.getHeader("Content-Disposition"));
            if (!StringUtils.ok(cd)) {
                cd = "file";
            }
            cd = cd.trim();
            Properties cdParams = parseHeaderParams(cd);
            //3 types of content disposition
            if (cd.startsWith("form-data")) {
                //COMMAND PARAMETER
                if (!StringUtils.ok(cdParams.getProperty("name"))) {
                    throw new IOException("Form-data Content-Disposition does not contains name parameter.");
                }
                if (parameters == null) {
                    parameters = new ParameterMap();
                }
                parameters.add(cdParams.getProperty("name"), stream2String(mimePart.readOnce()));
            } else if (mimePart.getContentType() != null && mimePart.getContentType().startsWith("application/json")) {
                //ACTION REPORT
                actionReport = actionReportReader.readFrom(mimePart.readOnce(), "application/json");
            } else {
                //PAYLOAD
                String name = "noname";
                if (cdParams.containsKey("name")) {
                    name = cdParams.getProperty("name");
                } else if (cdParams.containsKey("filename")) {
                    name = cdParams.getProperty("filename");
                }
                if (payload == null) {
                    payload = new RestPayloadImpl.Inbound();
                }
                String ct = mimePart.getContentType();
                if (!StringUtils.ok(ct) || ct.trim().startsWith("text/plain")) {
                    payload.add(name, stream2String(mimePart.readOnce()), mimePart.getAllHeaders());
                } else {
                    payload.add(name, mimePart.read(), ct, mimePart.getAllHeaders());
                }
            }
        }
        //Result
        return new ParamsWithPayload(payload, parameters, actionReport);
    }
    
    /** It is very simple implementation. Use it just for cli client
     */
    private static Properties parseHeaderParams(String contentType) {
        Properties result = new Properties();
        if (contentType == null) {
            return result;
        }
        int ind = contentType.indexOf(';');
        if (ind < 0) {
            return result;
        }
        contentType = contentType.substring(ind + 1);
        boolean parsingKey = true;
        boolean quoted = false;
        String key = "";
        StringBuilder tmp = new StringBuilder();
        for (char ch : contentType.toCharArray()) {
            switch (ch) {
                case '"':
                    quoted = !quoted;
                    break;
                case '=':
                    if (parsingKey && !quoted) {
                        key = tmp.toString();
                        tmp.setLength(0);
                        parsingKey = false;
                    } else {
                        tmp.append(ch);
                    }
                    break;
                case ';':
                    if (quoted) {
                        tmp.append(ch);
                    } else {
                        if (!parsingKey) {
                            parsingKey = true;
                            result.setProperty(key.trim(), tmp.toString().trim());
                            key = "";
                            tmp.setLength(0);
                        }
                    }
                    break;
                default:
                    tmp.append(ch);
            }
        }
        if (key.length() > 0) {
            result.setProperty(key.trim(), tmp.toString().trim());
        }
        return result;
    }
    
    private static String getFirst(List<String> lst) {
        if (lst == null || lst.isEmpty()) {
            return null;
        }
        return lst.get(0);
    }
    
    private static String stream2String(InputStream is) throws IOException {
        if (is == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buff = new byte[256];
            int count;
            while ((count = is.read(buff)) > 0) {
                baos.write(buff, 0, count);
            }
            return baos.toString("UTF-8");
        } finally {
            try { is.close(); } catch (Exception ex) {}
        }
    }

}
