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
 * ParameterInfo
 *
 * Created on January 31, 2003
 */

package com.sun.jdo.spi.persistence.utility;

//XXX FIXME This file may need to move under support/sqlstore.
public class ParameterInfo
{
    /** 
     * Parameter index.
     * The index corresponds to JDO QL parameters.
     */
    private final int index;

    /** Parameter type. See FieldTypeEnumeration for possible values. */
    private final int type;

    /**
     * Associated field to a parameter for runtime processing.
     * This is defined if and only if the corresponding subfilter is of
     * the form: field [relational op] _jdoParam or
     *           _jdoParam [relational op] field
     * Otherwise, this is null.
     */
    private final String associatedField;

    /** Constructor */
    public ParameterInfo(int index, int type)
    {
        this(index, type, null);
    }

    /**
     * Constructs a new ParameterInfo with the specified index, type and
     * associatedField.
     * @param index
     * @param type
     * @param associatedField
     */
    public ParameterInfo(int index, int type, String associatedField)
    {
        this.index = index;
        this.type = type;
        this.associatedField = associatedField;
    }

    /** Returns the parameter index. */
    public int getIndex()
    {
        return index;
    }

    /** Returns the parameter type. See FieldTypeEnumeration for possible values. */
    public int getType()
    {
        return type;
    }

    /**
     * Returns the associated field.
     */
    public String getAssociatedField()
    {
        return associatedField;
    }
}
