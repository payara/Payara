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

package com.sun.jdo.spi.persistence.support.sqlstore;

/**
 * A cache of "version consistent" StateManager instances.  These instances
 * are used so that we can avoid loading state from the database.
 *
 * @author Dave Bristor
 */
public interface VersionConsistencyCache {
    /**
     * Puts the given StateManager into a map that is keyed by the given OID.
     * We anticipate that implementations will want to use a two-level map,
     * and so the pc's class can be used as a key into a map to access a
     * second map, which would be that keyed by OID.
     * @param pcType class of instance, used as key in outer map.
     * @param oid Object id, used as key in inner map.
     * @param sm StateManager bound to <code>oid</code> in inner map.
     */
    public StateManager put(Class pcType, Object oid, StateManager sm);

    /**
     * Returns an SM, if found, else null.
     * @param pcType class of instance, used as key in outer map.
     * @param oid Object id, used as key in inner map.
     */
    public StateManager get(Class pcType, Object oid);

    /**
     * Removes entry based on pc and oid.  If map is empty after remove,
     * removes it from its containint map.
     * @param pcType class of instance, used as key in outer map.
     * @param oid Object id, used as key in inner map.
     */
    public StateManager remove(Class pcType, Object oid);

    /**
     * Informs the cache to expect that the given pcType will be used as a key
     * for the outer map in subsequent <code>putEntry</code> operations.
     * @param pcType class of instance, used as key in outer map.
     */
    public void addPCType(Class pcType);

    /**
     * Removes the map for the given pcType and all its elements.
     * @param pcType class of instance, used as key in outer map.
     */
    public void removePCType(Class pcType);
}
