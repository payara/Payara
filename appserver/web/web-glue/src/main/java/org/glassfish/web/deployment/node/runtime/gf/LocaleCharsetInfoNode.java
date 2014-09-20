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

package org.glassfish.web.deployment.node.runtime.gf;

import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.glassfish.web.deployment.runtime.LocaleCharsetInfo;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

/**
* node for locale-charset-info tag
*
* @author Jerome Dochez
*/
public class LocaleCharsetInfoNode extends RuntimeDescriptorNode<LocaleCharsetInfo> {
    
    /**
     * Initialize the child handlers
     */
    public LocaleCharsetInfoNode() {
	
        registerElementHandler(new XMLElement(RuntimeTagNames.LOCALE_CHARSET_MAP), 
                               LocaleCharsetMapNode.class, "addLocaleCharsetMap");		       
    }

    protected LocaleCharsetInfo descriptor = null;

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public LocaleCharsetInfo getDescriptor() {
        if (descriptor==null) {
            descriptor = new LocaleCharsetInfo();
        }
        return descriptor;
    }

    @Override
    public void startElement(XMLElement element, Attributes attributes) {
	if (element.getQName().equals(RuntimeTagNames.LOCALE_CHARSET_INFO)) {
            LocaleCharsetInfo info = (LocaleCharsetInfo) getDescriptor();
            for (int i=0; i<attributes.getLength();i++) {
                if (RuntimeTagNames.DEFAULT_LOCALE.equals( 
                    attributes.getQName(i))) {
                    info.setAttributeValue(LocaleCharsetInfo.DEFAULT_LOCALE, 
                        attributes.getValue(i));
                }
            }
        } else if (element.getQName().equals(
            RuntimeTagNames.PARAMETER_ENCODING)) {
	    LocaleCharsetInfo info = (LocaleCharsetInfo) getDescriptor();
            info.setParameterEncoding(true);
            for (int i=0; i<attributes.getLength();i++) {
                if (RuntimeTagNames.DEFAULT_CHARSET.equals(
                    attributes.getQName(i))) {
                    info.setAttributeValue(LocaleCharsetInfo.PARAMETER_ENCODING,
                        LocaleCharsetInfo.DEFAULT_CHARSET, 
                        attributes.getValue(i));
                }
                if (RuntimeTagNames.FORM_HINT_FIELD.equals(
                    attributes.getQName(i))) {
                    info.setAttributeValue(LocaleCharsetInfo.PARAMETER_ENCODING, 
                        LocaleCharsetInfo.FORM_HINT_FIELD,
                        attributes.getValue(i));
                }
            }
	} else super.startElement(element, attributes);
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param nodeName node name for the descriptor
     * @param descriptor the descriptor to write
     * @return the DOM tree top node
     */
    @Override
    public Node writeDescriptor(Node parent, String nodeName, LocaleCharsetInfo descriptor) {
	
	Element locale = (Element) super.writeDescriptor(parent, nodeName, descriptor);
	
	// locale-charset-map+
	if (descriptor.sizeLocaleCharsetMap()>0) {
	    LocaleCharsetMapNode lcmn = new LocaleCharsetMapNode();
	    for (int i=0;i<descriptor.sizeLocaleCharsetMap();i++) {
		lcmn.writeDescriptor(locale, RuntimeTagNames.LOCALE_CHARSET_MAP, descriptor.getLocaleCharsetMap(i));
	    }
	}
	
	// <!ELEMENT parameter-encoding EMPTY>
	//<!ATTLIST parameter-encoding form-hint-field CDATA #IMPLIED
	//		     default-charset CDATA #IMPLIED>
	if (descriptor.isParameterEncoding()) {
	    Element parameter = (Element) appendChild(locale, RuntimeTagNames.PARAMETER_ENCODING);
	    
	    if (descriptor.getAttributeValue(LocaleCharsetInfo.PARAMETER_ENCODING, LocaleCharsetInfo.FORM_HINT_FIELD)!=null) {
	        setAttribute(parameter, RuntimeTagNames.FORM_HINT_FIELD, 
	    	(String) descriptor.getAttributeValue(LocaleCharsetInfo.PARAMETER_ENCODING, LocaleCharsetInfo.FORM_HINT_FIELD));
	    }
	    
	    
	    if (descriptor.getAttributeValue(LocaleCharsetInfo.PARAMETER_ENCODING, LocaleCharsetInfo.DEFAULT_CHARSET)!=null) {
	        setAttribute(parameter, RuntimeTagNames.DEFAULT_CHARSET, 
	    	(String) descriptor.getAttributeValue(LocaleCharsetInfo.PARAMETER_ENCODING, LocaleCharsetInfo.DEFAULT_CHARSET));
	    }
	}	    
	
	// default_locale
        if (descriptor.getAttributeValue(LocaleCharsetInfo.DEFAULT_LOCALE) != null) {
	    setAttribute(locale, RuntimeTagNames.DEFAULT_LOCALE, 
	        (String) descriptor.getAttributeValue(LocaleCharsetInfo.DEFAULT_LOCALE));
        }	
	return locale;
    }

}
