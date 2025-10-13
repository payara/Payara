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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

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
        try (JsonParser jp = factory.createJsonParser(is)) {
            JsonToken token = jp.nextToken(); //sorounding object
            jp.nextToken(); //Name progress-status-event
            JsonToken token2 = jp.nextToken();
            if (token != JsonToken.START_OBJECT ||
                    token2 != JsonToken.START_OBJECT ||
                    !"progress-status-event".equals(jp.getCurrentName())) {
                throw new IOException("Not expected type (progress-status-event) but (" + jp.getCurrentName() + ")");
            }
            return readProgressStatusEvent(jp);
        }
    }

    public static ProgressStatusEvent readProgressStatusEvent(JsonParser jp) throws IOException {
        String id = null;
        JsonToken token = null;
        ProgressStatusEvent result = null;
        while ((token = jp.nextToken()) != JsonToken.END_OBJECT) {
            if (token == JsonToken.START_OBJECT) {
                String nm = jp.getCurrentName();
                if (null != nm) switch (nm) {
                    case "set":
                        result = new ProgressStatusEventSet(id);
                        readToPSEventSet((ProgressStatusEventSet) result, jp);
                        break;
                    case "progres":
                        result = new ProgressStatusEventProgress(id);
                        readToPSEventProgress((ProgressStatusEventProgress) result, jp);
                        break;
                    case "complete":
                        result = new ProgressStatusEventComplete(id);
                        readToPSEventComplete((ProgressStatusEventComplete) result, jp);
                        break;
                    case "create-child":
                        result = new ProgressStatusEventCreateChild(id);
                        readToPSEventCreateChild((ProgressStatusEventCreateChild) result, jp);
                        break;
                    default:
                        break;
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
            if (null != fieldname) switch (fieldname) {
                case "steps":
                    event.setSteps(jp.getIntValue());
                    break;
                case "message":
                    event.setMessage(jp.getText());
                    break;
                case "spinner":
                    event.setSpinner(jp.getBooleanValue());
                    break;
                default:
                    break;
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
            if (null != fieldname) switch (fieldname) {
                case "id":
                    event.setChildId(jp.getText());
                    break;
                case "allocated-steps":
                    event.setAllocatedSteps(jp.getIntValue());
                    break;
                case "total-step-count":
                    event.setTotalSteps(jp.getIntValue());
                    break;
                case "name":
                    event.setName(jp.getText());
                    break;
                default:
                    break;
            }
        }
    }
}
