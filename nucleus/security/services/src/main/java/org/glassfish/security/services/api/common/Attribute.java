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
package org.glassfish.security.services.api.common;


import java.util.Set;

/**
 * The Attribute interface defines an interface for interacting with individual Attributes.  It is a read-only interface.
 * <br>
 * Note that, because the collection used to hold attribute values is a Set, the attribute can be multi-valued.  Each value
 * must be distinct, however -- it is not a bag that can contain multiple instances of the same value.
 */
public interface Attribute {
	
	/**
	 * Get the name of this attribute.
	 * 
	 * @return The name.
	 */
	public String getName();
	
	/**
	 * Get a count of the number of values this attribute has (0-n).
	 * 
	 * @return The value count.
	 */
	public int getValueCount();
	
	/**
	 * Get the first value from the Set of attribute values, or null if the attribute has no values.
	 * This is a shorthand method that should be useful for single-valued attributes, but note that
	 * there are no guarantees about which value is returned in the case that there are multiple values.
	 * The value returned will be whichever value the underlying Set implementation returns first.
	 * 
	 * @return The attribute value.
	 */
	public String getValue();
	
	/**
	 * Get the Set of values for this attribute.  The Set returned is a copy of the original; changes
	 * to this Set will not affect the original.
	 * 
	 * @return The attribute values Set.
	 */
	public Set<String> getValues();
	
	/**
	 * Return the attributes values as a String array.  Note that this array can be zero-length
	 * in the case that there are no values.
	 * 
	 * @return The attribute values array.
	 */
	public String[] getValuesAsArray();
	
	public void addValue(String value);
	public void addValues(Set<String> values);
	public void addValues(String[] values);
	
	public void removeValue(String value);
	public void removeValues(Set<String> values);
	public void removeValues(String[] values);
	
	public void clear();

}
