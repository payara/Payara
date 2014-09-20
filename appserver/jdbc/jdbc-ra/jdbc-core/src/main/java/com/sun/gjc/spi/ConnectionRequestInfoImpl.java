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

package com.sun.gjc.spi;

/**
 * ConnectionRequestInfo implementation for Generic JDBC Connector.
 *
 * @author Binod P.G
 * @version 1.0, 02/07/31
 */
public class ConnectionRequestInfoImpl implements javax.resource.spi.ConnectionRequestInfo {

    private String user;
    private String password;

    /**
     * Constructs a new <code>ConnectionRequestInfoImpl</code> object
     *
     * @param user     User Name.
     * @param password Password
     */
    public ConnectionRequestInfoImpl(String user, String password) {
        this.user = user;
        this.password = password;
    }

    /**
     * Retrieves the user name of the ConnectionRequestInfo.
     *
     * @return User name of ConnectionRequestInfo.
     */
    public String getUser() {
        return user;
    }

    /**
     * Retrieves the password of the ConnectionRequestInfo.
     *
     * @return Password of ConnectionRequestInfo.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Verify whether two ConnectionRequestInfoImpls are equal.
     *
     * @return True, if they are equal and false otherwise.
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj instanceof ConnectionRequestInfoImpl) {
            ConnectionRequestInfoImpl other = (ConnectionRequestInfoImpl) obj;
            return (isEqual(this.user, other.user) &&
                    isEqual(this.password, other.password));
        } else {
            return false;
        }
    }

    /**
     * Retrieves the hashcode of the object.
     *
     * @return hashCode.
     */
    public int hashCode() {
        String result = "" + user + password;
        return result.hashCode();
    }

    /**
     * Compares two objects.
     *
     * @param o1 First object.
     * @param o2 Second object.
     */
    private boolean isEqual(Object o1, Object o2) {
        if (o1 == null) {
            return (o2 == null);
        } else {
            return o1.equals(o2);
        }
    }

}
