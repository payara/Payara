/*
 * Copyright (c) 2021 Contributors to Eclipse Foundation. All rights reserved.
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
package com.sun.enterprise.security;

import java.security.Principal;
import java.util.Enumeration;

/**
 * A group of principals.
 *
 * @author Arjan Tijms
 *
 */
public interface GroupPrincipal extends Principal {

    /**
     * Returns true when the given principal is in this group.
     *
     * <p>
     * A recursive search is done, meaning that if a principal is in a group which is itself in this group, the result is true.
     *
     * @param principal the principal for which we check to be in this group.
     *
     * @return true if the principal is in this group, false otherwise.
     */
    boolean isMember(Principal principal);

    /**
     * Returns an enumeration of all the principals in this group.
     *
     * <p>
     * The returned principals can include principals that are besides instanced of Principal also instances of GroupPrincipal.
     *
     * @return an enumeration of principals in this group, potentially including nested group principals.
     */
    Enumeration<? extends Principal> members();

}
