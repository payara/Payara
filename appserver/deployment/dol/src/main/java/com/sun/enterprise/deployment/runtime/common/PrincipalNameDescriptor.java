/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019-2024] [Payara Foundation and/or its affiliates]
// Portions Copyright 2024 Contributors to the Eclipse Foundation
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license
package com.sun.enterprise.deployment.runtime.common;

import java.security.Principal;

import com.sun.enterprise.deployment.node.runtime.common.DescriptorPrincipalName;
import org.glassfish.deployment.common.Descriptor;

/**
 * This is an in memory representation of the principal-name with its name of the implementation class.
 * 
 * @author deployment dev team
 */
public class PrincipalNameDescriptor extends Descriptor {

    private static final long serialVersionUID = 884693766288296132L;

    private String principalName;
    private String className;
    private transient ClassLoader classLoader;

    public PrincipalNameDescriptor(String principalName) {
        this.principalName = principalName;
    }

    public String getName() {
        return principalName;
    }

    public String getClassName() {
        if (className == null) {
            return DescriptorPrincipalName.class.getName();
        }
        
        return className;
    }

    public void setName(String name) {
        principalName = name;
    }

    public void setClassName(String name) {
        className = name;
    }

    public void setClassLoader(ClassLoader c) {
        classLoader = c;
    }

    public Principal getPrincipal() {
        try {
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            
            return (Principal) 
                Class.forName(getClassName(), true, classLoader)
                     .getConstructor(new Class<?>[] { String.class })
                     .newInstance(new Object[] { principalName });
            
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public String toString() {
        return "principal-name " + principalName + "; className " + getClassName();
    }
}
