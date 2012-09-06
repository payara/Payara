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
package org.glassfish.admin.rest.provider;

import com.sun.enterprise.v3.common.ActionReporter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.glassfish.admin.rest.composite.RestModel;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;

/** Based on Jackson streaming to check performance
 *
 * @author mmares
 */
@Provider
@Produces("actionreport/json")
public class ActionReportDtoJson2Provider extends BaseProvider<ActionReporter> {
    
    private static final JsonFactory factory = new JsonFactory();
    
    public ActionReportDtoJson2Provider() {
        super(ActionReporter.class, new MediaType("actionreport", "json"));
    }
    
    @Override
    protected boolean isGivenTypeWritable(Class<?> type, Type genericType) {
        return desiredType.isAssignableFrom(type);
    }

    @Override
    public String getContent(ActionReporter proxy) {
        throw new UnsupportedOperationException("Not supported.");
    }
    
    @Override
    public void writeTo(ActionReporter proxy, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        JsonGenerator out = factory.createJsonGenerator(entityStream, JsonEncoding.UTF8);
        out.writeStartObject();
        writeJson(null, proxy, out);
        out.writeEndObject();
        out.flush();
    }
    
    public void writeJson(String name, ActionReporter ar, JsonGenerator out) throws IOException {
        if (ar == null) {
            return;
        }
        if (name != null) {
            out.writeObjectFieldStart(name);
        }
        String combinedMessage = (ar instanceof RestActionReporter) ? ((RestActionReporter)ar).getCombinedMessage() : ar.getMessage();
        out.writeStringField("message", combinedMessage); //Because of beckward compatibility - hmmmm
        if (!combinedMessage.equals(ar.getTopMessagePart().getMessage())) {
            out.writeStringField("top_message", ar.getTopMessagePart().getMessage());
        }
        out.writeStringField("command", ar.getActionDescription());
        out.writeStringField("exit_code", ar.getActionExitCode().name());
        if (ar.getFailureCause() != null) {
            out.writeStringField("failure_cause", ar.getFailureCause().getLocalizedMessage());
        }
        if (ar.getTopMessagePart().getProps() != null) {
            writeJson("properties", ar.getTopMessagePart().getProps(), out);
        }
        writeJson("extraProperties", ar.getExtraProperties(), out);
        List<ActionReporter> subReports = ar.getSubActionsReport();
        if (subReports != null && !subReports.isEmpty()) {
            out.writeArrayFieldStart("subReports");
            for (ActionReporter subReport : subReports) {
                out.writeStartObject();
                writeJson(null, subReport, out);
                out.writeEndObject();
            }
            out.writeEndArray();
        }
        List<MessagePart> children = ar.getTopMessagePart().getChildren();
        if ((children != null) && (!children.isEmpty())) {
            out.writeArrayFieldStart("children");
            for (MessagePart child : children) {
                writeJson(null, child, out);
            }
            out.writeEndArray();
        }
        if (name != null) {
            out.writeEndObject();
        }
    }
    
    public void writeJson(String name, ActionReport.MessagePart mp, JsonGenerator out) throws IOException {
        if (mp == null) {
            return;
        }
        if (name != null) {
            out.writeObjectFieldStart(name);
        } else {
            out.writeStartObject();
        }
        out.writeStringField("message", mp.getMessage());
        //out.writeStringField("children-type", mp.getChildrenType());
        writeJson("properties", mp.getProps(), out);
        List<MessagePart> children = mp.getChildren();
        if (children != null && !children.isEmpty()) {
            out.writeArrayFieldStart("children");
            for (MessagePart messagePart : children) {
                writeJson(null, messagePart, out);
            }
            out.writeEndArray();
        }
        out.writeEndObject();
    }
    
    public void writeJsonForObject(String name, Object obj, JsonGenerator out) throws IOException {
        if (obj == null) {
            if (name != null) {
                out.writeNullField(name);
            } else {
                out.writeNull();
            }
        } else if (obj instanceof Collection) {
            writeJson(name, (Collection) obj, out);
        } else if (obj instanceof Map) {
            writeJson(name, (Map) obj, out);
        } else if (RestModel.class.isAssignableFrom(obj.getClass())) {
            writeJson(name, (RestModel) obj, out);
        } else {
            if (name != null) {
                out.writeStringField(name, String.valueOf(obj));
            } else {
                out.writeString(String.valueOf(obj));
            }
        }
    }
    
    public void writeJson(String name, Collection col, JsonGenerator out) throws IOException {
        if (name != null) {
            out.writeArrayFieldStart(name);
        } else {
            out.writeStartArray();
        }
        for (Object obj : col) {
            writeJsonForObject(name, obj, out);
        }
        out.writeEndArray();
    }
    
    
    public void writeJson(String name, Map<?, ?> map, JsonGenerator out) throws IOException {
        if (map == null || map.isEmpty()) {
            return;
        }
        if (name != null) {
            out.writeObjectFieldStart(name);
        } else {
            out.writeStartObject();
        }
        for (Map.Entry entry : map.entrySet()) {
            writeJsonForObject(String.valueOf(entry.getKey()), entry.getValue(), out);
        }
        out.writeEndObject();
    }
    
    public void writeJson(String name, RestModel model, JsonGenerator out) throws IOException {
        if (name != null) {
            out.writeObjectFieldStart(name);
        } else {
            out.writeStartObject();
        }
        for (Method m : model.getClass().getDeclaredMethods()) {
            if (m.getName().startsWith("get")) { // && !m.getName().equals("getClass")) {
                String propName = m.getName().substring(3);
                propName = Character.toLowerCase(propName.charAt(0)) + propName.substring(1);
                try {
                    writeJsonForObject(propName, m.invoke(model), out);
                } catch (Exception e) {}
            }
        }
        out.writeEndObject();
    }
    
    
}
