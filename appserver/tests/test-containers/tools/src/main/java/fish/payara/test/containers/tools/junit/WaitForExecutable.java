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
package fish.payara.test.containers.tools.junit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Repeatedly gives chances to pass the executable tests until timeout not exceeded.
 * <p>
 * Note: Dont't confuse with {@link org.junit.jupiter.api.AssertTimeout}
 *
 * @author David Matejcek
 */
public class WaitForExecutable implements Executable {

    private static final Logger LOG = LoggerFactory.getLogger(WaitForExecutable.class);

    private final Executable executable;

    private WaitForExecutable(final Executable executable) {
        this.executable = executable;
    }


    /**
     * Creates and runs new executable waiting to pass given executable.
     *
     * @param executable executable that has to end without errors to pass the test
     * @param timeoutInMillis timeout to pass
     * @throws Throwable thrown after timeout, not sooner
     */
    public static void waitFor(final Executable executable, final long timeoutInMillis) throws Throwable {
        waitForPasses(executable, timeoutInMillis).execute();
    }


    /**
     * Creates a new executable waiting to pass given executable. Don't forget to execute in
     * {@link Assertions#assertAll(Executable...)} or similar method..
     *
     * @param executable executable that has to end without errors to pass the test
     * @param timeoutInMillis timeout to pass
     * @return new instance of {@link WaitForExecutable}
     */
    public static WaitForExecutable waitForPasses(final Executable executable, final long timeoutInMillis) {
        final Executable newExecutable = () -> {
            long currentTimeMillis = System.currentTimeMillis();
            final long limit = currentTimeMillis + timeoutInMillis;
            while (true) {
                try {
                    executable.execute();
                    return;
                } catch (final AssertionError e) {
                    final long now = System.currentTimeMillis();
                    if (now > limit) {
                        throw e;
                    }
                    LOG.warn("Nope. Remaining time is {} ms. Waiting ...", limit - now);
                    Thread.sleep(100L);
                } catch (final Throwable e) {
                    fail(e);
                }
            }
        };
        return new WaitForExecutable(newExecutable);
    }


    @Override
    public void execute() throws Throwable {
        executable.execute();
    }
}
