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

package com.sun.jdo.api.persistence.enhancer.meta;


/**
 * Provides the JDO meta information neccessary for byte-code enhancement.
 * <p>
 * <b>Please note: This interface deals with fully qualified names in the
 * JVM notation, that is, with '/' as package separator character&nbsp; (instead
 * of '.').</b>
 * <p>
 * The following convention is used to specify the format of a given name:
 * Something called ...
 * <ul>
 * <li>
 * <i>name</i> represents a non-qualified name (e.g. <code>JDOPersistenceCapableName</code>
 * = "<code>PersistenceCapable</code>")</li>
 * <li>
 * <i>type</i> represents a Java-qualified class name (e.g. <code>JDOPersistenceCapablePath</code>
 * = '<code>com.sun.jdo.spi.persistence.support.sqlstore.PersistenceCapable</code>")</li>
 * <li>
 * <i>path</i> represents a JVM-qualified name (e.g. <code>JDOPersistenceCapablePath</code>
 * = '<code>com/sun/jdo/spi/persistence/support/sqlstore/PersistenceCapable</code>")</li>
 * <li>
 * <i>sig</i> (for <i>signature</i>) represents a JVM-qualified type-signature
 * name (e.g. <code>JDOPersistenceCapableSig</code>
 * = "L<code>com/sun/jdo/spi/persistence/support/sqlstore/PersistenceCapable;</code>")</li>
 * </ul>
 */
//@olsen: new interface
public interface JDOMetaData
{
    String JDOExternalPath = "com/sun/jdo/api/persistence/support/";//NOI18N
    String JDOPath = "com/sun/jdo/spi/persistence/support/sqlstore/";//NOI18N

    String JDOPersistenceCapableName = "PersistenceCapable";//NOI18N
    String JDOPersistenceCapablePath
    = JDOPath + JDOPersistenceCapableName;//NOI18N
    String JDOPersistenceCapableSig
    = "L" + JDOPersistenceCapablePath + ";";//NOI18N
    String JDOPersistenceCapableType
        = JDOPersistenceCapablePath.replace('/', '.');

    static String javaLangCloneablePath = "java/lang/Cloneable";

    String JDOInstanceCallbacksName = "InstanceCallbacks";//NOI18N
    String JDOInstanceCallbacksPath
    = JDOPath + JDOInstanceCallbacksName;//NOI18N
    String JDOInstanceCallbacksSig
    = "L" + JDOInstanceCallbacksPath + ";";//NOI18N
    String JDOInstanceCallbacksType
        = JDOInstanceCallbacksPath.replace('/', '.');

    String JDOSecondClassObjectBaseName = "SCO";//NOI18N
    String JDOSecondClassObjectBasePath
    = JDOPath + JDOSecondClassObjectBaseName;//NOI18N
    String JDOSecondClassObjectBaseSig
    = "L" + JDOSecondClassObjectBasePath + ";";//NOI18N
    String JDOSecondClassObjectBaseType
        = JDOSecondClassObjectBasePath.replace('/', '.');

    String JDOPersistenceManagerName = "PersistenceManager";//NOI18N
    // we use the external, "public" PersistenceManager interface only
    String JDOPersistenceManagerPath
    = JDOExternalPath + JDOPersistenceManagerName;//NOI18N
    String JDOPersistenceManagerSig
    = "L" + JDOPersistenceManagerPath + ";";//NOI18N
    String JDOPersistenceManagerType
        = JDOPersistenceManagerPath.replace('/', '.');

    String JDOStateManagerName = "StateManager";//NOI18N
    String JDOStateManagerPath
    = JDOPath + JDOStateManagerName;//NOI18N
    String JDOStateManagerSig
    = "L" + JDOStateManagerPath + ";";//NOI18N
    String JDOStateManagerType
        = JDOStateManagerPath.replace('/', '.');

    String JDOStateManagerFieldName = "jdoStateManager";//NOI18N
    String JDOStateManagerFieldType = JDOStateManagerType;
    String JDOStateManagerFieldSig = JDOStateManagerSig;

    String JDOFlagsFieldName = "jdoFlags";//NOI18N
    String JDOFlagsFieldType = "byte";//NOI18N
    String JDOFlagsFieldSig = "B";//NOI18N

    /**
     * Tests whether a class is known to be transient.
     * <P>
     * The following invariant holds:
     *   isTransientClass(classPath)
     *       => !isPersistenceCapableClass(classPath)
     * @param classPath the JVM-qualified name of the class
     * @return true if this class is known to be transient; otherwise false
     */
    boolean isTransientClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    /**
     * Tests whether a class is known to be persistence-capable.
     * <P>
     * The following invariant holds:
     *   isPersistenceCapableClass(classPath)
     *       => !isTransientClass(classPath)
     *          && !isSecondClassObjectType(classPath)
     * @param classPath the JVM-qualified name of the class
     * @return true if this class is persistence-capable; otherwise false
     */
    boolean isPersistenceCapableClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    /**
     * Tests whether a class is known as a persistence-capable root class.
     * <P>
     * @param classPath the JVM-qualified name of the class
     * @return true if this class is persistence-capable and does not
     *         derive from another persistence-capable class; otherwise false
     */
    boolean isPersistenceCapableRootClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    /**
     * Returns the name of the persistence-capable root class of a class.
     * <P>
     * @param classPath the JVM-qualified name of the class
     * @return the name of the least-derived persistence-capable class that
     *         is equal to or a super class of the argument class; if the
     *         argument class is not persistence-capable, null is returned.
     */
    String getPersistenceCapableRootClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    /**
     * Returns the name of the superclass of a class.
     * <P>
     * @param classPath the JVM-qualified name of the class
     * @return the name of the superclass.
     */
    String getSuperClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    /**
     * Tests whether a class is known as type for Second Class Objects.
     * <P>
     * The following invariant holds:
     *   isSecondClassObjectType(classPath)
     *       => !isPersistenceCapableClass(classPath)
     * @param classPath the JVM-qualified name of the type
     * @return true if this type is known for second class objects;
     *         otherwise false
     */
    boolean isSecondClassObjectType(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    /**
     * Tests whether a class is known as type for Mutable Second Class Objects.
     * <P>
     * @param classPath the JVM-qualified name of the type
     * @return true if this type is known for mutable second class objects;
     *         otherwise false
     */
    boolean isMutableSecondClassObjectType(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    /**
     * Tests whether a field of a class is known to be persistent.
     * <P>
     * @param classPath the JVM-qualified name of the class
     * @param fieldName the name of the field
     * @return true if this field is known to be persistent; otherwise false
     */
    boolean isPersistentField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    /**
     * Tests whether a field of a class is known to be transactional.
     * <P>
     * @param classPath the JVM-qualified name of the class
     * @param fieldName the name of the field
     * @return true if this field is known to be transactional; otherwise false
     */
    boolean isTransactionalField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    /**
     * Tests whether a field of a class is known to be Primary Key.
     * <P>
     * @param classPath the JVM-qualified name of the class
     * @param fieldName the name of the field
     * @return true if this field is known to be primary key; otherwise false
     */
    boolean isPrimaryKeyField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    /**
     * Tests whether a field of a class is known to be part of the
     * Default Fetch Group.
     * <P>
     * @param classPath the JVM-qualified name of the class
     * @param fieldName the name of the field
     * @return true if this field is known to be part of the
     *         default fetch group; otherwise false
     */
    boolean isDefaultFetchGroupField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    /**
     * Returns the unique field index of a declared, persistent field of a
     * class.
     * <P>
     * @param classPath the JVM-qualified name of the class
     * @param fieldName the name of the field
     * @return the non-negative, unique field index
     */
    int getFieldNo(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;

    /**
     * Returns an array of field names of all declared persistent and
     * transactional fields of a class.
     * <P>
     * The position of the field names in the result array corresponds
     * to their unique field index as returned by getFieldNo such that
     * these equations holds:
     * <P> getFieldNo(getManagedFields(classPath)[i]) == i
     * <P> getManagedFields(classPath)[getFieldNo(fieldName)] == fieldName
     * <P>
     * @param classPath the JVM-qualified name of the class
     * @return an array of all declared persistent and transactional
     *         fields of a class
     */
    String[] getManagedFields(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError;
}
