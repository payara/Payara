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
 * EJBQLASTFactory.java
 *
 * Created on November 12, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbqlc;

import antlr.collections.AST;
import antlr.ASTFactory;

import java.util.ResourceBundle;
import org.glassfish.persistence.common.I18NHelper;

/** 
 * Factory to create and connect EJBQLAST nodes.
 *
 * @author  Michael Bouschen
 */
public class EJBQLASTFactory
    extends ASTFactory
{
    /** The singleton EJBQLASTFactory instance. */    
    private static EJBQLASTFactory factory = new EJBQLASTFactory();

    /** I18N support. */
    private final static ResourceBundle msgs = 
        I18NHelper.loadBundle(EJBQLASTFactory.class);
    
    /** 
     * Get an instance of EJBQLASTFactory.
     * @return an instance of EJBQLASTFactory
     */    
    public static EJBQLASTFactory getInstance()
    {
        return factory;
    }
    
    /**
     * Constructor. EJBQLASTFactory is a singleton, please use 
     * {@link #getInstance} to get the factory instance.
     */
    protected EJBQLASTFactory()
    {
        this.theASTNodeTypeClass = EJBQLAST.class;
        this.theASTNodeType = this.theASTNodeTypeClass.getName();
    }
    
    /** Overwrites superclass method to create the correct AST instance. */
    public AST create() 
    {
        return new EJBQLAST();
    }

    /** Overwrites superclass method to create the correct AST instance. */
    public AST create(AST tr) 
    { 
        return create((EJBQLAST)tr);
    }

    /** Creates a clone of the specified EJBQLAST instance. */
    public EJBQLAST create(EJBQLAST tr) 
    { 
        try {
            return (tr==null) ? null : (EJBQLAST)tr.clone();
        }
        catch(CloneNotSupportedException ex) {
            throw new EJBQLException(
                I18NHelper.getMessage(msgs, "ERR_UnexpectedExceptionClone"), ex); //NOI18N
        }
    }
}

