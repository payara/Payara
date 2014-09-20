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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.helpers.NotIdentifiableEventImpl;
import javax.xml.bind.helpers.ValidationEventImpl;
import javax.xml.bind.helpers.ValidationEventLocatorImpl;

import org.xml.sax.SAXException;

import com.sun.xml.bind.ProxyGroup;
import com.sun.xml.bind.serializer.AbortSerializationException;
import com.sun.xml.bind.validator.Messages;

/**
 * Maintains information that needs to be stored across
 * validations of multiple objects.
 * 
 * Specifically, this object is responsible for:
 * 
 * <ol>
 *   <li>detecting a cycle in a content tree by keeping track of
 *       objects that were validated.
 * 
 *   <li>keeping an instance of NamespaceContextImpl, which is
 *       shared by all MSVValidators.
 * 
 *   <li>keeping a reference to {@link ValidationErrorHandler}.
 *       MSVValidators should use this error handler to report any error.
 * </ol>
 */
class ValidationContext
{
    final DefaultJAXBContextImpl jaxbContext;
    /**
     * @param validateID
     *      if true, ID/IDREF validation will be performed.
     */
    ValidationContext( DefaultJAXBContextImpl _context, ValidationEventHandler _eventHandler, boolean validateID ) {
        this.jaxbContext = _context;
        this.eventHandler = _eventHandler;
        this.validateID = validateID;
    }
    

    
//
//
// detecting cycles.
//
//
    
    /** Set of all validated objects. Used to detect a cycle. */
    private final IdentityHashSet validatedObjects = new IdentityHashSet();
    
    /**
     * Validates the sub-tree rooted at <code>vo</code> and reports
     * any errors/warnings to the error handler.
     */
    public void validate( ValidatableObject vo ) throws SAXException {
        if( validatedObjects.add(ProxyGroup.unwrap(vo)) ) {
            // setup a new validator for this object and validate it.
            MSVValidator.validate(jaxbContext,this,vo);
        } else {
            // this object has already been validated once.
            reportEvent( vo, Messages.format( Messages.CYCLE_DETECTED ) );
        }
    }

    
//
//
// Keeping track of namespace bindings.
//
//
    
    /** namespace context. */
    private final NamespaceContextImpl nsContext = new NamespaceContextImpl(null);
    
    public NamespaceContextImpl getNamespaceContext() { return nsContext; }
    

//
//
// ID/IDREF validation
//
//
    /** ID/IDREF validation is done only when this flag is true. */
    private final boolean validateID;
    
    private final HashSet IDs = new HashSet();
    private final HashMap IDREFs = new HashMap();
    
    public String onID( XMLSerializable owner, String value ) throws SAXException {
            
        if(!validateID) return value;
        
        if(!IDs.add(value)) {
            // this ID value has already been used.
            //reportEvent(Util.toValidatableObject(owner),
            //    Messages.format(Messages.DUPLICATE_ID,value));
            reportEvent(jaxbContext.getGrammarInfo().castToValidatableObject(owner),
                Messages.format(Messages.DUPLICATE_ID,value));
        }
        
        return value;
    }
    public String onIDREF( XMLSerializable referer, String value ) throws SAXException {
        if(!validateID) return value;
        
        if(IDs.contains(value))
            return value; // this IDREF has the corresponding ID.
        
        // remember the value to check the value later.
        IDREFs.put(value,referer);
        
        return value;
    }
    /** Tests if all IDREFs have corresponding IDs. */
    protected void reconcileIDs() throws SAXException {
        if(!validateID) return;
        
        for (Iterator itr = IDREFs.entrySet().iterator(); itr.hasNext();) {
            Map.Entry e = (Map.Entry) itr.next();
            
            if(IDs.contains(e.getKey()))
                continue;   // OK.
            
            // ID was not found.
            ValidatableObject source = (ValidatableObject)e.getValue();
            reportEvent(
                source,                
                new NotIdentifiableEventImpl( 
                    ValidationEvent.ERROR,
                    Messages.format( Messages.ID_NOT_FOUND, e.getKey() ),
                    new ValidationEventLocatorImpl( source ) ) );
        }
        
        IDREFs.clear();
    }

    
//
//
// Keeping track of ValidationErrorHandler
//
//
    private final ValidationEventHandler eventHandler;
    
    /**
     * Reports an error to the application.
     */
    public void reportEvent(ValidatableObject source, String formattedMessage) throws AbortSerializationException {
        reportEvent( 
            source, 
            new ValidationEventImpl( ValidationEvent.ERROR, 
                                     formattedMessage,
                                     new ValidationEventLocatorImpl(source) ) );
    }
    
    /**
     * Reports an error to the client.
     * This version should be used when an exception is thrown from sub-modules.
     */
    public void reportEvent(ValidatableObject source, Exception nestedException ) throws AbortSerializationException {
        reportEvent( 
            source, 
            new ValidationEventImpl( ValidationEvent.ERROR, 
                                     nestedException.toString(),
                                     new ValidationEventLocatorImpl(source),
                                     nestedException ) );
    }
    
    public void reportEvent( ValidatableObject source, ValidationEvent event ) throws AbortSerializationException {
        boolean r;
        
        try {
            r = eventHandler.handleEvent( event );
        } catch( RuntimeException re ) {
            // if the client event handler causes a RuntimeException, then
            // we have to return false.
            r = false;
        }
        
        if(!r) {
            // throw an exception to abort validation
            throw new AbortSerializationException( event.getMessage() );
        }
    }
        
    

}
