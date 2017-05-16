/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.appserv.web.cache.mapping;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.glassfish.web.LogFacade;

/** ValueConstraint class represents a field's value constraint; 
 *  supports common matching expressions. 
 */
public class ValueConstraint {

    private static final String[] EXPR_NAMES = {
        "", "'equals'", "'greater'", "'lesser'", "'not-equals'", "'in-range'"
    };

    private static final Logger _logger = LogFacade.getLogger();

    /**
     * The resource bundle containing the localized message strings.
     */
    private static final ResourceBundle _rb = _logger.getResourceBundle();

    // field values to match 
    private String matchValue = null; 
    private float minValue = Float.MIN_VALUE; 
    private float maxValue = Float.MAX_VALUE; 

    // the default match expr 
    private int matchExpr = Constants.MATCH_EQUALS; 

    // string representation of this expr and the object
    private String str; 

    // whether to cache if there was a match
    private boolean cacheOnMatch = true;
    // whether to cache if there was a failure to match
    private boolean cacheOnMatchFailure = false;

    /** create constraint: field value matches with the given string expression
     * @param value specific value to match
     * @param expr match expression
     */
    public ValueConstraint(String value, String expr) 
                                throws IllegalArgumentException {
        int match;

        if (expr == null || expr.equals("equals")) {
            match = Constants.MATCH_EQUALS;
        } else if (expr.equals("greater")) {
            match = Constants.MATCH_GREATER;
            try {
                minValue = Float.parseFloat(value);
            } catch (NumberFormatException nfe) {
                String msg = _rb.getString(LogFacade.GREATER_EXP_REQ_NUMERIC);
                Object[] params = { value };
                msg = MessageFormat.format(msg, params);

                throw new IllegalArgumentException(msg);
            }
        }
        else if (expr.equals("lesser")) {
            match = Constants.MATCH_LESSER;
            try {
                maxValue = Float.parseFloat(value);
            } catch (NumberFormatException nfe) {
                String msg = _rb.getString(LogFacade.LESSER_EXP_REQ_NUMERIC);
                Object[] params = { value };
                msg = MessageFormat.format(msg, params);

                throw new IllegalArgumentException(msg);
            }
        }
        else if (expr.equals("not-equals"))
            match = Constants.MATCH_NOT_EQUALS;
        else if (expr.equals("in-range")) {
            match = Constants.MATCH_IN_RANGE;
            parseRangeValue(value);
        }
        else {
            String msg = _rb.getString(LogFacade.ILLEGAL_VALUE_EXP);
            Object[] params = { value, expr };
            msg = MessageFormat.format(msg, params);

            throw new IllegalArgumentException(msg);
        }

        this.matchExpr = match;
        this.matchValue = value;
    }

    /**
     * match the given a range of values
     * @param value range 2034 - 4156
     * @throws IllegalArgumentException
     * check for numeric values
     * check if the first value is lesser than the next
     * at a minimum, there should be separator and one digit min and
     * max values("m-n").
     *
     * XXX: check how this handles negative ranges:  -1230 - -124.
     */
    private void parseRangeValue(String value) throws IllegalArgumentException {
        float val1, val2;

        if (value == null || value.length() <= 2) {
            String msg = _rb.getString(LogFacade.ILLEGAL_VALUE_RANGE);
            Object[] params = { value };
            msg = MessageFormat.format(msg, params);

            throw new IllegalArgumentException(msg);
        }

        // get the separator first
        int separator;
        if (value.charAt(0) == '-') {
           separator = value.indexOf('-', 1); 
        } else {
           separator = value.indexOf('-', 0); 
        }
        if (separator == -1) {
            String msg = _rb.getString(LogFacade.MISSING_RANGE_SEP);
            Object[] params = { value };
            msg = MessageFormat.format(msg, params);

            throw new IllegalArgumentException(msg);
        }

        // get the first value
        String sval1 = value.substring(0, separator).trim();
        try {
            val1 = Float.parseFloat(sval1);
        } catch (NumberFormatException nfe) {
            String msg = _rb.getString(LogFacade.LOWER_RANGE_REQ_NUMBER);
            Object[] params = { sval1 };
            msg = MessageFormat.format(msg, params);

            throw new IllegalArgumentException(msg);
        }

        // is max value specified at all?
        if (separator == value.length()){
            String msg = _rb.getString(LogFacade.RANGE_REQ_UPPER_BOUND);
            Object[] params = { value };
            msg = MessageFormat.format(msg, params);

            throw new IllegalArgumentException(msg);
        }

        String sval2 = value.substring(separator + 1).trim();
        try {
            val2 = Float.parseFloat(sval2);
        } catch (NumberFormatException nfe) {
            String msg = _rb.getString(LogFacade.UPPER_RANGE_REQ_NUMBER);
            Object[] params = { sval2 };
            msg = MessageFormat.format(msg, params);

            throw new IllegalArgumentException(msg);
        }

        this.minValue = val1;
        this.maxValue = val2;
    }

    /** set value for this constraint
     * @param value specific value to match
     */
    public void setValue(String value) {
        this.matchValue = value;
    }

    /** set minimum value
     * @param value minimum value
     */
    public void setMinValue(float value) {
        this.minValue = value;
    }

    /** set the maximum value
     * @param value maximum value
     */
    public void setMaxValue(float value) {
        this.maxValue = value;
    }

    /** set field matching expression
     * @param expr match expression
     */
    public void setMatchExpr(int expr) {
        this.matchExpr = expr;
    }

    /** set whether to cache if there was a match
     * @param cacheOnMatch should the field value match, enable cache?
     */
    public void setCacheOnMatch(boolean cacheOnMatch) {
        this.cacheOnMatch = cacheOnMatch;
    }

    /** set whether to cache if there was a failure to match
     * @param cacheOnMatchFailure should the field value doesn't match, 
     *  enable cache?
     */
    public void setCacheOnMatchFailure(boolean cacheOnMatchFailure) {
        this.cacheOnMatchFailure = cacheOnMatchFailure;
    }

    /** match with the given <code>Object</code> value.
     *  @return <code>true</code> if the value passes the constraint, 
     *  <code>false</code> otherwise. 
     */ 
    public boolean matches(Object value) {
        boolean result;
        switch (matchExpr) {
            case Constants.MATCH_EQUALS:
                result = matchValue.equals(value);
                break;
            case Constants.MATCH_NOT_EQUALS:
                result = !(matchValue.equals(value));
                break;
            case Constants.MATCH_GREATER:
                // convert to a Float type
                {
                    Float lval = new Float(value.toString());
                    result = (lval.floatValue() > minValue);
                }
                break;
            case Constants.MATCH_LESSER:
                // convert to a Float type
                {
                    Float lval = new Float(value.toString());
                    result = (lval.floatValue() < maxValue);
                }
                break;
            case Constants.MATCH_IN_RANGE:
                // convert to a Float type
                {
                    Float lval = new Float(value.toString());
                    result = (lval.floatValue() >= minValue && 
                                    lval.floatValue() <= maxValue);
                }
                break;
            default:
                result = false;
                break;
        }

        // did it match?
        return (result == true) ? cacheOnMatch : cacheOnMatchFailure;
    }

    /**
     * @return a string representation of this value/expr element
     */
    public String toString() {
        if (str == null) {
            str = "match value = " + matchValue + " match expr = "
                    + EXPR_NAMES[matchExpr];
        }
        return str;
    }
}
