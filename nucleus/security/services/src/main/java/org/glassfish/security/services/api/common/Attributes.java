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
 * An interface that represents a collectoin of Attributes.  Unlike the Attribute interface, this interface
 * is not read-only.  Methods are provided to update the underlying collection by adding, removing, or updating
 * the attributes it holds.
 * 
 * There is no factory method on this interface; instead, services that take Attributes parameters have
 * methods for generating instances of Attributes appropriate to the purpose at hand.
 */
public interface Attributes {

	/**
	 * Get a count of the number of Attributes (not attribute values) in this Attributes object.
	 * 
	 * @return The attribute count.
	 */
	public int getAttributeCount();
	
	/**
	 * Get the attribute names, as a Set.  This set is read-only; updating it will not change the
	 * underlying attributes collection.
	 * 
	 * @return The attribute names Set.
	 */
	public Set<String> getAttributeNames();
	
	/**
	 * Get the Attribute object for the named attribute, if present.
	 * 
	 * @param name The name of the Attribute to get.
	 * @return The Attribute, or null if an attribute by that name is not present.
	 */
	public Attribute getAttribute(String name);
	
	/**
	 * Get the value of the specified attribute, if present.  Note that, for multi-valued attributes,
	 * The value returned is whichever value is returned first by the underlying Set implementation;
	 * there are no ordering guarantees.  As such, this method is primarily useful as a shorthand
	 * method for attributes known (or expected) by the caller to be single-valued.
	 * 
	 * Note also that it is not possible, using this method, to distinguish between a missing attribute
	 * and an attribute that is present but has no values.  In both cases, null is returned.
	 * 
	 * @param name The name of the attribute whose value is wanted.
	 * @return The attribute value, or null if the attribute is missing or has no values.
	 */
	public String getAttributeValue(String name);
	
	/**
	 * Get the Set of values for the specified attribute, if present.
	 * 
	 * @param name The name of the attribute whose values are wanted.
	 * @return The Set of values, or null if the attribute is not present in the collection.  If the
	 * attribute is present but has no values, an empty Set is returned.
	 */
	public Set<String> getAttributeValues(String name);
	
	/**
	 * Get the set of values for the specified attribute as a String array.
	 * 
	 * @param name The name of the attribute whose values are wanted.
	 * @return The array of values, or null if the attribute is not present in the collection.  If the
	 * attribute is present but has no values, a zero-length array is returned.
	 */
	public String[] getAttributeValuesAsArray(String name);
	
	/**
	 * Add the specified attribute to the attributes collection.  Remove any existing values
	 * if the replace parameter is true, otherwise add the new value to the existing values.
	 * Duplicate values are silently dropped.
	 * 
	 * @param name The name of the attribute to add.
	 * @param value The single attribute value to add.
	 * @param replace If true, replace the existing attribute and any existing values.  If false,
	 * add the new value to those that are already present.
	 */
	public void addAttribute(String name, String value, boolean replace);
	
	/**
	 * Add the specified attribute to the attributes collection.  Remove any existing values
	 * if the replace parameter is true, otherwise add the new values to the existing values.
	 * Duplicate values are silently dropped.
	 * 
	 * @param name The name of the attribute to add.
	 * @param value The Set of values to add.
	 * @param replace If true, replace the existing attribute and any existing values.  If false,
	 * add the new values to those that are already present.
	 */
	public void addAttribute(String name, Set<String> values, boolean replace);
	
	/**
	 * Add the specified attribute to the attributes collection.  Remove any existing values
	 * if the replace parameter is true, otherwise add the new values to the existing values.
	 * Duplicate values are silently dropped.
	 * 
	 * @param name The name of the attribute to add.
	 * @param value The array of values to add.
	 * @param replace If true, replace the existing attribute and any existing values.  If false,
	 * add the new values to those that are already present.
	 */
	public void addAttribute(String name, String[] values, boolean replace);
	
	/**
	 * Remove the specified attribute from the collection.
	 * 
	 * @param name The name of the attribute to remove.
	 */
	public void removeAttribute(String name);
	
	/**
	 * Remove the specified value from the named attribute.
	 * 
	 * @param name The name of the attribute from which to remove a value.
	 * @param value The value to remove.  A value will be removed only if it exactly matches this parameter.
	 */
	public void removeAttributeValue(String name, String value);
	
	/**
	 * Remove the specified values from the named attribute.
	 * 
	 * @param name The name of the attribute from which to remove the values.
	 * @param value The Set of values to remove.  Only values that exactly match values in this Set will be removed.
	 */
	public void removeAttributeValues(String name, Set<String> values);
	
	/**
	 * Remove the specified values from the named attribute.
	 * 
	 * @param name The name of the attribute from which to remove the values.
	 * @param value The array of values to remove.  Only values that exactly match values in this array will be removed.
	 */
	public void removeAttributeValues(String name, String[] values);
	
	/**
	 * Remove all values associated with the named attribute, but do not remove
	 * the attribute from the collection.
	 * 
	 * @param name The name of the attribute whose values should be removed.
	 */
	public void removeAllAttributeValues(String name);
	
	/**
	 * Removes all attributes from the collection.
	 */
	public void clear();

}
