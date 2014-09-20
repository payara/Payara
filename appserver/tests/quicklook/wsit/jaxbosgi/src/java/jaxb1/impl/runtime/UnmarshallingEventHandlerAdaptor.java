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

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.helpers.ValidationEventImpl;
import javax.xml.bind.helpers.ValidationEventLocatorImpl;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Redirects events to another SAX ContentHandler.
 * 
 * <p>
 * Note that the SAXException returned by the ContentHandler is
 * unreported. So we have to catch them and report it, then rethrow
 * it if necessary. 
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class UnmarshallingEventHandlerAdaptor implements UnmarshallingEventHandler {
    
    protected final UnmarshallingContext context;

    /** This handler will receive SAX events. */
    protected final ContentHandler handler;

    public UnmarshallingEventHandlerAdaptor(UnmarshallingContext _ctxt,ContentHandler _handler) throws SAXException {
        this.context = _ctxt;
        this.handler = _handler;
        
        // emulate the start of documents
        try {
            handler.setDocumentLocator(context.getLocator());
            handler.startDocument();
            declarePrefixes( context.getAllDeclaredPrefixes() );
        } catch( SAXException e ) {
            error(e);
        }
    }

    public Object owner() {
        return null;
    }


    // nest level of elements.
    private int depth = 0;
        
    public void enterAttribute(String uri, String local, String qname) throws SAXException {
    }

    public void enterElement(String uri, String local, String qname, Attributes atts) throws SAXException {
        depth++;
        context.pushAttributes(atts,true);
        try {
            declarePrefixes(context.getNewlyDeclaredPrefixes());
            handler.startElement(uri,local,qname,atts);
        } catch( SAXException e ) {
            error(e);
        }
    }

    public void leaveAttribute(String uri, String local, String qname) throws SAXException {
    }

    public void leaveElement(String uri, String local, String qname) throws SAXException {
        try {
            handler.endElement(uri,local,qname);
            undeclarePrefixes(context.getNewlyDeclaredPrefixes());
        } catch( SAXException e ) {
            error(e);
        }
        context.popAttributes();
        
        depth--;
        if(depth==0) {
            // emulate the end of the document
            try {
                undeclarePrefixes(context.getAllDeclaredPrefixes());
                handler.endDocument();
            } catch( SAXException e ) {
                error(e);
            }
            context.popContentHandler();
        }
    }
    
    private void declarePrefixes( String[] prefixes ) throws SAXException {
        for( int i=prefixes.length-1; i>=0; i-- )
            handler.startPrefixMapping(
                prefixes[i],
                context.getNamespaceURI(prefixes[i]) );
    }
    
    private void undeclarePrefixes( String[] prefixes ) throws SAXException {
        for( int i=prefixes.length-1; i>=0; i-- )
            handler.endPrefixMapping( prefixes[i] );
    }

    public void text(String s) throws SAXException {
        try {
            handler.characters(s.toCharArray(),0,s.length());
        } catch( SAXException e ) {
            error(e);
        }
    }
    
    private void error( SAXException e ) throws SAXException {
        context.handleEvent(new ValidationEventImpl(
            ValidationEvent.ERROR,
            e.getMessage(),
            new ValidationEventLocatorImpl(context.getLocator()),
            e
        ), false);
    }

    public void leaveChild(int nextState) throws SAXException {
    }
}
