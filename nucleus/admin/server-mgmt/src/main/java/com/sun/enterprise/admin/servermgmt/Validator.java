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

package com.sun.enterprise.admin.servermgmt;

import com.sun.enterprise.util.i18n.StringManager;

/**
 * Base class for all domain config validators. Validates the non-null ness
 * of a domain config entry and its type.
 */
public class Validator
{
    /**
     * i18n strings manager object
     */
    private static final StringManager strMgr = 
        StringManager.getManager(Validator.class);

    /**
     * The accepted type of an entry.
     */
    private final Class type;

    /**
     * The name of an entry that is used in case of validation error.
     */
    private final String name;

    /**
     * Constructs new Validator object.
     * @param name Name of an entry that is used in case of validation errors.
     * If the name is null "" is used instead.
     * @param type 
     */
    public Validator(String name, Class type)
    {
        this.name = (name != null) ? name : "";
        this.type = (type != null) ? type : java.lang.Object.class;
    }

    /**
     * Returns the name of the entry.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Checks the validity of the given value for the entry. This method does
     * basic checks such as null ness & type.
     * @param obj
     * @Throws InvalidConfigException
     */
    public void validate(Object obj) throws InvalidConfigException
    {
        if (obj == null)
        {
            throw new InvalidConfigException(
                strMgr.getString("validator.invalid_value", getName(), null));
        }
        Class c = obj.getClass();
        if (!type.isAssignableFrom(c))
        {
            throw new InvalidConfigException(
                strMgr.getString("validator.invalid_type", 
                    getName(), type.getName(), c.getName()));
        }
    }
}
