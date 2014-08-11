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
package com.sun.enterprise.admin.remote.sse;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com), Martin Mares (martin.mares at oracle.com)
 */
public final class GfSseEventReceiver implements Closeable {

    private enum State {
        START,

        COMMENT,
        FIELD_NAME,
        FIELD_VALUE_FIRST,
        FIELD_VALUE,

        EVENT_FIRED
    }

    private final InputStream inputStream;
    private boolean closed = false;

    /**
     * Constructor.
     */
    GfSseEventReceiver(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public GfSseInboundEvent readEvent() throws IOException {
        GfSseInboundEvent inboundEvent = new GfSseInboundEvent();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        State currentState = State.START;
        String fieldName = null;

        try {
            int data = 0;
            while((data = inputStream.read()) != -1) {

                switch (currentState) {

                    case START:
                        if(data == ':') {
                            currentState = State.COMMENT;
                        } else if(data != '\n') {
                            baos.write(data);
                            currentState = State.FIELD_NAME;
                        } else if(data == '\n') {
//                            System.out.println(sbr);
                            if(!inboundEvent.isEmpty()) {
                                return inboundEvent;
                            }
                            inboundEvent = new GfSseInboundEvent();
                        }
                        break;
                    case COMMENT:
                        if(data == '\n') {
                            currentState = State.START;
                        }
                        break;
                    case FIELD_NAME:
                        if(data == ':') {
                            fieldName = baos.toString();
                            baos.reset();
                            currentState = State.FIELD_VALUE_FIRST;
                        } else if(data == '\n') {
                            processField(inboundEvent, baos.toString(), "".getBytes());
                            baos.reset();
                            currentState = State.START;
                        } else {
                            baos.write(data);
                        }
                        break;
                    case FIELD_VALUE_FIRST:
                        // first space has to be skipped
                        if(data != ' ') {
                            baos.write(data);
                        }

                        if(data == '\n') {
                            processField(inboundEvent, fieldName, baos.toByteArray());
                            baos.reset();
                            currentState = State.START;
                            break;
                        }

                        currentState = State.FIELD_VALUE;
                        break;
                    case FIELD_VALUE:
                        if(data == '\n') {
                            processField(inboundEvent, fieldName, baos.toByteArray());
                            baos.reset();
                            currentState = State.START;
                        } else {
                            baos.write(data);
                        }
                        break;
                    default:
                        //No-op default
                }

            }
            if(data == -1) {
                closed = true;
            }
            return null;
        } catch (IOException e) {
            closed = true;
            throw e;
        }

    }

    private void processField(GfSseInboundEvent inboundEvent, String name, byte[] value) {
        if(name.equals("event")) {
            inboundEvent.setName(new String(value));
        } else if(name.equals("data")) {
            inboundEvent.addData(value);
            inboundEvent.addData(new byte[]{'\n'});
        } else if(name.equals("id")) {
            String s = "";
            if (value != null) {
                s = new String(value);
                s = s.trim();
                if (!s.matches("\\-?\\d+")) {
                    s = "";
                }
            }
            inboundEvent.setId(s);
        }
    }

    /**
     * Get object state.
     *
     * @return true if no new {@link InboundEvent} can be received, false otherwise.
     */
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        inputStream.close();
    }

}
