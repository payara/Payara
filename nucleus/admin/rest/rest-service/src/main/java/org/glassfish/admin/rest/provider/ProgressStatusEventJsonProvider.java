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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.glassfish.api.admin.progress.ProgressStatusDTO;
import org.glassfish.api.admin.progress.ProgressStatusEvent;

/**
 *
 * @author mmares
 */
@Provider
@Produces({MediaType.APPLICATION_JSON, "application/x-javascript"})
public class ProgressStatusEventJsonProvider extends BaseProvider<ProgressStatusEvent> {
    
    private static final JsonFactory factory = new JsonFactory();
    
    public ProgressStatusEventJsonProvider() {
        super(ProgressStatusEvent.class, MediaType.APPLICATION_JSON_TYPE, new MediaType("application", "x-javascript"));
    }
    
    @Override
    protected boolean isGivenTypeWritable(Class<?> type, Type genericType) {
        return desiredType.isAssignableFrom(type);
    }
    
    @Override
    public void writeTo(ProgressStatusEvent proxy, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        JsonGenerator out = factory.createJsonGenerator(entityStream, JsonEncoding.UTF8);
        out.writeStartObject();
        writeJson("progress-status-event", proxy, out);
        out.writeEndObject();
        out.flush();
    }
    
    public void writeJson(String name, ProgressStatusEvent event, JsonGenerator out) throws IOException {
        if (event == null) {
            return;
        }
        if (name != null) {
            out.writeObjectFieldStart(name);
        } else {
            out.writeStartObject();
        }
        if (event.getChanged() != null && event.getChanged().length > 0) {
            out.writeArrayFieldStart("changed");
            for (ProgressStatusEvent.Changed chng : event.getChanged()) {
                if (chng != null) {
                    out.writeString(chng.name());
                }
            }
            out.writeEndArray();
        }
        if (event.getMessage() != null && !event.getMessage().isEmpty()) {
            out.writeStringField("message", event.getMessage());
        }
        if (event.getAllocatedSteps() > 0) {
            out.writeNumberField("allocated-steps", event.getAllocatedSteps());
        }
        if (event.getParentSourceId() != null) {
            out.writeStringField("parent-id", event.getParentSourceId());
        }
        writeJson("progress-status", event.getSource(), out);
        out.writeEndObject();
    }
    
    public void writeJson(String name, ProgressStatusDTO ps, JsonGenerator out) throws IOException {
        if (ps == null) {
            return;
        }
        if (name != null) {
            out.writeObjectFieldStart(name);
        } else {
            out.writeStartObject();
        }
        out.writeStringField("name", ps.getName());
        out.writeStringField("id", ps.getId());
        out.writeNumberField("total-step-count", ps.getTotalStepCount());
        out.writeNumberField("current-step-count", ps.getCurrentStepCount());
        out.writeBooleanField("complete", ps.isCompleted());
        out.writeEndObject();
    }

    @Override
    public String getContent(ProgressStatusEvent proxy) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
