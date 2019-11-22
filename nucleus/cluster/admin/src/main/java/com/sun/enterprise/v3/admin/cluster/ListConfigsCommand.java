/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.v3.admin.cluster;



import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.StringUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.config.ReferenceContainer;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;

import org.glassfish.api.admin.*;

/**
 *  This is a remote command that lists the configs.
 * Usage: list-config

 * @author Bhakti Mehta
 */
@Service(name = "list-configs")
@I18n("list.configs.command")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS})
@TargetType(value={CommandTarget.CLUSTER,
    CommandTarget.CONFIG, CommandTarget.DAS, CommandTarget.DOMAIN, CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTERED_INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean=Configs.class,
        opType=RestEndpoint.OpType.GET,
        path="list-configs",
        description="list-configs")
})
public final class ListConfigsCommand implements AdminCommand {

    @Inject
    private Domain domain;

    @Param(optional = true, primary = true, defaultValue = "domain")
    private String target;

    @Inject
    private Configs allConfigs;

    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        List<Config> configList = null;
        //Fix for issue 13356 list-configs doesn't take an operand
        //defaults to domain
        if (target.equals("domain" )) {
            Configs configs = domain.getConfigs();
            configList = configs.getConfig();
        } else {
            configList = createConfigList();

            if (configList == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(Strings.get("list.instances.badTarget", target));
                return;
            }
        }

        StringBuilder sb = new StringBuilder();
        List<String> configNames = new ArrayList<>();
        for (Config config : configList) {
            sb.append(config.getName()).append('\n');
            configNames.add(config.getName());
        }
        String output = sb.toString();
        //Fix for isue 12885
        report.addSubActionsReport().setMessage(output.substring(0,output.length()-1 ));

        Properties extraProperties = new Properties();
        extraProperties.put("configNames", configNames);
        report.setExtraProperties(extraProperties);

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    /*
    * if target was junk then return all the configs
    */
    private List<Config> createConfigList() {
        // 1. no target specified
        if (!StringUtils.ok(target))
            return allConfigs.getConfig();

        Config c = domain.getConfigNamed(target);
        if (c != null) {
            List<Config> cl = new LinkedList<Config>();
            cl.add(c);
            return cl;
        }

        ReferenceContainer rc = domain.getReferenceContainerNamed(target);
        if (rc == null) return null;

        if (rc.isServer()) {
            Server s =((Server) rc);
            List<Config> cl = new LinkedList<Config>();
            cl.add(s.getConfig());
            return  cl;
        }
        else if (rc.isCluster()) {
            Cluster cluster = (Cluster) rc;
            List<Config> cl = new LinkedList<Config>();
            cl.add(domain.getConfigNamed(cluster.getConfigRef()));
            return cl;
        }
        else return null;
    }

}
