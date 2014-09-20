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
 * JDOQLCodeGeneration.g
 *
 * Created on Decemember 10, 2001
 */

header
{
    package com.sun.jdo.spi.persistence.support.ejb.ejbqlc;
    
    import java.util.ResourceBundle;
    import org.glassfish.persistence.common.I18NHelper;
    import com.sun.jdo.spi.persistence.utility.StringHelper;
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
class JDOQLCodeGeneration extends TreeParser;

options
{
    importVocab = EJBQL;
    defaultErrorHandler = false;
    ASTLabelType = "EJBQLAST"; //NOI18N
}

{
    /** Type helper. */
    protected TypeSupport typeSupport;
    
    /** Parameter helper. */
    protected ParameterSupport paramSupport;
    
    /** I18N support. */
    protected final static ResourceBundle msgs = 
        I18NHelper.loadBundle(JDOQLCodeGeneration.class);
    
    /** The identification variable used for the candidate class. */
    private String candidateClassIdentificationVar;

    /** The name of the candidate class. */
    private String candidateClassName;

    /** The parameter declarations. */
    private StringBuffer parameterDecls;

    /** The variable declarations. */
    private StringBuffer variableDecls;

    /** The filter expression. */
    private StringBuffer filter;

    /** The ordering expression. */
    private StringBuffer ordering;

    /** The result expression. */
    private StringBuffer result;

    /** The result type. */
    private String resultType;

    /** Flag indicating whether the result element is a pc class. */
    private boolean isPCResult;

    /**
     *  Flag indicating whether the result element is associated to an
     *  aggregate function.
     */
    private boolean isAggregate = false;

    /** Counter for variables defined during codegen. */
    private int tmpVarCount = 0;

    /** 
     * Counter indicating how many parenthesis need to be closed 
     * at the end of the filter expr. 
     */
    private int parenCount = 0;
    
    /** Flag indicates whether the select clause has DISTINCT. */
    private boolean isDistinct = false;

    /**
     *
     */
    public void init(TypeSupport typeSupport, ParameterSupport paramSupport)
    {
        this.typeSupport = typeSupport;
        this.paramSupport = paramSupport;
    }

    /** */
    public void reportError(RecognitionException ex) {
        ErrorMsg.fatal(I18NHelper.getMessage(msgs, 
                "ERR_JDOQLCodeGenerationError"), ex); //NOI18N
    }

    /** */
    public void reportError(String s) {
        ErrorMsg.fatal(I18NHelper.getMessage(msgs, 
                "ERR_JDOQLCodeGenerationError") + s); //NOI18N
    }

    /** 
     * Returns the result of an EJBQL compile process. 
     * A JDOQLElements instances represents all the necessary information to 
     * create a JDOQL query instance that corresponds to the EJBQL query.
     * @return JDOQLElements instance representing the JDOQL query.
     */
    public JDOQLElements getJDOQLElements()
    {

        return new JDOQLElements(candidateClassName, parameterDecls.toString(),
            variableDecls.toString(), filter.toString(), ordering.toString(),
            result.toString(), resultType, isPCResult, isAggregate,
            paramSupport.getParameterEjbNames());
    }

    //========= Internal helper methods ==========

    /**
     * Extracts the name of the candidate class of the JDOQL query from the 
     * select- and from-clause of the EJBQL query.
     */
    private void handleCandidateClass(EJBQLAST query)
        throws RecognitionException
    {
        EJBQLAST from = (EJBQLAST)query.getFirstChild();
        EJBQLAST select = (EJBQLAST)from.getNextSibling();
        EJBQLAST var = null;
        var = extractIdentificationVariable(select);
        var = getIdentificationVarDecl(from, var.getText());
        candidateClassIdentificationVar = var.getText();
        candidateClassName = 
            typeSupport.getPCForTypeInfo(var.getTypeInfo());
    }

    /** 
     * Calculates the parameter declarations of the JDOQL query from the 
     * signature of the EJB finsder or selector method.
     */
    private void initParameterDeclarations()
    {
        parameterDecls = new StringBuffer();
        for (int i = 1; i <= paramSupport.getParameterCount(); i++) {
            String name = paramSupport.getParameterName(i);
            Object type = typeSupport.getTypeInfo(paramSupport.getParameterType(i));
            String ejbName = paramSupport.getParameterEjbName(i);
            String pcClassName = null;
            if (ejbName != null) {
                pcClassName = typeSupport.getPCForTypeInfo(ejbName);
            } else if (typeSupport.isLocalInterface(type) ||
                    typeSupport.isRemoteInterface(type)) {
                // This parameter corresponds to an EJB but the ejbName 
                // cannot be determined from query.
                // Since different EJBs may have the same interfaces,
                // the explicit pcClassName cannot be determined.
                pcClassName = "java.lang.Object";
            } else {
                pcClassName = typeSupport.getPCForTypeInfo(type);
            }

            parameterDecls.append(pcClassName);
            parameterDecls.append(" "); //NOI18N
            parameterDecls.append(name);
            parameterDecls.append(", "); //NOI18N
        }
    }

    /** 
     * EJBQL string literals escape a single quote using two single quotes. 
     * In JDOQL string literals single quotes need not to be escaped => 
     * replace '' by '.
     */
    private String convertStringLiteral(String ejbqlStringLiteral)
    {
        // first replace '' by '
        String ret = StringHelper.replace(ejbqlStringLiteral, "''", "'"); //NOI18N
        // Add a hack for a backslash at the end of the literal
        // Note, we might need to escape backslashes in the entire string 
        // literal, if the character following the backslash is an "escaped" 
        // char such as \n, \n, etc. Needs some further investigation.
        if (ret.endsWith("\\")) {
            ret = ret + "\\";
        }
        return ret;
    }
    
    /** */
    private EJBQLAST getIdentificationVarDecl(EJBQLAST from, String varName)
        throws RecognitionException
    {
        // iterate all identification var declarations
        for (EJBQLAST varDecl = (EJBQLAST)from.getFirstChild();
            varDecl != null;
            varDecl = (EJBQLAST)varDecl.getNextSibling()) {
            // domain of the current variable declaration
            EJBQLAST domain = (EJBQLAST)varDecl.getFirstChild();
            // identification variable node
            EJBQLAST varNode = (EJBQLAST)domain.getNextSibling();
            if (varNode.getText().equalsIgnoreCase(varName)) {
                // found the declaration node of the variable we are looking for
                if (domain.getType() == ABSTRACT_SCHEMA_NAME)
                    // the domain is a abstract schema type => found the var decl
                    return varNode;
                else
                    // domain is a collectionMemberDecl => use its var decl
                    return getIdentificationVarDecl(from, 
                        extractIdentificationVariable(domain).getText());
            }
        }
        return null;
    }

    /** 
     * Returns the name of a new variable of the JDOQL query.
     */
    private String getTmpVarName()
    {
        // TBD: The name must not conflict with a defined identification variable
        int no = tmpVarCount++;
        return "_jdoVar" + no; //NOI18N
    }
    
    /**
     * Returns the typeInfo of the element type of a collection valued CMR field.
     */
    private Object getElementTypeOfCollectionValuedCMR(EJBQLAST cmrFieldAccess)
    {
        EJBQLAST classExpr = (EJBQLAST)cmrFieldAccess.getFirstChild();
        EJBQLAST cmrField = (EJBQLAST)classExpr.getNextSibling();
        Object fieldInfo = typeSupport.getFieldInfo(
            classExpr.getTypeInfo(), cmrField.getText());
        return typeSupport.getElementType(fieldInfo);
    }
    
}

// rules

query
    :   #(  q:QUERY 
            {
                initParameterDeclarations();
                variableDecls = new StringBuffer();
                filter = new StringBuffer();
                ordering = new StringBuffer();
                result = new StringBuffer();
                handleCandidateClass(q);
            }
            fromClause 
            selectClause 
            whereClause
            orderbyClause
        )
    ;

extractIdentificationVariable returns [EJBQLAST var]
    :   #( SELECT ( DISTINCT )? var = extractIdentificationVariable )
    |   #( CMP_FIELD_ACCESS var = extractIdentificationVariable . )
    |   #( SINGLE_CMR_FIELD_ACCESS var = extractIdentificationVariable . )
    |   #( COLLECTION_CMR_FIELD_ACCESS var = extractIdentificationVariable . )
    |   #( OBJECT var = extractIdentificationVariable )
    |   #( AVG ( DISTINCT )? var = extractIdentificationVariable )
    |   #( MAX ( DISTINCT )? var = extractIdentificationVariable )
    |   #( MIN ( DISTINCT )? var = extractIdentificationVariable )
    |   #( SUM ( DISTINCT )? var = extractIdentificationVariable )
    |   #( COUNT ( DISTINCT )? var = extractIdentificationVariable )
    |   i:IDENTIFICATION_VAR { var = i; }
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
    :   #(  IN 
            {
                if (filter.length() > 0) {
                    // Please note, the FROM clause is processed prior to the 
                    // WHERE clause, so a filter of length == 0 means we are 
                    // processing the first IN clause of the FROM clause.
                    // We need to add an & operator and an open parenthesis for 
                    // all IN clauses but the first one. The parenthesis ensure 
                    // the resulting filter is portable, meaning the contains 
                    // clause must be the left expression of an AND-expression 
                    // where the variable is used in the right expression. 
                    filter.append(" & ("); //NOI18N
                    parenCount++;
                }
            }
            pathExpr[filter]
            v:IDENTIFICATION_VAR_DECL
            {
                // generate varibale declaration
                variableDecls.append(typeSupport.getPCForTypeInfo(
                        v.getTypeInfo()));
                variableDecls.append(' ');
                variableDecls.append(v.getText());
                variableDecls.append("; "); //NOI18N
                // now generate the contains clause
                filter.append(".contains("); //NOI18N
                filter.append(v.getText()); 
                filter.append(')');
            }
        )
    ;

rangeVarDecl
    :   #(  RANGE 
            ABSTRACT_SCHEMA_NAME 
            v:IDENTIFICATION_VAR_DECL
        )
        {
            // Do not generate variable decl for identification variable 
            // that represents the candidate class
            if (!v.getText().equalsIgnoreCase(candidateClassIdentificationVar)) {
                variableDecls.append(typeSupport.getPCForTypeInfo(
                        v.getTypeInfo()));
                variableDecls.append(' ');
                variableDecls.append(v.getText());
                variableDecls.append("; "); //NOI18N
            }
        }
    ;

// ----------------------------------
// rules: select clause
// ----------------------------------

selectClause
    :   #(  SELECT distinct[result] p:projection[result] )
        {
            isPCResult = typeSupport.isEjbName(p.getTypeInfo());
            resultType = isPCResult ? 
                typeSupport.getPCForTypeInfo(p.getTypeInfo()) :
                typeSupport.getTypeName(p.getTypeInfo());
        }
    ;

distinct[StringBuffer buf]
    :   // the code generation of this distinct is postponed until projection
        // checking as there is no need to generate distinct outside
        // aggregate functions
        DISTINCT { isDistinct = true; }
    |   // empty rule
    ;

aggregateDistinct[StringBuffer buf]
    :   DISTINCT { buf.append("distinct "); } //NOI18N
    |   // empty rule
    ;

projection[StringBuffer buf]
    :   #( AVG
            {
                isAggregate = true;
                buf.append("avg(");
            }
            aggregateDistinct[buf] pathExpr[buf]
            { buf.append(")"); }
        )
    |   #( MAX
            {
                isAggregate = true;
                buf.append("max(");
            }
            aggregateDistinct[buf] p:pathExpr[buf]
            { buf.append(")"); }
        )
    |   #( MIN
            {
                isAggregate = true;
                buf.append("min(");
            }
            aggregateDistinct[buf] pathExpr[buf]
            { buf.append(")"); }
        )
    |   #( SUM
            {
                isAggregate = true;
                buf.append("sum(");
            }
            aggregateDistinct[buf] pathExpr[buf]
            { buf.append(")"); }
        )
    |   #( COUNT
            {
                isAggregate = true;
                buf.append("count(");
            }
            aggregateDistinct[buf] pathExpr[buf]
            { buf.append(")"); }
        )
    |   {
            if (isDistinct) {
                buf.append("distinct "); //NOI18N
            }
        }
        pathExpr[buf]
    |   {
            if (isDistinct) {
                buf.append("distinct "); //NOI18N
            }
        }
        #( o:OBJECT pathExpr[buf] ) 
    ;

// ----------------------------------
// rules: where clause
// ----------------------------------

whereClause
    :   #(  WHERE 
            {
                if (filter.length() > 0) {
                    filter.append(" & ("); //NOI18N
                    // filter.length() > 0 means there are one or more contains 
                    // clauses generated from parsing the from clause => 
                    // enclose the where clause expression into parenthesis.
                    parenCount++;
                }
            }
            expression[filter]
            {
                while (parenCount > 0) {
                    filter.append(")"); //NOI18N
                    parenCount--;
                }
            }
        )
    ;

// ----------------------------------
// rules: orderby clause
// ----------------------------------

orderbyClause
    :   #(  ORDER
                (
                    {
                        if (ordering.length() > 0) {
                            ordering.append(", ");  
                        }
                    }
                    pathExpr[ordering]
                    ( 
                        ASC { ordering.append(" ascending"); } //NOI18N
                        |
                        DESC { ordering.append(" descending"); } //NOI18N
                    )
                )+
        )
    |
    ;

// ----------------------------------
// rules: expression
// ----------------------------------

expression[StringBuffer buf]
    :   conditionalExpr[buf]
    |   relationalExpr[buf]
    |   binaryArithmeticExpr[buf]
    |   unaryExpr[buf]
    |   betweenExpr[buf]
    |   likeExpr[buf]
    |   inExpr[buf]
    |   nullComparisonExpr[buf]
    |   emptyCollectionComparisonExpr[buf]
    |   collectionMemberExpr[buf]
    |   function[buf]
    |   primary[buf]
    ;

conditionalExpr[StringBuffer buf]
    :   #(  AND             { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" & "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    |   #(  OR              { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" | "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    ;

relationalExpr[StringBuffer buf]
    :   #(  EQUAL           { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" == "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    |   #(  NOT_EQUAL       { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" != "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    |   #(  LT              { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" < "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    |   #(  LE              { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" <= "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    |   #(  GT              { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" > "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    |   #(  GE              { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" >= "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    ;

binaryArithmeticExpr[StringBuffer buf]
    :   #(  PLUS            { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" + "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    |   #(  MINUS           { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" - "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    |   #(  STAR            { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" * "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    |   #(  DIV             { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" / "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    ;

unaryExpr[StringBuffer buf]
    :   #(  UNARY_PLUS
            expression[buf]
        )
    |   #(  UNARY_MINUS     { buf.append(" -("); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    |   #(  NOT             { buf.append(" !("); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N
        )
    ;

betweenExpr[StringBuffer buf]
{
    StringBuffer tmp = new StringBuffer();
}
    :   #(  BETWEEN
            expression[tmp]
            {   
                buf.append('('); 
                buf.append(tmp.toString()); 
                buf.append(" >= "); // NOI18N
            }
            expression[buf]
            { 
                buf.append(" & "); // NOI18N
                buf.append(tmp.toString());  
                buf.append(" <= "); // NOI18N
            }
            expression[buf]
            {   buf.append(')'); }
        )
    |   #(  NOT_BETWEEN
            expression[tmp]
            {   
                buf.append('('); 
                buf.append(tmp.toString()); 
                buf.append(" < "); // NOI18N
            }
            expression[buf]
            { 
                buf.append(" | "); // NOI18N
                buf.append(tmp.toString());  
                buf.append(" > "); // NOI18N
            }
            expression[buf]
            {   buf.append(')'); }
        )
    ;

likeExpr[StringBuffer buf]
    :   #(  LIKE 
            expression[buf]     { buf.append(".like("); } //NOI18N
            ( stringLiteral[buf] | parameter[buf] )
            escape[buf]         { buf.append(')'); }
        )
    |   #(  NOT_LIKE            { buf.append('!'); }
            expression[buf]     { buf.append(".like("); } //NOI18N
            ( stringLiteral[buf] | parameter[buf] )
            escape[buf]         { buf.append(')'); }
        )
    ;

escape[StringBuffer buf]
    :   #(  ESCAPE { buf.append (", "); } //NOI18N
            ( singleCharStringLiteral[buf] | parameter[buf] ) )
    |   // empty rule
    ;

singleCharStringLiteral[StringBuffer buf]
    :   s:STRING_LITERAL  
        {
            buf.append('\'');
            buf.append(convertStringLiteral(s.getText())); 
            buf.append('\'');
        }
    ;

inExpr[StringBuffer buf]
{
    StringBuffer expr = new StringBuffer();
    StringBuffer elementExpr = new StringBuffer();
}
    :   #(  IN 
            expression[expr]  
            { buf.append('('); } 
            primary[elementExpr]
            { 
                buf.append('(');
                buf.append(expr.toString());
                buf.append(" == "); //NOI18N
                buf.append(elementExpr.toString());
                buf.append(')');
            }
            (   
                {   
                    // create a new StringBuffer for the new elementExpr
                    elementExpr = new StringBuffer(); 
                }
                primary[elementExpr] 
                {
                    buf.append(" | "); //NOI18N
                    buf.append('(');
                    buf.append(expr.toString());
                    buf.append(" == "); //NOI18N
                    buf.append(elementExpr.toString());
                    buf.append(')');
                }
            )*
            {
                buf.append(')'); 
            }
        )
    |   #(  NOT_IN 
            expression[expr]
            { buf.append('('); } 
            primary[elementExpr]
            { 
                buf.append('(');
                buf.append(expr.toString());
                buf.append(" != "); //NOI18N
                buf.append(elementExpr.toString());
                buf.append(')');
            }
            (               
                {   
                    // create a new StringBuffer for the new elementExpr
                    elementExpr = new StringBuffer(); 
                }
                primary[elementExpr] 
                {
                    buf.append(" & "); //NOI18N
                    buf.append('(');
                    buf.append(expr.toString());
                    buf.append(" != "); //NOI18N
                    buf.append(elementExpr.toString());
                    buf.append(')');
                }
            )*
            {
                buf.append(')'); 
            }
        )
    ;

nullComparisonExpr[StringBuffer buf]
    :   #(  NULL 
            expression[buf] { buf.append(" == null"); } //NOI18N
        )
    |   #(  NOT_NULL 
            expression[buf] { buf.append(" != null"); } //NOI18N
        )
    ;

emptyCollectionComparisonExpr[StringBuffer buf]
    :   #(  EMPTY 
            expression[buf] { buf.append(".isEmpty()"); } //NOI18N
        )
    |   #(  NOT_EMPTY       { buf.append("!"); } //NOI18N
            expression[buf] { buf.append(".isEmpty()"); } //NOI18N
        )
    ;

collectionMemberExpr[StringBuffer buf]
{
    StringBuffer member = new StringBuffer();
    StringBuffer col = new StringBuffer();
}
    :   #(  MEMBER 
            expression[member]
            cmrAccess1:expression[col]
            { 
                String varName = getTmpVarName();
                // Use the element type as variable type. The value might be 
                // an input parameter of a local/remote interface which we 
                // cannot uniquely map to a PC class during deployment.
                Object varType = getElementTypeOfCollectionValuedCMR(cmrAccess1);
                // generate varibale declaration
                variableDecls.append(typeSupport.getPCForTypeInfo(varType));
                variableDecls.append(' ');
                variableDecls.append(varName);
                variableDecls.append("; "); //NOI18N
                buf.append("("); //NOI18N
                buf.append(col.toString());
                buf.append(".contains(");  //NOI18N
                buf.append(varName);
                buf.append(") & "); //NOI18N
                buf.append(varName);
                buf.append(" == "); //NOI18N
                buf.append(member.toString());
                buf.append(")"); //NOI18N
            }
        )
    |   #(  NOT_MEMBER 
            expression[member]
            cmrAccess2:expression[col]
            { 
                String varName = getTmpVarName();
                // Use the element type as variable type. The value might be 
                // an input parameter of a local/remote interface which we 
                // cannot uniquely map to a PC class during deployment.
                Object varType = getElementTypeOfCollectionValuedCMR(cmrAccess2);
                // generate varibale declaration
                variableDecls.append(typeSupport.getPCForTypeInfo(varType));
                variableDecls.append(' ');
                variableDecls.append(varName);
                variableDecls.append("; "); //NOI18N
                buf.append("("); //NOI18N
                buf.append(col.toString());
                buf.append(".isEmpty() | (!(");  //NOI18N
                buf.append(col.toString());
                buf.append(".contains(");  //NOI18N
                buf.append(varName);
                buf.append(") & "); //NOI18N
                buf.append(varName);
                buf.append(" == "); //NOI18N
                buf.append(member.toString());
                buf.append(")))"); //NOI18N
            }
        )
    ;

function[StringBuffer buf]
    :   concat[buf]
    |   substring[buf]
    |   length[buf]
    |   locate[buf]
    |   abs[buf]
    |   sqrt[buf]
    |   mod[buf]
    ;

concat[StringBuffer buf]
    :   #(  CONCAT          { buf.append("("); } //NOI18N
            expression[buf] { buf.append(" + "); } //NOI18N
            expression[buf] { buf.append(")"); } //NOI18N 
        )
    ;

substring[StringBuffer buf]
    :   // EJBQL: SUBSTRING(string, start, length) ->
        // JDOQL: string.substring(start-1, start+length-1)
        #(  SUBSTRING 
            expression[buf]   
            { buf.append(".substring("); } //NOI18N
            start:.
            length:.
        )
        {
            if ((start.getType() == INT_LITERAL) && 
                (length.getType() == INT_LITERAL)) {
                // Optimization: start and length are constant values =>
                // calulate beginIndex and endIndex of the JDOQL substring 
                // call at compile time.
                int startValue = Integer.parseInt(start.getText());
                int lengthValue = Integer.parseInt(length.getText());
                buf.append(startValue - 1);
                buf.append(", "); //NOI18N
                buf.append(startValue - 1 + lengthValue);
            }
            else {
                StringBuffer startBuf = new StringBuffer();
                expression(start, startBuf);
                buf.append(startBuf.toString()); 
                buf.append(" - 1, "); //NOI18N
                buf.append(startBuf.toString());
                buf.append(" - 1 + "); //NOI18N
                expression(length, buf);
            }
            buf.append(")"); //NOI18N
        }
    ;

length[StringBuffer buf]
    :   #(  LENGTH 
            expression[buf] { buf.append(".length()"); } //NOI18N
        )
    ;

locate[StringBuffer buf]
{
    StringBuffer pattern = new StringBuffer();
}
    :   // EJBQL: LOCATE(pattern, string) ->
        // JDOQL: (string.indexOf(pattern) + 1)
        // EJBQL: LOCATE(pattern, string, start) ->
        // JDOQL: (string.indexOf(pattern, start - 1) + 1)
        #(  LOCATE              { buf.append("("); } //NOI18N
            expression[pattern] 
            expression[buf]     { buf.append(".indexOf(");  //NOI18N
                                  buf.append(pattern.toString()); }
            locateStartPos[buf] { buf.append(") + 1)"); } //NOI18N
        )
    ;

locateStartPos[StringBuffer buf]
    :   start:.
        {
            buf.append(", ");  //NOI18N 
            if (start.getType() == INT_LITERAL) {
                // Optimization: start is a constant value =>
                // calulate startIndex of JDOQL indexOf call at compile time.
                buf.append(Integer.parseInt(start.getText()) - 1);
            }
            else {
                expression(start, buf);
                { buf.append(" - 1"); } //NOI18N
            }
        }
    |   // empty rule
    ;

abs[StringBuffer buf]
    :   #(  ABS  
            { buf.append ("java.lang.Math.abs("); } //NOI18N
            expression[buf]
            { buf.append (")"); } //NOI18N
        )
    ;

sqrt[StringBuffer buf]
    :   #(  SQRT 
            { buf.append ("java.lang.Math.sqrt("); } //NOI18N
            expression[buf]
            { buf.append (")"); } //NOI18N
        )
    ;

mod[StringBuffer buf]
    :   #(  MOD
            { buf.append("("); } //NOI18N
            expression[buf]
            { buf.append(" % "); } //NOI18N
            expression[buf]
            { buf.append(")"); } //NOI18N
        )
    ;   

primary[StringBuffer buf]
    :   literal[buf]
    |   pathExpr[buf]
    |   parameter[buf]
    ;

literal[StringBuffer buf]
    :   TRUE           
        { buf.append("true"); } // NOI18N
    |   FALSE          
        { buf.append("false"); } // NOI18N
    |   stringLiteral[buf]
    |   i:INT_LITERAL  
        { 
            buf.append(i.getText()); 
        }  
    |   l:LONG_LITERAL   
        { 
            buf.append(l.getText()); 
        }  
    |   f:FLOAT_LITERAL  
        { 
            buf.append(f.getText()); 
        }
    |   d:DOUBLE_LITERAL 
        { 
            buf.append(d.getText()); 
        }
    ;


stringLiteral[StringBuffer buf]
    :   s:STRING_LITERAL  
        {
            buf.append('"'); // NOI18N
            buf.append(convertStringLiteral(s.getText())); 
            buf.append('"'); // NOI18N
        }
    ;

pathExpr[StringBuffer buf]
    :   #(  CMP_FIELD_ACCESS
            pathExpr[buf]
            { buf.append('.'); }
            f1:field
            { buf.append(f1.getText()); }
        )
    |   #(  SINGLE_CMR_FIELD_ACCESS
            pathExpr[buf]
            { buf.append('.'); }
            f2:field
            { buf.append(f2.getText()); }
        )
    |   #(  COLLECTION_CMR_FIELD_ACCESS
            pathExpr[buf]
            { buf.append('.'); }
            f3:field
            { buf.append(f3.getText()); }
        )
    |   v:IDENTIFICATION_VAR
        {
            String name = v.getText();
            if (name.equalsIgnoreCase(candidateClassIdentificationVar))
               name = "this"; //NOI18N
            buf.append(name);
        }
    |   #( dot:DOT expression[buf]expression[buf])
        {
            ErrorMsg.fatal(I18NHelper.getMessage(msgs, "ERR_UnexpectedNode", //NOI18N
                    dot.getText(), String.valueOf(dot.getType())));
        }
    |   i:IDENT
        {
            ErrorMsg.fatal(I18NHelper.getMessage(msgs, "ERR_UnexpectedNode", //NOI18N
                    i.getText(), String.valueOf(i.getType())));
        }
    ;

field
    :   CMP_FIELD
    |   COLLECTION_CMR_FIELD
    |   SINGLE_CMR_FIELD
    ;

parameter [StringBuffer buf]
    :   param:INPUT_PARAMETER
        {
            buf.append(paramSupport.getParameterName(param.getText()));
        }
    ;
