/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.config.extensions.ldap.admin;

import fish.payara.nucleus.microprofile.config.source.extension.BaseGetConfigSourceConfigurationCommand;
import fish.payara.microprofile.config.extensions.ldap.LDAPConfigSourceConfiguration;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import java.util.Map;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

@Service(name = "get-ldap-config-source-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(value = {RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = MicroprofileConfigConfiguration.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-ldap-config-source-configuration",
            description = "List LDAP Config Source Configuration")
})
public class GetLDAPConfigSourceConfiguration extends BaseGetConfigSourceConfigurationCommand<LDAPConfigSourceConfiguration> {

    @Override
    protected Map<String, Object> getConfigSourceConfiguration(LDAPConfigSourceConfiguration configuration) {
        Map<String, Object> config = super.getConfigSourceConfiguration(configuration);
        if (configuration != null) {
            config.put("Url", configuration.getUrl());
            config.put("Auth Type", configuration.getAuthType());
            config.put("Start TLS Enabled", configuration.getStartTLSEnabled());
            config.put("BindDN", configuration.getBindDN());
            config.put("BindDN Password", configuration.getBindDNPassword());
            config.put("Search Base", configuration.getSearchBase());
            config.put("Search Filter", configuration.getSearchFilter());
            config.put("Search Scope", configuration.getSearchScope());
            config.put("Connection Timeout", configuration.getConnectionTimeout());
            config.put("Read Timeout", configuration.getReadTimeout());
        }
        return config;
    }

}
