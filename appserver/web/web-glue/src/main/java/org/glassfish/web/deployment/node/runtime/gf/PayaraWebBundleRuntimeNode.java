/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package org.glassfish.web.deployment.node.runtime.gf;

import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.DTDRegistry;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import java.util.List;
import java.util.Map;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;

/**
 * This class is responsible for handling payara-web.xml in the web bundle
 * @author jonathan coustick
 */
public class PayaraWebBundleRuntimeNode extends GFWebBundleRuntimeNode {
    
     public PayaraWebBundleRuntimeNode(WebBundleDescriptorImpl descriptor) {
        super(descriptor);
    }
    
    public PayaraWebBundleRuntimeNode() {
        super(null);    
    }
    
    @Override
    protected XMLElement getXMLRootTag() {
        return new XMLElement(RuntimeTagNames.PAYARA_WEB_RUNTIME_TAG);
    }    
    
    @Override
    public String getDocType() {
        return DTDRegistry.PAYARA_WEBAPP_4_DTD_PUBLIC_ID;
    }
    
    @Override
    public String getSystemID() {
        return DTDRegistry.PAYARA_WEBAPP_4_DTD_SYSTEM_ID;
    }

   /**
    * register this node as a root node capable of loading entire DD files
    * 
    * @param publicIDToDTD is a mapping between xml Public-ID to DTD 
    * @param versionUpgrades The list of upgrades from older versions to the latest schema
    * @return the doctype tag name
    */
    public static String registerBundle(Map<String, String> publicIDToDTD,
                                        Map<String, List<Class>> versionUpgrades) {
       publicIDToDTD.put(DTDRegistry.PAYARA_WEBAPP_4_DTD_PUBLIC_ID, DTDRegistry.PAYARA_WEBAPP_4_DTD_SYSTEM_ID);
       
       return RuntimeTagNames.PAYARA_WEB_RUNTIME_TAG;       
   }    
    
}