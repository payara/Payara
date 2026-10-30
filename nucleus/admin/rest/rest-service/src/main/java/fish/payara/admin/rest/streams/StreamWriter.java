/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
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

/**
 * Defines an interface that will handle streaming a data structure to an
 * {@link java.io.OutputStream}.
 */
public interface StreamWriter {

    /**
     * Returns a string of data that must appear at the end of the data stream.
     *
     * @return postfix
     */
    public String getPostfix();

    /**
     * Returns a string of data that must appear at the start of the data
     * stream. E.g. a namespace declaration.
     *
     * @return prefix
     */
    public String getPrefix();

    /**
     * Writes the data that is compulsory to appear at the start of the data
     * structure.
     * <br>
     * E.g. for XML this might be: {@code <?xml version="1.0" ?>}
     *
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link jakarta.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeStartDocument() throws Exception;

    /**
     * Writes the data that is compulsory to appear at the end of the data
     * structure.
     *
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link jakarta.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeEndDocument() throws Exception;

    /**
     * Writes the start of a new object, with the specified {@code name}.
     *
     * @param element The name of the object
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link jakarta.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeStartObject(String element) throws Exception;

    /**
     * Writes the end of an object. An object must have been started, or an
     * exception will be thrown.
     *
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link jakarta.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeEndObject() throws Exception;

    /**
     * Writes the start of an array, with the specified {@code name}. If the
     * data structure doesn't support arrays, this method will do nothing.
     *
     * @param element The name of the array
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link jakarta.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeStartArray(String element) throws Exception;

    /**
     * Writes the end of an array. An array must have been started, or an
     * exception will be thrown. If the data structure doesn't support arrays,
     * this method will do nothing.
     *
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link jakarta.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeEndArray() throws Exception;

    /**
     * Writes a {@code String} attribute with the specified {@code name} and
     * {@code value}. E.g. for JSON this will write: {@code "name":"value"}.
     *
     * @param name The name of the attribute
     * @param value The value of the attribute
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link jakarta.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeAttribute(String name, String value) throws Exception;

    /**
     * Writes a {@code Boolean} attribute with the specified {@code name} and
     * {@code value}. E.g. for JSON this will write: {@code "name":true/false}.
     *
     * @param name The name of the attribute
     * @param value The value of the attribute
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link jakarta.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeAttribute(String name, Boolean value) throws Exception;

    /**
     * Closes the {@link java.io.OutputStream} associated with this object. Some
     * OutputStreams require closing before any data will be written.
     *
     * @throws Exception Any exception thrown while closing the OutputStream.
     * E.g. for JSON this might be a
     * {@link jakarta.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void close() throws Exception;

}
