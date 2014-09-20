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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a single handler-chains in a webservice in webservices.xml
 */
public class WebServiceHandlerChain extends Descriptor {

    // List of handlers associated with this endpoint. 
    // Handler order is important and must be preserved.
    private LinkedList<WebServiceHandler> handlers;

    private String protocolBinding = null;
    private String serviceNamePattern = null;
    private String portNamePattern = null;
    
    // copy constructor
    public WebServiceHandlerChain(WebServiceHandlerChain other) {
	super(other);
        this.protocolBinding = other.protocolBinding;
        this.serviceNamePattern = other.serviceNamePattern;
        this.portNamePattern = other.portNamePattern;
	if (other.handlers != null) {
            handlers = new LinkedList();
	    for (Iterator i = other.handlers.iterator(); i.hasNext();) {
		WebServiceHandler wsh = (WebServiceHandler)i.next();
		handlers.addLast(new WebServiceHandler(wsh));
	    }
	} else {
	    handlers = null;
	}
    }

    public WebServiceHandlerChain() {
        handlers = new LinkedList();
    }

    public void setProtocolBindings(String bindingId) {
        protocolBinding = bindingId;

    }
    
    public String getProtocolBindings() {
        return protocolBinding;
    }
    
    public void setServiceNamePattern(String pattern) {
        serviceNamePattern = pattern;

    }
    
    public String getServiceNamePattern() {
        return serviceNamePattern;
    }
    
    public void setPortNamePattern(String pattern) {
        portNamePattern = pattern;

    }
    
    public String getPortNamePattern() {
        return portNamePattern;
    }
    
    /**
     *@return true if this endpoint has at least one handler in its
     * handler chain.
     */
    public boolean hasHandlers() {
        return ( handlers.size() > 0 );
    }

    /**
     * Append handler to end of handler chain for this endpoint.
     */
    public void addHandler(WebServiceHandler handler) {
        handlers.addLast(handler);

    }

    public void removeHandler(WebServiceHandler handler) {
        handlers.remove(handler);

    }

    public void removeHandlerByName(String handlerName) {
        for(Iterator iter = handlers.iterator(); iter.hasNext();) {
            WebServiceHandler next = (WebServiceHandler) iter.next();
            if( next.getHandlerName().equals(handlerName) ) {
                iter.remove();

                break;
            }
        }
    }

    /**
     * Get ordered list of WebServiceHandler handlers for this endpoint.
     */
    public List<WebServiceHandler> getHandlers() {
        return handlers;
    }
}
