/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.security.services.impl.authorization;

import org.glassfish.security.services.api.authorization.AzAttributes;
import org.glassfish.security.services.impl.common.AttributesImpl;

/**
 * <code>AzAttributesImpl</code> implements AzAttributes to provide an abstract
 * layer specifically for Authorization attributes.
 */
public abstract class AzAttributesImpl extends AttributesImpl implements AzAttributes {

    private final String name;


    /**
     * Copy constructor
     *
     * @param other The copy source
     */
    public AzAttributesImpl( AzAttributesImpl other ) {
        super( other );

        this.name = other.getName();
    }


    /**
     * Constructor
     * @param name The name of this collection.
     * @throws IllegalArgumentException Given name is null or empty
     */
    public AzAttributesImpl( String name ) {

        if ( null == name ) {
            throw new IllegalArgumentException( "Illegal null name given." );
        }

        name = name.trim();
        if ( name.isEmpty() ) {
            throw new IllegalArgumentException( "Illegal empty name given." );
        }

        this.name = name;
    }

    /**
     * Determines a name to denote this collection of attributes.
     * <p>
     * For example, this name may be used to qualify attributes by collection
     * type in XACML.
     *
     * @return The collection name
     * @see org.glassfish.security.services.api.authorization.AzAttributes#getName()
     */
    @Override
    public String getName() {
        return name;
    }
}
