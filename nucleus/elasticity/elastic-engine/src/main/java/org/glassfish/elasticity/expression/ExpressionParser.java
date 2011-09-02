package org.glassfish.elasticity.expression;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * booleanExpr: logicalExpr ([ && | ||] logicalExpr)*
 *     ;
 *     
 * logicalExpr: simpleExpr ([ > | < | >= | <= | ==] simpleExpr)* 
 * 			  ;
 * 
 * simpleExpr: factor ([+ | -] factor)*
 * 			 ;
 * 
 * factor: term ([* | /] term)*
 * 		 ;
 * 
 * term: TRUE
 * 		| FALSE 
 * 		| NUMBER 
 * 		| functionCall 
 * 		| '(' simpleExpr ')' 
 * 		| attributeAccess
 * 		;
 * 
 * functionCall: functionName ('(' | '[') simpleExpr (')' | ']') ;
 * 
 * attributeAccess: IDENTIFIER '.' IDENTIFIER ;
 * 
 * {lexer tokens}
 * 
 * IDENTIFIER, NUMBER, TRUE, FALSE
 * 
 * @author Mahesh Kannan
 * 
 */
public class ExpressionParser {

	private ExpressionLexer lexer;

	public ExpressionParser(CharSequence seq) {
		lexer = new ExpressionLexer(seq);
	}

	public void parse() {
		System.out.println("ExpressionTree: " + booleanExpr());
	}

	private ExpressionNode booleanExpr() {
		ExpressionNode root = logicalExpr();
		boolean done = false;
		do {
			Token tok = lexer.peek();
			switch (tok.getTokenType()) {
			case LOGICAL_AND:
			case LOGICAL_OR:
				match(tok.getTokenType());
				root = new ExpressionNode(tok, root, logicalExpr());
				break;
			default:
				done = true;
			}
		} while (!done);
		
		return root;
	}

	private ExpressionNode logicalExpr() {
		ExpressionNode root = expr();
		boolean done = false;
		do {
			Token tok = lexer.peek();
			switch (tok.getTokenType()) {
			case GT:
			case GTE:
			case LT:
			case LTE:
				match(tok.getTokenType());
				root = new ExpressionNode(tok, root, expr());
				break;
			default:
				done = true;
			}
		} while (!done);
		
		return root;
	}

	private ExpressionNode expr() {
		ExpressionNode root = factor();
		boolean done = false;
		do {
			Token tok = lexer.peek();
			switch (tok.getTokenType()) {
			case PLUS:
			case MINUS:
				match(tok.getTokenType());
				root = new ExpressionNode(tok, root, factor());
				break;
			default:
				done = true;
			}
		} while (!done);
		
		return root;
	}

	private ExpressionNode factor() {
		ExpressionNode root = term();
		boolean done = false;
		do {
			Token tok = lexer.peek();
			switch (tok.getTokenType()) {
			case MULT:
			case DIV:
				match(tok.getTokenType());
				root = new ExpressionNode(tok, root, term());
				break;
			default:
				done = true;
			}
		} while (!done);
		
		return root;
	}

	private ExpressionNode term() {
		ExpressionNode root = null;
		Token tok = lexer.peek();
		switch (tok.getTokenType()) {
		case TRUE:
		case FALSE:
		case DOUBLE:
		case INTEGER:
			root = new ExpressionNode(lexer.next(), null, null);
			break;
		case IDENTIFIER:
			Token metricNameToken = match(TokenType.IDENTIFIER);
			if (peek(TokenType.DOT)) {
				match(TokenType.DOT);
				Token attributeNameToken = match(TokenType.IDENTIFIER);
				System.out.println("Matched attribute access: "
						+ metricNameToken.value() + "."
						+ attributeNameToken.value());
				root = new ExpressionNode(new TokenImpl(TokenType.ATTR_ACCESS, "."),
						new ExpressionNode(metricNameToken, null, null), 
						new ExpressionNode(attributeNameToken, null, null));
			} else if (peek(TokenType.OPAR)) {
				Token funcCall = match(TokenType.OPAR);
				root = new FunctionCall(funcCall, metricNameToken);
				functionCallParams((FunctionCall) root);
				System.out.println("Matched function call: "
						+ metricNameToken.value());
				match(TokenType.CPAR);
			} else if (peek(TokenType.OARRAY)) {
				Token funcCall = match(TokenType.OARRAY);
				root = new FunctionCall(funcCall, metricNameToken);
				functionCallParams((FunctionCall) root);
				System.out.println("Matched function remote call access: "
						+ metricNameToken.value());
				match(TokenType.CARRAY);
			}
			break;
		default:
			throw new IllegalStateException("Unexpected token: " + tok);
		}
		
		return root;
	}

	private void functionCallParams(FunctionCall funcCall) {
		boolean done = false;
		do {
			funcCall.addParam(booleanExpr());
			done = peek(TokenType.CPAR);
			if (!done) {
				done = peek(TokenType.CARRAY);
			}
			if (!done) {
				match(TokenType.COMA);
			}
		} while (!done);
	}

	private Token match(TokenType type) {
		Token tok = lexer.next();
		if (tok.getTokenType() != type) {
			throw new IllegalStateException("Unexpected token: " + tok);
		}

		return tok;
	}

	private boolean peek(TokenType type) {
		return lexer.peek().getTokenType() == type;
	}	
	
	public static class FunctionCall
		extends ExpressionNode {
		
		Token functionNameToken;
		
		List<ExpressionNode> params = new ArrayList<ExpressionNode>();
		
		boolean remote;
		
		FunctionCall(Token tok, Token functionNameToken) {
			super(tok, null, null);
			this.functionNameToken = functionNameToken;
			remote = tok.getTokenType() == TokenType.OARRAY;

			System.out.println("Function call: " + functionNameToken);
		}
		
		void addParam(ExpressionNode param) {
			params.add(param);
			System.out.println("added param: " + param.getToken());
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder("{" + functionNameToken.value() + "(");
			for (ExpressionNode param : params) {
				sb.append(param.toString()).append(" ");
			}
			sb.append(")");
			
			return sb.toString();
		}
	}

}
