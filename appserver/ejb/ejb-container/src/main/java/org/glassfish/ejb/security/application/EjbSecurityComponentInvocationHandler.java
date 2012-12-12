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

package org.glassfish.ejb.security.application;

import com.sun.ejb.EjbInvocation;
import com.sun.logging.LogDomains;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType;
import org.glassfish.api.invocation.ComponentInvocationHandler;
import org.glassfish.api.invocation.RegisteredComponentInvocationHandler;
import org.glassfish.api.invocation.InvocationException;
import org.glassfish.api.invocation.InvocationManager;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

@Service(name="ejbSecurityCIH")
@Singleton
public class EjbSecurityComponentInvocationHandler implements  RegisteredComponentInvocationHandler {

    private static final Logger _logger =
            LogDomains.getLogger(EjbSecurityComponentInvocationHandler.class, LogDomains.EJB_LOGGER);

    @Inject
    private InvocationManager invManager;

    private ComponentInvocationHandler ejbSecurityCompInvHandler = new ComponentInvocationHandler() {

        public void beforePreInvoke(ComponentInvocationType invType,
                ComponentInvocation prevInv, ComponentInvocation newInv) throws InvocationException {
            if (invType == ComponentInvocationType.EJB_INVOCATION) {
                assert (newInv instanceof EjbInvocation);
                try {
                    if (!newInv.isPreInvokeDone()) {
                        ((EjbInvocation) newInv).getEjbSecurityManager().preInvoke(newInv);
                    }
                } catch (Exception ex) {
                    _logger.log(Level.SEVERE, "ejb.security_preinvoke_exception",ex);
                    throw new InvocationException(ex);
                }
            }
        }

        public void afterPreInvoke(ComponentInvocationType invType,
                ComponentInvocation prevInv, ComponentInvocation curInv) throws InvocationException {
        }

        public void beforePostInvoke(ComponentInvocationType invType,
                ComponentInvocation prevInv, ComponentInvocation curInv) throws InvocationException {
        }

        public void afterPostInvoke(ComponentInvocationType invType,
                ComponentInvocation prevInv, ComponentInvocation curInv) throws InvocationException {
            if (invType == ComponentInvocationType.EJB_INVOCATION) {
                assert (curInv instanceof EjbInvocation);
                try {
                    ((EjbInvocation) curInv).getEjbSecurityManager().postInvoke(curInv);
                } catch (Exception ex) {
                    _logger.log(Level.SEVERE, "ejb.security_postinvoke_exception", ex);
                    ((EjbInvocation) curInv).exception = ex;
                }
            }
        }
    };

    public ComponentInvocationHandler getComponentInvocationHandler() {
        return ejbSecurityCompInvHandler;
    }

    public void register() {
        invManager.registerComponentInvocationHandler(ComponentInvocationType.EJB_INVOCATION, this);
    }

}
