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
 * MappingTableElement.java
 *
 * Created on March 3, 2000, 1:11 PM
 */

package com.sun.jdo.api.persistence.model.mapping;

import java.util.ArrayList;

import org.netbeans.modules.dbschema.TableElement;
import org.netbeans.modules.dbschema.ColumnElement;

import com.sun.jdo.api.persistence.model.ModelException;

/** 
 * This is an element which represents a database table. It exists (separately
 * from TableElement in the database model) to allow the runtime to use a 
 * description of the underlying table that differs from the actual database.
 * For example, mapping table contains a key which can be thought of as a 
 * "fake primary key" and designates the columns which the runtime will use 
 * to identify rows.  It is analagous to the primary key of the underlying 
 * database table and is typically the same, however the important point 
 * is that it is not a requirement.  The table in the database may have a 
 * different primary key or may have no primary key at all.  Similarly, the 
 * mapping table contains a list of reference keys which can be thought of as 
 * "fake foreign key" objects and designate the column pairs used to join 
 * the primary table with a secondary table.  These are analagous to 
 * foreign keys and may in fact contain identical pairs as the foreign key, 
 * but again, this is not a requirement.  The foreign key may define a 
 * different set of pairs or may not exist at all.  Although any set of pairs 
 * is legal, the user should be careful to define pairs which represent a 
 * logical relationship between the two tables.
 * Any mapping table elements which are designated as primary tables have 
 * their key set up automatically.  Any mapping table elements which are 
 * designated as secondary tables should not have their keys set up directly; 
 * the setup is automatically part of the pair definition which makes up the 
 * reference key.
 *
 * @author Mark Munro
 * @author Rochelle Raccah
 * @version %I%
 */
public interface MappingTableElement extends MappingMemberElement
{
	//======================= table handling ===========================

	/** Returns the name of the table element used by this mapping table.
	 * @return the table name for this mapping table
	 */
	public String getTable ();

	/** Set the table element for this mapping table to the supplied table.
	 * @param table table element to be used by the mapping table.
	 * @exception ModelException if impossible
	 */
	public void setTable (TableElement table) throws ModelException;

	/** Returns true if the table element used by this mapping table is equal
	 * to the supplied table.
	 * @return <code>true</code> if table elements are equal, 
	 * <code>false</code> otherwise.
	 */
	public boolean isEqual (TableElement table);

	//===================== primary key handling ===========================

	/** Returns the list of column names in the primary key for this 
	 * mapping table.
	 * @return the names of the columns in the primary key for this 
	 * mapping table
	 */
	public ArrayList getKey ();

	/** Adds a column to the primary key of columns in this mapping table.
	 * This method should only be used to manipulate the key columns of the 
	 * primary table.  The secondary table key columns should be manipulated 
	 * using MappingReferenceKeyElement methods for pairs.
	 * @param column column element to be added
	 * @exception ModelException if impossible
	 */
	public void addKeyColumn (ColumnElement column) throws ModelException;

	/** Removes a column from the primary key of columns in this mapping table.
	 * This method should only be used to manipulate the key columns of the 
	 * primary table.  The secondary table key columns should be manipulated 
	 * using MappingReferenceKeyElement methods for pairs.
	 * @param columnName the relative name of the column to be removed
	 * @exception ModelException if impossible
	 */
	public void removeKeyColumn (String columnName) throws ModelException;

	//===================== reference key handling ===========================

	/** Returns the list of keys (MappingReferenceKeyElements) for this
	 * mapping table. There will be keys for foreign keys and "fake" foreign 
	 * keys.
	 * @return the reference key elements for this mapping table
	 */
	public ArrayList getReferencingKeys ();

	/** Adds a referencing key to the list of keys in this mapping table.
	 * @param referencingKey referencing key element to be added
	 * @exception ModelException if impossible
	 */
	public void addReferencingKey (MappingReferenceKeyElement referencingKey)
		throws ModelException;

	/** Removes the referencing key for the supplied table element from list 
	 * of keys in this mapping table.
	 * @param table mapping table element for which to remove referencing keys
	 * @exception ModelException if impossible
	 */
	public void removeReference (MappingTableElement table)
		throws ModelException;
}
