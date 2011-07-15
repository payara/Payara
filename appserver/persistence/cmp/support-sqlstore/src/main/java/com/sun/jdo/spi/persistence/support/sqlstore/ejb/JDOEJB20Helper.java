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
 * JDOEJB20Helper.java
 *
 * Created on January 17, 2002
 */

package com.sun.jdo.spi.persistence.support.sqlstore.ejb;

import java.util.Collection;
import java.util.Set;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBContext;

import com.sun.jdo.api.persistence.support.PersistenceManager;

/*
 * This is the helper interface for conversion of persistence-capable instances
 * to and from EJB objects of type EJBLocalObject and Collections of those.
 * It extends generic interface JDOEJB11Helper for all other types of conversions.
 *
 * @author Marina Vatkina
 */
public interface JDOEJB20Helper extends JDOEJB11Helper {

    /**
     * Converts persistence-capable instance to EJBLocalObject.
     * @param pc the persistence-capable instance to be converted as an Object.
     * @param pm the associated instance of PersistenceManager.
     * @return instance of EJBLocalObject.
     */
    EJBLocalObject convertPCToEJBLocalObject (Object pc, PersistenceManager pm);

    /**
     * Converts persistence-capable instance to EJBLocalObject. Returns null if
     * the instance is already removed via cascade-delete operation.
     * @param pc the persistence-capable instance to be converted as an Object.
     * @param pm the associated instance of PersistenceManager.
     * @param context the EJBContext of the calling bean.
     * @return instance of EJBLocalObject.
     */
    EJBLocalObject convertPCToEJBLocalObject (Object pc, PersistenceManager pm,
        EJBContext context);

    /**
     * Converts EJBLocalObject to persistence-capable instance.
     * @param o the EJBLocalObject instance to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @param validate true if the existence of the instance is to be validated.
     * @return persistence-capable instance.
     * @throws IllegalArgumentException if validate is true and instance does 
     * not exist in the database or is deleted.    
     */
    Object convertEJBLocalObjectToPC(EJBLocalObject o, PersistenceManager pm,
        boolean validate);

    /**
     * Converts Collection of persistence-capable instances to a Collection of
     * EJBLocalObjects.
     * @param pcs the Collection of persistence-capable instance to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @return Collection of EJBLocalObjects.
     */
    Collection convertCollectionPCToEJBLocalObject (Collection pcs, PersistenceManager pm);

    /**
     * Converts Collection of persistence-capable instances to a Set of
     * EJBLocalObjects.
     * @param pcs the Collection of persistence-capable instance to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @return Set of EJBLocalObjects.
     */
    Set convertCollectionPCToEJBLocalObjectSet (Collection pcs, PersistenceManager pm);

    /**
     * Converts Collection of EJBLocalObjects to a Collection of
     * persistence-capable instances.
     * @param coll the Collection of EJBLocalObject instances to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @param validate true if the existence of the instances is to be validated.
     * @return Collection of persistence-capable instance.
     * @throws IllegalArgumentException if validate is true and at least one instance does 
     * not exist in the database or is deleted.    
     */
    Collection convertCollectionEJBLocalObjectToPC (Collection coll, PersistenceManager pm,
        boolean validate);

    /**
     * Validates that this instance is of the correct implementation class
     * of a local interface type.
     *
     * @param o the instance to validate.
     * @throws IllegalArgumentException if validation fails.
     */
    void assertInstanceOfLocalInterfaceImpl(Object o);
    
}
