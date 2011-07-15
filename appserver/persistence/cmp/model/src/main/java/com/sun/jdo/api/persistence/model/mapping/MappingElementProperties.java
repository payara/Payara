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
 * MappingElementProperties.java
 *
 * Created on April 28, 2000, 6:24 PM
 */

package com.sun.jdo.api.persistence.model.mapping;

/** 
 *
 * @author raccah
 * @version %I%
 */
public interface MappingElementProperties
{
	/** Name of {@link MappingElement#getName name} property.
	 */
	public static final String PROP_NAME = "name";					// NOI18N

	/** Name of {@link MappingClassElement#isModified modified}
	 * flag for {@link MappingClassElement class elements}.
	 */
	public static final String PROP_MODIFIED = "modified";			// NOI18N

	/** Name of {@link MappingClassElement#getConsistencyLevel consistencyLevel}
	 * property for {@link MappingClassElement class elements}.
	 */
	public static final String PROP_CONSISTENCY = "consistencyLevel";	// NOI18N

	/** Name of {@link MappingClassElement#setDatabaseRoot root}
	 * property for {@link MappingClassElement class elements}.
	 */
	public static final String PROP_DATABASE_ROOT = "schema";	// NOI18N

	/** Name of {@link MappingClassElement#getTables tables}
	 * property for {@link MappingClassElement class elements}.
	 */
	public static final String PROP_TABLES = "tables";				// NOI18N

	/** Name of {@link MappingClassElement#getFields fields}
	 * property for {@link MappingClassElement class elements}.
	 */
	public static final String PROP_FIELDS = "fields";				// NOI18N

	/** Name of {@link MappingClassElement#isNavigable navigable}
	 * property for {@link MappingClassElement class elements}.
	 */
	public static final String PROP_NAVIGABLE = "navigable";		// NOI18N

	/** Name of {@link MappingFieldElement#isReadOnly read only}
	 * property for {@link MappingFieldElement field elements}.
	 */
	public static final String PROP_READ_ONLY = "readOnly";			// NOI18N

	/** Name of {@link MappingFieldElement#isInConcurrencyCheck in concurrency 
	 * check} property for {@link MappingFieldElement field elements}.
	 */
	public static final String PROP_IN_CONCURRENCY_CHECK = 
		"inConcurrencyCheck";										// NOI18N

	/** Name of {@link MappingFieldElement#isVersion version field} 
	 * property for {@link MappingFieldElement field elements}.
	 */
	public static final String PROP_VERSION_FIELD = "versionField";		// NOI18N

	/** Name of {@link MappingFieldElement#getFetchGroup fetch group} 
	 * property for {@link MappingFieldElement field elements}.
	 */
	public static final String PROP_FETCH_GROUP = "fetchGroup";		// NOI18N

	/** Name of {@link MappingFieldElement#getColumns columns} 
	 * property for {@link MappingFieldElement field elements}.
	 */
	public static final String PROP_COLUMNS = "columns";			// NOI18N

	/** Name of {@link MappingReferenceKeyElement#getTable table} and 
	 * {@link MappingTableElement#getTable table} property for 
	 * {@link MappingReferenceKeyElement reference key elements} and 
	 * {@link MappingTableElement mapping table elements}.
	 */
	public static final String PROP_TABLE = "table";				// NOI18N

	/** Name of {@link MappingTableElement#getReferencingKeys key columns}
	 * and {@link MappingTableElement#getKey key columns} property for 
	 * {@link MappingReferenceKeyElement reference key elements} and 
	 * {@link MappingTableElement mapping table elements}.
	 */
	public static final String PROP_KEY_COLUMNS = "keyColumns";		// NOI18N

	/** Name of {@link MappingRelationshipElement#getAssociatedColumns 
	 * associated columns} property for {@link MappingRelationshipElement 
	 * relationship elements}.
	 */
	public static final String PROP_ASSOCIATED_COLUMNS = 
		"associatedColumns";										// NOI18N

	/** Name of {@link MappingTableElement#getReferencingKeys referencing 
	 * keys} property for {@link MappingTableElement mapping table elements}.
	 */
	public static final String PROP_REFERENCING_KEYS = 
		"referencingKeys";											// NOI18N
}
