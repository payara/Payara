/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amxtest.config;

import com.sun.appserv.management.base.Container;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.AMXConfig;
import com.sun.appserv.management.config.ClusterConfig;
import com.sun.appserv.management.config.DomainConfig;
import org.glassfish.admin.amxtest.ClusterSupportRequired;

import java.util.Map;

/**
 Test the creation/removal of ClusterConfig
 */
public final class ClusterConfigTest
        extends ConfigMgrTestBase
        implements ClusterSupportRequired {
    public ClusterConfigTest() {
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getDomainConfig());
        }
    }

    public static String
    getDefaultInstanceName() {
        return getDefaultInstanceName("test-cluster");
    }

    public static ClusterConfig
    ensureDefaultInstance(final DomainConfig domainConfig) {
        ClusterConfig result =
                domainConfig.getClustersConfig().getClusterConfigMap().get(getDefaultInstanceName());

        if (result == null) {
            result = domainConfig.getClustersConfig().createClusterConfig(
                    getDefaultInstanceName(), null, null);
        }

        return result;
    }

    protected Container
    getProgenyContainer() {
        return getDomainConfig();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.CLUSTER_CONFIG;
    }


    protected void
    removeProgeny(final String name) {
        getDomainConfig().getClustersConfig().removeClusterConfig(name);
    }


    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> options) {
        assert (name != null && name.length() >= 1);
        return getDomainConfig().getClustersConfig().createClusterConfig(name, null, options);
    }

    public void
    testCreateWithIllegalConfig() {
        try {
            getDomainConfig().getClustersConfig().createClusterConfig("dummy-cluster-1", "no such config", null);
            failure("expecting failure creating cluster with illegal config name");

            getDomainConfig().getClustersConfig().createClusterConfig("dummy-cluster-2", "server-config", null);
            failure("expecting failure creating cluster with name 'server-config'");

            getDomainConfig().getClustersConfig().createClusterConfig("dummy-cluster-2", "default-config", null);
            failure("expecting failure creating name 'default-config'");
        }
        catch (Exception e) {
            // good
        }
    }
}








