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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.glassfish.deployment.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class defines the additional environment needed to
 * connect to a particular application server deployment
 * backend.  For now, the properties supported are specific
 * to jmx https connector.  We will extend it to support
 * jmx rmi connector when the jmx implementation is ready.
 * <br>
 * Environment supported in this class are defined by the
 * JMX connectors.
 * @see also com.sun.enterprise.admin.jmx.remote.DefaultConfiguration
 * <br>
 * For example, to set a client trust manager, the key of env shall
 * be DefaultConfiguration.TRUST_MANAGER_PROPERTY_NAME.
 *
 * @author Qingqing Ouyang
 */
public final class ServerConnectionEnvironment extends HashMap {

    /**
     * Creates a new instance of ServerConnectionEnvironment
     */
    public ServerConnectionEnvironment() {
        super();
    }

    /**
     * Creates a new instance of ServerConnectionEnvironment
     */
    public ServerConnectionEnvironment(Map env) {
        super(env);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("ServerConnectionEnvironments: \n");
        Iterator entries = entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            buf.append("Key = " + entry.getKey());
            buf.append("    ");
            buf.append("Value = " + entry.getValue());
            buf.append(";\n");
        }

        return buf.toString();
    }
}
