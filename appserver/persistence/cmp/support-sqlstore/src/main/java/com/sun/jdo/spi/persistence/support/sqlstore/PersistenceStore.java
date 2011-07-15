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
 * PersistenceStore.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore;


import java.util.Collection;

/**
 * <P>This interface represents a Persistence store
 * that knows how to create, find, modify and delete persistence
 * capable objects from a backing store such as a database.
 */
public interface PersistenceStore {

    /**
     */
    public void execute(PersistenceManager pm, Collection actions);

    /**
     */
    public void executeBatch(PersistenceManager pm, UpdateObjectDesc request, boolean forceFlush);

    /**
     */
    public Object retrieve(PersistenceManager pm,
                               RetrieveDesc action,
                               ValueFetcher parameters);

    /**
     */
    public Class getClassByOidClass(Class oidType);

    /**
     */
    public StateManager getStateManager(Class classType);

    /**
     * Returns a new retrieve descriptor for an external (user) query.
     *
     * @param classType Type of the persistence capable class to be queried.
     * @return A new retrieve descriptor for an external (user) query.
     */
    public RetrieveDesc getRetrieveDesc(Class classType);

    /**
     * Returns a new retrieve descriptor for an external (user) query.
     * This retrieve descriptor can be used to query for the foreign
     * field <code>name</code>.
     *
     * @param fieldName Name of the foreign field to be queried.
     * @param classType Persistence capable class including <code>fieldName</code>.
     * @return A new retrieve descriptor for an external (user) query.
     */
    public RetrieveDesc getRetrieveDesc(String fieldName, Class classType);

    /**
     */
    public UpdateObjectDesc getUpdateObjectDesc(Class classType);


    /**
     */
    public PersistenceConfig getPersistenceConfig(
            Class classType);

    /**
     * Returns ConfigCache associated with this store.
     *
     * @return ConfigCache associated with this store.
     */
    public ConfigCache getConfigCache();
}
