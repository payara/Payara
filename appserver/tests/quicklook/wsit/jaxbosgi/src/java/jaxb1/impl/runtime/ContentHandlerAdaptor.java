/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package jaxb1.impl.runtime;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Receives SAX2 events and send the equivalent events to
 * {@link com.sun.xml.bind.serializer.XMLSerializer}
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class ContentHandlerAdaptor implements ContentHandler {

    /** Stores newly declared prefix-URI mapping. */
    private final ArrayList prefixMap = new ArrayList();
    
    /** Events will be sent to this object. */
    private final XMLSerializer serializer;
    
    private final StringBuffer text = new StringBuffer();
    
    
    public ContentHandlerAdaptor( XMLSerializer _serializer ) {
        this.serializer = _serializer;
    }
    
    

    public void startDocument() throws SAXException {
        prefixMap.clear();
    }

    public void endDocument() throws SAXException {
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        prefixMap.add(prefix);
        prefixMap.add(uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
        throws SAXException {
        
        flushText();

        int len = atts.getLength();
        
        serializer.startElement(namespaceURI,localName);
        // declare namespace events
        for( int i=0; i<len; i++ ) {
            String qname = atts.getQName(i);
            int idx = qname.indexOf(':');
            String prefix = (idx==-1)?qname:qname.substring(0,idx);
            
            serializer.getNamespaceContext().declareNamespace(
                atts.getURI(i), prefix, true );
        }
        for( int i=0; i<prefixMap.size(); i+=2 ) {
            String prefix = (String)prefixMap.get(i); 
            serializer.getNamespaceContext().declareNamespace(
                (String)prefixMap.get(i+1),
                prefix,
                prefix.length()!=0 );
        }
        
        serializer.endNamespaceDecls();
        // fire attribute events
        for( int i=0; i<len; i++ ) {
            serializer.startAttribute( atts.getURI(i), atts.getLocalName(i) );
            serializer.text(atts.getValue(i),null);
            serializer.endAttribute();
        }
        prefixMap.clear();
        serializer.endAttributes();
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        flushText();
        serializer.endElement();
    }
    
    private void flushText() throws SAXException {
        if( text.length()!=0 ) {
            serializer.text(text.toString(),null);
            text.setLength(0);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        text.append(ch,start,length);
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        text.append(ch,start,length);
    }



    public void setDocumentLocator(Locator locator) {
    }
    
    public void processingInstruction(String target, String data) throws SAXException {
    }

    public void skippedEntity(String name) throws SAXException {
    }

}
