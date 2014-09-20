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

/**
 *	This generated bean class Property matches the schema element property
 *
 *	Generated on Thu May 06 00:44:23 PDT 2004
 */
package org.glassfish.loadbalancer.admin.cli.beans;

import org.netbeans.modules.schema2beans.*;
import java.util.*;

// BEGIN_NOI18N
public class Property extends org.netbeans.modules.schema2beans.BaseBean {

    static Vector comparators = new Vector();
    static public final String NAME = "Name";	// NOI18N
    static public final String VALUE = "Value";	// NOI18N
    static public final String DESCRIPTION = "Description";	// NOI18N

    public Property() {
        this(Common.USE_DEFAULT_VALUES);
    }

    public Property(int options) {
        super(comparators, new org.netbeans.modules.schema2beans.Version(1, 2, 0));
        // Properties (see root bean comments for the bean graph)
        this.createProperty("description", // NOI18N
                DESCRIPTION,
                Common.TYPE_0_1 | Common.TYPE_STRING | Common.TYPE_KEY,
                String.class);
        this.initialize(options);
    }

    // Setting the default values of the properties
    void initialize(int options) {
    }

    // This attribute is mandatory
    public void setName(java.lang.String value) {
        setAttributeValue(NAME, value);
    }

    //
    public java.lang.String getName() {
        return getAttributeValue(NAME);
    }

    // This attribute is mandatory
    public void setValue(java.lang.String value) {
        setAttributeValue(VALUE, value);
    }

    //
    public java.lang.String getValue() {
        return getAttributeValue(VALUE);
    }

    // This attribute is optional
    public void setDescription(String value) {
        this.setValue(DESCRIPTION, value);
    }

    //
    public String getDescription() {
        return (String) this.getValue(DESCRIPTION);
    }

    //
    public static void addComparator(org.netbeans.modules.schema2beans.BeanComparator c) {
        comparators.add(c);
    }

    //
    public static void removeComparator(org.netbeans.modules.schema2beans.BeanComparator c) {
        comparators.remove(c);
    }

    public void validate() throws org.netbeans.modules.schema2beans.ValidateException {
        boolean restrictionFailure = false;
        // Validating property name
        if (getName() == null) {
            throw new org.netbeans.modules.schema2beans.ValidateException("getName() == null", "name", this);	// NOI18N
        }
        // Validating property value
        if (getValue() == null) {
            throw new org.netbeans.modules.schema2beans.ValidateException("getValue() == null", "value", this);	// NOI18N
        }
        // Validating property description
        if (getDescription() != null) {
        }
    }

    // Dump the content of this bean returning it as a String
    @Override
    public void dump(StringBuffer str, String indent) {
        String s;
        Object o;
        org.netbeans.modules.schema2beans.BaseBean n;
        str.append(indent);
        str.append("Description");	// NOI18N
        str.append(indent + "\t");	// NOI18N
        str.append("<");	// NOI18N
        s = this.getDescription();
        str.append((s == null ? "null" : s.trim()));	// NOI18N
        str.append(">\n");	// NOI18N
        this.dumpAttributes(DESCRIPTION, 0, str, indent);

    }

    @Override
    public String dumpBeanNode() {
        StringBuffer str = new StringBuffer();
        str.append("Property\n");	// NOI18N
        this.dump(str, "\n  ");	// NOI18N
        return str.toString();
    }
}

// END_NOI18N

