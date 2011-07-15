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
 * Definition.java
 *
 * Created on March 8, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.query.util.scope;

import java.util.ResourceBundle;

import org.glassfish.persistence.common.I18NHelper;

import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.Type;

/**
 * Super class of all possible identifier definitions
 *
 * @author  Michael Bouschen
 * @version 0.1
 */
public abstract class Definition
{
    /**
     * I18N support
     */
	protected final static ResourceBundle messages = 
            I18NHelper.loadBundle(Definition.class);
    
    /**
     * Scope level of the definition
     */
    protected int scope;

    /**
     * Type of the identifier
     */
    protected Type type;

    /**
     * Creates a new definition.
     * A definition contains at least the type of the identifier.
     * @param type type of the declared identifier
     */
    public Definition (Type type)
    {
        this.type = type;
    }

    /**
     * Set the scope of the identifier's definition.
     */
    public void setScope(int scope)
    {
        this.scope = scope;
    }

    /**
     * Returns the scope of the identifier's definition.
     */
    public int getScope()
    {
        return scope;
    }

    /**
     * Returns the type of the identifiers's definition.
     */
    public Type getType()
    {
        return type;
    }
	
    /**
     * Returns the name of the definition.
     */
    public abstract String getName();
}
