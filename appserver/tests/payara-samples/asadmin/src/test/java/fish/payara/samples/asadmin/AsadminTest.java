/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.samples.asadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.DomainExtension;

import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.common.util.admin.UnacceptableValueException;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandResult.ExitStatus;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.Target;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.jvnet.hk2.config.UnsatisfiedDependencyException;

import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.PayaraTestShrinkWrap;
import fish.payara.samples.ServerOperations;

@RunWith(PayaraArquillianTestRunner.class)
public abstract class AsadminTest {

    private final ServiceLocator serviceLocator = Globals.getDefaultHabitat();
    private CommandRunner commandRunner;
    private Target targetUtil;

    @Deployment
    public static Archive<?> deploy() {
        return PayaraTestShrinkWrap.getWebArchive()
                .addClasses(AsadminTest.class, ServerOperations.class);
    }

    @Before
    public final void setUpFields() {
        commandRunner = serviceLocator.getService(CommandRunner.class);
        targetUtil = serviceLocator.getService(Target.class);
    }

    protected final CommandResult asadmin(String command, String... args) {
        return new PlainCommandResult(commandRunner.run(command, args));
    }

    protected final <T extends DomainExtension> T getDomainExtensionByType(Class<T> type) {
        return serviceLocator.getService(Domain.class).getExtensionByType(type);
    }

    protected final <T extends ConfigExtension> T getConfigExtensionByType(String target, Class<T> type) {
        return targetUtil.getConfig(target).getExtensionByType(type);
    }

    protected final <T> T getService(Class<T> type) {
        return serviceLocator.getService(type);
    }

    protected static void assertUnchanged(boolean expected, boolean actual) {
        assertEquals(Boolean.valueOf(expected), Boolean.valueOf(actual));
    }

    protected static void assertFalse(String flag) {
        assertEquals("false", flag);
    }

    protected static void assertTrue(String flag) {
        assertEquals("true", flag);
    }

    protected static void assertTrue(boolean expression) {
        org.junit.Assert.assertTrue(expression);
    }

    protected static void assertTrue(String msg, boolean expression) {
        org.junit.Assert.assertTrue(msg, expression);
    }

    protected static void assertFalse(boolean expression) {
        org.junit.Assert.assertFalse(expression);
    }

    protected static void assertFalse(String msg, boolean expression) {
        org.junit.Assert.assertFalse(msg, expression);
    }

    protected static void assertSuccess(CommandResult result) {
        ExitStatus actual = result.getExitStatus();
        if (ExitStatus.SUCCESS != actual) {
            String msg = result.getOutput();
            if (actual == ExitStatus.FAILURE && result.getFailureCause() != null) {
                StringWriter writer = new StringWriter();
                result.getFailureCause().printStackTrace(new PrintWriter(writer));
                msg = writer.toString();
            }
            if (msg != null) {
                assertEquals(msg, ExitStatus.SUCCESS, actual);
            } else {
                assertEquals(ExitStatus.SUCCESS, actual);
            }
        }
    }

    protected static void assertWarning(CommandResult result) {
        assertEquals(ExitStatus.WARNING, result.getExitStatus());
    }

    protected static void assertContains(String expected, String actual) {
        assertThat(actual, CoreMatchers.containsString(expected));
    }

    protected static void assertFailure(CommandResult result) {
        assertEquals(ExitStatus.FAILURE, result.getExitStatus());
    }

    private static void assertStatesUsage(CommandResult result) {
        assertTrue("No usage was given.", result.getOutput().contains("Usage: "));
    }

    protected static void assertMissingParameter(String name, CommandResult result) {
        assertFailure(result);
        Throwable cause = result.getFailureCause();
        assertNotNull(cause);
        assertTrue("Error is not caused by a missing parameter but " + cause.getClass().getName(),
                UnsatisfiedDependencyException.class.isAssignableFrom(cause.getClass()));
        String text = result.getOutput();
        int afterName = text.indexOf(" with class ");
        int beforeName = text.substring(0, afterName).lastIndexOf('.');
        String actualName = text.substring(beforeName + 1, afterName);
        assertEquals("Error was about another parameter", name, actualName);
        assertStatesUsage(result);
    }

    protected static void assertUnacceptableParameter(String name, CommandResult result) {
        assertFailure(result);
        Throwable cause = result.getFailureCause();
        assertNotNull(cause);
        assertTrue("Error is not caused by a unacceptable parameter but " + cause.getClass().getName(),
                UnacceptableValueException.class.isAssignableFrom(cause.getClass()));
        String text = result.getOutput();
        try {
            assertContains("Invalid parameter: " + name, text);
        } catch (AssertionError e) {
            assertContains("on parameter [ "+name+" ]", text);
        }
        assertStatesUsage(result);
    }

    private static final class PlainCommandResult implements CommandResult {

        final CommandResult wrapped;

        public PlainCommandResult(CommandResult wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public ExitStatus getExitStatus() {
            return wrapped.getExitStatus();
        }

        @Override
        public Throwable getFailureCause() {
            return wrapped.getFailureCause();
        }

        @Override
        public String getOutput() {
            String output = wrapped.getOutput();
            if (!output.startsWith("PlainTextActionReporter")) {
                return output;
            }
            int index = output.indexOf("SUCCESS");
            if (index < 0) {
                index = output.indexOf("FAILURE");
            }
            if (index > 0) {
                return output.substring(index + 7);
            }
            return output;
        }

        @Override
        public String toString() {
            return getExitStatus() + ": " + getOutput();
        }
    }
}
