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
package org.glassfish.security.services.impl.common;


import java.util.Set;
import java.util.TreeMap;

import org.glassfish.security.services.api.common.Attribute;
import org.glassfish.security.services.api.common.Attributes;


public class AttributesImpl implements Attributes {

	private final TreeMap<String, Attribute> attributes;

    public AttributesImpl() {
        attributes = new TreeMap<String, Attribute>();
    }

    /**
     * Copy constructor
     */
    public AttributesImpl( AttributesImpl other ) {
        if ( null == other ) {
            throw new NullPointerException( "Given illegal null AttributesImpl." );
        }
        attributes = new TreeMap<String, Attribute>( other.attributes );
    }

    public int getAttributeCount() {
		return attributes.size();
	}

	public Set<String> getAttributeNames() {
		return attributes.keySet();
	}

	public Attribute getAttribute(String name) {
		return attributes.get(name);
	}

	public String getAttributeValue(String name) {
		Attribute a = attributes.get(name);
		if(a != null) {
			return a.getValue();
		}
		return null;
	}

	public Set<String> getAttributeValues(String name) {
		Attribute a = attributes.get(name);
		if(a != null) {
			return a.getValues();
		}
		return null;
	}

	public String[] getAttributeValuesAsArray(String name) {
		Attribute a = attributes.get(name);
		if(a != null) {
			return a.getValuesAsArray();
		}
		return null;
	}

	public void addAttribute(String name, String value, boolean replace) {
		Attribute a = attributes.get(name);
		if(a != null && !replace) {
			a.addValue(value);
		}
		else {
			attributes.put(name, new AttributeImpl(name, value));
		}
	}

	public void addAttribute(String name, Set<String> values, boolean replace) {
		Attribute a = attributes.get(name);
		if(a != null && !replace) {
			a.addValues(values);
		}
		else {
			attributes.put(name, new AttributeImpl(name, values));
		}
	}

	public void addAttribute(String name, String[] values, boolean replace) {
		Attribute a = attributes.get(name);
		if(a != null && !replace) {
			a.addValues(values);
		}
		else {
			attributes.put(name, new AttributeImpl(name, values));
		}
	}

	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	public void removeAttributeValue(String name, String value) {
		Attribute a = attributes.get(name);
		if (a != null) {
			a.removeValue(value);
		}
	}

	public void removeAttributeValues(String name, Set<String> values) {
		Attribute a = attributes.get(name);
		if (a != null) {
			a.removeValues(values);
		}
	}

	public void removeAttributeValues(String name, String[] values) {
		Attribute a = attributes.get(name);
		if (a != null) {
			a.removeValues(values);
		}
	}

	public void removeAllAttributeValues(String name) {
		Attribute a = attributes.get(name);
		if (a != null) {
			a.clear();
		}
	}

	public void clear() {
		attributes.clear();

	}

}
