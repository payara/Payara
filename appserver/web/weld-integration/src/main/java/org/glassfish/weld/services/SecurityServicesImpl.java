/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package org.glassfish.weld.services;

import com.sun.enterprise.security.SecurityContext;
import java.security.Principal;
import java.util.function.Consumer;

import org.jboss.weld.security.spi.SecurityServices;

public class SecurityServicesImpl implements SecurityServices {

    @Override
    public Principal getPrincipal() {
        return SecurityContext.getCurrent().getCallerPrincipal();
    }

    @Override
    public void cleanup() {}

    @Override
    public org.jboss.weld.security.spi.SecurityContext getSecurityContext() {
        return new SecurityContextImpl();
    }

    static class SecurityContextImpl implements org.jboss.weld.security.spi.SecurityContext {

        private final SecurityContext myContext;
        private static ThreadLocal<SecurityContext> oldContext = new ThreadLocal<>();

        private SecurityContextImpl() {
            this.myContext = SecurityContext.getCurrent();
        }

        @Override
        public void associate() {
            if (oldContext.get() == null) {
                oldContext.set(SecurityContext.getCurrent());
            } else {
                throw new IllegalStateException("Security context is already associated");
            }
            SecurityContext.setCurrent(myContext);
        }

        @Override
        public void dissociate() {
            SecurityContext.setCurrent(oldContext.get());
            oldContext.remove();
        }

        @Override
        public void close() {

        }
    }
}

