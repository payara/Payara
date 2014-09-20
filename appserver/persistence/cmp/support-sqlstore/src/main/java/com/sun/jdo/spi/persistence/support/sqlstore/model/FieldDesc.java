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
 * FieldDesc.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.model;


import com.sun.jdo.api.persistence.support.JDOUserException;
import com.sun.jdo.api.persistence.support.JDOFatalUserException;
import com.sun.jdo.spi.persistence.support.sqlstore.*;
import com.sun.jdo.spi.persistence.support.sqlstore.sco.SqlTimestamp;
import com.sun.jdo.spi.persistence.utility.FieldTypeEnumeration;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.netbeans.modules.dbschema.ColumnElement;
import org.glassfish.persistence.common.I18NHelper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;

/**
 *
 */
public abstract class FieldDesc implements java.io.Serializable {

    public static final int GROUP_DEFAULT = 1;

    public static final int GROUP_NONE = 0;

    /** This field is used for concurrency check */
    public static final int PROP_IN_CONCURRENCY_CHECK = 0x1;

    /** Update before image when this field is updated. This property is always set */
    public static final int PROP_LOG_ON_UPDATE = 0x2;

    /** This field is read only */
    public static final int PROP_READ_ONLY = 0x4;

    /** Record updates on this field to DB. This property is always set for primitive fields. */
    public static final int PROP_RECORD_ON_UPDATE = 0x8;

    /** Relationship updates are processed from the side containing this field. */
    public static final int PROP_REF_INTEGRITY_UPDATES = 0x10;

    /**
     * This field is the primary tracked field.
     * <P>
     * Primitive fields track each other if they are mapped to same columns.
     * One of them is made the primary tracked field as per precedence rules
     * on the field types. This field is used to bind values to columns while
     * updating the database.
     * <P>
     * RESOLVE:
     * Investigate the runtime behaviour for relationship fields marked primary.
     *
     * @see #PROP_SECONDARY_TRACKED_FIELD
     * @see ClassDesc#computeTrackedPrimitiveFields
     * @see ClassDesc#computeTrackedRelationshipFields
     */
    public static final int PROP_PRIMARY_TRACKED_FIELD = 0x20;

    /**
     * This field is a secondary tracked field.
     * <P>
     * Seconday tracked primitive fields are not used for updates to the database
     * or for concurrency checking.
     * <P>
     * RESOLVE:
     * Investigate the runtime behaviour for secondary tracked relationship fields.
     *
     * @see #PROP_PRIMARY_TRACKED_FIELD
     * @see ClassDesc#computeTrackedPrimitiveFields
     * @see ClassDesc#computeTrackedRelationshipFields
     */
    public static final int PROP_SECONDARY_TRACKED_FIELD = 0x40;

    /**
     * This field tracks a relationship field.
     * Only primitive fields can have this property set.
     */
    public static final int PROP_TRACK_RELATIONSHIP_FIELD = 0x80;

    /** This field is part of a primary key. */
    public static final int PROP_PRIMARY_KEY_FIELD = 0x100;

    /** This field is used for version consistency validation. */
    public static final int PROP_VERSION_FIELD = 0x200;

    /** This field is part of a foreign key. */
    public static final int PROP_FOREIGN_KEY_FIELD = 0x400;

    private static final BigDecimal LONG_MIN_VALUE =
            new BigDecimal(String.valueOf(Long.MIN_VALUE));

    private static final BigDecimal LONG_MAX_VALUE =
            new BigDecimal(String.valueOf(Long.MAX_VALUE));

    public int absoluteID;

    public int fetchGroup;

    public int concurrencyGroup;

    public int sqlProperties;

    private Field field;

    private Class fieldType;

    /**
     * Contains a translation from the field type class to int constants.
     * These constants can be used in switch statements on the field type.
     */
    private int enumFieldType;

    private String fieldName;

    private Class declaringClass;

    // Should be moved to ForeignFieldDesc.
    private Class componentType;

    private ArrayList trackedFields;

    /** Back pointer to declaring class descriptor. */
    protected final ClassDesc classDesc;

    /** The logger. */
    protected final static Logger logger = LogHelperSQLStore.getLogger();

    /** I18N message handler. */
    protected final static ResourceBundle messages = I18NHelper.loadBundle(
            "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
            FieldDesc.class.getClassLoader());

    FieldDesc(ClassDesc classDesc) {
        // Set the default properties
        sqlProperties |= FieldDesc.PROP_REF_INTEGRITY_UPDATES;
        sqlProperties |= FieldDesc.PROP_IN_CONCURRENCY_CHECK;
        sqlProperties |= FieldDesc.PROP_LOG_ON_UPDATE;

        // For Boston, by default, a field is in group -1 which means that
        // it is in its own group.
        concurrencyGroup = -1;

        this.classDesc = classDesc;
    }

    public String toString() {
        return classDesc.getName() + "." + getName(); // NOI18N
    }

    public Class getComponentType() {
        return componentType;
    }

    public Class getDeclaringClass() {
        return declaringClass;
    }

    public Class getType() {
        if (fieldType != null) return fieldType;

        return Object.class;
    }

    public int getEnumType() {
        return enumFieldType;
    }

    public String getName() {
        if (fieldName == null)
            fieldName = "hidden" + -absoluteID; // NOI18N

        return fieldName;
    }

    public Object getValue(StateManager sm) {
        if (sm == null) return null;

        if (absoluteID >= 0) {
            return ((PersistenceCapable) sm.getPersistent()).jdoGetField(absoluteID);
        } else {
            return sm.getHiddenValue(absoluteID);
        }
    }

    public void setValue(StateManager sm, Object value) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (sm == null) return;

        if (absoluteID >= 0) {
            if (debug) {
                if ((value != null) &&
                        ((value instanceof com.sun.jdo.api.persistence.support.PersistenceCapable) ||
                        (value instanceof java.util.Collection))) {
                    Object[] items = new Object[] {field.getName(),value.getClass()};
                    logger.finest("sqlstore.model.fielddesc.fieldname",items); // NOI18N
                } else {
                    Object[] items = new Object[] {field.getName(),value};
                    logger.finest("sqlstore.model.fielddesc.fieldname",items); // NOI18N
                }
            }

            // If the given value is null, and the field type is primitive
            // scalar, we need to convert it to some default value. The following
            // table shows the mapping:
            // primitive number types (int, long, etc) --> 0
            // boolean                                 --> false
            // char                                    --> '\0'
            if (value == null) {
                switch (enumFieldType) {
                    case FieldTypeEnumeration.BOOLEAN_PRIMITIVE:
                        value = new Boolean(false);
                        break;
                    case FieldTypeEnumeration.CHARACTER_PRIMITIVE:
                        value = new Character('\0');
                        break;
                    case FieldTypeEnumeration.BYTE_PRIMITIVE:
                    case FieldTypeEnumeration.SHORT_PRIMITIVE:
                    case FieldTypeEnumeration.INTEGER_PRIMITIVE:
                    case FieldTypeEnumeration.LONG_PRIMITIVE:
                    case FieldTypeEnumeration.FLOAT_PRIMITIVE:
                    case FieldTypeEnumeration.DOUBLE_PRIMITIVE:
                        // Replace value type if necessary
                        value = convertValue(new Integer(0), sm);
                        break;
                }
            }

            // We've tried our best to convert the value to the proper type.
            // (convertValue() was called outside this class if necessary)
            // Here is where we actually set the value. Anything that didn't
            // get converted will get a ClassCastException.
            ((PersistenceCapable) sm.getPersistent()).jdoSetField(absoluteID, value);
        } else {
            if (debug) {
                Object[] items = new Object[] {getName(),value};
                logger.finest("sqlstore.model.fielddesc.fieldname",items); // NOI18N
            }

            sm.setHiddenValue(absoluteID, value);
        }
    }

    public ArrayList getTrackedFields() {
        return trackedFields;
    }

    /**
     * Returns true if this field is a primary key field.
     */
    public boolean isKeyField() {
        return ((sqlProperties & PROP_PRIMARY_KEY_FIELD) > 0);
    }

    /**
     * Returns true if this field is a foreign key field.
     */
    public boolean isForeignKeyField() {
        return ((sqlProperties & PROP_FOREIGN_KEY_FIELD) > 0);
    }

    /**
     * Returns true if this field is a relationship field.
     */
    public boolean isRelationshipField() {
        return false;
    }

    public Object convertValue(Object value, StateManager sm) {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (value == null) {
            if (debug)
                logger.finest("sqlstore.model.fielddesc.convertvalue"); // NOI18N
            return value;
        }

        if (absoluteID < 0) {
            // Hidden field nothing to convert
            if (debug)
                logger.finest("sqlstore.model.fielddesc.convertvalue.hidden",new Integer(absoluteID)); // NOI18N
            return value;
        }

        if (debug) {
            Object[] items = new Object[] {value,value.getClass().getName(),fieldType};
            logger.finest("sqlstore.model.fielddesc.convertvalue.from_to",items); // NOI18N
        }

        // Here, we'll try our best to convert the given value to the
        // proper type before setting the value using reflection.
        if (value instanceof Number) {
            Number number = (Number) value;

            switch (enumFieldType) {
                case FieldTypeEnumeration.BOOLEAN_PRIMITIVE:
                case FieldTypeEnumeration.BOOLEAN:
                    // Well, boolean in java is not really a number,
                    // but we'll try to convert it anyway. The algorithm is
                    // if the number is 0, set the field to false, otherwise
                    // set it to true.
                    // The easiest way to do this is to convert the number to
                    // a double before comparing it to 0.
                    if (number.doubleValue() == 0) {
                        value = new Boolean(false);
                    } else {
                        value = new Boolean(true);
                    }
                    break;
                case FieldTypeEnumeration.BYTE_PRIMITIVE:
                case FieldTypeEnumeration.BYTE:
                    if (!(value instanceof Byte)) {
                        assertIsValid(number, Byte.MIN_VALUE, Byte.MAX_VALUE);
                        value = new Byte(number.byteValue());
                    }
                    break;
                case FieldTypeEnumeration.SHORT_PRIMITIVE:
                case FieldTypeEnumeration.SHORT:
                    if (!(value instanceof Short)) {
                        assertIsValid(number, Short.MIN_VALUE, Short.MAX_VALUE);
                        value = new Short(number.shortValue());
                    }
                    break;
                case FieldTypeEnumeration.INTEGER_PRIMITIVE:
                case FieldTypeEnumeration.INTEGER:
                    if (!(value instanceof Integer)) {
                        assertIsValid(number, Integer.MIN_VALUE, Integer.MAX_VALUE);
                        value = new Integer(number.intValue());
                    }
                    break;
                case FieldTypeEnumeration.LONG_PRIMITIVE:
                case FieldTypeEnumeration.LONG:
                    if (!(value instanceof Long)) {
                        assertIsValidLong(number);
                        value = new Long(number.longValue());
                    }
                    break;
                case FieldTypeEnumeration.FLOAT_PRIMITIVE:
                case FieldTypeEnumeration.FLOAT:
                    if (!(value instanceof Float))
                        value = new Float(number.floatValue());
                    break;
                case FieldTypeEnumeration.DOUBLE_PRIMITIVE:
                case FieldTypeEnumeration.DOUBLE:
                    if (!(value instanceof Double)) {
                        // FIX FOR CONVERT FROM FLOAT.
                        if (value instanceof Float)
                            value = new Double(number.toString());
                        else
                            value = new Double(number.doubleValue());
                    }
                    break;
                case FieldTypeEnumeration.BIGDECIMAL:
                    if (!(value instanceof BigDecimal)) {
                        if (value instanceof Double)
                            value = new BigDecimal(number.doubleValue());
                        else if (value instanceof BigInteger)
                            value = new BigDecimal((java.math.BigInteger) value);
                        else
                            value = new BigDecimal(number.toString());
                    }
                    break;
                case FieldTypeEnumeration.BIGINTEGER:
                    if (!(value instanceof BigInteger)) {
                        if (value instanceof BigDecimal)
                            value = ((BigDecimal) value).toBigInteger();
                        else
                            value = new BigInteger(number.toString());
                    }
                    break;
            }
        } else {

            switch (enumFieldType) {
                case FieldTypeEnumeration.STRING:
                    // If the value is not a String, we take the string representation
                    // of the value.
                    if (!(value instanceof String)) {
                        value = value.toString();
                    }
                    break;

                case FieldTypeEnumeration.UTIL_DATE:
                case FieldTypeEnumeration.SQL_DATE:
                case FieldTypeEnumeration.SQL_TIME:
                case FieldTypeEnumeration.SQL_TIMESTAMP:
                    value = convertToDateFieldType(value, sm);
                    break;

                case FieldTypeEnumeration.CHARACTER_PRIMITIVE:
                case FieldTypeEnumeration.CHARACTER:
                    // If the value is not a character, we take the first character
                    // of the string representation.
                    if (!(value instanceof Character)) {
                        String str = value.toString();
                        value = getCharFromString(str);
                    }
                    break;

                case FieldTypeEnumeration.BOOLEAN_PRIMITIVE:
                case FieldTypeEnumeration.BOOLEAN:
                    // If the value is not a boolean, we construct a boolean
                    // using its string value.
                    if (!(value instanceof Boolean)) {
                        if ((value instanceof String) &&
                                (value).equals("1")) // NOI18N
                            value = "true"; // NOI18N

                        value = new Boolean(value.toString());
                    }
                    break;
            }
        }

        return value;
    }

    /**
     * Creates a new SCO instance. Therefore checks if PersistenceManager
     * settings require SCO creation. SCOs will typically be created
     * in a non managed environment.
     *
     * @param value Value being converted.
     * @param sm StateManager of the persistent object being bound.
     * @return New SCO instance according to <code>enumFieldType</code>
     * of this field. Returns null if no SCO was created.
     */
    public Object createSCO(Object value, StateManager sm) {
        Object retVal = null;

        PersistenceManager pm = null;
        if (sm != null) {
            pm = (PersistenceManager) sm.getPersistenceManagerInternal();
        }

        if (pm != null && pm.getRequireTrackedSCO()) {
            // Create a SCO instance and set values.
            retVal = pm.newSCOInstanceInternal(fieldType,
                    sm.getPersistent(),
                    getName());

            initializeSCO(retVal, value);
        }

        return retVal;
    }

    public static Character getCharFromString(String str) {
        Character retVal = null;
        if (str.length() == 0) {
            retVal = new Character('\0');
        } else {
            retVal = new Character(str.charAt(0));
        }
        return retVal;
    }

    /**
     * Converts <code>value</code> to the type given by <code>enumFieldType</code>.
     * Creates a new SCO instance if the PersistenceManager settings require SCO
     * creation.
     *
     * @param value Value being converted.
     * @param sm StateManager associated to the persistent object being bound.
     * @return Converted object. If no convertion is nessessary the original
     * value is returned.
     */
    private Object convertToDateFieldType(Object value, StateManager sm) {
        if (value instanceof SCO) {
            ((SCO) value).unsetOwner();
        }

        Object retVal = createSCO(value, sm);

        if (retVal == null) {
            // We didn't create a SCO. This might be because we're running
            // in a managed environment where we don't need to manage mutable
            // objects as SCO. Simply convert value to the field type for
            // this field.
            retVal = convertToDateFieldType(value);
        }

        return retVal;
    }

    /**
     * Returns an unmanaged Date instance. Converts <code>value</code>
     * to the field type for this field according to <code>enumFieldType</code>.
     *
     * @param value Value being converted.
     * @return Date instance according to <code>enumFieldType</code> of this field.
     * If <code>value</code> is already of the correct type, it's instantly
     * returned. Otherwise a new Date instance of the correct type is created.
     */
    private Object convertToDateFieldType(Object value) {
        Object retVal = value;

        if (!fieldType.equals(value.getClass())) {
            // Convert to the new fieldType only.

            if (value instanceof Date) {
                switch (enumFieldType) {
                    case FieldTypeEnumeration.UTIL_DATE:
                        retVal = new java.util.Date(((Date) value).getTime());
                        break;
                    case FieldTypeEnumeration.SQL_DATE:
                        retVal = new java.sql.Date(((Date) value).getTime());
                        break;
                    case FieldTypeEnumeration.SQL_TIME:
                        retVal = new java.sql.Time(((Date) value).getTime());
                        break;
                    case FieldTypeEnumeration.SQL_TIMESTAMP:
                        retVal = new java.sql.Timestamp(((Date) value).getTime());

                        // Adjust nano second information for Timestamps.
                        // NOTE: The constructor java.sql.Timestamp(long) calculates nano
                        // seconds. If the old and new instance both represent Timestamps,
                        // the calculation truncates nano second information from the
                        // original object, see java.sql.Timestamp#getTime().
                        if (value instanceof java.sql.Timestamp) {
                            // Overwrite nano second information with the original value.
                            ((java.sql.Timestamp) retVal).setNanos(((java.sql.Timestamp) value).getNanos());
                        }
                        break;
                    default:
                }
            }
        }

        return retVal;
    }

    /**
     * Initializes the SCO instance <code>scoVal</code> corresponding
     * to <code>value</code>. Currently only used for dates.
     *
     * @param scoVal SCO instance being populated.
     * @param value Instance used for initialisation.
     */
    private void initializeSCO(Object scoVal, Object value) {
        switch(enumFieldType) {
            case FieldTypeEnumeration.UTIL_DATE:
            case FieldTypeEnumeration.SQL_DATE:
            case FieldTypeEnumeration.SQL_TIME:
            case FieldTypeEnumeration.SQL_TIMESTAMP:
                // Initializing SCODate
                if (value instanceof Date) {
                    // Set milliseconds information
                    ((SCODate) scoVal).setTimeInternal(((Date) value).getTime());

                    // Adjust nano second information for Timestamps.
                    // NOTE: java.sql.Timestamp#setTime(long) recalculates nano
                    // seconds. If old and new instance both represent Timestamps,
                    // the recalculation truncates nano second information from the
                    // original object, see java.sql.Timestamp#getTime().
                    if (enumFieldType == FieldTypeEnumeration.SQL_TIMESTAMP
                            && value instanceof java.sql.Timestamp) {
                        // Overwrite nano second information with original value
                        ((SqlTimestamp) scoVal).setNanosInternal(((java.sql.Timestamp) value).getNanos());
                    }
                }
                break;
            default:
        }
    }

    private void assertIsValid(Number number, double min_value, double max_value) {
        double x = number.doubleValue();
        if (x < min_value)
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "core.fielddesc.minvalue", // NOI18N
                    new Object[]{number, String.valueOf(min_value), fieldType.getName()}));
        if (x > max_value)
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "core.fielddesc.maxvalue", // NOI18N
                    new Object[]{number, String.valueOf(max_value), fieldType.getName()}));
    }

    private void assertIsValidLong(Number number) {
        BigDecimal bd = null;
        if (number instanceof BigDecimal) {
            bd = (BigDecimal) number;
        } else {
            bd = new BigDecimal(number.toString());
        }
        if (bd.compareTo(LONG_MIN_VALUE) < 0) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "core.fielddesc.minvalue", // NOI18N
                    new Object[]{number, String.valueOf(Long.MIN_VALUE),
                        fieldType.getName()}));

        } else if (bd.compareTo(LONG_MAX_VALUE) > 0) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "core.fielddesc.maxvalue", // NOI18N
                    new Object[]{number, String.valueOf(Long.MAX_VALUE),
                        fieldType.getName()}));
        }
    }

    //
    // ------------ Initialisation methods ------------
    //

    void setupDesc(final Class classType, final String name) {

        Field f = (Field) java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction() {
                    public Object run() {
                        try {
                            return classType.getDeclaredField(name);
                        } catch (NoSuchFieldException e) {
                            throw new JDOFatalUserException(I18NHelper.getMessage(messages,
                            "core.configuration.loadfailed.field", // NOI18N
                            name, classType.getName()), e);
                        }
                    }
                });

        setupDesc(f);
    }

    protected void setupDesc(Field f) {
        field = f;
        fieldName = f.getName();
        declaringClass = field.getDeclaringClass();

        fieldType = field.getType();
        enumFieldType = translateToEnumType(fieldType);

        if (logger.isLoggable(Logger.FINEST)) {
            Object[] items= new Object[] {fieldName,fieldType};
            logger.finest("sqlstore.model.fielddesc.setupdesc",items); // NOI18N
        }
    }

    private static int translateToEnumType(Class fldType) {

        int retVal = FieldTypeEnumeration.NOT_ENUMERATED;

        if(fldType == Boolean.TYPE         ) retVal = FieldTypeEnumeration.BOOLEAN_PRIMITIVE;
        else if(fldType == Character.TYPE  ) retVal = FieldTypeEnumeration.CHARACTER_PRIMITIVE;
        else if(fldType == Byte.TYPE       ) retVal = FieldTypeEnumeration.BYTE_PRIMITIVE;
        else if(fldType == Short.TYPE      ) retVal = FieldTypeEnumeration.SHORT_PRIMITIVE;
        else if(fldType == Integer.TYPE    ) retVal = FieldTypeEnumeration.INTEGER_PRIMITIVE;
        else if(fldType == Long.TYPE       ) retVal = FieldTypeEnumeration.LONG_PRIMITIVE    ;
        else if(fldType == Float.TYPE      ) retVal = FieldTypeEnumeration.FLOAT_PRIMITIVE;
        else if(fldType == Double.TYPE     ) retVal = FieldTypeEnumeration.DOUBLE_PRIMITIVE;
        else if(fldType == Boolean.class   ) retVal = FieldTypeEnumeration.BOOLEAN;
        else if(fldType == Character.class ) retVal = FieldTypeEnumeration.CHARACTER;
        else if(fldType == Byte.class      ) retVal = FieldTypeEnumeration.BYTE;
        else if(fldType == Short.class     ) retVal = FieldTypeEnumeration.SHORT;
        else if(fldType == Integer.class   ) retVal = FieldTypeEnumeration.INTEGER;
        else if(fldType == Long.class      ) retVal = FieldTypeEnumeration.LONG;
        else if(fldType == Float.class     ) retVal = FieldTypeEnumeration.FLOAT;
        else if(fldType == Double.class    ) retVal = FieldTypeEnumeration.DOUBLE;
        else if(fldType == java.math.BigDecimal.class) retVal = FieldTypeEnumeration.BIGDECIMAL;
        else if(fldType == java.math.BigInteger.class) retVal = FieldTypeEnumeration.BIGINTEGER;
        else if(fldType == String.class              ) retVal = FieldTypeEnumeration.STRING;
        else if(fldType == java.util.Date.class      ) retVal = FieldTypeEnumeration.UTIL_DATE;
        else if(fldType == java.sql.Date.class       ) retVal = FieldTypeEnumeration.SQL_DATE;
        else if(fldType == java.sql.Time.class       ) retVal = FieldTypeEnumeration.SQL_TIME;
        else if(fldType == java.sql.Timestamp.class  ) retVal = FieldTypeEnumeration.SQL_TIMESTAMP;
        else if(fldType.isArray()) {
            if(fldType.getComponentType() == Byte.TYPE) {
                retVal = FieldTypeEnumeration.ARRAY_BYTE_PRIMITIVE;
            }
        }

        return retVal;
    }

    void setComponentType(Class type) {
        this.componentType = type;
    }

    protected void addTrackedField(FieldDesc f) {
        if (trackedFields == null)
            trackedFields = new ArrayList();

        if (logger.isLoggable(Logger.FINEST)) {
            Object[] items = new Object[] {f.getName(),getName()};
            logger.finest("sqlstore.model.fielddesc.addingfield",items); // NOI18N
        }

        trackedFields.add(f);
    }

    abstract void computeTrackedRelationshipFields();

    /**
     * This method compares the column lists between to fields to see if they match.
     * If f1 and f2 are both primitive fields, we do an exact match.
     * If f1 is primitve and f2 is a relationship field, we do an at-least-one match.
     * If both f1 and f2 are relationship fields, we do an exact match.
     * @return <code>true</code> if there is a match, <code>false</code>, otherwise.
     */
    static boolean compareColumns(FieldDesc f1, FieldDesc f2) {
        ArrayList columnList1 = null;
        ArrayList columnList2 = null;
        ArrayList columnList3 = null;
        ArrayList columnList4 = null;
        boolean exactMatch = false;

        // For LocalFieldDesc, we use columnDescs for comparison.
        // Otherwise, we use localColumns for comparison.
        if (f1 instanceof LocalFieldDesc) {
            columnList1 = ((LocalFieldDesc) f1).columnDescs;

            if (f2 instanceof LocalFieldDesc) {
                columnList2 = ((LocalFieldDesc) f2).columnDescs;

                // Not sure yet whether we need to a exact match
                // here yet.
                exactMatch = true;
            } else {
                // We are comparing LocalFieldDesc and ForeignFieldDesc.
                // We do not need an exact match. The relationship must change,
                // if one of the LocalFieldDesc's columns is changed.
                columnList2 = ((ForeignFieldDesc) f2).localColumns;
            }
        } else {
            if (f2 instanceof LocalFieldDesc) {
                return false;
            } else {
                ForeignFieldDesc ff1 = (ForeignFieldDesc) f1;
                ForeignFieldDesc ff2 = (ForeignFieldDesc) f2;

                if (ff1.useJoinTable() && ff2.useJoinTable()) {
                    columnList1 = ff1.assocLocalColumns;
                    columnList2 = ff2.assocLocalColumns;
                    columnList3 = ff1.assocForeignColumns;
                    columnList4 = ff2.assocForeignColumns;
                } else if (!ff1.useJoinTable() && !ff2.useJoinTable()) {
                    columnList1 = ff1.localColumns;
                    columnList2 = ff2.localColumns;
                    columnList3 = ff1.foreignColumns;
                    columnList4 = ff2.foreignColumns;
                } else {
                    return false;
                }

                exactMatch = true;
            }
        }

        boolean found = false;

        for (int k = 0; k < 2; k++) {
            if (k == 1) {
                if (columnList3 != null) {
                    columnList1 = columnList3;
                    columnList2 = columnList4;
                } else {
                    break;
                }
            }

            int size1 = columnList1.size();
            int size2 = columnList2.size();

            if (exactMatch && (size1 != size2)) {
                return false;
            }

            for (int i = 0; i < size1; i++) {
                found = false;

                ColumnElement c1 = (ColumnElement) columnList1.get(i);

                // Find if any column of columnList2 matches with c1.
                for (int j = 0; j < size2; j++) {
                    ColumnElement c2 = (ColumnElement) columnList2.get(j);

                    if (c1.getName().getFullName().equals(c2.getName().getFullName())) {
                        found = true;
                    }
                }

                // If we are doing an exact match, and no match is found,
                // we return false.
                if (exactMatch && !found) {
                    return false;
                }

                // If we are not doing an exact match and a match is found,
                // we return true;
                if (!exactMatch && found) {
                    return true;
                }
            }
        }

        // If found is true, that means we got here because the two column lists match
        // exactly. Otherwise, the two column lists don't match at all.
        return found;
    }

}
