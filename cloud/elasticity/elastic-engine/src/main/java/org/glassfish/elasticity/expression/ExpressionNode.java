/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
