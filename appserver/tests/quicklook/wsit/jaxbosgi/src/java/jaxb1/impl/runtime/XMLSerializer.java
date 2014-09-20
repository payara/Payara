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

import org.xml.sax.SAXException;

import com.sun.xml.bind.JAXBObject;
import com.sun.xml.bind.marshaller.IdentifiableObject;
import com.sun.xml.bind.serializer.AbortSerializationException;

/**
 * Receives XML serialization event
 * 
 * <p>
 * This object coordinates the overall marshalling efforts across different
 * content-tree objects and different target formats.
 * 
 * <p>
 * The following CFG gives the proper sequence of method invocation.
 * 
 * <pre>
 * MARSHALLING  :=  ELEMENT
 * ELEMENT      :=  "startElement" NSDECL* "endNamespaceDecls"
 *                        ATTRIBUTE* "endAttributes" BODY "endElement"
 * 
 * NSDECL       :=  "declareNamespace"
 * 
 * ATTRIBUTE    :=  "startAttribute" ATTVALUES "endAttribute"
 * ATTVALUES    :=  "text"*
 * 
 * 
 * BODY         :=  ( "text" | ELEMENT )*
 * </pre>
 * 
 * <p>
 * A marshalling of one element consists of two stages. The first stage is
 * for marshalling attributes and collecting namespace declarations.
 * The second stage is for marshalling characters/child elements of that element.
 * 
 * <p>
 * Observe that multiple invocation of "text" is allowed.
 * 
 * <p>
 * Also observe that the namespace declarations are allowed only between
 * "startElement" and "endAttributes".
 * 
 * 
 * @author  Kohsuke Kawaguchi
 */
public interface XMLSerializer
{
    /**
     * Errors detected by the XMLSerializable should be either thrown
     * as {@link SAXException} or reported through this method.
     * 
     * The callee should report an error to the client application
     * and 
     */
    void reportError( ValidationEvent e ) throws AbortSerializationException;
    
    /**
     * Starts marshalling of an element.
     * Calling this method will push the internal state into the
     * internal stack.
     */
    void startElement( String uri, String local ) throws SAXException;
    
    /**
     * Switches to the mode to marshal attribute values.
     * This method has to be called after the 1st pass is completed.
     */
    void endNamespaceDecls() throws SAXException;
    
    /**
     * Switches to the mode to marshal child texts/elements.
     * This method has to be called after the 2nd pass is completed.
     */
    void endAttributes() throws SAXException;
    
    /**
     * Ends marshalling of an element.
     * Pops the internal stack.
     */
    void endElement() throws SAXException;
    
    
    /**
     * Marshalls text.
     * 
     * <p>
     * This method can be called (i) after the startAttribute method
     * and (ii) before the endAttribute method, to marshal attribute values.
     * If the method is called more than once, those texts are considered
     * as separated by whitespaces. For example,
     * 
     * <pre>
     * c.startAttribute();
     * c.text("abc");
     * c.text("def");
     * c.endAttribute("","foo");
     * </pre>
     * 
     * will generate foo="abc def".
     * 
     * <p>
     * Similarly, this method can be called after the endAttributes
     * method to marshal texts inside elements. The same rule about
     * multiple invokations apply to this case, too. For example,
     * 
     * <pre>
     * c.startElement("","foo");
     * c.endNamespaceDecls();
     * c.endAttributes();
     * c.text("abc");
     * c.text("def");
     *   c.startElement("","bar");
     *   c.endAttributes();
     *   c.endElement();
     * c.text("ghi");
     * c.endElement();
     * </pre>
     * 
     * will generate <code>&lt;foo>abc def&lt;bar/>ghi&lt;/foo></code>.
     */
    void text( String text, String fieldName ) throws SAXException;
    
    
    /**
     * Starts marshalling of an attribute.
     * 
     * The marshalling of an attribute will be done by
     * <ol>
     *  <li>call the startAttribute method
     *  <li>call the text method (several times if necessary)
     *  <li>call the endAttribute method
     * </ol>
     * 
     * No two attributes can be marshalled at the same time.
     * Note that the whole attribute marshalling must be happened
     * after the startElement method and before the endAttributes method.
     */
    void startAttribute( String uri, String local ) throws SAXException;

    void endAttribute() throws SAXException;
    
    /**
     * Obtains a namespace context object, which is used to
     * declare/obtain namespace bindings.
     */
    NamespaceContext2 getNamespaceContext();
    
    
    /**
     * Notifies the serializer that an ID value has just marshalled.
     * 
     * The serializer may or may not check the consistency of ID/IDREFs
     * and may throw a SAXException.
     * 
     * @param owner
     *      JAXB content object that posesses the ID.
     * @param value
     *      The value of the ID.
     * 
     * @return
     *      Return the value parameter without any modification,
     *      so that the invocation of this method can be done transparently
     *      by a transducer.
     */
    String onID( IdentifiableObject owner, String value ) throws SAXException;
    
    /**
     * Notifies the serializer that an IDREF value has just marshalled.
     * 
     * The serializer may or may not check the consistency of ID/IDREFs
     * and may throw a SAXException.
     * 
     * @return
     *      Return the value parameter without any modification.
     *      so that the invocation of this method can be done transparently
     *      by a transducer.
     */
    String onIDREF( IdentifiableObject obj ) throws SAXException;
    
    
    // I suppose we don't want to use SAXException. -kk
    
    
    
    // those method signatures are purposely made to JAXBContext, not
    // XMLSerializable, to avoid N^2 proxy overhead.  
    
    /**
     * This method is called when an JAXBObject object is found
     * while the marshaller is in the "element" mode (i.e. marshalling
     * a content model of an element)
     * 
     * @param fieldName
     *      property name of the parent objeect from which 'o' comes.
     *      Used as a part of the error message in case anything goes wrong
     *      with 'o'. 
     */
    void childAsBody( JAXBObject o, String fieldName ) throws SAXException;
    
    /**
     * This method is called when an JAXBObject object is found
     * while the marshaller is in the "attribute" mode (i.e. marshalling
     * attributes of an element)
     * 
     * @param fieldName
     *      property name of the parent objeect from which 'o' comes.
     *      Used as a part of the error message in case anything goes wrong
     *      with 'o'. 
     */
    void childAsAttributes( JAXBObject o, String fieldName ) throws SAXException;
    
    /**
     * This method is called when an JAXBObject object is found
     * while the marshaller is in the "URI" mode.
     * 
     * @param fieldName
     *      property name of the parent objeect from which 'o' comes.
     *      Used as a part of the error message in case anything goes wrong
     *      with 'o'. 
     */
    void childAsURIs( JAXBObject o, String fieldName ) throws SAXException;
}
