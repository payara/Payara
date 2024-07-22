/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.certificate.management.admin;

import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.StringUtils;
import fish.payara.certificate.management.CertificateManagementCommon;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Remote Admin Command that removes a certificate or bundle from the keystore.
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Service(name = "_remove-keystore-entry")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(value = {RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE})
@RestEndpoints({
        @RestEndpoint(configBean = Server.class,
                opType = RestEndpoint.OpType.DELETE,
                path = "remove-keystore-entry",
                description = "Removes Keys and Certificates from key store",
                params = {
                        @RestParam(name = "target", value = "$parent")
                }
        )
})
public class RemoveKeystoreEntryCommand extends AbstractRemoteCertificateManagementCommand {

    @Param(name = "alias", primary = true)
    private String alias;

    @Override
    public void execute(AdminCommandContext context) {
        // Check if this instance is the target - we only want to run on the local instance
        if (StringUtils.ok(target) && !target.equals(serverEnvironment.getInstanceName())) {
            return;
        }

        resolveKeyStore();

        try {
            removeFromKeyStore();
        } catch (CommandException e) {
            context.getActionReport().failure(context.getLogger(), "Error removing keystore entry", e);
            return;
        }
    }

    /**
     * Removes the entry that matches the provided alias from the target keystore.
     * @throws CommandException If there's an issue removing the entry from the keystore.
     */
    private void removeFromKeyStore() throws CommandException {
        try {
            KeyStore store = KeyStore.getInstance(CertificateManagementCommon.getKeystoreType(keystore));
            store.load(new FileInputStream(keystore), keystorePassword);
            store.deleteEntry(alias);
            try (FileOutputStream out = new FileOutputStream(keystore)) {
                store.store(out, keystorePassword);
                out.flush();
            }
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException ex) {
            throw new CommandException(ex);
        }
    }
}
