package org.glassfish.elasticity.expression;

/**
 * A node in a parse tree
 * 
 * @author Mahesh Kannan
 *
 */
public class ExpressionNode {
	
	/**
	 * The token that contains the type and value
	 */
	private Token token;

	/**
	 * The subtrees that represents the arguments of this node
	 */
	private ExpressionNode left;
	
	private ExpressionNode right;
	
	public ExpressionNode(Token token, ExpressionNode left, ExpressionNode right) {
		super();
		this.token = token;
		this.left = left;
		this.right = right;
	}


	public Token getToken() {
		return token;
	}

	public ExpressionNode getLeft() {
		return left;
	}

	public ExpressionNode setLeft(ExpressionNode left) {
		this.left = left;
		return this;
	}

	public ExpressionNode getRight() {
		return right;
	}

	public ExpressionNode setRight(ExpressionNode right) {
		this.right = right;
		return this;
	}	
	
	public String toString() {
		return "{" + token.value() + " " + (left == null ? "" : left.toString()) + " " + (left == null ? "" : right.toString()) + "}";
	}
	
}
