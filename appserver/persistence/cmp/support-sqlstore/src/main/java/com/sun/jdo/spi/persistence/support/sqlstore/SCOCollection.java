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
 * SCOCollection.java
 *
 * created April 3, 2000
 *
 * @author Marina Vatkina
 * @version 1.0
 */

package com.sun.jdo.spi.persistence.support.sqlstore;
import java.util.Collection;

public interface SCOCollection extends java.util.Collection, SCO
{
    /**
     * Resets removed and added lists after flush
     */
    void reset();

    /*
     * Mark the collection as deferred so any updates won't
     * be applied until markDeferred() gets called
     */
    void markDeferred();

    /*
     * Return true is this collection has been marked as deferred.
     * False otherwise.
     */
    boolean isDeferred();

    /*
     * Apply deferred updates if there is any. The given
     * collection c will be added to the underlying collection
     * before the deferred updates are applied.
     */
    void applyDeferredUpdates(Collection c);

    /**
     * Adds object to the Collection without recording
     * the event. Used internaly to initially populate the Collection
     */
    void addInternal(Object o);

    /**
     * Adds objects of the given Collection to this Collection without recording
     * the event. Used internaly to initially populate the Collection
     */
    void addAllInternal(Collection c);

    /**
     * Adds an object to the list without recording changes.
     */
    void addToBaseCollection(Object o);

    /**
     * Removes objects of the given Collection from this Collection without recording
     * the event. Used internaly to remove a collection of elements from this collection.
     */
    void removeAllInternal(Collection c);

    /**
     * Clears Collection without recording
     * the event. Used internaly to clear the Collection
     */
    void clearInternal();


    /**
     * Removes element from the Collection without recording
     * the event. Used internaly to update the Collection
     */
    void removeInternal(Object o);

    /**
     * Returns the Collection of added elements
     *
     * @return Collection of the added elements as java.util.Collection
     */
    Collection getAdded();

    /**
     * Returns the Collection of removed elements
     *
     * @return Collection of the removed elements as java.util.Collection
     */
    Collection getRemoved();

    /**
     * Sets a new owner for the SCO instance that is not owned
     * by any other object.
     *
     * @param owner the new owner
     * @param fieldName as java.lang.String
     * @param elementType the new element type as Class, or null if type
     * is not to be checke.
     * @throws com.sun.jdo.api.persistence.support.JDOUserException if the
     * instance is owned by another owner.
     */
    void setOwner(Object owner, String fieldName, Class elementType);

}
