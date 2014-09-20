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
 * FieldInfo.java
 *
 * Created on May 2, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.query.util.type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ResourceBundle;

import com.sun.jdo.api.persistence.model.jdo.PersistenceFieldElement;
import com.sun.jdo.api.persistence.model.jdo.RelationshipElement;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import org.glassfish.persistence.common.I18NHelper;

/**
 *
 */
public class FieldInfo
{
    /**
     * The name of the field.
     */
    protected String name;

    /**
     * The corresponding classType object.
     */
    protected ClassType classType;
    
    /**
     * The reflection representation of the field.
     */
    protected Field field;

    /**
     * JDO model representation
     */
    protected PersistenceFieldElement pfe;

    /**
     * I18N support
     */
    protected final static ResourceBundle messages = I18NHelper.loadBundle(
        "com.sun.jdo.spi.persistence.support.sqlstore.query.Bundle", // NOI18N
        FieldInfo.class.getClassLoader());

    /**
     * 
     */
    public FieldInfo (Field field, ClassType classType)
    {
        this.name = field.getName();
        this.classType = classType;
        this.field = field;
        this.pfe = (classType.pce != null) ? classType.pce.getField(this.name) : null;
    }

    /**
     * Checks whether this field is defined as persistent field.
     * @return true if the field is defined as persistent;
     * false otherwise.
     */
    public boolean isPersistent()
    {
        if (pfe != null)
        {
            return pfe.getPersistenceType() == PersistenceFieldElement.PERSISTENT;
        }
        return false;
    }
    
    /**
     * Checks whether this field is defined with the public modifier.
     * @return true if the field is defined as public;
     * false otherwise.
     */
    public boolean isPublic()
    {
        return (field != null) && Modifier.isPublic(field.getModifiers());
    }
    /**
     * Checks whether this field is defined with the static modifier.
     * @return true if the field is defined as static;
     * false otherwise.
     */
    public boolean isStatic()
    {
        return (field != null) && Modifier.isStatic(field.getModifiers());
    }
    
    /**
     *
     */
    public Field getField ()
    {
        return field;
    }
    
    /**
     *
     */
    public String getName ()
    {
        return name;
    }
    
    /**
     * Returns the Type representation of the type of the field.
     * @return field type
     */
    public Type getType()
    {
        if (field == null)
            return classType.typetab.errorType;
        
        Type ret = classType.typetab.checkType(field.getType());
        if (ret == null)
            ret = classType.typetab.errorType;
        return ret;
	        
    }

    /**
     * Return the field number in the case of a field of a persistence capable class.
     */
    public int getFieldNumber()
    {
        if (pfe != null)
        {
            int index = pfe.getFieldNumber();
            if (index < 0)
                throw new JDOFatalInternalException(I18NHelper.getMessage(
                    messages, "query.util.type.fieldinfo.getfieldnumber.invalidfieldno", //NO18N
                    String.valueOf(index), name));
            return index;
        }
        else
        {
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages, "query.util.type.fieldinfo.getfieldnumber.missingfieldelement", //NO18N
                name));
        }
    }

    /**
     * @return true if the field is a relationship field
     */
    public boolean isRelationship()
    {
        return ((pfe != null) && (pfe instanceof RelationshipElement));
    }

    /**
     * @return the associated class (meaning the "other side") of the relationship; 
     * or null if this does not denote a relationship field.
     */
    public Type getAssociatedClass()
    {
        Type associatedClass = null;
        if ((pfe != null) && (pfe instanceof RelationshipElement))
        {
            String className = ((RelationshipElement)pfe).getElementClass();
            associatedClass = classType.typetab.checkType(className);
        }
        return associatedClass;
    }
    
}
