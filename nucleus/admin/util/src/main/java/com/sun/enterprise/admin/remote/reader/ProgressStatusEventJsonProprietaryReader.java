/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.glassfish.api.admin.progress.ProgressStatusEvent;
import org.glassfish.api.admin.progress.ProgressStatusEventComplete;
import org.glassfish.api.admin.progress.ProgressStatusEventCreateChild;
import org.glassfish.api.admin.progress.ProgressStatusEventProgress;
import org.glassfish.api.admin.progress.ProgressStatusEventSet;

/**
 *
 * @author mmares
 */
public class ProgressStatusEventJsonProprietaryReader implements ProprietaryReader<ProgressStatusEvent> {

    private static final JsonFactory factory = new JsonFactory();

    @Override
    public boolean isReadable(Class<?> type, String mimetype) {
        return type.isAssignableFrom(ProgressStatusEvent.class);
    }

    public ProgressStatusEvent readFrom(HttpURLConnection urlConnection) throws IOException {
        return readFrom(urlConnection.getInputStream(), urlConnection.getContentType());
    }

    @Override
    public ProgressStatusEvent readFrom(final InputStream is, final String contentType) throws IOException {
        JsonParser jp = factory.createJsonParser(is);
        try {
            JsonToken token = jp.nextToken(); //sorounding object
            jp.nextToken(); //Name progress-status-event
            JsonToken token2 = jp.nextToken();
            if (token != JsonToken.START_OBJECT ||
                    token2 != JsonToken.START_OBJECT ||
                    !"progress-status-event".equals(jp.getCurrentName())) {
                throw new IOException("Not expected type (progress-status-event) but (" + jp.getCurrentName() + ")");
            }
            return readProgressStatusEvent(jp);
        } finally {
            jp.close();
        }
    }

    public static ProgressStatusEvent readProgressStatusEvent(JsonParser jp) throws IOException {
        String id = null;
        JsonToken token = null;
        ProgressStatusEvent result = null;
        while ((token = jp.nextToken()) != JsonToken.END_OBJECT) {
            if (token == JsonToken.START_OBJECT) {
                String nm = jp.getCurrentName();
                if ("set".equals(nm)) {
                    result = new ProgressStatusEventSet(id);
                    readToPSEventSet((ProgressStatusEventSet) result, jp);
                } else if ("progres".equals(nm)) {
                    result = new ProgressStatusEventProgress(id);
                    readToPSEventProgress((ProgressStatusEventProgress) result, jp);
                } else if ("complete".equals(nm)) {
                    result = new ProgressStatusEventComplete(id);
                    readToPSEventComplete((ProgressStatusEventComplete) result, jp);
                } else if ("create-child".equals(nm)) {
                    result = new ProgressStatusEventCreateChild(id);
                    readToPSEventCreateChild((ProgressStatusEventCreateChild) result, jp);
                }
            } else {
                String fieldname = jp.getCurrentName();
                if ("id".equals(fieldname)) {
                    jp.nextToken(); // move to value
                    id = jp.getText();
                }
            }
        }
        return result;
    }

    public static void readToPSEventSet(ProgressStatusEventSet event, JsonParser jp) throws IOException {
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jp.getCurrentName();
            jp.nextToken(); // move to value
            if ("total-step-count".equals(fieldname)) {
                event.setTotalStepCount(jp.getIntValue());
            } else if ("current-step-count".equals(fieldname)) {
                event.setCurrentStepCount(jp.getIntValue());
            }
        }
    }

    public static void readToPSEventProgress(ProgressStatusEventProgress event, JsonParser jp) throws IOException {
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jp.getCurrentName();
            jp.nextToken(); // move to value
            if ("steps".equals(fieldname)) {
                event.setSteps(jp.getIntValue());
            } else if ("message".equals(fieldname)) {
                event.setMessage(jp.getText());
            } else if ("spinner".equals(fieldname)) {
                event.setSpinner(jp.getBooleanValue());
            }
        }
    }

    public static void readToPSEventComplete(ProgressStatusEventComplete event, JsonParser jp) throws IOException {
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jp.getCurrentName();
            jp.nextToken(); // move to value
            if ("message".equals(fieldname)) {
                event.setMessage(jp.getText());
            }
        }
    }

    public static void readToPSEventCreateChild(ProgressStatusEventCreateChild event, JsonParser jp) throws IOException {
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jp.getCurrentName();
            jp.nextToken(); // move to value
            if ("id".equals(fieldname)) {
                event.setChildId(jp.getText());
            } else if ("allocated-steps".equals(fieldname)) {
                event.setAllocatedSteps(jp.getIntValue());
            } else if ("total-step-count".equals(fieldname)) {
                event.setTotalSteps(jp.getIntValue());
            } else if ("name".equals(fieldname)) {
                event.setName(jp.getText());
            }
        }
    }
}
