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
 * Created on November 19, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbqlc;

import java.util.Map;
import java.util.HashMap;

/**
 * The symbol table handling declared identifies.
 *
 * @author  Michael Bouschen
 */
public class SymbolTable
{
    /**
     * The table of declared identifier (symbols).
     */
    protected Map symbols = new HashMap();

	/**
	 * This method adds the specified identifier to this SymbolTable. 
     * The specified decl object provides details anbout the declaration. 
     * If this SymbolTable already defines an identifier with the same name, 
     * the SymbolTable is not changed and the existing declaration is returned. 
     * Otherwise <code>null</code> is returned.
     * @param   ident   identifier to be declared
     * @param   decl new definition of identifier
     * @return  the old definition if the identifier was already declared; 
     * <code>null</code> otherwise
	 */
    public Object declare(String ident, Object decl)
    {
        Object old = symbols.get(ident);
        if (old == null) {
            symbols.put(ident.toUpperCase(), decl);
        }
        return old;
    }

    /**
     * Checks whether the specified identifier is declared.  
     * @param ident the name of identifier to be tested
     * @return <code>true</code> if the identifier is declared; 
     * <code>false</code> otherwise.
     */
    public boolean isDeclared(String ident)
    {
        return (getDeclaration(ident) != null);
    }

    /**
     * Checks the symbol table for the actual declaration of the specified 
     * identifier. The method returns the declaration object if available or
     * <code>null</code> for an undeclared identifier. 
     * @param ident the name of identifier
     * @return the declaration object if ident is declared;
     * <code>null</code> otherise.
     */
    public Object getDeclaration(String ident)
    {
        return symbols.get(ident.toUpperCase());
    }
	
}
