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

/*
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/config/IIOPListenerConfigTest.java,v 1.6 2007/05/05 05:23:54 tcfujii Exp $
* $Revision: 1.6 $
* $Date: 2007/05/05 05:23:54 $
*/
package org.glassfish.admin.amxtest.config;

import com.sun.appserv.management.base.Container;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.AMXConfig;
import com.sun.appserv.management.config.IIOPListenerConfig;
import com.sun.appserv.management.config.IIOPListenerConfigKeys;
import com.sun.appserv.management.config.IIOPServiceConfig;
import com.sun.appserv.management.config.PropertiesAccess;
import com.sun.appserv.management.config.SSLConfig;
import com.sun.appserv.management.util.misc.MapUtil;

import javax.management.ObjectName;
import java.util.HashMap;
import java.util.Map;

/**
 */
public final class IIOPListenerConfigTest
        extends ConfigMgrTestBase {
    static final String ADDRESS = "0.0.0.0";
    static final Map<String, String> OPTIONAL = new HashMap<String, String>();

    static {
        OPTIONAL.put(PropertiesAccess.PROPERTY_PREFIX + "xyz", "abc");
        OPTIONAL.put(IIOPListenerConfigKeys.ENABLED_KEY, "false");
        OPTIONAL.put(IIOPListenerConfigKeys.SECURITY_ENABLED_KEY, "true");
    }

    public IIOPListenerConfigTest() {
    }

    protected Container
    getProgenyContainer() {
        return getIIOPService();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.IIOP_LISTENER_CONFIG;
    }


    protected void
    removeProgeny(final String name) {
        getIIOPService().removeIIOPListenerConfig(name);
    }


    protected final ObjectName
    create(String name) {
        return Util.getObjectName(createProgeny(name, null));
    }

    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> options) {
        final Map<String, String> allOptions = MapUtil.newMap(options, OPTIONAL);

        final int port = (name.hashCode() % 32000) + 32000;
        allOptions.put(IIOPListenerConfigKeys.PORT_KEY, "" + port);

        return getIIOPService().createIIOPListenerConfig(name, ADDRESS, allOptions);
    }

    protected final IIOPServiceConfig
    getIIOPService() {
        return getConfigConfig().getIIOPServiceConfig();
    }

    public void
    testCreateSSL()
            throws Exception {
        if (!checkNotOffline("testCreateSSL")) {
            return;
        }

        final String NAME = "IIOPListenerConfigMgr-testCreateSSL";

        removeEx(NAME);

        final IIOPListenerConfig newListener =
                (IIOPListenerConfig) createProgeny(NAME, null);

        try {
            final Map<String, IIOPListenerConfig> listeners =
                    getIIOPService().getIIOPListenerConfigMap();

            final IIOPListenerConfig listener =
                    (IIOPListenerConfig) listeners.get(NAME);
            assert listener != null;
            assert listener == newListener;

            final String CERT_NICKNAME = NAME + "Cert";

            final SSLConfig ssl = listener.createSSLConfig(CERT_NICKNAME, null);
            assert ssl != null;
            assert ssl.getCertNickname().equals(CERT_NICKNAME);

            listener.removeSSLConfig();
        }
        finally {
            remove(NAME);
        }
    }
/*
		public void
	testCreateSSLClientConfig()
		throws Exception
	{
		final Set<IIOPServiceConfig> s = getQueryMgr().getJ2EETypeProxies("X-IIOPServiceConfig");
		assert s.size() >= 0;
		IIOPServiceConfig iiopService = (IIOPServiceConfig)s.iterator().next();
		assert iiopService != null;
		Map sslParams = new HashMap();
		sslParams.put("CertNickname", "mycert");
		final ObjectName on = iiopService.createIIOPSSLClientConfig(sslParams);
		assert on != null && on.equals(iiopService.getIIOPSSLClientConfigObjectName());
		IIOPSSLClientConfig sslClientConfig = iiopService.getIIOPSSLClientConfig();
		assert sslClientConfig != null;
	}
 */
}


