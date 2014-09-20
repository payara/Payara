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

/*
 * MappingFieldElement.java
 *
 * Created on March 3, 2000, 1:11 PM
 */

package com.sun.jdo.api.persistence.model.mapping;

import java.util.ArrayList;

import org.netbeans.modules.dbschema.DBMemberElement;

import com.sun.jdo.api.persistence.model.ModelException;

/** 
 *
 * @author raccah
 * @version %I%
 */
public interface MappingFieldElement extends MappingMemberElement
{
	/** Constant representing the jdo default fetch group. 
	 * This is what used to be mandatory for SynerJ.
	 */
	public static final int GROUP_DEFAULT = 1;

	/** Constant representing no fetch group. */
	public static final int GROUP_NONE = 0;

	/** Constant representing an independent fetch group.  All independent 
	 * fetch groups must have a value less than or equal to this constant.
	 */
	public static final int GROUP_INDEPENDENT = -1;

	// TBD:unmap all components, if remove from class remove here too
	//public void clear ();

	/** Determines whether this field element is read only or not.  
	 * @return <code>true</code> if the field is read only, 
	 * <code>false</code> otherwise
	 */
	public boolean isReadOnly ();

	/** Set whether this field element is read only or not.
	 * @param flag - if <code>true</code>, the field element is marked as
	 * read only; otherwise, it is not
	 * @exception ModelException if impossible
	 */
	public void setReadOnly (boolean flag) throws ModelException;

	/** Determines whether this field element is in a concurrency check or not. 
	 * @return <code>true</code> if the field is in a concurrency check, 
	 * <code>false</code> otherwise
	 */
	public boolean isInConcurrencyCheck ();

	/** Set whether this field element is in a concurrency check or not.
	 * @param flag - if <code>true</code>, the field element is marked as
	 * being in a concurrency check; otherwise, it is not
	 * @exception ModelException if impossible
	 */
	public void setInConcurrencyCheck (boolean flag) throws ModelException;

	/** Determines whether this field element is a version field or not.  
	 * @return <code>true</code> if the field is a version field, 
	 * <code>false</code> otherwise
	 */
	public boolean isVersion ();

	/** Set whether this field element is a version field or not.
	 * @param flag - if <code>true</code>, the field element is marked 
	 * as a version field; otherwise, it is not
	 * @exception ModelException if impossible
	 */
	public void setVersion (boolean flag) throws ModelException;

	//====================== fetch group handling ==========================

	/** Get the fetch group of this field element.
	 * @return the fetch group, one of {@link #GROUP_DEFAULT},  
	 * {@link #GROUP_NONE}, or anything less than or equal to 
	 * {@link #GROUP_INDEPENDENT}
	 */
	public int getFetchGroup ();

	/** Set the fetch group of this field element.
	 * @param group - an integer indicating the fetch group, one of:
	 * {@link #GROUP_DEFAULT}, {@link #GROUP_NONE}, or anything less than or
	 * equal to {@link #GROUP_INDEPENDENT}
	 * @exception ModelException if impossible
	 */
	public void setFetchGroup (int group) throws ModelException;

	//======================= column handling ===========================

	/** Returns the list of column names to which this mapping field is 
	 * mapped.
	 * @return the names of the columns mapped by this mapping field
	 */
	public ArrayList getColumns ();

	/** Adds a column to the list of columns mapped by this mapping field.
	 * @param column column element to be added to the mapping
	 * @exception ModelException if impossible
	 */
	public void addColumn (DBMemberElement column) throws ModelException;

	/** Removes a column from the list of columns mapped by this mapping field.
	 * @param columnName the relative name of the column to be removed from 
	 * the mapping
	 * @exception ModelException if impossible
	 */
	public void removeColumn (String columnName) throws ModelException;
}
