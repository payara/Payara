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
 * ActionDesc.java
 *
 * Create on March 3, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore;


/**
 * <P>This interface defines the constraint operation constants that are
 * recognized by the <code>addConstraint</code> method of the interface <code>RetrieveDesc</code>.
 */
public interface ActionDesc {
    // LAST VALUE USED: 63

    // Constants for life cycle events

    public static final int LOG_CREATE = 1;

    public static final int LOG_DESTROY = 2;

    public static final int LOG_UPDATE = 3;

    public static final int LOG_NOOP = 4;

    // Select operators

    /**
     * Unary operator for taking the absolute value.
     */
    public static final int OP_ABS = 1;

    /**
     * Binary operator for adding two values.
     */
    public static final int OP_ADD = 2;

    /**
     * Logical operator AND.
     */
    public static final int OP_AND = 3;

    /**
     */
    public static final int OP_APPROX = 4;

    /**
     */
    public static final int OP_FIELD = 5;

    /**
     */
    public static final int OP_BETWEEN = 6;

    /**
     * Qualifies the query with DISTINCT
     */
    public static final int OP_DISTINCT = 7;

    /**
     * Binary operator for dividing two values
     */
    public static final int OP_DIV = 8;

    /**
     * Equality operator.
     */
    public static final int OP_EQ = 9;

    /**
     */
    public static final int OP_EQ_CLASS = 41;

    /**
     */
    public static final int OP_EQUIJOIN = 10;

    /**
     */
    public static final int OP_FOR_UPDATE = 11;

    /**
     */
    public static final int OP_GE = 12;

    /**
     */
    public static final int OP_GT = 13;

    /**
     */
    public static final int OP_IN = 14;

    /**
     * Relational operator for less-than-and-equal.
     */
    public static final int OP_LE = 15;

    /**
     */
    public static final int OP_LEFTJOIN = 16;

    /**
     */
    public static final int OP_LENGTH = 17;

    /**
     */
    public static final int OP_LENGTHB = 18;

    /**
     * Pattern matching operator.
     */
    public static final int OP_LIKE = 19;

    /**
     */
    public static final int OP_LOWER = 20;

    /**
     * Relational operator for less-than.
     */
    public static final int OP_LT = 21;

    /**
     */
    public static final int OP_LTRIM = 22;

    /**
     */
    public static final int OP_MAX_ROWS = 23;

    /**
     * Binary operator for multiplying two values.
     */
    public static final int OP_MUL = 24;

    /**
     * Inequality operator.
     */
    public static final int OP_NE = 25;

    /**
     * Unary negation operator.
     */
    public static final int OP_NOT = 26;

    /**
     * Unary operator for checking non-null value.
     */
    public static final int OP_NOTNULL = 27;

    /**
     * Unary operator for checking null value.
     */
    public static final int OP_NULL = 28;

    /**
     * Logical operator OR.
     */
    public static final int OP_OR = 29;

    /**
     * Order the result by ascending order.
     */
    public static final int OP_ORDERBY = 30;

    /**
     * Order the result by descending order.
     */
    public static final int OP_ORDERBY_DESC = 31;

    /**
     */
    public static final int OP_PARAMETER_COUNT = 32;

    /**
     */
    public static final int OP_RIGHTJOIN = 33;

    /**
     * Unary operator for trimming trailing blanks in a string.
     */
    public static final int OP_RTRIM = 34;

    /**
     */
    public static final int OP_RTRIMFIXED = 43;

    /**
     */
    public static final int OP_SOUNDEX = 35;

    /**
     * square root of a number
     */
    public static final int OP_SQRT = 47;

    /**
     * Binary operator for subtracting one value from another.
     */
    public static final int OP_SUB = 36;

    /**
     * Operator for Substring with two arguments: string, start.
     */
    public static final int OP_SUBSTR = 37;

    /**
     */
    public static final int OP_SUBSTRB = 38;

    /**
     */
    public static final int OP_UPPER = 39;

    /**
     */
    public static final int OP_VALUE = 40;

    /**
     */
    public static final int OP_NONKEY = 42;

    /**
     * String concatenation operator
     */
    public static final int OP_CONCAT = 44;

    /**
     * Operator for not exists subquery
     */
    public static final int OP_NOTEXISTS = 45;

    /**
     * Operator for exists subquery
     */
    public static final int OP_EXISTS = 46;

    /**
     * Operator for like with escape
     */
    public static final int OP_LIKE_ESCAPE = 48;

    /**
     * Operator for Substring with three arguments: string, start, length.
     */
    public static final int OP_SUBSTRING = 49;

    /**
     * Operator for position
     */
    public static final int OP_POSITION = 50;

    /**
     * Operator for position with start parameter
     */
    public static final int OP_POSITION_START = 51;

    /**
     * Operator for a non relationship join.
     */
    public static final int OP_NONREL_JOIN = 52;

    /**
     * Operator for query parameters
     */
    public static final int OP_PARAMETER = 53;

    /**
     * Operator for queries on nullable columns mapped to primitive fields
     */
    public static final int OP_MAYBE_NULL = 54;

    /**
     * Operator for NOT IN
     */
    public static final int OP_NOTIN = 55;

    /**
     * Operator for null comparision by function.
     */
    public static final int OP_NULL_COMPARISION_FUNCTION = 56;

    /**
     * Binary operator for MOD.
     */
    public static final int OP_MOD = 57;

    /**
     * Operator for AVG aggregate function.
     */
    public static final int OP_AVG = 58;

    /**
     * Operator for MIN aggregate function.
     */
    public static final int OP_MIN = 59;

    /**
     * Operator for SUM aggregate function.
     */
    public static final int OP_SUM = 60;

    /**
     * Operator for MAX aggregate function.
     */
    public static final int OP_MAX = 61;

    /**
     * Operator for COUNT aggregate function.
     */
    public static final int OP_COUNT = 62;

    /**
     * Operator for COUNT aggregate function on pc objects.
     */
    public static final int OP_COUNT_PC = 63;

    /**
     */
    public Class getPersistenceCapableClass();

}
