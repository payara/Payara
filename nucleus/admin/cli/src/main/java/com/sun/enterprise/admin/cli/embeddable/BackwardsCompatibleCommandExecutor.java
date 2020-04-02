/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
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

package com.sun.enterprise.admin.cli.embeddable;

import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

/**
 * This is delegate for retaining embedded API after Command Runner moved to common Payara API.
 */
@Service
@PerLookup
@ContractsProvided({org.glassfish.embeddable.CommandRunner.class, CommandExecutorImpl.class})
public class BackwardsCompatibleCommandExecutor implements CommandRunner {
    @Inject
    CommandExecutorImpl delegate;

    @Override
    public CommandResult run(String command, String... args) {
        fish.payara.asadmin.CommandResult result = delegate.run(command, args);
        return new CommandResult() {
            @Override
            public ExitStatus getExitStatus() {
                return ExitStatus.valueOf(result.getExitStatus().name());
            }

            @Override
            public String getOutput() {
                return result.getOutput();
            }

            @Override
            public Throwable getFailureCause() {
                return result.getFailureCause();
            }
        };
    }

    @Override
    public void setTerse(boolean terse) {
        delegate.setTerse(terse);
    }

    @Override
    public fish.payara.asadmin.CommandRunner asPayaraApi() {
        return delegate;
    }
}
