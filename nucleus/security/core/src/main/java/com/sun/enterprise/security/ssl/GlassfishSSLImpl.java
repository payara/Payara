/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.ssl;

import java.net.Socket;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import org.glassfish.grizzly.config.ssl.JSSE14SocketFactory;
import org.glassfish.grizzly.config.ssl.SSLImplementation;
import org.glassfish.grizzly.config.ssl.ServerSocketFactory;
import org.glassfish.grizzly.ssl.SSLSupport;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Sudarsan Sridhar
 */
@Service(name="com.sun.enterprise.security.ssl.GlassfishSSLImpl")
@ContractsProvided({GlassfishSSLImpl.class, SSLImplementation.class})
public class GlassfishSSLImpl extends SSLImplementation {
    public static final String PROP_GLASSFISH_SOCKETFACTORY =
            "fish.payara.ssl.GlassfishServerSocketFactory";

    public GlassfishSSLImpl() {
    }

    public String getImplementationName() {
        return "Glassfish";
    }

    public ServerSocketFactory getServerSocketFactory() {
        if(Boolean.valueOf(System.getProperty(PROP_GLASSFISH_SOCKETFACTORY, "false"))) {
            return new GlassfishServerSocketFactory();
        } else {
            // Fixes Payara-499, Payara-2613 and 1047
            return new JSSE14SocketFactory();
        }
    }

    public SSLSupport getSSLSupport(Socket socket) {
        if(socket instanceof SSLSocket) {
            return new GlassfishSSLSupport((SSLSocket)socket);
        }
        return null;
    }

    public SSLSupport getSSLSupport(SSLEngine ssle) {
        return new GlassfishSSLSupport(ssle);
    }

}
