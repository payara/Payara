/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.elasticity.expression;

import java.util.ArrayList;


/**
 * Lexer
 * 
 * @author mk
 *
 */
public class ExpressionLexer {

	private CharSequence stream;
	
	private int index = 0;
	
	private int size = 0;

	public static final Token EOSTREAM = new TokenImpl(TokenType.EOSTREAM, "__EOSTREAM__");
	public static final Token MULT = new TokenImpl(TokenType.MULT, "*");
	public static final Token DIV = new TokenImpl(TokenType.DIV, "/");
	public static final Token PLUS = new TokenImpl(TokenType.PLUS, "+");
	public static final Token MINUS = new TokenImpl(TokenType.MINUS, "-");
	public static final Token OPAR = new TokenImpl(TokenType.OPAR, "(");
	public static final Token CPAR = new TokenImpl(TokenType.CPAR, ")");
	public static final Token OBRACE = new TokenImpl(TokenType.OBRACE, "{");
	public static final Token CBRACE = new TokenImpl(TokenType.CBRACE, "}");
	public static final Token OARRAY = new TokenImpl(TokenType.OARRAY, "[");
	public static final Token CARRAY = new TokenImpl(TokenType.CARRAY, "]");
	public static final Token DOT = new TokenImpl(TokenType.DOT, ".");
	public static final Token COMA = new TokenImpl(TokenType.COMA, ",");

	public static final Token LAND = new TokenImpl(TokenType.LOGICAL_AND, "&&");
	public static final Token LOR = new TokenImpl(TokenType.LOGICAL_OR, "||");
	public static final Token AND = new TokenImpl(TokenType.BIT_AND, "&");
	public static final Token OR = new TokenImpl(TokenType.BIT_OR, "|");

	public static final Token TRUE = new TokenImpl(TokenType.TRUE, "true");
	public static final Token FALSE = new TokenImpl(TokenType.FALSE, "false");
	
	public static final Token LT = new TokenImpl(TokenType.LT, "<");
	public static final Token LTE = new TokenImpl(TokenType.LTE, "<=");
	public static final Token GT = new TokenImpl(TokenType.GT, ">");
	public static final Token GTE = new TokenImpl(TokenType.GTE, ">=");
	public static final Token EQ = new TokenImpl(TokenType.EQ, "=");
	public static final Token EQEQ = new TokenImpl(TokenType.EQEQ, "==");

	private int tokenIndex = 0;
	
	private int tokenMark = 0;
	
	private ArrayList<Token> tokens = new ArrayList<Token>();
	
	public ExpressionLexer(CharSequence stream) {
		this.stream = stream;
		this.size = stream.length();
		
		for (Token tok = getNextToken(); tok != EOSTREAM; tok = getNextToken()) {
//			System.out.println("Got token: " + tok);
			tokens.add(tok);
		}
		
		tokens.add(EOSTREAM);
	}

	
	public Token peek() {
		return tokens.get(tokenIndex);
	}
	public Token next() {
		return tokenIndex < tokens.size() ? tokens.get(tokenIndex++) : EOSTREAM;
	}

	public void mark() {
		this.tokenMark = tokenIndex;
	}
	
	public void reset() {
		this.tokenIndex = tokenMark >= 0 ? tokenMark : tokenIndex;
//		System.out.println("index reset to : " + tokenMark + "; tokens.size: " + tokens.size());
	}
	
	private Token getNextToken() {
		
		Token tok = EOSTREAM;
		
		if (index >= size) {
			tok = EOSTREAM;
		} else if (Character.isJavaIdentifierStart(stream.charAt(index))) {
			int p = index;
			while ((index < size) && (Character.isJavaIdentifierPart(stream.charAt(index)))) {
				index++;
			}
			
			String value = stream.subSequence(p,  index).toString();
			TokenType tokId = "true".equals(value) 
					? TokenType.TRUE
					: ("false".equals(value) ? TokenType.FALSE : TokenType.IDENTIFIER);
			tok = new TokenImpl(tokId, value);
		} else if (Character.isDigit(stream.charAt(index))) {
			int startIndex = index;
			boolean isDouble = false;
			while ((index < size) && Character.isDigit(stream.charAt(index))) {
				index++;
			}	

			if (index < size && stream.charAt(index) == '.') {
				isDouble = true;
				index++; //for the dot
				while ((index < size) && Character.isDigit(stream.charAt(index))) {
					index++;
				}	
			}
			
			tok = new TokenImpl(
					isDouble ? TokenType.DOUBLE : TokenType.INTEGER,
							stream.subSequence(startIndex,  index).toString());
		} else if (Character.isWhitespace(stream.charAt(index))) {
			for (int p = index; index < size; index++) {
				if (! Character.isWhitespace(stream.charAt(index))) {
					tok = new TokenImpl(TokenType.WHITESPACE, stream.subSequence(p,  index).toString());
					break;
				}
			}
			
			//return tok OR call nextToken() to eat whitespace
			return getNextToken();
		} else {
			switch (stream.charAt(index)) {
			case '*' : 
				tok = MULT;
				index++;
				break;
			case '/' :  
				tok = DIV;
				index++;
				break;
			case '+' : 
				tok = PLUS;
				index++;
				break;
			case '-' : 
				tok = MINUS;
				index++;
				break;
			case '(' : 
				tok = OPAR;
				index++;
				break;
			case ')' : 
				tok = CPAR;
				index++;
				break;
			case '{' : 
				tok = OBRACE;
				index++;
				break;
			case '}' : 
				tok = CBRACE;
				index++;
				break;
			case '[' : 
				tok = OARRAY;
				index++;
				break;
			case ']' : 
				tok = CARRAY;
				index++;
				break;
			case '.' : 
				tok = DOT;
				index++;
				break;
			case ',' : 
				tok = COMA;
				index++;
				break;
			case '>' :
				tok = GT;
				index++;
				if (index < size && stream.charAt(index) == '=') {
					index++;
					tok = GTE;
				}
				break;
			case '<' :
				tok = LT;
				index++;
				if (index < size && stream.charAt(index) == '=') {
					index++;
					tok = LTE;
				}
				break;
			case '=' :
				tok = EQ;
				index++;
				if (index < size && stream.charAt(index) == '=') {
					index++;
					tok = EQEQ;
				}
				break;
			case '&' :
				tok = AND;
				index++;
				if (index < size && stream.charAt(index) == '&') {
					index++;
					tok = LAND;
				}
				break;
			case '|' :
				tok = OR;
				index++;
				if (index < size && stream.charAt(index) == '|') {
					index++;
					tok = LOR;
				}
				break;
			default: tok = new TokenImpl(TokenType.UNKNOWN, "" + stream.charAt(index++));
			}
		}
		
		return tok;
	}
	
}
