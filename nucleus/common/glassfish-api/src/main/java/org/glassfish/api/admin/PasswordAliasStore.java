/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.api.admin;

import java.util.Iterator;
import java.util.Map;
import org.jvnet.hk2.annotations.Contract;

/**
 * Represents a fully-functional password alias store.
 * <p>
 * If the implementation holds the aliases and passwords in memory, it
 * handles loading and saving the in-memory contents from and to persistent
 * storage, at the discretion of the implementer.  For example, loading would 
 * typically happen when the password alias store implementation is first 
 * instantiated, although an implementation could choose to load lazily on
 * first read.  Saving is at the discretion of the implementer as well, although
 * to maximize reliability the implementation should persist changes as they
 * occur.  The {@link #putAll ) methods can help optimize that.
 * 
 * @author tjquinn
 */
@Contract
public interface PasswordAliasStore {
    
    /**
     * Reports whether the store contains the specified alias.
     * @param alias the alias to check for
     * @return true if the alias appears in the store; false otherwise
     */
    boolean containsKey(String alias);
    
    /**
     * Returns the password associated with the specified alias.
     * @param alias the alias of interest
     * @return the password for that alias, if the store contains the alias; null otherwise
     */
    char[] get(String alias);
    
    /**
     * Reports whether the alias store is empty.
     * @return 
     */
    boolean isEmpty();
    
    /**
     * Returns an Iterator over aliases present in the alias store.
     * @return 
     */
    Iterator<String> keys();
    
    /**
     * Reports the number of aliases present in the store.
     * @return 
     */
    int size();
    
    /**
     * Deletes all password aliases from the store.
     */
    void clear();
    
    /**
     * Insert a new alias with the specified password, or assigns a new 
     * password to an existing alias.
     * @param alias the alias to create or reassign
     * @param password the password to be associated with the alias
     */
    void put(String alias, char[] password);
    
    /**
     * Adds all alias/password pairs from the specified store to this store.
     * @param otherStore the alias store from which to get entries
     */
    void putAll(PasswordAliasStore otherStore);
    
    /**
     * Adds a group of alias/password pairs in a single operation.
     * <p>
     * Callers might prefer to invoke this method once rather than invoking
     * {@link #put ) repeatedly, for example if an implementation persists each change
     * as it is made.
     * 
     * @param settings the alias/password pairs to add
     */
    void putAll(Map<String,char[]> settings);

    /**
     * Removes the specified alias (and the associated password) from the 
     * password alias store.
     * @param alias the alias to be removed
     */
    void remove(String alias);
}
