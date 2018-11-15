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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]

package com.sun.ejb.containers;

import org.glassfish.api.naming.NamespacePrefixes;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.glassfish.api.invocation.ComponentInvocation;

import com.sun.ejb.EjbInvocation;

import org.jvnet.hk2.annotations.Service;

import javax.naming.NamingException;

/**
 * Proxy for creating TimerService instances when requested by lookup or injection.
 * NamingManager will call the handle() method when the JNDI name is looked up.
 * Will return the instance that corresponds to the known name.
 *
 * @author Marina Vatkina
 */
@Service
@NamespacePrefixes(TimerServiceNamingProxy.EJB_TIMER_SERVICE)
public class TimerServiceNamingProxy 
        implements NamedNamingObjectProxy {

    static final String EJB_TIMER_SERVICE = "java:comp/TimerService";

    @Override
    public Object handle(String name) throws NamingException {

        if (EJB_TIMER_SERVICE.equals(name)) {
            return getTimerServiceWrapper();
        }
        return null;
    }

    private Object getTimerServiceWrapper() {

        // Cannot store EjbContainerUtilImpl.getInstance() in an instance 
        // variable because it shouldn't be accessed before EJB container 
        // is initialized. 
        // NamedNamingObjectProxy is initialized on the first lookup.

        ComponentInvocation currentInv = 
                EjbContainerUtilImpl.getInstance().getCurrentInvocation();

        if (currentInv == null) {
            throw new IllegalStateException("no current invocation");
        } else if (currentInv.getInvocationType()
                != ComponentInvocation.ComponentInvocationType.EJB_INVOCATION) {
            throw new IllegalStateException("Illegal invocation type for EJB Context : "
                    + currentInv.getInvocationType());
        }

        return EJBTimerService.getEJBTimerServiceWrapper(
                (EJBContextImpl) ((EjbInvocation) currentInv).context
        );
    }
}
