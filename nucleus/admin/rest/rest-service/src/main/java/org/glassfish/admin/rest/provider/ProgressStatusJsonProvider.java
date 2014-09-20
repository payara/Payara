/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Set;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.glassfish.api.admin.ProgressStatus;
import org.glassfish.api.admin.progress.ProgressStatusBase;
import org.glassfish.api.admin.progress.ProgressStatusBase.ChildProgressStatus;

/** Marshal ProgressStatus to JSON
 *
 * @author mmares
 */
@Provider
@Produces({MediaType.APPLICATION_JSON, "application/x-javascript"})
public class ProgressStatusJsonProvider extends BaseProvider<ProgressStatusBase> {

    private static final JsonFactory factory = new JsonFactory();

    public ProgressStatusJsonProvider() {
        super(ProgressStatus.class, MediaType.APPLICATION_JSON_TYPE, new MediaType("application", "x-javascript"));
    }

    @Override
    protected boolean isGivenTypeWritable(Class<?> type, Type genericType) {
        return desiredType.isAssignableFrom(type);
    }

    @Override
    public void writeTo(ProgressStatusBase proxy, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        JsonGenerator out = factory.createJsonGenerator(entityStream, JsonEncoding.UTF8);
        out.writeStartObject();
        writeJson("progress-status", proxy, -1, out);
        out.writeEndObject();
        out.flush();
    }

    public void writeJson(String name, ProgressStatusBase ps, int allocatedSteps, JsonGenerator out) throws IOException {
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
        if (allocatedSteps >= 0) {
            out.writeNumberField("allocated-steps", allocatedSteps);
        }
        out.writeNumberField("total-step-count", ps.getTotalStepCount());
        out.writeNumberField("current-step-count", ps.getCurrentStepCount());
        out.writeBooleanField("complete", ps.isComplete());
        Set<ChildProgressStatus> children = ps.getChildProgressStatuses();
        if (children != null && !children.isEmpty()) {
            out.writeArrayFieldStart("children");
            for (ChildProgressStatus child : children) {
                writeJson(null, child.getProgressStatus(), child.getAllocatedSteps(), out);
            }
            out.writeEndArray();
        }
        out.writeEndObject();
    }

    @Override
    public String getContent(ProgressStatusBase proxy) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
