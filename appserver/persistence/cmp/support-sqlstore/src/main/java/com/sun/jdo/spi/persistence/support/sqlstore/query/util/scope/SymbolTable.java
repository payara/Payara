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
 * SymbolTable.java
 *
 * Created on March 8, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.query.util.scope;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;

/**
 * The symbol table handling declared identifies.
 *
 * @author  Michael Bouschen
 * @version 0.1
 */
public class SymbolTable
{
    /**
     * The actual scope level.
     */
    protected int actualScope = 0;
    
    /**
     * Stack of old definitions.
     */
    protected Stack nestings = new Stack();
    
    /**
     * The table of declared identifier (symbols).
     */
    protected Hashtable symbols = new Hashtable();

    /**
     * Opens a new scope. 
     * Prepare everything to handle old definitions when 
     * a identifier declaration is hidden.
     */
    public void enterScope()
    {
        actualScope++;
        nestings.push(new Nesting());
    }

    /**
     * Closes the actual scope.
     * Hidden definitions are reinstalled.
     */
    public void leaveScope()
    {
        forgetNesting((Nesting)nestings.pop());
        actualScope--;
    }

    /**
     * Returns the level of the actual scope.
     * @return actual scope level.
     */
	public int getActualScope()
	{
		return actualScope;
	}
	
	/**
	 * Add identifier to the actual scope.
     * If the identifier was already declared in the actual
     * scope the symbol table is NOT changed and the old definition
     * is returned. Otherwise a possible definition of a lower 
     * level scope is saved in the actual nesting and the new definition 
     * is stored in the symbol table. This allows to reinstall the old
     * definition when the sctaul scope is closed.
     * @param   ident   identifier to be declared
     * @param   def new definition of identifier
     * @return  the old definition if the identifier was already declared 
	 *          in the actual scope; null otherwise
	 */
    public Definition declare(String ident, Definition def)
    {
        Definition old = (Definition)symbols.get(ident);
        def.setScope(actualScope);
        if ((old == null) || (old.getScope() < actualScope))
        {
            Nesting nest = (Nesting)nestings.peek();
            nest.add(ident, old); // save old definition in nesting
            symbols.put(ident, def); // install new definition as actual definition
			return null;
        }
        else
        {
            return old;
        }
    }

    /**
     * Checks whether the specified identifier is declared.  
     * @param ident the name of identifier to be tested
     * @return true if the identifier is declared; 
     * false otherwise.
     */
    public boolean isDeclared(String ident)
    {
        return (getDefinition(ident) != null);
    }

    /**
     * Checks the symbol table for the actual definition 
     * of the specified identifier. If the identifier is 
     * declared the definition is returned, otherwise null.
     * @param ident the name of identifier
     * @return the actual definition of ident is declared;
     * null otherise.
     */
    public Definition getDefinition(String ident)
    {
        return (Definition)symbols.get(ident);
    }
	
    /**
     * Internal method to reinstall the old definitions. 
     * The method is called when a scope is closed. 
     * For all identifier that were declared in the 
     * closed scope their former definition (that was hidden) 
     * is reinstalled.
     * @param nesting list of hidden definitions
     */
    protected void forgetNesting(Nesting nesting)
    {
        String ident = null;
        Definition hidden = null;

        Iterator idents = nesting.getIdents();
        Iterator hiddenDefs = nesting.getHiddenDefinitions();

        while (idents.hasNext())
        {
            ident = (String) idents.next();
            hidden = (Definition) hiddenDefs.next();
            if (hidden == null)
            {
                symbols.remove(ident);
            }
            else
            {
                symbols.put(ident, hidden);
            }
        }
    }
}
