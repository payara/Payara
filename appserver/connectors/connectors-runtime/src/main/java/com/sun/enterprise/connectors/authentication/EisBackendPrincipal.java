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

package com.sun.enterprise.connectors.authentication;

import java.io.Serializable;

/**
 * This a javabean class thatabstracts the backend principal.
 * The backend principal consist of the userName and password
 * which is used for authenticating/getting connection from
 * the backend.
 *
 * @author Srikanth P
 */
public class EisBackendPrincipal implements Serializable {

    private String userName;
    private String password;

    /**
     * Default constructor
     */
    public EisBackendPrincipal() {
    }

    /**
     * Constructor
     *
     * @param userName UserName
     * @param password Password
     */
    public EisBackendPrincipal(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    /**
     * Setter method for UserName property
     * @param userName UserName
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Setter method for password property
     * @param password Password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Getter method for UserName property
     * @return UserName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Getter method for Password property
     * @return Password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Overloaded method from "Object" class
     * Checks the equality.
     * @param backendPrincipal Backend principal against which equality has to
     * @return true if they are equal
     *         false if hey are not equal.
     */
    public boolean equals(Object backendPrincipal) {

        if (backendPrincipal == null ||
                !(backendPrincipal instanceof EisBackendPrincipal)) {
            return false;
        }
        EisBackendPrincipal eisBackendPrincipal =
                (EisBackendPrincipal) backendPrincipal;

        if (isEqual(eisBackendPrincipal.userName, this.userName) &&
                isEqual(eisBackendPrincipal.password, this.password)) {
            return true;
        } else {
            return false;

        }
    }

    /**
     * Checks whether two strings are equal including the null string
     * cases.
     * @param first  first String
     * @param second second String
     * @return boolean equality status
     */
    private boolean isEqual(String first, String second) {
        if (first == null && second == null) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        return (second.equals(first));
    }

    /**
     * Overloaded method from "Object" class
     * Generates the hashcode
     * @return a hash code value for this object
     */
    public int hashCode() {
        int result = 67;
        if (userName != null)
            result = 67 * result + userName.hashCode();
        if (password != null)
            result = 67 * result + password.hashCode();
        return result;
    }
}
