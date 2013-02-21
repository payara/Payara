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

import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationException;
import javax.xml.bind.Validator;
import javax.xml.bind.helpers.DefaultValidationEventHandler;

import org.xml.sax.SAXException;

import com.sun.xml.bind.validator.Messages;

/*
    TODO:
    reorganize classes into appropriate packages.
    to reflect the fact that some of the classes in
    the marshaller package are used for both marshalling
    and validation.

    In particular, the MarshallingContext interface should be
    renamed. It is not only for marshalling. 
    (something like "Serializer", maybe).
*/

/**
 * Validator implementation of JAXB RI.
 */
public class ValidatorImpl implements Validator
{
    /** Validation errors will be reported to this object. */
    private ValidationEventHandler eventHandler = 
        new DefaultValidationEventHandler();
    
    final DefaultJAXBContextImpl jaxbContext;
    
    public ValidatorImpl( DefaultJAXBContextImpl c ) {
        // initialize datatype converter with ours
        // DatatypeConverter.setDatatypeConverter(DatatypeConverterImpl.theInstance); NOT ALLOWED any more for the security reasons
        
        jaxbContext = c;
    }
    /**
     * We need to know whether an validation error was detected or not.
     * For this purpose, we set up the validation so that this interceptor
     * will "intercept" errors before the application receives it.
     */
    private static class EventInterceptor implements ValidationEventHandler {
        EventInterceptor( ValidationEventHandler _next ) {
            this.next = _next;
        }
        
        private boolean hadError = false;
        public boolean hadError() { return hadError; }
        
        /** event will be passed to this component. */
        private final ValidationEventHandler next;
        
        public boolean handleEvent( ValidationEvent e ) {
            hadError = true;
            boolean result;
            if( next!=null ) {
                // pass it to the application
                try {
                    result = next.handleEvent(e);
                } catch( RuntimeException re ) {
                    // if the client event handler causes a RuntimeException,
                    // then we have to return false
                    result = false;
                }
            } else {
                // if no error handler was specified, there is no point
                // in continuing the validation.
                result = false;
            }
            return result;
        }
    };

    public boolean validateRoot( Object o ) throws ValidationException {
        if( o == null ) {
            throw new IllegalArgumentException( 
                Messages.format( Messages.MUST_NOT_BE_NULL, "rootObj" ) );
        }
        
        return validate(o,true);
    }
    
    public boolean validate( Object o ) throws ValidationException {
        if( o == null ) {
            throw new IllegalArgumentException( 
                Messages.format( Messages.MUST_NOT_BE_NULL, "subrootObj" ) );
        }
        
        return validate(o,false);
    }
    
    private boolean validate( Object o, boolean validateId ) 
        throws ValidationException { 
            
        try {
        
            //ValidatableObject vo = Util.toValidatableObject(o);
            ValidatableObject vo = jaxbContext.getGrammarInfo().castToValidatableObject(o);
            
            if(vo==null)
                throw new ValidationException(
                    Messages.format( Messages.NOT_VALIDATABLE ) );
        
            EventInterceptor ei = new EventInterceptor(eventHandler);
            ValidationContext context = new ValidationContext(jaxbContext,ei,validateId);
            context.validate(vo);
            context.reconcileIDs();
            
            return !ei.hadError();
        } catch( SAXException e ) {
            // we need a consistent mechanism to convert SAXException into JAXBException
            Exception nested = e.getException();
            if( nested != null ) {
                throw new ValidationException( nested );
            } else {
                throw new ValidationException( e );
            }
            //return false;
        }
    }
    
    public ValidationEventHandler getEventHandler() {
        return eventHandler;
    }
    
    public void setEventHandler( ValidationEventHandler handler ) {
        if( handler == null ) {
            eventHandler = new DefaultValidationEventHandler();
        } else {
            eventHandler = handler;
        }
    }
    
    /**
     * There are no required properties, so simply throw an exception.  Other
     * providers may have support for properties on Validator, but the RI doesn't
     */
    public void setProperty( String name, Object value )
        throws PropertyException {
        
        if( name == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "name" ) );
        }

        throw new PropertyException(name, value);
    }
    
    /**
     * There are no required properties, so simply throw an exception.  Other
     * providers may have support for properties on Validator, but the RI doesn't
     */
    public Object getProperty( String name )
        throws PropertyException {
            
        if( name == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "name" ) );
        }

        throw new PropertyException(name);
    }

}
