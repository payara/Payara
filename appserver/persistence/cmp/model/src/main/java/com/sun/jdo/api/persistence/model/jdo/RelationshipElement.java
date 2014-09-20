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
 * RelationshipElement.java
 *
 * Created on February 29, 2000, 2:30 PM
 */

package com.sun.jdo.api.persistence.model.jdo;

import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.api.persistence.model.ModelException;
import org.glassfish.persistence.common.I18NHelper;

/* TODO:
	1. throw (Model or IllegalArgument)Exception on set illegal constant values?
		also applies to PersistenceClass, PersistenceField classes
	2. enforce/validate collection/bound combinations, document that other piece
		only used when ...
	3. document default values for all constants (should that go in impl docs?)
		also applies to PersistenceClass, PersistenceField classes
	4. document support collection classes?  are we supporting this at all or
		does the user have to type that directly into the code?
 */

/** 
 *
 * @author raccah
 * @version %I%
 */
public class RelationshipElement extends PersistenceFieldElement
{
	/** Constant representing no action. */
	public static final int NONE_ACTION = 0;

	/** Constant representing nullify action. */
	public static final int NULLIFY_ACTION = 1;

	/** Constant representing restrict action. */
	public static final int RESTRICT_ACTION = 2;

	/** Constant representing cascade action. */
	public static final int CASCADE_ACTION = 3;

	/** Constant representing aggregate action. */
	public static final int AGGREGATE_ACTION = 4;

	/** Create new RelationshipElement with no implementation. 
	 * This constructor should only be used for cloning and archiving.
	 */
	public RelationshipElement ()
	{
		this(null, null);
	}

	/** Create new RelationshipElement with the provided implementation. The 
	 * implementation is responsible for storing all properties of the object.
	 * @param impl the implementation to use
	 * @param declaringClass the class to attach to
	 */
	public RelationshipElement (RelationshipElement.Impl impl, 
		PersistenceClassElement declaringClass)
	{
		super(impl, declaringClass);
	}

	/** @return implemetation factory for this relationship
	 */
	final Impl getRelationshipImpl () { return (Impl)getImpl(); }

	//================ Update and Delete Semantics ============================

	/** Get the update action for this relationship element.
	 * @return the update action, one of {@link #NONE_ACTION}, 
	 * {@link #NULLIFY_ACTION}, {@link #RESTRICT_ACTION}, 
	 * {@link #CASCADE_ACTION}, or {@link #AGGREGATE_ACTION}
	 */
	public int getUpdateAction ()
	{
		return getRelationshipImpl().getUpdateAction();
	}

	/** Set the update action for this relationship element.
	 * @param action - an integer indicating the update action, one of:
	 * {@link #NONE_ACTION}, {@link #NULLIFY_ACTION}, {@link #RESTRICT_ACTION}, 
	 * {@link #CASCADE_ACTION}, or {@link #AGGREGATE_ACTION}
	 * @exception ModelException if impossible
	 */
	public void setUpdateAction (int action) throws ModelException
	{
		if ((action < NONE_ACTION) || (action > AGGREGATE_ACTION))
		{
			throw new ModelException(I18NHelper.getMessage(getMessages(),
				"jdo.relationship.update_action_invalid", action));	// NOI18N
		}

		getRelationshipImpl().setUpdateAction(action);
	}

	/** Get the delete action for this relationship element.
	 * @return the delete action, one of {@link #NONE_ACTION}, 
	 * {@link #NULLIFY_ACTION}, {@link #RESTRICT_ACTION}, 
	 * {@link #CASCADE_ACTION}, or {@link #AGGREGATE_ACTION}
	 */
	public int getDeleteAction ()
	{
		return getRelationshipImpl().getDeleteAction();
	}

	/** Set the delete action for this relationship element.
	 * @param action - an integer indicating the delete action, one of:
	 * {@link #NONE_ACTION}, {@link #NULLIFY_ACTION}, {@link #RESTRICT_ACTION}, 
	 * {@link #CASCADE_ACTION}, or {@link #AGGREGATE_ACTION}
	 * @exception ModelException if impossible
	 */
	public void setDeleteAction (int action) throws ModelException
	{
		if ((action < NONE_ACTION) || (action > AGGREGATE_ACTION))
		{
			throw new ModelException(I18NHelper.getMessage(getMessages(),
				"jdo.relationship.delete_action_invalid", action));	// NOI18N
		}

		getRelationshipImpl().setDeleteAction(action);
	}

	/** Determines whether this relationship element should prefetch or not.  
	 * @return <code>true</code> if the relationship should prefetch, 
	 * <code>false</code> otherwise
	 */
	public boolean isPrefetch ()
	{
		return getRelationshipImpl().isPrefetch();
	}

	/** Set whether this relationship element should prefetch or not.
	 * @param flag - if <code>true</code>, the relationship is set to prefetch;
	 * otherwise, it is not
	 * @exception ModelException if impossible
	 */
	public void setPrefetch (boolean flag) throws ModelException
	{
		getRelationshipImpl().setPrefetch(flag);
	}

	//================ Cardinality Bounds ============================

	/** Get the lower cardinality bound for this relationship element.
	 * @return the lower cardinality bound
	 */
	public int getLowerBound ()
	{
		return getRelationshipImpl().getLowerBound();
	}

	/** Set the lower cardinality bound for this relationship element.
	 * @param lowerBound - an integer indicating the lower cardinality bound
	 * @exception ModelException if impossible
	 */
	public void setLowerBound (int lowerBound) throws ModelException
	{
		if ((lowerBound > getUpperBound()) || (lowerBound < 0))
		{
			throw new ModelException(I18NHelper.getMessage(getMessages(),
				"jdo.relationship.lower_cardinality_invalid"));		// NOI18N
		}

		getRelationshipImpl().setLowerBound(lowerBound);
	}

	/** Get the upper cardinality bound for this relationship element.  Returns
	 * {@link java.lang.Integer#MAX_VALUE} for <code>n</code>
	 * @return the upper cardinality bound
	 */
	public int getUpperBound ()
	{
		return getRelationshipImpl().getUpperBound();
	}

	/** Set the upper cardinality bound for this relationship element.
	 * @param upperBound - an integer indicating the upper cardinality bound
	 * (use {@link java.lang.Integer#MAX_VALUE} for <code>n</code>)
	 * @exception ModelException if impossible
	 */
	public void setUpperBound (int upperBound) throws ModelException
	{
		if ((upperBound < getLowerBound()) || (upperBound <= 0))
		{
			throw new ModelException(I18NHelper.getMessage(getMessages(),
				"jdo.relationship.upper_cardinality_invalid"));		// NOI18N
		}

		getRelationshipImpl().setUpperBound(upperBound);
	}

	//================ Collection Support ============================

	/** Get the collection class (for example Set, List, Vector, etc.)
	 * for this relationship element.
	 * @return the collection class
	 */
	public String getCollectionClass ()
	{
		return getRelationshipImpl().getCollectionClass();
	}

	/** Set the collection class for this relationship element.
	 * @param collectionClass - a string indicating the type of 
	 * collection (for example Set, List, Vector, etc.)
	 * @exception ModelException if impossible
	 */
	public void setCollectionClass (String collectionClass)
		throws ModelException
	{
		getRelationshipImpl().setCollectionClass(collectionClass);
	}

	/** Get the element class for this relationship element.  If primitive 
	 * types are supported, you can use 
	 * <code><i>wrapperclass</i>.TYPE.toString()</code> to specify them.
	 * @return the element class
	 */
	public String getElementClass ()
	{
		return getRelationshipImpl().getElementClass();
	}

	/** Set the element class for this relationship element.
	 * @param elementClass - a string indicating the type of elements in
	 * the collection.  If primitive types are supported, you can use 
	 * <code><i>wrapperclass</i>.TYPE.toString()</code> to specify them.
	 * @exception ModelException if impossible
	 */
	public void setElementClass (String elementClass) throws ModelException
	{
		getRelationshipImpl().setElementClass(elementClass);
	}

	//================ Two-way Relationship Support ============================

	// Note that the fact that inverse relationship name (an implementation 
	// detail) is exposed and model is required as an argument for the get/set 
	// methods is an artifact of our current design (i.e. archiver/vertical 
	// split issues, no way to look up a related class without use of the 
	// Model, etc.) and should be removed in the new Orion model

	/** Get the relative name of the inverse relationship field for this 
	 * relationship element.  In the case of two-way relationships, the two 
	 * relationship elements involved are inverses of each other.  If this 
	 * relationship element does not participate in a two-way relationship, 
	 * this returns <code>null</code>.  Note that it is possible to have this 
	 * method return a value, but because of the combination of related class 
	 * and lookup, there may be no corresponding RelationshipElement which can 
	 * be found.
	 * @return the relative name of the inverse relationship element
	 * @see #getInverseRelationship
	 */
	public String getInverseRelationshipName ()
	{
		return getRelationshipImpl().getInverseRelationshipName();
	}

	/** Get the inverse relationship element for this relationship element.  
	 * In the case of two-way relationships, the two relationship elements 
	 * involved are inverses of each other.  If this relationship element does 
	 * not participate in a two-way relationship, this returns 
	 * <code>null</code>.  Note that it is also possible for this method to 
	 * return <code>null</code> even if {@link #getInverseRelationshipName}
	 * returns a value because the corresponding RelationshipElement cannot 
	 * be found using the combination of related class and lookup (model).
	 * @param model the model object to be used to look it up
	 * @return the inverse relationship element if it exists
	 * @see #getInverseRelationshipName
	 */
	public RelationshipElement getInverseRelationship (Model model)
	{
		String inverseName = getInverseRelationshipName();
		RelationshipElement inverse = null;

		if ((model != null) && (inverseName != null))
		{
			String relatedClass = model.getRelatedClass(this);

			if (relatedClass != null)
			{
				PersistenceClassElement relatedElement =
					model.getPersistenceClass(relatedClass);

				if (relatedElement != null)
					inverse = relatedElement.getRelationship(inverseName);
			}
		}

		return inverse;
	}

	/** Set the inverse relationship element for this relationship element.
	 * In the case of two-way relationships, the two relationship elements 
	 * involved are inverses of each other.
	 * @param inverseRelationship - a relationship element to be used as the 
	 * inverse for this relationship element or <code>null</code> if this 
	 * relationship element does not participate in a two-way relationship.
	 * @param model the model object to be used to look up the old inverse so 
	 * it can be unset
	 * @exception ModelException if impossible
	 */
	public void setInverseRelationship (RelationshipElement inverseRelationship,
		Model model) throws ModelException
	{
		RelationshipElement old = getInverseRelationship(model);

		if ((old != inverseRelationship) || ((inverseRelationship == null) && 
			(getInverseRelationshipName() != null)))
		{
			// clear old inverse which still points to here
			if (old != null)
			{
				RelationshipElement oldInverse = 
					old.getInverseRelationship(model);

				if (this.equals(oldInverse))
					old.changeInverseRelationship(null);
			}

			// link from here to new inverse
			changeInverseRelationship(inverseRelationship);

			// link from new inverse back to here
			if (inverseRelationship != null)
				inverseRelationship.changeInverseRelationship(this);
		}
	}

	/** Changes the inverse relationship element for this relationship element.
	 * This method is invoked for both sides from 
	 * {@link RelationshipElement#setInverseRelationship} and should handle the 
	 * vetoable change events, property change events, and setting the internal
	 * variable. 
	 * @param inverseRelationship - a relationship element to be used as the 
	 * inverse for this relationship element or <code>null</code> if this 
	 * relationship element does not participate in a two-way relationship.
	 * @exception ModelException if impossible
	 */
	public void changeInverseRelationship (
		RelationshipElement inverseRelationship) throws ModelException
	{
		getRelationshipImpl().changeInverseRelationship(inverseRelationship);
	}

	/** Pluggable implementation of the storage of relationship element 
	 * properties.
	 * @see RelationshipElement#RelationshipElement
	 */
	public interface Impl extends PersistenceFieldElement.Impl
	{
		/** Get the update action for this relationship element.
		 * @return the update action, one of {@link #NONE_ACTION}, 
		 * {@link #NULLIFY_ACTION}, {@link #RESTRICT_ACTION}, 
		 * {@link #CASCADE_ACTION}, or {@link #AGGREGATE_ACTION}
		 */
		public int getUpdateAction ();

		/** Set the update action for this relationship element.
		 * @param action - an integer indicating the update action, one of:
		 * {@link #NONE_ACTION}, {@link #NULLIFY_ACTION}, 
		 * {@link #RESTRICT_ACTION}, {@link #CASCADE_ACTION}, or 
		 * {@link #AGGREGATE_ACTION}
		 * @exception ModelException if impossible
		 */
		public void setUpdateAction (int action) throws ModelException;

		/** Get the delete action for this relationship element.
		 * @return the delete action, one of {@link #NONE_ACTION}, 
		 * {@link #NULLIFY_ACTION}, {@link #RESTRICT_ACTION}, 
		 * {@link #CASCADE_ACTION}, or {@link #AGGREGATE_ACTION}
		 */
		public int getDeleteAction ();

		/** Set the delete action for this relationship element.
		 * @param action - an integer indicating the delete action, one of:
		 * {@link #NONE_ACTION}, {@link #NULLIFY_ACTION}, 
		 * {@link #RESTRICT_ACTION}, {@link #CASCADE_ACTION}, or 
		 * {@link #AGGREGATE_ACTION}
		 * @exception ModelException if impossible
		 */
		public void setDeleteAction (int action) throws ModelException;

		/** Determines whether this relationship element should prefetch or not.
		 * @return <code>true</code> if the relationship should prefetch, 
		 * <code>false</code> otherwise
		 */
		public boolean isPrefetch ();

		/** Set whether this relationship element should prefetch or not.
		 * @param flag - if <code>true</code>, the relationship is set to 
		 * prefetch; otherwise, it is not
		 * @exception ModelException if impossible
		 */
		public void setPrefetch (boolean flag) throws ModelException;

		/** Get the lower cardinality bound for this relationship element.
		 * @return the lower cardinality bound
		 */
		public int getLowerBound ();

		/** Set the lower cardinality bound for this relationship element.
		 * @param lowerBound - an integer indicating the lower cardinality bound
		 * @exception ModelException if impossible
		 */
		public void setLowerBound (int lowerBound) throws ModelException;

		/** Get the upper cardinality bound for this relationship element.  
		 * Returns {@link java.lang.Integer#MAX_VALUE} for <code>n</code>
		 * @return the upper cardinality bound
		 */
		public int getUpperBound ();

		/** Set the upper cardinality bound for this relationship element.
		 * @param upperBound - an integer indicating the upper cardinality bound
		 * (use {@link java.lang.Integer#MAX_VALUE} for <code>n</code>)
		 * @exception ModelException if impossible
		 */
		public void setUpperBound (int upperBound) throws ModelException;

		/** Get the collection class (for example Set, List, Vector, etc.)
		 * for this relationship element.
		 * @return the collection class
		 */
		public String getCollectionClass ();

		/** Set the collection class for this relationship element.
		 * @param collectionClass - a string indicating the type of 
		 * collection (for example Set, List, Vector, etc.)
		 * @exception ModelException if impossible
		 */
		public void setCollectionClass (String collectionClass)
			throws ModelException;

		/** Get the element class for this relationship element.  If primitive 
		 * types are supported, you can use 
		 * <code><i>wrapperclass</i>.TYPE.toString()</code> to specify them.
		 * @return the element class
		 */
		public String getElementClass ();

		/** Set the element class for this relationship element.
		 * @param elementClass - a string indicating the type of elements 
		 * in the collection.  If primitive types are supported, you can use 
		 * <code><i>wrapperclass</i>.TYPE.toString()</code> to specify them.
		 * @exception ModelException if impossible
		 */
		public void setElementClass (String elementClass) throws ModelException;

		/** Get the relative name of the inverse relationship field for this 
		 * relationship element.  In the case of two-way relationships, the two 
		 * relationship elements involved are inverses of each other.  If this 
		 * relationship element does not participate in a two-way relationship, 
		 * this returns <code>null</code>.  Note that it is possible to have 
		 * this method return a value, but because of the combination of 
		 * related class and lookup, there may be no corresponding 
		 * RelationshipElement which can be found.
		 * @return the relative name of the inverse relationship element
		 * @see #getInverseRelationship
		 */
		public String getInverseRelationshipName ();

		/** Changes the inverse relationship element for this relationship 
		 * element.  This method is invoked for both sides from 
		 * {@link RelationshipElement#setInverseRelationship} and should handle 
		 * the vetoable change events, property change events, and setting the 
		 * internal variable. 
		 * @param inverseRelationship - a relationship element to be used as 
		 * the inverse for this relationship element or <code>null</code> if 
		 * this relationship element does not participate in a two-way 
		 * relationship.
		 * @exception ModelException if impossible
		 */
		public void changeInverseRelationship (
			RelationshipElement inverseRelationship) throws ModelException;
	}
}
