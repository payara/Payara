/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.config.modularity.parser.ConfigurationPopulator;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import jakarta.inject.Inject;
import java.beans.PropertyVetoException;
import javax.xml.stream.XMLStreamReader;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.GlassFishConfigBean;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Recreates a whole {@code config} element in a running instance's own configuration from the
 * XML the DAS serialized for it, so a deployment-group member that learns about a fellow member
 * live ends up holding that member's {@code config} — the same state it would have had after a
 * restart.
 * <p>
 * When an instance parses the {@code domain.xml} it synced from the DAS,
 * {@code InstanceReaderFilter} keeps the {@code config} elements of its <em>fellow group
 * members</em>, not only its own, because a member has to be able to resolve the {@code config-
 * ref} of every other member's {@code server} element. {@code _register-instance-at-instance}
 * only re-creates the {@code server} (and its {@code config-ref}) and never the {@code config}
 * itself, because members of a <em>cluster</em> all share one config; group members each have
 * their own, so without this command a live-added member's config only reached the others on
 * their next restart, leaving a {@code config-ref} dangling in the meantime.
 * <p>
 * The config cannot be copied the way {@code copy-config} does it ({@code config.deepCopy}),
 * because that needs the source {@code Config} bean present in this JVM and a member does not
 * have the joining member's config (nor {@code default-config}, which the sync filters out).
 * Instead the DAS serializes the config to XML and this command materialises it here: it parses
 * the fragment under this instance's live {@code configs} element and adds the parsed bean in a
 * configuration transaction, so it is both live and persisted to the instance's own
 * {@code domain.xml}. This mirrors how config-modularity instantiates config snippets at runtime
 * ({@code ConfigurationParser#parseAndSetConfigBean}).
 * <p>
 * It is idempotent: if the config is already present (synced at startup, or added by an earlier
 * replication of this command) it is a no-op, so re-running the join does not fail.
 *
 * @since 7.2026.10
 */
@Service(name = "_copy-config-at-instance")
@PerLookup
@ExecuteOn(value = {RuntimeType.INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "_copy-config-at-instance",
            description = "_copy-config-at-instance")
})
public class InstanceCopyConfigCommand implements AdminCommand {

    private static final LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(InstanceCopyConfigCommand.class);

    @Param(name = "name", primary = true)
    String configName;

    @Param(name = "configxml")
    String configXml;

    @Inject
    private Domain domain;

    @Inject
    private ServiceLocator serviceLocator;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        // Idempotent: the config may already be here (kept by InstanceReaderFilter at startup or
        // added by an earlier replication of this command), in which case there is nothing to do.
        if (domain.getConfigNamed(configName) != null) {
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            return;
        }

        try {
            final Configs configs = domain.getConfigs();

            // Parse the serialized <config> under this instance's live <configs> element. A
            // throwaway DomDocument is used for the parse itself (the same pattern config-
            // modularity uses); persistence is driven by the configuration transaction below,
            // not by this document.
            ConfigParser parser = new ConfigParser(serviceLocator);
            DomDocument doc = new DomDocument<GlassFishConfigBean>(serviceLocator) {
                @Override
                public Dom make(final ServiceLocator serviceLocator, XMLStreamReader reader,
                        GlassFishConfigBean parentDom, ConfigModel configModel) {
                    return new GlassFishConfigBean(serviceLocator, this, parentDom, configModel, reader);
                }
            };
            new ConfigurationPopulator(configXml, doc, configs).run(parser);
            if (doc.getRoot() == null) {
                // ConfigurationPopulator.run() logs and swallows a parse failure, leaving no root.
                report.setMessage(localStrings.getLocalString("copy.config.at.instance.unparseable",
                        "Config {0} could not be parsed on the instance", configName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
            final Config parsedConfig = doc.getRoot().createProxy(Config.class);

            ConfigSupport.apply(new SingleConfigCode<Configs>() {
                @Override
                public Object run(Configs param) throws PropertyVetoException, TransactionFailure {
                    param.getConfig().add(parsedConfig);
                    return parsedConfig;
                }
            }, configs);

            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString("copy.config.at.instance.failed",
                    "Copying config {0} to the instance failed", configName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }
}
