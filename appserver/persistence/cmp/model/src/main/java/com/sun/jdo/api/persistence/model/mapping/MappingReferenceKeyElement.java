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
 * MappingReferenceKeyElement.java
 *
 * Created on March 3, 2000, 1:11 PM
 */

package com.sun.jdo.api.persistence.model.mapping;

import java.util.ArrayList;

import org.netbeans.modules.dbschema.*;

import com.sun.jdo.api.persistence.model.ModelException;

/** 
 * This is an element which represents a relationship between two tables
 * (primary and secondary).  It should not be used for relationship fields 
 * (MappingRelationshipElement has its own set of pairs).  It can be thought 
 * of as a "fake foreign key" meaning it designates the column pairs used to 
 * join the primary table with a secondary table.  It is analagous to a 
 * foreign key and may in fact contain identical pairs as the foreign key, 
 * but this is not a requirement.  The foreign key may define a different 
 * set of pairs or may not exist at all.  Although any set of pairs is legal, 
 * the user should be careful to define pairs which represent a logical 
 * relationship between the two tables.  The relationship should be set up as 
 * follows: 
 * First, set a primary table for the mapping class.  Doing this sets up a 
 * "fake primary key" for the associated mapping table element.  Next, add 
 * a secondary table and set up the pairs which establish the connection 
 * on the returned reference key object.  This sets up whatever "fake primary 
 * key" information is necessary on the secondary table's mapping table, 
 * establishes the primary to secondary relationship via the reference keys, 
 * and puts the pair information into the "fake foreign key".
 *
 * @author Mark Munro
 * @author Rochelle Raccah
 * @version %I%
 */
public interface MappingReferenceKeyElement 
	extends MappingMemberElement, ReferenceKey
{
	//======================= table handling ===========================

	/** Returns the mapping table element for this referencing key.
	 * @return the meta data table for this referencing key
	 */
	public MappingTableElement getTable ();

	/** Set the mapping table for this referencing key to the supplied table.
	 * @param table mapping table element to be used with this key.
	 * @exception ModelException if impossible
	 */
	public void setTable (MappingTableElement table) throws ModelException;

	//======================= column handling ===========================

	/** Returns the list of relative column pair names in this referencing key. 
	 * @return the names of the column pairs in this referencing key
	 */
	public ArrayList getColumnPairNames ();

	/** Remove a column pair from the holder.  This method can be used to 
	 * remove a pair by name when it cannot be resolved to an actual pair.
	 * @param pairName the relative name of the column pair to remove
	 * @throws ModelException if impossible
	 */
	public void removeColumnPair (String pairName) throws ModelException;

	/** Remove some column pairs from the holder.  This method can be used to 
	 * remove pairs by name when they cannot be resolved to actual pairs.
	 * @param pairNames the relative names of the column pairs to remove
	 * @throws ModelException if impossible
	 */
	public void removeColumnPairs (ArrayList pairNames) throws ModelException;

	//==== redefined from ReferenceKey to narrow Exception->ModelException ===

	/** Add a new column pair to the holder.
	 * @param pair the pair to add
	 * @throws ModelException if impossible
	 */
	public void addColumnPair (ColumnPairElement pair) throws ModelException;

	/** Add some new column pairs to the holder.
	 * @param pairs the column pairs to add
	 * @throws ModelException if impossible
	 */
	public void addColumnPairs (ColumnPairElement[] pairs)
		throws ModelException;

	/** Remove a column pair from the holder.
	 * @param pair the column pair to remove
	 * @throws ModelException if impossible
	 */
	public void removeColumnPair (ColumnPairElement pair)
		throws ModelException;

	/** Remove some column pairs from the holder.
	 * @param pairs the column pairs to remove
	 * @throws ModelException if impossible
	 */
	public void removeColumnPairs (ColumnPairElement[] pairs)
		throws ModelException;

	/** Set the column pairs for this holder.
	 * Previous column pairs are removed.
	 * @param pairs the new column pairs
	 * @throws ModelException if impossible
	 */
	public void setColumnPairs (ColumnPairElement[] pairs)
		throws ModelException;
}
