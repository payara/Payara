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
 * ExtentCollection.java
 *
 * Created on April 6, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore;

import com.sun.jdo.api.persistence.support.JDOUnsupportedOptionException;
import com.sun.jdo.api.persistence.support.JDOUserException;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManager;
import com.sun.jdo.api.persistence.model.Model;
import org.glassfish.persistence.common.I18NHelper;

import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

/**
 *
 * @author  Michael Bouschen
 * @version 0.1
 */
public class ExtentCollection
        implements Collection {
    /**
     * The PersistenceManager getExtent is called from
     */
    protected PersistenceManager pm;

    /**
     * This extent collection reperesents the extent of persistenceCapableClass.
     */
    protected Class persistenceCapableClass;

    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
           ExtentCollection.class);

    /**
     *
     * @param persistenceCapableClass Class of instances
     * @param subclasses whether to include instances of subclasses
     */
    public ExtentCollection(PersistenceManager pm, Class persistenceCapableClass, boolean subclasses) {
        this.pm = pm;
        this.persistenceCapableClass = persistenceCapableClass;

        // check persistenceCapableClass parameter being null
        if (persistenceCapableClass == null)
            throw new JDOUserException(
                    I18NHelper.getMessage(messages, "jdo.extentcollection.constructor.invalidclass", "null"));// NOI18N
        // check persistence-capable
        if (Model.RUNTIME.getMappingClass(persistenceCapableClass.getName(),
                persistenceCapableClass.getClassLoader()) == null)
            throw new JDOUserException(
                    I18NHelper.getMessage(messages, "jdo.extentcollection.constructor.nonpc", // NOI18N
                            persistenceCapableClass.getName()));

        // subclasses == true is not yet supported
        if (subclasses)
            throw new JDOUnsupportedOptionException(
                    I18NHelper.getMessage(messages, "jdo.extentcollection.constructor.subclasses"));// NOI18N
    }

    /**
     *
     */
    public Class getPersistenceCapableClass() {
        return persistenceCapableClass;
    }

    /**
     *
     */
    public int size() {
        throw new JDOUnsupportedOptionException(
                I18NHelper.getMessage(messages, "jdo.extentcollection.methodnotsupported", "size"));// NOI18N
    }

    /**
     *
     */
    public boolean isEmpty() {
        throw new JDOUnsupportedOptionException(
                I18NHelper.getMessage(messages, "jdo.extentcollection.methodnotsupported", "isEmpty"));// NOI18N
    }

    /**
     *
     */
    public boolean contains(Object o) {
        throw new JDOUnsupportedOptionException(
                I18NHelper.getMessage(messages, "jdo.extentcollection.methodnotsupported", "contains"));// NOI18N
    }

    /**
     *
     */
    public Iterator iterator() {
        RetrieveDesc rd = pm.getRetrieveDesc(persistenceCapableClass);
        return ((Collection)pm.retrieve(rd)).iterator();
    }

    /**
     *
     */
    public Object[] toArray() {
        throw new JDOUnsupportedOptionException(
                I18NHelper.getMessage(messages, "jdo.extentcollection.methodnotsupported", "toArray"));// NOI18N
    }

    /**
     *
     */
    public Object[] toArray(Object a[]) {
        throw new JDOUnsupportedOptionException(
                I18NHelper.getMessage(messages, "jdo.extentcollection.methodnotsupported", "toArray"));// NOI18N
    }

    /**
     * Extent collection is unmodifiable => throw UnsupportedOperationException
     */
    public boolean add(Object o) {
        throw new UnsupportedOperationException(
                I18NHelper.getMessage(messages, "jdo.extentcollection.illegalmodification", // NOI18N
                        persistenceCapableClass.getName()));
    }

    /**
     * Extent collection is unmodifiable => throw UnsupportedOperationException
     */
    public boolean remove(Object o) {
        throw new UnsupportedOperationException(
                I18NHelper.getMessage(messages, "jdo.extentcollection.illegalmodification", // NOI18N
                        persistenceCapableClass.getName()));
    }

    /**
     *
     */
    public boolean containsAll(Collection c) {
        throw new JDOUnsupportedOptionException(
                I18NHelper.getMessage(messages, "jdo.extentcollection.methodnotsupported", "containsAll"));// NOI18N
    }

    /**
     * Extent collection is unmodifiable => throw UnsupportedOperationException
     */
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException(
                I18NHelper.getMessage(messages, "jdo.extentcollection.illegalmodification", // NOI18N
                        persistenceCapableClass.getName()));
    }

    /**
     * Extent collection is unmodifiable => throw UnsupportedOperationException
     */
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException(
                I18NHelper.getMessage(messages, "jdo.extentcollection.illegalmodification", // NOI18N
                        persistenceCapableClass.getName()));
    }

    /**
     * Extent collection is unmodifiable => throw UnsupportedOperationException
     */
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException(
                I18NHelper.getMessage(messages, "jdo.extentcollection.illegalmodification", // NOI18N
                        persistenceCapableClass.getName()));
    }

    /**
     * Extent collection is unmodifiable => throw UnsupportedOperationException
     */
    public void clear() {
        throw new UnsupportedOperationException(
                I18NHelper.getMessage(messages, "jdo.extentcollection.illegalmodification", // NOI18N
                        persistenceCapableClass.getName()));
    }

    /**
     * Two extent collections are equal, iff the names of their persistence capable class are equal
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof ExtentCollection) {
            String otherClassName = ((ExtentCollection) o).persistenceCapableClass.getName();
            return persistenceCapableClass.getName().equals(otherClassName);
        }
        return false;
    }

    /**
     * The hashCode is mapped to the hashCode of the name of the extent collection's persistence capable class
     */
    public int hashCode() {
        return persistenceCapableClass.getName().hashCode();
    }
}
