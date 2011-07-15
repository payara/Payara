/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * Optimizer.g
 *
 * Created on June 11, 2001
 */

header
{
    package com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc;
    
    import java.util.*;
    
    import java.math.BigDecimal;
    import java.math.BigInteger;

    import com.sun.jdo.api.persistence.support.JDOFatalUserException;
    import org.glassfish.persistence.common.I18NHelper;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.TypeTable;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.Type;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.ClassType;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.FieldInfo;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.NumericType;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.NumericWrapperClassType;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.NumberType;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.StringType;
}

/**
 * This class defines the optimizer pass of the JQL compiler.
 * It takes the typed AST as produced by the smenatic analysis and
 * converts it into a simpler but equivalent typed AST.
 * 
 * @author  Michael Bouschen
 * @version 0.1
 */
class Optimizer extends TreeParser;

options
{
    importVocab = JQL;
    buildAST = true;
    defaultErrorHandler = false;
    ASTLabelType = "JQLAST"; //NOI18N
}

{
    /**
     * I18N support
     */
    protected final static ResourceBundle messages = 
        I18NHelper.loadBundle(Optimizer.class);
    
    /**
     * type table 
     */
    protected TypeTable typetab;
    
    /**
     * query parameter table
     */
    protected ParameterTable paramtab;
    
    /**
     *
     */
    protected ErrorMsg errorMsg;

    /**
     *
     */
    public void init(TypeTable typetab, ParameterTable paramtab, 
                     ErrorMsg errorMsg)
    {
        this.typetab = typetab;
        this.paramtab = paramtab;
        this.errorMsg = errorMsg;
    }

    /**
     *
     */
    public void reportError(RecognitionException ex) {
        errorMsg.fatal("Optimizer error", ex); //NOI18N
    }

    /**
     *
     */
    public void reportError(String s) {
        errorMsg.fatal("Optimizer error: " + s); //NOI18N
    }

    /**
     * Converts the string argument into a single char.
     */
    protected static char parseChar(String text)
    {
        char first = text.charAt(0);
        if (first == '\\')
        {
            //found escape => check the next char
            char second = text.charAt(1);
            switch (second)
            {
            case 'n': return '\n';
            case 'r': return '\r';
            case 't': return '\t';
            case 'b': return '\b';
            case 'f': return '\f';
            case 'u': 
                // unicode spec
                return (char)Integer.parseInt(text.substring(2, text.length()), 16);
            case '0':            
            case '1':
            case '2':
            case '3':            
            case '4':
            case '5':            
            case '6':
            case '7': 
                // octal spec 
                return (char)Integer.parseInt(text.substring(1, text.length()), 8);
            default : return second;
            }
        }
        return first;
    }
    

    /**
     * Check an AND operation (BAND, AND) for constant operands 
     * that could be optimized.
     * @param op the AND operator
     * @param left the left operand
     * @param right the right operand
     * @return optimized JQLAST 
     */
    protected JQLAST checkAnd(JQLAST op, JQLAST left, JQLAST right)
    {
        JQLAST ast = op;

        if (isBooleanValueAST(left))
        {
            ast = handleValueAndExpr(op, left.getValue(), right);
        }
        else if (isBooleanValueAST(right))
        {
            ast = handleValueAndExpr(op, right.getValue(), left);
        }
        return ast;
    }

    /**
     * Check an OR operation (BOR, OR) for constant operands 
     * that could be optimized.
     * @param op the OR operator
     * @param left the left operand
     * @param right the right operand
     * @return optimized JQLAST 
     */
    protected JQLAST checkOr(JQLAST op, JQLAST left, JQLAST right)
    {
        JQLAST ast = op;

        if (isBooleanValueAST(left))
        {
            ast = handleValueOrExpr(op, left.getValue(), right);
        }
        else if (isBooleanValueAST(right))
        {
            ast = handleValueOrExpr(op, right.getValue(), left);
        }
        return ast;
    }

    /**
     * Check a equality operation (EQUAL, NOT_EQUAL) for constant operands
     * that could be optimized.
     * @param op the equality operator
     * @param left the left operand
     * @param right the right operand
     * @param negate true for not equal operation, false otherwise
     * @return optimized JQLAST 
     */
    protected JQLAST checkEqualityOp(JQLAST op, JQLAST left, JQLAST right, 
                                     boolean negate)
    {
        JQLAST ast = op;

        // case <VALUE> <op> <VALUE> 
        if ((left.getType() == VALUE) && (right.getType() == VALUE))
        {
            ast = handleValueEqValue(op, left, right, negate);
        }
        // case <boolean VALUE> <op> <expr>
        else if (isBooleanValueAST(left))
        {
            ast = handleBooleanValueEqExpr(op, left.getValue(), right, negate);
        }
        // case <expr> <op> <boolean VALUE>
        else if (isBooleanValueAST(right))
        {
            ast = handleBooleanValueEqExpr(op, right.getValue(), left, negate);
        }
        return ast;
    }

    /**
     * Check a object equality operation (OBJECT_EQUAL, OBJECT_NOT_EQUAL) 
     * for constant operands that could be optimized.
     * @param op the object equality operator
     * @param left the left operand
     * @param right the right operand
     * @param negate true for not equal operation, false otherwise
     * @return optimized JQLAST 
     */
    protected JQLAST checkObjectEqualityOp(JQLAST op, JQLAST left, JQLAST right,
                                           boolean negate)
    {
        JQLAST ast = op;

        if ((left.getType() == VALUE) && (right.getType() == VALUE))
        {
            ast = handleValueEqValue(op, left, right, negate);
        }
        return ast;
    }

    /**
     * Check a collection equality operation (COLLECTION_EQUAL, 
     * COLLECTION_NOT_EQUAL) for constant operands that could be optimized.
     * @param op the collection equality operator
     * @param left the left operand
     * @param right the right operand
     * @param negate true for not equal operation, false otherwise
     * @return optimized JQLAST 
     */
    protected JQLAST checkCollectionEqualityOp(JQLAST op, JQLAST left, 
                                               JQLAST right, boolean negate)
    {
        JQLAST ast = op;
        boolean isLeftConstant = (left.getType() == VALUE);
        boolean isRightConstant = (right.getType() == VALUE);
        
        if (isLeftConstant && isRightConstant)
        {
            ast = handleValueEqValue(op, left, right, negate);
        }
        else if ((isLeftConstant && (left.getValue() == null) && isNonConstantCollection(right)) || 
                 (isRightConstant && (right.getValue() == null) && isNonConstantCollection(left))) 
        {
            // This optimization is datastore dependend. 
            // In TP we know a collection returned by the datastore is never null.
            // null == <collection field> -> false
            // <collection field> == null -> false
            // null != <collection field> -> true
            // <collection field> != null -> true
            ast.setType(VALUE);
            ast.setValue(new Boolean(negate));
            ast.setFirstChild(null);
        }

        return ast;
    }

    /**
     * Check a logical not operation (LNOT) for a constant operand 
     * that could be optimized.
     * @param op the logical not operator
     * @param arg the operand
     * @return optimized JQLAST 
     */
    protected JQLAST checkLogicalNotOp(JQLAST op, JQLAST arg)
    {
        JQLAST ast = op;

        if (arg.getType() == VALUE)
        {
            // !value may be calculated at compile time.
            Object valueObj = arg.getValue();
            boolean value = (valueObj instanceof Boolean) ? 
                            ((Boolean)valueObj).booleanValue() : false;
            arg.setType(VALUE);
            arg.setValue(new Boolean(!value));
            arg.setNextSibling(null);
            ast = arg;
        }
        else
        {
            ast = deMorgan(arg);
        }
        return ast;
    }

    /**
     * Check a binary plus operation (PLUS) for constant operands
     * that could be optimized.
     * @param op the plus operator
     * @param left the left operand
     * @param right the right operand
     * @return optimized JQLAST 
     */
    protected JQLAST checkBinaryPlusOp(JQLAST op, JQLAST left, JQLAST right)
    {
        JQLAST ast = op;

        if ((left.getType() == VALUE) && (right.getType() == VALUE))
        {
            Object leftValue = left.getValue();
            Object rightValue = right.getValue();
            Object value = null;
            if (leftValue == null)
                value = rightValue;
            else if (rightValue == null)
                value = leftValue;
            else
            {
                Type type = op.getJQLType();
                
                if (type instanceof NumericWrapperClassType)
                    type = ((NumericWrapperClassType)type).getPrimitiveType();
                
                if (type.equals(typetab.intType))
                    value = new Integer(((Number)leftValue).intValue() + 
                        ((Number)rightValue).intValue());
                else if (type.equals(typetab.longType))
                    value = new Long(((Number)leftValue).longValue() + 
                        ((Number)rightValue).longValue());
                else if (type.equals(typetab.floatType))
                    value = new Float(((Number)leftValue).floatValue() + 
                        ((Number)rightValue).floatValue());
                else if (type.equals(typetab.doubleType))
                    value = new Double(((Number)leftValue).doubleValue() + 
                        ((Number)rightValue).doubleValue());
                else if (type.equals(typetab.bigDecimalType))
                    value = getBigDecimalValue(leftValue).add(
                       getBigDecimalValue(rightValue));
                else if (type.equals(typetab.bigIntegerType))
                    value = getBigIntegerValue(leftValue).add(
                        getBigIntegerValue(rightValue));
                else 
                    errorMsg.fatal(I18NHelper.getMessage(messages,
                        "jqlc.optimizer.checkbinaryplusop.invalidtype", //NOI18N
                        String.valueOf(type)));
            }
            ast.setType(VALUE);
            ast.setValue(value);
            ast.setFirstChild(null);
        }
        return ast;
    }
    
    /**
     * Check a string concatenation operation (CONCAT) for constant operands
     * that could be optimized.
     * @param op the concat operator
     * @param left the left operand
     * @param right the right operand
     * @return optimized JQLAST 
     */
    protected JQLAST checkConcatOp(JQLAST op, JQLAST left, JQLAST right)
    {
        JQLAST ast = op;

        if ((left.getType() == VALUE) && (right.getType() == VALUE))
        {
            Object leftValue = left.getValue();
            Object rightValue = right.getValue();
            Object value = null;
            if (leftValue == null)
                value = rightValue;
            else if (rightValue == null)
                value = leftValue;
            else 
                value = leftValue.toString() + rightValue.toString();
            ast.setType(VALUE);
            ast.setValue(value);
            ast.setFirstChild(null);
        }
        return ast;
    }

    /**
     * Check a binary minus operation (MINUS) for constant operands
     * that could be optimized.
     * @param op the minus operator
     * @param left the left operand
     * @param right the right operand
     * @return optimized JQLAST 
     */
    protected JQLAST checkBinaryMinusOp(JQLAST op, JQLAST left, JQLAST right)
    {
        JQLAST ast = op;

        if ((left.getType() == VALUE) && (right.getType() == VALUE))
        {
            Object leftValue = left.getValue();
            Object rightValue = right.getValue();
            Object value = null;
            if (rightValue == null)
                value = leftValue;
            else
            {
                if (leftValue == null)
                    leftValue = new Integer(0);
                
                Type type = op.getJQLType();
                
                if (type instanceof NumericWrapperClassType)
                    type = ((NumericWrapperClassType)type).getPrimitiveType();
                
                if (type.equals(typetab.intType))
                    value = new Integer(((Number)leftValue).intValue() -
                        ((Number)rightValue).intValue());
                else if (type.equals(typetab.longType))
                    value = new Long(((Number)leftValue).longValue() -
                        ((Number)rightValue).longValue());
                else if (type.equals(typetab.floatType))
                    value = new Float(((Number)leftValue).floatValue() - 
                        ((Number)rightValue).floatValue());
                else if (type.equals(typetab.doubleType))
                    value = new Double(((Number)leftValue).doubleValue() - 
                        ((Number)rightValue).doubleValue());
                else if (type.equals(typetab.bigDecimalType))
                    value = getBigDecimalValue(leftValue).subtract(
                       getBigDecimalValue(rightValue));
                else if (type.equals(typetab.bigIntegerType))
                    value = getBigIntegerValue(leftValue).subtract(
                        getBigIntegerValue(rightValue));
                else 
                    errorMsg.fatal(I18NHelper.getMessage(messages,
                        "jqlc.optimizer.checkbinaryminusop.invalidtype", //NOI18N
                        String.valueOf(type)));
            }
            ast.setType(VALUE);
            ast.setValue(value);
            ast.setFirstChild(null);
        }
        return ast;
    }
    
    /**
     * Check a binary multiplication operation (STAR) for constant operands
     * that could be optimized.
     * @param op the multiplication operator
     * @param left the left operand
     * @param right the right operand
     * @return optimized JQLAST 
     */
    protected JQLAST checkMultiplicationOp(JQLAST op, JQLAST left, JQLAST right)
    {
        JQLAST ast = op;

        if ((left.getType() == VALUE) && (right.getType() == VALUE))
        {
            Object leftValue = left.getValue();
            Object rightValue = right.getValue();
            Object value = null;
            if (leftValue == null)
                leftValue = new Integer(0);
            if (rightValue == null)
                rightValue = new Integer(0);
            Type type = op.getJQLType();
                
            if (type instanceof NumericWrapperClassType)
                type = ((NumericWrapperClassType)type).getPrimitiveType();
                
            if (type.equals(typetab.intType))
                value = new Integer(((Number)leftValue).intValue() *
                    ((Number)rightValue).intValue());
            else if (type.equals(typetab.longType))
                value = new Long(((Number)leftValue).longValue() *
                    ((Number)rightValue).longValue());
            else if (type.equals(typetab.floatType))
                value = new Float(((Number)leftValue).floatValue() * 
                    ((Number)rightValue).floatValue());
            else if (type.equals(typetab.doubleType))
                value = new Double(((Number)leftValue).doubleValue() * 
                    ((Number)rightValue).doubleValue());
            else if (type.equals(typetab.bigDecimalType))
                value = getBigDecimalValue(leftValue).multiply(
                    getBigDecimalValue(rightValue));
            else if (type.equals(typetab.bigIntegerType))
                value = getBigIntegerValue(leftValue).multiply(
                    getBigIntegerValue(rightValue));
            else 
                errorMsg.fatal(I18NHelper.getMessage(messages,
                    "jqlc.optimizer.checkmultiplicationop.invalidtype", //NOI18N
                    String.valueOf(type)));

            ast.setType(VALUE);
            ast.setValue(value);
            ast.setFirstChild(null);
        }
        return ast;
    }
    
    /**
     * Check a binary division operation (DIV) for constant operands
     * that could be optimized.
     * @param op the division operator
     * @param left the left operand
     * @param right the right operand
     * @return optimized JQLAST 
     */
    protected JQLAST checkDivisionOp(JQLAST op, JQLAST left, JQLAST right)
    {
        JQLAST ast = op;

        if ((left.getType() == VALUE) && (right.getType() == VALUE))
        {
            Object leftValue = left.getValue();
            Object rightValue = right.getValue();
            Object value = null;
            if (leftValue == null)
                leftValue = new Integer(0);
            if (rightValue == null)
                // division by zero!
                rightValue = new Integer(0);

            Type type = op.getJQLType();
            
            if (type instanceof NumericWrapperClassType)
                type = ((NumericWrapperClassType)type).getPrimitiveType();
                
            if (type.equals(typetab.intType))
                value = new Integer(((Number)leftValue).intValue() /
                    ((Number)rightValue).intValue());
            else if (type.equals(typetab.longType))
                value = new Long(((Number)leftValue).longValue() /
                    ((Number)rightValue).longValue());
            else if (type.equals(typetab.floatType))
                value = new Float(((Number)leftValue).floatValue() / 
                    ((Number)rightValue).floatValue());
            else if (type.equals(typetab.doubleType))
                value = new Double(((Number)leftValue).doubleValue() / 
                    ((Number)rightValue).doubleValue());
            else if (type.equals(typetab.bigDecimalType))
                value = getBigDecimalValue(leftValue).divide(
                   getBigDecimalValue(rightValue), BigDecimal.ROUND_HALF_EVEN);
            else if (type.equals(typetab.bigIntegerType))
                value = getBigIntegerValue(leftValue).divide(
                    getBigIntegerValue(rightValue));
            else 
                errorMsg.fatal(I18NHelper.getMessage(messages,
                    "jqlc.optimizer.checkdivisionop.invalidtype", //NOI18N
                    String.valueOf(type)));

            ast.setType(VALUE);
            ast.setValue(value);
            ast.setFirstChild(null);
        }
        return ast;
    }
    
    /**
     * Check a binary modular operation (MOD) for constant operands
     * that could be optimized.
     * @param op the mod operator
     * @param left the left operand
     * @param right the right operand
     * @return optimized JQLAST 
     */
    protected JQLAST checkModOp(JQLAST op, JQLAST left, JQLAST right)
    {
        JQLAST ast = op;

        if ((left.getType() == VALUE) && (right.getType() == VALUE))
        {
            Object leftValue = left.getValue();
            Object rightValue = right.getValue();
            Object value = null;
            if (leftValue == null)
                leftValue = new Integer(0);
            if (rightValue == null)
                // division by zero!
                rightValue = new Integer(0);

            Type type = op.getJQLType();
            
            if (type instanceof NumericWrapperClassType)
                type = ((NumericWrapperClassType)type).getPrimitiveType();
                
            if (type.equals(typetab.intType))
                value = new Integer(((Number)leftValue).intValue() %
                    ((Number)rightValue).intValue());
            else if (type.equals(typetab.longType))
                value = new Long(((Number)leftValue).longValue() %
                    ((Number)rightValue).longValue());
            else if (type.equals(typetab.floatType))
                value = new Float(((Number)leftValue).floatValue() % 
                    ((Number)rightValue).floatValue());
            else if (type.equals(typetab.doubleType))
                value = new Double(((Number)leftValue).doubleValue() % 
                    ((Number)rightValue).doubleValue());
            else if (type.equals(typetab.bigDecimalType))
            {
                BigDecimal leftBigDecimal = getBigDecimalValue(leftValue);
                BigDecimal rightBigDecimal = getBigDecimalValue(rightValue);
                //use ROUND_HALF_EVEN so that it is consistent with div
                BigDecimal quotient = leftBigDecimal.divide(rightBigDecimal,
                    0, BigDecimal.ROUND_HALF_EVEN);
                value = leftBigDecimal.subtract(
                    rightBigDecimal.multiply(quotient));
            }
            else if (type.equals(typetab.bigIntegerType))
                value = getBigIntegerValue(leftValue).remainder(
                    getBigIntegerValue(rightValue));
            else 
                errorMsg.fatal(I18NHelper.getMessage(messages,
                    "jqlc.optimizer.checkmodop.invalidtype", //NOI18N
                    String.valueOf(type)));

            ast.setType(VALUE);
            ast.setValue(value);
            ast.setFirstChild(null);
        }
        return ast;
    }

    /**
     * Check a unary minus operation (UNARY_MINUS) for a constant operand
     * that could be optimized.
     * @param op the unary minus operator
     * @param arg the operand
     * @return optimized JQLAST 
     */
    protected JQLAST checkUnaryMinusOp(JQLAST op, JQLAST arg)
    {
        JQLAST ast = op;
        
        if (arg.getType() == VALUE)
        {
            Object value = arg.getValue();
            Type type = op.getJQLType();
            Object negate = null;
            
            if (type instanceof NumberType)
                negate = ((NumberType)type).negate((Number)value);
            else 
                errorMsg.fatal(I18NHelper.getMessage(messages,
                    "jqlc.optimizer.checkunaryminusop.invalidtype", //NOI18N
                    String.valueOf(type)));
            
            ast.setType(VALUE);
            ast.setValue(negate);
            ast.setFirstChild(null);
        }
        return ast;
    }

    /**
     * Check a cast operation for a constant operand
     * that could be optimized.
     * @param op the cast operator
     * @param castType the cast type
     * @param expr the non constant operand
     * @return optimized JQLAST 
     */
    protected JQLAST checkCastOp(JQLAST op, JQLAST castType, JQLAST expr)
    {
        JQLAST ast = op;
        
        if (expr.getType() == VALUE)
        {
            Object value = expr.getValue();
            Type type = op.getJQLType();
            if (type instanceof NumericWrapperClassType)
                type = ((NumericWrapperClassType)type).getPrimitiveType();
                
            if (type.equals(typetab.intType))
                value = new Integer(((Number)value).intValue());
            else if (type.equals(typetab.longType))
                value = new Long(((Number)value).longValue());
            else if (type.equals(typetab.floatType))
                value = new Float(((Number)value).floatValue());
            else if (type.equals(typetab.doubleType))
                value = new Double(((Number)value).doubleValue());
            else if (type.equals(typetab.bigDecimalType))
                value = getBigDecimalValue(value);
            else if (type.equals(typetab.bigIntegerType))
                value = getBigIntegerValue(value);
            else if (type.equals(typetab.byteType))
                value = new Byte((byte)((Number)value).intValue());
            else if (type.equals(typetab.shortType))
                value = new Short((short)((Number)value).intValue());
            else if (type.equals(typetab.charType))
                value = new Character((char)((Number)value).intValue());
            
            // If non of the above type applies, leave the value as it is

            // convert the TYPECAST op into a VALUE
            ast.setType(VALUE);
            ast.setValue(value);
            ast.setFirstChild(null);
        }
        return ast;
    }

    /**
     * Converts the specified value into a BigDecimal value. 
     * @param value value to be converted
     * @return BigDecimal representation
     */
    protected BigDecimal getBigDecimalValue(Object value)
    {
        BigDecimal ret = null;
        if (value instanceof Number)
            ret = (BigDecimal)typetab.bigDecimalType.getValue((Number)value);
        else
            errorMsg.fatal(I18NHelper.getMessage(messages,
                "jqlc.optimizer.getbigdecimalvalue.notnumber", //NOI18N
                String.valueOf(value)));

        return ret;
    }

    /**
     * Converts the specified value into a BigInteger value. 
     * @param value value to be converted
     * @return BigInteger representation
     */
    protected BigInteger getBigIntegerValue(Object value)
    {
        BigInteger ret = null;

        if (value instanceof Number)
            ret = (BigInteger)typetab.bigIntegerType.getValue((Number)value);
        else
            errorMsg.fatal(I18NHelper.getMessage(messages,
                "jqlc.optimizer.getbigintegervalue.notnumber", //NOI18N
                String.valueOf(value)));

        return ret;
    }
    
    /**
     * This method is called in the case of an equality operation having two 
     * constant operands. It calculates the result of this constant operation 
     * and returns a JQLAST node representing a constant boolean value.
     * @param op the equality operator
     * @param left the left operand
     * @param right the right operand
     * @param negate true for not equal operation, false otherwise
     * @return optimized JQLAST 
     */
    protected JQLAST handleValueEqValue(JQLAST op, JQLAST left, JQLAST right, 
                                        boolean negate)
    {
        Object leftValue = left.getValue();
        Object rightValue = right.getValue();
        boolean value = false;
        
        if ((leftValue == null) && (rightValue == null))
        {
            // both values are null -> true
            value = true;
        }
        else if ((leftValue != null) && (rightValue != null))
        {
            // both values are not null -> use equals
            value = leftValue.equals(rightValue);
        }
        else
        {
            // one value is null, the other is not null -> false
            value = false;
        }
        if (negate) 
        {
            value = !value;
        }
        op.setType(VALUE);
        op.setValue(new Boolean(value));
        op.setFirstChild(null);
        return op;
    }

    /**
     * This method is called in the case of an equality operation having 
     * a boolean constant operand and a non constant operand. 
     * It returns the non constant operand either as it is or inverted, 
     * depending on the equality operation.
     * @param op the equality operator
     * @param value the contant boolean value
     * @param expr the non constant operand
     * @param negate true for not equal operation, false otherwise
     * @return optimized JQLAST 
     */
    private JQLAST handleBooleanValueEqExpr(JQLAST op, Object value, 
                                            JQLAST expr, boolean negate)
    {
        JQLAST ast;
        boolean skip = (value instanceof Boolean) ? 
                       ((Boolean)value).booleanValue() : false;
        if (negate) skip = !skip;

        if (skip)
        {
            // expr == true -> expr
            // expr != false -> expr
            ast = expr;
        }
        else 
        {
            // if expr is a equality op or a not op the invert operation may be "inlined":
            //   (expr1 == expr2) != true -> expr1 != expr2
            //   (expr1 != expr2) != true -> expr1 == expr2
            //   !expr != true -> expr
            //   !expr == false -> expr
            // Otherwise wrap the expr with a not op
            //   expr != true -> !expr
            //   expr == false -> !expr
            switch (expr.getType())
            {
            case EQUAL:
                expr.setType(NOT_EQUAL);
                expr.setText("!="); //NOI18N
                ast = expr;
                break;
            case NOT_EQUAL:
                expr.setType(EQUAL);
                expr.setText("=="); //NOI18N
                ast = expr;
                break;
            case LNOT:
                ast = (JQLAST)expr.getFirstChild();
                break;
            default:
                op.setType(LNOT);
                op.setText("!"); //NOI18N
                op.setFirstChild(expr);
                ast = op;
            }
            expr.setNextSibling(null);
        }
        return ast;
    }

    /**
     * This method is called in the case of an AND operation having at least 
     * one constant operand. If the constant operand evaluates to true it 
     * returns the other operand. If it evaluates to false it returns an AST
     * representing the constant boolean value false.
     * @param op the AND operator
     * @param left the left operand
     * @param right the right operand
     * @return optimized JQLAST 
     */
    private JQLAST handleValueAndExpr(JQLAST op, Object value, JQLAST expr)
    {
        JQLAST ast;

        if ((value instanceof Boolean) && ((Boolean)value).booleanValue())
        {
            // true AND expr -> expr
            // expr AND true -> expr
            expr.setNextSibling(null);
            ast = expr;
        }
        else
        {
            // false AND expr -> false
            // expr AND false -> false
            op.setType(VALUE);
            op.setText("false"); //NOI18N
            op.setValue(new Boolean(false));
            op.setFirstChild(null);
            ast = op;
        }
        return ast;
    }

    /**
     * This method is called in the case of an OR operation having at least 
     * one constant operand. If the constant operand evaluates to false it 
     * returns the other operand. If it evaluates to true it returns an AST
     * representing the constant boolean value true.
     * @param op the AND operator
     * @param left the left operand
     * @param right the right operand
     * @return optimized JQLAST 
     */
    private JQLAST handleValueOrExpr(JQLAST op, Object value, JQLAST expr)
    {
        JQLAST ast;

        if ((value instanceof Boolean) && ((Boolean)value).booleanValue())
        {
            // true OR expr -> true
            // expr OR true -> true
            op.setType(VALUE);
            op.setText("true"); //NOI18N
            op.setValue(new Boolean(true));
            op.setFirstChild(null);
            ast = op;
        }
        else
        {
            // false OR expr -> expr
            // expr OR false -> expr
            expr.setNextSibling(null);
            ast = expr;
        }
        return ast;
    }

    /**
     * Returns true if the specified AST represents a constant boolean value.
     */
    protected boolean isBooleanValueAST(JQLAST ast)
    {
        return (ast.getType() == VALUE) && 
               (typetab.booleanType.equals(ast.getJQLType()));
    }

    /**
     * Returns true if the specified AST represents a datastore value. 
     */
    protected boolean isNonConstantCollection(JQLAST ast)
    {
        switch (ast.getType())
        {
        case FIELD_ACCESS :
        case NAVIGATION :
            return true;
        case TYPECAST :
            JQLAST expr = (JQLAST)ast.getFirstChild().getNextSibling();
            return isNonConstantCollection(expr);
        default:
            return false;
        }
    }

    /**
     * Implements DeMorgans rule: 
     * <br>
     * NOT (a AND b) -> NOT a OR NOT b
     * <br>
     * NOT (a OR b) -> NOT a AND NOT b
     * <br>
     * NOT (NOT a) -> a
     * <br>
     * The method assumes that the tree passed as an argument does not include 
     * the initial NOT. Note, this method checks for contains clauses, because 
     * they require special treatement.
     */
    protected JQLAST deMorgan(JQLAST tree)
    {
        JQLAST result = null;
        JQLAST left = null;
        JQLAST right = null;
        switch (tree.getType()) 
        {
        case AND:
        case BAND:
            left = (JQLAST)tree.getFirstChild();
            right = (JQLAST)left.getNextSibling();
            String leftVar = getVariableFromContainsClause(left);
            String rightVar = getVariableFromContainsClause(right);
            if (leftVar != null)
            {
                // found AND ( CONTAINS, right ), so check right for special
                // variable treatement
                result = buildAST(tree, left, deMorgan(right, leftVar));
            }
            else if (rightVar != null)
            {
                // found AND ( left, CONTAINS, ), so check left for special
                // variable treatement
                result = buildAST(tree, right, deMorgan(left, rightVar));
            }
            else
            {
                invertNode(tree);
                result = buildAST(tree, deMorgan(left), deMorgan(right));
            }
            break;
        case OR:
        case BOR:
            left = (JQLAST)tree.getFirstChild();
            right = (JQLAST)left.getNextSibling();
            invertNode(tree);
            result = buildAST(tree, deMorgan(left), deMorgan(right));
            break;
        case LNOT:
            // This is !(!arg) => return arg
            result = (JQLAST)tree.getFirstChild();
            break;
        default:
            // wrap arg into not operator
            result = buildAST(new JQLAST(LNOT, "!", typetab.booleanType), tree);
            break;
        }       
        return result;
    }

    /**
     * This overloaded deMorgan method implements special treatment of variable 
     * access expressions in the case of !contains. The method keeps an expression
     * accessing the specified variable as it is, but it inverts an expression NOT
     * accessing the variable following regular DeMorgan rules.
     */
    protected JQLAST deMorgan(JQLAST tree, String var)
    {
        JQLAST result = tree;
        switch (tree.getType()) 
        {
        case AND:
        case BAND:
        case OR:
        case BOR:
            JQLAST left = (JQLAST)tree.getFirstChild();
            JQLAST right = (JQLAST)left.getNextSibling();
            if (!includesVariableAccess(left, var) ||
                !includesVariableAccess(right, var))
            {
                invertNode(tree);
            }
            result = buildAST(tree, deMorgan(left, var), deMorgan(right, var));
            break;
        default:
            if (!includesVariableAccess(tree, var))
            {
                result = deMorgan(tree);
            }
            break;
        }
        return result;
    }

    /** 
     * Checks the specified tree being a CONATAINS clause. If yes it returns 
     * the variable used in the contains clause. Otherwise it returns null.
     */
    protected String getVariableFromContainsClause(JQLAST tree)
    {
        switch (tree.getType())
        {
        case CONTAINS:
        case NOT_CONTAINS:
            return tree.getFirstChild().getNextSibling().getText();
        default:
            return null;
        }
    }
    
    /**
     * Checks whether the specified tree accesses the variable with the 
     * specified name. Accessing means either this node or of of the subnodes 
     * has the type VARIABLE. 
     * NOTE, the method is intended to be used in the ! contains case only!
     * If it find a variable access node of the form
     * <br>
     * #(VARIABLE collection)
     * it maps it to
     * #(VARIABLE #(NOT_IN (collection))
     * <br>
     * This incdicates a variable belonging to a !contains clause.
     */
    protected boolean includesVariableAccess(AST tree, String var)
    {
        if ((tree == null) || (var == null))
            return false;

        boolean found = false;
        JQLAST child = (JQLAST)tree.getFirstChild();
        if ((tree.getType() == VARIABLE) && (tree.getText().equals(var)) && 
            (child != null))
        {
            found = true;
            if (child.getType() != NOT_IN)
            {
                tree.setFirstChild(buildAST(
                    new JQLAST(NOT_IN, "notIn", typetab.booleanType), child));
            }
        }
        for (AST node = tree.getFirstChild(); node != null; node = node.getNextSibling())
        {
            if (includesVariableAccess(node, var))
                found = true;
        }
        return found;
    }

    /** 
     * Inverts the specified node: AND -> OR, == -> !=, etc.
     */
    protected void invertNode(JQLAST node)
    {
        switch(node.getType())
        {
        case AND:
            node.setType(OR);
            node.setText("||");
            break;
        case BAND:
            node.setType(BOR);
            node.setText("|");
            break;
        case OR:
            node.setType(AND);
            node.setText("&&");
            break;
        case BOR:
            node.setType(BAND);
            node.setText("&");
            break;
        case EQUAL:
            node.setType(NOT_EQUAL);
            node.setText("!=");
            break;
        case NOT_EQUAL:
            node.setType(EQUAL);
            node.setText("==");
            break;
        case LT:
            node.setType(GE);
            node.setText(">=");
            break;
        case LE:
            node.setType(GT);
            node.setText(">");
            break;
        case GT:
            node.setType(LE);
            node.setText("<=");
            break;
        case GE:
            node.setType(LT);
            node.setText("<");
            break;
        }
    }

    /** Builds a binary tree. */
    protected JQLAST buildAST(JQLAST root, JQLAST left, JQLAST right)
    {
        root.setFirstChild(left);
        left.setNextSibling(right);
        right.setNextSibling(null);
        return root;
    }

    /** */
    protected JQLAST buildAST(JQLAST root, JQLAST arg)
    {
        root.setFirstChild(arg);
        arg.setNextSibling(null);
        return root;
    }
}

// rules

query
    :   #(  q:QUERY
            candidateClass
            parameters
            variables
            ordering
            result
            filter
        )
    ;

// ----------------------------------
// rules: candidate class
// ----------------------------------

candidateClass
{   
    errorMsg.setContext("setCandidates"); //NOI18N
}
    :   CLASS_DEF
    ;

// ----------------------------------
// rules: parameter declaration
// ----------------------------------

parameters
{   
    errorMsg.setContext("declareParameters"); //NOI18N
}
    :   ( declareParameter )*
    ;

declareParameter
    :   #( PARAMETER_DEF type IDENT )
    ;

// ----------------------------------
// rules: variable declaration
// ----------------------------------

variables 
{ 
    errorMsg.setContext("declareVariables"); //NOI18N
}
    :   ( declareVariable )*
    ;

declareVariable
    :   #( VARIABLE_DEF type IDENT )
    ;

// ----------------------------------
// rules: ordering specification
// ----------------------------------

ordering 
{   
    errorMsg.setContext("setOrdering"); //NOI18N
}
    :   ( orderSpec )*
    ;

orderSpec
    :   #(  ORDERING_DEF ( ASCENDING | DESCENDING ) expression )
    ;

// ----------------------------------
// rules: result expression
// ----------------------------------

result
{   
    errorMsg.setContext("setResult"); //NOI18N
}
    :   #( r:RESULT_DEF resultExpr )
    |   // empty rule
    ;

resultExpr
    :   #( DISTINCT resultExpr )
    |   #( AVG resultExpr )
    |   #( MAX resultExpr )
    |   #( MIN resultExpr )
    |   #( SUM resultExpr )
    |   #( COUNT resultExpr )
    |   expression
    ;

// ----------------------------------
// rules: filer expression
// ----------------------------------

filter
{   
    errorMsg.setContext("setFilter"); //NOI18N
}
    :   #( FILTER_DEF expression )
    ;

expression 
    :   primary
    |   bitwiseExpr
    |   conditionalExpr
    |   relationalExpr
    |   binaryArithmeticExpr
    |   unaryArithmeticExpr
    |   complementExpr
    ;

bitwiseExpr 
    :   #( op1:BAND left1:expression right1:expression )
        {
            #bitwiseExpr = checkAnd(#op1, #left1, #right1);
        }
    |   #( op2:BOR  left2:expression right2:expression )
        {
            #bitwiseExpr = checkOr(#op2, #left2, #right2);
        }
    |   #( op3:BXOR left3:expression right3:expression )
    ;

conditionalExpr 
    :   #( op1:AND left1:expression right1:expression )
        {
            #conditionalExpr = checkAnd(#op1, #left1, #right1);
        }
    |   #( op2:OR  left2:expression right2:expression )
        {
            #conditionalExpr = checkOr(#op2, #left2, #right2);
        }
   ;

relationalExpr
    :   #( op1:EQUAL left1:expression right1:expression )
        {
            #relationalExpr = checkEqualityOp(#op1, #left1, #right1, false);
        }
    |   #( op2:NOT_EQUAL left2:expression right2:expression )
        {
            #relationalExpr = checkEqualityOp(#op2, #left2, #right2, true);
        }
    |   #( op3:OBJECT_EQUAL left3:expression right3:expression ) 
        {
            #relationalExpr = checkObjectEqualityOp(#op3, #left3, #right3, false);
        }
    |   #( op4:OBJECT_NOT_EQUAL left4:expression right4:expression )
        {
            #relationalExpr = checkObjectEqualityOp(#op4, #left4, #right4, true);
        }
    |   #( op5:COLLECTION_EQUAL left5:expression right5:expression )
        {
            #relationalExpr = checkCollectionEqualityOp(#op5, #left5, #right5, false);
        }
    |   #( op6:COLLECTION_NOT_EQUAL left6:expression right6:expression )
        {
            #relationalExpr = checkCollectionEqualityOp(#op6, #left6, #right6, true);
        }
    |   #( LT expression expression )
    |   #( GT expression expression )
    |   #( LE expression expression )
    |   #( GE expression expression )
    ;

binaryArithmeticExpr 
    :   #( op1:PLUS   left1:expression right1:expression )
        {
            #binaryArithmeticExpr = checkBinaryPlusOp(#op1, #left1, #right1);
        }
    |   #( op2:CONCAT left2:expression right2:expression )
        {
            #binaryArithmeticExpr = checkConcatOp(#op2, #left2, #right2);
        }
    |   #( op3:MINUS  left3:expression right3:expression )
        {
            #binaryArithmeticExpr = checkBinaryMinusOp(#op3, #left3, #right3);
        }
    |   #( op4:STAR   left4:expression right4:expression )
        {
            #binaryArithmeticExpr = checkMultiplicationOp(#op4, #left4, #right4);
        }
    |   #( op5:DIV    left5:expression right5:expression )
        {
            #binaryArithmeticExpr = checkDivisionOp(#op5, #left5, #right5);
        }
    |   #( op6:MOD    left6:expression right6:expression )
        {
            #binaryArithmeticExpr = checkModOp(#op6, #left6, #right6);
        }
    ;

unaryArithmeticExpr 
    :   #( UNARY_PLUS expression )
    |   ( unaryMinusLiteralExpr )=> unaryMinusLiteralExpr
    |   #( op2:UNARY_MINUS arg2:expression )
        {
            #unaryArithmeticExpr = checkUnaryMinusOp(#op2, #arg2);
        }
    ;

unaryMinusLiteralExpr
    :   #( UNARY_MINUS ( i:INT_LITERAL | l:LONG_LITERAL ) )
        {
            JQLAST li = (#i != null) ? #i : #l;
            li.setText("-" + li.getText());
            // calling literal here directly does not work properly
            // the following logic need to be in sync with that of literal
            li.setValue(#literalHelper(li));
            li.setType(VALUE);
            #unaryMinusLiteralExpr = #li;
        }
    ;

complementExpr 
    :   #( op1:BNOT arg1:expression )
    |   #( op2:LNOT arg2:expression )
        {
            #complementExpr = checkLogicalNotOp(#op2, #arg2);
        }
    ;

primary 
    :   castExpr
    |   literal
    |   VALUE
    |   THIS
    |   parameter
    |   staticFieldAccess
    |   fieldAccess
    |   navigation
    |   variableAccess
    |   #( CONTAINS expression VARIABLE )
    |   #( NOT_CONTAINS expression VARIABLE )
    |   startsWith
    |   endsWith
    |   isEmpty
    |   like
    |   substring
    |   indexOf
    |   length
    |   abs
    |   sqrt
    ;

castExpr
    :   #( c:TYPECAST t:type e:expression )
        {
            #castExpr = checkCastOp(#c, #t, #e);
        }
    ;

literal
{ 
    Object value = null; 
}
    :   value = l:literalHelper
        {
            #l.setType(VALUE);
            #l.setValue(value);
        }
    ;

literalHelper returns [Object value]
{ 
    value = null; 
}
    :   TRUE
        { value = new Boolean(true); }
    |   FALSE
        { value = new Boolean(false); }
    |   i:INT_LITERAL
        {
            try
            {
                value = Integer.decode(i.getText());
            }
            catch (NumberFormatException ex)
            {
                errorMsg.error(i.getLine(), i.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.optimizer.literal.invalid",  //NOI18N
                        i.getJQLType().getName(), i.getText()));
            }
        }
    |   l:LONG_LITERAL
        {   
            String txt = l.getText();
            char last = txt.charAt(txt.length() - 1);
            if ((last == 'l') || (last == 'L'))
            {
                txt = txt.substring(0, txt.length() - 1);
            }
            try
            {
                value = Long.decode(txt);
            }
            catch (NumberFormatException ex)
            {
                errorMsg.error(l.getLine(), l.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.optimizer.literal.invalid",  //NOI18N
                        l.getJQLType().getName(), l.getText()));
            }
        }
    |   f:FLOAT_LITERAL
        {  
            String txt = f.getText();
            char last = txt.charAt(txt.length() - 1);
            if ((last == 'f') || (last == 'F'))
            {
                txt = txt.substring(0, txt.length() - 1);
            }
            try
            {
                value = new Float(txt);
            }
            catch (NumberFormatException ex)
            {
                errorMsg.error(f.getLine(), f.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.optimizer.literal.invalid",  //NOI18N
                        f.getJQLType().getName(), f.getText()));
            }
        }
    |   d:DOUBLE_LITERAL
        {  
            String txt = d.getText();
            char last = txt.charAt(txt.length() - 1);
            if ((last == 'd') || (last == 'd'))
            {
                txt = txt.substring(0, txt.length() - 1);
            }
            try
            {
                value = new Double(txt);
            }
            catch (NumberFormatException ex)
            {
                errorMsg.error(d.getLine(), d.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.optimizer.literal.invalid",  //NOI18N
                        d.getJQLType().getName(), d.getText()));
            }
        }
    |   c:CHAR_LITERAL
        { value = new Character(parseChar(c.getText())); }
    |   s:STRING_LITERAL
        { value = s.getText(); }
    |   n:NULL
        { value = null; }
    ;

parameter
    :   p:PARAMETER
        { 
            if (paramtab.inline(#p.getText())) {
                #p.setType(VALUE);
                #p.setValue(paramtab.getValueByName(#p.getText()));     
            }
        }
    ;

staticFieldAccess
{   
    Object value = null; 
}
    :   #( s:STATIC_FIELD_ACCESS t:TYPENAME i:IDENT)
        {
            // Calculate the value of the static field at compile time
            // and treat it as constant value.
            ClassType classType = (ClassType)t.getJQLType();
            FieldInfo fieldInfo = classType.getFieldInfo(i.getText());
            try
            {
                value = fieldInfo.getField().get(null);
                #s.setType(VALUE);
                #s.setValue(value);
                #s.setFirstChild(null);
            }
            catch (IllegalAccessException e) 
            {
                throw new JDOFatalUserException(
                    I18NHelper.getMessage(messages, 
                        "jqlc.optimizer.staticfieldaccess.illegal",  //NOI18N
                        i.getText(), classType.getName()), e);
            }
        }
    ;

fieldAccess
    :   #( f:FIELD_ACCESS o:expression name:IDENT )
        {
            if (#o.getType() == VALUE)
            {
                // If the object of the field access is a constant value, 
                // evaluate the field access at compile time and 
                // treat the expression as constant value.
                Object object = #o.getValue();
                ClassType classType = (ClassType)#o.getJQLType();
                Object value = CodeGeneration.getFieldValue(classType, object, 
                                                            #name.getText());
                #f.setType(VALUE);
                #f.setValue(value);
                #f.setFirstChild(null);
            }
        }
    ;

navigation
    :   #(  n:NAVIGATION o:expression name:IDENT )
        {
            if (#o.getType() == VALUE)
            {
                // If the object of the navigation is a constant value, 
                // evaluate the field access at compile time and 
                // treat the expression as constant value.
                Object object = #o.getValue();
                ClassType classType = (ClassType)#o.getJQLType();
                Object value = CodeGeneration.getFieldValue(classType, object, 
                                                            #name.getText());
                #n.setType(VALUE);
                #n.setValue(value);
                #n.setFirstChild(null);
            }
        }
    ;

variableAccess
    :   #( VARIABLE ( expression )? )
    ;

startsWith
    :   #( STARTS_WITH expression expression ) 
    ;

endsWith
    :   #( ENDS_WITH expression expression ) 
    ;

isEmpty
    :   #( op:IS_EMPTY e:expression)
        {
            if (#e.getType() == VALUE)
            {
                // If the expression that specifies the collection is a constant value, 
                // evaluate the isEmpty call at compile time and treat the expression 
                // as constant value.
                Object object = #e.getValue();
                Object value = null;
                if (object == null)
                {
                    value = new Boolean(false);
                }
                else if (object instanceof Collection)
                {
                    value = new Boolean(((Collection)object).isEmpty());
                }
                else
                {
                    errorMsg.fatal(I18NHelper.getMessage(messages, "jqlc.optimizer.isempty.requirecollection")); //NOI18N
                }
                #op.setType(VALUE);
                #op.setValue(value);
                #op.setFirstChild(null);
            }
        }
    ;

like
    :   #( LIKE expression expression ( expression )? ) 
    ;

substring
    :   #( SUBSTRING expression expression expression ) 
    ;

indexOf
    :   #( INDEXOF expression expression ( expression )? ) 
    ;

length
    :   #( LENGTH expression )
    ;

abs
    :   #( ABS expression )
    ;

sqrt
    :   #( SQRT expression )
    ;

// ----------------------
// types
// ----------------------

type
    :   TYPENAME
    |   primitiveType
    ;

primitiveType
    :   BOOLEAN
    |   BYTE
    |   CHAR
    |   SHORT
    |   INT
    |   FLOAT
    |   LONG
    |   DOUBLE
    ;
