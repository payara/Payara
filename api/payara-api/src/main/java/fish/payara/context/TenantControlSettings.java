/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.context;

import java.util.Set;

/**
 * control settings of Hazelcast tenant control
 * Sometimes it's necessary to alter default behavior of Tenant Control, i.e.
 * when wanting to fail fast instead of block until tenant becomes available.
 * Also, allows to filter classes by 'instanceof' that are considered available
 * no matter what.
 *
 * Example:
 * @Inject TenantControlSettings tcs;
 * tcs.getDisabledTenants().add("my-application-SNAPSHOT-1.0");
 *
 * @author lprimak
 */
public interface TenantControlSettings {
    /**
     * allows adding / removing / getting tenants that are considered always available,
     * i.e. tenant control availability function is disabled for these tenants.
     * tenant is the name of the applications, i.e. --name argument to asadmin
     * or 'name' in Admin console.
     * Tenants that are disabled here will fail if they are not loaded.
     *
     * @return mutable set of disabled tenants
     */
    Set<String> getDisabledTenants();
}
