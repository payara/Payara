/*
 * Copyright (c) 2022, 2024 Contributors to the Eclipse Foundation
 * Copyright (c) 2022 Eclipse Foundation and/or its affiliates. All rights reserved.
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

package org.glassfish.security.common;

import java.util.Arrays;
import java.util.Objects;

/**
 * Concrete implementation of {@link UserPrincipal} with username and password.
 * <ul>
 * <li>The password is not mandatory, it can be null.
 * <li>Two instances are equal if they have the same name and the same password.
 * </ul>
 *
 * @author David Matejcek
 */
public class UserNameAndPassword implements UserPrincipal {

    private static final long serialVersionUID = 1L;
    private final String name;
    private final char[] password;
    private final int hashCode;

    /**
     * @param name non-null name
     */
    public UserNameAndPassword(String name) {
        this(name, (char[]) null);
    }


    /**
     * @param name non-null name
     * @param password can be null.
     */
    public UserNameAndPassword(String name, String password) {
        this(name, password == null ? null : password.toCharArray());
    }


    /**
     * @param name non-null name
     * @param password can be null.
     */
    public UserNameAndPassword(String name, char[] password) {
        this.name = Objects.requireNonNull(name);
        this.password = password == null ? null : Arrays.copyOf(password, password.length);
        this.hashCode = 31 * name.hashCode() + Arrays.hashCode(this.password);
    }


    /**
     * @return never null.
     */
    @Override
    public final String getName() {
        return name;
    }


    /**
     * @return password as a string, can be null.
     */
    public final String getStringPassword() {
        return password == null ? null : new String(password);
    }


    /**
     * @return password as a string, can be null.
     */
    public final char[] getPassword() {
        return password == null ? null : Arrays.copyOf(password, password.length);
    }


    /**
     * @return true if the object is an instance of {@link UserNameAndPassword} and has the same
     *         name and the same password.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof UserNameAndPassword) {
            UserNameAndPassword another = (UserNameAndPassword) o;
            return Objects.equals(getName(), another.getName())
                    && Objects.equals(getPassword(), another.getPassword());
        }
        return false;
    }


    @Override
    public int hashCode() {
        return this.hashCode;
    }


    @Override
    public String toString() {
        return "UserNameAndPassword[" + this.name + "]";
    }
}