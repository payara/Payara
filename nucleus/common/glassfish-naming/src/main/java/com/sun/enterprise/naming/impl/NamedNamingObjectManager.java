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
 */

package com.sun.enterprise.naming.impl;

import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.jvnet.hk2.component.Habitat;

import javax.naming.NamingException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Mahesh Kannan
 *         Date: Mar 4, 2008
 */
public class NamedNamingObjectManager {

    private static AtomicReference gotAllNamedProxies
            = new AtomicReference();

    private static List<NamedNamingObjectProxy> proxies = new ArrayList<NamedNamingObjectProxy>();


    public static void checkAndLoadProxies(Habitat habitat)
        throws NamingException {
        if (gotAllNamedProxies.get() != habitat) {
            if (habitat != null) {
                synchronized (gotAllNamedProxies) {
                    if (gotAllNamedProxies.get() != habitat) {
                        proxies.clear();
                        GlassfishNamingManager nm =
                                habitat.getByContract(GlassfishNamingManager.class);
                        for (NamedNamingObjectProxy proxy : habitat.getAllByContract(NamedNamingObjectProxy.class)) {
                            //System.out.println("Got NamedNamingObjectProxy: " + proxy.getClass().getName());
                            proxies.add(proxy);
                        }
                        gotAllNamedProxies.set(habitat);
                    }
                }
            }
        }
    }

    public static Object tryNamedProxies(String name)
        throws NamingException {

        Object obj = null;
        for (NamedNamingObjectProxy proxy : proxies) {
            obj = proxy.handle(name);
            if (obj != null) {
                break;
            }
        }

        return obj;
    }
}
