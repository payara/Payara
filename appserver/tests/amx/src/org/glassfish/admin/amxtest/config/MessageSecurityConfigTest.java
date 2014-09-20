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

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.MessageSecurityConfig;
import com.sun.appserv.management.config.ProviderConfig;
import com.sun.appserv.management.config.SecurityServiceConfig;
import org.glassfish.admin.amxtest.AMXTestBase;

import java.util.HashMap;
import java.util.Map;


/**
 */
public final class MessageSecurityConfigTest
        extends AMXTestBase {
    public MessageSecurityConfigTest() {
    }

    private static SecurityServiceConfig
    getDefaultSecurityServiceConfig(final DomainRoot domainRoot) {
        final ConfigConfig config = ConfigConfigTest.ensureDefaultInstance(domainRoot);
        final SecurityServiceConfig ss = config.getSecurityServiceConfig();
        assert (ss != null);
        return ss;
    }

    private static MessageSecurityConfig
    create(
            final DomainRoot domainRoot,
            final String authLayer) {
        final SecurityServiceConfig ss = getDefaultSecurityServiceConfig(domainRoot);

        final Map<String, String> optional = new HashMap<String, String>();
        final MessageSecurityConfig msc = ss.createMessageSecurityConfig(authLayer,
                                                                         "ClientProvider", ProviderConfig.PROVIDER_TYPE_CLIENT,
                                                                         "com.sun.xml.wss.provider.ClientSecurityAuthModul", optional);

        msc.createProviderConfig("ServerProvider",
                                 ProviderConfig.PROVIDER_TYPE_SERVER, "com.sun.xml.wss.provider.ServerSecurityAuthModule", optional);

        msc.createProviderConfig("DummyProvider1",
                                 ProviderConfig.PROVIDER_TYPE_SERVER, "AMX.TEST.DummySecurityAuthModule", optional);

        msc.createProviderConfig("DummyProvider2",
                                 ProviderConfig.PROVIDER_TYPE_SERVER, "AMX.TEST.DummySecurityAuthModule", optional);

        msc.removeProviderConfig("DummyProvider1");
        msc.removeProviderConfig("DummyProvider2");

        return msc;
    }

    static private final String AUTH_TYPE = MessageSecurityConfig.AUTH_LAYER_HTTP_SERVLET;

    /**
     Note: this can't be tested except by making a new one, and the names are predefined, so
     if it already exists, it must be deleted first.
     */
    public void
    testCreateRemove() {
        final SecurityServiceConfig ss = getDefaultSecurityServiceConfig(getDomainRoot());
        final Map<String, MessageSecurityConfig> messageSecurityConfigs = ss.getMessageSecurityConfigMap();
        MessageSecurityConfig msc = messageSecurityConfigs.get(AUTH_TYPE);

        if (msc != null) {
            ss.removeMessageSecurityConfig(AUTH_TYPE);
            msc = null;
        }

        msc = create(getDomainRoot(), AUTH_TYPE);

        ss.removeMessageSecurityConfig(AUTH_TYPE);
    }

}



























