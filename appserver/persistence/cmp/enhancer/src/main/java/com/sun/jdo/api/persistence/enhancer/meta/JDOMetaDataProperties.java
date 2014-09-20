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

//JDOMetaDataProperties - Java Source


//***************** package ***********************************************

package com.sun.jdo.api.persistence.enhancer.meta;


//***************** import ************************************************

import java.lang.reflect.Modifier;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Map;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import java.text.MessageFormat;


//#########################################################################
/**
 *  This class parses properties containing meta data information
 *  about classes. The syntax of the properties is the following:
 *  <ul> <li>
 *  the keys in the properties file are fully qualified classnames or
 *  fully qualified fieldnames
 *  </li> <li>
 *  a fields is separated by a classname with a hash mark ('#')
 *  (e.g. "test.Test#test1")
 *  </li> <li>
 *  all classnames are given in a natural form (e.g. "java.lang.Integer",
 *  "java.lang.Integer[][]", "int", "test.Test$Test1")
 *  </li> <li>
 *  property keys are classnames and fieldnames
 *  (e.g. "test.Test=...", "test.Test#field1=...") <br>
 *  </li> <li>
 *  Classnames can have the following attributes:
 *    <ul> <li>
 *  jdo:{persistent|transactional}
 *    </li> <li>
 *  super: &#60;classname&#62;
 *    </li> <li>
 *  access: {public|protected|package|private}
 *    </li> </ul> <li>
 *  Fieldnames can have the following attributes:
 *    <ul> <li>
 *  type:&#60;type&#62;
 *    </li> <li>
 *  access: {public|protected|package|private}
 *    </li> <li>
 *  jdo:{persistent|transactional|transient}
 *    </li> <li>
 *  annotation:{pk|dfg|mediated}
 *    </li> </ul>
 *  </li> <li>
 *  the names of the attributes can be ommitted: you can say <br>
 *  test.Test1#field1=jdo:persistent,type:java.lang.String,pk,... <br>
 *  or <br>
 *  test.Test1#field1=persistent,java.lang.String,pk,... <br>
 *  or <br>
 *  test.Test1#field1=jdo:persistent,java.lang.String,pk,... <br>
 *  </li> <li>
 *  in order to find fields of a class, a line for the class has to be
 *  specified in the properties: To find the field
 *  <code>test.Test1#field</code>, the keys <code>test.Test1</code> and
 *  <code>test.Test1#Field</code> have to be present.
 *  </li> </ul>
 *  This class is not thread safe.
 */
//#########################################################################

public final class JDOMetaDataProperties
{


    /**
     *  The delimiter of a property key between the class- and fieldname.
     */
    private static final char FIELD_DELIMITER = '#';

    /**
     *  A string of delimiter characters between attributes.
     */
    private static final String PROPERTY_DELIMITERS = " \t,;";

    /**
     *  A delimiter character between attribute name and attribute value
     */
    private static final char PROPERTY_ASSIGNER = ':';


    //attribute names for classes and fields
    private static final String PROPERTY_ACCESS_MODIFIER = "access";
    private static final String PROPERTY_JDO_MODIFIER    = "jdo";
    private static final String PROPERTY_SUPER_CLASSNAME = "super";
    private static final String PROPERTY_OID_CLASSNAME   = "oid";
    private static final String PROPERTY_TYPE            = "type";
    private static final String PROPERTY_ANNOTATION_TYPE = "annotation";

    //values of the access attribute of classes and fields.
    private static final String ACCESS_PRIVATE       = "private";
    private static final String ACCESS_PACKAGE_LOCAL = "package";
    private static final String ACCESS_PROTECTED     = "protected";
    private static final String ACCESS_PUBLIC        = "public";

    //values of the jdo attribute of classes and fields.
    private static final String JDO_TRANSIENT     = "transient";
    private static final String JDO_PERSISTENT    = "persistent";
    private static final String JDO_TRANSACTIONAL = "transactional";

    //values of the annotation type attribute of fields.
    private static final String ANNOTATION_TYPE_PK       = "pk";
    private static final String ANNOTATION_TYPE_DFG      = "dfg";
    private static final String ANNOTATION_TYPE_MEDIATED = "mediated";


    /**
     *  The properties to parse.
     */
    private Properties properties;


    /**
     *  A map of already read class properties. The keys are the
     *  classnames, the values are the appropriate
     *  <code>JDOClass</code>-object.
     */
    private final Map cachedJDOClasses = new HashMap ();


    /**
     *  A constant for the cache indicating that a given classname
     *  if not specified in the properties.
     */
    private static final JDOClass NULL = new JDOClass (null);


    /**
     *  A temporary vector (this is the reason why the implementation is not
     *  thread safe).
     */
    private final List tmpTokens = new ArrayList ();


    /**********************************************************************
     *  Creates a new object with the given properties.
     *
     *  @param  props  The properties.
     *
     *  @see  #properties
     *********************************************************************/

    public JDOMetaDataProperties (Properties props)
    {

        this.properties = props;

    }  //JDOMetaDataProperties.<init>


    /**********************************************************************
     *  Get the information about the class with the given name.
     *
     *  @param  classname  The classname.
     *
     *  @return  The information about the class or <code>null</code> if no
     *           information is given.
     *
     *  @throws  JDOMetaDataUserException  If something went wrong parsing
     *                                     the properties.
     *********************************************************************/

    public final JDOClass getJDOClass (String classname)
                          throws JDOMetaDataUserException
    {

        classname = toCanonicalClassName (classname);
        JDOClass clazz = (JDOClass) this.cachedJDOClasses.get (classname);
        if  (clazz == NULL)  //already searched but not found
        {
            return null;
        }
        if  (clazz != null)
        {
            return clazz;
        }

        //load it from the properties file
        String s = this.properties.getProperty (classname);
        if  (s == null)  //class not defined
        {
            this.cachedJDOClasses.put (classname, NULL);
            return null;
        }

        //the class could be found in the properties
        clazz = parseJDOClass (classname, s);  //parse the class attributes
        parseJDOFields (clazz);  //parse all fields
        validateDependencies (clazz);  //check dependencies
        this.cachedJDOClasses.put (clazz.getName (), clazz);

        return clazz;

    }  //JDOMetaDataProperties.getJDOClass()


    /**********************************************************************
     *  Gets the information about the specified field.
     *
     *  @param  classname  The name of the class.
     *  @param  fieldname  The name of the field of the class.
     *
     *  @return  The information about the field or <code>null</code> if
     *           no information could be found.
     *
     *  @throws  JDOMetaDataUserException  If something went wrong parsing
     *                                     the properties.
     *********************************************************************/

    public final JDOField getJDOField (String fieldname,
                                       String classname)
                          throws JDOMetaDataUserException
    {

        JDOClass clazz = getJDOClass (classname);
        return (clazz != null  ?  clazz.getField (fieldname)  :  null);

    }  //JDOMetaDataProperties.getJDOField()


    /**********************************************************************
     *  Gets all classnames in the properties.
     *
     *  @return  All classnames in the properties.
     *********************************************************************/

    public final String [] getKnownClassNames ()
    {

        Collection classnames = new HashSet ();
        for  (Enumeration names = this.properties.propertyNames (); names.hasMoreElements ();)
        {
            String name = (String) names.nextElement ();
            if  (name.indexOf (FIELD_DELIMITER) < 0)
            {
                classnames.add (fromCanonicalClassName (name));
            }
        }

        return (String []) classnames.toArray (new String [classnames.size ()]);

    }  //JDOMetaDataProperties.getKnownClassNames()


    /**********************************************************************
     *  Converts a classname given in a given VM-similar notation (with slashes)
     *  into a canonical notation (with dots).
     *
     *  @param  The VM-similar notation of the classname.
     *
     *  @return  The canonical classname.
     *
     *  @see  #fromCanonicalClassName
     *********************************************************************/

    private static final String toCanonicalClassName (String classname)
    {

        return classname.replace ('/', '.');

    }  //JDOMetaDataProperties.toCanonicalClassName()


    /**********************************************************************
     *  Converts a classname given in a canonical form (with dots) into
     *  a VM-similar notation (with slashes)
     *
     *  @param  classname  The canonical classname.
     *
     *  @return  The VM-similar classname notation.
     *
     *  @see  #toCanonicalClassName
     *********************************************************************/

    private static final String fromCanonicalClassName (String classname)
    {

        return classname.replace ('.', '/');

    }  //JDOMetaDataProperties.fromCanonicalClassName()


    /**********************************************************************
     *  Parses the attributes-string of a class and puts them into a
     *  <code>JDOClass</code>-object.
     *
     *  @param  classname  The name of the class.
     *  @param  atributes  The attribute-string as specified in the properties.
     *
     *  @return  @return  The create <code>JDOClass</code>-object.
     *
     *  @throws  JDOMetaDataUserException  If something went wrong parsing
     *                                     the attributes.
     *********************************************************************/

    private final JDOClass parseJDOClass (String classname,
                                          String attributes)
                           throws JDOMetaDataUserException
    {

        List props = parseProperties (attributes);

        //check each property
        for  (int i = 0; i < props.size (); i++)
        {
            Property prop = (Property) props.get (i);
            validateClassProperty (prop, classname);
        }

        //check dependencies of all properties
        checkForDuplicateProperties (props, classname);

        //properties are OK - assign them to the JDOClass object
        JDOClass clazz = new JDOClass (classname);
        for  (int i = 0; i < props.size (); i++)
        {
            Property prop = (Property) props.get (i);
            if  (prop.name.equals (PROPERTY_ACCESS_MODIFIER))
            {
                clazz.modifiers = getModifiers (prop.value);
            }
            else if  (prop.name.equals (PROPERTY_JDO_MODIFIER))
            {
                clazz.isPersistent = prop.value.equals (JDO_PERSISTENT);
            }
            else if  (prop.name.equals (PROPERTY_SUPER_CLASSNAME))
            {
                clazz.setSuperClassName (prop.value);
            }
            else if (prop.name.equals(PROPERTY_OID_CLASSNAME)) {
                clazz.setOidClassName(prop.value);
            }

        }

        return clazz;

    }  //JDOMetaDataProperties.parseJDOClass()


    /**********************************************************************
     *  Checks if the given attribute-property of a class is valid.
     *
     *  @param  prop       The attribute-property.
     *  @param  classname  The classname.
     *
     *  @throws  JDOMetaDataUserException  If the validation failed.
     *********************************************************************/

    private static final void validateClassProperty (Property prop,
                                                     String   classname)
                              throws JDOMetaDataUserException
    {

        String value = prop.value;
        if  (prop.name == null)  //try to guess the property name
        {
            //check access modifier
            if  (value.equals (ACCESS_PUBLIC)  ||
                 value.equals (ACCESS_PROTECTED)  ||
                 value.equals (ACCESS_PACKAGE_LOCAL)  ||
                 value.equals (ACCESS_PRIVATE))
            {
                prop.name = PROPERTY_ACCESS_MODIFIER;
            }

            //check persistence
            else if  (value.equals (JDO_PERSISTENT)  ||
                      value.equals (JDO_TRANSIENT))
            {
                prop.name = PROPERTY_JDO_MODIFIER;
            }

            //assume the the given value is the superclassname
            else
            {
                prop.name = PROPERTY_SUPER_CLASSNAME;
            }
        }
        else
        {
            //do we have a valid property name?
            String name = prop.name;
            checkPropertyName (prop.name, new String []
                                              {
                                                PROPERTY_OID_CLASSNAME,
                                                PROPERTY_ACCESS_MODIFIER,
                                                PROPERTY_JDO_MODIFIER,
                                                PROPERTY_SUPER_CLASSNAME
                                              }, classname);

            //do we have a valid property value?
            checkPropertyValue (prop,
                                new String []
                                    {
                                        ACCESS_PUBLIC,
                                        ACCESS_PROTECTED,
                                        ACCESS_PACKAGE_LOCAL,
                                        ACCESS_PRIVATE
                                    },
                                PROPERTY_ACCESS_MODIFIER,
                                classname);
            checkPropertyValue (prop,
                                new String [] { JDO_TRANSIENT, JDO_PERSISTENT },
                                PROPERTY_JDO_MODIFIER,
                                classname);
        }

    }  //JDOMetaDataProperties.validateClassProperty()


    /**********************************************************************
     *  Parses all fields of a given class.
     *
     *  @param  clazz  The representation of the class.
     *
     *  @throws  JDOMetaDataUserException  If something went wrong parsing
     *                                     the properties.
     *********************************************************************/

    private final void parseJDOFields (JDOClass clazz)
                       throws JDOMetaDataUserException
    {

        //search for fields of the class
        for  (Enumeration names = this.properties.propertyNames (); names.hasMoreElements ();)
        {
            String name = (String) names.nextElement ();
            if  (name.startsWith (clazz.getName () + FIELD_DELIMITER))  //field found
            {
                String fieldname = name.substring (name.indexOf (FIELD_DELIMITER) + 1, name.length ());
                validateFieldName (fieldname, clazz.getName ());
                clazz.addField (parseJDOField (this.properties.getProperty (name), fieldname, clazz));
            }
        }
        clazz.sortFields ();

    }  //JDOMetaDataProperties.parseJDOField()


    /**********************************************************************
     *  Parses the attribute-string of a field.
     *
     *  @param  attributes  The attribute-string.
     *  @param  fieldname   The fieldname.
     *  @param  clazz       The class to field belongs to.
     *
     *  @throws  JDOMetaDataUserException  If something went wrong parsing
     *                                     the attributes.
     *********************************************************************/

    private final JDOField parseJDOField (String   attributes,
                                          String   fieldname,
                                          JDOClass clazz)
                           throws JDOMetaDataUserException
    {

        List props = parseProperties (attributes);

        //check each property
        for  (int i = 0; i < props.size (); i++)
        {
            Property prop = (Property) props.get (i);
            validateFieldProperty (prop, fieldname, clazz.getName ());
        }

        //check dependencies of all properties
        checkForDuplicateProperties (props, clazz.getName () + FIELD_DELIMITER + fieldname);

        //properties are OK - assign them to the JDOField object
        JDOField field = new JDOField (fieldname);
        for  (int i = 0; i < props.size (); i++)
        {
            Property prop = (Property) props.get (i);
            if  (prop.name.equals (PROPERTY_ACCESS_MODIFIER))
            {
                field.modifiers = getModifiers (prop.value);
            }
            else if  (prop.name.equals (PROPERTY_JDO_MODIFIER))
            {
                field.jdoModifier = prop.value;
            }
            else if  (prop.name.equals (PROPERTY_TYPE))
            {
                field.setType (prop.value);
            }
            else if  (prop.name.equals (PROPERTY_ANNOTATION_TYPE))
            {
                field.annotationType = prop.value;
            }
        }

        return field;

    }  //JDOMetaDataProperties.parseJDOField()


    /**********************************************************************
     *  Checks if the given attribute-property if valid for a field.
     *
     *  @param  prop       The attribute-property.
     *  @param  fieldname  The fieldname.
     *  @param  classname  The classname.

     *  @throws  JDOMetaDataUserException  If the check fails.
     *********************************************************************/

    private final void validateFieldProperty (Property prop,
                                              String   fieldname,
                                              String   classname)
                       throws JDOMetaDataUserException
    {

        String value = prop.value;
        if  (prop.name == null)  //try to guess the property name
        {
            //check access modifier
            if  (value.equals (ACCESS_PUBLIC)  ||
                 value.equals (ACCESS_PROTECTED)  ||
                 value.equals (ACCESS_PACKAGE_LOCAL)  ||
                 value.equals (ACCESS_PRIVATE))
            {
                prop.name = PROPERTY_ACCESS_MODIFIER;
            }

            //check persistence
            else if  (value.equals (JDO_PERSISTENT)  ||
                      value.equals (JDO_TRANSIENT)  ||
                      value.equals (JDO_TRANSACTIONAL))
            {
                prop.name = PROPERTY_JDO_MODIFIER;
            }

            //annotation type?
            else if  (value.equals (ANNOTATION_TYPE_PK)  ||
                      value.equals (ANNOTATION_TYPE_DFG)  ||
                      value.equals (ANNOTATION_TYPE_MEDIATED))
            {
                prop.name = PROPERTY_ANNOTATION_TYPE;
            }

            else
            {
                //assume the the given value is the type
                prop.name = PROPERTY_TYPE;
            }
        }
        else
        {
            String entry = classname + FIELD_DELIMITER + fieldname;

            //do we have a valid property name?
            checkPropertyName (prop.name,
                               new String []
                                   {
                                       PROPERTY_ACCESS_MODIFIER,
                                       PROPERTY_JDO_MODIFIER,
                                       PROPERTY_TYPE,
                                       PROPERTY_ANNOTATION_TYPE
                                   },
                               entry);

            //do we have a valid property value
            checkPropertyValue (prop,
                                new String []
                                    {
                                        ACCESS_PUBLIC,
                                        ACCESS_PROTECTED,
                                        ACCESS_PACKAGE_LOCAL,
                                        ACCESS_PRIVATE
                                    },
                                PROPERTY_ACCESS_MODIFIER,
                                entry);
            checkPropertyValue (prop,
                                new String []
                                    {
                                        JDO_PERSISTENT,
                                        JDO_TRANSIENT,
                                        JDO_TRANSACTIONAL
                                    },
                                PROPERTY_JDO_MODIFIER,
                                entry);
            checkPropertyValue (prop,
                                new String []
                                    {
                                        ANNOTATION_TYPE_PK,
                                        ANNOTATION_TYPE_DFG,
                                        ANNOTATION_TYPE_MEDIATED
                                    },
                                PROPERTY_ANNOTATION_TYPE,
                                entry);
        }

    }  //JDOMetaDataProperties.validateFieldProperty()


    /**********************************************************************
     *  Validates dependencies between a class and its fields and between.
     *
     *  @param  clazz  The class.
     *
     *  @throws  JDOMetaDataUserException  If the validation fails.
     *********************************************************************/

    private final void validateDependencies (JDOClass clazz)
                       throws JDOMetaDataUserException
    {

        for  (int i = clazz.fields.size () - 1; i >= 0; i--)
        {
            JDOField field = (JDOField) clazz.fields.get (i);

            //set the jdo field modifier according to the jdo class modifier (if jdo field not set yet)
            if  (field.jdoModifier == null)
            {
                field.jdoModifier = (clazz.isPersistent ()  ?  JDO_PERSISTENT  :  JDO_TRANSIENT);
            }
            //if we have a non-persistent class
            else if  (clazz.isTransient ())
            {
                //non-persistent classes cannot have persistent fields
                if  (field.isPersistent ())
                {
                    throw new JDOMetaDataUserException (getErrorMsg (IErrorMessages.ERR_TRANSIENT_CLASS_WITH_PERSISTENT_FIELD,
                                                                     new String [] { clazz.getName (), field.getName () }));
                }
                //non-persistent classes cannot have transactional fields
                if  (field.isTransactional ())
                {
                    throw new JDOMetaDataUserException (getErrorMsg (IErrorMessages.ERR_TRANSIENT_CLASS_WITH_TRANSACTIONAL_FIELD,
                                                                     new String [] { clazz.getName (), field.getName () }));
                }
            }

            //a non-persistent class cannot have an annotated field
            if  (field.isAnnotated ()  &&  clazz.isTransient ())
            {
                throw new JDOMetaDataUserException (getErrorMsg (IErrorMessages.ERR_TRANSIENT_CLASS_WITH_ANNOTATED_FIELD,
                                                                 new String [] { clazz.getName (), field.getName () }));
            }

            //a non-persistent field cannot have an annotation type
            if  ( ! field.isPersistent ()  &&  field.isAnnotated ())
            {
                field.annotationType = ANNOTATION_TYPE_MEDIATED;
            }

            //set the annotation type if not done yet
            if  ( ! field.isAnnotated ()  &&  clazz.isPersistent ())
            {
                field.annotationType = ANNOTATION_TYPE_MEDIATED;
            }
        }

    }  //JDOMetaDataProperties.validateDependencies()


    /**********************************************************************
     *  Checks if a given fieldname is a valid Java identifier.
     *
     *  @param  fieldname  The fieldname.
     *  @param  classname  The corresponding classname.
     *
     *  @throws  JDOMetaDataUserException  If the check fails.
     *********************************************************************/

    private static final void validateFieldName (String fieldname,
                                                 String classname)
                              throws JDOMetaDataUserException
    {

        if  (fieldname.length () == 0)
        {
            throw new JDOMetaDataUserException (getErrorMsg (IErrorMessages.ERR_EMPTY_FIELDNAME,
                                                             new String [] { classname }));
        }
        if  ( ! Character.isJavaIdentifierStart (fieldname.charAt (0)))
        {
            throw new JDOMetaDataUserException (getErrorMsg (IErrorMessages.ERR_INVALID_FIELDNAME,
                                                             new String [] { classname, fieldname }));
        }
        for  (int i = fieldname.length () - 1; i >= 0; i--)
        {
            final char c = fieldname.charAt (i);
            if  ( ! Character.isJavaIdentifierPart (c))
            {
                throw new JDOMetaDataUserException (getErrorMsg (IErrorMessages.ERR_INVALID_FIELDNAME,
                                                                 new String [] { classname, fieldname }));
            }
        }

    }  //JDOMetaDataProperties.checkFieldName()


    /**********************************************************************
     *  Checks if an attribute-property was entered twice for a class or field.
     *
     *  @param  props  The properties.
     *  @param  entry  The class- or fieldname.
     *
     *  @throws  JDOMetaDataUserException  If the check fails.
     *********************************************************************/

    private static final void checkForDuplicateProperties (List   props,
                                                           String entry)
                              throws JDOMetaDataUserException
    {

        for  (int i = 0; i < props.size (); i++)
        {
            for  (int j = i + 1; j < props.size (); j++)
            {
                Property p1 = (Property) props.get (i);
                Property p2 = (Property) props.get (j);
                if  (p1.name.equals (p2.name)  &&  ! p1.value.equals (p2.value))
                {
                    throw new JDOMetaDataUserException (getErrorMsg (IErrorMessages.ERR_DUPLICATE_PROPERTY_NAME,
                                                                     new String [] { entry, p1.name, p1.value, p2.value }));
                }
            }
        }

    }  //JDOMetaDataProperties.checkForDuplicateEntries()


    /**********************************************************************
     *  Checks if an attribute name is recognized by the parser.
     *
     *  @param  name        The name of the attribute.
     *  @param  validnames  A list of valid names (the attribute name has to
     *                      be in this list).
     *  @param  entry       The class- or fieldname.
     *
     *  @throws  JDOMetaDataUserException  If the check fails.
     *********************************************************************/

    private static final void checkPropertyName (String    name,
                                                 String [] validnames,
                                                 String    entry)
                              throws JDOMetaDataUserException
    {

        for  (int i = 0; i < validnames.length; i++)
        {
            if  (name.equals (validnames [i]))
            {
                return;
            }
        }

        throw new JDOMetaDataUserException (getErrorMsg (IErrorMessages.ERR_INVALID_PROPERTY_NAME,
                                                         new String [] { entry, name }));

    }  //JDOMetaDataProperties.checkPropertyName()


    /**********************************************************************
     *  Checks if the given value of an attribute-property is recognized by
     *  by the parser if that value belongs to a given attribute name.
     *
     *  @param  prop         The attribute-property (with name and value).
     *  @param  validvalues  A list of valid values.
     *  @param  name         The name of the attribute-property to check.
     *  @param  entry        The class- or fieldname.
     *
     *  @throws  JDOMetaDataUserException  If the check fails.
     *********************************************************************/

    private static final void checkPropertyValue (Property  prop,
                                                  String [] validvalues,
                                                  String    name,
                                                  String    entry)
                              throws JDOMetaDataUserException
    {

        if  ( ! prop.name.equals (name))
        {
            return;
        }

        for  (int i = 0; i < validvalues.length; i++)
        {
            if  (prop.value.equals (validvalues [i]))
            {
                return;
            }
        }

        throw new JDOMetaDataUserException (getErrorMsg (IErrorMessages.ERR_INVALID_PROPERTY_VALUE,
                                                         new String [] { entry, name, prop.value }));

    }  //JDOMetaDataProperties.checkPropertyValue()


    /**********************************************************************
     *  Formats an error message with the given parameters.
     *
     *  @param  msg     The message with format strings.
     *  @param  params  The params to format the message with.
     *
     *  @return  The formatted error message.
     *********************************************************************/

    static final String getErrorMsg (String    msg,
                                     String [] params)
    {

        return MessageFormat.format (msg, (Object []) params);

    }  //JDOMetaDataProperties.getErrorMsg()


    /**********************************************************************
     *  Parses the attribute-string of a class- or fieldname.
     *
     *  @param  attributes  The attribute-string.
     *
     *  @return  A list of <code>Propert<</code>-objects for the attributes.
     *
     *  @exception  JDOMetaDataUserException  If the parsing fails.
     *********************************************************************/

    final List parseProperties (String attributes)
               throws JDOMetaDataUserException
    {

        this.tmpTokens.clear ();
        StringTokenizer t = new StringTokenizer (attributes, PROPERTY_DELIMITERS);
        while (t.hasMoreTokens ())
        {
            this.tmpTokens.add (parseProperty (t.nextToken ()));
        }

        return this.tmpTokens;

    }  //JDOMetaDataProperties.getTokens()


    /**********************************************************************
     *  Parses the given attribute and splits it into name and value.
     *
     *  @param  attribute  The attribute-string.
     *
     *  @return  The <code>Propert</code>-object.
     *
     *  @exception  JDOMetaDataUserException  If the parsing fails.
     *********************************************************************/

    private final Property parseProperty (String attribute)
                           throws JDOMetaDataUserException
    {

        Property prop = new Property ();
        int idx = attribute.indexOf (PROPERTY_ASSIGNER);
        if (idx < 0)
        {
            prop.value = attribute;
        }
        else
        {
            prop.name = attribute.substring (0, idx);
            prop.value = attribute.substring (idx + 1, attribute.length ());
            if  (prop.name.length () == 0  ||  prop.value.length () == 0)
            {
                throw new JDOMetaDataUserException (getErrorMsg (IErrorMessages.ERR_EMPTY_PROPERTY_NAME_OR_VALUE,
                                                                 new String [] { attribute }));
            }
        }

        return prop;

    }  //JDOMetaDataProperties.parseProperty()


    /**********************************************************************
     *
     *********************************************************************/

    private static final int getModifiers (String modifier)
    {

        if  (modifier.equals (ACCESS_PUBLIC))
        {
            return Modifier.PUBLIC;
        }
        if  (modifier.equals (ACCESS_PRIVATE))
        {
            return Modifier.PRIVATE;
        }
        if  (modifier.equals (ACCESS_PROTECTED))
        {
            return Modifier.PROTECTED;
        }
        return 0;

    }  //JDOMetaDataProperties.getModifiers()


    /**********************************************************************
     *  A simple test to run from the command line.
     *
     *  @param  argv  The command line arguments.
     *********************************************************************/

/*
    public static void main (String [] argv)
    {

        if  (argv.length != 1)
        {
            System.err.println ("Error: no property filename specified");
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
            String [] classnames = props.getKnownClassNames ();
            for  (int i = 0; i < classnames.length; i++)
            {
                String classname = classnames [i];
                System.out.println (classname + ": " + props.getJDOClass (classname));
            }
        }
        catch (Throwable ex)
        {
            ex.printStackTrace (System.err);
        }

    }  //JDOMetaDataProperties.main()
*/


    //#####################################################################
    /**
     *  The holder-class for the name and the value of a property.
     */
    //#####################################################################

    private static final class Property
    {


        /**
         *  The name of the property.
         */
        String name = null;


        /**
         *  The value of the property.
         */
        String value = null;


        /******************************************************************
         *  Creates a string-representation of this object.
         *
         *  @return  The string-representation of this object.
         *****************************************************************/

        public final String toString ()
        {

            return '<' + name + ':' + value + '>';

        }  //Property.toString()


    }  //Property


    //#####################################################################
    /**
     *  Holds all unformatted error messages.
     */
    //#####################################################################

    private static interface IErrorMessages
    {


        //the unformatted error messages
        static final String PREFIX = "Error Parsing meta data properties: ";
        static final String ERR_EMPTY_FIELDNAME =
                            PREFIX + "The class ''{0}'' may not have an empty fieldname.";
        static final String ERR_INVALID_FIELDNAME =
                            PREFIX + "The field name ''{1}'' of class ''{0}'' is not valid.";
        static final String ERR_EMPTY_PROPERTY_NAME_OR_VALUE  =
                            PREFIX + "The property name and value may not be empty if a ''" + PROPERTY_ASSIGNER + "'' is specified: ''{0}''.";
        static final String ERR_INVALID_PROPERTY_NAME =
                            PREFIX + "Invalid property name for entry ''{0}'': ''{1}''.";
        static final String ERR_INVALID_PROPERTY_VALUE =
                            PREFIX + "Invalid value for property ''{1}'' of entry ''{0}'': ''{2}''.";
        static final String ERR_DUPLICATE_PROPERTY_NAME =
                            PREFIX + "The property ''{1}'' for the entry ''{0}'' entered twice with values: ''{2}'' and ''{3}''.";
        static final String ERR_TRANSIENT_CLASS_WITH_PERSISTENT_FIELD =
                            PREFIX + "A non-persistent class cannot have a persistent field (class ''{0}'' with field ''{1})''.";
        static final String ERR_TRANSIENT_CLASS_WITH_TRANSACTIONAL_FIELD =
                            PREFIX + "A non-persistent class cannot have a transactional field (class ''{0}'' with field ''{1})''.";
        static final String ERR_TRANSIENT_CLASS_WITH_ANNOTATED_FIELD =
                            PREFIX + "A non-persistent class cannot have an annotated field (''{1}'' of class ''{0}'') can''t have a fetch group.";
        static final String ERR_NON_PERSISTENT_ANNOTATED_FIELD =
                            PREFIX + "A non-persistent field (''{1}'' of class ''{0}'') can''t be a annotated.";


    }  //IErrorMessages


    //#####################################################################
    /**
     *  A class to hold all parsed attributes of a class.
     */
    //#####################################################################

    static final class JDOClass
    {


        /**
         *  The name of the class.
         */
        private String name;


        /**
         *  The name of the superclass.
         */
        private String superClassName = null;


        /**
         *  The name of the oid class.
         */
        private String oidClassName = null;


        /**
         *  The access modifier.
         */
        private int modifiers = Modifier.PUBLIC;


        /**
         *  Do we have a persistent class?
         */
        private boolean isPersistent = true;


        /**
         *  A list of all parsed fields.
         */
        private final List fields = new ArrayList ();


        /**
         *
         */
        private String [] managedFieldNames = null;


        /**
         *
         */
        private String [] fieldNames = null;


        /******************************************************************
         *  Constructs a new object with the given name.
         *
         *  @param  name  The name of the class.
         *
         *  @see  #name
         *****************************************************************/

        JDOClass (String name)
        {

            this.name = name;

        }  //JDOClass.<init>


        /******************************************************************
         *  Gets the name of the class.
         *
         *  @return  The name of the class.
         *
         *  @see  #name
         *****************************************************************/

        public final String getName ()
        {

            return this.name;

        }  //JDOClass.getName()


        /******************************************************************
         *
         *****************************************************************/

        public final int getModifiers ()
        {

            return this.modifiers;

        }  //JDOClass.getModifiers()


        /******************************************************************
         *  Sets the superclassname. The given classname should have a canonical
         *  form (with dots). It is converted to the CM-similar notation
         *  (with slashes).
         *
         *  @param  classname  The superclassname.
         *
         *  @see  #superClassName
         *****************************************************************/

        private final void setSuperClassName (String classname)
        {

            this.superClassName = fromCanonicalClassName (classname);

        }  //JDOClass.setSuperClassName()


        /******************************************************************
         *  Gets the superclassname.
         *
         *  @return  The superclassname.
         *
         *  @see  #superClassName
         *****************************************************************/

        public final String getSuperClassName ()
        {

            return this.superClassName;

        }  //JDOClass.getSuperClassName()


        /******************************************************************
         *  Sets the superclassname. The given classname should have a canonical
         *  form (with dots). It is converted to the CM-similar notation
         *  (with slashes).
         *
         *  @param  classname  The superclassname.
         *
         *  @see  #superClassName
         *****************************************************************/

        public void setOidClassName(String classname)
        {
            this.oidClassName = fromCanonicalClassName(classname);
        }


        /******************************************************************
         *  Gets the oidClassName.
         *
         *  @return  The oidClassName.
         *
         *  @see  #oidClassName
         *****************************************************************/

        public String getOidClassName()
        {
            return oidClassName;
        }


        /******************************************************************
         *  Do we have a persistent class.
         *
         *  @return  Do we have a persistent class?
         *
         *  @see  #isPersistent
         *****************************************************************/

        public final boolean isPersistent ()
        {

            return this.isPersistent;

        }  //JDOMetaClass.isPersistent()


        /******************************************************************
         *  Do we have a transient class.
         *
         *  @return  Do we have a transient class?
         *
         *  @see  #isPersistent
         *****************************************************************/

        public final boolean isTransient ()
        {

            return ! isPersistent ();

        }  //JDOMetaClass.isTransient()


        /******************************************************************
         *  Adds a new field.
         *
         *  @param  field  The new field.
         *
         *  @see  #fields
         *****************************************************************/

        private final void addField (JDOField field)
        {

            this.fields.add (field);

        }  //JDOClass.addField()


        /******************************************************************
         *  Gets the field with the given name.
         *
         *  @param  name  The name of the requested field.
         *
         *  @return  The field or <code>null</code> if not found.
         *
         *  @see  #fields
         *****************************************************************/

        public final JDOField getField (String name)
        {

            int idx = getIndexOfField (name);
            return (idx > -1  ?  (JDOField) this.fields.get (idx)  :  null);

        }  //JDOField.getField()


        /******************************************************************
         *  Gets the index of the field with the given name.
         *
         *  @param  name  The name of the field.
         *
         *  @return  The index or <code>-1</code> if the field was not found.
         *
         *  @see  #fields
         *****************************************************************/

        public final int getIndexOfField (String name)
        {

            for  (int i = 0; i < this.fields.size (); i++)
            {
                JDOField field = (JDOField) this.fields.get (i);
                if  (field.getName ().equals (name))
                {
                    return i;
                }
            }

            return -1;

        }  //JDOClass.getIndexOfField()


        /******************************************************************
         *  Gets all fields of this class.
         *
         *  @return  The fields.
         *
         *  @see  #fields
         *****************************************************************/

        public final String [] getFields ()
        {

            if  (this.fieldNames == null)
            {
                final int n = this.fields.size ();
                String [] fields = new String [n];
                for  (int i = 0; i < n; i++)
                {
                    fields [i] = ((JDOField) this.fields.get (i)).getName ();
                }
                this.fieldNames = fields;
            }

            return this.fieldNames;

        }  //JDOClass.getFields()


        /******************************************************************
         *  Sorts the fields of this class according to the names. This method
         *  should be called if all fields are added. It is necessary to
         *  establish an order on the fields.
         *
         *  @see  #fields
         *****************************************************************/

        private final void sortFields ()
        {

            Collections.sort (this.fields,
                              new Comparator ()
                                  {
                                      public final int compare (Object f1, Object f2)
                                      {
                                          JDOField field1 = (JDOField) f1;
                                          JDOField field2 = (JDOField) f2;
                                          //if we dont have managed fields we dont care
                                          if  ( ! (field1.isManaged ()  &&  field2.isManaged ()))
                                          {
                                              return (field1.isManaged ()  ?  -1  :  1);
                                          }
                                          return (field1).getName ().compareTo (field2.getName ());
                                      }
                                  });

        }  //JDOClass.sortFields()


        /******************************************************************
         *  Gets a list of persistent field names of this class.
         *
         *  @return  The persistent fieldnames.
         *
         *  @see  #fields
         *****************************************************************/

        public final String [] getManagedFieldNames ()
        {

            if  (this.managedFieldNames == null)
            {
                final int n = this.fields.size ();
                List tmp = new ArrayList (n);
                for  (int i = 0; i < n; i++)
                {
                    JDOField field = (JDOField) this.fields.get (i);
                    if  (field.isManaged ())
                    {
                        tmp.add (field.getName ());
                    }
                }
                this.managedFieldNames = (String []) tmp.toArray (new String [tmp.size ()]);
            }

            return this.managedFieldNames;

        }  //JDOClass.getManagedFieldNames()


        /******************************************************************
         *  Creates a string-representation for this object.
         *
         *  @return  The string-representation of this object.
         *****************************************************************/

        public final String toString ()
        {

            return '<' + PROPERTY_SUPER_CLASSNAME + ':' + this.superClassName + ',' +
                         PROPERTY_ACCESS_MODIFIER + ':' + Modifier.toString (this.modifiers) + ',' +
                         PROPERTY_JDO_MODIFIER +    ':' + this.isPersistent     + ',' +
                         "fields:" + this.fields + '>';

        }  //JDOClass.toString()


    }  //JDOClass


    //#####################################################################
    /**
     *  A class to hold the properties of a field.
     */
    //#####################################################################

    static final class JDOField
    {


        /**
         *  The name of the field.
         */
        private String name;


        /**
         *  The type of the field.
         */
        private String type = null;


        /**
         *  The access modifier of the field.
         */
        private int modifiers = Modifier.PRIVATE;


        /**
         *  The JDO modifier of the field.
         */
        private String jdoModifier = null;


        /**
         *  The annotation type.
         */
        private String annotationType = null;


        /******************************************************************
         *  Creates a new object with the given name.
         *
         *  @param  name  The name of the field.
         *
         *  @see  #name
         *****************************************************************/

        JDOField (String name)
        {

            this.name = name;

        }  //JDOField.<init>


        /******************************************************************
         *  Gets the name of the field.
         *
         *  @return  The name of the field.
         *
         *  @see  #name
         *****************************************************************/

        public final String getName ()
        {

            return this.name;

        }  //JDOField.getName()


        /******************************************************************
         *  Sets the type of the field. The given classname should have a
         *  natural form (with dots) and is converted to a VM-similar
         *  notation (with slashes).
         *
         *  @param  type  The natural classname.
         *
         *  @see  #type
         *****************************************************************/

        public final void setType (String type)
        {

            this.type = fromCanonicalClassName (type);

        }  //JDOField.setType()


        /******************************************************************
         *  Gets the type of the field.
         *
         *  @return  The type of the field.
         *
         *  @see  #type
         *****************************************************************/

        public final String getType ()
        {

            return this.type;

        }  //JDOField.getType()


        /******************************************************************
         *
         *****************************************************************/

        public final int getModifiers ()
        {

            return this.modifiers;

        }  //JDOField.getModifiers()


        /******************************************************************
         *  Do we have an annotated field?
         *
         *  @return  Do we have an annotated field?
         *
         *  @see  #annotationType
         *****************************************************************/

        public final boolean isAnnotated ()
        {

            return this.annotationType != null;

        }  //JDOField.isAnnotated()


        /******************************************************************
         *  Do we have a primary key?
         *
         *  @return  Do we have a primary key?
         *
         *  @see  #annotationType
         *****************************************************************/

        public final boolean isPk ()
        {

            return (this.annotationType != null  &&  this.annotationType.equals (ANNOTATION_TYPE_PK));

        }  //JDOField.isPk()


        /******************************************************************
         *  Is the field in the default fetch group?
         *
         *  @return  Is the field in the default fetch group?
         *
         *  @see  #annotationType
         *****************************************************************/

        public final boolean isInDefaultFetchGroup ()
        {

            return (this.annotationType != null  &&  this.annotationType.equals (ANNOTATION_TYPE_DFG));

        }  //JDOField.isInDefaultFetchGroup()


        /******************************************************************
         * Returns whether the field is declared transient.
         *
         * @return  true if declared transient field.
         * @see  #jdoModifier
         */
        public boolean isKnownTransient()
        {
            return (jdoModifier != null
                && jdoModifier.equals(JDO_TRANSIENT));
        }


        /******************************************************************
         *  Do we have a persistent field.
         *
         *  @return  Do we have a persistent field.
         *
         *  @see  #jdoModifier
         *****************************************************************/

        public final boolean isPersistent ()
        {

            return (this.jdoModifier != null  &&  this.jdoModifier.equals (JDO_PERSISTENT));

        }  //JDOField.isPersistent()


        /******************************************************************
         *  Do we have a transactional field.
         *
         *  @return  So we have a transactional field?
         *
         *  @see  #jdoModifier
         *****************************************************************/

        public final boolean isTransactional ()
        {

            return (this.jdoModifier != null  &&  this.jdoModifier.equals (JDO_TRANSACTIONAL));

        }  //JDOField.isTransactional()


        /******************************************************************
         *  Do we have a managed field?
         *
         *  @return  Do we have a managed field?
         *****************************************************************/

        public final boolean isManaged ()
        {

            return (isPersistent ()  ||  isTransactional ());

        }  //JDOField.isManaged()


        /******************************************************************
         *  Creates a string-representation of the object.
         *
         *  @return  The string-representation of the object.
         *****************************************************************/

        public final String toString ()
        {

            return '<' + "name:" + this.name + ',' +
                         PROPERTY_TYPE + ':' +  this.type + ',' +
                         PROPERTY_ACCESS_MODIFIER + ':' + Modifier.toString (this.modifiers) + ',' +
                         PROPERTY_JDO_MODIFIER +    ':' + this.jdoModifier + ',' +
                         PROPERTY_ANNOTATION_TYPE + ':' + this.annotationType +
                   '>';

        }  //JDOField.toString()


    }  //JDOField


}  //JDOMetaDataProperties
