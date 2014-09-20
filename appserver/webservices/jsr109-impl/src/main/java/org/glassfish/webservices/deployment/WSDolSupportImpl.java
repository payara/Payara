/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.deployment;

import org.jvnet.hk2.annotations.Service;

import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.WebServiceClient;
import javax.xml.namespace.QName;

import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WSDolSupport;

/**
 *Implementation of jaxws dependent services for the DOL
 *
 * @author Jerome Dochez
 */
@Service
public class WSDolSupportImpl implements WSDolSupport {

    public static final String SOAP11_TOKEN = "##SOAP11_HTTP";
    public static final String SOAP12_TOKEN = "##SOAP12_HTTP";
    public static final String SOAP11_MTOM_TOKEN = "##SOAP11_HTTP_MTOM";
    public static final String SOAP12_MTOM_TOKEN = "##SOAP12_HTTP_MTOM";
    public static final String XML_TOKEN = "##XML_HTTP";
    
    public String getProtocolBinding(String value) {
        if (value==null) {
            return SOAPBinding.SOAP11HTTP_BINDING ;
        } else if(SOAP11_TOKEN.equals(value)) {
            return SOAPBinding.SOAP11HTTP_BINDING;
        } else if(SOAP11_MTOM_TOKEN.equals(value)) {
            return SOAPBinding.SOAP11HTTP_MTOM_BINDING;
        } else if(SOAP12_TOKEN.equals(value)) {
            return SOAPBinding.SOAP12HTTP_BINDING;
        } else if(SOAP12_MTOM_TOKEN.equals(value)) {
            return SOAPBinding.SOAP12HTTP_MTOM_BINDING;
        } else if(XML_TOKEN.equals(value)) {
            return HTTPBinding.HTTP_BINDING;
        } else {
            return value;
        }
    }

    public String getSoapAddressPrefix(String protocolBinding) {
        if((SOAPBinding.SOAP12HTTP_BINDING.equals(protocolBinding)) ||
            (SOAPBinding.SOAP12HTTP_MTOM_BINDING.equals(protocolBinding)) ||
            (SOAP12_TOKEN.equals(protocolBinding)) ||
            (SOAP12_MTOM_TOKEN.equals(protocolBinding))) {
            return "soap12";
        }
        // anything else should be soap11
        return "soap";
    }

    public void setServiceRef(Class annotatedClass, ServiceReferenceDescriptor ref) {
        WebServiceClient wsc = (WebServiceClient)annotatedClass.getAnnotation(javax.xml.ws.WebServiceClient.class);
        if (wsc != null) {
            ref.setWsdlFileUri(wsc.wsdlLocation());
            //we set the service QName too from the @WebServiceClient annotation
            ref.setServiceName(new QName(wsc.targetNamespace(),wsc.name()) );
        }    
    }

    public Class getType(String className) throws ClassNotFoundException {
        return this.getClass().getClassLoader().loadClass(className);
    }
}
