/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amxtest;


import com.sun.appserv.management.util.misc.TypeCast;
import org.glassfish.admin.amxtest.base.*;
import org.glassfish.admin.amxtest.client.MiscTest;
import org.glassfish.admin.amxtest.client.PerformanceTest;
import org.glassfish.admin.amxtest.client.ProxyFactoryTest;
import org.glassfish.admin.amxtest.client.ProxyTest;
import org.glassfish.admin.amxtest.config.*;
import org.glassfish.admin.amxtest.ext.logging.LoggingHelperTest;
import org.glassfish.admin.amxtest.ext.logging.LoggingTest;
import org.glassfish.admin.amxtest.ext.logging.StatefulLoggingHelperTest;
import org.glassfish.admin.amxtest.helper.RefHelperTest;
import org.glassfish.admin.amxtest.j2ee.J2EETest;
import org.glassfish.admin.amxtest.j2ee.ServletTest;
import org.glassfish.admin.amxtest.monitor.CallFlowMonitorTest;
import org.glassfish.admin.amxtest.monitor.JMXMonitorMgrTest;
import org.glassfish.admin.amxtest.monitor.MonitorTest;
import org.glassfish.admin.amxtest.support.CoverageInfoTest;

import java.util.ArrayList;
import java.util.List;


/**
 <b>The place</b> to put list any new test; the official list
 of tests.  The file amxtest.classes is also used, but since
 it may be inadvertantly modified, this is the official list
 of tests.
 */
public class Tests {
    private Tests() {}

    private static final Class<junit.framework.TestCase>[] TestClasses =
            TypeCast.asArray(new Class[]
            {
                TestTemplateTest.class, // ensure that the template one works OK, too!

                // these tests are standalone and do not require a
                // server connection
                CoverageInfoTest.class,

                //  Tests that follow require a server connection
                //AppserverConnectionSourceTest.class,
                RunMeFirstTest.class,

                ProxyTest.class,
                ProxyFactoryTest.class,
                AMXTest.class,
                GetSetAttributeTest.class,
                ContainerTest.class,
                GenericTest.class,
                PropertiesAccessTest.class,
                SystemPropertiesAccessTest.class,

                LoggingTest.class,
                LoggingHelperTest.class,
                StatefulLoggingHelperTest.class,

                DomainRootTest.class,
                UploadDownloadMgrTest.class,
                BulkAccessTest.class,
                QueryMgrTest.class,
                NotificationEmitterServiceTest.class,
                NotificationServiceMgrTest.class,
                NotificationServiceTest.class,
                MiscTest.class,

                MonitorTest.class,
                JMXMonitorMgrTest.class,

                J2EETest.class,
                ServletTest.class,

                DanglingRefsTest.class,
                ConfigRunMeFirstTest.class,
                DescriptionTest.class,
                EnabledTest.class,
                LibrariesTest.class,
                RefHelperTest.class,
                ListenerTest.class,
                DomainConfigTest.class,
                ConfigConfigTest.class,
                SecurityServiceConfigTest.class,
                MessageSecurityConfigTest.class,
                StandaloneServerConfigTest.class,
                ClusteredServerConfigTest.class,
                NodeAgentConfigTest.class,
                CustomMBeanConfigTest.class,
                ReferencesTest.class,
                HTTPServiceConfigTest.class,
                HTTPListenerConfigTest.class,
                ClusterConfigTest.class,
                SSLConfigTest.class,
                JMXConnectorConfigTest.class,
                IIOPListenerConfigTest.class,
                HTTPListenerConfigTest.class,
                AuditModuleConfigTest.class,
                AuthRealmConfigTest.class,
                JavaConfigTest.class,
                ProfilerConfigTest.class,
                VirtualServerConfigTest.class,
                JACCProviderConfigTest.class,
                AdminObjectResourceConfigTest.class,
                JDBCResourceConfigTest.class,
                MailResourceConfigTest.class,
                ConnectorConnectionPoolConfigTest.class,
                JDBCConnectionPoolConfigTest.class,
                PersistenceManagerFactoryResourceConfigTest.class,
                JNDIResourceConfigTest.class,
                ThreadPoolConfigTest.class,
                LBTest.class,
                SecurityMapConfigTest.class,
                ConnectorConnectionPoolConfigTest.class,
                ResourceAdapterConfigTest.class,
                CustomResourceConfigTest.class,
                ConnectorServiceConfigTest.class,
                DiagnosticServiceConfigTest.class,

                PerformanceTest.class,
                CallFlowMonitorTest.class,
                RunMeLastTest.class,
            });

    public static List<Class<junit.framework.TestCase>>
    getTestClasses() {
        final List<Class<junit.framework.TestCase>> classes =
                new ArrayList<Class<junit.framework.TestCase>>();

        for (int i = 0; i < TestClasses.length; ++i) {
            final Class<junit.framework.TestCase> testClass = TestClasses[i];

            classes.add(testClass);
        }

        return (classes);
    }

};

