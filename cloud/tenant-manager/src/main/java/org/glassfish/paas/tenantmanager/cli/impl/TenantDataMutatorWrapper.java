/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.tenantmanager.cli.impl;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandWrapperImpl;
import org.glassfish.paas.tenantmanager.api.TenantManager;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.RetryableException;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.lang.annotation.Annotation;

/**
 * Wraps a command annotated with @TenantDataMutator
 */
@Service
public class TenantDataMutatorWrapper implements CommandWrapperImpl {
    @Inject
    TenantManager tm;

    /**
     * creates a wrapper that wraps command execution with following
     * <ul>
     *     <li>setCurrentTenant</li>
     *     <li>Obtain tenant lock for this process</li>
     *     <li>Obtain config transaction</li>
     * </ul>
     */
    @Override
    public AdminCommand createWrapper(final Annotation ann, final CommandModel model, final AdminCommand command, final ActionReport report) {
        return new AdminCommand() {
            @Override
            public void execute(final AdminCommandContext context) {

                try {
                    tm.setCurrentTenant("");
                    tm.lock();
                    Transaction currentTransaction = new Transaction();
                    TenantDataMutatorAdminCommandContext mutatorAdminCommandContext = new TenantDataMutatorAdminCommandContext(currentTransaction, context);
                    command.execute(mutatorAdminCommandContext);
                    //TODO improve exception handling.
                    try {
                        currentTransaction.commit();
                    } catch (RetryableException e) {
                        currentTransaction.rollback();
                        throw new RuntimeException(new TransactionFailure(e.getMessage(), e) );
                    } catch (TransactionFailure transactionFailure) {
                        throw new RuntimeException(transactionFailure);
                    }
                } finally {
                    tm.unlock();
                    tm.resetCurrentTenant();
                }
            }
        };


    }
}
