/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.StreamReaderDelegate;

/**
 * {@link XMLStreamReader} wrapper that cuts off sub-trees.
 * @author Kohsuke Kawaguchi
 */
abstract class XMLStreamReaderFilter extends StreamReaderDelegate {
    XMLStreamReaderFilter(XMLStreamReader reader) {
        super(reader);
    }

    XMLStreamReaderFilter() {
    }

    public int next() throws XMLStreamException {
        while(true) {
            int r = super.next();
            if(r != START_ELEMENT || !filterOut())
                return r;
            skipTree();
        }
    }

    public int nextTag() throws XMLStreamException {
        while(true) {
            // Fix for issue 9127
            // The following call to super.nextTag() is replaced with thisNextTag() 
            int r = thisNextTag();
            if(r != START_ELEMENT || !filterOut())
                return r;
            skipTree();
        }
    }

    // Fix for issue 9127
    // This method is a modified version of the super.nextTag()
    // In addition to all other event types skipped in super.nextTag() in search for 
    // START_ELEMENT this method also includes DTD eventType in the skip-list 
    private int thisNextTag() throws XMLStreamException {
        int eventType = super.next();
        while((eventType == XMLStreamConstants.CHARACTERS && isWhiteSpace()) // skip whitespace
                || (eventType == XMLStreamConstants.CDATA && isWhiteSpace())
                // skip whitespace
                || eventType == XMLStreamConstants.SPACE
                || eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                || eventType == XMLStreamConstants.COMMENT
                || eventType == XMLStreamConstants.DTD) {
            eventType = super.next();
        }

        if(eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
            throw new XMLStreamException(
                    "found: " + getEventTypeString(eventType)
                    + ", expected " + getEventTypeString(XMLStreamConstants.START_ELEMENT)
                    + " or " + getEventTypeString(XMLStreamConstants.END_ELEMENT));
        }

        return eventType;

    }

    final static String getEventTypeString(int eventType) {
        switch (eventType) {
            case XMLEvent.START_ELEMENT:
                return "START_ELEMENT";
            case XMLEvent.END_ELEMENT:
                return "END_ELEMENT";
            case XMLEvent.PROCESSING_INSTRUCTION:
                return "PROCESSING_INSTRUCTION";
            case XMLEvent.CHARACTERS:
                return "CHARACTERS";
            case XMLEvent.COMMENT:
                return "COMMENT";
            case XMLEvent.START_DOCUMENT:
                return "START_DOCUMENT";
            case XMLEvent.END_DOCUMENT:
                return "END_DOCUMENT";
            case XMLEvent.ENTITY_REFERENCE:
                return "ENTITY_REFERENCE";
            case XMLEvent.ATTRIBUTE:
                return "ATTRIBUTE";
            case XMLEvent.DTD:
                return "DTD";
            case XMLEvent.CDATA:
                return "CDATA";
            case XMLEvent.SPACE:
                return "SPACE";
        }
        return "UNKNOWN_EVENT_TYPE, " + String.valueOf(eventType);
    }

    /**
     * Skips a whole subtree, and return with the cursor pointing to the end element
     * of the skipped subtree.
     */
    private void skipTree() throws XMLStreamException {
        int depth = 1;

        while(depth > 0) {
            // nextTag may cause problems.  We are just throwing it all away so
            // next() is fine...
            int r = super.next();

            if(r == START_ELEMENT) {
                depth++;
            }
            else if(r == END_ELEMENT) {
                depth--;
            }
            // else ignore everything else...
        }
    }

    /**
     * Called when the parser is at the start element state, to decide if we are to skip the current element
     * or not.
     */
    abstract boolean filterOut() throws XMLStreamException;
}
