/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

/*
 * InvocationTrace.java
 *
 * Created on November 22, 2004, 4:35 PM
 */

package org.glassfish.webservices.monitoring;

import org.glassfish.webservices.SOAPMessageContext;

import java.util.logging.Level;
import java.io.ByteArrayOutputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.glassfish.webservices.LogUtils;

/**
 * An invocation trace contains the timestamp os a particular
 * message invocation, the stringified SOAP request and 
 * response or the SOAP Faults if the invocation resulted in one.
 * <p><b>NOT THREAD SAFE: mutable instance variables</b>
 *
 * @author Jerome Dochez
 */
public class MessageTraceImpl implements MessageTrace {
        
    private Endpoint source;
    private String soapMessage=null;
    private TransportInfo transportInfo=null;
    
    /** Creates a new instance of InvocationTrace */
    public MessageTraceImpl() {
        
    }
    
    /** 
     * Return the SOAPMessage as a string including the SOAPHeaders or not
     * @param includeHeaders the soap headers.
     * @return the soap message
     */ 
    public String getMessage(boolean includeHeaders) {

        if (soapMessage!=null) {
            if (includeHeaders) {
                return soapMessage;
            }
        
            Pattern p = Pattern.compile("<env:Body>.*</env:Body>");
            Matcher m = p.matcher(soapMessage);
            if (m.find()) {
                return soapMessage.substring(m.start(),m.end());
            } else {
                return soapMessage;
            }
        }
        return null;
    }
    
    /**
     * Return the endpoint where this message originated from
     */
    public Endpoint getEndpoint() {
        return source;
    }
    
    public void setMessageContext(com.sun.xml.rpc.spi.runtime.SOAPMessageContext soapMessageCtx) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            soapMessageCtx.getMessage().writeTo(baos);
        } catch(Exception e) {
            WebServiceEngineImpl.sLogger.log(Level.WARNING, LogUtils.CANNOT_LOG_SOAPMSG, e.getMessage());
        }

        soapMessage = baos.toString();
    }    
    
    public void setMessageContext(SOAPMessageContext soapMessageCtx) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();        
        try {
            soapMessageCtx.getMessage().writeTo(baos);       
        } catch(Exception e) {
            WebServiceEngineImpl.sLogger.log(Level.WARNING, LogUtils.CANNOT_LOG_SOAPMSG, e.getMessage());
        }    

        soapMessage = baos.toString();          
    }
    
    
    public void setEndpoint(Endpoint source) {
        this.source = source;
    }
    
    public TransportInfo getTransportInfo() {
        return transportInfo;
    }
    
    public void setTransportInfo(TransportInfo info) {
        transportInfo = info;
    }
}
