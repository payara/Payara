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
 * RelationshipElementImpl.java
 *
 * Created on March 2, 2000, 6:21 PM
 */

package com.sun.jdo.api.persistence.model.jdo.impl;

import java.beans.PropertyVetoException;

import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.ModelVetoException;
import com.sun.jdo.api.persistence.model.jdo.RelationshipElement;
import com.sun.jdo.spi.persistence.utility.JavaTypeHelper;

/* TODO:
	1. upper and lower bound defaults/ get method constraints based on field 
	 type (whether it's a collection or nullable etc.); pseudo-code:
	 defaults:
		 boolean isCollection = 
		 boolean isNullable = 

		_lowerBound = (isNullable) ? 0 : 1;
		_upperBound = (isCollection) ? Integer.MAX_VALUE : 1);
	getElement/CollectionClass:
		return (isCollection) ? element/collectionClass : null;
	2. What is the default for Collection Class?
 */

/** 
 *
 * @author raccah
 * @version %I%
 */
public class RelationshipElementImpl extends PersistenceFieldElementImpl
	implements RelationshipElement.Impl
{
	/** Update action of the relationship element. */
	private int _updateAction;

	/** Delete action of the relationship element. */
	private int _deleteAction;

	/** Flag indicating whether this relationship element should prefetch. */
	private boolean _isPrefetch;

	/** Lower cardinality bound of the relationship element. */
	private int _lowerBound;

	/** Upper cardinality bound of the relationship element. */
	private int _upperBound;

	/** Collection lass of the relationship element. */
	private String _collectionClass;

	/** Element class of the relationship element. */
	private String _elementClass;

	/** Relative name of the inverse relationship. */
	private String _inverseRelationshipName;

	/** Create new RelationshipElementImpl with no corresponding name.  This 
	 * constructor should only be used for cloning and archiving.
	 */
	public RelationshipElementImpl ()
	{
		this(null);
	}

	/** Creates new RelationshipElementImpl with the corresponding name 
	 * @param name the name of the element
	 */
	public RelationshipElementImpl (String name)
	{
		super(name);
		_updateAction = RelationshipElement.NONE_ACTION;
		_deleteAction = RelationshipElement.NONE_ACTION;
		_isPrefetch = false;
		_lowerBound = 0;
		_upperBound = Integer.MAX_VALUE;
	}

	/** Get the update action for this relationship element.
	 * @return the update action, one of 
	 * {@link RelationshipElement#NONE_ACTION}, 
	 * {@link RelationshipElement#NULLIFY_ACTION},
	 * {@link RelationshipElement#RESTRICT_ACTION}, 
	 * {@link RelationshipElement#CASCADE_ACTION}, or
	 * {@link RelationshipElement#AGGREGATE_ACTION}.  The default is
	 * NONE_ACTION.
	 */
	public int getUpdateAction () { return _updateAction; }

	/** Set the update action for this relationship element.
	 * @param action - an integer indicating the update action, one of:
	 * {@link RelationshipElement#NONE_ACTION}, 
	 * {@link RelationshipElement#NULLIFY_ACTION},
	 * {@link RelationshipElement#RESTRICT_ACTION}, 
	 * {@link RelationshipElement#CASCADE_ACTION}, or
	 * {@link RelationshipElement#AGGREGATE_ACTION}
	 * @exception ModelException if impossible
	 */
	public void setUpdateAction (int action) throws ModelException
	{
		Integer old = new Integer(getUpdateAction());
		Integer newAction = new Integer(action);

		try
		{
			fireVetoableChange(PROP_UPDATE_ACTION, old, newAction);
			_updateAction = action;
			firePropertyChange(PROP_UPDATE_ACTION, old, newAction);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Get the delete action for this relationship element.
	 * @return the delete action, one of 
	 * {@link RelationshipElement#NONE_ACTION}, 
	 * {@link RelationshipElement#NULLIFY_ACTION},
	 * {@link RelationshipElement#RESTRICT_ACTION}, 
	 * {@link RelationshipElement#CASCADE_ACTION}, or
	 * {@link RelationshipElement#AGGREGATE_ACTION}.  The default is
	 * NONE_ACTION.
	 */
	public int getDeleteAction () { return _deleteAction; }

	/** Set the delete action for this relationship element.
	 * @param action - an integer indicating the delete action, one of:
	 * {@link RelationshipElement#NONE_ACTION}, 
	 * {@link RelationshipElement#NULLIFY_ACTION},
	 * {@link RelationshipElement#RESTRICT_ACTION}, 
	 * {@link RelationshipElement#CASCADE_ACTION}, or
	 * {@link RelationshipElement#AGGREGATE_ACTION}
	 * @exception ModelException if impossible
	 */
	public void setDeleteAction (int action) throws ModelException
	{
		Integer old = new Integer(getDeleteAction());
		Integer newAction = new Integer(action);

		try
		{
			fireVetoableChange(PROP_DELETE_ACTION, old, newAction);
			_deleteAction = action;
			firePropertyChange(PROP_DELETE_ACTION, old, newAction);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Determines whether this relationship element should prefetch or not.
	 * @return <code>true</code> if the relationship should prefetch,
	 * <code>false</code> otherwise.  The default is <code>false</code>.
	 */
	public boolean isPrefetch () { return _isPrefetch; }

	/** Set whether this relationship element should prefetch or not.
	 * @param flag - if <code>true</code>, the relationship is set to
	 * prefetch; otherwise, it is not
	 * @exception ModelException if impossible
	 */
	public void setPrefetch (boolean flag) throws ModelException
	{
		Boolean old = JavaTypeHelper.valueOf(isPrefetch());
		Boolean newFlag = JavaTypeHelper.valueOf(flag);

		try
		{
			fireVetoableChange(PROP_PREFETCH, old, newFlag);
			_isPrefetch = flag;
			firePropertyChange(PROP_PREFETCH, old, newFlag);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Get the lower cardinality bound for this relationship element.  The 
	 * default is 0.
	 * @return the lower cardinality bound
	 */
	public int getLowerBound () { return _lowerBound; }

	/** Set the lower cardinality bound for this relationship element.
	 * @param lowerBound - an integer indicating the lower cardinality bound
	 * @exception ModelException if impossible
	 */
	public void setLowerBound (int lowerBound) throws ModelException
	{
		Integer old = new Integer(getLowerBound());
		Integer newBound = new Integer(lowerBound);

		try
		{
			fireVetoableChange(PROP_CARDINALITY, old, newBound);
			_lowerBound = lowerBound;
			firePropertyChange(PROP_CARDINALITY, old, newBound);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Get the upper cardinality bound for this relationship element.  The 
	 * default is Integer.MAX_VALUE.
	 * Returns {@link java.lang.Integer#MAX_VALUE} for <code>n</code>
	 * @return the upper cardinality bound
	 */
	public int getUpperBound () { return _upperBound; }

	/** Set the upper cardinality bound for this relationship element.
	 * @param upperBound - an integer indicating the upper cardinality bound
	 * (use {@link java.lang.Integer#MAX_VALUE} for <code>n</code>)
	 * @exception ModelException if impossible
	 */
	public void setUpperBound (int upperBound) throws ModelException
	{
		Integer old = new Integer(getUpperBound());
		Integer newBound = new Integer(upperBound);

		try
		{
			fireVetoableChange(PROP_CARDINALITY, old, newBound);
			_upperBound = upperBound;
			firePropertyChange(PROP_CARDINALITY, old, newBound);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Get the collection class (for example Set, List, Vector, etc.)
	 * for this relationship element.
	 * @return the collection class
	 */
	public String getCollectionClass () { return _collectionClass; }

	/** Set the collection class for this relationship element.
	 * @param collectionClass - a string indicating the type of
	 * collection (for example Set, List, Vector, etc.)
	 * @exception ModelException if impossible
	 */
	public void setCollectionClass (String collectionClass)
		throws ModelException
	{
		String old = getCollectionClass();

		try
		{
			fireVetoableChange(PROP_COLLECTION_CLASS, old, collectionClass);
			_collectionClass = collectionClass;
			firePropertyChange(PROP_COLLECTION_CLASS, old, collectionClass);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
 	}

	/** Get the element class for this relationship element.  If primitive
	 * types are supported, you can use
	 * <code><i>wrapperclass</i>.TYPE.toString()</code> to specify them.
	 * @return the element class
	 */
	public String getElementClass () { return _elementClass; }

	/** Set the element class for this relationship element.
	 * @param elementClass - a string indicating the type of elements
	 * in the collection.  If primitive types are supported, you can use
	 * <code><i>wrapperclass</i>.TYPE.toString()</code> to specify them.
	 * @exception ModelException if impossible
	 */
	public void setElementClass (String elementClass) throws ModelException
	{
		String old = getElementClass();

		try
		{
			fireVetoableChange(PROP_ELEMENT_CLASS, old, elementClass);
			_elementClass = elementClass;
			firePropertyChange(PROP_ELEMENT_CLASS, old, elementClass);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
 	}

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
		return _inverseRelationshipName;
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
		String newName = ((inverseRelationship != null) ?
			inverseRelationship.getName() : null);
		String oldName = getInverseRelationshipName();

		try
		{
			fireVetoableChange(PROP_INVERSE_FIELD, oldName, newName);
			_inverseRelationshipName = newName;
			firePropertyChange(PROP_INVERSE_FIELD, oldName, newName);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}
}
