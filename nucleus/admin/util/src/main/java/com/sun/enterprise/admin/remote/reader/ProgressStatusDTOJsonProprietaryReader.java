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
import org.glassfish.api.admin.progress.ProgressStatusDTO;
import org.glassfish.api.admin.progress.ProgressStatusDTO.ChildProgressStatusDTO;

/**
 *
 * @author mmares
 */
public class ProgressStatusDTOJsonProprietaryReader implements ProprietaryReader<ProgressStatusDTO> {

    private static final JsonFactory factory = new JsonFactory();

    @Override
    public boolean isReadable(Class<?> type, String mimetype) {
        return type.isAssignableFrom(ProgressStatusDTO.class);
    }

    public ProgressStatusDTO readFrom(final HttpURLConnection urlConnection) throws IOException {
        return readFrom(urlConnection.getInputStream(), urlConnection.getContentType());
    }

    @Override
    public ProgressStatusDTO readFrom(final InputStream is, final String contentType) throws IOException {
        JsonParser jp = factory.createJsonParser(is);
        try {
            JsonToken token = jp.nextToken(); //sorounding object
            jp.nextToken(); //Name progress-status
            JsonToken token2 = jp.nextToken();
            if (token != JsonToken.START_OBJECT ||
                    token2 != JsonToken.START_OBJECT ||
                    !"progress-status".equals(jp.getCurrentName())) {
                throw new IOException("Not expected type (progress-status) but (" + jp.getCurrentName() + ")");
            }
            return readProgressStatus(jp);
        } finally {
            jp.close();
        }
    }

    public static ProgressStatusDTO readProgressStatus(JsonParser jp) throws IOException {
        ChildProgressStatusDTO child = readChildProgressStatus(jp);
        return child.getProgressStatus();
    }

    public static ChildProgressStatusDTO readChildProgressStatus(JsonParser jp) throws IOException {
        ProgressStatusDTO psd = new ProgressStatusDTO();
        int allocatedSteps = 0;
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jp.getCurrentName();
            jp.nextToken(); // move to value
            if ("name".equals(fieldname)) {
                psd.setName(jp.getText());
            } else if ("id".equals(fieldname)) {
                psd.setId(jp.getText());
            } else if ("total-step-count".equals(fieldname)) {
                psd.setTotalStepCount(jp.getIntValue());
            } else if ("current-step-count".equals(fieldname)) {
                psd.setCurrentStepCount(jp.getIntValue());
            } else if ("complete".equals(fieldname)) {
                psd.setCompleted(jp.getBooleanValue());
            } else if ("allocated-steps".equals(fieldname)) {
                allocatedSteps = jp.getIntValue();
            } else if ("children".equals(fieldname)) {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
                        ProgressStatusDTO.ChildProgressStatusDTO child = readChildProgressStatus(jp);
                        psd.getChildren().add(child);
                    }
                }
            }
        }
        return new ChildProgressStatusDTO(allocatedSteps, psd);
    }

}
