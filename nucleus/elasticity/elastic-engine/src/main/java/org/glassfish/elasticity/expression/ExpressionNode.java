package org.glassfish.elasticity.expression;

import java.io.Serializable;

/**
 * A node in a parse tree
 * 
 * @author Mahesh Kannan
 *
 */
public class ExpressionNode
    implements Serializable {
	
	/**
	 * The token that contains the type and value
	 */
	private Token token;

    private Object data;

    private Object evaluatedResult;

    private Class evaulatedType;

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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getEvaluatedResult() {
        return evaluatedResult;
    }

    public void setEvaluatedResult(Object evaluatedResult) {
        this.evaluatedResult = evaluatedResult;
    }

    public Class getEvaulatedType() {
        return evaulatedType;
    }

    public void setEvaulatedType(Class evaulatedType) {
        this.evaulatedType = evaulatedType;
    }

    public String toString() {
		return "{" + token.value() + " " + (left == null ? "" : left.toString()) + " " + (left == null ? "" : right.toString()) + "}";
	}
	
}
