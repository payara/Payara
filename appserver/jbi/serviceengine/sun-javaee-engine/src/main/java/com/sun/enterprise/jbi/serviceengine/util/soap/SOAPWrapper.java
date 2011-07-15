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

package com.sun.enterprise.jbi.serviceengine.util.soap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.soap.SOAPMessage;


/**
 * This object provides a wrapper for SOAP Messages and also contains status information.
 * The wrapper allows clients to attach properties to it.
 *
 * @author Sun Microsystems, Inc.
 */
public class SOAPWrapper
{
    /**
     * A place holder to hold additional name-value pairs.
     */
    private Map mMap;

    /**
     * Contains handle to the soap message.
     */
    private SOAPMessage mMessage;

    /**
     * Request Status.
     */
    private int mStatus;

    /**
     * Internal handle to the service URL.
     */
    private String mServiceURL;

    /**
     * Creates a new instance of SOAPWrapper.
     *
     * @param soapMessage - soap message
     */
    public SOAPWrapper(SOAPMessage soapMessage)
    {
        mMessage = soapMessage;
        mMap = new HashMap();
    }

    /**
     * Sets status.
     *
     * @param status request status
     */
    public void setStatus(int status)
    {
        mStatus = status;
    }

    /**
     * Gets status.
     *
     * @return status information
     */
    public int getStatus()
    {
        return mStatus;
    }

    /**
     * Gets Service URL.
     *
     * @return service URL
     */
    public String getServiceURL()
    {
        return mServiceURL;
    }

    /**
     * Sets Service URL.
     *
     * @param serviceURL service url.
     */
    public void setServiceURL(String serviceURL)
    {
        mServiceURL = serviceURL;
    }

    /**
     * Gets the soap message.
     *
     * @return soap message instance
     */
    public SOAPMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Sets a property to the SOAP Wrapper.
     *
     * @param propertyName property name
     * @param value property value
     */
    public void setValue(String propertyName, Object value)
    {
        mMap.put(propertyName, value);
    }

    /**
     * Sets a property to the SOAP Wrapper.
     *
     * @param propertyName property name
     *
     * @return property value
     */
    public Object getValue(String propertyName)
    {
        return mMap.get(propertyName);
    }

    /**
     * Get the property list.
     *
     * @return a list of property names.
     */
    public Iterator getProperties()
    {
        return mMap.keySet().iterator();
    }
}
