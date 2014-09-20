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
 * JQL.g
 *
 * Created on March 8, 2000
 */

header
{
    package com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc;

    import antlr.MismatchedTokenException;
    import antlr.MismatchedCharException;
    import antlr.NoViableAltException;
    import antlr.NoViableAltForCharException;
    import antlr.TokenStreamRecognitionException;
    
    import java.util.Locale;
    import java.util.ResourceBundle;
    import com.sun.jdo.api.persistence.support.JDOQueryException;
    import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
    import org.glassfish.persistence.common.I18NHelper;

}

//===== Lexical Analyzer Class Definitions =====

/**
 * This class defines the lexical analysis for the JQL compiler.
 *
 * @author  Michael Bouschen
 * @author  Shing Wai Chan
 * @version 0.1
 */
class JQLLexer extends Lexer;
options
{
    k = 2;
    exportVocab = JQL;
    charVocabulary = '\u0000'..'\uFFFE'; //NOI18N
}

tokens {

    IMPORT = "import"; //NOI18N
    THIS = "this"; //NOI18N
    ASCENDING = "ascending"; //NOI18N
    DESCENDING = "descending"; //NOI18N

    // non-standard extensions
    DISTINCT = "distinct"; //NOI18N
    
    // types
    
    BOOLEAN = "boolean"; //NOI18N
    BYTE = "byte"; //NOI18N
    CHAR = "char"; //NOI18N
    SHORT = "short"; //NOI18N
    INT = "int"; //NOI18N
    FLOAT = "float"; //NOI18N
    LONG = "long"; //NOI18N
    DOUBLE = "double"; //NOI18N
    
    // literals
    
    NULL = "null"; //NOI18N
    TRUE = "true"; //NOI18N
    FALSE = "false"; //NOI18N

    // aggregate functions
    AVG = "avg"; //NOI18N
    MAX = "max"; //NOI18N
    MIN = "min"; //NOI18N
    SUM = "sum"; //NOI18N
    COUNT = "count"; //NOI18N
    
}

{
    /**
     * I18N support
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            JQLLexer.class);
    
    /**
     *
     */
    protected ErrorMsg errorMsg;
    
    /**
     * The width of a tab stop.
     * This value is used to calculate the correct column in a line
     * conatining a tab character.
     */
    protected static final int TABSIZE = 4;

    /**
     *
     */
    public void init(ErrorMsg errorMsg)
    {
        this.errorMsg = errorMsg;
    }
    
    /**
     *
     */
    public void tab() 
    {
        int column = getColumn();
        int newColumn = (((column-1)/TABSIZE)+1)*TABSIZE+1;
        setColumn(newColumn);
    }

    /**
     *
     */
    public void reportError(int line, int column, String s)
    {
        errorMsg.error(line, column, s);
    }

    /**
     * Report lexer exception errors caught in nextToken()
     */
    public void reportError(RecognitionException e)
    {
        JQLParser.handleANTLRException(e, errorMsg);
    }

    /**
     * Lexer error-reporting function
     */
    public void reportError(String s)
    {
        errorMsg.error(0, 0, s);
    }

    /**
     * Lexer warning-reporting function
     */
    public void reportWarning(String s)
    {
        throw new JDOQueryException(s);
    }
}

// OPERATORS
LPAREN          :   '('     ;
RPAREN          :   ')'     ;
COMMA           :   ','     ;
//DOT           :   '.'     ;
EQUAL           :   "=="    ; //NOI18N
LNOT            :   '!'     ;
BNOT            :   '~'     ;
NOT_EQUAL       :   "!="    ; //NOI18N
DIV             :   '/'     ;
PLUS            :   '+'     ;
MINUS           :   '-'     ;
STAR            :   '*'     ;
MOD             :   '%'     ;
GE              :   ">="    ; //NOI18N
GT              :   ">"     ; //NOI18N
LE              :   "<="    ; //NOI18N
LT              :   '<'     ;
BXOR            :   '^'     ;
BOR             :   '|'     ;
OR              :   "||"    ; //NOI18N
BAND            :   '&'     ;
AND             :   "&&"    ; //NOI18N
SEMI            :   ';'     ;

// Whitespace -- ignored
WS
    :   (   ' '
        |   '\t'
        |   '\f'
        )
        { _ttype = Token.SKIP; }
    ;

NEWLINE
    :   (   "\r\n"  //NOI18N
        |   '\r'
        |   '\n'
        )
        { 
            newline(); 
            _ttype = Token.SKIP; 
        }
    ;

// character literals
CHAR_LITERAL
    :   '\'' ( ESC | ~'\'' ) '\'' 
    ;

// string literals
STRING_LITERAL
    :  '"' ( ESC | ~'"')* '"' //NOI18N
    ;

// escape sequence -- note that this is protected; it can only be called
//   from another lexer rule -- it will not ever directly return a token to
//   the parser
// There are various ambiguities hushed in this rule.  The optional
// '0'...'9' digit matches should be matched here rather than letting
// them go back to STRING_LITERAL to be matched.  ANTLR does the
// right thing by matching immediately; hence, it's ok to shut off
// the FOLLOW ambig warnings.
protected
ESC
    :   '\\'
        (   options { warnWhenFollowAmbig = false; }    
        :   'n'
        |   'r'
        |   't'
        |   'b'
        |   'f'
        |   '"' //NOI18N
        |   '\''
        |   '\\'
        |   ('u')+ HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT 
        |   ('0'..'3')
            (
                options {
                    warnWhenFollowAmbig = false;
                }
            :   ('0'..'7')
                (   
                    options {
                        warnWhenFollowAmbig = false;
                    }
                :   '0'..'7'
                )?
            )?
        |   ('4'..'7')
            (
                options {
                    warnWhenFollowAmbig = false;
                }
            :   ('0'..'9')
            )?
        )?
    ;

// hexadecimal digit (again, note it's protected!)
protected
HEX_DIGIT
    :   ('0'..'9'|'A'..'F'|'a'..'f')
    ;


// a numeric literal
INT_LITERAL
    {   
        boolean isDecimal=false;
        int tokenType = DOUBLE_LITERAL; 
    }
    :   '.' {_ttype = DOT;}
            (('0'..'9')+ {tokenType = DOUBLE_LITERAL;}
             (EXPONENT)? 
             (tokenType = FLOATINGPOINT_SUFFIX)?
            { _ttype = tokenType; })?
    |   (   '0' {isDecimal = true;} // special case for just '0'
            (   ('x'|'X')
                (                                           // hex
                    // the 'e'|'E' and float suffix stuff look
                    // like hex digits, hence the (...)+ doesn't
                    // know when to stop: ambig.  ANTLR resolves
                    // it correctly by matching immediately.  It
                    // is therefor ok to hush warning.
                    options {
                        warnWhenFollowAmbig=false;
                    }
                :   HEX_DIGIT
                )+
            |   ('0'..'7')+                                 // octal
            )?
        |   ('1'..'9') ('0'..'9')*  {isDecimal=true;}       // non-zero decimal
        )
        (   ('l'|'L') { _ttype = LONG_LITERAL; }
        
        // only check to see if it's a float if looks like decimal so far
        |   {isDecimal}?
            {tokenType = DOUBLE_LITERAL;} 
            (   '.' ('0'..'9')* (EXPONENT)? 
                (tokenType = FLOATINGPOINT_SUFFIX)?
            |   EXPONENT (tokenType = FLOATINGPOINT_SUFFIX)?
            |   tokenType = FLOATINGPOINT_SUFFIX
            )
            { _ttype = tokenType; }
        )?
    ;

// a couple protected methods to assist in matching floating point numbers
protected
EXPONENT
    :   ('e'|'E') ('+'|'-')? ('0'..'9')+
    ;

protected
FLOATINGPOINT_SUFFIX returns [int tokenType]
    : 'f' { tokenType = FLOAT_LITERAL; } 
    | 'F' { tokenType = FLOAT_LITERAL; } 
    | 'd' { tokenType = DOUBLE_LITERAL; } 
    | 'D' { tokenType = DOUBLE_LITERAL; } 
    ;

// an identifier.  Note that testLiterals is set to true!  This means
// that after we match the rule, we look in the literals table to see
// if it's a literal or really an identifer

IDENT
    options {paraphrase = "an identifier"; testLiterals=true;} //NOI18N
    :   (   'a'..'z'
        |   'A'..'Z'
        |   '_'
        |   '$'
        |   UNICODE_ESCAPE
        |   c1:'\u0080'..'\uFFFE'
            {   
                if (!Character.isJavaIdentifierStart(c1)) {
                    errorMsg.error(getLine(), getColumn(), 
                        I18NHelper.getMessage(messages, "jqlc.parser.unexpectedchar", //NOI18N
                            String.valueOf(c1)));
                }
            }
        ) 
        (   'a'..'z'
        |   'A'..'Z'
        |   '_'
        |   '$'
        |   '0'..'9'
        |   UNICODE_ESCAPE
        |   c2:'\u0080'..'\uFFFE'
            {   
                if (!Character.isJavaIdentifierPart(c2)) {
                    errorMsg.error(getLine(), getColumn(), 
                        I18NHelper.getMessage(messages, "jqlc.parser.unexpectedchar", //NOI18N
                        String.valueOf(c2)));
                }
            }
        )*
    ;

protected
UNICODE_ESCAPE
    : '\\' ('u')+ HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
        {
            try {
                String tmp = text.toString();
                char c  = (char)Integer.parseInt(tmp.substring(tmp.length() - 4, tmp.length()), 16);
                // problems using ANTLR feature $setText => use generated code
                text.setLength(_begin); 
                text.append(new Character(c).toString());
            }
            catch (NumberFormatException ex) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages, "jqlc.parser.invalidunicodestr"), ex); //NOI18N
            }
        }
    ;

//===== Parser Class Definitions =====

/**
 * This class defines the syntax analysis (parser) of the JQL compiler.
 *
 * @author  Michael Bouschen
 * @version 0.1
 */
class JQLParser extends Parser;

options {
    k = 2;                   // two token lookahead
    exportVocab = JQL;
    buildAST = true;
    ASTLabelType = "JQLAST"; // AST variables are defined as JQLAST
}

tokens
{
    // "imaginary" tokens, that have no corresponding real input

    QUERY;
    CLASS_DEF;
    IMPORT_DEF;
    PARAMETER_DEF;
    VARIABLE_DEF;
    ORDERING_DEF;
    FILTER_DEF;
    ARG_LIST;

    // operators
    UNARY_MINUS;
    UNARY_PLUS;
    TYPECAST;
    OBJECT_EQUAL;
    OBJECT_NOT_EQUAL;
    COLLECTION_EQUAL;
    COLLECTION_NOT_EQUAL;
    CONCAT;

    // special dot expressions
    FIELD_ACCESS;
    STATIC_FIELD_ACCESS;
    CONTAINS;
    NOT_CONTAINS;
    NAVIGATION;
    STARTS_WITH;
    ENDS_WITH;
    IS_EMPTY;
    
    // identifier types
    VARIABLE;
    PARAMETER;
    TYPENAME;

    // constant value
    VALUE;

    // result definition
    RESULT_DEF;

    // non-standard extensions (operators)
    LIKE;
    SUBSTRING;
    INDEXOF;
    LENGTH;
    ABS;
    SQRT;

    // 
    NOT_IN;
}

{
    /**
     * I18N support
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            JQLParser.class);

    /** */
    protected static final int EOF_CHAR = 65535; // = (char) -1 = EOF

    /**
     *
     */
    protected ErrorMsg errorMsg;
    
    /**
     *
     */
    public void init(ErrorMsg errorMsg)
    {
        this.errorMsg = errorMsg;
    }
    
    /**
     * ANTLR method called when an error was detected.
     */
    public void reportError(RecognitionException ex)
    {
        JQLParser.handleANTLRException(ex, errorMsg);
    }

    /**
     * ANTLR method called when an error was detected.
     */
    public void reportError(String s)
    {
        errorMsg.error(0, 0, s);
    }

    /**
     *
     */
    public void reportError(int line, int column, String s)
    {
        errorMsg.error(line, column, s);
    }

    /**
     * ANTLR method called when a warning was detected.
     */
    public void reportWarning(String s)
    {
        throw new JDOQueryException(s);
    }

    /**
     *
     */
    public static void handleANTLRException(ANTLRException ex, ErrorMsg errorMsg)
    {
        if (ex instanceof MismatchedCharException)
        {
            MismatchedCharException mismatched = (MismatchedCharException)ex;
            if (mismatched.mismatchType == MismatchedCharException.CHAR)
            {
                if (mismatched.foundChar == EOF_CHAR) 
                {
                    errorMsg.error(mismatched.getLine(), mismatched.getColumn(), 
                        I18NHelper.getMessage(messages, "jqlc.parser.unexpectedEOF")); //NOI18N
                }
                else 
                {
                    errorMsg.error(mismatched.getLine(), mismatched.getColumn(), 
                        I18NHelper.getMessage(messages, "jqlc.parser.expectedfoundchar", //NOI18N
                            String.valueOf((char)mismatched.expecting), 
                            String.valueOf((char)mismatched.foundChar)));
                }
                return;
            }
        }
        else if (ex instanceof MismatchedTokenException)
        {
            MismatchedTokenException mismatched = (MismatchedTokenException)ex;
            Token token = mismatched.token;
            if ((mismatched.mismatchType == MismatchedTokenException.TOKEN) &&
                (token != null)) 
            {
                if (token.getType() == Token.EOF_TYPE) {
                    errorMsg.error(token.getLine(), token.getColumn(), 
                        I18NHelper.getMessage(messages, "jqlc.parser.unexpectedEOF")); //NOI18N
                }
                else {
                    errorMsg.error(token.getLine(), token.getColumn(), 
                        I18NHelper.getMessage(messages, "jqlc.parser.syntaxerrorattoken", token.getText())); //NOI18N
                }
                return;
            }
        }
        else if (ex instanceof NoViableAltException)
        {
            Token token = ((NoViableAltException)ex).token;
            if (token != null)
            {
                if (token.getType() == Token.EOF_TYPE) 
                {
                    errorMsg.error(token.getLine(), token.getColumn(),
                        I18NHelper.getMessage(messages, "jqlc.parser.unexpectedEOF")); //NOI18N
                }
                else 
                {
                    errorMsg.error(token.getLine(), token.getColumn(), 
                        I18NHelper.getMessage(messages, "jqlc.parser.unexpectedtoken", token.getText())); //NOI18N
                }
                return;
            }
        }
        else if (ex instanceof NoViableAltForCharException)
        {
            NoViableAltForCharException noViableAlt = (NoViableAltForCharException)ex;
            errorMsg.error(noViableAlt.getLine(), noViableAlt.getColumn(), 
                I18NHelper.getMessage(messages, "jqlc.parser.unexpectedchar", //NOI18N
                    String.valueOf(noViableAlt.foundChar)));
        }
        else if (ex instanceof TokenStreamRecognitionException)
        {
            handleANTLRException(((TokenStreamRecognitionException)ex).recog, errorMsg);
        }

        // no special handling from aboves matches the exception if this line is reached =>
        // make it a syntax error
        int line = 0;
        int column = 0;
        if (ex instanceof RecognitionException)
        {
            line = ((RecognitionException)ex).getLine();
            column = ((RecognitionException)ex).getColumn();
        }
        errorMsg.error(line, column, I18NHelper.getMessage(messages, "jqlc.parser.syntaxerror")); //NOI18N
    }
}

// ----------------------------------
// rules: import declaration
// ----------------------------------

parseImports
{   
    errorMsg.setContext("declareImports");  //NOI18N
}
    :   ( declareImport ( SEMI! declareImport )* )? ( SEMI! )? EOF!
    ;

declareImport
    :   i:IMPORT^ qualifiedName //NOI18N
        {
            #i.setType(IMPORT_DEF);
        }
    ;

// ----------------------------------
// rules: parameter declaration
// ----------------------------------

parseParameters
{   
    errorMsg.setContext("declareParameters"); //NOI18N
}
    :   ( declareParameter ( COMMA! declareParameter )* )? ( COMMA! )? EOF!
    ;

declareParameter
    :   type IDENT
        { #declareParameter = #(#[PARAMETER_DEF,"parameterDef"], #declareParameter); } //NOI18N
    ;

// ----------------------------------
// rules: variables declaration
// ----------------------------------

parseVariables
{   
    errorMsg.setContext("declareVariables");  //NOI18N
}
    :   ( declareVariable ( SEMI! declareVariable )* )? ( SEMI! )? EOF!
    ;

declareVariable
    :   type IDENT
        {  #declareVariable = #(#[VARIABLE_DEF,"variableDef"], #declareVariable); } //NOI18N
    ;

// ----------------------------------
// rules ordering specification
// ----------------------------------

parseOrdering
{   
    errorMsg.setContext("setOrdering");  //NOI18N
}
    :   ( orderSpec ( COMMA! orderSpec )* )? ( COMMA! )? EOF!
    ;

orderSpec!
    :   e:expression d:direction
        { #orderSpec = #(#[ORDERING_DEF,"orderingDef"], #d, #e); } //NOI18N
    ; 

direction
    :    ASCENDING
    |    DESCENDING
    ;

// ----------------------------------
// rules result expression
// ----------------------------------

parseResult
{  
    errorMsg.setContext("setResult");  //NOI18N
}
    :   ( ( DISTINCT^ )? ( a:aggregateExpr | e:expression ) )? EOF!
        {  
            // create RESULT_DEF node if there was a projection
            if (#a != null) {
                // skip a possible first distinct in case of an aggregate expr
                #parseResult = #(#[RESULT_DEF, "resultDef"], #a);
            }
            else if (#e != null) {
                #parseResult = #(#[RESULT_DEF,"resultDef"], #parseResult); //NOI18N
            }
        }
    ;

aggregateExpr
    :   ( AVG^ | MAX^ | MIN^ | SUM^ | COUNT^) LPAREN! distinctExpr RPAREN!
    ;

distinctExpr
    :   DISTINCT^ e:expression 
    |   expression
    ;

// ----------------------------------
// rules filer expression
// ----------------------------------

parseFilter!
{  
    errorMsg.setContext("setFilter");  //NOI18N
}
    :   e:expression EOF!
        {  #parseFilter = #(#[FILTER_DEF,"filterDef"], #e); } //NOI18N
    ;

// This is a list of expressions.
expressionList
    :   expression (COMMA! expression)*
    ;

expression
    :   conditionalOrExpression
    ;

// conditional or ||
conditionalOrExpression
    :   conditionalAndExpression (OR^ conditionalAndExpression)*
    ;

// conditional and &&
conditionalAndExpression
    :   inclusiveOrExpression (AND^ inclusiveOrExpression)*
    ;

// bitwise or logical or |
inclusiveOrExpression
    :   exclusiveOrExpression (BOR^ exclusiveOrExpression)*
    ;

// exclusive or ^
exclusiveOrExpression
    :   andExpression (BXOR^ andExpression)*
    ;

// bitwise or logical and &
andExpression
    :   equalityExpression (BAND^ equalityExpression)*
    ;

// equality/inequality ==/!=
equalityExpression
    :   relationalExpression ((NOT_EQUAL^ | EQUAL^) relationalExpression)*
    ;
// boolean relational expressions
relationalExpression
    :   additiveExpression
        (   (   LT^
            |   GT^
            |   LE^
            |   GE^
            )
            additiveExpression
        )*
    ;

// binary addition/subtraction
additiveExpression
    :   multiplicativeExpression ((PLUS^ | MINUS^) multiplicativeExpression)*
    ;
// multiplication/division/modulo
multiplicativeExpression
    :   unaryExpression ((STAR^ | DIV^ | MOD^ ) unaryExpression)*
    ;

unaryExpression
    :   MINUS^ {#MINUS.setType(UNARY_MINUS);} unaryExpression
    |   PLUS^  {#PLUS.setType(UNARY_PLUS);} unaryExpression
    |   unaryExpressionNotPlusMinus
    ;

unaryExpressionNotPlusMinus
    :   BNOT^ unaryExpression
    |   LNOT^ unaryExpression
    |   ( LPAREN type RPAREN unaryExpression )=>
          lp:LPAREN^ {#lp.setType(TYPECAST);} type RPAREN! unaryExpression
    |   postfixExpression
    ;

// qualified names, field access, method invocation
postfixExpression
    :   primary
        (   DOT^ IDENT ( argList )? )*
    ;

argList
    :   LPAREN!
        (   expressionList
            {#argList = #(#[ARG_LIST,"ARG_LIST"], #argList); } //NOI18N

        |   /* empty list */
            {#argList = #[ARG_LIST,"ARG_LIST"];} //NOI18N
        )
        RPAREN!
    ;

// the basic element of an expression
primary
    :   IDENT
    |   literal
    |   THIS
    |   LPAREN! expression RPAREN!
    ;

literal
    :   TRUE
    |   FALSE
    |   INT_LITERAL
    |   LONG_LITERAL
    |   FLOAT_LITERAL
    |   DOUBLE_LITERAL
    |   c:CHAR_LITERAL
        {
            // strip quotes from the token text
            String text = #c.getText();
            #c.setText(text.substring(1,text.length()-1));
        }
    |   s:STRING_LITERAL
        {
            // strip quotes from the token text
            String text = #s.getText();
            #s.setText(text.substring(1,text.length()-1));
        }
    |   NULL
    ;

qualifiedName
    :   IDENT ( DOT^ IDENT )*
    ;

type
    :   qualifiedName
    |   primitiveType
    ;

// The primitive types.
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

