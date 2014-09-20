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
 * EJBHashSet.java
 *
 * Created on December 10, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.cmp;

import java.util.*;

import javax.ejb.EJBLocalObject;

import com.sun.jdo.api.persistence.support.PersistenceManager;
import com.sun.jdo.api.persistence.support.Transaction;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.JDOEJB20Helper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperSQLStore;
/*
 * This is the implementation of the java.util.Set interface for the CMP
 * fields of the Collection type. Represents many side of the relationships.
 *
 * @author Marina Vatkina
 */
public class EJBHashSet extends HashSet {
    // Reference to the PersistenceManager and Transaction that were active 
    // at the time of this Set creation.
    private PersistenceManager pm = null;
    private Transaction tx = null;

    // HashSet of the persistence-capable instances associated with this Set.
    private HashSet pcSet = null;

    // Helper instance for conversion of the persistence-capable instances
    // to the EJBLocalObject type and back.
    private JDOEJB20Helper helper = null;

    // Flag that indicates invalid state of this Set.
    private boolean valid = false;
    
    //The logger
    private static Logger logger = LogHelperSQLStore.getLogger();

    /**
     * Creates new instance of <code>EJBHashSet</code> for this parameters.
     * @param pm the PersistenceManager associated with the calling bean.
     * @param helper the JDOEJB20Helper instance.
     * @param pcs a Collection of  persistence-capable instances.
     */
    public EJBHashSet(PersistenceManager pm, JDOEJB20Helper helper, Collection pcs) {
        this.pm = pm;
        tx = pm.currentTransaction();
        this.helper = helper;

        // Convert Collection.
        setSCOHashSet(pcs);

        valid = true;
    }

        // -------------------------Public Methods------------------

    /**
     * Adds the specified element to this set if it is not already
     * present.
     *   
     * @param o element to be added to this set.
     * @return <tt>true</tt> if the set did not already contain the specified
     * element.
     * @see java.util.HashSet
     */  
    public boolean add(Object o) {
        logger.finest("---EJBHashSet.add---"); // NOI18N
        assertIsValid();
        assertInTransaction();
        helper.assertInstanceOfLocalInterfaceImpl(o);
        Object pc = helper.convertEJBLocalObjectToPC((EJBLocalObject) o, pm, true);
        return pcSet.add(pc);
    }


    /**
     * Adds all of the elements in the specified collection to this collection
     *   
     * @param c collection whose elements are to be added to this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     * call.
     * @throws UnsupportedOperationException if the <tt>addAll</tt> method is
     *            not supported by this collection.
     *
     * @see java.util.AbstractCollection
     * @see java.util.HashSet
     */
    public boolean addAll(Collection c) {
        logger.finest("---EJBHashSet.addAll---"); // NOI18N
        assertIsValid();
        assertInTransaction();
        assertInstancesOfLocalInterfaceImpl(c); 
        return pcSet.addAll(helper.convertCollectionEJBLocalObjectToPC(c, pm, true));
    }

    /**
     * Removes the given element from this set if it is present.
     *   
     * @param o object to be removed from this set, if present.
     * @return <tt>true</tt> if the set contained the specified element.
     * @see java.util.HashSet
     */  
    public boolean remove(Object o) {
        logger.finest("---EJBHashSet.remove---"); // NOI18N
        assertIsValid();
        assertInTransaction();
        helper.assertInstanceOfLocalInterfaceImpl(o);
        EJBLocalObject lo = (EJBLocalObject) o;
        return pcSet.remove(helper.convertEJBLocalObjectToPC(lo, pm, true));
    }

    /**
     * Removes from this collection all of its elements that are contained in
     * the specified collection (optional operation). <p>
     * Processes each element remove internally not to have call backs
     * into #remove(Object).
     *   
     * @param c elements to be removed from this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     * call.
     *   
     * @throws    UnsupportedOperationException removeAll is not supported
     *            by this collection.
     *   
     * @see java.util.HashSet
     * @see java.util.AbstractCollection
     */  
    public boolean removeAll(Collection c) {
        logger.finest("---EJBHashSet.removeAll---"); // NOI18N
        assertIsValid();
        assertInTransaction();
        assertInstancesOfLocalInterfaceImpl(c); 
        return pcSet.removeAll(helper.convertCollectionEJBLocalObjectToPC(c, pm, true));
    }

    /**
     * Retains only the elements in this collection that are contained in the
     * specified collection (optional operation).
     *   
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call.
     *   
     * @throws UnsupportedOperationException if the <tt>retainAll</tt> method
     *            is not supported by this collection.
     *   
     * @see java.util.HashSet
     * @see java.util.AbstractCollection
     */  
    public boolean retainAll(Collection c) {
        logger.finest("---EJBHashSet.retainAll---"); // NOI18N
        assertIsValid();
        assertInTransaction();
        assertInstancesOfLocalInterfaceImpl(c); 
        return pcSet.retainAll(helper.convertCollectionEJBLocalObjectToPC(c, pm, true));
    }


    /**
     * Removes all of the elements from this set.
     * @see java.util.HashSet
     */  
    public void clear() {
        logger.finest("---EJBHashSet.clear---"); // NOI18N
        assertIsValid();
        assertInTransaction();
        pcSet.clear();
    }

    /**
     * Returns the number of elements in this set (its cardinality).
     * 
     * @return the number of elements in this set (its cardinality).
     */
    public int size() {
        logger.finest("---EJBHashSet.size---"); // NOI18N
        assertIsValid();
        assertInTransaction();
        return pcSet.size();
    }

    /**
     * Returns <tt>true</tt> if this set contains no elements.
     * 
     * @return <tt>true</tt> if this set contains no elements.
     */
    public boolean isEmpty() {
        logger.finest("---EJBHashSet.isEmpty---"); // NOI18N
        assertIsValid();
        assertInTransaction();
        return pcSet.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if this set contains the specified element.
     * 
     * @param o element whose presence in this set is to be tested.
     * @return <tt>true</tt> if this set contains the specified element.
     */
    public boolean contains(Object o) {
        logger.finest("---EJBHashSet.contains---"); // NOI18N
        assertIsValid();
        assertInTransaction();
        helper.assertInstanceOfLocalInterfaceImpl(o);
        EJBLocalObject lo = (EJBLocalObject) o;
        return pcSet.contains(helper.convertEJBLocalObjectToPC(lo, pm, true));
    }

    /**
     * Returns <tt>true</tt> if this collection contains all of the elements
     * in the specified collection. <p>
     *   
     * This implementation iterates over the specified collection, checking
     * each element returned by the iterator in turn to see if it's
     * contained in this collection.  If all elements are so contained
     * <tt>true</tt> is returned, otherwise <tt>false</tt>.
     *   
     * @param c collection to be checked for containment in this collection.
     * @return <tt>true</tt> if this collection contains all of the elements
     *         in the specified collection.
     *   
     * @see #contains(Object)
     */  
    public boolean containsAll(Collection c) {
        logger.finest("---EJBHashSet.containsAll---"); // NOI18N
        assertIsValid();
        assertInTransaction();
        assertInstancesOfLocalInterfaceImpl(c);
        return pcSet.containsAll(helper.convertCollectionEJBLocalObjectToPC(c, pm, true));
    }

    /**
     * Returns a shallow copy of this <tt>HashSet</tt> instance: the elements
     * themselves are not cloned.
     *   
     * @return a shallow copy of this set.
     */  
    public Object clone() {
        logger.finest("---EJBHashSet.clone---"); // NOI18N
        EJBHashSet newSet = (EJBHashSet)super.clone();
        newSet.pcSet = (HashSet)pcSet.clone();
        return newSet;
    }

    /** 
     * Returns set of the persistence-capable instances associated
     * with this Set.
     * @return Set of the persistence-capable instances.
     */
    public HashSet getSCOHashSet() {
        assertIsValid();
        assertInTransaction();
        return (pcSet != null) ? (HashSet)pcSet.clone() : null;
    }

    /** 
     * Replace the set of the persistence-capable instances associated
     * with this EJBHashSet.
     * There is no need to check transaction as it has already been checked
     * in this case.
     */
    public void setSCOHashSet(Collection coll) {
        if (coll instanceof java.util.HashSet)
            pcSet = (java.util.HashSet)coll;
        else
            pcSet = new java.util.HashSet(coll);
    }

    /**
     * Returns an iterator over the elements in this set.  The elements
     * are returned in no particular order.
     *
     * @return an Iterator over the elements in this set.
     * @see ConcurrentModificationException
     */
    public Iterator iterator() {
        assertIsValid();
        assertInTransaction();
        return new EJBHashIterator();
    }

    private class EJBHashIterator implements Iterator {
        Iterator _iterator = null;
        Object lastReturned = null;

        EJBHashIterator() {
            _iterator = pcSet.iterator();
        }

        public boolean hasNext() {
            assertIsValid();
            assertInTransaction();
            return _iterator.hasNext();
        }

        public Object next() {
            assertIsValid();
            assertInTransaction();
            try {
                lastReturned = _iterator.next();
            } catch(ConcurrentModificationException e) {
                IllegalStateException ise = new IllegalStateException(
                        e.toString());
                ise.initCause(e);
                throw ise;
            }
            return  helper.convertPCToEJBLocalObject(lastReturned, pm);
        }

        public void remove() {
            assertIsValid();
            assertInTransaction();
            try {
                _iterator.remove();
            } catch(ConcurrentModificationException e) {
                IllegalStateException ise = new IllegalStateException(
                        e.toString());
                ise.initCause(e);
                throw ise;
            }
        }
    }

    /**
     * Verifies that this Set is not marked as invalid.
     * @throw IllegalStateException of validation fails.
     */
    private void assertIsValid() {
        if (!valid)
            throw new IllegalStateException(); // RESOLVE Exception text.
    }

    /**
     * Verifies that persistence manager is not closed and
     * the current transaction is active.
     * @throw IllegalStateException of validation fails.
     */
    private void assertInTransaction() {
        if (pm.isClosed() || !tx.isActive()) {
            invalidate();
            throw new IllegalStateException(); // RESOLVE Exception text.
        }
    }

    /**
     * Verifies that elements of this Collection are of the expected type.
     * @param c the Collection to verify.
     * @throw EJBException of validation fails.
     */
    private void assertInstancesOfLocalInterfaceImpl(Collection c) {
        for (Iterator it = c.iterator(); it.hasNext();)
            helper.assertInstanceOfLocalInterfaceImpl(it.next());
    }

    /**
     * Marks this Set as invalid and releases all references.
     */
    public void invalidate() {
        valid = false;
        pm = null;
        tx = null;
        helper = null;
        pcSet = null;
    }
}
