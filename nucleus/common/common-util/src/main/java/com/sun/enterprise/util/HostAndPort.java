/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.util;

import java.util.StringTokenizer;

/**
 * Represents a host and a port in a convenient package that also
 * accepts a convenient constructor.
 */
public class HostAndPort {
    private final String host;
    private final int port;
    private final boolean secure;

    /**
     * Construct a HostAndPort object.
     *
     * @param   host    the host name
     * @param   port    the port number
     * @param   secure  does this host require a secure (SSL) connection?
     */
    public HostAndPort(String host, int port, boolean secure) {
        this.host = host;
        this.port = port;
        this.secure = secure;
    }

    public HostAndPort(HostAndPort rhs) {
        this(rhs.host, rhs.port, rhs.secure);
    }

    public HostAndPort(String host, int port) {
        this(host, port, false);
    }

    /**
     * Construct a new HostAndPort from a string of the form "host:port".
     *
     * @param  str string of the form "host:port"
     */
    public HostAndPort(String str) {
        StringTokenizer tokenizer = new StringTokenizer(str, ":", false);

        host = tokenizer.nextToken();

        final String portString = tokenizer.nextToken();
        port = Integer.parseInt(portString);
        secure = false;
    }

    public boolean isSecure() {
        return secure;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        if (port == 0) {
            return secure ? 8181 : 8080;    // default ports
        } else {
            return port;
        }
    }

    public String toString() {
        return host + ":" + port;
    }
}
