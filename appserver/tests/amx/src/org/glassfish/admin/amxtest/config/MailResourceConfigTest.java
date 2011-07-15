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
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/config/MailResourceConfigTest.java,v 1.9 2007/05/05 05:23:55 tcfujii Exp $
* $Revision: 1.9 $
* $Date: 2007/05/05 05:23:55 $
*/
package org.glassfish.admin.amxtest.config;

import com.sun.appserv.management.base.Container;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.AMXConfig;
import com.sun.appserv.management.config.DomainConfig;
import com.sun.appserv.management.config.MailResourceConfig;
import com.sun.appserv.management.config.ResourceConfigKeys;
import com.sun.appserv.management.config.ResourceRefConfig;
import com.sun.appserv.management.config.StandaloneServerConfig;
import com.sun.appserv.management.helper.RefHelper;
import com.sun.appserv.management.util.misc.MapUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import com.sun.appserv.management.helper.AttributeResolverHelper;

/**
 */
public final class MailResourceConfigTest
        extends ResourceConfigTestBase {
    private static final String MAIL_RESOURCE_HOST = "localhost";
    private static final String MAIL_RESOURCE_USER = "someone";
    private static final String MAIL_RESOURCE_FROM = "someone@somewhere.com";

    private static final Map<String, String> OPTIONS = Collections.unmodifiableMap(MapUtil.newMap(
            new String[]{ResourceConfigKeys.ENABLED_KEY, "false"}));

    public MailResourceConfigTest() {
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getDomainConfig());
        }
    }

    public static String
    getDefaultInstanceName() {
        return getDefaultInstanceName("MailResourceConfig");
    }

    /**
     synchronized because multiple instances are created, and we've chosen to remove/add
     this resource multiple times for some specific tests.
     */
    public static synchronized MailResourceConfig
    ensureDefaultInstance(final DomainConfig dc) {
        MailResourceConfig result = dc.getResourcesConfig().getMailResourceConfigMap().get(getDefaultInstanceName());

        /*
        if ( result != null )
        {
            System.out.println( "ensureDefaultInstance(): removing: " +
                JMXUtil.toString( Util.getExtra(result).getObjectName() ) );
            dc.removeMailResourceConfig( result.getName() );
            result  = null;
        }
        */

        if (result == null) {
            result = createInstance(dc, getDefaultInstanceName(),
                                    MAIL_RESOURCE_HOST, MAIL_RESOURCE_USER, MAIL_RESOURCE_FROM, OPTIONS);
            assert ! AttributeResolverHelper.resolveBoolean( result, "Enabled" );
            
            final StandaloneServerConfig serverConfig = dc.getServersConfig().getStandaloneServerConfigMap().get("server");

            final Map<String, String> options = new HashMap<String, String>();
            options.put(ResourceConfigKeys.ENABLED_KEY, "false");
            final ResourceRefConfig ref = serverConfig.createResourceRefConfig(result.getName(), options);
            assert ! AttributeResolverHelper.resolveBoolean( ref, "Enabled" );

            RefHelper.removeAllRefsTo(result, false);
        }

        return result;
    }

    public static MailResourceConfig
    createInstance(
            final DomainConfig ss,
            final String name,
            final String host,
            final String user,
            final String from,
            Map<String, String> optional) {
        return ss.getResourcesConfig().createMailResourceConfig(name, host, user, from, optional);
    }

    protected String
    getProgenyTestName() {
        return ("jndi/MailResourceConfigMgrTest");
    }

    protected Container
    getProgenyContainer() {
        return getDomainConfig();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.MAIL_RESOURCE_CONFIG;
    }


    protected void
    removeProgeny(final String name) {
        getDomainConfig().getResourcesConfig().removeMailResourceConfig(name);
    }

    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> options) {
        final MailResourceConfig config =
                getDomainConfig().getResourcesConfig().createMailResourceConfig(name,
                                                           MAIL_RESOURCE_HOST,
                                                           MAIL_RESOURCE_USER,
                                                           MAIL_RESOURCE_FROM,
                                                           options);
        assert (config != null);
        return (config);
    }

}


