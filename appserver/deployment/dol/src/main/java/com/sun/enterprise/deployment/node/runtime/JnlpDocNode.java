/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.XMLNode;
import com.sun.enterprise.deployment.runtime.JavaWebStartAccessDescriptor;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;

/**
 * Represents the jnlp-doc node under java-web-start-access.
 * We need this node in order to support the <jnlp-doc href="path-to-custom-JNLP-doc"/>
 * notation.  The DTD file describes the href attribute although the doc has not
 * historically mentioned it.  Instead the doc has said to place the path to
 * the custom JNLP as the text value of the <jnlp-doc> element.  
 * 
 * @author tjquinn
 */
public class JnlpDocNode extends DeploymentDescriptorNode<JavaWebStartAccessDescriptor> {
    protected JavaWebStartAccessDescriptor descriptor;
    
    public JnlpDocNode() {
        
    }
    /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    @Override
    public JavaWebStartAccessDescriptor getDescriptor() {
	if (descriptor==null) {
	    XMLNode parentNode = getParentNode();
            if (parentNode != null) {
                Object parentDescriptor = parentNode.getDescriptor();
                if (parentDescriptor != null && (parentDescriptor instanceof JavaWebStartAccessDescriptor) ) {
                    descriptor = (JavaWebStartAccessDescriptor) parentDescriptor;
                }
            }
	} 
	return descriptor;
    }
    
    @Override
    protected boolean setAttributeValue(XMLElement elementName, XMLElement attributeName, String value) {
        if (attributeName.getQName().equals("href")) {
            getDescriptor().setJnlpDocument(value);
            return true;
        } else {
            return super.setAttributeValue(elementName, attributeName, value);
        }
    }

    @Override
    public void setElementValue(XMLElement element, String value) {
        if (element.getQName().equals(RuntimeTagNames.JNLP_DOC)) {
            getDescriptor().setJnlpDocument(value);
        }
    }
    
    
}
