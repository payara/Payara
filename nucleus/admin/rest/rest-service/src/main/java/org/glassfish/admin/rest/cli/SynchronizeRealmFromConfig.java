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

package org.glassfish.admin.rest.cli;

import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoint.OpType;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * returns the list of targets
 *
 * @author ludovic Champenois
 */
@Service(name = "__synchronize-realm-from-config")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE,
    CommandTarget.CLUSTER, CommandTarget.CONFIG, CommandTarget.CLUSTERED_INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean=Config.class,
        opType=OpType.POST,
        path="synchronize-realm-from-config",
        params={
            @RestParam(name="target", value="$parent")
        })
})
public class SynchronizeRealmFromConfig implements AdminCommand {

    @Inject
    com.sun.enterprise.config.serverbeans.Domain domain;
    //TODO: for consistency with other commands dealing with realms
    //uncomment this below.
    //@Param(name="authrealmname")
    @Param
    String realmName;
    @Param(name = "target", primary = true, optional = true, defaultValue =
    SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    private String target;
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;
    @Inject
    private Configs configs;
    @Inject
    RealmsManager realmsManager;
    private static final LocalStringManagerImpl _localStrings =
            new LocalStringManagerImpl(SupportsUserManagementCommand.class);

    @Override
    public void execute(AdminCommandContext context) {
        Config realConfig = null;

        try {
            realConfig = configs.getConfigByName(target);
        } catch (Exception ex) {
        }
        if (realConfig == null) {
            Server targetServer = domain.getServerNamed(target);
            if (targetServer != null) {
                realConfig = domain.getConfigNamed(targetServer.getConfigRef());
            }
            com.sun.enterprise.config.serverbeans.Cluster cluster = domain.getClusterNamed(target);
            if (cluster != null) {
                realConfig = domain.getConfigNamed(cluster.getConfigRef());
            }
        }

        ActionReport report = context.getActionReport();
        try {
            //TODO: can i use realConfig.equals(config) instead
            if (realConfig.getName().equals(config.getName())) {
                this.setRestartRequired(report);
                return;
            }
            //this is not an active config so try and update the backend
            //directly
            Realm r = realmsManager.getFromLoadedRealms(realConfig.getName(), realmName);
            if (r == null) {
                //realm is not loaded yet
                report.setMessage(
                        _localStrings.getLocalString("REALM_SYNCH_SUCCESSFUL",
                        "Synchronization of Realm {0} from Configuration Successful.",
                        new Object[]{realmName}));
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return;
            }
            //now we really need to update the realm in the backend from the config.
            realmsManager.removeFromLoadedRealms(realConfig.getName(), realmName);
            boolean done = this.instantiateRealm(realConfig, realmName);
            if (done) {
                report.setMessage(
                        _localStrings.getLocalString("REALM_SYNCH_SUCCESSFUL",
                        "Synchronization of Realm {0} from Configuration Successful.",
                        new Object[]{realmName}));
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return;
            }
        } catch (BadRealmException ex) {
            //throw new RuntimeException(ex);
            report.setFailureCause(ex);
            report.setActionExitCode(ExitCode.FAILURE);
        } catch (NoSuchRealmException ex) {
            //throw new RuntimeException(ex);
            report.setFailureCause(ex);
            report.setActionExitCode(ExitCode.FAILURE);
        } catch (Exception ex) {
            report.setFailureCause(ex);
            report.setActionExitCode(ExitCode.FAILURE);
        }

    }

    private void setRestartRequired(ActionReport report) {
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ActionReport.MessagePart mp = report.getTopMessagePart();

        Properties extraProperties = new Properties();
        Map<String, Object> entity = new HashMap<String, Object>();
        mp.setMessage(_localStrings.getLocalString("RESTART_REQUIRED",
                "Restart required for configuration updates to active server realm: {0}.",
                new Object[]{realmName}));
        entity.put("restartRequired", "true");
        extraProperties.put("entity", entity);
        ((ActionReport) report).setExtraProperties(extraProperties);
    }

    private boolean instantiateRealm(Config cfg, String realmName)
            throws BadRealmException, NoSuchRealmException {
        List<AuthRealm> authRealmConfigs = cfg.getSecurityService().getAuthRealm();
        for (AuthRealm authRealm : authRealmConfigs) {
            if (realmName.equals(authRealm.getName())) {
                List<Property> propConfigs = authRealm.getProperty();
                Properties props = new Properties();
                for (Property p : propConfigs) {
                    String value = p.getValue();
                    props.setProperty(p.getName(), value);
                }
                Realm.instantiate(authRealm.getName(), authRealm.getClassname(), props, cfg.getName());
                return true;
            }
        }
        throw new NoSuchRealmException(
                _localStrings.getLocalString("NO_SUCH_REALM", "No Such Realm: {0}",
                new Object[]{realmName}));
    }
}
