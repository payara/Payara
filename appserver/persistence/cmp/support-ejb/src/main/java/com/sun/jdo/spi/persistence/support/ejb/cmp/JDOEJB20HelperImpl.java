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
 * JDOEJB20HelperImpl.java
 *
 * Created on January 17, 2002
 */

package com.sun.jdo.spi.persistence.support.ejb.cmp;

import java.util.Collection;
import java.util.Set;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBException;
import javax.ejb.EJBContext;

import com.sun.jdo.api.persistence.support.PersistenceManager;
import com.sun.jdo.api.persistence.support.JDOHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.CMPHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.JDOEJB20Helper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;


/*
 * This is an abstract class which is a generic implementation of the
 * JDOEJBHelper interface for conversion of persistence-capable instances
 * to and from EJB objects of type EJBLocalObject and Collections of those.
 * It extends JDOEJB11HelperImpl for conversion of instances of other types.
 *
 * @author Marina Vatkina
 */
abstract public class JDOEJB20HelperImpl extends JDOEJB11HelperImpl
    implements JDOEJB20Helper {

    /**
     * Converts persistence-capable instance to EJBLocalObject.
     * @param pc the persistence-capable instance to be converted as an Object.
     * @param pm the associated instance of PersistenceManager.
     * @return instance of EJBLocalObject.
     */
    public EJBLocalObject convertPCToEJBLocalObject (Object pc, PersistenceManager pm) {
        if (pc == null) return null;
        Object jdoObjectId = pm.getObjectId(pc);
        Object key = convertObjectIdToPrimaryKey(jdoObjectId);
        try {
            return CMPHelper.getEJBLocalObject(key, getContainer());
        } catch (Exception ex) {
            EJBException e = new EJBException(I18NHelper.getMessage(messages,
                        "EXC_ConvertPCToEJBLocalObject", key.toString()), ex);// NOI18N
            logger.throwing("JDOEJB20HelperImpl", "convertPCToEJBLocalObject", e); // NOI18N
            throw e;
        }
    }

    /**
     * Converts persistence-capable instance to EJBLocalObject. Returns null if
     * the instance is already removed via cascade-delete operation.
     * @param pc the persistence-capable instance to be converted as an Object.
     * @param pm the associated instance of PersistenceManager.
     * @param context the EJBContext of the calling bean.
     * @return instance of EJBLocalObject.
     */
    public EJBLocalObject convertPCToEJBLocalObject (Object pc, PersistenceManager pm,
        EJBContext context) {
        if (pc == null) return null;
        Object jdoObjectId = pm.getObjectId(pc);
        Object key = convertObjectIdToPrimaryKey(jdoObjectId);
        try {
            return CMPHelper.getEJBLocalObject(key, getContainer(), context);
        } catch (Exception ex) {
            EJBException e = new EJBException(I18NHelper.getMessage(messages,
                        "EXC_ConvertPCToEJBLocalObjectCtx", key.toString()), ex);// NOI18N
            logger.throwing("JDOEJB20HelperImpl", "convertPCToEJBLocalObjectCtx", e); // NOI18N
            throw e;
        }
    }

    /**
     * Converts EJBLocalObject to persistence-capable instance.
     * @param o the EJBLocalObject instance to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @param validate true if the existence of the instance is to be validated.
     * @return persistence-capable instance.
     * @throws IllegalArgumentException if validate is true and instance does
     * not exist in the database or is deleted.
     */
    public Object convertEJBLocalObjectToPC(EJBLocalObject o, PersistenceManager pm, boolean validate) {
        Object key = null; 
        try {
            key = o.getPrimaryKey();
        } catch (Exception ex) {   
            EJBException e = new EJBException(I18NHelper.getMessage(messages,
                        "EXC_ConvertEJBObjectToPC", o.getClass().getName()), ex);// NOI18N
            logger.throwing("JDOEJB20HelperImpl", "convertEJBLocalObjectToPC", e); // NOI18N
            throw e;
        }
        return convertPrimaryKeyToPC(key, pm, validate);
    }

    /**
     * Converts Collection of persistence-capable instances to a Collection of
     * EJBLocalObjects.
     * @param pcs the Collection of persistence-capable instance to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @return Collection of EJBLocalObjects.
     */
    public Collection convertCollectionPCToEJBLocalObject (Collection pcs, PersistenceManager pm){
        Collection rc = new java.util.ArrayList();
        Object o = null;
        boolean debug = false;

        for (java.util.Iterator it = pcs.iterator(); it.hasNext();) {
            o = convertPCToEJBLocalObject((Object)it.next(), pm);
            if(logger.isLoggable(Logger.FINEST) ) {
                logger.finest(
                    "\n---JDOEJB20HelperImpl.convertCollectionPCToEJBLocalObject() adding: " + o);// NOI18N
            }
            rc.add(o);
        }
        return rc;
    }

    /**
     * Converts Collection of persistence-capable instances to a Set of
     * EJBLocalObjects.
     * @param pcs the Collection of persistence-capable instance to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @return Set of EJBLocalObjects.
     */
    public Set convertCollectionPCToEJBLocalObjectSet (Collection pcs, PersistenceManager pm) {
        java.util.Set rc = new java.util.HashSet();
        Object o = null;

        boolean debug = false;

        for (java.util.Iterator it = pcs.iterator(); it.hasNext();) {
            o = convertPCToEJBLocalObject((Object)it.next(), pm);
            if(logger.isLoggable(Logger.FINEST) ) {
                logger.finest(
                    "\n---JDOEJB20HelperImpl.convertCollectionPCToEJBLocalObjectSet() adding: " + o);// NOI18N
            }
            rc.add(o);
        }
        return rc;
    }

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
    public Collection convertCollectionEJBLocalObjectToPC (Collection coll, PersistenceManager pm,
                                                           boolean validate) {
        Collection rc = new java.util.ArrayList();
        Object o = null;

        boolean debug = false;

        for (java.util.Iterator it = coll.iterator(); it.hasNext();) {
            o = convertEJBLocalObjectToPC((EJBLocalObject)it.next(), pm, validate);
            if(logger.isLoggable(Logger.FINEST) ) {
                logger.finest(
                    "\n---JDOEJB20HelperImpl.convertCollectionEJBLocalObjectToPC() adding: " + o);// NOI18N
            }
            rc.add(o);
        }
        return rc;
    }

    /**
     * Validates that this instance is of the correct implementation class
     * of a local interface type.
     *
     * @param o the instance to validate.
     * @throws IllegalArgumentException if validation fails.
     */
    abstract public void assertInstanceOfLocalInterfaceImpl(Object o);

   /**
     * Validates that this instance is of the correct implementation class
     * of a local interface. 
     * Throws IllegalArgumentException if the passed
     * argument is of a wrong type.
     *
     * @param o the instance to validate.
     * @param beanName as String.
     * @throws IllegalArgumentException if validation fails.
     */
    protected void assertInstanceOfLocalInterfaceImpl(Object o,
        String beanName) {

        // We can't check if null is the correct type or not. So
        // we let it succeed.
        if (o == null)
            return;

        try {
            CMPHelper.assertValidLocalObject(o, getContainer());

        } catch (EJBException ex) {
            String msg = I18NHelper.getMessage(messages, "EXC_WrongLocalInstance", // NOI18N
                new Object[] {o.getClass().getName(), beanName,
                    ex.getMessage()});
            logger.log(Logger.WARNING, msg);
            throw new IllegalArgumentException(msg);
        }
    }

}
