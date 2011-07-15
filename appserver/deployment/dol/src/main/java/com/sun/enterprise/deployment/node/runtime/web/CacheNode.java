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

package com.sun.enterprise.deployment.node.runtime.web;

import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.runtime.RuntimeDescriptor;
import com.sun.enterprise.deployment.runtime.web.Cache;
import com.sun.enterprise.deployment.runtime.web.CacheHelper;
import com.sun.enterprise.deployment.runtime.web.CacheMapping;
import com.sun.enterprise.deployment.runtime.web.DefaultHelper;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
* node for cache tag
*
* @author Jerome Dochez
*/
public class CacheNode extends WebRuntimeNode {
    
    public CacheNode() {
	
	registerElementHandler(new XMLElement(RuntimeTagNames.CACHE_HELPER), 
				CacheHelperNode.class, "addNewCacheHelper"); 	
	registerElementHandler(new XMLElement(RuntimeTagNames.DEFAULT_HELPER), 
				WebPropertyNode.class, "setDefaultHelper");
	registerElementHandler(new XMLElement(RuntimeTagNames.PROPERTY), 
				WebPropertyNode.class, "addWebProperty"); 	
	registerElementHandler(new XMLElement(RuntimeTagNames.CACHE_MAPPING), 
				CacheMappingNode.class, "addNewCacheMapping"); 							
    }
    /**
     * parsed an attribute of an element
     *
     * @param the element name
     * @param the attribute name
     * @param the attribute value
     * @return true if the attribute was processed
     */
    protected boolean setAttributeValue(XMLElement elementName, XMLElement attributeName, String value) {
	RuntimeDescriptor descriptor = (RuntimeDescriptor) getRuntimeDescriptor();
	if (descriptor==null) {
	    throw new RuntimeException("Trying to set values on a null descriptor");
	} 	
	if (attributeName.getQName().equals(RuntimeTagNames.MAX_ENTRIES)) {
	    descriptor.setAttributeValue(Cache.MAX_ENTRIES, value);
	    return true;    
	} else
	if (attributeName.getQName().equals(RuntimeTagNames.TIMEOUT_IN_SECONDS)) {
	    descriptor.setAttributeValue(Cache.TIMEOUT_IN_SECONDS, value);
	    return true;    
	} else
	if (attributeName.getQName().equals(RuntimeTagNames.ENABLED)) {
	    descriptor.setAttributeValue(Cache.ENABLED, value);
	    return true;    
	} else
	return false;
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name 
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, String nodeName, Cache descriptor) {       

	Element cache = (Element) super.writeDescriptor(parent, nodeName, descriptor);
	
	// cache-helpers*
	CacheHelper[] cacheHelpers = descriptor.getCacheHelper();
	if (cacheHelpers!=null && cacheHelpers.length>0) {
	    CacheHelperNode chn = new CacheHelperNode();
	    for (int i=0;i<cacheHelpers.length;i++) {
		chn.writeDescriptor(cache, RuntimeTagNames.CACHE_HELPER, cacheHelpers	[i]);
	    }
	}
	
	WebPropertyNode wpn = new WebPropertyNode();
	
	// default-helper?
	DefaultHelper dh = descriptor.getDefaultHelper();
	if (dh!=null && dh.getWebProperty()!=null) {
	    Node dhn = appendChild(cache, RuntimeTagNames.DEFAULT_HELPER);
	    wpn.writeDescriptor(dhn, RuntimeTagNames.PROPERTY, dh.getWebProperty());
	}
	
	// property*
	wpn.writeDescriptor(cache, RuntimeTagNames.PROPERTY, descriptor.getWebProperty());
	
	// cache-mapping
	CacheMapping[] mappings = descriptor.getCacheMapping();
	if (mappings!=null && mappings.length>0) {
	    CacheMappingNode cmn = new CacheMappingNode();
	    for (int i=0;i<mappings.length;i++) {
		cmn.writeDescriptor(cache, RuntimeTagNames.CACHE_MAPPING, mappings[i]);
	    }
	}
	
	// max-entries, timeout-in-seconds, enabled
	setAttribute(cache, RuntimeTagNames.MAX_ENTRIES, (String) descriptor.getAttributeValue(Cache.MAX_ENTRIES));
	setAttribute(cache, RuntimeTagNames.TIMEOUT_IN_SECONDS, (String) descriptor.getAttributeValue(Cache.TIMEOUT_IN_SECONDS));
	setAttribute(cache, RuntimeTagNames.ENABLED, (String) descriptor.getAttributeValue(Cache.ENABLED));
	
	return cache;
    }
}
