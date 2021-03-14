/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.test.containers.tst.security;

import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.container.PayaraServerFiles;
import fish.payara.test.containers.tools.env.DockerEnvironment;
import fish.payara.test.containers.tools.env.TestConfiguration;
import fish.payara.test.containers.tools.junit.DockerITestExtension;
import fish.payara.test.containers.tst.security.jar.jaspic.CustomSAM;
import fish.payara.test.containers.tst.security.war.jaspic.servlet.PublicServlet;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.utility.MountableFile;

import static fish.payara.test.containers.tools.container.TestablePayaraPort.CLUSTERED_INSTANCE_HTTP_PORT;
import static fish.payara.test.containers.tst.security.jar.jaspic.CustomSAM.RESPONSE_CUSTOMSAM_INVOKED;
import static fish.payara.test.containers.tst.security.war.jaspic.servlet.ProtectedServlet.RESPONSE_PROTECTED_SERVLET_INVOKED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test verifies that JASPIC SAM authentication works after a war file is deployed to
 * a new clustered domain.
 * <p>
 * This test was created as a reproducer for issue CUSTCOM-133.
 *
 * @author David Matejcek
 */
@ExtendWith(DockerITestExtension.class)
public class JaspicSamAuthenticationITest {

    private static final Logger LOG = LoggerFactory.getLogger(JaspicSamAuthenticationITest.class);

    private static final String DOMAIN_NAME = JaspicSamAuthenticationITest.class.getSimpleName() + "Domain";
    private static final String SSH_NODE_NAME = JaspicSamAuthenticationITest.class.getSimpleName() + "SshNode";
    private static final String WAR_ROOT_CTX = "/jaspic-lifecycle";

    private static final TestConfiguration TEST_CFG = TestConfiguration.getInstance();
    private static final Class<CustomSAM> CLASS_AUTHMODULE = CustomSAM.class;
    private static final Class<PublicServlet> CLASS_WAR = PublicServlet.class;

    private static File jarFileOnHost;
    private static File jarFileOnServer;
    private static File warFileOnHost;
    private static File warFileOnServer;

    private static PayaraServerContainer payara;

    @BeforeAll
    public static void createArtifacts() throws Exception {
        payara = DockerEnvironment.getInstance().getPayaraContainer();

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class) //
            .addPackages(true, CLASS_AUTHMODULE.getPackage()) //
        ;
        LOG.info(jar.toString(true));
        jarFileOnHost = new File(TEST_CFG.getBuildDirectory(), JaspicSamAuthenticationITest.class.getSimpleName() + "-sam.jar");
        jar.as(ZipExporter.class).exportTo(jarFileOnHost, true);
        final PayaraServerFiles payaraFilesInDocker = new PayaraServerFiles(
            payara.getPayaraFileStructureInDocker().getMainDirectory(), DOMAIN_NAME);
        jarFileOnServer = new File(payaraFilesInDocker.getDomainLibDirectory(), jarFileOnHost.getName());

        final File webInfDir = TEST_CFG.getClassDirectory().toPath()
            .resolve(Paths.get("security", "war", "jaspic", "servlet", "WEB-INF")).toFile();
        final WebArchive war = ShrinkWrap.create(WebArchive.class) //
            .addPackages(true, CLASS_WAR.getPackage()) //
            .addAsWebInfResource(new File(webInfDir, "web.xml"))
            .addAsWebInfResource(new File(webInfDir, "payara-web.xml"))
        ;
        LOG.info(war.toString(true));
        warFileOnHost = new File(TEST_CFG.getBuildDirectory(), JaspicSamAuthenticationITest.class.getSimpleName() + ".war");
        war.as(ZipExporter.class).exportTo(warFileOnHost, true);
        warFileOnServer = new File("/", warFileOnHost.getName());
    }


    @BeforeEach
    public void initDomain() throws Exception {
        payara.asLocalAdmin("stop-domain", TEST_CFG.getPayaraDomainName());
    }


    @AfterEach
    public void destroyDomain() throws Exception {
        payara.asLocalAdmin("stop-domain", DOMAIN_NAME);
    }


    @AfterAll
    public static void restartDefaultDomain() throws Exception {
        payara.asLocalAdmin("restart-domain", TEST_CFG.getPayaraDomainName());
    }


    @Test
    public void testJaspicEnabledApp() throws Throwable {
        payara.asLocalAdmin("create-domain", "--nopassword", DOMAIN_NAME);
        final PayaraServerFiles origPaths = payara.getPayaraFileStructureInDocker();
        final File srcLoggingProps = new File(origPaths.getDomainConfigDirectory(), "logging.properties");
        final File tgtLoggingProps = origPaths.getDomainDirectory().getParentFile().toPath()
            .resolve(Paths.get(DOMAIN_NAME, "config", srcLoggingProps.getName())).toFile();
        final ExecResult cpResult = payara.execInContainer("cp", srcLoggingProps.getAbsolutePath(),
            tgtLoggingProps.getAbsolutePath());
        assertEquals(0, cpResult.getExitCode(), cpResult.getStderr());
        payara.copyFileToContainer(MountableFile.forHostPath(jarFileOnHost.getAbsolutePath()),
            jarFileOnServer.getAbsolutePath());
        payara.copyFileToContainer(MountableFile.forHostPath(warFileOnHost.getAbsolutePath()),
            warFileOnServer.getAbsolutePath());

        payara.asLocalAdmin("start-domain", DOMAIN_NAME);
        payara.asLocalAdmin("create-node-ssh", "--nodehost=localhost", SSH_NODE_NAME);
        payara.asLocalAdmin("copy-config", "default-config", "cluster-config");
        payara.asLocalAdmin("create-cluster", "--config=cluster-config", "cluster");
        payara.asLocalAdmin("create-instance", "--cluster=cluster", "--node=" + SSH_NODE_NAME, "inst1");
        payara.asLocalAdmin("start-cluster", "cluster");
        payara.asLocalAdmin("create-message-security-provider", "--classname=" + CLASS_AUTHMODULE.getName(),
            "--isdefaultprovider=true", "--layer=HttpServlet", "--providertype=server", "--target=cluster-config",
            "TestSAM");

        final URL protectedServletUrl = new URL("http", "localhost", CLUSTERED_INSTANCE_HTTP_PORT.getPort(),
            WAR_ROOT_CTX + "/protected/servlet");
        final ExecResult protectedServletResp0 = payara.execInContainer("curl", "-s", "-G", "--http1.1",
            protectedServletUrl.toExternalForm());
        assertThat("First response before deployment should be HTTP 404", protectedServletResp0.getStdout(),
            containsString("HTTP Status 404 - Not Found"));

        payara.asLocalAdmin("deploy", "--contextroot=" + WAR_ROOT_CTX, "--target=cluster",
            warFileOnServer.getAbsolutePath());

        final ExecResult protectedServletResp = payara.execInContainer("curl", "-s", "-G", "--http1.1",
            protectedServletUrl.toExternalForm());
        assertAll( //
            () -> assertEquals(0, protectedServletResp.getExitCode(), "Second: exit code"),
            () -> assertThat(
                "Second response after deployment should be HTTP 200, because SAM has been activated",
                protectedServletResp.getStdout(), containsString(RESPONSE_PROTECTED_SERVLET_INVOKED)),
            () -> assertThat("Second response after deployment should be HTTP 200, because SAM has been activated",
                protectedServletResp.getStdout(), containsString(RESPONSE_CUSTOMSAM_INVOKED)));
    }
}
