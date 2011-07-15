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
 * MappingRelationshipElement.java
 *
 * Created on March 3, 2000, 1:11 PM
 */

package com.sun.jdo.api.persistence.model.mapping;

import java.util.ArrayList;

import org.netbeans.modules.dbschema.ColumnPairElement;

import com.sun.jdo.api.persistence.model.ModelException;

/** 
 * This is a specialized field element which represents a relationship 
 * between two classes.  The mapping portion should be set up as follows:
 * When mapping a non-join table relationship, call the {@link #addColumn}
 * method once with each pair of columns between the local table and the 
 * foreign table.  When mapping a join table relationship, call the 
 * {@link #addLocalColumn} once for each pair of columns between the 
 * local table and the join table and {@link #addAssociatedColumn} once for 
 * each pair of columns between the join table and the foreign table.  
 * Note that the number of pairs (local and associated) may differ and that
 * the order of adding them (local first or associated first) is not 
 * important.
 *
 * @author Mark Munro
 * @author Rochelle Raccah
 * @version %I%
 */
public interface MappingRelationshipElement extends MappingFieldElement 
{
	//=================== column handling for join tables ====================

	/** Returns the list of associated column names to which this 
	 * mapping field is mapped.  This is used for join tables.
	 * @return the names of the columns mapped by this mapping field
	 * @see MappingFieldElement#getColumns
	 */
	public ArrayList getAssociatedColumns ();

	/** Adds a column to the list of columns mapped by this mapping field.  
	 * Call this method instead of <code>addColumn</code> when mapping join 
	 * tables.  This method is used to map between the local column and the 
	 * join table, while <code>addAssociatedColumn</code> is used to 
	 * map between the join table and the foreign table.
	 * @param column foreign column element to be added to the mapping
	 * @exception ModelException if impossible
	 * @see MappingFieldElement#addColumn
	 * @see #addAssociatedColumn
	 */
	public void addLocalColumn (ColumnPairElement column) throws ModelException;

	/** Adds a column to the list of associated columns mapped by this mapping 
	 * field.  Call this method instead of <code>addColumn</code> when mapping 
	 * join tables.  This method is used to map between the join table column  
	 * and the foreign table column, while <code>addLocalColumn</code> is used 
	 * to map between the local table and the join table.
	 * @param column foreign column element to be added to the mapping
	 * @exception ModelException if impossible
	 * @see MappingFieldElement#addColumn
	 * @see #addLocalColumn
	 */
	public void addAssociatedColumn (ColumnPairElement column)
		throws ModelException;
}
