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
 * LocalFieldDesc.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.model;

import org.netbeans.modules.dbschema.ColumnElement;
import com.sun.jdo.spi.persistence.utility.FieldTypeEnumeration;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import com.sun.jdo.spi.persistence.support.sqlstore.StateManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.lang.reflect.Field;
import java.sql.Types;
import java.math.BigInteger;
import java.math.BigDecimal;


/**
 *
 */
public class LocalFieldDesc extends FieldDesc {

    /** Array of ColumnElement. */
    final ArrayList columnDescs;

    /** Stores this special mapping information. */
    private Boolean primitiveMappedToNullableColumn;

    /** SQL Column type for primary column. */
    private final int primaryColumnType;

    LocalFieldDesc(ClassDesc config, ArrayList columnDescs) {
        super(config);
        this.columnDescs = columnDescs;

        //Initialize primary column's type.
        primaryColumnType = getPrimaryColumn().getType();

        sqlProperties |= FieldDesc.PROP_RECORD_ON_UPDATE;
    }

    public boolean isPrimitiveMappedToNullableColumn() {
        if (primitiveMappedToNullableColumn == null) {
            boolean rc = getType().isPrimitive();

            for (Iterator iter = columnDescs.iterator(); iter.hasNext() && rc; ) {
                ColumnElement c = (ColumnElement) iter.next();
                rc = c.isNullable();
            }
            primitiveMappedToNullableColumn = new Boolean(rc);
        }

        return primitiveMappedToNullableColumn.booleanValue();
    }

    /**
     * Determines if this field is mapped to a LOB column type. It is assumed that
     * lob fields are mapped to only one columns and UI and model verifications enforce it.
     * @return <code>true</code> if field is mapped to LOB column type. <code>false</code>
     * 			otherwise.
     */
    public boolean isMappedToLob() {
        return
            primaryColumnType == Types.BLOB             ||

            //primaryColumnType == Types.BINARY           ||
            //primaryColumnType == Types.VARBINARY        ||
            //primaryColumnType == Types.LONGVARBINARY    ||

            isCharLobType(primaryColumnType)		||
            // If none of above, check if the field is mapped to byte[].
            // We should treat any field mapped to byte[] as mapped to a LOB.
            getEnumType() == FieldTypeEnumeration.ARRAY_BYTE_PRIMITIVE;
    }

    /**
     * Determines if <code>sqltype</code> passed to this method is to be considered a character
     * LOB type.
     * @return <code>true</code> if field is mapped to character LOB column type. <code>false</code>
     * 			otherwise.
     */
    public static boolean isCharLobType(int sqlType) {
        //Resolve : Need to check for all supported datbases all possible LOB types
        return
            sqlType == Types.LONGVARCHAR  ||
            sqlType == Types.CLOB;
    }

    /**
     * Determines if the <code>sqlType</code> passed to this method corresponds to
     * a fixed char type.
     *
     * @param sqlType The input sqlType.
     * @return <code>true</code> if field is mapped to Types.CHAR
     * <code>false</code> otherwise.
     */
    public static boolean isFixedCharType(int sqlType) {
        return sqlType == Types.CHAR;
    }

    /**
     * Gets the <code>ColumnElement</code> for the primary column of this field.
     * @return The <code>ColumnElement</code> for the primary column of this field.
     */
    public ColumnElement getPrimaryColumn() {
        return ((ColumnElement) columnDescs.get(0));
    }

    /**
     * Returns an iterator on the mapped column elements.
     * @return An iterator on the mapped column elements.
     */
    public Iterator getColumnElements() {
        return columnDescs.iterator();
    }

    /**
     * Returns true if this field is a version field.
     */
    public boolean isVersion() {
        return ((sqlProperties & FieldDesc.PROP_VERSION_FIELD) > 0);
    }

    /**
     * Increments this field in the instance managed by state manager
     * <code>sm</code> by one.
     *
     * @param sm State manager to be modified.
     */
    public void incrementValue(StateManager sm) {
        assert isVersion();

        Long val = (Long) getValue(sm);
        long value = (val != null) ? val.longValue() : 0;

        setValue(sm, new Long(++value));
    }

    //
    // ------------ Initialisation methods ------------
    //

    /**
     * Calls the superclass method and disables concurrency checking
     * for certain field types.
     */
    protected void setupDesc(Field f) {
        super.setupDesc(f);

        // Disables concurrency check for fields mapped to LOB columns.
        if (isMappedToLob() ) {
            sqlProperties = sqlProperties & ~PROP_IN_CONCURRENCY_CHECK;
        }

        // Disables the concurrency check for scaled numeric fields.
        switch (getEnumType()) {
            case FieldTypeEnumeration.FLOAT_PRIMITIVE:
            case FieldTypeEnumeration.FLOAT:
            case FieldTypeEnumeration.DOUBLE_PRIMITIVE:
            case FieldTypeEnumeration.DOUBLE:
            case FieldTypeEnumeration.BIGDECIMAL:
                sqlProperties &= ~PROP_IN_CONCURRENCY_CHECK;
        }
    }

    /**
     * This array specifies the precedence of Java types that are mapped
     * to non-nullable database types without scale.
     * The primitive type long has the highest precedence and the type
     * Float has the lowest.
     */
    private static final Class[] nonNullableNonScaledTypes =
            {
                Long.TYPE, Integer.TYPE, Short.TYPE, Byte.TYPE, Double.TYPE,
                Float.TYPE, BigInteger.class, BigDecimal.class, Long.class,
                Integer.class, Short.class, Byte.class, Double.class, Float.class
            };

    /**
     * This array specifies the precedence of Java types that are mapped
     * to nullable SQL types without scale.
     * The type BigDecimal has the highest precedence and the primitive
     * type float has the lowest.
     */
    private static final Class[] nullableNonScaledTypes =
            {
                BigInteger.class, BigDecimal.class, Long.class, Integer.class, Short.class,
                Byte.class, Double.class, Float.class, Long.TYPE, Integer.TYPE,
                Short.TYPE, Byte.TYPE, Double.TYPE, Float.TYPE
            };

    /**
     * This array specifies the precedence of Java types that are mapped
     * to non-nullable SQL types with scale.
     * The primitive type double has the highest precedence and the primitive
     * type byte has the lowest.
     */
    private static final Class[] nonNullableScaledTypes =
            {
                Double.TYPE, Float.TYPE, Long.TYPE, Integer.TYPE, Short.TYPE,
                Byte.TYPE,  BigDecimal.class, Double.class, BigInteger.class,
                Long.class, Integer.class, Short.class, Byte.class
            };

    /**
     * This array specifies the precedence of Java types that are mapped
     * to non-nullable SQL types without scale.
     * The primitive type BigDecimal has the highest precedence and the primitive
     * type float has the lowest.
     */
    private static final Class[] nullableScaledTypes =
            {
                BigDecimal.class, Double.class, Float.class, BigInteger.class,
                Long.class, Integer.class, Short.class, Byte.class, Double.TYPE,
                Float.TYPE, Long.TYPE, Integer.TYPE, Short.TYPE, Byte.TYPE
            };

    /**
     * This method looks up the type precedence given a typePrecedence array.
     * @param type the class type whose precedence we want to look up
     * @param typePrecedence an array of types. Possible values are:
     * @see #nonNullableNonScaledTypes
     * @see #nullableNonScaledTypes
     * @see #nonNullableScaledTypes
     * @see #nullableScaledTypes
     * @return an integer value indicating the precedence
     */
    private static int lookupTypePrecedence(Class type, Class typePrecedence[]) {
        for (int i = 0; i < typePrecedence.length; i++) {
            if (type == typePrecedence[i]) {
                return i;
            }
        }

        return Integer.MAX_VALUE;
    }

    /**
     * This method computes the type precedence for the given field f.
     * @return an integer value indicating the precedence. 0 indicates
     * highest precedence and Integer.MAX_VALUE indicates lowest.
     */
    private int computeTypePrecedence() {
        ColumnElement c = (ColumnElement) columnDescs.get(0);
        int sqlType = c.getType();
        Class type = getType();
        boolean isNullable = c.isNullable();
        int precedence = Integer.MAX_VALUE;

        switch (sqlType) {
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
                if (isNullable) {
                    precedence = lookupTypePrecedence(type, nullableNonScaledTypes);
                } else {
                    precedence = lookupTypePrecedence(type, nonNullableNonScaledTypes);
                }
                break;
            case Types.REAL:
            case Types.FLOAT:
            case Types.DOUBLE:
                if (isNullable) {
                    precedence = lookupTypePrecedence(type, nullableScaledTypes);
                }  else {
                    precedence = lookupTypePrecedence(type, nonNullableScaledTypes);
                }
                break;
            case Types.NUMERIC:
            case Types.DECIMAL:
                int scale = -1;
                if ((scale = c.getScale().intValue()) == 0) {
                    // non scaled type
                    if (isNullable) {
                        precedence = lookupTypePrecedence(type, nullableNonScaledTypes);
                    } else {
                        precedence = lookupTypePrecedence(type, nonNullableNonScaledTypes);
                    }
                } else if (scale > 0) {
                    // scaled type
                    if (isNullable) {
                        precedence = lookupTypePrecedence(type, nullableScaledTypes);
                    } else {
                        precedence = lookupTypePrecedence(type, nonNullableScaledTypes);
                    }
                }
                break;
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                if (type == String.class) {
                    precedence = 0;
                }
                break;
            case Types.DATE:
            case Types.TIMESTAMP:
                if (java.util.Date.class.isAssignableFrom(type)) {
                    precedence = 0;
                }
                break;
            case Types.BIT:
                if (type == Boolean.class) {
                    if (isNullable) {
                        precedence = 0;
                    } else {
                        precedence = 1;
                    }
                } else if (type == Boolean.TYPE) {
                    if (isNullable) {
                        precedence = 1;
                    } else {
                        precedence = 0;
                    }
                }
                break;
        }

        return precedence;
    }

    void computeTrackedPrimitiveFields() {
        for (int i = 0; i < classDesc.fields.size(); i++) {
            FieldDesc tf = (FieldDesc) classDesc.fields.get(i);

            if ((tf instanceof LocalFieldDesc) && (this != tf) && (compareColumns(this, tf) == true)) {
                addTrackedField(tf);
            }
        }
    }

    /**
     * Compute the primary tracked field.
     */
    void computePrimaryTrackedPrimitiveField() {
        ArrayList trackedFields = null;

        // We need to skip fields that are read-only.
        if (((trackedFields = getTrackedFields()) == null) ||
                (sqlProperties & (FieldDesc.PROP_PRIMARY_TRACKED_FIELD |
                FieldDesc.PROP_SECONDARY_TRACKED_FIELD |
                FieldDesc.PROP_READ_ONLY)) > 0) {
            return;
        }

        // We don't know which field is the primary field yet, so we set this field
        // to be secondary. Once we know that this field is primary, we can unset it.
        sqlProperties |= FieldDesc.PROP_SECONDARY_TRACKED_FIELD;

        FieldDesc primaryTrackedField = null;
        int currentPrecedence = Integer.MAX_VALUE;
        int precedence = 0;

        if ((precedence = computeTypePrecedence()) < currentPrecedence) {
            primaryTrackedField = this;
            currentPrecedence = precedence;
        }

        for (int j = 0; j < trackedFields.size(); j++) {
            FieldDesc tf = (FieldDesc) trackedFields.get(j);

            // We don't need to assign primary or secondary status for ForeignFieldDesc
            // because we don't write it to the disk.
            if (tf instanceof ForeignFieldDesc) {
                continue;
            }

            // We don't know which field is the primary field yet, so we set this field
            // to be secondary. Once we know the which field is primary, we can unset it.
            tf.sqlProperties |= FieldDesc.PROP_SECONDARY_TRACKED_FIELD;

            if ((precedence = ((LocalFieldDesc) tf).computeTypePrecedence()) < currentPrecedence) {
                primaryTrackedField = tf;
                currentPrecedence = precedence;
            }
        }

        // If we didn't find any candidate for the primary tracked field,
        // we pick f as the one.
        if (primaryTrackedField == null) {
            primaryTrackedField = this;
        }

        if (logger.isLoggable(Logger.FINEST)) {
            logger.finest("sqlstore.model.classdesc.primarytrackedfield", primaryTrackedField.getName()); // NOI18N
        }

        primaryTrackedField.sqlProperties |= FieldDesc.PROP_PRIMARY_TRACKED_FIELD;
        primaryTrackedField.sqlProperties &= ~FieldDesc.PROP_SECONDARY_TRACKED_FIELD;
    }

    void computeTrackedRelationshipFields() {

        if (((sqlProperties & FieldDesc.PROP_REF_INTEGRITY_UPDATES) == 0) &&
                ((sqlProperties & FieldDesc.PROP_PRIMARY_KEY_FIELD) == 0)) {
            return;
        }

        for (int k = 0; k < classDesc.foreignFields.size(); k++) {
            ForeignFieldDesc tf = (ForeignFieldDesc) classDesc.foreignFields.get(k);

            if (compareColumns(this, tf) == true) {
                // In the case where the relationship has cardinality LWB and UPB
                // both equal to 1, it is possible for the relationship field to
                // be mapped to same column as the primary key field. We will
                // allow this relationship field to be tracked by the primary key field.
                if (((sqlProperties & FieldDesc.PROP_PRIMARY_KEY_FIELD) > 0) &&
                        ((tf.cardinalityUPB > 1) || (tf.cardinalityLWB == 0))) {
                    continue;
                }

                // If f does not track other primitive fields, we need to
                // make it the primary tracked field because it was skipped
                // in computeTrackedPrimitiveFields().
                if (getTrackedFields() == null) {
                    sqlProperties |= FieldDesc.PROP_PRIMARY_TRACKED_FIELD;
                }

                addTrackedField(tf);

                // Mark f to indicate that it is tracking a relationship field.
                sqlProperties |= FieldDesc.PROP_TRACK_RELATIONSHIP_FIELD;
            }
        }
    }

    void cleanupTrackedFields() {
        ArrayList trackedFields = getTrackedFields();

        if (trackedFields != null) {
            for (int j = 1; ; j++) {
                int index = trackedFields.size() - j;

                if (index < 0) {
                    break;
                }

                FieldDesc tf = (FieldDesc) trackedFields.get(index);

                if (tf instanceof LocalFieldDesc) {
                    break;
                }

                ArrayList foreignTrackedFields = tf.getTrackedFields();

                if (foreignTrackedFields != null) {
                    trackedFields.removeAll(foreignTrackedFields);
                }
            }
        }
    }

}
