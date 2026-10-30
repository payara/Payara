/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2023 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.nucleus.hazelcast;

import com.hazelcast.spi.tenantcontrol.TenantControl;
import com.hazelcast.spi.tenantcontrol.TenantControlFactory;
import java.util.function.Supplier;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.JavaEEContextUtil;

/**
 * Java EE Context and class loading support for Hazelcast objects and thread-based callbacks
 *
 * @author lprimak
 */
public class PayaraHazelcastTenantFactory implements TenantControlFactory {
    private static final String DISABLE_BLOCKING_PROPERTY = "fish.payara.tenantcontrol.blocking.disable";
    private static final Supplier<Boolean> getDisableBlockingProperty =
            () -> Boolean.parseBoolean(System.getProperty(DISABLE_BLOCKING_PROPERTY, Boolean.TRUE.toString()));

    private final JavaEEContextUtil ctxUtil = Globals.getDefaultHabitat().getService(JavaEEContextUtil.class);
    private final InvocationManager invocationMgr = Globals.getDefaultHabitat().getService(InvocationManager.class);

    static boolean blockingDisabled = getDisableBlockingProperty.get();

    @Override
    public TenantControl saveCurrentTenant() {
        ComponentInvocation invocation = invocationMgr.getCurrentInvocation();
        TenantControl tenantControl = TenantControl.NOOP_TENANT_CONTROL;
        if (invocation != null) {
            tenantControl = invocation.getRegistryFor(TenantControl.class);
            if (tenantControl == null && ctxUtil.isInvocationLoaded()) {
                blockingDisabled = getDisableBlockingProperty.get();
                tenantControl = new PayaraHazelcastTenant();
                invocation.setRegistryFor(TenantControl.class, tenantControl);
            } else if (tenantControl == null) {
                tenantControl = TenantControl.NOOP_TENANT_CONTROL;
            }
        }
        return tenantControl;
    }

    @Override
    public boolean isClassesAlwaysAvailable() {
        return false;
    }
}
