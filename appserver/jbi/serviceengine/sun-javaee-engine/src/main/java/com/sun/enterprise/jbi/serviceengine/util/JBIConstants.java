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

/*
 * JBIConstants.java
 *
 * Created on November 20, 2006, 8:27 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.enterprise.jbi.serviceengine.util;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.TransformerFactory;

/**
 *
 * @author bhavani
 */
public interface JBIConstants {
    
    // Constants copied from WrapperUtil.java
    public final String WRAPPER_DEFAULT_NAMESPACE_PREFIX = "jbi";
    public final String WRAPPER_DEFAULT_NAMESPACE = "http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper";
    public final String WRAPPER_MESSAGE_LOCALNAME = "message";
    public final String WRAPPER_MESSAGE_QNAME = 
            WRAPPER_DEFAULT_NAMESPACE_PREFIX + ":" + WRAPPER_MESSAGE_LOCALNAME;
    
    public final String WRAPPER_ATTRIBUTE_VERSION = "version";
    public final String WRAPPER_ATTRIBUTE_VERSION_VALUE = "1.0";
    public final String WRAPPER_ATTRIBUTE_TYPE = "type";
    public final String WRAPPER_ATTRIBUTE_NAME = "name";
    public final String WRAPPER_PART_LOCALNAME = "part";
    public final String WRAPPER_PART_QNAME = 
            WRAPPER_DEFAULT_NAMESPACE_PREFIX + ":" + WRAPPER_PART_LOCALNAME;
    
    public final String USED_WITH = "com.sun.enterprise.jbi.se.usedwith";
    public final String USED_WITH_HTTP_SOAP_BC = "httpsoapbc";
    public final String USED_WITH_JMAC_PROVIDER = "jmacprovider";
    public final String USED_WITH_NON_SOAP_WSDL = "nonsoapwsdl";
    public final String CLIENT_CACHE = "com.sun.enterprise.jbi.se.clientcache";
    public final String AUTO_ENDPOINT_ENABLING = "com.sun.enterprise.jbi.se.autoendpointenabling";
    
    public static final XMLInputFactory XIF = Util.getXMLInputFactory();
    public static final XMLOutputFactory XOF = Util.getXMLOutputFactory();
    public static final TransformerFactory TF = TransformerFactory.newInstance();

}
