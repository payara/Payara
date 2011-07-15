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

package com.sun.appserv.management.client.prefs;

/**
 * An immutable class that represents an arbitrary LoginInfo for Appserver Administration Client. A LoginInfo
 * is specific to an admin host and admin port. Thus, with this scheme, there can be
 * at the most one LoginInfo for an operating system user of Appserver, for a given admin host
 * and admin port.
 * @since Appserver 9.0
 */
public final class LoginInfo implements Comparable<LoginInfo> {
    private String host;
    private int    port;
    private String user;
    private String password;
    
    /**
     * Creates an Immutable instance of a LoginInfo from given 4-tuple. 
     * The host, user and password may not be null.
     * The port may not be a negative integer.
     * @param host String representing host
     * @param port integer representing port
     * @param user String representing user
     * @param password String representing password
     * @throws IllegalArgumentException if parameter contract is violated
     */
    public LoginInfo(final String host, final int port, final String user, final String password) {
        if (host == null || port < 0 || user == null || password == null)
            throw new IllegalArgumentException("null value"); // TODO
        init(host, port, user, password);
    }
    public String getHost() {
        return ( host );
    }
    public int getPort() {
        return ( port );
    }
    public String getUser() {
        return ( user );
    }
    public String getPassword() {
        return ( password );
    }
    public boolean equals(final Object other) {
        boolean same = false;
        if (other instanceof LoginInfo) {
            final LoginInfo that = (LoginInfo) other;
            same = this.host.equals(that.host) &&
                   this.port == that.port      &&
                   this.user.equals(that.user) &&
                   this.password.equals(that.password);
        }
        return ( same );
    }
    public int hashCode() {
        return ( (int) 31 * host.hashCode() + 23 * port + 53 * user.hashCode() + 13 * password.hashCode() );
    }
    
    private void init(final String host, final int port, final String user, final String password) {
        this.host     = host;
        this.port     = port;
        this.user     = user;
        this.password = password;
    }
    
    public String toString() {
        return ( host + port + user + password );
    }

    public int compareTo(final LoginInfo that) {
        final String thisKey = this.user + this.host + this.port;
        final String thatKey = that.user + that.host + that.port;        
        return ( thisKey.compareTo(thatKey) );
    }
}
