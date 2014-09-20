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

//ExtendedJDOMetaData - Java Source


//***************** package ***********************************************

package com.sun.jdo.api.persistence.enhancer.meta;


//***************** import ************************************************


//#########################################################################
/**
 * Provides extended JDO meta information for byte-code enhancement.
 */
//#########################################################################

public interface ExtendedJDOMetaData
       extends   JDOMetaData
{
    /**
     * The JDO field flags.
     */
    int CHECK_READ    = 0x01;
    int MEDIATE_READ  = 0x02;
    int CHECK_WRITE   = 0x04;
    int MEDIATE_WRITE = 0x08;
    int SERIALIZABLE  = 0x10;

    /**********************************************************************
     *  Gets all known classnames.
     *
     *  @return  All known classnames.
     *********************************************************************/

    public String [] getKnownClasses ()
                     throws JDOMetaDataUserException,
                            JDOMetaDataFatalError;


    /**********************************************************************
     *  Gets all known fieldnames of a class.
     *
     *  @param  classname  The classname.
     *
     *  @return  All known fieldnames.
     *********************************************************************/

    public String [] getKnownFields (String classname)
                     throws JDOMetaDataUserException,
                            JDOMetaDataFatalError;


    /**********************************************************************
     *  Gets the type of a field.
     *
     *  @param  classname  The classname.
     *  @param  fieldname  The fieldname.
     *
     *  @return  The type of the field.
     *********************************************************************/

    public String getFieldType (String classname,
                                String fieldname)
                  throws JDOMetaDataUserException,
                         JDOMetaDataFatalError;


    /**********************************************************************
     *  Gets the modifiers of a class. The return value is a constant of the
     *  <code>java.lang.reflect.Modifier</code> class.
     *
     *  @param  classname  The classname.
     *
     *  @return  The modifiers.
     *
     *  @see  java.lang.reflect.Modifier
     *********************************************************************/

    public int getClassModifiers (String classname)
               throws JDOMetaDataUserException,
                      JDOMetaDataFatalError;


    /**********************************************************************
     *  Gets the modifiers of a field. The return value is a constant of the
     *  <code>java.lang.reflect.Modifier</code> class.
     *
     *  @param  classname  The classname.
     *  @param  fieldname  The fieldname.
     *
     *  @return  The modifiers.
     *
     *  @see  java.lang.reflect.Modifier
     *********************************************************************/

    public int getFieldModifiers (String classname,
                                  String fieldname)
               throws JDOMetaDataUserException,
                      JDOMetaDataFatalError;


    /**********************************************************************
     * Returns the name of the key class of a class.
     * <P>
     * The following holds:
     *   (String s = getKeyClass(classPath)) != null
     *       ==> !isPersistenceCapableClass(s)
     *           && isPersistenceCapableClass(classPath)
     * @param classPath the non-null JVM-qualified name of the class
     * @return the name of the key class or null if there is none
     * @see #isPersistenceCapableClass(String)
     *********************************************************************/

    public String getKeyClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;


    /**********************************************************************
     * Returns whether a field of a class is known to be non-managed.
     * <P>
     * This method differs from isManagedField() in that a field may or
     * may not be managed if its not known as non-managed.
     * The following holds (not vice versa!):
     *   isKnownNonManagedField(classPath, fieldName)
     *       ==> !isManagedField(classPath, fieldName)
     * <P>
     * This method doesn't require the field having been declared by
     * declareField().
     * @param classPath the non-null JVM-qualified name of the class
     * @param fieldName the non-null name of the field
     * @param fieldSig the non-null type signature of the field
     * @return true if this field is known to be non-managed; otherwise false
     * @see #isManagedField(String, String)
     * 
     *********************************************************************/

    public boolean isKnownNonManagedField(String classPath,
                                   String fieldName,
                                   String fieldSig)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;


   /********************************************************************** 
     * Returns whether a field of a class is transient transactional
     * or persistent.
     * <P>
     * A managed field must not be known as non-managed and must be either
     * transient transactional or persistent.  The following holds:
     *   isManagedField(classPath, fieldName)
     *       ==> !isKnownNonManagedField(classPath, fieldName)
     *           && (isPersistentField(classPath, fieldName)
     *               ^ isTransactionalField(classPath, fieldName))
     * <P>
     * This method requires the field having been declared by declareField().
     * @param classPath the non-null JVM-qualified name of the class
     * @param fieldName the non-null name of the field
     * @return true if this field is managed; otherwise false
     * @see #isKnownNonManagedField(String, String, String)
     * @see #isPersistentField(String, String)
     * @see #isPersistenceCapableClass(String)
     *********************************************************************/

    public boolean isManagedField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;


    /**********************************************************************
     * Returns whether a field of a class is key.
     * <P>
     * A key field must be persistent.
     * The following holds:
     *   isKeyField(classPath, fieldName)
     *       ==> isPersistentField(classPath, fieldName)
     *           && !isDefaultFetchGroupField(classPath, fieldName)
     * <P>
     * This method requires the field having been declared by declareField().
     * @param classPath the non-null JVM-qualified name of the class
     * @param fieldName the non-null name of the field
     * @return true if this field is key; otherwise false
     * @see #isPersistentField(String, String)
     * 
     *********************************************************************/

    public boolean isKeyField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;


    /**********************************************************************
     * Returns the field flags for a declared field of a class.
     * <P>
     * The following holds for the field flags:
     *   int f = getFieldFlags(classPath, fieldName);
     *
     *   !isManagedField(classPath, fieldName)
     *       ==> (f & CHECK_READ == 0) && (f & MEDIATE_READ == 0) &&
     *           (f & CHECK_WRITE == 0) && (f & MEDIATE_WRITE == 0)
     *
     *   isTransientField(classPath, fieldName)
     *       ==> (f & CHECK_READ == 0) && (f & MEDIATE_READ == 0) &&
     *           (f & CHECK_WRITE != 0) && (f & MEDIATE_WRITE == 0)
     *
     *   isKeyField(classPath, fieldName)
     *       ==> (f & CHECK_READ == 0) && (f & MEDIATE_READ == 0) &&
     *           (f & CHECK_WRITE == 0) && (f & MEDIATE_WRITE != 0)
     *
     *   isDefaultFetchGroupField(classPath, fieldName)
     *       ==> (f & CHECK_READ != 0) && (f & MEDIATE_READ != 0) &&
     *           (f & CHECK_WRITE == 0) && (f & MEDIATE_WRITE == 0)
     *
     *   isPersistentField(classPath, fieldName)
     *   && isKeyField(classPath, fieldName)
     *   && isDefaultFetchGroupField(classPath, fieldName)
     *       ==> (f & CHECK_READ == 0) && (f & MEDIATE_READ == 0) &&
     *           (f & CHECK_WRITE != 0) && (f & MEDIATE_WRITE != 0)
     * <P>
     * This method requires the field having been declared by declareField().
     * @param classPath the non-null JVM-qualified name of the class
     * @param fieldName the non-null name of the field
     * @return the field flags for this field
     * 
     *********************************************************************/

    public int getFieldFlags(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    
    /**********************************************************************
     * Returns the field flags for some declared, managed fields of a class.
     * <P>
     * This method requires all fields having been declared by declareField().
     * @param classPath the non-null JVM-qualified name of the class
     * @param fieldNames the non-null array of names of the declared fields
     * @return the field flags for the fields
     * 
     *********************************************************************/

    public int[] getFieldFlags(String classPath, String[] fieldNames)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;


    /**********************************************************************
     *  Gets the type of some fields.
     *
     *  @param  classname  The classname.
     *  @param  fieldnames  The fieldnames.
     *  @return  The type of the fields.
     *********************************************************************/

    public String[] getFieldType(String classname,
                          String[] fieldnames)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;


    /**********************************************************************
     * Returns the unique field index of some declared, managed fields of a
     * class.
     * <P>
     * This method requires all fields having been declared by declareField().
     * @param classPath the non-null JVM-qualified name of the class
     * @param fieldNames the non-null array of names of the declared fields
     * @return the non-negative, unique field indices
     * 
     *********************************************************************/

    public int[] getFieldNo(String classPath, String[] fieldNames)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;


    /**********************************************************************
     * Returns an array of field names of all key fields of a class.
     * <P>
     * This method requires all fields having been declared by declareField().
     * @param classPath the non-null JVM-qualified name of the class
     * @return an array of all declared key fields of a class
     * 
     *********************************************************************/

    public String[] getKeyFields(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;


    /**********************************************************************
     * Returns the name of the persistence-capable superclass of a class.
     * <P>
     * The following holds:
     *   (String s = getPersistenceCapableSuperClass(classPath)) != null
     *       ==> isPersistenceCapableClass(classPath)
     *           && !isPersistenceCapableRootClass(classPath)
     * @param classPath the non-null JVM-qualified name of the class
     * @return the name of the PC superclass or null if there is none
     * @see #isPersistenceCapableClass(String)
     * @see #getPersistenceCapableRootClass(String)
     *********************************************************************/

    public String getPersistenceCapableSuperClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;


    /**********************************************************************
     * Returns the name of the key class of the next persistence-capable
     * superclass that defines one.
     * <P>
     * The following holds:
     *   (String s = getSuperKeyClass(classPath)) != null
     *       ==> !isPersistenceCapableClass(s)
     *           && isPersistenceCapableClass(classPath)
     *           && !isPersistenceCapableRootClass(classPath)
     * @param classPath the non-null JVM-qualified name of the class
     * @return the name of the key class or null if there is none
     * @see #getKeyClass(String)
     * @see #getPersistenceCapableSuperClass(String)
     *********************************************************************/

    public String getSuperKeyClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

}  //ExtendedJDOMetaData


//ExtendedJDOMetaData - Java Source End
