/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment;

import org.glassfish.deployment.common.Descriptor;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This class describes a web service message handler.
 *
 * @author Jerome Dochez
 * @author Kenneth Saks
 */
public class WebServiceHandler extends Descriptor {
    
    private String handlerName = null;

    private String handlerClass = null;

    private Collection initParams = new HashSet();
    
    private Collection soapHeaders = new HashSet();
    
    private Collection soapRoles = new HashSet();

    private Collection portNames = new HashSet();
    
    /**
    * copy constructor.
    */
    public WebServiceHandler(WebServiceHandler other) {
	super(other);
	handlerName = other.handlerName; // String
	handlerClass = other.handlerClass; // String
	portNames.addAll(other.portNames); // Set of String
	soapRoles.addAll(other.soapRoles); // Set of String
	soapHeaders.addAll(other.soapHeaders); // Set of QName (immutable)
	for (Iterator i = other.initParams.iterator(); i.hasNext();) {
	    initParams.add(new NameValuePairDescriptor((NameValuePairDescriptor)i.next()));
	}
    }

    public WebServiceHandler() {
    }

    /**
     * Sets the class name for this handler
     * @param class name
     */
    public void setHandlerClass(String className) {
        handlerClass = className;

    }
    
    /**
     * @return the class name for this handler
     */
    public String getHandlerClass() {
        return handlerClass;
    }   
    
    public void setHandlerName(String name) {
        handlerName = name;

    }

    public String getHandlerName() {
        return handlerName;
    }
  
    /**
     * add an init param to this handler
     * @param the init param
     */
    public void addInitParam(NameValuePairDescriptor newInitParam) {
        initParams.add(newInitParam);

    }
  
    /**
     * remove an init param from this handler
     * @param the init param
     */
    public void removeInitParam(NameValuePairDescriptor initParamToRemove) {
        initParams.remove(initParamToRemove);

    }

    /**
     * @return the list of init params for this handler
     */
    public Collection getInitParams() {
        return initParams;
    }
    
    public void addSoapHeader(QName soapHeader) {
        soapHeaders.add(soapHeader);

    }

    public void removeSoapHeader(QName soapHeader) {
        soapHeaders.remove(soapHeader);

    }

    // Collection of soap header QNames
    public Collection getSoapHeaders() {
        return soapHeaders;
    }

    public void addSoapRole(String soapRole) {
        soapRoles.add(soapRole);

    }

    public void removeSoapRole(String soapRole) {
        soapRoles.remove(soapRole);

    }

    public Collection getSoapRoles() {
        return soapRoles;
    }

    public void addPortName(String portName) {
        portNames.add(portName);

    }

    public void removePortName(String portName) {
        portNames.remove(portName);

    }

    // Collection of port name Strings
    public Collection getPortNames() {
        return portNames;
    }
    
    /**
     * @return a string describing the values I hold
     */
    public void print(StringBuffer toStringBuffer) {
        toStringBuffer.append("\nHandler name = ").append(handlerName).append( 
            "Handler class name = ").append(handlerClass);
        for (Iterator i=getInitParams().iterator(); i.hasNext(); ) {
            toStringBuffer.append("\n").append(i.next().toString());
        }
    }

}
