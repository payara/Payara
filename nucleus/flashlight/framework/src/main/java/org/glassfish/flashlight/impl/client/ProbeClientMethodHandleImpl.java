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
package org.glassfish.flashlight.impl.client;

import org.glassfish.flashlight.client.ProbeClientInvoker;
import org.glassfish.flashlight.client.ProbeClientMethodHandle;
import org.glassfish.flashlight.provider.FlashlightProbe;

/**
 * @author Mahesh Kannan
 * @author Byron Nevins
 */
public class ProbeClientMethodHandleImpl
        implements ProbeClientMethodHandle {

    private final int clientMethodId;
    private final ProbeClientInvoker clientMethodInvoker;
    private final FlashlightProbe probe;
    private boolean enabled = true;

    public ProbeClientMethodHandleImpl(int id, ProbeClientInvoker invoker, FlashlightProbe probe) {
        this.clientMethodId = id;
        this.clientMethodInvoker = invoker;
        this.probe = probe;
    }

    @Override
    public int getId() {
        return clientMethodId;
    }

    @Override
    public synchronized boolean isEnabled() {
        return enabled;
    }

    @Override
    public synchronized void enable() {
        probe.addInvoker(clientMethodInvoker);
        enabled = true;
        ProbeProviderClassFileTransformer.update(probe.getProviderClazz());
    }

    @Override
    public synchronized void disable() {
        probe.removeInvoker(clientMethodInvoker);
        enabled = false;
        ProbeProviderClassFileTransformer.update(probe.getProviderClazz());
    }
}
