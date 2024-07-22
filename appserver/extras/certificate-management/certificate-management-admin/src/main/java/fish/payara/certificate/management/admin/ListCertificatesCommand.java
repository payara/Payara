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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Remote Admin Command that lists the entries in the key and/or trust store.
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Service(name = "list-certificates")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(value = {RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE})
@RestEndpoints({
        @RestEndpoint(configBean = Server.class,
                opType = RestEndpoint.OpType.GET,
                path = "list-certificates",
                description = "List Keys and Certificates in key or trust store",
                params = {
                        @RestParam(name = "target", value = "$parent")
                }
        )
})
public class ListCertificatesCommand extends AbstractRemoteCertificateManagementCommand {

    @Param(name = "listkeys", optional = true)
    private boolean listKeys;

    @Param(name = "listtrusted", optional = true)
    private boolean listTrusted;

    @Param(name = "verbose", optional = true, shortName = "v")
    private boolean verbose;

    @Param(name = "alias", optional = true, primary = true)
    private String alias;

    @Override
    public void execute(AdminCommandContext context) {
        // Check if this instance is the target - we only want to run on the local instance
        if (StringUtils.ok(target) && !target.equals(serverEnvironment.getInstanceName())) {
            return;
        }

        initStoresAndPasswords();

        Properties extraProps = new Properties();
        try {
            List<Map<String, String>> entriesList = new ArrayList<>();
            if (listKeys) {
                Map<String, String> keyEntries = CertificateManagementCommon.getEntries(
                        keystore, keystorePassword, alias, verbose);
                context.getActionReport().appendMessage("Key Store Entries: \n");
                appendMessage(context, keyEntries);

                for (Map.Entry<String, String> keyEntry : keyEntries.entrySet()) {
                    Map<String, String> keyEntryMap = new HashMap<>();
                    keyEntryMap.put("alias", keyEntry.getKey());
                    keyEntryMap.put("entry", keyEntry.getValue());
                    keyEntryMap.put("store", keystore.getAbsolutePath());
                    entriesList.add(keyEntryMap);
                }
            }
            if (listTrusted) {
                Map<String, String> trustEntries = CertificateManagementCommon.getEntries(
                        truststore, truststorePassword, alias, verbose);
                context.getActionReport().appendMessage("Trust Store Entries: \n");
                appendMessage(context, trustEntries);

                for (Map.Entry<String, String> trustEntry : trustEntries.entrySet()) {
                    Map<String, String> trustEntryMap = new HashMap<>();
                    trustEntryMap.put("alias", trustEntry.getKey());
                    trustEntryMap.put("entry", trustEntry.getValue());
                    trustEntryMap.put("store", truststore.getAbsolutePath());
                    entriesList.add(trustEntryMap);
                }
            }
            extraProps.put("entries", entriesList);
        } catch (CommandException e) {
            context.getActionReport().failure(context.getLogger(), "Error getting store entries", e);
            return;
        }

        context.getActionReport().setExtraProperties(extraProps);
    }

    /**
     * Initialises the key and/or trust store location and the password requried to access it.
     */
    private void initStoresAndPasswords() {
        if (!listKeys && !listTrusted) {
            listKeys = true;
            listTrusted = true;
        }

        if (listKeys) {
            resolveKeyStore();
        }

        if (listTrusted) {
            resolveTrustStore();
        }
    }

    /**
     * Appends the output of the keystore or truststore entries to the action report.
     * @param context The admin command context to get the action report from
     * @param entries The entries from the key or trust store.
     */
    private void appendMessage(AdminCommandContext context, Map<String, String> entries) {
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            context.getActionReport().appendMessage("Alias: " + entry.getKey());
            context.getActionReport().appendMessage("\n" + entry.getValue());
            context.getActionReport().appendMessage("\n" + "--------------------" + "\n");
        }
    }
}