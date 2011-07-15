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
 * JDOEJB11Helper.java
 *
 * Created on January 17, 2002
 */

package com.sun.jdo.spi.persistence.support.sqlstore.ejb;

import java.util.Collection;
import java.util.Set;

import javax.ejb.EJBObject;

import com.sun.jdo.api.persistence.support.PersistenceManager;
import com.sun.jdo.spi.persistence.support.sqlstore.utility.NumericConverter;

/**
 * This is the helper interface for conversion of persistence-capable instances
 * to and from EJB objects of type: EJBObject, PrimaryKey, and Collections of those.
 * This interface is generic for CMP1.1 and CMP2.0.
 *
 * @author Marina Vatkina
 */
public interface JDOEJB11Helper {

    /**
     * Converts persistence-capable instance to EJBObject.
     * @param pc the persistence-capable instance to be converted as an Object.
     * @param pm the associated instance of PersistenceManager.
     * @return instance of EJBObject.
     */
    EJBObject convertPCToEJBObject (Object pc, PersistenceManager pm);

    /**
     * Converts EJBObject to persistence-capable instance.
     * @param o the EJBObject instance to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @param validate true if the existence of the instance is to be validated.
     * @return persistence-capable instance.
     * @throws IllegalArgumentException if validate is true and instance does
     * not exist in the database or is deleted.
     */
    Object convertEJBObjectToPC(EJBObject o, PersistenceManager pm, boolean validate);

    /**
     * Converts Collection of persistence-capable instances to a Collection of
     * EJBObjects.
     * @param pcs the Collection of persistence-capable instance to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @return Collection of EJBObjects.
     */
    Collection convertCollectionPCToEJBObject (Collection pcs, PersistenceManager pm);

    /**
     * Converts Collection of persistence-capable instances to a Set of
     * EJBObjects.
     * @param pcs the Collection of persistence-capable instance to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @return Set of EJBObjects.
     */
    Set convertCollectionPCToEJBObjectSet (Collection pcs, PersistenceManager pm);

    /**
     * Converts Collection of EJBObjects to a Collection of
     * persistence-capable instances.
     * @param coll the Collection of EJBObject instances to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @param validate true if the existence of the instances is to be validated.
     * @return Collection of persistence-capable instance.
     * @throws IllegalArgumentException if validate is true and at least one instance does
     * not exist in the database or is deleted.
     */
    Collection convertCollectionEJBObjectToPC (Collection coll, PersistenceManager pm,
        boolean validate);

    /**
     * Converts persistence-capable instance to an instance of the PrimaryKey Class.
     * @param pc the persistence-capable instance to be converted as an Object.
     * @param pm the associated instance of PersistenceManager.
     * @return instance of the PrimaryKey Class.
     */
    Object convertPCToPrimaryKey (Object pc, PersistenceManager pm);

    /**
     * Converts Collection of persistence-capable instances to a Collection of
     * the PrimaryKey Class instances.
     * @param pcs Collection of the persistence-capable instances.
     * @param pm the associated instance of PersistenceManager.
     * @return Collection of the PrimaryKey Class instances.
     */
    Collection convertCollectionPCToPrimaryKey (Collection pcs, PersistenceManager pm);

   /**
     * Converts Object Id of a persistence-capable instance to an instance of the
     * PrimaryKey Class.
     * @param objectId the Object Id to be converted.
     * @return instance of the PrimaryKey Class.
     */
    Object convertObjectIdToPrimaryKey (Object objectId);

   /**
     * Converts instance of a PrimaryKey Class to an instance of the Object Id of a
     * corresponding persistence-capable Class.
     * @param key the PrimaryKey instance to be converted.
     * @return instance of the Object Id.
     */
    Object convertPrimaryKeyToObjectId (Object key);

   /**
     * Converts Collection of Object Id's of persistence-capable instances to a
     * Collection of of the PrimaryKey instances.
     * @param oids Collection of the Object Id to be converted.
     * @return Collection of of the PrimaryKey Class instances.
     */
    Collection convertCollectionObjectIdToPrimaryKey (Collection oids);

   /**
     * Converts Collection of PrimaryKey instances to a Collection of Object Id's
     * of a corresponding persistence-capable Class.
     * @param key Collection of the PrimaryKey instances to be converted.
     * @return Collection of the Object Id's.
     */
    Collection convertCollectionPrimaryKeyToObjectId (Collection key);

    /**
     * Returns the class object of the corresponding persistence-capable class
     * of the concrete bean class.
     * @return the pc class object
     */
    Class getPCClass ();

    /**
     * Serializes serializableObject into a byte array
     * @param serializableObject Instance of a Serializable Object
     * @return serializableObject serialized into a byte array
     */
    byte[] writeSerializableObjectToByteArray(java.io.Serializable serializableObject);

    /**
     * Constructs a Serializable object from byteArray. It is expected that
     * byteArray was constructed using a previous call to writeSerializableObjectToByteArray
     * @param byteArray Array of byte obtained from a call to writeSerializableObjectToByteArray
     * @return A Serializable object contructed from byteArray
     * @see #writeSerializableObjectToByteArray(Serializable)
     */
    java.io.Serializable readSerializableObjectFromByteArray(byte[] byteArray);


    /**
     * Returns Container object associated with the corresponding concrete bean class.
     * @return a Container object.
     */  
    Object getContainer();

    /**
     * Validates that this instance is of the correct implementation class
     * of a remote interface type.
     *
     * @param o the instance to validate.
     * @throws IllegalArgumentException if validation fails.
     */
    void assertInstanceOfRemoteInterfaceImpl(Object o);

    /**
     * Return NumericConverter for conversion from Number to BigDecimal or
     * BigInteger for this bean type. It is responsible for passing the
     * correct policy value to the NumericConverterFactory.
     * @return NumericConverter for given object policy
     */
    NumericConverter getNumericConverter();
}
