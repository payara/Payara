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
 * JDOEJB11HelperImpl.java
 *
 * Created on January 17, 2002
 */

package com.sun.jdo.spi.persistence.support.ejb.cmp;

import java.util.Collection;
import java.util.Set;
import java.util.ResourceBundle;
import java.io.*;

import javax.ejb.EJBObject;
import javax.ejb.EJBException;

import com.sun.jdo.api.persistence.support.PersistenceManager;
import com.sun.jdo.api.persistence.support.JDOHelper;
import com.sun.jdo.api.persistence.support.JDOFatalDataStoreException;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.api.persistence.support.JDOObjectNotFoundException;
import com.sun.jdo.api.persistence.support.JDOUserException;

import com.sun.jdo.spi.persistence.support.sqlstore.ejb.JDOEJB11Helper;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.CMPHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.utility.NumericConverter;
import com.sun.jdo.spi.persistence.support.sqlstore.utility.NumericConverterFactory;

import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

/*
 * This is an abstract class which is a generic implementation of the
 * JDOEJB11Helper interface for conversion of persistence-capable instances
 * to and from EJB objects of type: EJBObject, PrimaryKey, and Collections of those.
 * These implementations are common for CMP1.1 and CMP2.0 beans.
 *
 * @author Marina Vatkina
 */
abstract public class JDOEJB11HelperImpl implements JDOEJB11Helper {

    /**
     * I18N message handler
     */
    protected final static ResourceBundle messages = I18NHelper.loadBundle(
        JDOEJB11HelperImpl.class);

    //The logger
    protected static final Logger logger = LogHelperEntityInternal.getLogger();

    /**
     * Converts persistence-capable instance to EJBObject.
     * @param pc the persistence-capable instance to be converted as an Object.
     * @param pm the associated instance of PersistenceManager.
     * @return instance of EJBObject.
     */
    public EJBObject convertPCToEJBObject (Object pc, PersistenceManager pm) {
        if (pc == null) return null;
        Object jdoObjectId = pm.getObjectId(pc);
        Object key = convertObjectIdToPrimaryKey(jdoObjectId);
        try {
            return CMPHelper.getEJBObject(key, getContainer());
        } catch (Exception ex) {
            EJBException e = new EJBException(I18NHelper.getMessage(messages,
                        "EXC_ConvertPCToEJBObject", key.toString()), ex);// NOI18N
            logger.throwing("JDOEJB11HelperImpl", "convertPCToEJBObject", e); // NOI18N
            throw e;
        }
    }

    /**
     * Converts EJBObject to persistence-capable instance.
     * @param o the EJBObject instance to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @param validate true if the existence of the instance is to be validated.
     * @return persistence-capable instance.
     * @throws IllegalArgumentException if validate is true and instance does
     * not exist in the database or is deleted.
     */
    public Object convertEJBObjectToPC(EJBObject o, PersistenceManager pm, boolean validate) {
        Object key = null;
        try {
            key = o.getPrimaryKey();
        } catch (Exception ex) {
            EJBException e = new EJBException(I18NHelper.getMessage(messages,
                        "EXC_ConvertEJBObjectToPC", o.getClass().getName()), ex);// NOI18N
            logger.throwing("JDOEJB11HelperImpl", "convertEJBObjectToPC", e); // NOI18N
            throw e;
        }
        return convertPrimaryKeyToPC(key, pm, validate);
    }

    /**
     * Converts PrimaryKey object to persistence-capable instance.
     * @param key the PrimaryKey object to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @param validate true if the existence of the instance is to be validated.
     * @return persistence-capable instance.
     * @throws IllegalArgumentException if validate is true and instance does
     * not exist in the database or is deleted.
     */
    protected Object convertPrimaryKeyToPC(Object key, PersistenceManager pm, boolean validate) {
        Object pc = null;
        try {
            Object jdoObjectId = convertPrimaryKeyToObjectId(key);
            pc = pm.getObjectById(jdoObjectId, validate);
        } catch (JDOObjectNotFoundException ex) {
            logger.fine("---JDOEJB11HelperImpl.convertPrimaryKeyToPC: Object not found for: " + key); // NOI18N

            throw new IllegalArgumentException(I18NHelper.getMessage(messages,
                        "EXC_DeletedInstanceOtherTx", key.toString()));// NOI18N
        }

        if (validate && JDOHelper.isDeleted(pc)) {
            logger.fine("---JDOEJB11HelperImpl.convertPrimaryKeyToPC: Object is deleted for: " + key); // NOI18N

            throw new IllegalArgumentException(I18NHelper.getMessage(messages,
                        "EXC_DeletedInstanceThisTx", key.toString()));// NOI18N
        }

        return pc;
    }

    /**
     * Converts Collection of persistence-capable instances to a Collection of
     * EJBObjects.
     * @param pcs the Collection of persistence-capable instance to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @return Collection of EJBObjects.
     */
    public Collection convertCollectionPCToEJBObject (Collection pcs, PersistenceManager pm) {
        Collection rc = new java.util.ArrayList();

        Object o = null;
        for (java.util.Iterator it = pcs.iterator(); it.hasNext();) {
            o = convertPCToEJBObject((Object)it.next(), pm);
            if(logger.isLoggable(Logger.FINEST) ) {
                logger.finest(
                    "\n---JDOEJB11HelperImpl.convertCollectionPCToEJBObject() adding: " + o);// NOI18N
            }
            rc.add(o);
        }
        return rc;
    }

    /**
     * Converts Collection of persistence-capable instances to a Set of
     * EJBObjects.
     * @param pcs the Collection of persistence-capable instance to be converted.
     * @param pm the associated instance of PersistenceManager.
     * @return Set of EJBObjects.
     */
    public Set convertCollectionPCToEJBObjectSet (Collection pcs, PersistenceManager pm) {
        java.util.Set rc = new java.util.HashSet();

        Object o = null;
        for (java.util.Iterator it = pcs.iterator(); it.hasNext();) {
            o = convertPCToEJBObject((Object)it.next(), pm);
            if(logger.isLoggable(Logger.FINEST) ) {
                logger.finest(
                    "\n---JDOEJB11HelperImpl.convertCollectionPCToEJBObjectSet() adding: " + o);// NOI18N
            }
            rc.add(o);
        }
        return rc;
    }

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
    public Collection convertCollectionEJBObjectToPC (Collection coll, PersistenceManager pm, 
                                                      boolean validate) {
        Collection rc = new java.util.ArrayList();

        Object o = null;
        for (java.util.Iterator it = coll.iterator(); it.hasNext();) {
            o = convertEJBObjectToPC((EJBObject)it.next(), pm, validate);
            if(logger.isLoggable(Logger.FINEST) ) {
                logger.finest(
                    "\n---JDOEJB11HelperImpl.convertCollectionEJBObjectToPC() adding: " + o);// NOI18N
            }
            rc.add(o);
        }
        return rc;
    }

    /**
     * Converts persistence-capable instance to an instance of the PrimaryKey Class.
     * @param pc the persistence-capable instance to be converted as an Object.
     * @param pm the associated instance of PersistenceManager.
     * @return instance of the PrimaryKey Class.
     */
    public Object convertPCToPrimaryKey (Object pc, PersistenceManager pm) {
        if (pc == null) return null;
        Object rc = convertObjectIdToPrimaryKey(pm.getObjectId(pc));

        if(logger.isLoggable(Logger.FINEST) ) {
            logger.finest("\n---JDOEJB11HelperImpl.convertPCToPrimaryKey() PK: " + rc);// NOI18N
        }
        return rc;
    }


    /**
     * Converts Collection of persistence-capable instances to a Collection of
     * the PrimaryKey Class instances.
     * @param pcs Collection of the persistence-capable instances.
     * @param pm the associated instance of PersistenceManager.
     * @return Collection of the PrimaryKey Class instances.
     */
    public Collection convertCollectionPCToPrimaryKey (Collection pcs, PersistenceManager pm) {
        Collection rc = new java.util.ArrayList();

        Object o = null;
        for (java.util.Iterator it = pcs.iterator(); it.hasNext();) {
            o = convertPCToPrimaryKey(it.next(), pm);
            if(logger.isLoggable(Logger.FINEST) ) {
                logger.finest(
                    "\n---JDOEJB11HelperImpl.convertCollectionPCToPrimaryKey() adding: " + o);// NOI18N
            }
            rc.add(o);
        }
        return rc;
    }

   /**
     * Converts Object Id of a persistence-capable instance to an instance of the
     * PrimaryKey Class.
     * @param objectId the Object Id to be converted.
     * @return instance of the PrimaryKey Class.
     */
    abstract public Object convertObjectIdToPrimaryKey (Object objectId);

   /**
     * Converts instance of a PrimaryKey Class to an instance of the Object Id of a
     * corresponding persistence-capable Class.
     * @param key the PrimaryKey instance to be converted.
     * @return instance of the Object Id.
     */
    abstract public Object convertPrimaryKeyToObjectId (Object key);

   /**
     * Converts Collection of Object Id's of persistence-capable instances to a
     * Collection of of the PrimaryKey instances.
     * @param oids Collection of the Object Id to be converted.
     * @return Collection of of the PrimaryKey Class instances.
     */
    public Collection convertCollectionObjectIdToPrimaryKey (Collection oids) {
        Collection rc = new java.util.ArrayList();

        Object o = null;
        for (java.util.Iterator it = oids.iterator(); it.hasNext();) {
            o = convertObjectIdToPrimaryKey(it.next());
            if(logger.isLoggable(Logger.FINEST) ) {
                logger.finest(
                    "\n---JDOEJB11HelperImpl.convertCollectionObjectIdToPrimaryKey() adding: " + o);// NOI18N
            }
            rc.add(o);
        }
        return rc;
    }

   /**
     * Converts Collection of PrimaryKey instances to a Collection of Object Id's
     * of a corresponding persistence-capable Class.
     * @param keys Collection of the PrimaryKey instances to be converted.
     * @return Collection of the Object Id's.
     */
    public Collection convertCollectionPrimaryKeyToObjectId (Collection keys) {
        Collection rc = new java.util.ArrayList();

        Object o = null;
        for (java.util.Iterator it = keys.iterator(); it.hasNext();) {
            o = convertPrimaryKeyToObjectId(it.next());
            if(logger.isLoggable(Logger.FINEST) ) {
                logger.finest(
                    "\n---JDOEJB11HelperImpl.convertCollectionPrimaryKeyToObjectId() adding: " + o);// NOI18N
            }
            rc.add(o);
        }
        return rc;
    }

    /**
     * Serializes serializableObject into a byte array
     * @param serializableObject Instance of a Serializable Object
     * @return serializableObject serialized into a byte array
     */
    public byte[] writeSerializableObjectToByteArray(Serializable serializableObject)
    {
        byte[] byteArray = null;
        if(serializableObject != null)
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;
            try
            {
                oos = new ObjectOutputStream(bos);
                oos.writeObject(serializableObject);
                byteArray = bos.toByteArray();
            }
            catch(java.io.IOException e)
            {
                String clsName = serializableObject.getClass().getName();
                throw new JDOUserException(I18NHelper.getMessage(messages,
                        "EXC_IOWriteSerializableObject", clsName), e);// NOI18N
            }
        }
        return byteArray;
    }

    /**
     * Constructs a Serializable object from byteArray. It is expected that
     * byteArray was constructed using a previous call to writeSerializableObjectToByteArray
     * @param byteArray Array of byte obtained from a call to writeSerializableObjectToByteArray
     * @return A Serializable object contructed from byteArray
     * @see #writeSerializableObjectToByteArray(Serializable)
     */
    public Serializable readSerializableObjectFromByteArray(byte[] byteArray)
    {
        Serializable serializableObject = null;
        if(byteArray != null)
        {
            ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
            HelperObjectInputStream ois = null;

            //
            // Take the current class loader to resolve the class to be deserialized.
            //
            ClassLoader cl = this.getClass().getClassLoader();
            try
            {
                ois = new HelperObjectInputStream(bis, cl);
                serializableObject = (Serializable) ois.readObject();
            }
            catch (ClassNotFoundException e)
            {
                throw new JDOFatalDataStoreException(I18NHelper.getMessage(messages,
                        "EXC_CNFReadSerializableObject"), e);// NOI18N
            }
            catch(java.io.IOException e)
            {
                throw new JDOFatalDataStoreException(I18NHelper.getMessage(messages,
                        "EXC_IOReadSerializableObject"), e);// NOI18N
            }
        }
        return serializableObject;
    }

    /**
     * Return NumericConverter for conversion from Number to BigDecimal or
     * BigInteger for this bean type. It is responsible for passing the
     * correct policy value to the NumericConverterFactory.
     * @return NumericConverter for given object policy
     */
    public NumericConverter getNumericConverter() {
        int policy = CMPHelper.getNumericConverterPolicy(getContainer());
        return NumericConverterFactory.getNumericConverter(policy);
    }

    /**
     * Returns the class object of the corresponding persistence-capable class
     * of the concrete bean class.
     * @return the pc class object
     */
    abstract public Class getPCClass ();

    /**
     * Validates that this instance is of the correct implementation class
     * of a remote interface type.
     *
     * @param o the instance to validate.
     * @throws IllegalArgumentException if validation fails.
     */
    abstract public void assertInstanceOfRemoteInterfaceImpl(Object o);

   /**
     * Validates that this instance is of the correct implementation class
     * of bean remote interface. 
     * Throws IllegalArgumentException if the argument is of a wrong type.
     *
     * @param o the instance to validate.
     * @param beanName as String.
     * @throws IllegalArgumentException if validation fails.
     */
    protected void assertInstanceOfRemoteInterfaceImpl(Object o, 
        String beanName) {

        // We can't check if null is the correct type or not. So
        // we let it succeed.
        if (o == null)
            return;

        try {
            CMPHelper.assertValidRemoteObject(o, getContainer());

        } catch (EJBException ex) {
            String msg = I18NHelper.getMessage(messages, "EXC_WrongRemoteInstance", // NOI18N
                new Object[] {o.getClass().getName(), beanName, 
                    ex.getMessage()});
            logger.log(Logger.WARNING, msg);
            throw new IllegalArgumentException(msg); 
        }
    }

   /**
     * Validates that the primary key instance is not null.
     * Throws IllegalArgumentException otherwise.
     * @param pk the primary key instance to validate.
     * @throws IllegalArgumentException if validation fails.
     */  
    protected void assertPrimaryKeyNotNull(Object pk) {
        if (pk == null) {
            throw new IllegalArgumentException(I18NHelper.getMessage(
                messages, "EXC_pknull_exception")); // NOI18N
        }
    } 

   /**
     * Validates that the primary key field of an Object type  is not null.
     * Throws IllegalArgumentException otherwise.
     * @param pkfield the primary key field instance to validate.
     * @param pkfieldName the primary key field name.
     * @param beanName the EJB name.
     * @throws IllegalArgumentException if validation fails.
     */
    public void assertPrimaryKeyFieldNotNull(Object pkfield, String pkfieldName,
        String beanName) {

        if (pkfield == null) {
            throw new IllegalArgumentException(I18NHelper.getMessage(
                messages, "EXC_pkfieldnull_exception", // NOI18N
                pkfieldName, beanName));
        }
    }


   /**   
     * Validates that the object id instance is not null.  
     * Throws JDOFatalInternalException otherwise. 
     * @param oid the object id instance to validate. 
     * @throws JDOFatalInternalException if validation fails. 
     */   
    protected void assertObjectIdNotNull(Object oid) { 
        if (oid == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages, "EXC_oidnull_exception")); // NOI18N
        }
    }

    /** Helper class that allows to use specified class loader to resolve
      * class name.
      */
     static class HelperObjectInputStream extends ObjectInputStream {

         java.lang.ClassLoader classLoader;

         /** Creates new HelperObjectInputStream */
         public HelperObjectInputStream(InputStream is, ClassLoader cl)
          throws IOException, StreamCorruptedException {
             super(is);
             classLoader = cl;
         }

         /** Overrides the same method of the base class */
         protected Class resolveClass(ObjectStreamClass v)
              throws IOException, ClassNotFoundException {
              return Class.forName(v.getName(), true, classLoader);
         }

     }
}
