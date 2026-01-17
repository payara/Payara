/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.enterprise.deployment.node.runtime.common;

import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;

import org.glassfish.security.common.UserPrincipal;

/**
 * {@link Principal} loaded from XML descriptor.
 * When the equals method is used, it compares just principal names and that the other object
 * is an {@link Principal} instance too.
 */
// Must be UserPrincipal, because RoleMapper.internalAssignRole knows just that and Group.
public class DescriptorPrincipalName implements UserPrincipal, Serializable {

    private static final long serialVersionUID = -640182254691955451L;

    private final String name;

    /**
     * @param name must not be null.
     */
    public DescriptorPrincipalName(String name) {
        this.name = Objects.requireNonNull(name, "XML principal-name element must not be null.");
    }


    @Override
    public String getName() {
        return name;
    }


    @Override
    public int hashCode() {
        return name.hashCode();
    }


    /**
     * We match user principals just by name.
     * This is used in Jakarta Security to resolve authorisation.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Principal) {
            Principal other = (Principal) o;
            return getName().equals(other.getName());
        }
        return false;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getName() + "]";
    }
}
