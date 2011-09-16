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

	public ExpressionNode parse() {
        ExpressionNode node = booleanExpr();
		System.out.println("ExpressionTree: " + node);

        return node;
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
            root = new ExpressionNode(metricNameToken, null, null);
			if (peek(TokenType.DOT)) {
                AttributeAccessNode attrNode = new AttributeAccessNode(metricNameToken);
                root = attrNode;
                do {
                    match(TokenType.DOT);
                    attrNode.addAttribute(match(TokenType.IDENTIFIER));
                } while (peek(TokenType.DOT));
			} else if (peek(TokenType.OPAR)) {
				Token funcCall = match(TokenType.OPAR);
				root = new FunctionCall(funcCall, metricNameToken);
				functionCallParams((FunctionCall) root);
				System.out.println("Matched function call: " + metricNameToken.value() + " has " + ((FunctionCall) root).getParams().size() + " params");
				match(TokenType.CPAR);
			} else if (peek(TokenType.OARRAY)) {
				Token funcCall = match(TokenType.OARRAY);
				root = new FunctionCall(funcCall, metricNameToken);
				functionCallParams((FunctionCall) root);
				System.out.println("Matched function remote call access: " + metricNameToken.value() + " has " + ((FunctionCall) root).getParams().size() + " params");
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

    public static class AttributeAccessNode
        extends ExpressionNode {

        private Object evaluatedResult;

        private boolean isTabularMetric;

        public AttributeAccessNode(Token metricHolderToken) {
            super(new TokenImpl(TokenType.ATTR_ACCESS, metricHolderToken.value()), null, null);
            super.setData(new ArrayList());
        }

        public void addAttribute(Token token) {
            ((ArrayList) super.getData()).add(token.value());
        }

        public boolean isTabularMetric() {
            return isTabularMetric;
        }

        public void setTabularMetric(boolean tabularMetric) {
            isTabularMetric = tabularMetric;
        }

        public Object getEvaluatedResult() {
            return evaluatedResult;
        }

        public void setEvaluatedResult(Object evaluatedResult) {
            this.evaluatedResult = evaluatedResult;
        }
    }
	public static class FunctionCall
		extends ExpressionNode {

        private int nodeID;

		Token functionNameToken;
		
		List<ExpressionNode> params = new ArrayList<ExpressionNode>();
		
		boolean remote;
		
		FunctionCall(Token tok, Token functionNameToken) {
			super(new TokenImpl(TokenType.FUNCTION_CALL, tok.getTokenType().toString()), null, null);
			this.functionNameToken = functionNameToken;
			remote = tok.getTokenType() == TokenType.OARRAY;

			System.out.println("Function call: " + functionNameToken + "; isRemote: " + remote
            + "; this.getToken.getTokenType: " + (this.getToken().getTokenType() == TokenType.FUNCTION_CALL));
		}

        public int getNodeID() {
            return nodeID;
        }

        public void setNodeID(int nodeID) {
            this.nodeID = nodeID;
        }

        void addParam(ExpressionNode param) {
			params.add(param);
//			System.out.println("added param: " + param.getToken());
		}

        public boolean isRemote() {
            return remote;
        }

        public Token getFunctionNameToken() {
            return functionNameToken;
        }

        public List<ExpressionNode> getParams() {
            return params;
        }

		public String toString() {
			StringBuilder sb = new StringBuilder("{" + functionNameToken.value() + ":");
			for (ExpressionNode param : params) {
				sb.append(param.toString()).append(" ");
			}
			sb.append("); isRemote = " + remote + "}");
			
			return sb.toString();
		}
	}

}
