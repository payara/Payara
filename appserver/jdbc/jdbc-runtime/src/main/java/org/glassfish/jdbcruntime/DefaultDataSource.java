/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jdbcruntime;

import javax.naming.NamingException;
import javax.sql.DataSource;
import org.glassfish.api.naming.DefaultResourceProxy;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.glassfish.api.naming.NamespacePrefixes;
import org.jvnet.hk2.annotations.Service;

/**
 * Naming Object Proxy to handle the Default Data Source.
 * Maps to a pre-configured data source, when binding for
 * a datasource reference is absent in the @Resource annotation.
 *
 * @author Shalini M
 */
@Service
@NamespacePrefixes({DefaultDataSource.DEFAULT_DATASOURCE})
public class DefaultDataSource implements NamedNamingObjectProxy, DefaultResourceProxy {

    static final String DEFAULT_DATASOURCE = "java:comp/DefaultDataSource";
    static final String DEFAULT_DATASOURCE_PHYS = "jdbc/__default";
    private DataSource dataSource;

    @Override
    public Object handle(String name) throws NamingException {
        if(dataSource == null) {
            javax.naming.Context ctx = new javax.naming.InitialContext();
            // cache the datasource to avoid JNDI lookup overheads
            dataSource = (DataSource)ctx.lookup(DEFAULT_DATASOURCE_PHYS);
        }
        return dataSource;
    }

    @Override
    public String getPhysicalName() {
        return DEFAULT_DATASOURCE_PHYS;
    }

    @Override
    public String getLogicalName() {
        return DEFAULT_DATASOURCE;
    }
}
