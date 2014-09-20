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
 * PersistenceElementProperties.java
 *
 * Created on March 2, 2000, 12:54 PM
 */

package com.sun.jdo.api.persistence.model.jdo;

/** 
 *
 * @author raccah
 * @version %I%
 */
public interface PersistenceElementProperties
{
	/** Name of {@link PersistenceElement#getName name} property.
	 */
	public static final String PROP_NAME = "name";					// NOI18N

	/** Name of {@link PersistenceClassElement#isModified modified}
	 * flag for {@link PersistenceClassElement class elements}.
	 */
	public static final String PROP_MODIFIED = "modified";			// NOI18N

	/** Name of {@link PersistenceClassElement#getObjectIdentityType identity}
	 * property for {@link PersistenceClassElement class elements}.
	 */
	public static final String PROP_IDENTITY = "identity";			// NOI18N

	/** Name of {@link PersistenceClassElement#getKeyClass key class}
	 * property for {@link PersistenceClassElement class elements}.
	 */
	public static final String PROP_KEY_CLASS = "keyClass";			// NOI18N

	/** Name of {@link PersistenceClassElement#getFields fields}
	 * property for {@link PersistenceClassElement class elements}.
	 */
	public static final String PROP_FIELDS = "fields";				// NOI18N

	/** Name of {@link PersistenceClassElement#getConcurrencyGroups concurrency
	 * groups} property for {@link PersistenceClassElement class elements}.
	 */
	public static final String PROP_GROUPS = "groups";				// NOI18N

	/** Name of {@link PersistenceFieldElement#getPersistenceType persistence}
	 * property for {@link PersistenceFieldElement field elements}.
	 */
	public static final String PROP_PERSISTENCE = "persistence";	// NOI18N

	/** Name of {@link PersistenceFieldElement#isReadSensitive read sensitivity}
	 * and {@link PersistenceFieldElement#isWriteSensitive write sensitivity}
	 * property for {@link PersistenceFieldElement field elements}.
	 */
	public static final String PROP_SENSITIVITY = "sensitivity";	// NOI18N

	/** Name of {@link PersistenceFieldElement#isKey key field}
	 * property for {@link PersistenceFieldElement field elements}.
	 */
	public static final String PROP_KEY_FIELD = "keyField";			// NOI18N

	/** Name of {@link RelationshipElement#getUpdateAction update action}
	 * property for {@link RelationshipElement relationship elements}.
	 */
	public static final String PROP_UPDATE_ACTION = "updateAction";	// NOI18N

	/** Name of {@link RelationshipElement#getDeleteAction delete action}
	 * property for {@link RelationshipElement relationship elements}.
	 */
	public static final String PROP_DELETE_ACTION = "deleteAction";	// NOI18N

	/** Name of {@link RelationshipElement#isPrefetch prefetch}
	 * property for {@link RelationshipElement relationship elements}.
	 */
	public static final String PROP_PREFETCH = "prefetch";			// NOI18N

	/** Name of {@link RelationshipElement#getLowerBound lower bound}
	 * and {@link RelationshipElement#getUpperBound upper bound}
	 * property for {@link RelationshipElement relationship elements}.
	 */
	public static final String PROP_CARDINALITY = "cardinality";	// NOI18N

	/** Name of {@link RelationshipElement#getCollectionClass collection class}
	 * property for {@link RelationshipElement relationship elements}.
	 */
	public static final String PROP_COLLECTION_CLASS = 
		"collectionClass";											// NOI18N

	/** Name of {@link RelationshipElement#getElementClass element class}
	 * property for {@link RelationshipElement relationship elements}.
	 */
	public static final String PROP_ELEMENT_CLASS = "elementClass";	// NOI18N

	/** Name of {@link RelationshipElement#getInverseRelationshipName inverse
	 * relationship name} property for {@link RelationshipElement relationship 
	 * elements}.
	 */
	public static final String PROP_INVERSE_FIELD = "relatedField";	// NOI18N
}
