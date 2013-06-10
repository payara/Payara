/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.admin.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.security.SecurityLoggerInfo;

import java.util.Iterator;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 * Adjusts the DAS configuration to turn off secure admin, as if by
 * executing these commands:
 * <pre>
 * {@code

asadmin -s set configs.config.server-config.network-config.network-listeners.network-listener.admin-listener.protocol=admin-listener

asadmin -s delete-protocol sec-admin-listener
asadmin -s delete-protocol admin-http-redirect
asadmin -s delete-protocol pu-protocol
}
 * 
 * @author Tim Quinn
 */
@Service(name = "disable-secure-admin")
@PerLookup
@I18n("disable.secure.admin.command")
@ExecuteOn({RuntimeType.DAS,RuntimeType.INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="disable-secure-admin", 
        description="disable-secure-admin")
})
@AccessRequired(resource="domain/secure-admin", action="disable")
public class DisableSecureAdminCommand extends SecureAdminCommand {

    @Override
    protected String transactionErrorMessageKey() {
        return SecurityLoggerInfo.disablingSecureAdminError;
    }

    @Override
    Iterator<Work<TopLevelContext>> secureAdminSteps() {
        return reverseStepsIterator(secureAdminSteps);
    }

    @Override
    Iterator<Work<ConfigLevelContext>> perConfigSteps() {
        return reverseStepsIterator(perConfigSteps);
    }

    /**
     * Iterator which returns array elements from back to front.
     * @param <T>
     * @param steps
     * @return
     */
    private <T extends SecureAdminCommand.Context> Iterator<Work<T>> reverseStepsIterator(Step<T>[] steps) {
        return new Iterator<Work<T>> () {
            private Step<T>[] steps;
            private int nextSlot;

            @Override
            public boolean hasNext() {
                return nextSlot >= 0;
            }

            /**
             * Returns the disable work associated with the next step we should
             * process for disabling secure admin.
             */
            @Override
            public Work<T> next() {
                return steps[nextSlot--].disableWork();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            Iterator<Work<T>> init(Step<T>[] steps) {
                this.steps = steps;
                nextSlot = this.steps.length - 1;
                return this;
            }

        }.init(steps);
    }
}
