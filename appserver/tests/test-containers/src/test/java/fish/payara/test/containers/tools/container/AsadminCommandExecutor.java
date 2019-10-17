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
package fish.payara.test.containers.tools.container;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;

import static java.util.stream.Stream.of;

/**
 * In container asadmin command executor. Uses asadmin command directly.
 *
 * @author David Matejcek
 */
public class AsadminCommandExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(AsadminCommandExecutor.class);
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private final PayaraServerContainer container;
    private final String[] defaultArgs;


    /**
     * @param container
     * @param defaultArgs - arguments, which will be added before the command name.
     */
    public AsadminCommandExecutor(final PayaraServerContainer container, final String[] defaultArgs) {
        this.container = container;
        this.defaultArgs = defaultArgs;
    }


    /**
     * Executes the asadmin command.
     *
     * @param command - command name - must not be null.
     * @param arguments - command arguments - nullable.
     * @throws AsadminCommandException
     */
    public void exec(final String command, final String... arguments) throws AsadminCommandException {
        LOG.debug("exec(command={}, arguments={})", command, arguments);
        Objects.requireNonNull(command, "command");
        final String asadmin = container.getAsadmin().getAbsolutePath();
        final String[] args = concat(of(asadmin), of(defaultArgs), of(command), of(arguments));
        try {
            final ExecResult result = container.execInContainer(CHARSET, args);
            final int exitCode = result.getExitCode();
            LOG.debug("args={}, exitCode={},\nstdout:\n{}\nstderr:\n{}", args, exitCode, result.getStdout(),
                result.getStderr());
            if (exitCode == 0) {
                return;
            }
            throw new AsadminCommandException(
                "Execution of command '" + command + "' failed with: \n" + result.getStderr());
        } catch (final UnsupportedOperationException | InterruptedException | IOException e) {
            throw new IllegalStateException("Could not execute asadmin command " + command, e);
        }
    }


    @SafeVarargs
    private static String[] concat(final Stream<String>... streams) {
        return Stream.of(streams).flatMap(Function.identity()).toArray(String[]::new);
    }
}
