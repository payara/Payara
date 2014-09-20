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
 * Semantic.g
 *
 * Created on November 19, 2001
 */

header
{
    package com.sun.jdo.spi.persistence.support.ejb.ejbqlc;
    
    import java.util.ResourceBundle;
    import java.lang.reflect.Method;
    import org.glassfish.persistence.common.I18NHelper;
    import com.sun.jdo.spi.persistence.support.ejb.ejbc.MethodHelper;
}

/**
 * This class defines the semantic analysis of the EJBQL compiler.
 * Input of this pass is the AST as produced by the parser,
 * that consists of EJBQLAST nodes.
 * The result is a typed EJBQLAST tree.
 *
 * @author  Michael Bouschen
 * @author  Shing Wai Chan
 */
class Semantic extends TreeParser;

options
{
    importVocab = EJBQL;
    buildAST = true;
    defaultErrorHandler = false;
    ASTLabelType = "EJBQLAST"; //NOI18N
}

{
    /** Name of the property to disable order by validation. */
    public static final String DISABLE_ORDERBY_VALIDATION_PROPERTY =
        "com.sun.jdo.spi.persistence.support.ejb.ejbqlc.DISABLE_ORDERBY_VALIDATION"; // NOI18N

    /**
     * Property to disable order by validation. 
     * Note, the default is false, meaning the compiler checks that select 
     * clause and orderby clause are compatible.
     */
    private static final boolean DISABLE_ORDERBY_VALIDATION = 
        Boolean.getBoolean(DISABLE_ORDERBY_VALIDATION_PROPERTY);

    /** Symbol table handling names of variables and parameters. */
    protected SymbolTable symtab;

    /** Type info access helper. */
    protected TypeSupport typeSupport;
    
    /** Parameter info helper. */
    protected ParameterSupport paramSupport;
    
    /** The Method instance of the finder/selector method. */
    protected Method method;

    /** result-type-mapping element from the DD. */
    protected int resultTypeMapping;

    /** Flag indicating finder or selector. */
    protected boolean finderNotSelector;

    /** Flag indicating have aggregate function or not. */
    protected boolean isAggregate = false;

    /** The ejb-name. */
    protected String ejbName;

    /** I18N support. */
    protected final static ResourceBundle msgs = I18NHelper.loadBundle(
        Semantic.class);
    
    /**
     * Initializes the semantic analysis.
     * @param typeSupport type info access helper.
     * @param paramSupport parameter info helper.
     * @param method method instance of the finder/selector method.
     * @param resultTypeMapping result-type-mapping element from the DD
     * @param finderNotSelector <code>true</code> for finder; 
     * <code>false</code> for selector
     * @param ejbName the ejb name of the finder/selector method.
     */
    public void init(TypeSupport typeSupport, ParameterSupport paramSupport,
                     Method method, int resultTypeMapping,  
                     boolean finderNotSelector, String ejbName)
    {
        this.symtab = new SymbolTable();
        this.typeSupport = typeSupport;
        this.paramSupport = paramSupport;
        this.method = method;
        this.resultTypeMapping = resultTypeMapping;
        this.finderNotSelector = finderNotSelector;
        this.ejbName = ejbName;
    }

    /** */
    public void reportError(RecognitionException ex) {
        ErrorMsg.fatal(I18NHelper.getMessage(msgs, "ERR_SemanticError"), ex); //NOI18N
    }

    /** */
    public void reportError(String s) {
        ErrorMsg.fatal(I18NHelper.getMessage(msgs, "ERR_SemanticError") + s); //NOI18N
    }
    
    //========= Internal helper methods ==========

    /**
     * Checks the return type and the type of the select clause expression 
     * of a finder method.
     * <p>
     * The return type of a finder must be one of the following: 
     * <ul>
     * <li>java.util.Collection (multi-object finder)
     * <li>java.util.Enumeration (EJB 1.1 multi-object finder)
     * <li>the entity bean's remote interface (single-object finder)
     * <li>the entity bean's local interface (single-object finder)
     * </ul>
     * The type of the select clause expression of a finder must be 
     * the entity bean's local or remote interface.
     * @param returnType the return type of the finder/selector method object
     * @param selectClauseTypeInfo the type info of the select clause 
     * expression. 
     */
    private void checkFinderReturnType(
        Class returnType, Object selectClauseTypeInfo)
    {
        String selectClauseTypeName = typeSupport.getTypeName(selectClauseTypeInfo);
        Object returnTypeInfo = typeSupport.getTypeInfo(returnType);
        // The return type of a finder must be Collection or Enumeration or
        // the entity bean's remote or local interface 
        if ((returnType != java.util.Collection.class) &&
            (returnType != java.util.Enumeration.class) &&
            (!typeSupport.isRemoteInterfaceOfEjb(returnTypeInfo, ejbName)) &&
            (!typeSupport.isLocalInterfaceOfEjb(returnTypeInfo, ejbName))) {
            ErrorMsg.error(I18NHelper.getMessage(msgs, 
                "EXC_InvalidFinderReturnType", returnType.getName())); //NOI18N
                    
        }
        
        // The type of the select clause expression must be the ejb name 
        // of this bean.
        if (!selectClauseTypeName.equals(this.ejbName)) {
            ErrorMsg.error(I18NHelper.getMessage(msgs, 
                "EXC_InvalidFinderSelectClauseType", selectClauseTypeName)); //NOI18N
        }
    }

    /**
     * Implements type compatibility for selector. The method returns
     * <code>true</code> if returnTypeInfo is compatible with 
     * selectClauseTypeInfo.
     */
    private boolean isCompatibleSelectorSelectorReturnType(
            Object returnTypeInfo, Object selectClauseTypeInfo)
    {
        if (isAggregate) {
            return getCommonOperandType(selectClauseTypeInfo, returnTypeInfo) != TypeSupport.errorType;   
        } else {
            return typeSupport.isCompatibleWith(selectClauseTypeInfo, returnTypeInfo);
        }
    }
    

    /**
     * Checks the return type and the type of the select clause expression 
     * of a selector method.
     * <p>
     * The return type of a selector must be one of the following: 
     * <ul>
     * <li>java.util.Collection (multi-object selector)
     * <li>java.util.Set (multi-object selector)
     * <li>assignable from the type of the select clause expression 
     * (single-object selector)
     * </ul>
     * @param returnType the return type of the finder/selector method object
     * @param selectClauseTypeInfo the type info of the select clause 
     * expression. 
     */
    private void checkSelectorReturnType(
        Class returnType, Object selectClauseTypeInfo)
    {
        String selectClauseTypeName = typeSupport.getTypeName(selectClauseTypeInfo);
        Object returnTypeInfo = typeSupport.getTypeInfo(returnType);
        // The return type of a selector must be Collection or Set or 
        // assingable from the type of the select clause expression
        if ((returnType != java.util.Collection.class) &&
            (returnType != java.util.Set.class) &&
            !isCompatibleSelectorSelectorReturnType(returnTypeInfo,
                selectClauseTypeInfo)) {
            ErrorMsg.error(I18NHelper.getMessage(msgs,
                "EXC_InvalidSelectorReturnType", //NOI18N
                typeSupport.getTypeName(returnTypeInfo), selectClauseTypeName)); 
        }
    }

    /**
     * Checks the result-type-mapping element setting in the case of a finder 
     * method. Finder must not specify result-type-mapping.
     */
    private void checkFinderResultTypeMapping()
    {
        if (resultTypeMapping != MethodHelper.NO_RETURN) {
            ErrorMsg.error(I18NHelper.getMessage(msgs, 
                "EXC_InvalidResultTypeMappingForFinder")); //NOI18N
        }
    }

    /**
     * Checks the setting of the result-type-mapping element for a 
     * selector. Only selectors returning a entity object may 
     * specify this.
     * <p>
     * The method checks the following error cases:
     * <ul>
     * <li>result-type-mapping is specified as Remote, 
     * but bean does not have remote interface
     * <li>result-type-mapping is specified as Local, 
     * but bean does not have local interface
     * <li>single-object selector returns remote interface,
     * but result-type-mapping is not specified as Remote
     * <li>single-object selector returns local interface,
     * but result-type-mapping is specified as Remote
     * <li>result-type-mapping is specified for a selector returning 
     * non-entity objects.
     * </ul>
     * @param returnType the return type of the finder/selector method object
     * @param selectClauseTypeInfo the type info of the select clause.
     */
    private void checkSelectorResultTypeMapping(
        Class returnType, Object selectClauseTypeInfo)
    {
        Object returnTypeInfo = typeSupport.getTypeInfo(returnType);

        // case: multi-object selector returning entity objects
        if (typeSupport.isCollectionType(returnTypeInfo) && 
            typeSupport.isEjbName(selectClauseTypeInfo)) {
            if (resultTypeMapping == MethodHelper.REMOTE_RETURN) {
                // result-type-mapping is Remote => 
                // bean must have remote interface
                if (!typeSupport.hasRemoteInterface(selectClauseTypeInfo)) {
                    ErrorMsg.error(I18NHelper.getMessage(msgs, 
                        "EXC_InvalidRemoteResultTypeMappingForMultiSelector", //NOI18N
                        selectClauseTypeInfo)); 
                }
            }
            else {
                // result-type-mapping is Local or not specified => 
                // bean must have local interface
                if (!typeSupport.hasLocalInterface(selectClauseTypeInfo)) {
                    ErrorMsg.error(I18NHelper.getMessage(msgs,
                        "EXC_InvalidLocalResultTypeMappingForMultiSelector", //NOI18N
                        selectClauseTypeInfo)); 
                }
            }
        }
        // case: single-object selector returning remote interface
        else if (typeSupport.isRemoteInterface(returnTypeInfo)) {
            // result-type-mapping must be Remote
            if (resultTypeMapping != MethodHelper.REMOTE_RETURN) {
                ErrorMsg.error(I18NHelper.getMessage(msgs,
                    "EXC_InvalidLocalResultTypeMappingForSingleSelector")); //NOI18N     
            }
        }
        // case: single-object selector returning local interface
        else if (typeSupport.isLocalInterface(returnTypeInfo)) {
            // result-type-mapping must be Local or not specified
            if (resultTypeMapping == MethodHelper.REMOTE_RETURN) {
                ErrorMsg.error(I18NHelper.getMessage(msgs,
                    "EXC_InvalidRemoteResultTypeMappingForSingleSelector")); //NOI18N     
            }
        }
        // cases: single-object and multi-object selector 
        // returning non-enity object(s)
        else if (resultTypeMapping != MethodHelper.NO_RETURN) {
            // result-type-mapping must not be specified
            ErrorMsg.error(I18NHelper.getMessage(msgs, 
                "EXC_InvalidResultTypeMappingForSelector", //NOI18N
                selectClauseTypeInfo)); 
        }
    }

    /**
     * Checks that select clause and orderby clause are compatible.
     * <p>
     * The method checks the following error cases:
     * <ul>
     * <li>if the select clause is an identification variable or
     * a single valued cmr path expression, then the orderby item
     * must be a cmp field of the entity bean abstract schema
     * type value returned by the SELECT clause
     * <li>if the select clause is a cmp field, then
     * orderby item must be empty or the same cmp field.
     * </ul>
     * @param select the select clause of the query
     * @param orderby the orderby clause of the query
     */
    private void checkSelectOrderbyClause(EJBQLAST select, EJBQLAST orderby)
    {
        // nothing to check if no orderby clause or 
        // if orderby validation is disabled
        if ((orderby == null) || DISABLE_ORDERBY_VALIDATION) {
            return;
        }

        AST selectReturnAST = select.getFirstChild();
        // skip DISTINCT node, so selectReturnAST should be one of the following:
        //     Object(x), cmr-field, cmp-field
        // it is illegal to be an aggregate function node
        if (selectReturnAST.getType() == DISTINCT) {
            selectReturnAST = selectReturnAST.getNextSibling();
        }

        if (selectReturnAST.getType() == CMP_FIELD_ACCESS) {
            StringBuffer buf = new StringBuffer();
            genPathExpression(selectReturnAST, buf);
            String selectReturnPathExpr = buf.toString();
            for (AST sibling = orderby.getFirstChild();
                    sibling != null;
                    sibling = sibling.getNextSibling().getNextSibling()) {

                // share buf
                buf.setLength(0);
                genPathExpression(sibling, buf);
                String siblingPathExpr = buf.toString();
                if (!selectReturnPathExpr.equals(siblingPathExpr)) {
                    ErrorMsg.error(I18NHelper.getMessage(msgs, 
                    "EXC_InvalidOrderbyItemForCMPSelect", //NOI18N
                    siblingPathExpr)); 
                }
            }
        } else {
            AST abstractSchemaAST = null;
            if (selectReturnAST.getType() == SINGLE_CMR_FIELD_ACCESS) {
                abstractSchemaAST = selectReturnAST;
            } else if (selectReturnAST.getType() == OBJECT) {
                abstractSchemaAST = selectReturnAST.getFirstChild();
            } else { // it must be an aggregate function node
                ErrorMsg.error(I18NHelper.getMessage(msgs,
                "EXC_InvalidAggregateOrderby" //NOI18N
                ));
            }

            StringBuffer buf = new StringBuffer();
            genPathExpression(abstractSchemaAST, buf);
            String abstractSchemaExpr = buf.toString();
            for (AST sibling = orderby.getFirstChild();
                    sibling != null;
                    sibling = sibling.getNextSibling().getNextSibling()) {

                // share  buf
                buf.setLength(0);
                genPathExpression(sibling.getFirstChild(), buf);
                String siblingRootExpr = buf.toString();
                if (!abstractSchemaExpr.equals(siblingRootExpr)) {
                    buf.setLength(0);
                    genPathExpression(sibling, buf);
                    ErrorMsg.error(I18NHelper.getMessage(msgs, 
                    "EXC_InvalidOrderbyItem", //NOI18N
                    buf.toString())); 
                }
            }
        } 
    }

    /**
     * Form a string representation of a dot expression and append to given
     * StringBuffer.
     * @param ast the AST node representing the root the of the expression
     * @param buf the StringBuffer that will have result of path expression
     * append
     */
    //SW: We can write this method without recursion. Michael suggests to use
    //recursion for readability.
    private void genPathExpression(AST ast, StringBuffer buf) {
        if (ast == null) {
            return;
        }
        switch (ast.getType()) {
            case CMP_FIELD_ACCESS:
            case COLLECTION_CMR_FIELD_ACCESS:
            case SINGLE_CMR_FIELD_ACCESS:
                AST left = ast.getFirstChild();
                AST right = left.getNextSibling();
                genPathExpression(left, buf);
                buf.append('.');
                genPathExpression(right, buf);
                break;
            default:
                buf.append(ast.getText());
                break;
        }
    }

    /**
     * Analyses a logical operation AND, OR
     * @param op the logical operator
     * @param leftAST left operand 
     * @param rightAST right operand
     * @return the type info of the operator 
     */
    private Object analyseConditionalExpr(EJBQLAST op, EJBQLAST leftAST, EJBQLAST rightAST)
    {
        Object left = leftAST.getTypeInfo();
        Object right = rightAST.getTypeInfo();
        
        // handle error type
        if (typeSupport.isErrorType(left) || typeSupport.isErrorType(right))
            return typeSupport.errorType;
        
        if (typeSupport.isBooleanType(left) && typeSupport.isBooleanType(right)) {
            Object common = typeSupport.booleanType;
            return common;
        }

        // if this code is reached a bitwise operator was used with invalid arguments
        ErrorMsg.error(op.getLine(), op.getColumn(), 
            I18NHelper.getMessage(msgs, "EXC_InvalidArguments",  op.getText())); //NOI18N
        return typeSupport.errorType;
    }
    
    /** 
     * Analyses a equality operation (==, <>)
     * @param op the relational operator
     * @param leftAST left operand 
     * @param rightAST right operand
     * @return the type info of the operator
     */
    private Object analyseEqualityExpr(EJBQLAST op, EJBQLAST leftAST, EJBQLAST rightAST)
    {
        Object left = leftAST.getTypeInfo();
        Object right = rightAST.getTypeInfo();
        
        // handle error type
        if (typeSupport.isErrorType(left) || typeSupport.isErrorType(right)) {
            return typeSupport.errorType;
        }

        // check left hand side for literals and input params 
        if (isLiteral(leftAST)) {
            ErrorMsg.error(leftAST.getLine(), leftAST.getColumn(), 
                I18NHelper.getMessage(msgs, "EXC_InvalidLHSLiteral", //NOI18N 
                    leftAST.getText(), op.getText()));
            return typeSupport.errorType;
        }
        else if (isInputParameter(leftAST)) {
            ErrorMsg.error(leftAST.getLine(), leftAST.getColumn(), 
                I18NHelper.getMessage(msgs, "EXC_InvalidLHSParameter", //NOI18N 
                    leftAST.getText(), op.getText()));
            return typeSupport.errorType;
        }
        
        // check operand types 
        if (typeSupport.isNumberType(left) && typeSupport.isNumberType(right)) {
            return typeSupport.booleanType;
        }
        else if (typeSupport.isStringType(left) && typeSupport.isStringType(right)) {
            return typeSupport.booleanType;
        }
        else if (typeSupport.isDateTimeType(left) && typeSupport.isDateTimeType(right)) {
            return typeSupport.booleanType;
        }
        else if (isEntityBeanValue(leftAST) && isEntityBeanValue(rightAST) && 
                 (typeSupport.isCompatibleWith(left, right) ||
                  typeSupport.isCompatibleWith(right, left))) {
            String leftEjbName = (String)leftAST.getTypeInfo();
            // the input parameter must be on right hand side of an equality
            // expression ('?1' = e.department is not supported)
            return analyseParameterEjbName(rightAST, leftEjbName);
        }
        else if (typeSupport.isBooleanType(left) && typeSupport.isBooleanType(right)) {
            return typeSupport.booleanType;
        }

        // if this code is reached a conditional operator was used with invalid arguments
        ErrorMsg.error(op.getLine(), op.getColumn(), 
            I18NHelper.getMessage(msgs, "EXC_InvalidArguments",  op.getText())); //NOI18N 
        return typeSupport.errorType;
    }
    
    /**
     * Analyses a relational operation (<, <=, >, >=)
     * @param op the relational operator
     * @param leftAST left operand 
     * @param rightAST right operand
     * @return the type info of the operator
     */
    private Object analyseRelationalExpr(EJBQLAST op, EJBQLAST leftAST, EJBQLAST rightAST)
    {
        Object left = leftAST.getTypeInfo();
        Object right = rightAST.getTypeInfo();

        // handle error type
        if (typeSupport.isErrorType(left) || typeSupport.isErrorType(right)) {
            return typeSupport.errorType;
        }

        // check left hand side for literals and input params 
        if (isLiteral(leftAST)) {
            ErrorMsg.error(leftAST.getLine(), leftAST.getColumn(), 
                I18NHelper.getMessage(msgs, "EXC_InvalidLHSLiteral", //NOI18N 
                    leftAST.getText(), op.getText()));
            return typeSupport.errorType;
        }
        else if (isInputParameter(leftAST)) {
            ErrorMsg.error(leftAST.getLine(), leftAST.getColumn(), 
                I18NHelper.getMessage(msgs, "EXC_InvalidLHSParameter", //NOI18N 
                    leftAST.getText(), op.getText()));
            return typeSupport.errorType;
        }
        
        // check operand types
        if ((typeSupport.isNumberType(left) && typeSupport.isNumberType(right)) ||
            (typeSupport.isDateTimeType(left) && typeSupport.isDateTimeType(right)) ||
            (typeSupport.isStringType(left) && typeSupport.isStringType(right))) {
            return typeSupport.booleanType;
        }

        // if this code is reached a conditional operator was used with invalid arguments
        ErrorMsg.error(op.getLine(), op.getColumn(), 
            I18NHelper.getMessage(msgs, "EXC_InvalidArguments",  op.getText())); //NOI18N 
        return typeSupport.errorType;
    }
    
    /**
     * Analyses a binary arithmetic expression +, -, *, /.
     * @param op the  operator
     * @param leftAST left operand 
     * @param rightAST right operand
     * @return the type info of the operator
     */
    private Object analyseBinaryArithmeticExpr(EJBQLAST op, EJBQLAST leftAST, EJBQLAST rightAST)
    {
        Object left = leftAST.getTypeInfo();
        Object right = rightAST.getTypeInfo();

        // handle error type
        if (typeSupport.isErrorType(left) || typeSupport.isErrorType(right)) {
            return typeSupport.errorType;
        }

        if (typeSupport.isNumberType(left) && typeSupport.isNumberType(right)) {
            Object common = getCommonOperandType(left, right);
            if (!typeSupport.isErrorType(common))
                return common;
        }

        // if this code is reached a conditional operator was used with invalid arguments
        ErrorMsg.error(op.getLine(), op.getColumn(), 
            I18NHelper.getMessage(msgs, "EXC_InvalidArguments",  op.getText())); //NOI18N
        return typeSupport.errorType;
    }

    /**
     * Returns the common type info for the specified operand types. 
     * This includes binary numeric promotion as specified in Java.
     * @param left type info of left operand 
     * @param right type info of right operand
     * @return the type info of the operator
     */
    private Object getCommonOperandType(Object left, Object right)
    {
        if (typeSupport.isNumberType(left) && typeSupport.isNumberType(right)) {
            boolean wrapper = false;

            // handle java.math.BigDecimal:
            if (typeSupport.bigDecimalType.equals(left)) {
                return left;
            }
            if (typeSupport.bigDecimalType.equals(right)) {
                return right;
            }
            
            // handle java.math.BigInteger
            if (typeSupport.bigIntegerType.equals(left)) {
                // if right is floating point return BigDecimal, 
                // otherwise return BigInteger
                return typeSupport.isFloatingPointType(right) ? 
                       typeSupport.bigDecimalType : left;
            }
            if (typeSupport.bigIntegerType.equals(right)) {
                // if left is floating point return BigDecimal, 
                // otherwise return BigInteger
                return typeSupport.isFloatingPointType(left) ? 
                       typeSupport.bigDecimalType : right;
            }       

            if (typeSupport.isNumericWrapperType(left)) {
                left = typeSupport.getPrimitiveType(left);
                wrapper = true;
            }
            if (typeSupport.isNumericWrapperType(right)) {
                right = typeSupport.getPrimitiveType(right);
                wrapper = true;
            }
            
            // handle numeric types with arbitrary arithmetic operator
            if (typeSupport.isNumericType(left) && typeSupport.isNumericType(right)) {
                Object promotedType = typeSupport.binaryNumericPromotion(left, right);
                if (wrapper) 
                    promotedType = typeSupport.getWrapperType(promotedType);
                return promotedType;
            }
        }
        else if (typeSupport.isBooleanType(left) && typeSupport.isBooleanType(right)) {
            // check for boolean wrapper class: if one of the operands has the 
            // type Boolean return Boolean, otherwise return boolean.
            if (left.equals(typeSupport.booleanClassType) || 
                right.equals(typeSupport.booleanClassType))
                return typeSupport.booleanClassType;
            else
                return typeSupport.booleanType;
        }
        else if (typeSupport.isCompatibleWith(left, right)) {
            return right;
        }
        else if (typeSupport.isCompatibleWith(right, left)) {
            return left;
        }

        // not compatible types => return errorType
        return typeSupport.errorType;
    }

    /**
     * Analyses a unary expression (+ and -).
     * @param op the operator
     * @param argASTleftAST left operand 
     * @param rightAST right operand
     * @return the type info of the operator 
     */
    private Object analyseUnaryArithmeticExpr(EJBQLAST op, EJBQLAST argAST)
    {
        Object arg = argAST.getTypeInfo();

        // handle error type
        if (typeSupport.isErrorType(arg))
            return arg;
        
        if (typeSupport.isNumberType(arg)) {
            boolean wrapper = false;
            if (typeSupport.isNumericWrapperType(arg)) {
                arg = typeSupport.getPrimitiveType(arg);
                wrapper = true;
            }

            Object promotedType = typeSupport.unaryNumericPromotion(arg);
            if (wrapper)
                promotedType = typeSupport.getWrapperType(promotedType);
            return promotedType;
        }
        
        // if this code is reached a conditional operator was used with invalid arguments
        ErrorMsg.error(op.getLine(), op.getColumn(), 
            I18NHelper.getMessage(msgs, "EXC_InvalidArguments",  op.getText())); //NOI18N
        return typeSupport.errorType;
    }
    
    /** 
     * Analyses a expression node that is expected to access a collection 
     * valued CMR field. It returns the element type of the collection valued 
     * CMR field. 
     * @param fieldAccess the field access node
     * @return the type info of the operator 
     */
    private Object analyseCollectionValuedCMRField(EJBQLAST fieldAccess)
    {
        if (fieldAccess.getType() != COLLECTION_CMR_FIELD_ACCESS) {
            ErrorMsg.fatal(I18NHelper.getMessage(msgs, "ERR_InvalidPathExpr")); //NOI18N
            return typeSupport.errorType;
        }

        EJBQLAST classExpr = (EJBQLAST)fieldAccess.getFirstChild();
        EJBQLAST field = (EJBQLAST)classExpr.getNextSibling();
        Object fieldInfo = 
            typeSupport.getFieldInfo(classExpr.getTypeInfo(), field.getText());
        return typeSupport.getElementType(fieldInfo);
    }

    /**
     * Analyses a MEMBER OF operation. 
     * @param op the MEMBER OF operator
     * @param value node representing the value to be tested
     * @param col the collection
     * @return the type info of the operator
     */
    private Object analyseMemberExpr(EJBQLAST op, EJBQLAST value, EJBQLAST col)
    {
        Object valueTypeInfo = value.getTypeInfo();
        Object elementTypeInfo = analyseCollectionValuedCMRField(col);

        // handle error type
        if (typeSupport.isErrorType(valueTypeInfo) || 
            typeSupport.isErrorType(elementTypeInfo)) {
            return typeSupport.errorType;
        }

        // check compatibility
        if (typeSupport.isCompatibleWith(valueTypeInfo, elementTypeInfo) ||
            typeSupport.isCompatibleWith(elementTypeInfo, valueTypeInfo)) {

            return analyseParameterEjbName(value, (String)elementTypeInfo);
        }
        
        // if this code is reached there is a compatibility problem 
        // with the value and the collection expr
        ErrorMsg.error(op.getLine(), op.getColumn(), 
            I18NHelper.getMessage(msgs, "EXC_CollectionElementTypeMismatch", //NOI18N
                typeSupport.getTypeName(elementTypeInfo), 
                typeSupport.getTypeName(valueTypeInfo)));
        return typeSupport.errorType;
    }

    /**
     * Analyses the type of the element to be compatible with the type of the
     * value expression in the sense that element type can be cast into value
     * type without losing precision.
     * For instance, element type can be a double and value type can be an
     * integer.
     * @param elementAST given element
     * @param valueTypeInfo the type to be check for compatibility
     * @return the type info of the elementAST or typeSupport.errorType
     */
    private Object analyseInCollectionElement(EJBQLAST elementAST,
            Object valueTypeInfo)
    {
        Object elementTypeInfo = elementAST.getTypeInfo();

        // handle error type
        if (typeSupport.isErrorType(valueTypeInfo) || 
            typeSupport.isErrorType(elementTypeInfo)) {
            return typeSupport.errorType;
        }

        Object common = getCommonOperandType(elementTypeInfo, valueTypeInfo);
        if (!typeSupport.isErrorType(common) &&
                elementTypeInfo.equals(common)) {
            return common;
        }

        // if this code is reached there is a compatibility problem
        // with the value and the collection expr
        ErrorMsg.error(elementAST.getLine(), elementAST.getColumn(),
            I18NHelper.getMessage(msgs, "EXC_CollectionElementTypeMismatch", //NOI18N
            typeSupport.getTypeName(valueTypeInfo),
            typeSupport.getTypeName(elementTypeInfo)));
        return typeSupport.errorType;
    }

    /**
     * Analyses whether paramAST can be associated to a ejbName.
     * @param paramAST AST node corresponds to a PARAMETER
     * @param ejbName name to be check with paramAST
     * @return the type info of typeSupport.booleanType or typeSupport.errorType
     */
    private Object analyseParameterEjbName(EJBQLAST paramAST, String ejbName)
    {
        if (isInputParameter(paramAST)) {
            String paramName = paramAST.getText();
            String paramEjbName = paramSupport.getParameterEjbName(paramName);
            if (paramEjbName != null && !paramEjbName.equals(ejbName)) {
                ErrorMsg.error(paramAST.getLine(), paramAST.getColumn(),
                    I18NHelper.getMessage(msgs,
                    "EXC_MultipleEJBNameParameter", // NOI18N
                    paramName, ejbName, paramEjbName));
                return typeSupport.errorType;
            } else {
                paramSupport.setParameterEjbName(paramName, ejbName);
            }
        }
        return typeSupport.booleanType;
    }
    
    /** 
     * Returns <code>true</code> if ast denotes a entity bena value.
     */
    private boolean isEntityBeanValue(EJBQLAST ast)
    {
        switch(ast.getType()) {
        case SINGLE_CMR_FIELD_ACCESS:
        case IDENTIFICATION_VAR:
            return true;
        case INPUT_PARAMETER:
            Object typeInfo = ast.getTypeInfo();
            return typeSupport.isEjbOrInterfaceName(typeInfo);
        }
        return false;
    }

    /** 
     * Returns <code>true</code> if ast denotes a literal.
     */
    private boolean isLiteral(EJBQLAST ast)
    {
        int tokenType = ast.getType();
        return ((tokenType == INT_LITERAL) || 
                (tokenType == LONG_LITERAL) ||
                (tokenType == STRING_LITERAL) || 
                (tokenType == FLOAT_LITERAL) || 
                (tokenType == DOUBLE_LITERAL) ||
                (tokenType == TRUE) || 
                (tokenType == FALSE));
    }

    /** 
     * Returns <code>true</code> if ast denotes a input parameter access.
     */
    private boolean isInputParameter(EJBQLAST ast)
    {
        return ast.getType() == INPUT_PARAMETER;
    }
    
    /**
     * The method checks the specified node being an expression of type String. 
     * @param expr the expression to be checked
     * @return <code>true</code> if the specified expression has the type String.
     */
    private boolean isStringExpr(EJBQLAST expr)
    {
        Object exprType = expr.getTypeInfo();
        
        // handle error type
        if (typeSupport.isErrorType(exprType))
            return true;
        
        // expr must have the type String
        if (!typeSupport.isStringType(exprType)) {
            ErrorMsg.error(expr.getLine(), expr.getColumn(), 
                I18NHelper.getMessage(msgs, "EXC_StringExprExpected", //NOI18N
                    typeSupport.getTypeName(exprType)));
            return false;
        }
        
        // everything is ok => return true;
        return true;
    }

    /**
     * The method checks the specified node being an expression of 
     * type int or java.lang.Integer.
     * @param expr the expression to be checked
     * @return <code>true</code> if the specified expression has the type 
     * int or java.lang.Integer.
     */
    private boolean isIntExpr(EJBQLAST expr)
    {
        Object exprType = expr.getTypeInfo();
        
        // handle error type
        if (typeSupport.isErrorType(exprType))
            return true;
        
        // expr must have the type int or Integer
        if (!typeSupport.isIntType(exprType)) {
            ErrorMsg.error(expr.getLine(), expr.getColumn(), 
                I18NHelper.getMessage(msgs, "EXC_IntExprExpected", //NOI18N
                    typeSupport.getTypeName(exprType)));
            return false;
        }
        
        // everything is ok => return true;
        return true;
    }

    /**
     * The method checks the specified node being an expression of 
     * type double or java.lang.Double.
     * @param expr the expression to be checked
     * @return <code>true</code> if the specified expression has the type 
     * double or java.lang.Double.
     */
    private boolean isDoubleExpr(EJBQLAST expr)
    {
        Object exprType = expr.getTypeInfo();
        
        // handle error type
        if (typeSupport.isErrorType(exprType))
            return true;
        
        // expr must have the type double or Double
        if (!typeSupport.isDoubleType(exprType)) {
            ErrorMsg.error(expr.getLine(), expr.getColumn(), 
                I18NHelper.getMessage(msgs, "EXC_DoubleExprExpected", //NOI18N
                    typeSupport.getTypeName(exprType)));
            return false;
        }
        
        // everything is ok => return true;
        return true;
    }

    /**
     * The method checks the specified node being an expression of a number type
     * (a numeric type or a number wrapper class).
     * @param expr the expression to be checked
     * @return <code>true</code> if the specified expression has a number type.
     */
    private boolean isNumberExpr(EJBQLAST expr)
    {
        Object exprType = expr.getTypeInfo();
        
        // handle error type
        if (typeSupport.isErrorType(exprType))
            return true;
        
        // expr must have a number type
        if (!typeSupport.isNumberType(exprType)) {
            ErrorMsg.error(expr.getLine(), expr.getColumn(), 
                I18NHelper.getMessage(msgs, "EXC_NumberExprExpected", //NOI18N
                    typeSupport.getTypeName(exprType)));
            return false;
        }
        
        // everything is ok => return true;
        return true;
    }

    /**
     * The method checks the specified node being an expression of a number type
     * (a numeric type or a number wrapper class).
     * @param expr the expression to be checked
     * @return <code>true</code> if the specified expression has a number or
     * String type 
     */
    private boolean isNumberOrStringExpr(EJBQLAST expr)
    {
        Object exprType = expr.getTypeInfo();
        
        // handle error type
        if (typeSupport.isErrorType(exprType))
            return true;
        
        // expr must have a number type
        if (!typeSupport.isNumberType(exprType) &&
                !typeSupport.isStringType(exprType)) {
            ErrorMsg.error(expr.getLine(), expr.getColumn(), 
                I18NHelper.getMessage(msgs,
                    "EXC_NumberOrStringExprExpected", //NOI18N
                    typeSupport.getTypeName(exprType)));
            return false;
        }
        
        // everything is ok => return true;
        return true;
    }

    /** 
     * The method checks whether the specified node denotes a valid abstract 
     * schema type.
     * @param ident the node to be checked
     * @return the type info for the abstract bean class of the specified 
     * abstract schema type.
     */
    private Object checkAbstractSchemaType(EJBQLAST ident)
    {
        String name = ident.getText();
        Object typeInfo = 
            typeSupport.getTypeInfoForAbstractSchema(name);
        if (typeInfo == null) {
            ErrorMsg.error(ident.getLine(), ident.getColumn(), 
                I18NHelper.getMessage(msgs, 
                    "EXC_AbstractSchemNameExpected", name)); //NOI18N
            typeInfo = typeSupport.errorType;
        }
        return typeInfo;
    }

    /**
     * Returns true if the specified text is a string literal consisting of a
     * single char. Escaped chars are counted as a single char such as \ uxxxx.
     */
    private boolean isSingleCharacterStringLiteral(String text)
    {
        int i = 0;
        int length = text.length();
        if (length == 0) {
            // empty string
            return false;
        }
        if (text.charAt(i) == '\\')
        {
            i++;
            if (i == length) {
                // string literal was '\'
                return true;
            }
            // escaped char => check the next char
            if (text.charAt(i) == 'u') {
                // unicode
                i +=5;
            }
            else if (('0' <= text.charAt(i)) && (text.charAt(i) <= '3')) {
                i++;
                if ((i < length) && isOctalDigit(text.charAt(i))) {
                    i++;
                    if ((i < length) && isOctalDigit(text.charAt(i))) {
                        i++;
                    }
                }
            }
            else if (isOctalDigit(text.charAt(i))) {
                i++;
                if ((i < length) && isOctalDigit(text.charAt(i))) {
                    i++;
                }
            }
            else {
                i++;
            }
        }
        else if (text.charAt(i) == '\''){
            // check special EJBQL single quote char
            i++;
            if ((i < length) && (text.charAt(i) == '\'')) {
                i++;
            }
        }
        else {
            i++;
        }
        // reached end of text?
        return (i == length);
    }

    /** Returns true if the specified char is an octal digit */
    private boolean isOctalDigit(char c)
    {
        return ('0' <= c && c <= '7');
    }

}

// rules

query
    :   #(QUERY fromClause s:selectClause whereClause o:orderbyClause)
        {
            checkSelectOrderbyClause(#s, #o);
        }
    ;

// ----------------------------------
// rules: from clause
// ----------------------------------

fromClause
    :   #( FROM ( identificationVarDecl )+ )
    ;

identificationVarDecl
    :   collectionMemberDecl
    |   rangeVarDecl
    ;

collectionMemberDecl
    :   #(IN p:collectionValuedPathExpression var:IDENT)
        {
            Object typeInfo = analyseCollectionValuedCMRField(#p);
            String name = #var.getText();
            Object identVar = new IdentificationVariable(name, typeInfo);
            if (symtab.declare(name, identVar) != null) {
                ErrorMsg.error(#var.getLine(), #var.getColumn(),
                    I18NHelper.getMessage(msgs, "EXC_MultipleDeclaration", name)); //NOI18N
            }
            #var.setType(IDENTIFICATION_VAR_DECL);
            #var.setTypeInfo(typeInfo);
        }
    ;

rangeVarDecl
    :   #(RANGE abstractSchemaName:ABSTRACT_SCHEMA_NAME var:IDENT)
        {
            // check abstract schema name
            Object typeInfo = 
                checkAbstractSchemaType(#abstractSchemaName);
            #abstractSchemaName.setTypeInfo(typeInfo);

            // check identification variable
            String name = #var.getText();
            Object identVar = new IdentificationVariable(name, typeInfo);
            if (symtab.declare(name, identVar) != null) {
                ErrorMsg.error(#var.getLine(), #var.getColumn(),
                    I18NHelper.getMessage(msgs, "EXC_MultipleDeclaration", name)); //NOI18N
            }
            #var.setType(IDENTIFICATION_VAR_DECL);
            #var.setTypeInfo(typeInfo);
        }
    ;

// ----------------------------------
// rules: select clause
// ----------------------------------

selectClause
    :   #( SELECT distinct p:projection )
        {
            Object selectClauseTypeInfo = #p.getTypeInfo();
            Class returnType = method.getReturnType();
            if (finderNotSelector) {
                checkFinderReturnType(returnType, selectClauseTypeInfo);
                checkFinderResultTypeMapping();
            }
            else {
                checkSelectorReturnType(returnType, selectClauseTypeInfo);
                checkSelectorResultTypeMapping(returnType, 
                                               selectClauseTypeInfo);
            }
        }
    ;

distinct
    :   DISTINCT
    |   // empty rule
        {
            // Insert DISTINCT keyword, in the case of a multi-object selector 
            // having java.util.Set as return type
            if (!finderNotSelector && 
                (method.getReturnType() == java.util.Set.class)) {
                #distinct = #[DISTINCT,"distinct"];
            }
        }
    ; 

projection
    :   singleValuedPathExpression
    |   #( o:OBJECT var:IDENT )
        {
            String name = #var.getText();
            Object decl = symtab.getDeclaration(name);
            Object typeInfo = null;
            if ((decl != null) && 
                (decl instanceof IdentificationVariable)) {
                #var.setType(IDENTIFICATION_VAR);
                typeInfo = ((IdentificationVariable)decl).getTypeInfo();
            }
            else {
                ErrorMsg.error(#var.getLine(), #var.getColumn(), 
                    I18NHelper.getMessage(msgs, 
                        "EXC_IdentificationVariableExcepted", name)); //NOI18N
            }
            #var.setTypeInfo(typeInfo);
            #o.setTypeInfo(typeInfo);
        }
    |   #( sum:SUM ( DISTINCT )? sumExpr:cmpPathExpression )
        {
            // check numeric type
            Object typeInfo = #sumExpr.getTypeInfo();
            if (!typeSupport.isNumberType(typeInfo) ||
                    typeSupport.isCharType(typeInfo)) {
                ErrorMsg.error(#sumExpr.getLine(), #sumExpr.getColumn(),
                    I18NHelper.getMessage(msgs,
                        "EXC_NumberExprExpected", //NO18N
                        typeSupport.getTypeName(typeInfo)));
            }
            #sum.setTypeInfo(typeSupport.getSumReturnType(typeInfo));
            isAggregate = true;
        }
    |   #( avg:AVG ( DISTINCT )? avgExpr:cmpPathExpression )
        {
            // check numeric type
            Object typeInfo = #avgExpr.getTypeInfo();
            if (!typeSupport.isNumberType(typeInfo) ||
                    typeSupport.isCharType(typeInfo)) {
                ErrorMsg.error(#avgExpr.getLine(), #avgExpr.getColumn(),
                    I18NHelper.getMessage(msgs,
                        "EXC_NumberExprExpected", //NO18N
                        typeSupport.getTypeName(typeInfo)));
            }
            #avg.setTypeInfo(typeSupport.getAvgReturnType(typeInfo));
            isAggregate = true;
        }
    |   #( min:MIN ( DISTINCT )? minExpr:cmpPathExpression )
        {
            // check orderable type
            Object typeInfo = #minExpr.getTypeInfo();
            if (!typeSupport.isOrderableType(typeInfo)) {
                ErrorMsg.error(#minExpr.getLine(), #minExpr.getColumn(),
                    I18NHelper.getMessage(msgs,
                        "EXC_OrderableExpected", //NO18N
                        typeSupport.getTypeName(typeInfo)));
            }
            #min.setTypeInfo(typeSupport.getMinMaxReturnType(typeInfo));
            isAggregate = true;
        }
    |   #( max:MAX ( DISTINCT )? maxExpr:cmpPathExpression )
        {
            // check orderable type
            Object typeInfo = #maxExpr.getTypeInfo();
            if (!typeSupport.isOrderableType(typeInfo)) {
                ErrorMsg.error(#maxExpr.getLine(), #maxExpr.getColumn(),
                    I18NHelper.getMessage(msgs,
                        "EXC_OrderableExpected", //NO18N
                        typeSupport.getTypeName(typeInfo)));
            }
            #max.setTypeInfo(typeSupport.getMinMaxReturnType(typeInfo));
            isAggregate = true;
        }
    |   #( c:COUNT ( DISTINCT )? countExpr )
        {
            #c.setTypeInfo(typeSupport.longClassType);
            isAggregate = true;
        }
    ;

countExpr
    :   v:IDENT
        {
            String name = #v.getText();
            Object decl = symtab.getDeclaration(name);
            Object typeInfo = null;
            if ((decl != null) && 
                (decl instanceof IdentificationVariable)) {
                #v.setType(IDENTIFICATION_VAR);
                typeInfo = ((IdentificationVariable)decl).getTypeInfo();
            }
            else {
                ErrorMsg.error(#v.getLine(), #v.getColumn(), 
                    I18NHelper.getMessage(msgs, 
                    "EXC_IdentificationVariableExcepted", name)); //NOI18N
            }
            #v.setTypeInfo(typeInfo);
        }
    |   singleValuedPathExpression
    ;

// ----------------------------------
// rules: where clause
// ----------------------------------

whereClause
    :   #( WHERE e:expression )
        {
            Object typeInfo = #e.getTypeInfo();
            if (!typeSupport.isBooleanType(typeInfo)) {
                ErrorMsg.error(#e.getLine(), #e.getColumn(),
                    I18NHelper.getMessage(msgs, "EXC_BooleanWhereClauseExpected",  //NOI18N
                        typeSupport.getTypeName(typeInfo)));
            }
        }
    ;

// ----------------------------------
// rules: order by clause
// ----------------------------------

orderbyClause
    :   #( ORDER ( orderbyItem )+ )
    |   // empty rule
    ;

orderbyItem
    :   expr:cmpPathExpression ( ASC | DESC )
        {
            // check orderable type
            Object typeInfo = #expr.getTypeInfo();
            if (!typeSupport.isOrderableType(typeInfo)) {
                ErrorMsg.error(#expr.getLine(), #expr.getColumn(),
                    I18NHelper.getMessage(msgs,
                        "EXC_OrderableOrderbyClauseExpected", //NO18N
                        typeSupport.getTypeName(typeInfo)));
            }
        }
    ;

// ----------------------------------
// rules: expression
// ----------------------------------

expression
    :   conditionalExpr
    |   relationalExpr
    |   binaryArithmeticExpr
    |   unaryExpr
    |   betweenExpr
    |   likeExpr
    |   inExpr
    |   nullComparisonExpr
    |   emptyCollectionComparisonExpr
    |   collectionMemberExpr
    |   function
    |   primary
    ;

conditionalExpr
    :   #( op1:AND left1:expression right1:expression )
        {
            #op1.setTypeInfo(analyseConditionalExpr(#op1, #left1, #right1));
        }
    |   #( op2:OR  left2:expression right2:expression )
        {
            #op2.setTypeInfo(analyseConditionalExpr(#op2, #left2, #right2));
        }
    ;

relationalExpr
    :   #( op1:EQUAL left1:expression right1:expression )
        {
            #op1.setTypeInfo(analyseEqualityExpr(#op1, #left1, #right1));
        }
    |   #( op2:NOT_EQUAL left2:expression right2:expression )
        {
            #op2.setTypeInfo(analyseEqualityExpr(#op2, #left2, #right2));
        }
    |   #( op3:LT left3:expression right3:expression )
        {
            #op3.setTypeInfo(analyseRelationalExpr(#op3, #left3, #right3));
        }
    |   #( op4:LE left4:expression right4:expression )
        {
            #op4.setTypeInfo(analyseRelationalExpr(#op4, #left4, #right4));
        }
    |   #( op5:GT left5:expression right5:expression )
        {
            #op5.setTypeInfo(analyseRelationalExpr(#op5, #left5, #right5));
        }
    |   #( op6:GE left6:expression right6:expression )
        {
            #op6.setTypeInfo(analyseRelationalExpr(#op6, #left6, #right6));
        }
    ;

binaryArithmeticExpr
    :   #( op1:PLUS left1:expression right1:expression )
        {
            #op1.setTypeInfo(analyseBinaryArithmeticExpr(#op1, #left1, #right1));
        }
    |   #( op2:MINUS left2:expression right2:expression )
        {
            #op2.setTypeInfo(analyseBinaryArithmeticExpr(#op2, #left2, #right2));
        }
    |   #( op3:STAR left3:expression right3:expression )
        {
            #op3.setTypeInfo(analyseBinaryArithmeticExpr(#op3, #left3, #right3));
        }
    |   #( op4:DIV left4:expression right4:expression )
        {
            #op4.setTypeInfo(analyseBinaryArithmeticExpr(#op4, #left4, #right4));
        }
    ;

unaryExpr
    :   #( op1:UNARY_PLUS arg1:expression )
        {
            #op1.setTypeInfo(analyseUnaryArithmeticExpr(#op1, #arg1));
        }
    |   #( op2:UNARY_MINUS arg2:expression )
        {
            #op2.setTypeInfo(analyseUnaryArithmeticExpr(#op2, #arg2));
        }
    |   #( op3:NOT arg3:expression )
        {
            Object typeInfo = typeSupport.errorType;
            Object arg = #arg3.getTypeInfo();
            if (typeSupport.isErrorType(arg))
                typeInfo = typeSupport.errorType;
            else if (typeSupport.isBooleanType(arg))
                typeInfo = arg;
            else {
                ErrorMsg.error(#op3.getLine(), #op3.getColumn(), 
                    I18NHelper.getMessage(msgs, "EXC_InvalidArguments", //NOI18N
                        #op3.getText())); 
            }
            #op3.setTypeInfo(typeInfo);
        }
    ;

betweenExpr 
    :   #( op1:BETWEEN expr1:expression lower1:expression upper1:expression )
        {
            #op1.setTypeInfo((isNumberExpr(#expr1) && isNumberExpr(#lower1) && isNumberExpr(#upper1)) ? 
                typeSupport.booleanType : typeSupport.errorType);
        }
    |   #( op2:NOT_BETWEEN expr2:expression lower2:expression upper2:expression )
        {
            #op2.setTypeInfo((isNumberExpr(#expr2) && isNumberExpr(#lower2) && isNumberExpr(#upper2)) ? 
                typeSupport.booleanType : typeSupport.errorType);
        }
    ;

likeExpr
    :   #( op1:LIKE expr1:cmpPathExpression pattern escape )
        {
            #op1.setTypeInfo(isStringExpr(#expr1) ? 
                typeSupport.booleanType : typeSupport.errorType);
        }
    |   #( op2:NOT_LIKE expr2:cmpPathExpression pattern escape )
        {
            #op2.setTypeInfo(isStringExpr(#expr2) ? 
                typeSupport.booleanType : typeSupport.errorType);
        }
    ;

pattern
    :   STRING_LITERAL 
    |   p:inputParameter
        {
            if (!typeSupport.isStringType(#p.getTypeInfo())) {
                ErrorMsg.error(#p.getLine(), #p.getColumn(),
                    I18NHelper.getMessage(msgs, "EXC_InvalidPatternDefinition",
                        #p.getText())); //NOI18N
            }
        }
    ;

escape
    :   #( ESCAPE escapeCharacter )
    |   // empty rule
    ;

escapeCharacter
    :   s:STRING_LITERAL
        {
            String literal = #s.getText();
            // String must be single charater string literal =>
            // either '<char>' or ''''
            if (!isSingleCharacterStringLiteral(#s.getText())) {
                ErrorMsg.error(#s.getLine(), #s.getColumn(),
                    I18NHelper.getMessage(msgs, 
                        "EXC_InvalidEscapeDefinition", #s.getText())); //NOI18N
            }
        }
    |   p:inputParameter
        {
            Object paramType = #p.getTypeInfo();
            if (!typeSupport.isCharType(paramType)) {
                ErrorMsg.error(#p.getLine(), #p.getColumn(),
                    I18NHelper.getMessage(msgs, 
                        "EXC_InvalidEscapeParameterDefinition", #p.getText())); //NOI18N
            }
        }
    ;

inExpr
    :   #( op1:IN expr1:cmpPathExpression inCollection[#expr1.getTypeInfo()] )
        {
            #op1.setTypeInfo(isNumberOrStringExpr(#expr1) ? 
                typeSupport.booleanType : typeSupport.errorType);
        }
    |   #( op2:NOT_IN expr2:cmpPathExpression inCollection[#expr2.getTypeInfo()] )
        {
            #op2.setTypeInfo(isNumberOrStringExpr(#expr2) ? 
                typeSupport.booleanType : typeSupport.errorType);
        }
    ;

nullComparisonExpr
    :   #( op1:NULL ( singleValuedPathExpression | inputParameter ) )
        {
            #op1.setTypeInfo(typeSupport.booleanType);
        }
    |   #( op2:NOT_NULL ( singleValuedPathExpression | inputParameter ) )
        {
            #op2.setTypeInfo(typeSupport.booleanType);
        }
    ;

emptyCollectionComparisonExpr
{ 
    Object elementTypeInfo = null; 
}
    :   #( op1:EMPTY col1:collectionValuedPathExpression )
        {
            elementTypeInfo = analyseCollectionValuedCMRField(#col1);
            #op1.setTypeInfo(typeSupport.isErrorType(elementTypeInfo) ? 
                typeSupport.errorType : typeSupport.booleanType );
        }
    |   #( op2:NOT_EMPTY col2:collectionValuedPathExpression )
        {
            elementTypeInfo = analyseCollectionValuedCMRField(#col2);
            #op2.setTypeInfo(typeSupport.isErrorType(elementTypeInfo) ? 
                typeSupport.errorType : typeSupport.booleanType );
        }
    ;

collectionMemberExpr
    :   #( op1:MEMBER value1:member col1:collectionValuedPathExpression )
        {
            #op1.setTypeInfo(analyseMemberExpr(#op1, #value1, #col1));
        }
    |   #( op2:NOT_MEMBER value2:member col2:collectionValuedPathExpression )
        {
            #op2.setTypeInfo(analyseMemberExpr(#op2, #value2, #col2));
        }
    ;

member
    :   identificationVariable
    |   inputParameter
    |   singleValuedCmrPathExpression
    ;

function
    :   concat
    |   substring
    |   length
    |   locate
    |   abs
    |   sqrt
    |   mod
    ;

concat
    :   #( op:CONCAT arg1:expression arg2:expression )
        {
            #op.setTypeInfo((isStringExpr(#arg1) && isStringExpr(#arg2)) ?
                typeSupport.stringType : typeSupport.errorType);
        }
    ;

substring
    :   #( op:SUBSTRING arg1:expression arg2:expression arg3:expression )
        {
            #op.setTypeInfo((isStringExpr(#arg1) && isIntExpr(#arg2) && isIntExpr(#arg3)) ? 
                typeSupport.stringType : typeSupport.errorType);
        }
    ;

length
    :   #( op:LENGTH arg:expression )
        {
            #op.setTypeInfo(isStringExpr(#arg) ? 
                typeSupport.intType : typeSupport.errorType);
        }
    ;

locate
    :   #( op:LOCATE arg1:expression arg2:expression ( arg3:expression )? )
        {
            #op.setTypeInfo((isStringExpr(#arg1) && isStringExpr(#arg2) && 
                             ((#arg3 == null) || isIntExpr(#arg3))) ?
                typeSupport.intType : typeSupport.errorType);
        }
    ;

abs
    :   #( op:ABS expr:expression )
        {
            #op.setTypeInfo(isNumberExpr(#expr) ? 
                #expr.getTypeInfo() : typeSupport.errorType);
        }
    ;

sqrt
    :   #( op:SQRT expr:expression )
        {
            #op.setTypeInfo(isDoubleExpr(#expr) ? 
                #expr.getTypeInfo() : typeSupport.errorType);
        }
    ;

mod
    :   #( op:MOD arg1:expression arg2:expression )
        {
            #op.setTypeInfo((isIntExpr(#arg1) && isIntExpr(#arg2)) ? 
                typeSupport.intType : typeSupport.errorType);
        }
    ;

primary
    :   literal
    |   singleValuedPathExpression
    |   identificationVariable
    |   inputParameter
    ;

literal
    :   b1:TRUE          { #b1.setTypeInfo(typeSupport.booleanType); }
    |   b2:FALSE         { #b2.setTypeInfo(typeSupport.booleanType); }
    |   s:STRING_LITERAL { #s.setTypeInfo(typeSupport.stringType); }
    |   i:INT_LITERAL    { #i.setTypeInfo(typeSupport.intType); }
    |   l:LONG_LITERAL   { #l.setTypeInfo(typeSupport.longType); }
    |   f:FLOAT_LITERAL  { #f.setTypeInfo(typeSupport.floatType); }
    |   d:DOUBLE_LITERAL { #d.setTypeInfo(typeSupport.doubleType); }
    ;

pathExpression
    :   #(  dot:DOT  o:objectDenoter i:IDENT )
        {
            String fieldName = #i.getText();
            Object typeInfo = #o.getTypeInfo();
            Object fieldTypeInfo = 
                typeSupport.getFieldType(typeInfo, fieldName);
            if (fieldTypeInfo == null) {
                // field is not known
                ErrorMsg.error(#i.getLine(), #i.getColumn(),
                    I18NHelper.getMessage(msgs, "EXC_UnknownField", fieldName, //NOI18N
                        typeSupport.getAbstractSchemaForTypeInfo(typeInfo)));
                fieldTypeInfo = typeSupport.errorType;
            }
            else {
                Object fieldInfo = typeSupport.getFieldInfo(typeInfo, fieldName);
                if (fieldInfo == null) {
                    ErrorMsg.fatal(I18NHelper.getMessage(msgs, 
                            "ERR_MissingFieldInfo",  //NOI18N
                            fieldName, typeSupport.getTypeName(typeInfo)));
                }
                if (!typeSupport.isRelationship(fieldInfo)) {
                    // field is not a relationship => cmp field
                    #i.setType(CMP_FIELD);
                    #dot.setType(CMP_FIELD_ACCESS);
                }
                else if (typeSupport.isCollectionType(fieldTypeInfo)) {
                    // field is a relationship of a collection type =>
                    // collection valued cmr field
                    #i.setType(COLLECTION_CMR_FIELD);
                    #dot.setType(COLLECTION_CMR_FIELD_ACCESS);
                }
                else {
                    // field is a relationship of a non collection type =>
                    // single valued cmr field
                    #i.setType(SINGLE_CMR_FIELD);
                    #dot.setType(SINGLE_CMR_FIELD_ACCESS);
                }
            }
            #dot.setTypeInfo(fieldTypeInfo);
            #i.setTypeInfo(fieldTypeInfo);
        }

    ;

objectDenoter
    :   identificationVariable
    |   singleValuedCmrPathExpression
    ;

identificationVariable
    :   i:IDENT 
        {
            String name = #i.getText();
            Object decl = symtab.getDeclaration(name);
            // check for identification variables
            if ((decl != null) && (decl instanceof IdentificationVariable)) {
                #i.setType(IDENTIFICATION_VAR);
                #i.setTypeInfo(((IdentificationVariable)decl).getTypeInfo());
            }
            else {
                #i.setTypeInfo(typeSupport.errorType);
                ErrorMsg.error(#i.getLine(), #i.getColumn(),
                    I18NHelper.getMessage(msgs, "EXC_UndefinedIdentifier", name)); //NOI18N
                        
            }
        }
    ;

singleValuedPathExpression
    :   p:pathExpression
        {
            int fieldTokenType = #p.getType();
            if ((fieldTokenType != SINGLE_CMR_FIELD_ACCESS) && 
                (fieldTokenType != CMP_FIELD_ACCESS)) {
                EJBQLAST classExpr = (EJBQLAST)#p.getFirstChild();
                EJBQLAST field = (EJBQLAST)classExpr.getNextSibling();
                ErrorMsg.error(field.getLine(), field.getColumn(),
                    I18NHelper.getMessage(msgs, "EXC_SingleValuedCMROrCMPFieldExpected", //NOI18N
                        field.getText(), typeSupport.getTypeName(field.getTypeInfo())));
                #p.setType(SINGLE_CMR_FIELD_ACCESS);
            }
        }
    ;

cmpPathExpression
    :   p:pathExpression
        {
            int fieldTokenType = #p.getType();
            if ((fieldTokenType != CMP_FIELD_ACCESS)) {
                EJBQLAST classExpr = (EJBQLAST)#p.getFirstChild();
                EJBQLAST field = (EJBQLAST)classExpr.getNextSibling();
                ErrorMsg.error(field.getLine(), field.getColumn(),
                    I18NHelper.getMessage(msgs, "EXC_CMPFieldExpected", //NOI18N
                        field.getText(), typeSupport.getTypeName(field.getTypeInfo())));
                #p.setType(CMP_FIELD_ACCESS);
            }
        }
    ;

singleValuedCmrPathExpression
    :   p:pathExpression
        {
            int fieldTokenType = #p.getType();
            if (fieldTokenType != SINGLE_CMR_FIELD_ACCESS) {
                EJBQLAST classExpr = (EJBQLAST)#p.getFirstChild();
                EJBQLAST field = (EJBQLAST)classExpr.getNextSibling();
                ErrorMsg.error(field.getLine(), field.getColumn(),
                    I18NHelper.getMessage(msgs, "EXC_SingleValuedCMRFieldExpected", //NOI18N
                        field.getText(), typeSupport.getTypeName(field.getTypeInfo())));
                #p.setType(COLLECTION_CMR_FIELD_ACCESS);
            }
        }
    ;

collectionValuedPathExpression
    :   p:pathExpression
        {
            int fieldTokenType = #p.getType();
            if (fieldTokenType != COLLECTION_CMR_FIELD_ACCESS) {
                EJBQLAST classExpr = (EJBQLAST)#p.getFirstChild();
                EJBQLAST field = (EJBQLAST)classExpr.getNextSibling();
                ErrorMsg.error(field.getLine(), field.getColumn(),
                    I18NHelper.getMessage(msgs, "EXC_CollectionValuedCMRFieldExpected", //NOI18N
                        field.getText(), typeSupport.getTypeName(field.getTypeInfo())));
                #p.setType(COLLECTION_CMR_FIELD_ACCESS);
            }
        }
    ;

inCollection [Object valueExprTypeInfo]
    :   ( inCollectionElement[valueExprTypeInfo] )+
    ;

inCollectionElement [Object valueExprTypeInfo]
    :   l:literal
        {
            l.setTypeInfo(analyseInCollectionElement(#l, valueExprTypeInfo));
        }
    |   i:inputParameter
        {
            i.setTypeInfo(analyseInCollectionElement(#i, valueExprTypeInfo));
        }
    ;

inputParameter
    :   param:INPUT_PARAMETER
        {
            Object typeInfo = typeSupport.getTypeInfo(
                paramSupport.getParameterType(#param.getText()));
            #param.setTypeInfo(typeInfo);
        }
    ;

