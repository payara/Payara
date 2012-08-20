/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
 * LocalizedNode.java
 *
 * Created on August 16, 2002, 4:01 PM
 */

package com.sun.enterprise.deployment.node;

import com.sun.enterprise.deployment.xml.TagNames;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for handling the xml lang attribute of 
 * an xml element
 *
 * @author Jerome Dochez
 */
public class LocalizedNode extends DeploymentDescriptorNode {
    
    protected String lang = null;
    protected String localizedValue = null;
    
    /**
     * @return the descriptor for this node
     */
    public Object getDescriptor() {
        return getParentNode().getDescriptor();
    }
    
    /**
     * notification of element start with attributes.
     */
    public void startElement(XMLElement element, Attributes attributes) {
        if (attributes.getLength()>0) {
            for (int i=0;i<attributes.getLength();i++) {
                if (attributes.getLocalName(i).equals(TagNames.LANG)) {
                    lang = attributes.getValue(i);
                }
            } 
        }
    }      
    
    /**
     * receives notification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
        if (element.equals(getXMLRootTag())) {
            localizedValue=value;
        } else 
            super.setElementValue(element, value);        
    }
    
    /**
     * writes all the localized map element usign the tagname with 
     * the lang attribute to a DOM node
     */
    public void writeLocalizedMap(Node parentNode, String tagName, Map localizedMap) {
        if (localizedMap!=null) {
            Set<Map.Entry> entrySet = localizedMap.entrySet();
            Iterator<Map.Entry> entryIt = entrySet.iterator();
            while (entryIt.hasNext()) {
                Map.Entry entry = entryIt.next();
                String lang = (String)entry.getKey();
                Element aLocalizedNode = (Element) appendTextChild(parentNode, tagName, (String) entry.getValue());
                if ((aLocalizedNode!=null) && (Locale.getDefault().getLanguage().equals(lang))) { 
		    aLocalizedNode.setAttributeNS(TagNames.XML_NAMESPACE, TagNames.XML_NAMESPACE_PREFIX + TagNames.LANG, lang);
 		 } 
            }
        }        
    }
    
}
