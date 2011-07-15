/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.flashlight.xml;

/**
 * An abstract class that you can build upon to make your own custom parser.
 * @author bnevins
 */

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.*;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

public abstract class StaxParser {

    abstract protected void read() throws XMLStreamException, EndDocumentException;

    public StaxParser(File f) throws XMLStreamException {
        try {
            xmlStream = new FileInputStream(f);
            createParser();
        }
        catch(IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }
    
    public StaxParser(String resource, ClassLoader cl) throws XMLStreamException {
        xmlStream = cl.getResourceAsStream(resource);
        createParser();
    }

    public StaxParser(InputStream is) throws XMLStreamException {
        xmlStream = is;
        createParser();
    }

    /**
     * The same as calling XmlStreamReader.next() except that we throw a special
     * Exception if the end of the document was reached
     * @return
     * @throws XMLStreamException
     * @throws xml.StaxParser.EndDocumentException if the end of the document is here
     */
     protected int next() throws XMLStreamException, EndDocumentException {
        int event = parser.next();
        if (event == END_DOCUMENT) {
            parser.close();
            throw new EndDocumentException();
        }
        return event;
    }

    /**
     * The cursor will be pointing at the START_ELEMENT of name when it returns.
     * note that skipTree must be called.  Otherwise we could be fooled by a
     * sub-element with the same name as an outer element
     *
     * @param name the Element to skip to
     * @throws javax.xml.stream.XMLStreamException
     */
    protected void skipTo(String name) throws XMLStreamException, EndDocumentException {
        while (true) {
            nextStart();
            // cursor is at a START_ELEMENT
            String localName = parser.getLocalName();
            if (name.equals(localName)) {
                return;
            } else {
                skipTree(localName);
            }
        }
    }

    /**
     * The cursor must be pointing at a START_ELEMENT.  Returns all attributes
     * in a Map
     * @return Map<String, String> of all attributes
     * @throws IllegalStateException if the cursor is not pointing at a START_ELEMENT
     */

    protected Map<String, String> parseAttributes() {
        int num = parser.getAttributeCount();
        Map<String, String> map = new HashMap<String, String>();

        for (int i = 0; i < num; i++) {
            map.put(parser.getAttributeName(i).getLocalPart(), parser.getAttributeValue(i));
        }

        return map;
    }
    /**
     * Skip to the first START_ELEMENT after the given START_ELEMENT name
     * This is useful for skipping past the root element
     * @param name The START_ELEMENT to skip past
     * @throws XMLStreamException if any errors
     * @throws xml.StaxParser.EndDocumentException if end of document reached first
     */
    protected void skipPast(String name) throws XMLStreamException, EndDocumentException {
        // Move to the first 'top-level' element under name
        // Return with cursor pointing to first sub-element
        skipTo(name);
        nextStart();
    }

    /**
     * Skip to the next START_ELEMENT
     * @throws XMLStreamException
     * @throws xml.StaxParser.EndDocumentException
     */
    protected void nextStart() throws XMLStreamException, EndDocumentException {
        while (next() != START_ELEMENT)
            ;
    }

    protected void close() {
        try {
            if (parser != null) {
                parser.close();
            }
        }
        catch (Exception e) {
            // ignore
        }
        try {
            if (xmlStream != null) {
                xmlStream.close();
            }
        }
        catch (Exception e) {
            // ignore
        }
    }

     /////////////////////  private below //////////////////////////////////////

     private void createParser() throws XMLStreamException {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        parser = xif.createXMLStreamReader(xmlStream);
    }


     private void skipTree(String name) throws XMLStreamException, EndDocumentException {
        // The cursor is pointing at the start-element of name.
        // throw everything in this element away and return with the cursor
        // pointing at its end-element.
        while (true) {
            int event = next();
            if (event == END_ELEMENT && name.equals(parser.getLocalName())) {
                return;
            }
        }
    }

     // this is so we can return from arbitrarily nested calls -- it is VERY easy
     // to get into an infinite loop without this!
    protected static class EndDocumentException extends Exception {
        EndDocumentException() {
        }
    }
    private InputStream xmlStream;
    protected XMLStreamReader parser;
}
