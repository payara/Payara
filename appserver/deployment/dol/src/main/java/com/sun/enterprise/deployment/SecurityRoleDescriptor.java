/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.web.SecurityRole;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.security.common.Role;

/**
 * I am an abstract role..
 *
 * @author Danny Coward
 */
public class SecurityRoleDescriptor extends Descriptor implements SecurityRole {

    private static final long serialVersionUID = 7523991714027594391L;

    /**
     * Default constructor.
     */
    public SecurityRoleDescriptor() {
    }

    /**
     * Construct a SecurityRoleDescriptor from the given role name.
     */
    public SecurityRoleDescriptor(String name) {
        setName(name);
    }

    /**
     * Construct a SecurityRoleDescriptor from the given role name and description.
     */
    public SecurityRoleDescriptor(String name, String description) {
        super(name, description);
    }

    /**
     * Construct a SecurityRoleDescriptor from the given role object.
     */
    public SecurityRoleDescriptor(Role role) {
        super(role.getName(), role.getDescription());
    }

    /**
     * Equality on rolename.
     */
    public boolean equals(Object other) {
        if (other instanceof SecurityRoleDescriptor && this.getName().equals(((SecurityRoleDescriptor) other).getName())) {
            return true;
        }
        return false;
    }

    /**
     * My hashcode.
     */

    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * Formatted string representing my state.
     */
    public void print(StringBuilder toStringBuilder) {
        toStringBuilder.append("SecurityRole ");
        super.print(toStringBuilder);
    }

}
