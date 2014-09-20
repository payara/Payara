/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.connectors.jms.system;

import javax.jms.ConnectionFactory;
import javax.naming.NamingException;
import org.glassfish.api.naming.DefaultResourceProxy;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.glassfish.api.naming.NamespacePrefixes;
import org.jvnet.hk2.annotations.Service;
import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.enterprise.connectors.ConnectorRuntime;

/**
 * Naming Object Proxy to handle the Default JMS Connection Factory.
 * Maps to a pre-configured jms connectionFactory, when binding for
 * a jms connectionFactory reference is absent in the @Resource annotation.
 *
 * @author David Zhao
 */
@Service
@NamespacePrefixes({DefaultJMSConnectionFactory.DEFAULT_CF})
public class DefaultJMSConnectionFactory implements NamedNamingObjectProxy, DefaultResourceProxy {
    static final String DEFAULT_CF = "java:comp/DefaultJMSConnectionFactory";
    static final String DEFAULT_CF_PHYS = "jms/__defaultConnectionFactory";
    private ConnectionFactory connectionFactory;
    private ConnectionFactory connectionFactoryPM;

    @Override
    public Object handle(String name) throws NamingException {
        ConnectionFactory cachedCF = null;
        boolean isCFPM = false;
        if (name != null && name.endsWith(ConnectorConstants.PM_JNDI_SUFFIX)) {
            cachedCF = connectionFactoryPM;
            isCFPM = true;
        } else {
            cachedCF = connectionFactory;
        }
        if(cachedCF == null) {
            javax.naming.Context ctx = new javax.naming.InitialContext();
            if (isCFPM) {
                ConnectorRuntime connectorRuntime = ConnectorRuntime.getRuntime();
                cachedCF = (ConnectionFactory) connectorRuntime.lookupPMResource(DEFAULT_CF_PHYS, false);
                connectionFactoryPM = cachedCF;
            } else {
                cachedCF = (ConnectionFactory) ctx.lookup(DEFAULT_CF_PHYS);
                connectionFactory = cachedCF;
            }
        }
        return cachedCF;
    }

    @Override
    public String getPhysicalName() {
        return DEFAULT_CF_PHYS;
    }

    @Override
    public String getLogicalName() {
        return DEFAULT_CF;
    }
}
