/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.admin.rest.streams;

import java.io.IOException;
import java.io.OutputStream;
import jakarta.json.Json;
import jakarta.json.stream.JsonGenerationException;
import jakarta.json.stream.JsonGenerator;
import org.glassfish.admin.rest.Constants;

/**
 * A {@link StreamWriter} for handling JSON.
 */
public class JsonStreamWriter implements StreamWriter {

    private final String prefix;
    private final String postfix;
    private final OutputStream os;
    private final JsonGenerator writer;

    private boolean inArray;

    /**
     * Creates a {@link StreamWriter} for handling JSON.
     *
     * @param os The OutputStream to write to.
     * @param prefix Any data that needs writing at the start of the stream.
     * @param postfix Any data that needs writing at the end of the stream.
     */
    public JsonStreamWriter(OutputStream os, String prefix, String postfix) {
        this.prefix = prefix;
        this.postfix = postfix;
        this.os = os;
        this.writer = Json.createGenerator(os);
    }

    /**
     * Creates a {@link StreamWriter} for handling JSON, with a {@code null}
     * prefix and postfix.
     *
     * @param os The OutputStream to write to.
     */
    public JsonStreamWriter(OutputStream os) {
        this(os, null, null);
    }

    @Override
    public String getPostfix() {
        return postfix;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void writeStartDocument() throws JsonGenerationException, IOException {
        if (prefix != null) {
            os.write(prefix.getBytes(Constants.ENCODING));
        }
        writer.writeStartObject();
        inArray = false;
    }

    @Override
    public void writeEndDocument() throws JsonGenerationException, IOException {
        writer.writeEnd();
        if (postfix != null) {
            os.write(postfix.getBytes(Constants.ENCODING));
        }
    }

    @Override
    public void writeStartObject(String element) throws JsonGenerationException {
        // Objects inside arrays in JSON don't have names
        if (inArray) {
            writer.writeStartObject();
        } else {
            writer.writeStartObject(element);
        }
    }

    @Override
    public void writeEndObject() throws JsonGenerationException {
        writer.writeEnd();
    }

    @Override
    public void writeAttribute(String name, String value) throws JsonGenerationException {
        writer.write(name, value);
    }

    @Override
    public void writeAttribute(String name, Boolean value) throws JsonGenerationException {
        writer.write(name, value);
    }

    @Override
    public void close() throws JsonGenerationException {
        writer.close();
    }

    @Override
    public void writeStartArray(String element) throws Exception {
        writer.writeStartArray(element);
        inArray = true;
    }

    @Override
    public void writeEndArray() throws Exception {
        writer.writeEnd();
        inArray = false;
    }

}
