/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.admin.cli;

import static fish.payara.micro.ClusterCommandResult.ExitStatus.FAILURE;
import static fish.payara.micro.ClusterCommandResult.ExitStatus.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;

import fish.payara.micro.ClusterCommandResult;
import fish.payara.micro.ClusterCommandResult.ExitStatus;
import fish.payara.test.util.PayaraMicroServer;

public abstract class AsAdminIntegrationTest {

    private static final String UNSATISFIED_DEPENDENCY_EXCEPTION_CLASS_NAME = 
            "org.jvnet.hk2.config.UnsatisfiedDependencyException";
    private static final String UNACCEPTABLE_VALUE_EXCEPTION_CLASS_NAME = 
            "org.glassfish.common.util.admin.UnacceptableValueException";

    protected final PayaraMicroServer server = PayaraMicroServer.DEFAULT;

    @Before
    public void serverSetUp() throws Exception {
        server.start();
    }

    @After
    public void serverTearDown() {
        server.stop();
    }

    protected static void assertUnchanged(boolean expected, boolean actual) {
        assertEquals(Boolean.valueOf(expected), Boolean.valueOf(actual));
    }

    protected static void assertContains(String expected, String actual) {
        assertThat(actual, CoreMatchers.containsString(expected));
    }

    protected static void assertSuccess(ClusterCommandResult result) {
        ExitStatus actual = result.getExitStatus();
        if (SUCCESS != actual) {
            String msg = result.getOutput();
            if (actual == FAILURE && result.getFailureCause() != null) {
                StringWriter writer = new StringWriter();
                result.getFailureCause().printStackTrace(new PrintWriter(writer));
                msg = writer.toString();
            }
            if (msg != null) {
                assertEquals(msg, SUCCESS, actual);
            } else {
                assertEquals(SUCCESS, actual);
            }
        }
    }

    protected static void assertFailure(ClusterCommandResult result) {
        assertEquals(FAILURE, result.getExitStatus());
    }

    private static void assertStatesUsage(ClusterCommandResult result) {
        assertTrue("No usage was given.", result.getOutput().contains("Usage: "));
    }

    protected final void assertMissingParameter(String name, ClusterCommandResult result) {
        assertFailure(result);
        Class<?> expectedExceptionType = server.getClass(UNSATISFIED_DEPENDENCY_EXCEPTION_CLASS_NAME);
        Throwable cause = result.getFailureCause();
        assertNotNull(cause);
        assertTrue("Error is not caused by a missing parameter but " + cause.getClass().getName(),
                expectedExceptionType.isAssignableFrom(cause.getClass()));
        String text = result.getOutput();
        int afterName = text.indexOf(" with class ");
        int beforeName = text.substring(0, afterName).lastIndexOf('.');
        String actualName = text.substring(beforeName + 1, afterName);
        assertEquals("Error was about another parameter", name, actualName);
        assertStatesUsage(result);
    }

    protected final void assertUnacceptableParameter(String name, ClusterCommandResult result) {
        assertFailure(result);
        Class<?> expectedExceptionType = server.getClass(UNACCEPTABLE_VALUE_EXCEPTION_CLASS_NAME);
        Throwable cause = result.getFailureCause();
        assertNotNull(cause);
        assertTrue("Error is not caused by a unacceptable parameter but " + cause.getClass().getName(),
                expectedExceptionType.isAssignableFrom(cause.getClass()));
        String text = result.getOutput();
        try {
            assertContains("Invalid parameter: " + name, text);
        } catch (AssertionError e) {
            assertContains("on parameter [ "+name+" ]", text);
        }
        assertStatesUsage(result);
    }

    /**
     * Runs an as-admin command on the {@link #server} 
     */
    protected final ClusterCommandResult asadmin(String command, String...args) {
        return new PlainClusterCommandResult(server.getInstance().executeLocalAsAdmin(command, args));
    }

    protected final Class<?> getClass(String name) {
        return server.getClass(name);
    }

    protected final <T> T getExtensionByType(String target, Class<T> type) {
        return server.getExtensionByType(target, type);
    }

    private static final class PlainClusterCommandResult implements ClusterCommandResult {

        final ClusterCommandResult wrapped;

        public PlainClusterCommandResult(ClusterCommandResult wrapped) {
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
