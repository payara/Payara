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

import com.sun.enterprise.deployment.runtime.RuntimeDescriptor;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.glassfish.web.deployment.runtime.LocaleCharsetMap;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
/**
* node for web property tag
*
* @author Jerome Dochez
*/
public class LocaleCharsetMapNode extends RuntimeDescriptorNode<LocaleCharsetMap> {

    protected LocaleCharsetMap descriptor = null;

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public LocaleCharsetMap getDescriptor() {
        if (descriptor==null) {
            descriptor = new LocaleCharsetMap();
        }
        return descriptor;
    }

    /**
     * receives notification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    @Override
    public void setElementValue(XMLElement element, String value) {
	RuntimeDescriptor descriptor = getDescriptor();
	if (element.getQName().equals(RuntimeTagNames.LOCALE)) {
	    descriptor.setAttributeValue(LocaleCharsetMap.LOCALE, value);
	} else 
	if (element.getQName().equals(RuntimeTagNames.AGENT)) {
	    descriptor.setAttributeValue(LocaleCharsetMap.AGENT, value);
	}
	if (element.getQName().equals(RuntimeTagNames.CHARSET)) {
	    descriptor.setAttributeValue(LocaleCharsetMap.CHARSET, value);
	} 
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
    public Node writeDescriptor(Node parent, String nodeName, LocaleCharsetMap descriptor) {
	
	Element locale = (Element) super.writeDescriptor(parent, nodeName, descriptor);
	
	// description?
	appendTextChild(locale, RuntimeTagNames.DESCRIPTION, descriptor.getDescription());
	
	// locale, agent, charset attributes
	setAttribute(locale, RuntimeTagNames.LOCALE, (String) descriptor.getAttributeValue(LocaleCharsetMap.LOCALE));
	setAttribute(locale, RuntimeTagNames.AGENT, (String) descriptor.getAttributeValue(LocaleCharsetMap.AGENT));
	setAttribute(locale, RuntimeTagNames.CHARSET, (String) descriptor.getAttributeValue(LocaleCharsetMap.CHARSET));
	
	return locale;
    }
}
