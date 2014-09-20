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
import javax.xml.bind.ValidationEventLocator;
import javax.xml.bind.helpers.ValidationEventImpl;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sun.xml.bind.validator.Locator;

/**
 * Receives errors through {@link ErrorHandler} and reports to the
 * {@link SAXUnmarshallerHandler}.
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class ErrorHandlerAdaptor implements ErrorHandler {
    
    /** the client event handler that will receive the validation events */
    private final SAXUnmarshallerHandler host;
    
    /** the locator object responsible for filling in the validation event
     *  location info **/
    private final Locator locator;
   
    public ErrorHandlerAdaptor(
        SAXUnmarshallerHandler _host, Locator locator ) {
        this.host = _host;
        this.locator = locator;
    }
    
    public void error(SAXParseException exception) 
        throws SAXException {
            
        propagateEvent( ValidationEvent.ERROR, exception );
    }
    
    public void warning(SAXParseException exception) 
        throws SAXException {
            
        propagateEvent( ValidationEvent.WARNING, exception );
    }
    
    public void fatalError(SAXParseException exception) 
        throws SAXException {
            
        propagateEvent( ValidationEvent.FATAL_ERROR, exception );
    }
    
    private void propagateEvent( int severity, SAXParseException saxException ) 
        throws SAXException {
            
        // get location info:
        //     sax locators simply use the location info embedded in the 
        //     sax exception, dom locators keep a reference to their DOMScanner
        //     and call back to figure out where the error occurred.
        ValidationEventLocator vel = 
            locator.getLocation( saxException );

        ValidationEventImpl ve = 
            new ValidationEventImpl( severity, saxException.getMessage(), vel  );

        Exception e = saxException.getException();
        if( e != null ) {
            ve.setLinkedException( e );
        } else {
            ve.setLinkedException( saxException );
        }
        
        // call the client's event handler.
        host.handleEvent( ve, severity!=ValidationEvent.FATAL_ERROR );
    }
}
