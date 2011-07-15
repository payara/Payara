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

package com.sun.jdo.spi.persistence.generator.database;


import com.sun.jdo.spi.persistence.utility.StringHelper;

import com.sun.jdo.spi.persistence.utility.logging.Logger;

/**
 * Represents how a JDBC type (i.e., one defined by java.sql.Types) is
 * created in the database.
 */
class JDBCInfo {

    //
    // These constants are used to locate properties indicating values for
    // the fields in instances of JDBCInfo.
    //
    //
    
    /** Indicator that property designates the type of a mapped SQL type. */
    private static final String INDICATOR_TYPE =
        DatabaseGenerationConstants.INDICATOR_JDBC_TYPE;

    /** Indicator that property designates nullability of mapped SQL type. */
    private static final String INDICATOR_NULLABLE =
    DatabaseGenerationConstants.INDICATOR_JDBC_NULLABLE;

    /** Indicator that property designates the precision of mapped SQL type. */
    private static final String INDICATOR_PRECISION =
        DatabaseGenerationConstants.INDICATOR_JDBC_PRECISION;

    /** Indicator that property designates the scale of mapped SQL type. */
    private static final String INDICATOR_SCALE =
    DatabaseGenerationConstants.INDICATOR_JDBC_SCALE;

    /** Indicator that property designates length of a mapped SQL type. */
    private static final String INDICATOR_LENGTH =
        DatabaseGenerationConstants.INDICATOR_JDBC_LENGTH;

    /** Indicator  that a type does not have a length associated with it. */
    private static final String NO_LENGTH_INDICATOR = "null";

    /** Flag value which indicates that a JDBCInfo does not have a length. */
    private static final Integer NO_LENGTH = new Integer(-1);

     /** Logger for warning & error messages */
    private static final Logger logger =
            LogHelperDatabaseGenerator.getLogger();
    
    /** Value from java.sql.Types. */
    private int jdbcType;
    
    /** True iff a column of this type is nullable; default is false.  */
    private boolean nullable = false;
    
    /** Indicates precision of a fixed-point number column; default is null. */
    private Integer precision = null;
    
    /** Indicates scale of a fixed-point number column; default is null. */
    private Integer scale = null;
    
    /** Indicates length of a char, etc. column; default is null. */
    private Integer length = null;


    //
    // Allow determining if which fields have been assigned values.
    //
    
    /** Indicates which fields in this instance have been set. */     
    private byte fieldsWithValues = 0;

    /** Mask to indicate whether or not {@link #jdbcType} has a value. */
    private static final byte MASK_JDBC_TYPE =  1 << 0;

    /** Mask to indicate whether or not {@link #nullable} has a value. */
    private static final byte MASK_NULLABLE =  1 << 1;

    /** Mask to indicate whether or not {@link #precision} has a value. */
    private static final byte MASK_PRECISION = 1 << 2;

    /** Mask to indicate whether or not {@link #scale} has a value. */
    private static final byte MASK_SCALE =     1 << 3;

    /** Mask to indicate whether or not {@link #length} has a value. */
    private static final byte MASK_LENGTH =    1 << 4;

    /** Mask to access all field flags at once. */
    private static final byte MASK_ALL = MASK_JDBC_TYPE | MASK_NULLABLE
        | MASK_PRECISION | MASK_SCALE | MASK_LENGTH;
    

    /**
     * Constructor which initializes all fields.
     * @param jdbcType See {@link jdbcType}.
     * @param precision See {@link precision}.
     * @param scale See {@link scale}.
     * @param length See {@link length}.
     * @param nullable See {@link nullable}.
     */
    JDBCInfo(int jdbcType, Integer precision, Integer scale, 
            Integer length, boolean nullable) {

        this.jdbcType = jdbcType;
        this.precision = precision;
        this.scale = scale;
        this.length = length;
        this.nullable = nullable;

        fieldsWithValues = MASK_ALL;
    }

    /**
     * Use this constructor in conjunction with multiple setValue to
     * initialize an instance.
     */
    JDBCInfo() { }

    
    /**
     * Sets the value of one field of this JDBCInfo.
     * @param indicator Determines which field is set.
     * @param value String form of the new value of a field.  Must not be
     * null.  Empty String means to reset the field value to its default,
     * except for jdbcType: That field has no default, so given an empty
     * String the value of jdbcType is unchanged.
     * @throws IllegalJDBCTypeException if <code>indicator</code> shows that
     * we are setting a JDBC Type and <code>value</code> is not
     * recognized as being a valid member of java.sql.Types.
     */
    void setValue(String value, String indicator)
            throws IllegalJDBCTypeException {

        if (indicator.equals(INDICATOR_TYPE)) {
            if (!StringHelper.isEmpty(value)) {
                Integer type = MappingPolicy.getJdbcType(value);
                if (null == type) {
                    throw new IllegalJDBCTypeException();
                }
                this.jdbcType = type.intValue();
                this.fieldsWithValues |= MASK_JDBC_TYPE;
            }

        } else if (indicator.equals(INDICATOR_NULLABLE)) {
            if (StringHelper.isEmpty(value)) {
                this.nullable = false; // default
            } else {
                this.nullable = Boolean.valueOf(value).booleanValue();
            }
            this.fieldsWithValues |= MASK_NULLABLE;

        } else if (indicator.equals(INDICATOR_PRECISION)) {
            this.precision = getIntegerValue(value);
            this.fieldsWithValues |= MASK_PRECISION;

        } else if (indicator.equals(INDICATOR_SCALE)) {
            this.scale = getIntegerValue(value);
            this.fieldsWithValues |= MASK_SCALE;

        } else if (indicator.equals(INDICATOR_LENGTH)) {
            if (value.trim().equals(NO_LENGTH_INDICATOR)) {
                this.length = NO_LENGTH;
            } else {
                this.length = getIntegerValue(value);
            }
            this.fieldsWithValues |= MASK_LENGTH;
        } 
    }

    /**
     * @param s String whose Integer value is sought.
     * @return the value of s as an Integer, or null if s is empty.
     */
    private Integer getIntegerValue(String s) {
        Integer rc = null;
        if (!StringHelper.isEmpty(s)) {
            rc = new Integer(s);
        }
        return rc;
    }

    //
    // A note about "completeness".
    // JDBCInfo instances are created one of 2 ways: By loading a .properties
    // file for a database, or by a user override.  In the first case, the
    // JDBCInfo will have values for all relevant fields, because that is the
    // way we have created the .properties files. ("relevant" here means
    // that, e.g., a length value will be present if required for a field
    // type for which length is relevant such as VARCHAR.)
    //
    // In the second case, a user override might provide only one overriden
    // value, let's say length.  So for a particular field name, we may know
    // only that it should have a length; of course we need more.  So before
    // allowing access to a JDBCInfo that was created for a field, complete()
    // it with a JDBCInfo that was created for that field's type.
    //
    // See MappingPolicy.JDBCInfo().
    //

    
    /**
     * Fill in values for fields based on values in another JDBCInfo
     * instance.  Only those fields for which this instance does not already
     * have a value are changed.
     * @param other Another instance of JDBCInfo that has values which are
     * used to set as-yet-unset values in this instance.
     */
    // XXX For precision and scale, this is not entirely correct.
    // We should check if this.<val> is set.  If so, it must not be greater
    // than other.<val>.  In other words, the other instance's value
    // overrules the value in this instance, because the other value was
    // provided in the dbvendor-specific .properties file, which users must
    // not override.  If this instance (i.e., the user override's instance)
    // specifies an invalid override, we should log a warning, warn the user,
    // and use the other.<val>.
    void complete(JDBCInfo other) {
        if (logger.isLoggable(Logger.FINEST)) {
            logger.finest("Entering JDBCInfo.complete: " // NOI18N
                          + "\nthis: " + this // NOI18N
                          + "\nother: " + other); // NOI18N
        }
        if (MASK_ALL != fieldsWithValues) {
            if ((fieldsWithValues & MASK_JDBC_TYPE) == 0) {
                this.jdbcType = other.jdbcType;
            }

            if ((fieldsWithValues & MASK_NULLABLE) == 0) {
                this.nullable = other.nullable;
            }
            
            if ((fieldsWithValues & MASK_PRECISION) == 0) {
                this.precision = other.precision;
            }

            if ((fieldsWithValues & MASK_SCALE) == 0) {
                this.scale = other.scale;
            }

            if ((fieldsWithValues & MASK_LENGTH) == 0
                || (NO_LENGTH.equals(other.length))
                || (other.length.intValue() < this.length.intValue())) {
                this.length = other.length;
            }

            fieldsWithValues = MASK_ALL;
        }
        if (logger.isLoggable(Logger.FINEST)) {
            logger.finest("Leaving JDBCInfo.complete: " // NOI18N
                          + "\nthis: " + this); // NOI18N
        }
    }

    /**
     * @return <code>true</code> if this instance has been assigned values
     * for all fields.
     */
    boolean isComplete() {
        return fieldsWithValues == MASK_ALL;
    }

    /**
     * Update this JDBCInfo with information from the other JDBCInfo.
     * @param other The JDBCInfo with values that will overwrite values in
     * this JDBCInfo.
     */
    void override(JDBCInfo other) {
        if (null != other) {
            this.jdbcType = other.jdbcType;
        }
    }

    /**
     * @return true iff this instance has a value for jdbcType
     */
    public boolean hasJdbcType() {
        return (fieldsWithValues & MASK_JDBC_TYPE) == 1;
    }

    /**
     * @return The JDBC corresponding to this JDBCInfo.  See
     * {@link java.sql.Types}.
     */
    public int getJdbcType() {
        return jdbcType;
    }

    /**
     * @return <code>true</code> of columns based on this JDBCInfo should be
     * nullable.
     */
    public boolean getNullable() {
        return nullable;
    }

    /**
     * @return The precision of of columns based on this JDBCInfo.
     */
    public Integer getPrecision() {
        return precision;
    }

    /**
     * @return The scale of of columns based on this JDBCInfo.
     */
    public Integer getScale() {
        return scale;
    }

    /**
     * @return The length of of columns based on this JDBCInfo, or null if
     * this JDBCInfo does not need a length (e.g. CLOB on Oracle).
     */
    public Integer getLength() {
        return NO_LENGTH.equals(length) ? null : length;
    }

    /**
     * Debugging support.
     * @return A String with the value of each field.
     */
    public String toString() {
        return "JDBCInfo:" // NOI18N
            + " jdbcType=" + jdbcType // NOI18N
            + " nullable=" + nullable // NOI18N
            + " precision=" + precision // NOI18N
            + " scale=" + scale // NOI18N
            + " length=" + length // NOI18N
            + " fieldsWithValues=0x" + Integer.toHexString(fieldsWithValues); // NOI18N
    }

    /**
     * Used to indicate that a given JDBC Type name is not recognized.
     */
    static class IllegalJDBCTypeException extends Exception {
        IllegalJDBCTypeException() { }
    }
}
