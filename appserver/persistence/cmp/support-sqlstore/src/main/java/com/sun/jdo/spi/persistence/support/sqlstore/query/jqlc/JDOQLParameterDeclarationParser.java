/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

import antlr.ANTLRException;
import antlr.collections.AST;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import org.glassfish.ejb.deployment.descriptor.QueryParser;
import org.glassfish.persistence.common.I18NHelper;

/** Helper class to support parsing of JDOQL parameter declarations.
 * 
 * Created on October 16, 2002
 * @author  Michael Bouschen
 */
public class JDOQLParameterDeclarationParser
    implements QueryParser
{
    /**
     * I18N support
     */
    protected final static ResourceBundle messages = I18NHelper.loadBundle(
        JDOQLParameterDeclarationParser.class);

    /**
     * Returns an iterator over the parameter types of the specified JDOQL
     * parameter declartion. The types are represented by their name, thus
     * the Iterator's next method returns Strings. 
     * @param text the JDOQL parameter declaration
     * @return an iterator over parameter types
     * @exception JDOQueryException indicates a parse error
     */
    public Iterator parameterTypeIterator(String text)
    {
        return new ParameterTypeIterator(parse(text));
    }
    
    /**
     * Internal method parsing the JDOQL parameter declaration.
     * @param text the JDOQL parameter declaration
     * @return an AST representing the parameter declarations
     */
    private AST parse(String text)
    {
        if (text == null) {
            return null;
        }

        // return value
        AST paramsAST = null;

        // error message helper
        ErrorMsg errorMsg = new ErrorMsg();

        // create parser
        JQLParser parser = JQLC.createStringParser(text, errorMsg);

        try {
            // start parsing
            parser.parseParameters();
            // get the AST representation
            paramsAST = parser.getAST();
        }
        catch (ANTLRException ex) {
            // handle any exceptions thrown by lexer or parser
            JQLParser.handleANTLRException(ex, errorMsg);
        }

        return paramsAST;
    }
    
   /**
    * Iterator over the parameter types. The next method returns the type
    * of the next parameter represented as String.
    */
    private static class ParameterTypeIterator 
        implements Iterator
    {
        // current parameter declaration node
        private AST current;

        /**
         * The constructor takes the parameter declarations AST. A
         * parameter declaration node must have a PARAMETER_DEF node as
         * root. The first child is the type, the next child is the name of
         * the parameter. All subsequent parameter declarations as siblings
         * of the specified ast node.
         * @param ast the list of parameter declarations nodes
         */
        ParameterTypeIterator(AST ast)
        {
            current = ast;
        }

        /**
         * Returns <code>true</code> if the iteration has more elements.
         * @return <code>true</code> if the iterator has more elements.
         */
        public boolean hasNext()
        {
            return (current != null);
        }
        
        /**
         * Returns the next element in the iteration. For this Iterator it
         * returns the String representation of the type of the next
         * parameter declaration. 
         * @return the type of the next parameter declaration.
         * @exception NoSuchElementException iteration has no more elements.
         */
        public Object next()
        {
            // check whether iteration has no more elements
            if (current == null)
                throw new NoSuchElementException();

            // Check whether the current node has the token type
            // PARAMETER_DEF => throw exception if not.
            if (current.getType() != JQLParser.PARAMETER_DEF)
                throw new JDOFatalInternalException(I18NHelper.getMessage(
                    messages,
                    "jqlc.jdoqlparameterdeclarationparser.next.wrongtoken", //NOI18N
                    current.getType()));

            // get string repr of parameter type node
            String typeRepr = getTypeRepr(current.getFirstChild());

            // advance current ast node to next parameter declaration node
            current = current.getNextSibling();

            return typeRepr;
        }
        
        /**
         * Not supported.
         * @exception UnsupportedOperationException remove is not supported
         * by this Iterator
         */
        public void remove() { throw new UnsupportedOperationException(); }

        /**
         * Internal method to calculate the string representation of a type
         * node.
         * @param ast the type node
         * @return the string representation 
         */
        private String getTypeRepr(AST ast)
        {
            if (ast == null)
                return "";

            // Check for DOT nodes #(DOT left right)
            // They are represented as left.right
            if (ast.getType() == JQLParser.DOT) {
                AST left = ast.getFirstChild();
                AST right = left.getNextSibling();
                return getTypeRepr(left) + "." + getTypeRepr(right);
            }

            // For all other nodes return the token text
            return ast.getText();
        }
    }
    
    /**
     * Method main for testing purposes. Parameter args is expected to a
     * an array of parameter declaration String. One parameter declaration
     * may declare multiple parameters according to the JDOQL parameter
     * declaration syntax. Calling  
     * java com...jqlc.ParameterDeclarationHelper "int a, String b"
     * will print
     * <br><code>
     * Parameter types for >int a, String b<
     * <br>
     *   int
     * <br>
     *   String</code>
     * <br>
     */
    public static void main(String[] args)
    {
        QueryParser helper = new JDOQLParameterDeclarationParser();
        for (int i = 0; i < args.length; i++) {
            String text = args[i];
            System.out.println("Parameter types for >" + text + "<");
            for (Iterator types = helper.parameterTypeIterator(text); 
                 types.hasNext();) {
                System.out.println("  " + types.next());
            }
        }
    }

}
