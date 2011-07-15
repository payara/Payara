/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

//----------------------------------------------------------------------------
//
// Module:      NestingInfo.java
//
// Description: Subtransaction state management.
//
// Product:     com.sun.jts.CosTransactions
//
// Author:      Simon Holdsworth
//
// Date:        March, 1997
//
// Copyright (c):   1995-1997 IBM Corp.
//
//   The source code for this program is not published or otherwise divested
//   of its trade secrets, irrespective of what has been deposited with the
//   U.S. Copyright Office.
//
//   This software contains confidential and proprietary information of
//   IBM Corp.
//----------------------------------------------------------------------------

package com.sun.jts.CosTransactions;

import java.util.*;

import org.omg.CORBA.*;
import org.omg.CosTransactions.*;

/**
 * The NestingInfo interface provides operations that record all nesting
 * information relevant to a Coordinator, that is, the set of children
 * (SubCoordinators) and the sequence of ancestors (Coordinators/global IDs).
 * As an instance of this class may be accessed from multiple threads within a
 * process, serialisation for thread-safety is necessary in the
 * implementation. The information recorded in an instance of this class does
 * not need to be reconstructible in the case of a system failure as
 * subtransactions are not durable.
 *
 * @version 0.01
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see
 */
class NestingInfo {
    CoordinatorImpl[] ancestorSeq = null;
    Vector childSet = new Vector();
    boolean removed = false;

    /**
     * Default NestingInfo constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    NestingInfo() {}

    /**
     * Defines the sequence of ancestors and initialises the
     * set of children to be empty.
     *
     * @param ancestors  The ancestors
     *
     * @return
     *
     * @see
     */
    NestingInfo(CoordinatorImpl[] ancestors) {

        // If the sequence of ancestors is empty, set the removed flag as this
        // NestingInfo is part of a top-level transaction.

        ancestorSeq = (CoordinatorImpl[])ancestors.clone();
        removed = (ancestors.length == 0);
    }

    /**
     * Adds the given SubCoordinator as a child.
     * <p>
     * If the reference is already in the set, the operation returns false.
     *
     * @param child  The child Coordinator.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    boolean addChild(CoordinatorImpl child) {

        boolean result = !childSet.contains(child);
        if( result ) childSet.addElement(child);

        return result;
    }

    /**
     * Removes the given SubCoordinator as a child.
     * <p>
     * If the reference is not in the set, the operation returns false.
     *
     * @param child  The child Coordinator.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    boolean removeChild(CoordinatorImpl child) {
        boolean result = childSet.removeElement(child);
        return result;
    }

    /**
     * Empties the set of children.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    void empty() {
        childSet.removeAllElements();
    }

    /**
     * Removes the given Coordinator as a child from the parent Coordinator.
     * <p>
     * If the child could not be removed from the parent, the operation returns
     * false.
     *
     * @param child  The child coordinator.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    boolean removeFromParent(CoordinatorImpl child) {

        // If not already done, remove the child from the set of its parents.
        // If the NestingInfo instance was created for a top-level
        // transaction, the known to parent flag will always be set.
        // NOTE: Assumes parent is a CoordinatorImpl.

        boolean result = true;
        if(!removed) {
            CoordinatorImpl parent = ancestorSeq[0];

            result = parent.removeChild(child);
            removed = true;
        }

        return result;
    }

    /**
     * Returns a reference to the parent Coordinator.
     * <p>
     * If there is none, returns null.
     * <p>
     * The parent Coordinator is the first in the sequence of ancestors.
     * <p>
     * If the forgetting flag is set, the NestingInfo must not call the parent
     * when the child calls removeFromParent.
     *
     * @param forgetting  Indicates whether the transaction is being forgotten.
     *
     * @return  The parent Coordinator.
     *
     * @see
     */
    CoordinatorImpl getParent(boolean forgetting) {

        CoordinatorImpl result = null;

        // If there are no ancestors, there is no parent,
        // otherwise return the first ancestor.

        if (ancestorSeq.length != 0)
            result = ancestorSeq[0];

        // If the Coordinator is being cleaned up, then we must not
        // call the parent when the child calls removeFromParent.

        if( forgetting ) removed = true;

        return result;
    }

    /**
     * Returns a reference to the top-level Coordinator.
     * <p>
     * If the containing Coordinator is the top-level one, a null reference is
     * returned.
     * <p>
     * The top-level Coordinator is the last in the sequence of ancestors.
     *
     * @param
     *
     * @return  The top-level ancestor.
     *
     * @see
     */
    CoordinatorImpl getTopLevel() {

        CoordinatorImpl result = null;

        // If there are no ancestors, there is no top-level,
        // otherwise return the last ancestor.

        if( ancestorSeq.length != 0 )
            result = ancestorSeq[ancestorSeq.length - 1];

        return result;
    }

    /**
     * Returns a copy of the sequence of ancestors of the Coordinator.
     * <p>
     * If the containing Coordinator is the top-level one, an empty
     * sequence is returned.
     * <p>
     * The caller is responsible for freeing the sequence storage.
     *
     *  @param
     *
     * @return  The sequence of ancestors.
     *
     * @see
     */
    CoordinatorImpl[] getAncestors() {

        CoordinatorImpl[] result = null;

        // If there are no ancestors, return the empty sequence.
        // If we cannot obtain a buffer to copy the sequence, return
        // empty sequence Perhaps an exception should be raised instead ?

        result = (CoordinatorImpl[]) ancestorSeq.clone();

        return result;
    }

    /**
     * Returns a count of the number of children that have been defined.
     * <p>
     * If no nesting information has been defined, the operation returns 0.
     *
     * @param
     *
     * @return  The number of children.
     *
     * @see
     */
    int numChildren() {
        return childSet.size();
    }

    /**
     * Checks whether any child represents an active transaction.
     * <p>
     * This is used during sending_reply to check for outstanding work.
     * <p>
     * 'active' here means subordinate children that have not
     * registered with their superior, or root children -
     * these represent parts of a transaction  that may not be
     * committed or rolled back before the parent completes.
     *
     * @param
     *
     * @return  Indicates if all is OK.
     *
     * @see
     */
    boolean replyCheck() {

        boolean result = false;

        // If there are any children, browse through the set, checking them.

        for (int i = 0; i < childSet.size() && !result; i++) {
            CoordinatorImpl child = (CoordinatorImpl) childSet.elementAt(i);
            result = child.isActive();
        }

        return result;
    }

    /**
     * Determines whether the Coordinator containing the NestingInfo object is
     * a descendant of the other Coordinator.
     * <p>
     * This is true if the other Coordinator is the same
     * as one of the ancestors.
     *
     * @param other  The other Coordinator.
     *
     * @return  Indicates success of the operation.
     *
     * @see
    */
    boolean isDescendant(Coordinator other) {

        boolean result = false;

        // Go through the ancestors, checking whether the given
        // transaction is the same as any of them.

        try {
            for (int i = 0; i < ancestorSeq.length && !result; i++) {
                result = ancestorSeq[i].is_same_transaction(other);
            }
        } catch(SystemException exc) {
            result = false;
        }

        return result;
    }

    /**
     * Rolls back all children in the set; if there are none the operation does
     * nothing.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    void rollbackFamily() {

        // If there are any children, browse through the set,
        // rolling them back.

        while (childSet.size() > 0) {

            // Note that we browse in a different way here, as each
            // child removes itself from the set when the rollback is
            // complete, so we just keep getting the
            // first element in the set until the set is empty.

            CoordinatorImpl child = (CoordinatorImpl)childSet.elementAt(0);
            try {
                child.rollback(true);
            } catch (Throwable exc) {}
        }
    }
}
