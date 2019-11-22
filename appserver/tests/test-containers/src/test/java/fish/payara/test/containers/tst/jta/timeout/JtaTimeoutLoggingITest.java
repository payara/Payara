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
package fish.payara.test.containers.tst.jta.timeout;

import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.junit.DockerITest;
import fish.payara.test.containers.tools.log4j.EventCollectorAppender;
import fish.payara.test.containers.tst.jta.timeout.war.AsynchronousTimeoutingJob;
import fish.payara.test.containers.tst.jta.timeout.war.SlowJpaPartitioner;

import io.github.zforgo.arquillian.junit5.ArquillianExtension;

import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.spi.LoggingEvent;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fish.payara.test.containers.tools.junit.WaitForExecutable.waitFor;
import static fish.payara.test.containers.tst.jta.timeout.war.SlowJpaPartitioner.TIMEOUT_IN_SECONDS;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author David Matejcek
 */
@ExtendWith(ArquillianExtension.class)
public class JtaTimeoutLoggingITest extends DockerITest {

    private static final Logger LOG = LoggerFactory.getLogger(JtaTimeoutLoggingITest.class);

    private static final Class<AsynchronousTimeoutingJob> ASYNCJOB_CLASS = AsynchronousTimeoutingJob.class;

    private static final Set<String> MESSAGES_FOR_FILTER = Arrays
        .stream(new String[] {"STDOUT:   javax.ejb.EJBTransactionRolledbackException: Client's transaction aborted", //
            "Transaction with id=", //
            "A system exception occurred during an invocation on", //
            "Rolling back timed out transaction" //
        }).collect(Collectors.toSet());

    private static EventCollectorAppender domainLog;

    @ArquillianResource
    private static URL base;

    private static final Pattern P_TIMEOUT = Pattern.compile( //
        "STDOUT:\\s+Transaction with id=[0-9]+ timed out after 200[0-9] ms.\\|\\#\\]");
    private static final Pattern P_SYS_EXCEPTION_P = Pattern
        .compile("STDOUT:\\s+A system exception occurred during an invocation on EJB SlowJpaPartitioner," //
            + " method: public void " + SlowJpaPartitioner.class.getName()
            + ".executePreparedPartition\\(\\)\\|\\#\\]");
    private static final Pattern P_EXCEPTION_TX = Pattern.compile( //
        "STDOUT:\\s+javax.ejb.EJBTransactionRolledbackException: Client's transaction aborted");


    @BeforeAll
    public static void addLogCollector() {
        final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("D-PAYARA");
        assertNotNull(logger, "D-PAYARA logger was not found");
        final Predicate<LoggingEvent> filter = event -> {
            final Predicate<? super String> predicate = msgPart -> {
                final String message = event.getMessage().toString();
                return message.contains(msgPart);
            };
            return MESSAGES_FOR_FILTER.stream().anyMatch(predicate);
        };
        domainLog = new EventCollectorAppender().withCapacity(20).withEventFilter(filter);
        logger.addAppender(domainLog);
    }


    @AfterEach
    public void resetLogCollector() {
        assertThat("log collector size", domainLog.getSize(), equalTo(0));
        domainLog.clearCache();
    }


    @AfterAll
    public static void removeLogCollectorAndResetTimeout() throws Exception {
        final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("D-PAYARA");
        assertNotNull(logger, "D-PAYARA logger was not found");
        logger.removeAppender(domainLog);
        domainLog.close();
        final PayaraServerContainer das = getDockerEnvironment().getPayaraContainer();
        das.asAdmin("set", "configs.config.server-config.transaction-service.timeout-in-seconds=0");
    }


    @Deployment(testable = false)
    public static WebArchive getArchiveToDeploy() throws Exception {
        LOG.info("createDeployment()");

        final PayaraServerContainer das = getDockerEnvironment().getPayaraContainer();
        das.asAdmin("set", "configs.config.server-config.transaction-service.timeout-in-seconds=" + TIMEOUT_IN_SECONDS);
        das.asAdmin("restart-domain");

        final String cpPrefix = "jta/timeout/war";
        final WebArchive war = ShrinkWrap.create(WebArchive.class) //
            .addPackages(true, ASYNCJOB_CLASS.getPackage()) //
            .addAsWebInfResource(cpPrefix + "/WEB-INF/classes/META-INF/persistence.xml",
                "classes/META-INF/persistence.xml") //
            .addAsWebInfResource(cpPrefix + "/WEB-INF/beans.xml", "beans.xml") //
            .addAsWebInfResource(cpPrefix + "/WEB-INF/payara-web.xml", "payara-web.xml") //
        ;
        LOG.info(war.toString(true));
        return war;
    }


    @Test
    public void testTimeoutOnly() throws Throwable {
        callService("1");
        waitFor(() -> assertThat("log entries count", domainLog.getSize(), equalTo(2)), 5000L);
        final Matcher<String> matchers = Matchers.allOf(getPatternForRollbackMessage(STATUS_MARKED_ROLLBACK));
        assertAll( //
            () -> assertThat("log entry 0", domainLog.pop().getMessage().toString(), matchesPattern(P_TIMEOUT)), //
            () -> assertThat("log entry 1", domainLog.pop().getMessage().toString(), matchers) //
            );
    }


    @Test
    public void testTimeoutWithError() throws Throwable {
        callService("2");
        waitFor(() -> assertThat("log entries count", domainLog.getSize(), equalTo(5)), 5000L);

        final Pattern sysExceptionJ = Pattern
            .compile("STDOUT:\\s+A system exception occurred during an invocation on EJB "
                + ASYNCJOB_CLASS.getSimpleName() + ", method: public void " + ASYNCJOB_CLASS.getName()
                + ".timeoutingAsyncWithFailingNextStep\\(\\)\\|\\#\\]");
        final Matcher<String> rollbackMatchers = Matchers.allOf(getPatternForRollbackMessage(STATUS_MARKED_ROLLBACK));
        assertAll( //
            () -> assertThat("log entry 0", domainLog.pop().getMessage().toString(), matchesPattern(P_TIMEOUT)), //
            () -> assertThat("log entry 1", domainLog.pop().getMessage().toString(), matchesPattern(P_SYS_EXCEPTION_P)),
            () -> assertThat("log entry 2", domainLog.pop().getMessage().toString(), rollbackMatchers), //
            () -> assertThat("log entry 3", domainLog.pop().getMessage().toString(), matchesPattern(sysExceptionJ)), //
            () -> assertThat("log entry 4", domainLog.pop().getMessage().toString(), matchesPattern(P_EXCEPTION_TX)) //
        );
    }


    @Test
    public void testTimeoutWithCatchedErrorAndRedo() throws Throwable {
        callService("3");
        waitFor(() -> assertThat("log entries count", domainLog.getSize(), equalTo(6)), 5000L);

        final Pattern sysExceptionJ = Pattern
            .compile("STDOUT:\\s+A system exception occurred during an invocation on EJB "
                + ASYNCJOB_CLASS.getSimpleName() + ", method: public void " + ASYNCJOB_CLASS.getName()
                + ".timeoutingAsyncWithFailingNextStepCatchingExceptionAndRedo\\(\\)\\|\\#\\]");
        final Matcher<String> rollbackMatchers = Matchers.allOf(getPatternForRollbackMessage(STATUS_MARKED_ROLLBACK));
        assertAll( //
            () -> assertThat("log entry 0", domainLog.pop().getMessage().toString(), matchesPattern(P_TIMEOUT)), //
            () -> assertThat("log entry 1", domainLog.pop().getMessage().toString(), matchesPattern(P_SYS_EXCEPTION_P)),
            () -> assertThat("log entry 2", domainLog.pop().getMessage().toString(), matchesPattern(P_SYS_EXCEPTION_P)),
            () -> assertThat("log entry 3", domainLog.pop().getMessage().toString(), rollbackMatchers), //
            () -> assertThat("log entry 4", domainLog.pop().getMessage().toString(), matchesPattern(sysExceptionJ)),
            () -> assertThat("log entry 5", domainLog.pop().getMessage().toString(), matchesPattern(P_EXCEPTION_TX)) //
        );
    }


    private void callService(String urlRelativePath) {
        final WebTarget target = getAnonymousBasicWebTarget();
        final WebTarget pgstorePath = target.path("/timeout");
        final Builder builder = pgstorePath.path(urlRelativePath).request();
        try (Response response = builder.post(Entity.text(""))) {
            assertEquals(Status.NO_CONTENT, response.getStatusInfo().toEnum(), "response.status");
            assertFalse(response.hasEntity(), "response.hasEntity");
        }
    }


    private Set<Matcher<? super String>> getPatternForRollbackMessage(final int localTxStatus) {
        return Arrays.stream(new String[] {"STDOUT:\\s+EJB5123:Rolling back timed out transaction ", //
            "\\[JavaEETransactionImpl\\:" //
                + " txId=[0-9]+ nonXAResource=1 jtsTx=null localTxStatus=" + localTxStatus + " syncs=", //
            "\\[com.sun.ejb.containers.SimpleEjbResourceHandlerImpl\\@[0-9a-f]+", //
            " com.sun.ejb.containers.ContainerSynchronization\\@[0-9a-f]+", //
            " org.eclipse.persistence.internal.jpa.transaction.JTATransactionWrapper\\$1\\@[0-9a-f]+", //
            " org.eclipse.persistence.transaction.JTASynchronizationListener\\@[0-9a-f]+", //
            " com.sun.enterprise.resource.pool.PoolManagerImpl\\$SynchronizationListener\\@[0-9a-f]+", //
            "\\]\\] for \\[AsynchronousTimeoutingJob\\]\\|\\#\\]"}).map(s -> ".*" + s + ".*").map(Pattern::compile)
            .map(Matchers::matchesPattern).collect(Collectors.toSet());
    }
}
