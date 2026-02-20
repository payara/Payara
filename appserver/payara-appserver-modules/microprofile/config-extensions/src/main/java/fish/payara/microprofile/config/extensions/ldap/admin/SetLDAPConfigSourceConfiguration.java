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

import com.sun.enterprise.util.StringUtils;
import fish.payara.nucleus.microprofile.config.source.extension.BaseSetConfigSourceConfigurationCommand;
import fish.payara.microprofile.config.extensions.ldap.LDAPConfigSourceConfiguration;
import static fish.payara.microprofile.config.extensions.ldap.LDAPConfigSourceConfiguration.AUTH_TYPE_NONE;
import static fish.payara.microprofile.config.extensions.ldap.LDAPConfigSourceConfiguration.AUTH_TYPE_SIMPLE;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import java.beans.PropertyVetoException;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

@Service(name = "set-ldap-config-source-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = MicroprofileConfigConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-ldap-config-source-configuration",
            description = "Configures LDAP Config Source")
})
public class SetLDAPConfigSourceConfiguration extends BaseSetConfigSourceConfigurationCommand<LDAPConfigSourceConfiguration> {

    @Param(name = "url")
    private String url;

    @Param(name = "authType", acceptableValues = "simple,none", defaultValue = AUTH_TYPE_NONE)
    private String authType;

    @Param(name = "startTLSEnabled", optional = true, defaultValue = "false")
    private Boolean startTLSEnabled;

    @Param(name = "bindDN", optional = true)
    private String bindDN;

    @Param(name = "bindDNPassword", optional = true)
    private String bindDNPassword;

    @Param(name = "searchBase", optional = true)
    private String searchBase;

    @Param(name = "searchFilter", optional = true)
    private String searchFilter;

    @Param(name = "searchScope", acceptableValues = "subtree,onelevel,object", optional = true)
    private String searchScope;

    @Param(name = "connectionTimeout", optional = true)
    private String connectionTimeout;

    @Param(name = "readTimeout", optional = true)
    private String readTimeout;

    @Override
    protected void applyValues(ActionReport report, LDAPConfigSourceConfiguration configuration) throws PropertyVetoException {
        if (authType.equals(AUTH_TYPE_SIMPLE) && (!StringUtils.ok(bindDN) || !StringUtils.ok(bindDNPassword))) {
            report.setMessage("bindDN and bindDNPassword param can not be empty for 'simple' auth type");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        } else if (authType.equals(AUTH_TYPE_NONE) && (!StringUtils.ok(searchBase) || !StringUtils.ok(searchFilter))) {
            report.setMessage("searchBase and searchFilter param can not be empty for 'none' auth type");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        super.applyValues(report, configuration);
        if (url != null) {
            configuration.setUrl(url);
        }
        if (authType != null) {
            configuration.setAuthType(authType);
        }
        if (startTLSEnabled != null) {
            configuration.setStartTLSEnabled(Boolean.toString(startTLSEnabled));
        }
        if (bindDN != null) {
            configuration.setBindDN(bindDN);
        }
        if (bindDNPassword != null) {
            configuration.setBindDNPassword(bindDNPassword);
        }
        if (searchBase != null) {
            configuration.setSearchBase(searchBase);
        }
        if (searchFilter != null) {
            configuration.setSearchFilter(searchFilter);
        }
        if (searchScope != null) {
            configuration.setSearchScope(searchScope);
        }
        if (connectionTimeout != null) {
            configuration.setConnectionTimeout(connectionTimeout);
        }
        if (readTimeout != null) {
            configuration.setReadTimeout(readTimeout);
        }
    }

}
