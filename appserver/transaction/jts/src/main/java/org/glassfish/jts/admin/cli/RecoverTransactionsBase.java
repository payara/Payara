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

package org.glassfish.jts.admin.cli;

import javax.inject.Inject;

import org.glassfish.api.Param;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;

import java.util.logging.Logger;
import java.util.logging.Level;

public class RecoverTransactionsBase {

    static StringManager localStrings =
            StringManager.getManager(RecoverTransactionsBase.class);

    static Logger _logger = LogDomains.getLogger(RecoverTransactionsBase.class, 
            LogDomains.TRANSACTION_LOGGER);

    @Inject
    Servers servers;

    @Param(name = "transactionlogdir", optional = true)
    String transactionLogDir;

    @Param(name = "server_name", primary = true)
    String serverToRecover;

    String validate(String destinationServer, boolean validateAllParams) { 
        if (_logger.isLoggable(Level.INFO)) {
            _logger.info("==> validating target: " + destinationServer + " ... server: " + serverToRecover);
        }

        if (servers.getServer(serverToRecover) == null) {
            return localStrings.getString("recover.transactions.serverBeRecoveredIsNotKnown",
                    serverToRecover);
        }

        if (isServerRunning(serverToRecover)) {
            if (destinationServer != null && !serverToRecover.equals(destinationServer)) {
                return localStrings.getString(
                        "recover.transactions.runningServerBeRecoveredFromAnotherServer",
                        serverToRecover, destinationServer);
            }
            if (transactionLogDir != null) {
                return localStrings.getString(
                        "recover.transactions.logDirShouldNotBeSpecifiedForSelfRecovery");
            }
        } else if (destinationServer == null) {
            return localStrings.getString("recover.transactions.noDestinationServer");

        } else if (servers.getServer(destinationServer) == null) {
            return localStrings.getString("recover.transactions.DestinationServerIsNotKnown");

        } else if (!isServerRunning(destinationServer)) {
            return localStrings.getString("recover.transactions.destinationServerIsNotAlive",
                    destinationServer);

        } else if (validateAllParams && transactionLogDir == null) {
             return localStrings.getString("recover.transactions.logDirNotSpecifiedForDelegatedRecovery");
        }

        return null;
    }

    private boolean isServerRunning(String serverName) {
        boolean rs = false;
        for(Server server : servers.getServer()) {
            if(serverName.equals(server.getName())) {
                rs = server.isRunning();
                break;
            }
        }

        return rs;
    }

}
