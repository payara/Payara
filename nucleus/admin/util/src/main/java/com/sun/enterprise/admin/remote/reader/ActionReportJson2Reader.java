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
package com.sun.enterprise.admin.remote.reader;

import com.sun.enterprise.admin.remote.Metrix;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Properties;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.glassfish.api.ActionReport;

/** JAX-WS reader for ActionReport in JSON format. This is reader of format 
 * created by {@code ActionReportDtoJsonProvider}.
 *
 * @author mmares
 */
@Provider
@Consumes({"actionreport/json"})
public class ActionReportJson2Reader implements MessageBodyReader<ActionReport> {
    
    private static final JsonFactory factory = new JsonFactory();
    
//    private static final Logger logger =
//            LogDomains.getLogger(ActionReportJson2Reader.class, LogDomains.ADMIN_LOGGER);
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.isAssignableFrom(ActionReport.class);
    }
    
    @Override
    public ActionReport readFrom(Class<ActionReport> type, Type genericType, Annotation[] annotations, 
                    MediaType mediaType, MultivaluedMap<String, String> httpHeaders, 
                    InputStream entityStream) throws IOException, WebApplicationException {
        Metrix.event("readFrom() - AcctionReport - start");
        JsonParser jp = factory.createJsonParser(entityStream);
        try {
            JsonToken token = jp.nextToken(); //sorounding object
            //jp.nextToken(); //Name action-report
            JsonToken token2 = jp.nextToken();
            if (token != JsonToken.START_OBJECT || 
                    token2 != JsonToken.START_OBJECT) {
                throw new IOException("Not expected type");
            }
            CliActionReport result = new CliActionReport();
            fillActionReport(result, jp);
            return result;
        } finally {
            jp.close();
            Metrix.event("readFrom() - AcctionReport - done");
        }
    }
    
    public static ActionReport fillActionReport(ActionReport ar, JsonParser jp) throws IOException {
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jp.getCurrentName();
            jp.nextToken(); // move to value
            if ("exit_code".equals(fieldname)) {
                ar.setActionExitCode(ActionReport.ExitCode.valueOf(jp.getText()));
            } else if ("command".equals(fieldname)) {
                ar.setActionDescription(jp.getText());
            } else if ("failure_cause".equals(fieldname)) {
                String failure = jp.getText();
                if (failure != null && !failure.isEmpty()) {
                    ar.setFailureCause(new Exception(failure));
                }
            } else if ("extraProperties".equals(fieldname)) {
                ar.setExtraProperties(readProperties(jp));
            } else if ("message".equals(fieldname)) {
                fillMessage(ar.getTopMessagePart(), jp);
            } else if ("action-reports".equals(fieldname)) {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
                        fillActionReport(ar.addSubActionsReport(), jp);
                    }
                }
            }
        }
        return ar;
    }
    
    public static void fillMessage(ActionReport.MessagePart mp, JsonParser jp) throws IOException {
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jp.getCurrentName();
            jp.nextToken();
            if ("value".equals(fieldname)) {
                mp.setMessage(jp.getText());
            } else if ("children-type".equals(fieldname)) {
                mp.setChildrenType(jp.getText());
            } else if ("properties".equals(fieldname)) {
                Properties props = readProperties(jp);
                for (String key : props.stringPropertyNames()) {
                    mp.addProperty(key, props.getProperty(key));
                }
            } else if ("messages".equals(fieldname)) {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
                        fillMessage(mp.addChild(), jp);
                    }
                }
            }
        }
    }
    
    public static Properties readProperties(JsonParser jp) throws IOException {
        Properties result = new Properties();
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
                String key = null;
                String value = null;
                while (jp.nextToken() != JsonToken.END_OBJECT) {
                    String fieldname = jp.getCurrentName();
                    jp.nextToken();
                    if ("name".equals(fieldname)) {
                        key = jp.getText();
                    } else if ("value".equals(fieldname)) {
                        value = jp.getText();
                    }
                }
                if (key != null && value != null) {
                    result.setProperty(key, value);
                }
            }
        }
        return result;
    }
    
}
