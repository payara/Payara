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
 * Interface to access SQLStore config information.
 *
 * @author Marina Vatkina
 */ 
public interface ConfigCache {

    /**
     * Get the PersistenceConfig for given pcClass. The config is looked up
     * from a cache. If a config can not be found in cache, a new
     * instance is created and returned.
     *
     * @param pcClass The input pcClass.
     * @return PersistenceConfig for given pcClass.
     */
    PersistenceConfig getPersistenceConfig(Class pcClass);

    /**
     * Gets the Class instance corresponding to given oidType.
     *
     * @param oidType The input oidType.
     * @return The Class instance corresponding to given oidType.
     */
    Class getClassByOidClass(Class oidType);

    /**
     * Sets VersionConsistencyCache field.
     *   
     * @param vcCache the VersionConsistencyCache instance.
     */  
    void setVersionConsistencyCache(VersionConsistencyCache vcCache);
}
