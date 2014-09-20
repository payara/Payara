/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * EJBQLAST.java
 *
 * Created on November 12, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbqlc;

import antlr.Token;
import antlr.CommonAST;
import antlr.collections.AST;

/** 
 * An instance of this class represents a node of the intermediate 
 * representation (AST) used by the query compiler. It stores per node:
 * <ul>
 * <li> token type info
 * <li> token text 
 * <li> line info
 * <li> column info
 * <li> type info the semantic analysis calculates the type of an expression 
 * and adds this info to each node.
 * </ul>
 * 
 * @author  Michael Bouschen
 */
public class EJBQLAST
    extends CommonAST
    implements Cloneable
{
    /** */
    private static char SEPARATOR = '\n';

    /** */
    private static String INDENT = "  "; //NOI18N

    /** The line info */
    protected int line = 0;

    /** The column info */
    protected int column = 0;

    /** The type info */
    protected transient Object typeInfo;

    /** No args constructor. */
    public EJBQLAST() {}

    /** Constructor taking token type, text and type info. */
    public EJBQLAST(int type, String text, Object typeInfo)
    {
        initialize(type, text, typeInfo);
    }

    /** Copy constructor. */
    public EJBQLAST(EJBQLAST ast)
    {
        initialize(ast);
    }
    
    /** */
    public void initialize(Token t)
    {
        setType(t.getType());
        setText(t.getText());
        setLine(t.getLine());
        setColumn(t.getColumn());
    }

    /** */
    public void initialize(int type, String text, Object typeInfo)
    {
        setType(type);
        setText(text);
        setTypeInfo(typeInfo);
    }

    /** */
    public void initialize(AST _ast)
    {
        EJBQLAST ast = (EJBQLAST)_ast;
        setType(ast.getType());
        setText(ast.getText());
        setLine(ast.getLine());
        setColumn(ast.getColumn());
        setTypeInfo(ast.getTypeInfo());
    }
    
    /** */
    public void setLine(int line)
    {
        this.line = line;
    }

    /** */
    public int getLine()
    {
        return line;
    }

    /** */
    public void setColumn(int column)
    {
        this.column = column;
    }

    /** */
    public int getColumn()
    {
        return column;
    }

    /** */
    public void setTypeInfo(Object typeInfo)
    {
        this.typeInfo = typeInfo;
    }

    /** */
    public Object getTypeInfo()
    {
        return typeInfo;
    }

    /** 
     * Returns a string representation of this EJBQLAST w/o child ast nodes.
     * @return a string representation of the object.
     */
    public String toString()
    {
        Object typeInfo = getTypeInfo();
        StringBuffer repr = new StringBuffer();
        // token text
        repr.append((getText() == null ? "null" : getText())); //NOI18N
        repr.append(" ["); //NOI18N
        // token type
        repr.append(getType());
        // line/column info
        repr.append(", ("); //NOI18N
        repr.append(getLine() + "/" + getColumn()); //NOI18N
        repr.append(")"); //NOI18N
        // type info
        repr.append(", "); //NOI18N
        repr.append(typeInfo);
        repr.append("]"); //NOI18N
        return repr.toString();
    }

    /** 
     * Returns a full string representation of this JQLAST. 
     * The returned string starts with the specified title string, 
     * followed by the string representation of this ast,
     * followed by the string representation of the child ast nodes of this ast.
     * The method dumps each ast node on a separate line. 
     * Child ast nodes are indented.
     * The method calls toString to dump a single node w/o children.
     * @return string representation of this ast including children.
     */
    public String getTreeRepr(String title)
    {
        return title + this.getTreeRepr(0);
    }

    /** Helper method for getTreeRepr. */
    private String getTreeRepr(int level)
    {
        StringBuffer repr = new StringBuffer();
        // current node
        repr.append(SEPARATOR);
        repr.append(getIndent(level));
        repr.append(this.toString());
        // handle children
        for (EJBQLAST node = (EJBQLAST)this.getFirstChild(); 
             node != null; 
             node = (EJBQLAST)node.getNextSibling()) {
            repr.append(node.getTreeRepr(level+1));
        }
        return repr.toString();
    }
    
    /** Returns the indent specified by level. */
    private String getIndent(int level) 
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < level; i++) {
            buf.append(INDENT);
        }
        return buf.toString();
    }

    /**
     * Creates and returns a copy of this object.
     * The returned EJBQLAST shares the same state as this object, meaning 
     * the fields type, text, line, column, and typeInfo have the same values. 
     * But it is not bound to any tree structure, thus the child is null 
     * and the sibling is null.
     * @return a clone of this instance.
     */
    protected Object clone()
        throws CloneNotSupportedException
    {
        EJBQLAST clone = (EJBQLAST)super.clone();
        clone.setFirstChild(null);
        clone.setNextSibling(null);
        return clone;
    }
    
}

