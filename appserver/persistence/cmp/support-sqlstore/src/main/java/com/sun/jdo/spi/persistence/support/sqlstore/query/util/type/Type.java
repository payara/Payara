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
 * Type.java
 *
 * Created on March 8, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.query.util.type;

import com.sun.jdo.spi.persistence.utility.FieldTypeEnumeration;

/**
 *
 * @author  Michael Bouschen
 * @version 0.1
 */
public abstract class Type
{
    /**
     * The name of the type represented by this object.
     */
    protected String name;

    /**
     * The corresponding class object.
     */
    protected Class clazz;

    /**
     * The FieldTypeEnumeration constant for this Type.
     */
    protected int enumType;

    /**
     * Creates a new Type object with the specified name.
     * @param name name of the type represented by this
     * @param clazz the class object for this type
     */
    public Type(String name, Class clazz)
    {
        this(name, clazz, FieldTypeEnumeration.NOT_ENUMERATED);
    }

    /**
     * Creates a new Type object with the specified name.
     * @param name name of the type represented by this
     * @param clazz the class object for this type
     * @param enumType the FieldTypeEnumeration value for this type
     */
    public Type(String name, Class clazz, int enumType)
    {
        this.name = name;
        this.clazz = clazz;
        this.enumType = enumType;
    }

    /**
     * Returns the name of the type.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the corresponding class object.
     */
    public Class getJavaClass() {
        return this.clazz;
    }

    /**
     * Checks type compatibility.
     * @param type the type this is checked with.
     * @return true if this is compatible with type;
     * false otherwise.
     */
    public abstract boolean isCompatibleWith(Type type);

    /**
     * Returns whether this represents a type with an
     * defined order.
     * @return true if an order is defined for this;
     * false otherwise.
     */
    public boolean isOrderable()
    {
        return false;
    }

    /**
     * Returns the FieldTypeEnumeration value for this type.
     */
    public int getEnumType()
    {
        return enumType;
    }

    /**
     * Representation of this type as a string.
     */
    public String toString()
    {
        return getName();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * Two types are equal if their names are equal.
     */
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        else if (obj instanceof Type)
            return this.name.equals(((Type)obj).name);
        else
            return false;
    }


}
