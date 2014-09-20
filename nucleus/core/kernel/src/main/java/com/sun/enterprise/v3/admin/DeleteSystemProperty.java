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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommandSecurity;

/**
 * Delete System Property Command
 * 
 * Removes one system property of the domain, configuration, cluster, or server 
 * instance, at a time
 * 
 * Usage: delete-system-property [--terse=false] [--echo=false] [--interactive=true] 
 * [--host localhost] [--port 4848|4849] [--secure|-s=true] [--user admin_user] [
 * --passwordfile file_name] [--target target(Default server)] property_name
 * 
 */
@Service(name="delete-system-property")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value={CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE,
CommandTarget.CONFIG, CommandTarget.DAS, CommandTarget.DOMAIN, CommandTarget.STANDALONE_INSTANCE})
@I18n("delete.system.property")
public class DeleteSystemProperty implements AdminCommand,
        AdminCommandSecurity.Preauthorization, AdminCommandSecurity.AccessCheckProvider {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeleteSystemProperty.class);

    @Param(optional=true, defaultValue=SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name="property_name", primary=true)
    String propName;
    
    @Inject
    Domain domain;

    private SystemPropertyBag spb;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        spb = CLIUtil.chooseTarget(domain, target);
        if (spb == null) {
            final ActionReport report = context.getActionReport();
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            String msg = localStrings.getLocalString("invalid.target.sys.props",
                    "Invalid target:{0}. Valid targets types are domain, config, cluster, default server, clustered instance, stand alone instance", target);
            report.setMessage(msg);
            return false;
        }
        return true;
    }

    @Override
    public Collection<? extends AccessRequired.AccessCheck> getAccessChecks() {
        final Collection<AccessRequired.AccessCheck> result = new ArrayList<AccessRequired.AccessCheck>();
        result.add(new AccessRequired.AccessCheck(AccessRequired.Util.resourceNameFromConfigBeanProxy(spb), "update"));
        return result;
    }

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        Property domainProp = domain.getProperty("administrative.domain.name");
        String domainName = domainProp.getValue();
        if(!spb.containsProperty(propName)) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            String msg = localStrings.getLocalString("no.such.property",
                    "System Property named {0} does not exist at the given target {1}", propName, target);
            report.setMessage(msg);
            return;
        }
        if (definitions(propName) == 1) { //implying user is deleting the "last" definition of this property
            List<String> refs = new ArrayList<String>();
            List<Dom> doms = new ArrayList<Dom>();
            if ("domain".equals(target) || target.equals(domainName)) {
                for (Server s : domain.getServers().getServer()) {
                    Config config = s.getConfig();
                    Cluster cluster = s.getCluster();
                    if (!s.containsProperty(propName) && !config.containsProperty(propName)) {
                        if (cluster != null) {
                            if (!cluster.containsProperty(propName)) {
                                doms.add(Dom.unwrap(s));
                            }
                        } else {
                            doms.add(Dom.unwrap(s));
                        }
                    }
                }
            } else {
                Config config = domain.getConfigNamed(target);
                if (config != null) {
                    doms.add(Dom.unwrap(config));
                    String configName = config.getName();
                    for (Server s : domain.getServers().getServer()) {
                        String configRef = s.getConfigRef();
                        if (configRef.equals(configName)) {
                            if (!s.containsProperty(propName)) {
                                doms.add(Dom.unwrap(s));
                            }
                        }
                    }
                    for (Cluster c : domain.getClusters().getCluster()) {
                        String configRef = c.getConfigRef();
                        if (configRef.equals(configName)) {
                            if (!c.containsProperty(propName)) {
                                doms.add(Dom.unwrap(c));
                            }
                        }
                    }
                } else {
                    Cluster cluster = domain.getClusterNamed(target);
                    if (cluster != null) {
                        doms.add(Dom.unwrap(cluster));
                        Config clusterConfig = domain.getConfigNamed(cluster.getConfigRef());
                        doms.add(Dom.unwrap(clusterConfig));
                        for (Server s : cluster.getInstances()) {
                            if (!s.containsProperty(propName)) {
                                doms.add(Dom.unwrap(s));
                            }
                        }
                    } else {
                        Server server = domain.getServerNamed(target);
                        doms.add(Dom.unwrap(server));
                        doms.add(Dom.unwrap(domain.getConfigNamed(server.getConfigRef())));
                    }
                }
            }
            String sysPropName = SystemPropertyConstants.getPropertyAsValue(propName);
            for (Dom d : doms) {
                listRefs(d, sysPropName, refs);
            }
            if (!refs.isEmpty()) {
                //there are some references
                String msg = localStrings.getLocalString("cant.delete.referenced.property",
                        "System Property {0} is referenced by {1} in the configuration. Please remove the references first.", propName, Arrays.toString(refs.toArray()));
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        //now we are sure that the target exits in the config, just remove the given property
        try {
            ConfigSupport.apply(new SingleConfigCode<SystemPropertyBag>() {
                public Object run(SystemPropertyBag param) throws PropertyVetoException, TransactionFailure {
                    param.getSystemProperty().remove(param.getSystemProperty(propName));
                    return param;
                }
            }, spb);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            String msg = localStrings.getLocalString("delete.sysprops.ok",
                    "System Property named {0} deleted from given target {1}. Make sure you check its references.", propName, target);
            report.setMessage(msg);
        } catch (TransactionFailure tf) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tf);
        }
    }

    private int definitions(String propName) {
        //are there multiple <system-property> definitions for the given name?
        int defs = 0;
        SystemPropertyBag bag = domain;
        if (bag.containsProperty(propName))
            defs++;
        
        bag = domain.getServerNamed(target);
        if (bag != null && bag.containsProperty(propName)) {
            defs++;
            Server server = (Server)bag;
            Cluster cluster = server.getCluster();
            if (cluster != null && cluster.containsProperty(propName))
                defs++;
            if (server.getConfig().containsProperty(propName))
                defs++;
        }
        
        bag = domain.getClusterNamed(target);
        if (bag != null && bag.containsProperty(propName)) {
            defs++;
            Cluster c = (Cluster)bag;
            Config clusterConfig = domain.getConfigNamed(c.getConfigRef());
            if (clusterConfig.containsProperty(propName))
                defs++;
        }

        bag = domain.getConfigNamed(target);
        if (bag != null && bag.containsProperty(propName))
            defs++;

        return defs;
    }

    private static void listRefs(Dom dom, String value, List<String> refs) {
        //this method is rather ugly, but it works. See 9340 which presents a compatibility issue
        //frankly, it makes no sense to do an extensive search of all references of <system-property> being deleted,
        //but that's what resolution of this issue demands. --- Kedar 10/5/2009
        for (String aname : dom.getAttributeNames()) {
            String raw = dom.rawAttribute(aname);
            if (raw != null && raw.equals(value)) {
                refs.add(dom.model.getTagName() + ":" + aname);
            }
        }
        for (String ename : dom.getElementNames()) {
            List<Dom> nodes = null;
            try {
                nodes = dom.nodeElements(ename);
            } catch(Exception e) {
                //ignore, in some situations, HK2 might throw ClassCastException here
            }
            if (nodes != null) {
                for (Dom node : nodes)
                    listRefs(node, value, refs);  //beware: recursive call ...
            }
        }
    }
}
