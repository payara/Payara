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

package com.sun.enterprise.naming.util;

import com.sun.enterprise.naming.spi.NamingObjectFactory;
import org.glassfish.api.naming.NamingObjectProxy;
import org.jvnet.hk2.annotations.Service;

import javax.naming.Context;
import javax.naming.NamingException;
import java.io.*;

/**
 * <p>A naming object factory that is used by resource-references
 * of type JDBC/JMS/Connector/Mail so that Application Client
 * Container based lookup of these references will result in
 * the server returning the "Reference" instead of the actual
 * object since the actual object will not work in the application
 * client container.</p>
 * <p> By virtue of implementing NamingObjectProxy.InitializationNamingObjectProxy,
 * above requirement of returning "Reference" is achieved. Refer other
 * implementations like org.glassfish.resourcebase.resources.api.ResourceProxy and
 * org.glassfish.javaee.services.CommonResourceProxy</p>
 *
 */
@Service
public class JndiInitializationNamingObjectFactory implements NamingObjectFactory,
        NamingObjectProxy.InitializationNamingObjectProxy, Serializable{

    private String name;
    private String jndiName;
    private boolean cacheResult;

    private transient JndiNamingObjectFactory jndiNamingObjectFactory ;

    public JndiInitializationNamingObjectFactory() {
        //need a no-org constructor since it's serializable.
    }

    public JndiInitializationNamingObjectFactory(String name, String jndiName, boolean cacheResult) {
        this.name = name;
        this.jndiName = jndiName;
        this.cacheResult = cacheResult;
        //couldn't make JndiInitializationNamingObjectFactory simply extend JndiNaminObjectFactory
        //since serialization/de-serialization requires no-arg constructor for super classes too.
        jndiNamingObjectFactory = new JndiNamingObjectFactory(name, jndiName, cacheResult);
    }

    /**
     * @inheritDoc
     */
    public boolean isCreateResultCacheable() {
        return getJndiNamingObjectFactory().isCreateResultCacheable();
    }

    /**
     * re-construct JndiNamingObjectFactory in case it is null (due to de-serialization)
     * @return JndiNamingObjectFactory
     */
    private JndiNamingObjectFactory getJndiNamingObjectFactory() {

        if(jndiNamingObjectFactory == null){
            jndiNamingObjectFactory = new JndiNamingObjectFactory(name, jndiName, cacheResult);
        }
        return jndiNamingObjectFactory;
    }

    /**
     * @inheritDoc
     */
    public Object create(Context ic) throws NamingException {
        return  getJndiNamingObjectFactory().create(ic);
    }
}
