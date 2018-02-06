/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import java.util.Stack;

/**
 * @author Kohsuke Kawaguchi
 */
public class IndentingXMLStreamWriter extends DelegatingXMLStreamWriter {
    private final static Object SEEN_NOTHING = new Object();
    private final static Object SEEN_ELEMENT = new Object();
    private final static Object SEEN_DATA = new Object();

    private Object state = SEEN_NOTHING;
    private Stack<Object> stateStack = new Stack<Object>();

    private String indentStep = "  ";
    private int depth = 0;

    public IndentingXMLStreamWriter(XMLStreamWriter writer) {
        super(writer);
    }

    /**
     * Return the current indent step.
     *
     * <p>Return the current indent step: each start tag will be
     * indented by this number of spaces times the number of
     * ancestors that the element has.</p>
     *
     * @return The number of spaces in each indentation step,
     *         or 0 or less for no indentation.
     * @see #setIndentStep(int)
     *
     * @deprecated
     *      Only return the length of the indent string.
     */
    public int getIndentStep() {
        return indentStep.length();
    }


    /**
     * Set the current indent step.
     *
     * @param indentStep The new indent step (0 or less for no
     *        indentation).
     * @see #getIndentStep()
     *
     * @deprecated
     *      Should use the version that takes string.
     */
    public void setIndentStep(int indentStep) {
        StringBuilder s = new StringBuilder();
        for (; indentStep > 0; indentStep--) s.append(' ');
        setIndentStep(s.toString());
    }

    public void setIndentStep(String s) {
        this.indentStep = s;
    }

    private void onStartElement() throws XMLStreamException {
        stateStack.push(SEEN_ELEMENT);
        state = SEEN_NOTHING;
        if (depth > 0) {
            super.writeCharacters("\n");
        }
        doIndent();
        depth++;
    }

    private void onEndElement() throws XMLStreamException {
        depth--;
        if (state == SEEN_ELEMENT) {
            super.writeCharacters("\n");
            doIndent();
        }
        state = stateStack.pop();
    }

    private void onEmptyElement() throws XMLStreamException {
        state = SEEN_ELEMENT;
        if (depth > 0) {
            super.writeCharacters("\n");
        }
        doIndent();
    }

    /**
     * Print indentation for the current level.
     *
     * @exception org.xml.sax.SAXException If there is an error
     *            writing the indentation characters, or if a filter
     *            further down the chain raises an exception.
     */
    private void doIndent() throws XMLStreamException {
        if (depth > 0) {
            for (int i = 0; i < depth; i++)
                super.writeCharacters(indentStep);
        }
    }


    public void writeStartDocument() throws XMLStreamException {
        super.writeStartDocument();
        super.writeCharacters("\n");
    }

    public void writeStartDocument(String version) throws XMLStreamException {
        super.writeStartDocument(version);
        super.writeCharacters("\n");
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        super.writeStartDocument(encoding, version);
        super.writeCharacters("\n");
    }

    public void writeStartElement(String localName) throws XMLStreamException {
        onStartElement();
        super.writeStartElement(localName);
    }

    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        onStartElement();
        super.writeStartElement(namespaceURI, localName);
    }

    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        onStartElement();
        super.writeStartElement(prefix, localName, namespaceURI);
    }

    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        onEmptyElement();
        super.writeEmptyElement(namespaceURI, localName);
    }

    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        onEmptyElement();
        super.writeEmptyElement(prefix, localName, namespaceURI);
    }

    public void writeEmptyElement(String localName) throws XMLStreamException {
        onEmptyElement();
        super.writeEmptyElement(localName);
    }

    public void writeEndElement() throws XMLStreamException {
        onEndElement();
        super.writeEndElement();
    }

    public void writeCharacters(String text) throws XMLStreamException {
        state = SEEN_DATA;
        super.writeCharacters(text);
    }

    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        state = SEEN_DATA;
        super.writeCharacters(text, start, len);
    }

    public void writeCData(String data) throws XMLStreamException {
        state = SEEN_DATA;
        super.writeCData(data);
    }
}
