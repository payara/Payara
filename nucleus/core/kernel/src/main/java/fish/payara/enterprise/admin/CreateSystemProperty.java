/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2024 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.enterprise.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import com.sun.enterprise.config.serverbeans.SystemPropertyBag;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.v3.admin.CLIUtil;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Transaction;

import java.util.ArrayList;
import java.util.Collection;

@Service(name="create-system-property")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value={CommandTarget.CLUSTER,
    CommandTarget.CONFIG, CommandTarget.DAS, CommandTarget.DOMAIN, CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTERED_INSTANCE})
@I18n("create.system.property")
public class CreateSystemProperty implements AdminCommand, AdminCommandSecurity.Preauthorization, AdminCommandSecurity.AccessCheckProvider {
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateSystemProperty.class);

    @Param(optional=true, defaultValue=SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name="name_value", primary=true)
    String propertyAssignment;

    @Inject
    Domain domain;

    private SystemPropertyBag spb;

    @Override
    public void execute (AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        String[] assignment = propertyAssignment.split("=");
        String propertyName;
        String propertyValue;

        try {
            propertyName = assignment[0];
            propertyValue = assignment[1];
            ConfigSupport.apply(propertyBag -> {
                // update existing system property
                for (SystemProperty property : propertyBag.getSystemProperty()) {
                    if (property.getName().equals(propertyName)) {
                        Transaction transaction = Transaction.getTransaction(propertyBag);
                        property = transaction.enroll(property);
                        property.setValue(propertyValue);
                        return property;
                    }
                }

                // create system-property
                SystemProperty newSysProp = propertyBag.createChild(SystemProperty.class);
                newSysProp.setName(propertyName);
                newSysProp.setValue(propertyValue);
                propertyBag.getSystemProperty().add(newSysProp);
                return newSysProp;
            }, spb);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch(Exception e) {
            report.setMessage(localStrings.getLocalString("create.system.property.failed",
                "System property {0} creation failed", propertyAssignment));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }

    @Override
    public Collection<? extends AccessRequired.AccessCheck<?>> getAccessChecks () {
        final Collection<AccessRequired.AccessCheck<?>> result = new ArrayList<>();
        result.add(new AccessRequired.AccessCheck<>(AccessRequired.Util.resourceNameFromConfigBeanProxy(spb), "update"));
        return result;
    }

    @Override
    public boolean preAuthorization (AdminCommandContext context) {
        this.spb = CLIUtil.chooseTarget(domain, target);
        if (this.spb == null) {
            final ActionReport report = context.getActionReport();
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            String msg = localStrings.getLocalString(
                "invalid.target.sys.props",
                "Invalid target:{0}. Valid targets types are domain, config, cluster, default server, clustered instance, stand alone instance", target
            );
            report.setMessage(msg);
            return false;
        }
        return true;
    }
}
