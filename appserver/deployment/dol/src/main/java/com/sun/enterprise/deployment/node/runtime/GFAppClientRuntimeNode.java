/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.node.runtime;

import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.deployment.xml.DTDRegistry;

import java.util.Map;


/**
 * This node is responsible for handling all runtime information for 
 * application client.
 */
public class GFAppClientRuntimeNode extends AppClientRuntimeNode {

    public GFAppClientRuntimeNode(ApplicationClientDescriptor descriptor) {
        super(descriptor);
    }

    public GFAppClientRuntimeNode() {
    }
    
    /**
     * @return the XML tag associated with this XMLNode
     */
    protected XMLElement getXMLRootTag() {
        return new XMLElement(RuntimeTagNames.GF_APPCLIENT_RUNTIME_TAG);
    }    
    
    /** 
     * @return the DOCTYPE that should be written to the XML file
     */
    public String getDocType() {
        return DTDRegistry.GF_APPCLIENT_602_DTD_PUBLIC_ID;
    }
    
    /**
     * @return the SystemID of the XML file
     */
    public String getSystemID() {
        return DTDRegistry.GF_APPCLIENT_602_DTD_SYSTEM_ID;
    }

   /**
    * register this node as a root node capable of loading entire DD files
    * 
    * @param publicIDToDTD is a mapping between xml Public-ID to DTD 
    * @return the doctype tag name
    */
   public static String registerBundle(Map publicIDToDTD) {    
       publicIDToDTD.put(DTDRegistry.GF_APPCLIENT_602_DTD_PUBLIC_ID, DTDRegistry.GF_APPCLIENT_602_DTD_SYSTEM_ID);
       return RuntimeTagNames.GF_APPCLIENT_RUNTIME_TAG;       
   }    
}
