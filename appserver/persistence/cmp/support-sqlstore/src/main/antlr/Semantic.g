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
 * Created on March 8, 2000
 */

header
{
    package com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc;
    
    import java.util.Locale;
    import java.util.ResourceBundle;
    import java.util.Collection;
    
    import org.glassfish.persistence.common.I18NHelper;

    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.TypeTable;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.Type;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.ClassType;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.FieldInfo;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.NumericType;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.NumericWrapperClassType;
    
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.scope.SymbolTable;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.scope.Definition;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.scope.TypeName;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.scope.Variable;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.scope.Parameter;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.scope.Field;
}

/**
 * This class defines the semantic analysis of the JQL compiler.
 * Input of this pass is the AST as produced by the parser,
 * that consists of JQLAST nodes.
 * The result is a typed JQLAST tree.
 *
 * @author  Michael Bouschen
 * @author  Shing Wai Chan
 * @version 0.1
 */
class Semantic extends TreeParser;

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
        I18NHelper.loadBundle(Semantic.class);
    
    /**
     * symbol table handling names of fields, variables and parameters
     */
    protected SymbolTable symtab;
    
    /**
     * symbol table handling type names (candidate class and imported names)
     */
    protected SymbolTable typeNames;

    /**
     * type table 
     */
    protected TypeTable typetab;
    
    /**
     * query parameter table
     */
    protected ParameterTable paramtab;
    
    /**
     * variable table
     */
    protected VariableTable vartab;
    
    /**
     *
     */
    protected ErrorMsg errorMsg;
    
    /**
     * Result class for this query. This class is set by setClass.
     */
    protected ClassType candidateClass; 
    
    /**
     *
     */
    public void init(TypeTable typetab, ParameterTable paramtab, ErrorMsg errorMsg)
    {
        this.symtab = new SymbolTable();
        this.typeNames = new SymbolTable();
        this.vartab = new VariableTable(errorMsg);
        this.typetab = typetab;
        this.paramtab = paramtab;
        this.errorMsg = errorMsg;
    }

    /**
     *
     */
    public void reportError(RecognitionException ex) {
        errorMsg.fatal("Error: " + ex); //NOI18N
    }

    /**
     *
     */
    public void reportError(String s) {
        errorMsg.fatal("Error: " + s); //NOI18N
    }
    
    /**
     * Combines partial ASTs into one query AST.
     */
    public JQLAST createQueryAST(JQLAST candidateClass, 
                                 JQLAST importsAST, 
                                 JQLAST paramsAST, 
                                 JQLAST varsAST, 
                                 JQLAST orderingAST,
                                 JQLAST resultAST,
                                 JQLAST filterAST)
    {
        JQLAST query = new JQLAST(QUERY, "query", null); //NOI18N
        if (candidateClass != null)
            query.addChild(candidateClass);
        if (importsAST != null)
            query.addChild(importsAST);
        if (paramsAST != null)
            query.addChild(paramsAST);
        if (varsAST != null)
            query.addChild(varsAST);
        if (orderingAST != null)
            query.addChild(orderingAST);
        if (resultAST != null)
            query.addChild(resultAST);
        if (filterAST != null)
            query.addChild(filterAST);
        return query;
    }
    
    /**
     * Creates the CLASS_DEF AST that represents the setClass value.
     */
    public JQLAST checkCandidateClass(Class candidateClass)
    {
        Type type = typetab.checkType(candidateClass);
        if (type == null)
        {
            errorMsg.fatal(I18NHelper.getMessage(messages,
                "jqlc.semantic.checkcandidateclass.unknowntype", //NOI18N
                String.valueOf(candidateClass)));
        }
        return new JQLAST(CLASS_DEF, "classDef", type); //NOI18N
    }

    /**
     * This method analyses the expression of a single ordering definition.
     * It checks whether the expression
     * - is valid (see checkValidOrderingExpr)
     * - is of a orderable type
     * @param expr the expression of an ordering definition
     */
    protected void analyseOrderingExpression(JQLAST expr)
    {
        checkValidOrderingExpr(expr);
        Type exprType = expr.getJQLType();
        if (!exprType.isOrderable())
        {
            errorMsg.error(expr.getLine(), expr.getColumn(),
                I18NHelper.getMessage(messages, "jqlc.semantic.analyseorderingexpression.notorderable", //NOI18N 
                    exprType.getName()));
            expr.setJQLType(typetab.errorType);
        }
    }

    /**
     * This method checks whether the ordering expression is valid.
     * The following expressions are valid:
     * - field access using the this object
     * - navigation from a field of the this object
     * @param expr the ordering definition
     */
    private void checkValidOrderingExpr(JQLAST expr)
    {
        switch(expr.getType())
        {
        case THIS:
        case VARIABLE:
            //OK;
            break;
        case FIELD_ACCESS:
        case NAVIGATION:
            JQLAST child = (JQLAST)expr.getFirstChild();
            if (child != null)
            {
                // check first part of dot expr
                checkValidOrderingExpr(child);
            }
            break;
        default:
            errorMsg.error(expr.getLine(), expr.getColumn(), 
                I18NHelper.getMessage(messages, "jqlc.semantic.checkvalidorderingexpr.invalidordering", //NOI18N
                    expr.getText()));
        }
    }

    /**
     * This method checks whether the result expression is valid.
     * The following expressions are valid:
     * - field access using the this object
     * - navigation from a field of the this object
     * - variable access
     * - distinct expression
     * - aggreagte expression
     * @param expr the result expression
     */
    private void checkValidResultExpr(JQLAST expr)
    {
        switch(expr.getType())
        {
        case THIS:
            //OK;
            break;
        case FIELD_ACCESS:
        case NAVIGATION:
            JQLAST child = (JQLAST)expr.getFirstChild();
            if (child != null)
            {
                // check first part of dot expr
                checkValidResultExpr(child);
            }
            break;
        case VARIABLE:
            // OK
            break;
        case DISTINCT:
            checkValidResultExpr((JQLAST)expr.getFirstChild());
            break;
        case AVG:
        case SUM:
            if (!typetab.isNumberType(expr.getJQLType()) ||
                    typetab.isCharType(expr.getJQLType())) {
                errorMsg.error(expr.getLine(), expr.getColumn(), 
                    I18NHelper.getMessage(messages,
                    "jqlc.semantic.checkvalidresultexpr.invalidavgsumexpr", //NOI18N
                    expr.getJQLType().getName(), expr.getText()));
            }
            checkValidResultExpr((JQLAST)expr.getFirstChild());
            break;
        case MAX:
        case MIN:
            if (!expr.getJQLType().isOrderable()) {
                errorMsg.error(expr.getLine(), expr.getColumn(), 
                    I18NHelper.getMessage(messages,
                    "jqlc.semantic.checkvalidresultexpr.invalidminmaxexpr", //NOI18N
                    expr.getJQLType().getName(), expr.getText()));
            }
            checkValidResultExpr((JQLAST)expr.getFirstChild());
            break;
        case COUNT:
            checkValidResultExpr((JQLAST)expr.getFirstChild());
            break;            
        default:
            errorMsg.error(expr.getLine(), expr.getColumn(), 
                I18NHelper.getMessage(messages, "jqlc.semantic.checkvalidresultexpr.invalidresult", //NOI18N
                    expr.getText()));
        }
    }

    /**
     *  Checks that result and ordering are compatible.
     *  If the query result is a field, then it must be the same as ordering
     *  item. If the query is an object, then ordering expression must
     *  have the same navigation prefix of the result expression.
     */
    private void checkResultOrdering(JQLAST result, JQLAST ordering) {
        if (ordering == null) {
            return;
        }

        AST resultReturnAST = result;
        boolean hasResultDistinct = false;
        if (resultReturnAST == null) { // distinct THIS
            resultReturnAST = new JQLAST(THIS, "this", candidateClass);
            hasResultDistinct = true;
        }

        // skip RESULT_DEF node
        if (resultReturnAST.getType() == RESULT_DEF) {
            resultReturnAST = resultReturnAST.getFirstChild();
        }
        // skip DISTINCT node
        if (resultReturnAST.getType() == DISTINCT) {
            resultReturnAST = resultReturnAST.getFirstChild();
            hasResultDistinct = true;
        }

        if (!hasResultDistinct) {
            return;
        }

        if (resultReturnAST.getType() == FIELD_ACCESS) {
            StringBuffer buf = new StringBuffer();
            genPathExpression(resultReturnAST, buf);
            String resultReturnPathExpr = buf.toString();
            
            for (AST sibling = ordering;
                    sibling != null && sibling.getType() == ORDERING_DEF;
                    sibling = sibling.getNextSibling()) {
    
                // share buf
                buf.setLength(0);
                genPathExpression(sibling.getFirstChild().getNextSibling(), buf);
                String orderingItemExpr = buf.toString();
                if (!orderingItemExpr.equals(resultReturnPathExpr)) {
                    errorMsg.error(ordering.getLine(), ordering.getColumn(), 
                        I18NHelper.getMessage(messages, 
                            "jqlc.semantic.checkresultordering.invalidorderingfordistinctresultfield", //NOI18N
                            resultReturnPathExpr, orderingItemExpr)); 
                }
            }
        } else if (resultReturnAST.getType() == NAVIGATION ||
                resultReturnAST.getType() ==  THIS ) {
            StringBuffer buf = new StringBuffer();
            genPathExpression(resultReturnAST, buf);
            String resultReturnPathExpr = buf.toString();
            
            for (AST sibling = ordering;
                    sibling != null && sibling.getType() == ORDERING_DEF;
                    sibling = sibling.getNextSibling()) {
    
                // share buf
                buf.setLength(0);
                genPathExpression(sibling.getFirstChild().getNextSibling().getFirstChild(), buf);
                String orderingRootExpr = buf.toString();
                if (!orderingRootExpr.equals(resultReturnPathExpr)) {
                    buf.setLength(0);
                    genPathExpression(sibling.getFirstChild().getNextSibling(), buf);
                    errorMsg.error(ordering.getLine(), ordering.getColumn(), 
                        I18NHelper.getMessage(messages, 
                            "jqlc.semantic.checkresultordering.invalidorderingfordistinctresult", //NOI18N
                            resultReturnPathExpr, buf.toString())); 
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
    private void genPathExpression(AST ast, StringBuffer buf) {
        if (ast == null) {
            return;
        }
        switch (ast.getType()) {
            case FIELD_ACCESS:
            case STATIC_FIELD_ACCESS:
            case NAVIGATION:
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
     * This method analyses a dot expression of the form expr.ident or
     * expr.ident(params) where expr itself can again be a dot expression.
     * It checks whether the dot expression is 
     * - part of a qualified class name specification
     * - field access,
     * - a method call
     * The method returns a temporary single AST node that is defined with a
     * specific token type (field access, method call, etc.). This node also
     * contains the type of the dot expression.
     * @param expr the left hand side of the dot expression
     * @param ident the right hand side of the dot expression
     * @param args arguments (in the case of a call)
     * @return AST node representing the specialized dot expr
     */
    protected JQLAST analyseDotExpr(JQLAST dot, JQLAST expr, JQLAST ident, JQLAST args)
    {
        Type exprType = expr.getJQLType();
        String name = ident.getText();
        dot.setText(expr.getText() + '.' + name);
        if (exprType instanceof ClassType)
        {
            // left expression is of a class type
            ClassType classType = (ClassType)exprType;
            if (args == null)
            {
                // no paranethesis specified => field access
                FieldInfo fieldInfo = classType.getFieldInfo(name);
                if (fieldInfo == null)
                {
                    errorMsg.error(ident.getLine(), ident.getColumn(),
                                   I18NHelper.getMessage(messages, "jqlc.semantic.generic.unknownfield",  //NOI18N
                                                         ident.getText(), exprType.getName()));
                    dot.setJQLType(typetab.errorType);
                    ident.setJQLType(typetab.errorType);
                    return dot;
                }
                else if (expr.getType() == TYPENAME)
                {
                    // access of the form: className.staticField
                    return analyseStaticFieldAccess(dot, expr, ident, classType, fieldInfo);
                }
                else
                {
                    // access of the form: object.field
                    return analyseFieldAccess(dot, expr, ident, classType, fieldInfo);
                }
            }
            else
            {
                // parenthesis specified => method call
                if (typetab.isCollectionType(exprType))
                {
                    return analyseCollectionCall(dot, expr, ident, args);
                }
                else if (exprType.equals(typetab.stringType))
                {
                    return analyseStringCall(dot, expr, ident, args);
                }
                else if (typetab.isJavaLangMathType(exprType))
                {
                    return analyseMathCall(dot, expr, ident, args);
                }
                errorMsg.error(dot.getLine(), dot.getColumn(),  
                               I18NHelper.getMessage(messages, "jqlc.semantic.generic.invalidmethodcall")); //NOI18N
                dot.setJQLType(typetab.errorType);
                return dot;
            }
        }
        else
        {
            errorMsg.error(expr.getLine(), expr.getColumn(),
                           I18NHelper.getMessage(messages, "jqlc.semantic.analysedotexpr.classexprexpected", //NOI18N
                                                 ident.getText(), exprType.getName()));
            dot.setJQLType(typetab.errorType);
            return dot;
        }
    }

    /**
     * 
     */
    protected JQLAST analyseFieldAccess(JQLAST access, JQLAST objectExpr, JQLAST ident, 
                                        ClassType classType, FieldInfo fieldInfo)
    {
        String name = ident.getText();
        Type fieldType = fieldInfo.getType();
        if (classType.isPersistenceCapable())
        {
            if (!fieldInfo.isPersistent())
            {
                errorMsg.error(ident.getLine(), ident.getColumn(),  
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.analysefieldaccess.nonperistentfield", name, classType.getName())); //NOI18N
            }
            if (typetab.isPersistenceCapableType(fieldType))
            {
                access.setType(NAVIGATION);
            }
            else
            {
                access.setType(FIELD_ACCESS);
            }
        }
        else
        {
            if (!fieldInfo.isPublic())
            {
                errorMsg.error(ident.getLine(), ident.getColumn(),  
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.analysefieldaccess.nonpublicfield", name, classType.getName())); //NOI18N
            }
            access.setType(FIELD_ACCESS);
        }
        access.setText(objectExpr.getText() + '.' + name);
        access.setJQLType(fieldType);
        ident.setJQLType(fieldType);
        access.setFirstChild(objectExpr);
        objectExpr.setNextSibling(ident);
        return access;
    }

    /**
     * 
     */
    protected JQLAST analyseStaticFieldAccess(JQLAST access, JQLAST typename, JQLAST ident, 
                                              ClassType classType, FieldInfo fieldInfo)
    {
        String name = ident.getText();
        Type fieldType = fieldInfo.getType();
        if (!fieldInfo.isStatic())
        {
            errorMsg.error(ident.getLine(), ident.getColumn(),  
                I18NHelper.getMessage(messages, "jqlc.semantic.analysestaticfieldaccess.staticreference", //NOI18N 
                    ident.getText(), classType.getName()));
        }
        if (!fieldInfo.isPublic())
        {
            errorMsg.error(ident.getLine(), ident.getColumn(),  
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.analysestaticfieldaccess.nonpublicfield", name, classType.getName())); //NOI18N
        }
        access.setType(STATIC_FIELD_ACCESS);
        access.setText(typename.getText() + '.' + name);
        access.setJQLType(fieldType);
        ident.setJQLType(fieldType);
        access.setFirstChild(typename);
        typename.setNextSibling(ident);
        return access;
    }

    /**
     * This method analyses and identifier defined in the current scope: 
     * - a field, variable or parameter defined in the symbol table
     * - a type define in a separate symbol table for type names
     * @param ident the identifier AST
     * @param def the entry in the symbol table of the type names tables
     * @return AST node representing a defined identifier 
     */
    protected JQLAST analyseDefinedIdentifier(JQLAST ident, Definition def)
    {
        Type type = def.getType();
        if (def instanceof Variable)
        {
            ident.setType(VARIABLE);
        }
        else if (def instanceof Parameter)
        {   
            ident.setType(PARAMETER); 
        }
        else if (def instanceof Field)
        {
            FieldInfo fieldInfo = ((Field)def).getFieldInfo();
            JQLAST fieldAccessAST = ident;
            JQLAST identAST = new JQLAST(ident);
            if (fieldInfo.isStatic())
            {
                JQLAST typeNameAST = new JQLAST(TYPENAME, candidateClass.getName(), candidateClass);
                ident = analyseStaticFieldAccess(fieldAccessAST, typeNameAST, 
                                                 identAST, candidateClass, fieldInfo);
            }
            else
            {
                JQLAST thisAST = new JQLAST(THIS, "this", candidateClass); //NOI18N
                ident = analyseFieldAccess(fieldAccessAST, thisAST, 
                                           identAST, candidateClass, fieldInfo);
            }
        }
        else if (def instanceof TypeName)
        {
            ident.setType(TYPENAME);
            ident.setText(((TypeName)def).getQualifiedName());
        }
        else
        {
            type = typetab.errorType;
            errorMsg.fatal(I18NHelper.getMessage(messages,
                "jqlc.semantic.analysedefinedidentifier.illegalident", //NOI18N
                String.valueOf(def)));
        }
        ident.setJQLType(type);
        return ident;
    }
    
    /**
     * Analyses a call for an object that implements Collection. 
     * Currently, contains is the only valid Collection method in a query filter.
     */
    protected JQLAST analyseCollectionCall(JQLAST dot, JQLAST collection, JQLAST method, JQLAST args)
    {
        String methodName = method.getText();
        JQLAST firstArg = (JQLAST)args.getFirstChild();
        if (methodName.equals("contains")) //NOI18N
        {
            checkContainsArgs(collection, method, firstArg);
            dot.setType(CONTAINS);
            dot.setJQLType(typetab.booleanType);
            dot.setFirstChild(collection);
            collection.setNextSibling(firstArg);
            return dot;
        }
        else if (methodName.equals("isEmpty")) //NOI18N
        {
            // isEmpty does not take parameters
            checkNoArgs(method, firstArg);
            dot.setType(IS_EMPTY);
            dot.setJQLType(typetab.booleanType);
            dot.setFirstChild(collection);
            collection.setNextSibling(null);
            return dot;
        }
        
        errorMsg.error(dot.getLine(), dot.getColumn(),  
            I18NHelper.getMessage(messages, "jqlc.semantic.generic.invalidmethodcall"));  //NOI18N
        dot.setJQLType(typetab.errorType);
        return dot;
    }

    /**
     * Analyses a call for an object of type String.
     * Currently startsWith and endsWith are the only valid String methods in a query filter
     */
    protected JQLAST analyseStringCall(JQLAST dot, JQLAST string, JQLAST method, JQLAST args)
    {
        String methodName = method.getText();
        JQLAST firstArg = (JQLAST)args.getFirstChild();
        if (methodName.equals("startsWith")) //NOI18N
        {
            dot.setType(STARTS_WITH);
            checkOneStringArg(method, firstArg);
            dot.setJQLType(typetab.booleanType);
            dot.setFirstChild(string);
            string.setNextSibling(firstArg);
        }
        else if (methodName.equals("endsWith")) //NOI18N
        {
            dot.setType(ENDS_WITH);
            checkOneStringArg(method, firstArg);
            dot.setJQLType(typetab.booleanType);
            dot.setFirstChild(string);
            string.setNextSibling(firstArg);
        }
        else if (methodName.equals("like")) //NOI18N
        {
            checkLikeArgs(method, firstArg);
            dot.setType(LIKE);
            dot.setJQLType(typetab.booleanType);
            dot.setFirstChild(string);
            string.setNextSibling(firstArg);
        }
        else if (methodName.equals("substring")) //NOI18N
        {
            checkTwoIntArgs(method, firstArg);
            dot.setType(SUBSTRING);
            dot.setJQLType(typetab.stringType);
            dot.setFirstChild(string);
            string.setNextSibling(firstArg);
        }
        else if (methodName.equals("indexOf")) //NOI18N
        {
            checkIndexOfArgs(method, firstArg);
            dot.setType(INDEXOF);
            dot.setJQLType(typetab.intType);
            dot.setFirstChild(string);
            string.setNextSibling(firstArg);
        }
        else if (methodName.equals("length")) //NOI18N
        {
            // length does not take parameters
            checkNoArgs(method, firstArg);
            dot.setType(LENGTH);
            dot.setJQLType(typetab.intType);
            dot.setFirstChild(string);
            string.setNextSibling(null);
        }
        else
        {
            errorMsg.error(dot.getLine(), dot.getColumn(),  
                I18NHelper.getMessage(messages, "jqlc.semantic.generic.invalidmethodcall"));  //NOI18N
            dot.setJQLType(typetab.errorType);
        }
        return dot;
    }

    /**
     * Analyses a java.lang.Math call.
     */
    protected JQLAST analyseMathCall(JQLAST dot, JQLAST type, JQLAST method, JQLAST args)
    {
        String methodName = method.getText();
        JQLAST firstArg = (JQLAST)args.getFirstChild();
        if (methodName.equals("abs")) //NOI18N
        {
            checkAbsArgs(method, firstArg);
            dot.setType(ABS);
            dot.setJQLType(firstArg.getJQLType());
            dot.setFirstChild(firstArg);
        }
        else if (methodName.equals("sqrt")) //NOI18N
        {
            checkSqrtArgs(method, firstArg);
            dot.setType(SQRT);
            dot.setJQLType(firstArg.getJQLType());
            dot.setFirstChild(firstArg);
        }
        else
        {
            errorMsg.error(dot.getLine(), dot.getColumn(),  
                I18NHelper.getMessage(messages, "jqlc.semantic.generic.invalidmethodcall"));  //NOI18N
            dot.setJQLType(typetab.errorType);
        }
        return dot;
    }

    /**
     * This method checks the specified node (args) representing an empty 
     * argument list.
     */
    protected void checkNoArgs(JQLAST method, JQLAST firstArg)
    {
        if (firstArg != null) 
        {
            errorMsg.error(firstArg.getLine(), firstArg.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
    }

    /**
     * This method checks the specified node (args) representing an argument 
     * list which consists of a single argument of type String. 
     */
    protected void checkOneStringArg(JQLAST method, JQLAST firstArg)
    {
        if (firstArg == null)
        {
            errorMsg.error(method.getLine(), method.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else if (firstArg.getNextSibling() != null)
        {
            JQLAST nextArg = (JQLAST)firstArg.getNextSibling();
            errorMsg.error(nextArg.getLine(), nextArg.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else
        {
            Type argType = firstArg.getJQLType();
            if (!argType.equals(typetab.stringType))
            {
                errorMsg.error(firstArg.getLine(), firstArg.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.generic.arguments.typemismatch", //NOI18N
                        argType.getName(), typetab.stringType.getName()));
            }
        }
    }

    /**
     * This method checks the specified node (args) representing a valid contains 
     * argument list: one argument denoting a variable.
     */
    protected void checkContainsArgs(JQLAST collection, JQLAST method, JQLAST firstArg)
    {
        if (firstArg == null)
        {
            errorMsg.error(method.getLine(), method.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else if (firstArg.getNextSibling() != null)
        {
            JQLAST nextArg = (JQLAST)firstArg.getNextSibling();
            errorMsg.error(nextArg.getLine(), nextArg.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else if (firstArg.getType() != VARIABLE)
        {
            errorMsg.unsupported(firstArg.getLine(), firstArg.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.analysecollectioncall.nonvariable")); //NOI18N
        }
        else
        {
            FieldInfo collectionFieldInfo = getCollectionField(collection);
            if (collectionFieldInfo == null)
            {
                errorMsg.unsupported(collection.getLine(), collection.getColumn(),  
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.analysecollectioncall.unsupportedcollectionexpr", //NOI18N
                    collection.getText()));
            }
            else if (!collectionFieldInfo.isRelationship())
            {
                // check compatibilty of collection element type and type of variable
                errorMsg.error(collection.getLine(), collection.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.analysecollectioncall.relationshipexpected", //NOI18N
                        collectionFieldInfo.getName()));
            }
            Type variableType = firstArg.getJQLType();
            Type elementType = collectionFieldInfo.getAssociatedClass();
            if (!elementType.isCompatibleWith(variableType))
            {
                errorMsg.error(collection.getLine(), collection.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.analysecollectioncall.typemismatch", //NOI18N
                        elementType.getName(), variableType.getName()));
            }
        }
    }

    /**
     * This method checks the specified node (args) representing a valid like 
     * argument list: a string argument plus an optional char argument.
     */
    protected void checkLikeArgs(JQLAST method, JQLAST firstArg)
    {
        if (firstArg == null)
        {
            errorMsg.error(method.getLine(), method.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else if ((firstArg.getNextSibling() != null) && 
            (firstArg.getNextSibling().getNextSibling() != null))
        {
            JQLAST nextArg = (JQLAST)firstArg.getNextSibling().getNextSibling();
            errorMsg.error(nextArg.getLine(), nextArg.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else
        {
            // check type of first arg
            Type firstArgType = firstArg.getJQLType();
            if (!firstArgType.equals(typetab.stringType))
            {
                errorMsg.error(firstArg.getLine(), firstArg.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.generic.arguments.typemismatch", //NOI18N
                        firstArgType.getName(), typetab.stringType.getName()));
            }
            // check type of second arg (if available)
            JQLAST secondArg = (JQLAST)firstArg.getNextSibling();
            if (secondArg != null)
            {
                Type secondArgType = secondArg.getJQLType();
                if (!typetab.isCharType(secondArgType))
                {
                    errorMsg.error(secondArg.getLine(), secondArg.getColumn(),
                        I18NHelper.getMessage(messages, 
                            "jqlc.semantic.generic.arguments.typemismatch", //NOI18N
                        secondArgType.getName(), typetab.charType.getName()));
                }
            }
        }
    }

    /**
     * This method checks the specified node (args) representing an argument 
     * list which consists of two integer arguments.
     */
    protected void checkTwoIntArgs(JQLAST method, JQLAST firstArg)
    {
        if (firstArg == null)
        {
            // no args specified
            errorMsg.error(method.getLine(), method.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else if (firstArg.getNextSibling() == null)
        {
            // one arg specified
            errorMsg.error(method.getLine(), method.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else if (firstArg.getNextSibling().getNextSibling() != null)
        {
            // more than two args specified
            JQLAST nextArg = (JQLAST)firstArg.getNextSibling().getNextSibling();
            errorMsg.error(nextArg.getLine(), nextArg.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else
        {
            // specified two args
            // check type of first arg
            Type firstArgType = firstArg.getJQLType();
            if (!typetab.isIntType(firstArgType))
            {
                errorMsg.error(firstArg.getLine(), firstArg.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.generic.arguments.typemismatch", //NOI18N
                        firstArgType.getName(), typetab.intType.getName()));
            }
            // check type of second arg
            JQLAST secondArg = (JQLAST)firstArg.getNextSibling();
            Type secondArgType = firstArg.getJQLType();
            if (!typetab.isIntType(secondArgType))
            {
                errorMsg.error(secondArg.getLine(), secondArg.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.generic.arguments.typemismatch", //NOI18N
                        secondArgType.getName(), typetab.intType.getName()));
            }
        }
    }

    /**
     * This method checks the specified node (args) representing a valid indexOf 
     * argument list: a string argument plus an optional char argument.
     */
    protected void checkIndexOfArgs(JQLAST method, JQLAST firstArg)
    {
        if (firstArg == null)
        {
            errorMsg.error(method.getLine(), method.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else if ((firstArg.getNextSibling() != null) && 
            (firstArg.getNextSibling().getNextSibling() != null))
        {
            JQLAST nextArg = (JQLAST)firstArg.getNextSibling().getNextSibling();
            errorMsg.error(nextArg.getLine(), nextArg.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else
        {
            // check type of first arg
            Type firstArgType = firstArg.getJQLType();
            if (!firstArgType.equals(typetab.stringType))
            {
                errorMsg.error(firstArg.getLine(), firstArg.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.generic.arguments.typemismatch", //NOI18N
                        firstArgType.getName(), typetab.stringType.getName()));
            }
            // check type of second arg (if available)
            JQLAST secondArg = (JQLAST)firstArg.getNextSibling();
            if (secondArg != null)
            {
                Type secondArgType = secondArg.getJQLType();
                if (!typetab.isIntType(secondArgType))
                {
                    errorMsg.error(secondArg.getLine(), secondArg.getColumn(),
                        I18NHelper.getMessage(messages, 
                            "jqlc.semantic.generic.arguments.typemismatch", //NOI18N
                            secondArgType.getName(), typetab.intType.getName()));
                }
            }
        }
    }

    /**
     * This method checks the specified node (args) representing a valid abs
     * argument list: a single number argument.
     */
    protected void checkAbsArgs(JQLAST method, JQLAST firstArg)
    {
        if (firstArg == null)
        {
            errorMsg.error(method.getLine(), method.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else if (firstArg.getNextSibling() != null)
        {
            JQLAST nextArg = (JQLAST)firstArg.getNextSibling();
                errorMsg.error(nextArg.getLine(), nextArg.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else
        {
            Type argType = firstArg.getJQLType();
            if (!typetab.isNumberType(argType))
            {
                errorMsg.error(firstArg.getLine(), firstArg.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.generic.arguments.typemismatch", //NOI18N
                        argType.getName(), "number type"));
            }
        }
    }

    /**
     * This method checks the specified node (args) representing a valid sqrt
     * argument list: a single argument of type double or Double.
     */
    protected void checkSqrtArgs(JQLAST method, JQLAST firstArg)
    {
        if (firstArg == null)
        {
            errorMsg.error(method.getLine(), method.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else if (firstArg.getNextSibling() != null)
        {
            JQLAST nextArg = (JQLAST)firstArg.getNextSibling();
                errorMsg.error(nextArg.getLine(), nextArg.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.semantic.generic.arguments.numbermismatch")); //NOI18N
        }
        else
        {
            Type argType = firstArg.getJQLType();
            if (!typetab.isDoubleType(argType))
            {
                errorMsg.error(firstArg.getLine(), firstArg.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.generic.arguments.typemismatch", //NOI18N
                        argType.getName(), "double or Double"));
            }
        }
    }

    /**
     *
     */
    protected FieldInfo getCollectionField(JQLAST expr)
    {
        JQLAST child = (JQLAST)expr.getFirstChild();
        switch (expr.getType())
        {
        case FIELD_ACCESS:
        case NAVIGATION:
            if ((child != null) && (child.getNextSibling() != null))
            {
                ClassType classType = (ClassType)child.getJQLType();
                String fieldName = child.getNextSibling().getText();
                return classType.getFieldInfo(fieldName);
            }
            errorMsg.fatal(I18NHelper.getMessage(messages, "jqlc.semantic.getcollectionfield.missingchildren")); //NOI18N
            break;
        case TYPECAST:
            if ((child != null) && (child.getNextSibling() != null))
            {
                return getCollectionField((JQLAST)child.getNextSibling());
            }
            errorMsg.fatal(I18NHelper.getMessage(messages, "jqlc.semantic.getcollectionfield.missingchildren")); //NOI18N
            break;
        }
        return null;
    }

    /**
     * Analyses a bitwise/logical operation (&, |, ^)
     * @param op the bitwise/logical operator
     * @param leftAST left operand 
     * @param rightAST right operand
     * @return Type
     */
    protected Type analyseBitwiseExpr(JQLAST op, JQLAST leftAST, JQLAST rightAST)
    {
        Type left = leftAST.getJQLType();
        Type right = rightAST.getJQLType();
        
        // handle error type
        if (left.equals(typetab.errorType) || right.equals(typetab.errorType))
            return typetab.errorType;
        
        switch(op.getType())
        {
        case BAND:
        case BOR:
            if (typetab.isBooleanType(left) && typetab.isBooleanType(right))
                return typetab.booleanType;
            else if (typetab.isIntegralType(left) || typetab.isIntegralType(right))
            {
                errorMsg.unsupported(op.getLine(), op.getColumn(), 
                    I18NHelper.getMessage(messages, "jqlc.semantic.analysebitwiseexpr.integerbitwiseop", //NOI18N
                        op.getText()));
                return typetab.errorType;
            }
            break;
        case BXOR:
            if (typetab.isBooleanType(left) && typetab.isBooleanType(right))
            {
                errorMsg.unsupported(op.getLine(), op.getColumn(), 
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.analysebitwiseexpr.exclusiveorop")); //NOI18N
                return typetab.errorType;
            }
            else if (typetab.isIntegralType(left) || typetab.isIntegralType(right))
            {
                errorMsg.unsupported(op.getLine(), op.getColumn(), 
                    I18NHelper.getMessage(messages, "jqlc.semantic.analysebitwiseexpr.integerbitwiseop",  //NOI18N
                        op.getText()));
                return typetab.errorType;
            }
            break;
        }

        // if this code is reached a bitwise operator was used with invalid arguments
        errorMsg.error(op.getLine(), op.getColumn(), 
            I18NHelper.getMessage(messages, "jqlc.semantic.generic.arguments.invalid", //NOI18N
                op.getText()));
        return typetab.errorType;
    }
    
    /**
     * Analyses a boolean conditional operation (&&, ||)
     * @param op the conditional operator
     * @param leftAST left operand 
     * @param rightAST right operand
     * @return Type
     */
    protected Type analyseConditionalExpr(JQLAST op, JQLAST leftAST, JQLAST rightAST)
    {
        Type left = leftAST.getJQLType();
        Type right = rightAST.getJQLType();

        // handle error type
        if (left.equals(typetab.errorType) || right.equals(typetab.errorType))
            return typetab.errorType;

        switch(op.getType())
        {
        case AND:
        case OR:
            if (typetab.isBooleanType(left) && typetab.isBooleanType(right))
                return typetab.booleanType;
            break;
        }
        
        // if this code is reached a conditional operator was used with invalid arguments
        errorMsg.error(op.getLine(), op.getColumn(), 
            I18NHelper.getMessage(messages, "jqlc.semantic.generic.arguments.invalid", //NOI18N
                op.getText()));
        return typetab.errorType;
    }

    /**
     * Analyses a relational operation (<, <=, >, >=, ==, !=)
     * @param op the relational operator
     * @param leftAST left operand 
     * @param rightAST right operand
     * @return Type 
     */
    protected Type analyseRelationalExpr(JQLAST op, JQLAST leftAST, JQLAST rightAST)
    {
        Type left = leftAST.getJQLType();
        Type right = rightAST.getJQLType();

        // handle error type
        if (left.equals(typetab.errorType) || right.equals(typetab.errorType))
            return typetab.errorType;

        // special check for <, <=, >, >=
        // left and right hand types must be orderable
        switch(op.getType())
        {
        case LT:
        case LE:
        case GT:
        case GE:
            if (!left.isOrderable())
            {
                errorMsg.error(op.getLine(), op.getColumn(),
                    I18NHelper.getMessage(messages, "jqlc.semantic.analyserelationalexpr.notorderable", //NOI18N
                        left.getName(), op.getText()));
                return typetab.errorType;
            }
            if (!right.isOrderable())
            {
                errorMsg.error(op.getLine(), op.getColumn(),
                    I18NHelper.getMessage(messages, "jqlc.semantic.analyserelationalexpr.notorderable", //NOI18N 
                        right.getName(), op.getText()));
                return typetab.errorType;
            }
            break;
        case EQUAL:
        case NOT_EQUAL:
            if ((leftAST.getType() == CONTAINS) || (rightAST.getType() == CONTAINS))
            {
                errorMsg.unsupported(op.getLine(), op.getColumn(),
                    I18NHelper.getMessage(messages, 
                        "jqlc.semantic.generic.unsupportedconstraintop", op.getText())); //NOI18N
                return typetab.errorType;
            }
            break;
        }
        
        // check for numeric types, numeric wrapper class types and math class types
        if (typetab.isNumberType(left) && typetab.isNumberType(right))
            return typetab.booleanType;

        // check for boolean and java.lang.Boolean
        if (typetab.isBooleanType(left) && typetab.isBooleanType(right))
            return typetab.booleanType;

        if (left.isCompatibleWith(right) || right.isCompatibleWith(left))
            return typetab.booleanType;
        
        // if this code is reached a conditional operator was used with invalid arguments
        errorMsg.error(op.getLine(), op.getColumn(), 
            I18NHelper.getMessage(messages, "jqlc.semantic.generic.arguments.invalid", //NOI18N 
                op.getText()));
        return typetab.errorType;
    }
    
    /**
     * Analyses a 
     * @param op the  operator
     * @param leftAST left operand 
     * @param rightAST right operand
     * @return Type
     */
    protected Type analyseBinaryArithmeticExpr(JQLAST op, JQLAST leftAST, JQLAST rightAST)
    {
        Type left = leftAST.getJQLType();
        Type right = rightAST.getJQLType();

        // handle error type
        if (left.equals(typetab.errorType) || right.equals(typetab.errorType))
            return typetab.errorType;

        if (typetab.isNumberType(left) && typetab.isNumberType(right))
        {
            // handle java.math.BigDecimal
            if (left.isCompatibleWith(typetab.bigDecimalType))
                return left;
            if (right.isCompatibleWith(typetab.bigDecimalType))
                return right;
            
            // handle java.math.BigInteger
            if (left.isCompatibleWith(typetab.bigIntegerType))
            {
                // if right is floating point return BigDecimal, 
                // otherwise return BigInteger
                return typetab.isFloatingPointType(right) ? 
                       typetab.bigDecimalType : left;
            }
            if (right.isCompatibleWith(typetab.bigIntegerType))
            {
                // if left is floating point return BigDecimal, 
                // otherwise return BigInteger
                return typetab.isFloatingPointType(left) ? 
                       typetab.bigDecimalType : right;
            }       

            boolean wrapper = false;
            if (left instanceof NumericWrapperClassType)
            {
                left = ((NumericWrapperClassType)left).getPrimitiveType();
                wrapper = true;
            }
            if (right instanceof NumericWrapperClassType)
            {
                right = ((NumericWrapperClassType)right).getPrimitiveType();
                wrapper = true;
            }
            
            // handle numeric types with arbitrary arithmetic operator
            if ((left instanceof NumericType) && (right instanceof NumericType))
            {
                Type promotedType = typetab.binaryNumericPromotion(left, right);
                if (wrapper && (promotedType instanceof NumericType))
                {   
                    promotedType =  ((NumericType)promotedType).getWrapper();
                }
                return promotedType;
            }
        }
        else if (op.getType() == PLUS)
        {
            // handle + for strings
            // MBO: note, this if matches char + char (which it should'nt),
            // but this case is already handled above 
            if ((left.equals(typetab.stringType) || left.equals(typetab.charType)) && 
                (right.equals(typetab.stringType) || right.equals(typetab.charType)))
            {
                return typetab.stringType;
            }
        }

        // if this code is reached a conditional operator was used with invalid arguments
        errorMsg.error(op.getLine(), op.getColumn(), 
            I18NHelper.getMessage(messages, "jqlc.semantic.generic.arguments.invalid", //NOI18N
                op.getText()));
        return typetab.errorType;
    }

    /**
     * Analyses a 
     * @param op the operator
     * @param argAST right operand
     * @return Type 
     */
    protected Type analyseUnaryArithmeticExpr(JQLAST op, JQLAST argAST)
    {
        Type arg = argAST.getJQLType();

        // handle error type
        if (arg.equals(typetab.errorType))
            return typetab.errorType;
        
        // handle java.math.BigDecimal and java.math.BigInteger
        if (arg.isCompatibleWith(typetab.bigDecimalType))
            return arg;

        // handle java.math.BigInteger
        if (arg.isCompatibleWith(typetab.bigIntegerType))
            return arg;

        boolean wrapper = false;
        if (arg instanceof NumericWrapperClassType)
        {
            arg = ((NumericWrapperClassType)arg).getPrimitiveType();
            wrapper = true;
        }

        if (arg instanceof NumericType)
        {
            Type promotedType = typetab.unaryNumericPromotion(arg);
            if (wrapper && (promotedType instanceof NumericType))
            {
                promotedType =  ((NumericType)promotedType).getWrapper();
            }
            return promotedType;
        }
        
        // if this code is reached a conditional operator was used with invalid arguments
        errorMsg.error(op.getLine(), op.getColumn(), 
            I18NHelper.getMessage(messages, "jqlc.semantic.generic.arguments.invalid", //NOI18N
                op.getText()));
        return typetab.errorType;
    }
    /**
     * Analyses a 
     * @param op the operator
     * @param argAST right operand
     * @return Type 
     */
    protected Type analyseComplementExpr(JQLAST op, JQLAST argAST)
    {
        Type arg = argAST.getJQLType();

        // handle error type
        if (arg.equals(typetab.errorType))
            return typetab.errorType;

        switch(op.getType())
        {
        case BNOT:
            if (typetab.isIntegralType(arg))
            {
                return arg;
            }
            break;
        case LNOT:
            if (typetab.isBooleanType(arg))
            {
                if (argAST.getType() == CONTAINS)
                {
                    errorMsg.unsupported(op.getLine(), op.getColumn(),
                        I18NHelper.getMessage(messages, 
                            "jqlc.semantic.generic.unsupportedconstraintop", op.getText())); //NOI18N
                    return typetab.errorType;
                }
                return arg;
            }
            break;
        }
        
        // if this code is reached a conditional operator was used with invalid arguments
        errorMsg.error(op.getLine(), op.getColumn(), 
            I18NHelper.getMessage(messages, "jqlc.semantic.generic.arguments.invalid", //NOI18N 
                op.getText()));
        return typetab.errorType;
    }
    
    /**
     *
     */
    protected void checkConstraints(JQLAST ast, VariableTable tab)
    {
        checkConstraints(ast, null, tab);
    }

    /**
     *
     */
    protected void checkConstraints(JQLAST ast, String dependentVariable, VariableTable tab)
    {
        if (ast == null) return;
        switch (ast.getType())
        {
        case VARIABLE:  
            tab.markUsed(ast, dependentVariable);
            break;
        case CONTAINS:
            JQLAST expr = (JQLAST)ast.getFirstChild();
            JQLAST var = (JQLAST)expr.getNextSibling();
            checkConstraints(expr, var.getText(), tab);
            tab.markConstraint(var, expr);
            break;
        case BOR:
        case BXOR:
        case OR:
            JQLAST left = (JQLAST)ast.getFirstChild();
            JQLAST right = (JQLAST)left.getNextSibling();
            // prepare tab copy for right hand side and merge the right hand side copy into vartab
            VariableTable copy = new VariableTable(tab);
            checkConstraints(left, dependentVariable, tab);
            checkConstraints(right, dependentVariable, copy);
            tab.merge(copy);
            break;
        default:
            for (JQLAST node = (JQLAST)ast.getFirstChild(); node != null; node = (JQLAST)node.getNextSibling())
            {
                checkConstraints(node, dependentVariable, tab);
            }
            break;
        }
    }

}

// rules

query
    :   #(  QUERY
            {   
                symtab.enterScope(); 
                typeNames.enterScope();
            }
            candidateClass
            imports
            {
                // enter new scope for variable and parameter names
                symtab.enterScope();
            }
            parameters
            variables
            o:ordering
            r:result
            filter
            {   
                typeNames.leaveScope();
                // leaves variable and parameter name scope
                symtab.leaveScope();
                // leaves global scope
                symtab.leaveScope(); 
            }
        )
        {
            checkResultOrdering(#r, #o);
        }
    ;

// ----------------------------------
// rules: candidate class
// ----------------------------------

candidateClass
{   
    errorMsg.setContext("setClass"); //NOI18N
}
    :   c:CLASS_DEF
        {
            // check persistence capable
            candidateClass = (ClassType)#c.getJQLType();
            String className = candidateClass.getName();
            if (!candidateClass.isPersistenceCapable())
            {
                errorMsg.unsupported(#c.getLine(), #c.getColumn(),
                    I18NHelper.getMessage(messages, "jqlc.semantic.candidateclass.nonpc", //NOI18N 
                        className));
            }

            // get base name
            int index = className.lastIndexOf('.');
            String identName = index>0 ? className.substring(index+1) : className;
            typeNames.declare(identName, new TypeName(candidateClass, className));

            // init symbol table with field names of the candidate class
            FieldInfo[] fieldInfos = candidateClass.getFieldInfos();
            for (int i = 0; i < fieldInfos.length; i++)
            {
                FieldInfo fieldInfo = fieldInfos[i];
                symtab.declare(fieldInfo.getName(), new Field(fieldInfo));
            }
        }
    ;

// ----------------------------------
// rules: import declaration
// ----------------------------------

imports!
{   
    errorMsg.setContext("declareImports"); //NOI18N
}
    :   ( declareImport )*
    ;

declareImport
    {  String name = null; }
    :   #( i:IMPORT_DEF name = qualifiedName )
        {
            Type type = typetab.checkType(name);
            if (type == null)
            {
                errorMsg.error(#i.getLine(), #i.getColumn(),
                    I18NHelper.getMessage(messages, "jqlc.semantic.generic.unknowntype", name)); //NOI18N
            }

            // get base name
            int index = name.lastIndexOf('.');
            String identName = index>0 ? name.substring(index+1) : name;

            Definition old = typeNames.declare(identName, new TypeName(type, name));
            if (old != null)
            {
                errorMsg.error(#i.getLine(), #i.getColumn(),
                    I18NHelper.getMessage(messages, "jqlc.semantic.generic.alreadydeclared", //NOI18N
                        identName, old.getName()));
            }
        }
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
    :   #( PARAMETER_DEF t:type i:IDENT )
        {
            String name = #i.getText();
            Type type = #t.getJQLType();
            Definition old = symtab.declare(name, new Parameter(type));
            if (old != null)
            {
                errorMsg.error(#i.getLine(), #i.getColumn(),
                    I18NHelper.getMessage(messages, "jqlc.semantic.generic.alreadydeclared", //NOI18N
                        name, old.getName()));
            }
            #i.setJQLType(type);
            paramtab.add(name, type); 
        }
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
    :   #( VARIABLE_DEF t:type i:IDENT )
        {
            String name = #i.getText();
            Type type = #t.getJQLType();
            Definition old = symtab.declare(name, new Variable(type));
            if (old != null)
            {
                errorMsg.error(#i.getLine(), #i.getColumn(),
                    I18NHelper.getMessage(messages, "jqlc.semantic.generic.alreadydeclared", //NOI18N
                        name, old.getName()));
            }
            vartab.add(name);
            #i.setJQLType(type);
        }
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
    :   #( ORDERING_DEF (ASCENDING | DESCENDING) e:expression )
        {
            analyseOrderingExpression(#e);
            checkConstraints(#e, vartab);
        }
    ;

// ----------------------------------
// rules: result expression
// ----------------------------------

result
{   
    errorMsg.setContext("setResult"); //NOI18N
}
    :   #( r:RESULT_DEF e:resultExpr )
        {
            checkValidResultExpr(#e);
            #r.setJQLType(#e.getJQLType());
            checkConstraints(#e, vartab);
        }
    |   // empty rule
    ;

resultExpr
    :   #( d:DISTINCT  e0:resultExpr )
        {
            #d.setJQLType(#e0.getJQLType());
        }
    |   #( a:AVG e1:resultExpr )
        {
            #a.setJQLType(typetab.getAvgReturnType(#e1.getJQLType()));
        }
    |   #( max:MAX e2:resultExpr )
        {
            #max.setJQLType(typetab.getMinMaxReturnType(#e2.getJQLType()));
        }
    |   #( min:MIN e3:resultExpr )
        {
            #min.setJQLType(typetab.getMinMaxReturnType(#e3.getJQLType()));
        }
    |   #( s:SUM e4:resultExpr )
        {
            #s.setJQLType(typetab.getSumReturnType(#e4.getJQLType()));
        }
    |   #( c:COUNT resultExpr )
        {
            #c.setJQLType(typetab.longType);
        }
    |   expression
    ;

// ----------------------------------
// rules: filer expression
// ----------------------------------

filter
{   
    errorMsg.setContext("setFilter"); //NOI18N
}
        // There is always a filter defined and it is the last node of the query tree.
        // Otherwise all the remaining subtrees after the CLASS_DEF subtree are empty
        // which results in a ClassCastException antlr.ASTNullType when analysis 
        // the (non existsent) subtrees
    :   #( FILTER_DEF e:expression )
        {
            Type exprType = #e.getJQLType();
            if (!(typetab.isBooleanType(exprType) || exprType.equals(typetab.errorType)))
            {
                // filter expression must have the type boolean or java.lang.Boolean
                errorMsg.error(#e.getLine(), #e.getColumn(),
                    I18NHelper.getMessage(messages, "jqlc.semantic.filter.booleanexpected", exprType)); //NOI18N
            }
            checkConstraints(#e, vartab);
            vartab.checkConstraints();
        }
    ;

expression
    {   String repr; }
    :   repr = e:exprNoCheck[false]
        {
            if (repr != null)
            {
               #e.setJQLType(typetab.errorType);
               errorMsg.error(#e.getLine(), #e.getColumn(),
                    I18NHelper.getMessage(messages, "jqlc.semantic.expression.undefined", repr)); //NOI18N
            }
        }
    ;

exprNoCheck [boolean insideDotExpr] returns [String repr]
    {   repr = null; }  // repr is used to get the text of identifier
                        // inside a dot expression
    :   bitwiseExpr
    |   conditionalExpr
    |   relationalExpr
    |   binaryArithmeticExpr
    |   unaryArithmeticExpr
    |   complementExpr
    |   repr = primary[insideDotExpr]
    ;

bitwiseExpr
    :   #( op1:BAND left1:expression right1:expression )
        {
            #op1.setJQLType(analyseBitwiseExpr(#op1, #left1, #right1));
        }
    |   #( op2:BOR  left2:expression right2:expression )
        {
            #op2.setJQLType(analyseBitwiseExpr(#op2, #left2, #right2));
        }
    |   #( op3:BXOR left3:expression right3:expression )
        {
            #op3.setJQLType(analyseBitwiseExpr(#op3, #left3, #right3));
        }
    ;

conditionalExpr
    :   #( op1:AND left1:expression right1:expression )
        {
            #op1.setJQLType(analyseConditionalExpr(#op1, #left1, #right1));
        }
    |   #( op2:OR  left2:expression right2:expression )
        {
            #op2.setJQLType(analyseConditionalExpr(#op2, #left2, #right2));
        }
    ;

relationalExpr
{
    Type left = null;
    Type right = null;
}
    :   #( op1:EQUAL left1:expression right1:expression )
        {
            #op1.setJQLType(analyseRelationalExpr(#op1, #left1, #right1));
            left = #left1.getJQLType();
            right = #right1.getJQLType();
            if (typetab.isPersistenceCapableType(left) || typetab.isPersistenceCapableType(right))
            {
                #op1.setType(OBJECT_EQUAL);
            }
            else if (typetab.isCollectionType(left) || typetab.isCollectionType(right))
            {
                #op1.setType(COLLECTION_EQUAL);
            }
        }
    |   #(  op2:NOT_EQUAL left2:expression right2:expression )
        {
            #op2.setJQLType(analyseRelationalExpr(#op2, #left2, #right2));
            left = #left2.getJQLType();
            right = #right2.getJQLType();
            if (typetab.isPersistenceCapableType(left) || typetab.isPersistenceCapableType(right))
            {
                #op2.setType(OBJECT_NOT_EQUAL);
            }
            else if (typetab.isCollectionType(left) || typetab.isCollectionType(right))
            {
                #op2.setType(COLLECTION_NOT_EQUAL);
            }
        }
    |   #(  op3:LT left3:expression right3:expression )
        {
            #op3.setJQLType(analyseRelationalExpr(#op3, #left3, #right3));
        }
    |   #(  op4:GT left4:expression right4:expression )
        {
            #op4.setJQLType(analyseRelationalExpr(#op4, #left4, #right4));
        }
    |   #(  op5:LE left5:expression right5:expression )
        {
            #op5.setJQLType(analyseRelationalExpr(#op5, #left5, #right5));
        }
    |   #(  op6:GE left6:expression right6:expression )
        {
            #op6.setJQLType(analyseRelationalExpr(#op6, #left6, #right6));
        }
    ;

binaryArithmeticExpr
    :   #( op1:PLUS left1:expression right1:expression )
        {
            #op1.setJQLType(analyseBinaryArithmeticExpr(#op1, #left1, #right1));
            if (#op1.getJQLType().equals(typetab.stringType))
            {
                // change the operator from PLUS to CONCAT in the case of string concatenation
                #op1.setType(CONCAT);
            }
        }
    |   #( op2:MINUS left2:expression right2:expression )
        {
            #op2.setJQLType(analyseBinaryArithmeticExpr(#op2, #left2, #right2));
        }
    |   #( op3:STAR left3:expression right3:expression )
        {
            #op3.setJQLType(analyseBinaryArithmeticExpr(#op3, #left3, #right3));
        }
    |   #( op4:DIV left4:expression right4:expression )
        {
            #op4.setJQLType(analyseBinaryArithmeticExpr(#op4, #left4, #right4));
        }
    |   #( op5:MOD left5:expression right5:expression )
        {
            #op5.setJQLType(analyseBinaryArithmeticExpr(#op5, #left5, #right5));
        }
    ;

unaryArithmeticExpr
    :   #( op1:UNARY_PLUS arg1:expression )
        {
            #op1.setJQLType(analyseUnaryArithmeticExpr(#op1, #arg1));
        }
    |   #( op2:UNARY_MINUS arg2:expression )
        {
            #op2.setJQLType(analyseUnaryArithmeticExpr(#op2, #arg2));
        }
    ;

complementExpr
    :   #( op1:BNOT arg1:expression )
        {
            #op1.setJQLType(analyseComplementExpr(#op1, #arg1));
        }
    |   #( op2:LNOT arg2:expression )
        {
            #op2.setJQLType(analyseComplementExpr(#op2, #arg2));
        }
    ;

primary [boolean insideDotExpr] returns [String repr]
{   repr = null; } 
    :   #( c:TYPECAST t:type e:expression )
        {
            Type type = #t.getJQLType();
            Type exprType = #e.getJQLType();
            if (!(type.isCompatibleWith(exprType) || exprType.isCompatibleWith(type)))
            {
                errorMsg.error(#c.getLine(), #c.getColumn(),
                    I18NHelper.getMessage(messages, "jqlc.semantic.primary.invalidcast", //NOI18N
                        exprType.getName(), type.getName()));
                type = typetab.errorType;
            }
            #c.setJQLType(type);
        }
    |   literal
    |   i:THIS
        { #i.setJQLType(candidateClass); }
    |   repr = dotExpr
    |   repr = identifier [insideDotExpr]
    ;
 
dotExpr returns [String repr]
    {
        repr = null;
    }
    :   #( dot:DOT 
           repr = expr:exprNoCheck[true] ident:IDENT ( args:argList )? 
         )
        {
            Type type = null;
            if (repr != null) // possible package name
            {
                String qualifiedName = repr + '.' + #ident.getText();
                type = typetab.checkType(qualifiedName);
                if (type == null)
                {
                    // name does not define a valid class => return qualifiedName
                    repr = qualifiedName;
                }
                else if (#args == null)
                {
                    // found valid class name and NO arguments specified
                    // => use of the class name
                    repr = null;
                    #dot.setType(TYPENAME);
                    #dot.setText(qualifiedName);
                    #dot.setFirstChild(null);
                }
                else
                {
                    // found valid class name and arguments specified =>
                    // looks like constructor call
                    repr = null;
                    errorMsg.error(dot.getLine(), dot.getColumn(),  
                        I18NHelper.getMessage(messages, "jqlc.semantic.generic.invalidmethodcall")); //NOI18N
               }
                #dot.setJQLType(type);
                #dot.setText(#expr.getText() + '.' + #ident.getText());
            }
            else // no string repr of left hand side => expression is defined
            {
                #dotExpr = analyseDotExpr(#dot, #expr, #ident, #args);
            }
        }
    ;

argList
    :   #( ARG_LIST (expression)* )
    ;

identifier [boolean insideDotExpr] returns [String repr]
    {
        repr = null;   // repr is set when ident is part of a package name spec
    }
    :   ident:IDENT ( args:argList ) ?
        {
            String name = #ident.getText();
            Definition def = symtab.getDefinition(name);

            // check args, if defined => invalid method call
            if (#args != null)
            {
                #ident.setJQLType(typetab.errorType);
                errorMsg.error(#ident.getLine(), #ident.getColumn(),  
                    I18NHelper.getMessage(messages, "jqlc.semantic.generic.invalidmethodcall")); //NOI18N
            }
            else if (def != null)
            {
                #ident = analyseDefinedIdentifier(#ident, def);
            }
            else if (insideDotExpr)
            {
                Definition typedef = typeNames.getDefinition(name);
                if (typedef != null)
                {
                    #ident = analyseDefinedIdentifier(#ident, typedef);
                }
                else 
                {
                    repr = #ident.getText();
                }
            }
            else
            {
                #ident.setJQLType(typetab.errorType);
                errorMsg.error(ident.getLine(), ident.getColumn(),
                    I18NHelper.getMessage(messages, "jqlc.semantic.identifier.undefined", //NOI18N
                        ident.getText()));
            }
        }
    ;

literal
    :   b1:TRUE          { #b1.setJQLType(typetab.booleanType); }
    |   b2:FALSE         { #b2.setJQLType(typetab.booleanType); }
    |   i:INT_LITERAL    { #i.setJQLType(typetab.intType); }
    |   l:LONG_LITERAL   { #l.setJQLType(typetab.longType); }
    |   f:FLOAT_LITERAL  { #f.setJQLType(typetab.floatType); }
    |   d:DOUBLE_LITERAL { #d.setJQLType(typetab.doubleType); }
    |   c:CHAR_LITERAL   { #c.setJQLType(typetab.charType); }
    |   s:STRING_LITERAL { #s.setJQLType(typetab.stringType); }
    |   n:NULL           { #n.setJQLType(typetab.nullType); }
    ;

qualifiedName returns [String name]
    {   name = null; }
    :   id1:IDENT
        {
            name = #id1.getText();
        }
    |   #(  d:DOT
            name = qualifiedName
            id2:IDENT
            {
                name += (#d.getText() + #id2.getText());
            }
        )
    ;

type
    { String name = null; }
    :   name = qn:qualifiedName
        {
            Type type = null;
            if (typeNames.isDeclared(name))
            {
                Definition def = typeNames.getDefinition(name);
                if (def instanceof TypeName)
                {
                    type = def.getType();
                }
                else
                {
                    errorMsg.error(#qn.getLine(), #qn.getColumn(),
                        I18NHelper.getMessage(messages, "jqlc.semantic.type.notype", //NOI18N
                            name, def.getName())); 
                }
            }
            else
            {
                type = typetab.checkType(name);
                if ((type == null) && (name.indexOf('.') == -1))
                {
                    // ckeck java.lang class without package name 
                    type = typetab.checkType("java.lang." + name); //NOI18N
                }
                if (type == null)
                {
                    errorMsg.error(#qn.getLine(), #qn.getColumn(),
                        I18NHelper.getMessage(messages, "jqlc.semantic.generic.unknowntype", name)); //NOI18N
                }
            }
            // change AST to a single node that represents the full class name
            #qn.setType(TYPENAME);
            #qn.setText(name);
            #qn.setFirstChild(null);
            #qn.setJQLType(type);
        }
    |   p:primitiveType
        {
            #p.setJQLType(typetab.checkType(#p.getText()));
            #p.setType(TYPENAME);
        }
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

