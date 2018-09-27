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
// Portions Copyright [2016] [Payara Foundation]

package com.sun.web.security;

import org.apache.catalina.Realm;
import org.apache.catalina.core.ContainerBase;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType;
import org.glassfish.api.invocation.ComponentInvocationHandler;
import org.glassfish.api.invocation.RegisteredComponentInvocationHandler;
import org.glassfish.api.invocation.InvocationException;
import org.glassfish.api.invocation.InvocationManager;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION;

import javax.inject.Inject;

@Service(name = "webSecurityCIH")
@Singleton
public class WebSecurityComponentInvocationHandler implements RegisteredComponentInvocationHandler {

    @Inject
    private InvocationManager invManager;

    private ComponentInvocationHandler webSecurityCompInvHandler = new ComponentInvocationHandler() {

        @Override
        public void beforePreInvoke(ComponentInvocationType invType, ComponentInvocation prevInv, ComponentInvocation newInv)
                throws InvocationException {
            if (invType == SERVLET_INVOCATION) {
                Object cont = newInv.getContainer();
                if (cont instanceof ContainerBase) {
                    Realm realm = ((ContainerBase) cont).getRealm();
                    if (realm instanceof RealmAdapter) {
                        ((RealmAdapter) realm).preSetRunAsIdentity(newInv);
                    }
                }
            }
        }

        @Override
        public void afterPreInvoke(ComponentInvocationType invType, ComponentInvocation prevInv, ComponentInvocation curInv)
                throws InvocationException {
        }

        @Override
        public void beforePostInvoke(ComponentInvocationType invType, ComponentInvocation prevInv, ComponentInvocation curInv)
                throws InvocationException {
        }

        @Override
        public void afterPostInvoke(ComponentInvocationType invType, ComponentInvocation prevInv, ComponentInvocation curInv)
                throws InvocationException {
            if (invType == SERVLET_INVOCATION) {
                Object cont = curInv.getContainer();
                if (cont instanceof ContainerBase) {
                    Realm realm = ((ContainerBase) cont).getRealm();
                    if (realm instanceof RealmAdapter) {
                        ((RealmAdapter) realm).postSetRunAsIdentity(curInv);
                    }
                }
            }
        }
    };

    @Override
    public ComponentInvocationHandler getComponentInvocationHandler() {
        return webSecurityCompInvHandler;
    }

    @Override
    public void register() {
        invManager.registerComponentInvocationHandler(SERVLET_INVOCATION, this);
    }

}
