/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.test.containers.tst.restart;

import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.container.PayaraServerContainerConfiguration;
import fish.payara.test.containers.tools.env.DockerEnvironment;
import fish.payara.test.containers.tools.junit.DockerITestExtension;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Test for reliable server startup and shutdown with extremely verbose logging.
 *
 * @author David Matejcek
 */
@ExtendWith(DockerITestExtension.class)
public class VerboseLoggingITest {

    private static final Duration TIMEOUT = Duration.ofSeconds(120L);

    private static DockerEnvironment environment;
    private static File backupLoggingProperties;
    private static Logger logger;
    private static Level originalLevel;


    /**
     * Prevents slowing down the test by external output - ie. KDE's Konsole is very slow.
     */
    @BeforeAll
    public static void init() {
        environment = DockerEnvironment.getInstance();
        logger = Logger.getLogger("D-PAYARA");
        originalLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
    }


    @AfterAll
    public static void reset() throws Exception {
        final PayaraServerContainerConfiguration cfg = environment.getConfiguration().getPayaraServerConfiguration();
        // brutal force needed, because all other ways may hang
        environment.getPayaraContainer().execInContainer("killall", "-v", "-SIGKILL", "java");
        final PayaraServerContainer das = environment.getPayaraContainer();
        assertNotNull(das, "Payara Container is null");
        das.execInContainer("cp", "-v", backupLoggingProperties.getAbsolutePath(),
            cfg.getPayaraLoggingPropertiesInDocker().getAbsolutePath());
        environment.getPayaraContainer().asLocalAdmin("start-domain");
        logger.setLevel(originalLevel);
    }


    @Test
    public void restartDomainWithAllLoggingEnabled() throws Exception {
        final PayaraServerContainer das = environment.getPayaraContainer();
        das.asLocalAdmin("stop-domain");

        final PayaraServerContainerConfiguration cfg = environment.getConfiguration().getPayaraServerConfiguration();
        final File configDir = cfg.getPayaraLoggingPropertiesInDocker().getParentFile();
        backupLoggingProperties = new File(configDir, "logging.properties.before" + getClass().getSimpleName());
        final String loggingPropertiesPath = cfg.getPayaraLoggingPropertiesInDocker().getAbsolutePath();
        das.execInContainer("cp", "-v", loggingPropertiesPath, backupLoggingProperties.getAbsolutePath());
        das.execInContainer("cp", "-v", "/logging-everything.properties", loggingPropertiesPath);

        assertTimeoutPreemptively(TIMEOUT, () -> das.asLocalAdmin("start-domain"));
        // if the domain started, then it should be able to stop too
        assertTimeoutPreemptively(TIMEOUT, () -> das.asLocalAdmin("stop-domain"));
        // file system buffering - we are watching from different OS, so we don't have control over this.
        waitWhileServerLogChages(getServerLog(cfg));
    }


    private static File getServerLog(final PayaraServerContainerConfiguration cfg) {
        return cfg.getPayaraDomainDirectory().toPath().resolve(Paths.get("logs", "server.log")).toFile();
    }


    private static void waitWhileServerLogChages(final File serverLog) throws InterruptedException {
        long lastChange = getLastModified(serverLog);
        while (true) {
            Thread.sleep(2000L);
            final long actualChange = getLastModified(serverLog);
            if (lastChange == actualChange) {
                return;
            }
            lastChange = actualChange;
        }
    }


    private static long getLastModified(final File serverLog) {
        if (serverLog.canRead()) {
            return serverLog.lastModified();
        }
        return System.currentTimeMillis();
    }
}
