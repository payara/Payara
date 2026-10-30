/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
// Portions Copyright 2018-2025 Payara Foundation and/or its affiliates
// Portions Copyright 2024 Contributors to the Eclipse Foundation
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license

package org.glassfish.security.common;

import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;

/**
 * In EJBs, ACL checking is done using the Roles. Roles are an abstraction of an application specific Logical
 * Principals. These Principals do not have any properties of Principals within a Security Domain (or Realm). They
 * merely serve as abstraction to application specific entities.
 * 
 * @author Harish Prabandham
 */
public class Role implements Principal, Serializable {

    private static final long serialVersionUID = -7801565721107580516L;
    
    private String description;
    private final String name;

    /** Creates a new Role with a given name */
    public Role(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Role role = (Role) other;
        return getName().equals(role.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDescription() {
        if (this.description == null) {
            this.description = "";
        }
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
