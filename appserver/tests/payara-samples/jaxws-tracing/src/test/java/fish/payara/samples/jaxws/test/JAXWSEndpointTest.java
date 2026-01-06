/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */
package fish.payara.samples.jaxws.test;

import fish.payara.samples.PayaraTestShrinkWrap;
import fish.payara.samples.jaxws.endpoint.TraceMonitor;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.xml.ws.Service;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import static fish.payara.samples.CliCommands.payaraGlassFish;

public abstract class JAXWSEndpointTest {
    private static final Logger LOG = Logger.getLogger(JAXWSEndpointTest.class.getName());

    protected Service jaxwsEndPointService;

    @Inject
    private TraceMonitor traceMonitor;

    @Rule
    public TestName name = new TestName();

    @ArquillianResource
    protected URL url;

    public static WebArchive createBaseDeployment() {
        return PayaraTestShrinkWrap
                .getWebArchive()
                .addPackage(TraceMonitor.class.getPackage())
                .addClass(JAXWSEndpointTest.class);
    }

    public boolean isTraceMonitorTriggered() {
        return traceMonitor.isObserverCalled();
    }

    @BeforeClass
    public static void enableRequesttracing() throws Exception {
        if (!Boolean.parseBoolean(System.getProperty("skipConfig", "false"))) {
            payaraGlassFish(
                    "set-requesttracing-configuration",
                    "--thresholdValue=25",
                    "--enabled=true",
                    "--target=server-config",
                    "--thresholdUnit=MICROSECONDS",
                    "--dynamic=true"
            );

            payaraGlassFish(
                    "notification-cdieventbus-configure",
                    "--loopBack=true",
                    "--dynamic=true",
                    "--enabled=true",
                    "--hazelcastEnabled=true"
            );
        }
    }

    @Before
    public void logStart() {
        LOG.log(Level.INFO, "Test method {0} started.", name.getMethodName());
    }

    @After
    public void logEnd() {
        LOG.log(Level.INFO, "Test method {0} finished.", name.getMethodName());
    }

    @AfterClass
    public static void disableRequestTracing() {
        if (!Boolean.parseBoolean(System.getProperty("skipTestConfigCleanup", "false"))) {
            payaraGlassFish(
                    "set-requesttracing-configuration",
                    "--enabled=false",
                    "--dynamic=true"
            );

            payaraGlassFish(
                    "notification-cdieventbus-configure",
                    "--enabled=false",
                    "--dynamic=true"
            );
        }
    }

}
