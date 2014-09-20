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
import com.sun.appserv.management.config.HTTPListenerConfig;
import com.sun.appserv.management.config.HTTPListenerConfigKeys;
import com.sun.appserv.management.config.HTTPServiceConfig;
import com.sun.appserv.management.config.PropertiesAccess;
import com.sun.appserv.management.config.SSLConfig;
import static com.sun.appserv.management.config.SSLConfigKeys.*;
import com.sun.appserv.management.util.misc.MapUtil;
import com.sun.appserv.management.util.misc.TypeCast;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 */
public final class HTTPListenerConfigTest
        extends ConfigMgrTestBase {
    static final String ADDRESS = "0.0.0.0";
    static final String DEF_VIRTUAL_SERVER = "server";
    static final String SERVER_NAME = "localhost";

    // !!! deliberately use old, incorrect form; it should still succeed
    static final Map<String, Object> OPTIONAL = new HashMap<String, Object>();

    static {
        OPTIONAL.put(PropertiesAccess.PROPERTY_PREFIX + "xyz", "abc");
        OPTIONAL.put(HTTPListenerConfigKeys.ENABLED_KEY, Boolean.FALSE);
        OPTIONAL.put(HTTPListenerConfigKeys.ACCEPTOR_THREADS_KEY, new Integer(4));
        //OPTIONAL.put( HTTPListenerConfigKeys.BLOCKING_ENABLED_KEY, "false" );
        //OPTIONAL.put( HTTPListenerConfigKeys.REDIRECT_PORT_KEY, "9081" );
        OPTIONAL.put(HTTPListenerConfigKeys.XPOWERED_BY_KEY, Boolean.TRUE);
        //OPTIONAL.put( HTTPListenerConfigKeys.FAMILY_KEY, HTTPListenerConfigFamilyValues.INET );
    }

    public HTTPListenerConfigTest() {
    }


    HTTPServiceConfig
    getHTTPServiceConfig() {
        return (getConfigConfig().getHTTPServiceConfig());
    }

    public void
    testGetHTTPListeners() {
        final HTTPServiceConfig httpService =
                getConfigConfig().getHTTPServiceConfig();

        final Map<String, HTTPListenerConfig> proxies = httpService.getHTTPListenerConfigMap();

        for (final String listenerName : proxies.keySet()) {
            final HTTPListenerConfig listener = (HTTPListenerConfig)
                    proxies.get(listenerName);

            listener.getEnabled();
        }
    }

    protected String
    getProgenyTestName() {
        return ("HTTPListenerConfigMgrTest-test-listener");
    }

    protected Container
    getProgenyContainer() {
        return getHTTPService();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.HTTP_LISTENER_CONFIG;
    }


    protected void
    removeProgeny(final String name) {
        getHTTPService().removeHTTPListenerConfig(name);
    }

    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> options) {
        // this is incorrect code-on purpose-to test backward compatibility with Maps
        // that aren't of type <String,String>
        final Map<String, String> optional = TypeCast.asMap(OPTIONAL);
        assert (!MapUtil.isAllStrings(optional));

        final Map<String, String> allOptions = MapUtil.newMap(options, optional);
        assert (!MapUtil.isAllStrings(allOptions));

        final int port = 31000 + (name.hashCode() % 31000);

        final HTTPListenerConfig config =
                getHTTPService().createHTTPListenerConfig(name,
                                                          ADDRESS, port, DEF_VIRTUAL_SERVER, SERVER_NAME, allOptions);

        return (config);
    }

    protected final HTTPServiceConfig
    getHTTPService() {
        return getConfigConfig().getHTTPServiceConfig();
    }


    public void
    testCreateSSL()
            throws Exception {
        final Map<String, String> options =
                Collections.unmodifiableMap(MapUtil.newMap(
                        new String[]
                                {
                                        CLIENT_AUTH_ENABLED_KEY, "false",
                                        SSL_2_ENABLED_KEY, "true",
                                        SSL_3_ENABLED_KEY, "true",
                                        SSL_2_CIPHERS_KEY, "+rc4,-rc4export,-rc2,-rc2export,+idea,+des,+desede3",
                                        SSL3_TLS_CIPHERS_KEY,
                                        "+rsa_rc4_128_md5,+rsa3des_sha,+rsa_des_sha,-rsa_rc4_40_md5" +
                                                "-rsa_rc2_40_md5,-rsa_null_md5,-rsa_des_56_sha,-rsa_rc4_56_sha",
                                        TLS_ENABLED_KEY, "true",
                                        TLS_ROLLBACK_ENABLED_KEY, "true",
                                }
                ));

        if (!checkNotOffline("testCreateSSL")) {
            return;
        }

        final String NAME = "HTTPListenerConfigMgr-listener-for-testCreateSSL";

        try {
            removeEx(NAME);
            final HTTPListenerConfig newListener =
                    (HTTPListenerConfig) createProgeny(NAME, null);
            assert newListener != null;

            // verify that the new listener is present
            final Map<String, HTTPListenerConfig> listeners =
                    getHTTPService().getHTTPListenerConfigMap();
            final HTTPListenerConfig listener = listeners.get(NAME);
            assert listener != null;
            assert listener == newListener;

            final String CERT_NICKNAME = NAME + "Cert";

            final SSLConfig ssl = listener.createSSLConfig(CERT_NICKNAME, options);
            assert ssl != null;
            assert ssl.getCertNickname().equals(CERT_NICKNAME);

            listener.removeSSLConfig();
        }
        finally {
            remove(NAME);
        }
    }
}


