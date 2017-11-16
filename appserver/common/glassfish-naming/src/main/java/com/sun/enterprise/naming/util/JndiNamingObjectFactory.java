/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright [2017] Payara Foundation and/or affiliates
 */

package com.sun.enterprise.naming.util;

import com.sun.enterprise.naming.spi.NamingObjectFactory;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.jvnet.hk2.annotations.Service;

import javax.naming.Context;
import javax.naming.NamingException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory for creating Objects with a given jndi name
 * <p>
 * This factory creates objects by looking them up in the given {@link Context}
 */
@Service
public class JndiNamingObjectFactory
    implements NamingObjectFactory {

    private String name;

    private String jndiName;

    private AtomicReference value;

    private boolean cacheResult;

    /**
     * 
     * @param name the name of the object; equivalent to the name value of a {@link javax.annotation.Resource} annotation
     * @param jndiName the jndi name to lookup to get the object; equivalent to the lookup value of a {@link javax.annotation.Resource} annotation
     * @param cacheResult whether the result may have been cached
     */
    public JndiNamingObjectFactory(String name, String jndiName, boolean cacheResult) {
        this.name = name;
        this.jndiName = jndiName;
        this.cacheResult = cacheResult;
        this.value = new AtomicReference();       
    }

    @Override
    public boolean isCreateResultCacheable() {
        return cacheResult;
    }

    @Override
    public Object create(Context ic)
            throws NamingException {
        Object result = null;
        try {
            ic.addToEnvironment(GlassfishNamingManager.LOGICAL_NAME, name);
            if (cacheResult) {
                result = value.get();
                if (result == null) {
                    Object tempResult = ic.lookup(jndiName);
                    if (value.compareAndSet(null, tempResult)) {
                        result = tempResult;
                    } else {
                        result = value.get();
                    }
                }
            } else {
                result = ic.lookup(jndiName);
            }
        } finally {
            ic.removeFromEnvironment(GlassfishNamingManager.LOGICAL_NAME);
        }

        return result;
    }

}
