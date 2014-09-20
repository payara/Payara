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

import java.util.Map;
import java.util.List;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import java.io.PrintWriter;

import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaDataProperties.JDOClass;
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaDataProperties.JDOField;

import com.sun.jdo.api.persistence.enhancer.util.Support;

/**
 * Provides the JDO meta information based on properties.
 */
//@olsen: new class
public class JDOMetaDataPropertyImpl extends Support
    implements ExtendedJDOMetaData
{
    // misc
//    static final String nl = System.getProperty("line.separator", "\n");
//    protected final PrintWriter out;

    // model
    private static final HashSet transientTypePrefixes = new HashSet();
    private static final HashSet secondClassObjectTypes = new HashSet();
    private static final HashSet mutableSecondClassObjectTypes = new HashSet();


    /**
     *
     */
    private final JDOMetaDataProperties properties;


    /**********************************************************************
     *
     *********************************************************************/

    static
    {
        transientTypePrefixes.add("java/");//NOI18N
        transientTypePrefixes.add("javax/");//NOI18N
        transientTypePrefixes.add("com/sun/jdo/");//NOI18N

        mutableSecondClassObjectTypes.add("java/util/Date");//NOI18N
        mutableSecondClassObjectTypes.add("com/sun/jdo/spi/persistence/support/sqlstore/sco/Date");//NOI18N
        mutableSecondClassObjectTypes.add("java/sql/Date");//NOI18N
        mutableSecondClassObjectTypes.add("com/sun/jdo/spi/persistence/support/sqlstore/sco/SqlTime");//NOI18N
        mutableSecondClassObjectTypes.add("java/sql/Time");//NOI18N
        mutableSecondClassObjectTypes.add("com/sun/jdo/spi/persistence/support/sqlstore/sco/SqlDate");//NOI18N
        mutableSecondClassObjectTypes.add("java/sql/Timestamp");//NOI18N
        mutableSecondClassObjectTypes.add("com/sun/jdo/spi/persistence/support/sqlstore/sco/SqlTimestamp");//NOI18N
        mutableSecondClassObjectTypes.add("java/util/Collection");//NOI18N
        mutableSecondClassObjectTypes.add("java/util/Set");//NOI18N
        mutableSecondClassObjectTypes.add("java/util/List");//NOI18N
        mutableSecondClassObjectTypes.add("java/util/HashSet");//NOI18N
        mutableSecondClassObjectTypes.add("java/util/Vector");//NOI18N
        mutableSecondClassObjectTypes.add("java/util/ArrayList");//NOI18N

        secondClassObjectTypes.add("java/lang/Boolean");//NOI18N
        secondClassObjectTypes.add("java/lang/Byte");//NOI18N
        secondClassObjectTypes.add("java/lang/Short");//NOI18N
        secondClassObjectTypes.add("java/lang/Integer");//NOI18N
        secondClassObjectTypes.add("java/lang/Long");//NOI18N
        secondClassObjectTypes.add("java/lang/Float");//NOI18N
        secondClassObjectTypes.add("java/lang/Double");//NOI18N
        secondClassObjectTypes.add("java/lang/Number");//NOI18N
        secondClassObjectTypes.add("java/lang/Character");//NOI18N
        secondClassObjectTypes.add("java/lang/String");//NOI18N
        secondClassObjectTypes.add("java/math/BigInteger");//NOI18N
        secondClassObjectTypes.add("java/math/BigDecimal");//NOI18N
        secondClassObjectTypes.addAll(mutableSecondClassObjectTypes);

    }  //JDOMetaDataPropertyImpl.<static>


    /**
     * Creates an instance.
     * //@lars: out id not used anymore
     */
    public JDOMetaDataPropertyImpl(Properties properties,
                                   PrintWriter out)
       throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        // check arguments
        if (properties == null) {
            final String msg
                = "Initializing meta data: properties == null";//NOI18N
            throw new JDOMetaDataFatalError(msg);
        }
        /*
        if (out == null) {
            final String msg
                = "Initializing meta data: output stream == null";//NOI18N
            throw new JDOMetaDataFatalError(msg);
        }
        */

        this.properties = new JDOMetaDataProperties (properties);
        readProperties ();
        //this.out = out;
    }

    /**
     * Creates an instance.
     * //@lars: out id not used anymore
     */
    public JDOMetaDataPropertyImpl(Properties properties)
       throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        this (properties, null);
    }

    /**
     * Tests whether a class is known to be persistence-capable.
     */
    public boolean isPersistenceCapableClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        //check the transient prefixes
        for (Iterator i = transientTypePrefixes.iterator(); i.hasNext();) {
            final String typePrefix = (String)i.next();
            if (classPath.startsWith(typePrefix))
                return false;
        }
        JDOClass clazz = getJDOClass (classPath);
        return (clazz != null  ?  clazz.isPersistent ()  :  false);
    }

    /**********************************************************************
     *
     *********************************************************************/

    public boolean isTransientClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        return ! isPersistenceCapableClass (classPath);
    }

    /**
     * Tests whether a class is known as a persistence-capable root class.
     */
    public boolean isPersistenceCapableRootClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        //@lars
        if  ( ! isPersistenceCapableClass (classPath))
        {
            return false;
        }
        String superclass = getSuperClass (classPath);
        return (superclass != null  ?  ! isPersistenceCapableClass (superclass)  :  true);

        //^olsen: exchange dummy implementation
//        return isPersistenceCapableClass(classPath);
    }

    /**
     * Returns the name of the persistence-capable root class of a class.
     */
    public String getPersistenceCapableRootClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        //^olsen: exchange dummy implementation
        return (isPersistenceCapableClass(classPath) ? classPath : null);
    }

    /**
     *  Returns the superclass of a class.
     */

    public final String getSuperClass (String classname)
    {

        JDOClass clazz = getJDOClass (classname);
        return (clazz != null  ?  clazz.getSuperClassName ()  :  null);

    }  //JDOMetaDataPropertyImpl.getSuperClass()


    /**
     * Tests whether a type is known for Second Class Objects.
     */
    public boolean isSecondClassObjectType(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        return secondClassObjectTypes.contains(classPath);
    }

    /**
     * Tests whether a type is known for Mutable Second Class Objects.
     */
    public boolean isMutableSecondClassObjectType(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        return mutableSecondClassObjectTypes.contains(classPath);
    }

    /**
     * Tests whether a field of a class is known to be persistent.
     */
    public boolean isPersistentField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        JDOField field = getJDOField (classPath, fieldName);
        return (field != null  ?  field.isPersistent ()  :  false);
    }

    /**
     * Tests whether a field of a class is known to be transactional.
     */
    public boolean isTransactionalField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        JDOField field = getJDOField (classPath, fieldName);
        return (field != null  ?  field.isTransactional ()  :  false);
    }

    /**
     * Tests whether a field of a class is known to be Primary Key.
     */
    public boolean isPrimaryKeyField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        JDOField field = getJDOField (classPath, fieldName);
        return (field != null  ?  field.isPk ()  :  false);
    }

    /**
     * Tests whether a field of a class is known to be part of the
     * Default Fetch Group.
     */
    public boolean isDefaultFetchGroupField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        JDOField field = getJDOField (classPath, fieldName);
        return (field != null  ?  field.isInDefaultFetchGroup ()  :  false);
    }

    /**
     * Returns the unique field index of a declared, persistent field of a
     * class.
     */
    public int getFieldNo(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        JDOClass clazz = getJDOClass (classPath);
        return (clazz != null  ?  clazz.getIndexOfField (fieldName)  :  -1);
    }

    /**
     * Returns an array of field names of all declared, persistent fields
     * of a class.
     */
    public String [] getManagedFields (String classname)
    {

        JDOClass clazz = getJDOClass (classname);
        return (clazz != null  ?  clazz.getManagedFieldNames ()  :  new String [] {});

    }  //JDOMetaDataPropertyImpl.getManagedFields()


    /**********************************************************************
     *  No interface method.
     *********************************************************************/

    public final String [] getKnownClasses ()
    {

        return this.properties.getKnownClassNames ();

    }  //JDOMetaDataPropertyImpl.getKnownClasses()


    /**********************************************************************
     *  Gets all known fields of a class.
     *********************************************************************/

    public final String [] getKnownFields (String classname)
    {

        JDOClass clazz = getJDOClass (classname);
        return (clazz != null  ?  clazz.getFields ()  :  new String [] {});

    }  //JDOMetaDataPropertyImpl.getKnownFields()


    /**********************************************************************
     *  Gets the access modifier of a class.
     *********************************************************************/

    public final int getClassModifiers (String classname)
    {

        JDOClass clazz = getJDOClass (classname);
        return (clazz != null  ?  clazz.getModifiers ()  :  0);

    }  //JDOMetaDataPropertyImpl.getClassModifiers()


    /**********************************************************************
     *  Gets the access modifier of a field.
     *********************************************************************/

    public final int getFieldModifiers (String classname,
                                        String fieldname)
    {

        JDOField field = getJDOField (classname, fieldname);
        return (field != null  ?  field.getModifiers ()  :  0);

    }  //JDOMetaDataPropertyImpl.getFieldModifiers()


    /**********************************************************************
     *
     *********************************************************************/

    public final String getFieldType (String classname,
                                      String fieldname)
    {

        JDOField field = getJDOField (classname, fieldname);
        return (field != null  ?  field.getType ()  :  null);

    }  //JDOMetaDataPropertyImpl.getFieldType()


    /**********************************************************************
     *
     *********************************************************************/

    private final JDOClass getJDOClass (String classname)
                           throws JDOMetaDataUserException
    {

        return this.properties.getJDOClass (classname);

    }  //JDOMetaDataPropertyImpl.getJDOClass()


    /**********************************************************************
     *
     *********************************************************************/

    private final void readProperties ()
    {

        //read all classes
        String [] classnames = this.properties.getKnownClassNames ();
        for  (int i = classnames.length - 1; i >= 0; i--)
        {
            JDOClass clazz = getJDOClass (classnames [i]);  //should be always != null

            //if the class is persistence it cannot be a second class object type
            if  (clazz.isPersistent ()  &&  secondClassObjectTypes.contains (clazz.getName ()))
            {
                throw new JDOMetaDataUserException ("ERROR: Parsing meta data properties: " +
                                                    "The persistent-capable class '" + clazz.getName () +
                                                    "' is second class object type.");
            }
        }

    }  //JDOMetaDataPropertyImpl.readProperties()


    /**********************************************************************
     *
     *********************************************************************/

    private final JDOField getJDOField (String classname,
                                        String fieldname)
    {

        JDOClass clazz = getJDOClass (classname);
        return (clazz != null  ?  clazz.getField (fieldname)  :  null);

    }  //JDOMetaDataPropertyImpl.getJDOField()


    /**********************************************************************
     *
     *********************************************************************/

    public String getKeyClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        final JDOClass clazz = getJDOClass(classPath);
        return (clazz != null ? clazz.getOidClassName() : null);
    }


    /**********************************************************************
     *
     *********************************************************************/

    public boolean isKeyField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        final JDOField field = getJDOField(classPath, fieldName);
        return (field != null ? field.isPk() : false);
    }


    /**********************************************************************
     *
     *********************************************************************/

    public boolean isKnownNonManagedField(String classPath,
                                          String fieldName,
                                          String fieldSig)
    {
        final JDOClass clazz = getJDOClass(classPath);
        if (clazz == null) {
            return true;
        }
        final JDOField field = clazz.getField(fieldName);
        return (field != null ? field.isKnownTransient() : false);
    }


    /**********************************************************************
     *
     *********************************************************************/

    public boolean isManagedField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        return (isPersistentField(classPath, fieldName)
                || isTransactionalField(classPath, fieldName));
    }


    /**********************************************************************
     *
     *********************************************************************/

    public int getFieldFlags(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        if (!isManagedField(classPath, fieldName)) {
            affirm(!isTransactionalField(classPath, fieldName));
            affirm(!isPersistentField(classPath, fieldName));
            affirm(!isKeyField(classPath, fieldName));
            affirm(!isDefaultFetchGroupField(classPath, fieldName));
            return 0;
        }
        //affirm(isManagedField(classPath, fieldName));

        if (isTransactionalField(classPath, fieldName)) {
            affirm(!isPersistentField(classPath, fieldName));
            affirm(!isKeyField(classPath, fieldName));
            // ignore any dfg membership of transactional fields
            //affirm(!isDefaultFetchGroupField(classPath, fieldName));
            return CHECK_WRITE;
        }
        //affirm(!isTransactionalField(classPath, fieldName));
        affirm(isPersistentField(classPath, fieldName));

        if (isKeyField(classPath, fieldName)) {
            // ignore any dfg membership of key fields
            //affirm(!isDefaultFetchGroupField(classPath, fieldName));
            return MEDIATE_WRITE;
        }
        //affirm(!isKeyField(classPath, fieldName));

        if (isDefaultFetchGroupField(classPath, fieldName)) {
            return CHECK_READ | CHECK_WRITE;
        }
        //affirm(!isDefaultFetchGroupField(classPath, fieldName));

        return MEDIATE_READ | MEDIATE_WRITE;
    }


    /**********************************************************************
     *
     *********************************************************************/

    public int[] getFieldFlags(String classPath, String[] fieldNames)
        throws  JDOMetaDataUserException, JDOMetaDataFatalError
    {
        final int n = (fieldNames != null ? fieldNames.length : 0);
        final int[] flags = new int[n];
        for (int i = 0; i < n; i++) {
            flags[i] = getFieldFlags(classPath, fieldNames[i]);
        }
        return flags;
    }


    /**********************************************************************
     *
     *********************************************************************/

    public final String[] getFieldType(String classname,
                                        String[] fieldnames)
    {
        final int n = (fieldnames != null ? fieldnames.length : 0);
        final String[] types = new String[n];
        for (int i = 0; i < n; i++) {
            types[i] = getFieldType(classname, fieldnames[i]);
        }
        return types;
    }


    /**********************************************************************
     *
     *********************************************************************/

    public int[] getFieldNo(String classPath, String[] fieldNames)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        final int n = (fieldNames != null ? fieldNames.length : 0);
        final int[] flags = new int[n];
        for (int i = 0; i < n; i++) {
            flags[i] = getFieldNo(classPath, fieldNames[i]);
        }
        return flags;
    }


    /**********************************************************************
     *
     *********************************************************************/

    public String[] getKeyFields(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        final List keys = new ArrayList();
        final String[] fieldNames = getManagedFields(classPath);
        final int n = fieldNames.length;
        for (int i = 0; i < n; i++) {
            if (isKeyField(classPath, fieldNames[i])) {
                keys.add(fieldNames[i]);
            }
        }
        return (String[])keys.toArray(new String[keys.size()]);
    }


    /**********************************************************************
     *
     *********************************************************************/

    public String getPersistenceCapableSuperClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        for (String clazz = getSuperClass(classPath);
             clazz != null;
             clazz = getSuperClass(clazz))  {
            if (isPersistenceCapableClass(clazz)) {
                return clazz;
            }
        }
        return null;
    }


    /**********************************************************************
     *
     *********************************************************************/

    public String getSuperKeyClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        for (String superClass = getPersistenceCapableSuperClass(classPath);
             superClass != null;
             superClass = getPersistenceCapableSuperClass(superClass)) {
            final String superKeyClass = getKeyClass(superClass);
            if (superKeyClass != null) {
                return superKeyClass;
            }
        }
        return null;
    }


    /**********************************************************************
     *
     *********************************************************************/
/*
    public static void main (String [] argv)
    {

        if  (argv.length != 1)
        {
            System.err.println ("No property file specified.");
            return;
        }
        Properties p = new Properties ();
        try
        {
            java.io.InputStream  in = new java.io.FileInputStream (new java.io.File (argv [0]));
            p.load (in);
            in.close ();
            System.out.println ("PROPERTIES: " + p);
            System.out.println ("############");
            JDOMetaDataProperties props = new JDOMetaDataProperties (p);
        }
        catch (Throwable ex)
        {
            ex.printStackTrace (System.err);
        }

        JDOMetaDataPropertyImpl jdo = new JDOMetaDataPropertyImpl (p, new PrintWriter (System.out));
        String [] classnames = jdo.getKnownClasses ();
        for  (int k = 0; k < classnames.length; k++)
        {
            String classname = classnames [k];
            System.out.println ("CLASSNAME: " + classname);
            System.out.println ("\tpersistent: " + jdo.isPersistenceCapableClass (classname));
            System.out.println ("\tpersistent root: " + jdo.isPersistenceCapableRootClass (classname));
            System.out.println ("\tpersistent root: " + jdo.getPersistenceCapableRootClass (classname));
            String [] fieldnames = jdo.getKnownFields (classname);
            for  (int j = 0; j < fieldnames.length; j++)
            {
                String fieldname = (String) fieldnames [j];
                System.out.println ("FIELDNAME: " + fieldname);
                System.out.println ("\tpersistent field: " + jdo.isPersistentField (classname, fieldname));
                System.out.println ("\tpk field: " + jdo.isPrimaryKeyField (classname, fieldname));
                System.out.println ("\tdfg field: " + jdo.isDefaultFetchGroupField (classname, fieldname));
                System.out.println ("\tnumber: " + jdo.getFieldNo (classname, fieldname));
                String [] names = jdo.getManagedFields (classname);
                final int n = (fieldnames != null  ?  names.length  :  0);
                System.out.println ("managed fields: number: " + n);
                for  (int i = 0; i < n; i++)
                {
                    System.out.println (i + ": " + names [i] +
                                        " number: " + jdo.getFieldNo (classname, names [i]) +
                                        " pk: " + jdo.isPrimaryKeyField (classname, names [i]) +
                                        " dfg: " + jdo.isDefaultFetchGroupField (classname, names [i]));
                }
            }
        }

    }  //JDOMetaDataPropertyImpl.main
*/

}  //JDOMetaDataPropertyImpl


//JDOMetaDataPropertyImpl
