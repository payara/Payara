/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.elasticity.engine.commands;

import com.sun.jdi.VirtualMachine;
import org.glassfish.elasticity.config.serverbeans.*;
import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.util.StringUtils;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.Services;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import java.util.logging.Logger;
//import org.glassfish.virtualization.libvirt.*;
//import org.glassfish.virtualization.spi.*;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 8/24/11
 * Time: 3:57 PM
 * To change this template use File | Settings | File Templates.
 */
@Service(name = "test-alert")
@Scoped(PerLookup.class)
public class testElasticity implements AdminCommand {

@Inject
  ElasticServices elasticServices;

  @Param(name="name", primary = true)
   String servicename;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        ElasticService es= elasticServices.getElasticService(servicename);
        if (es == null) {
            //node doesn't exist
            String msg = Strings.get("noSuchService", servicename);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        System.out.println("enabled "+es.getEnabled());
        System.out.println("service min "+es.getMin());
        System.out.println("service max "+es.getMax());

        Alerts alerts= es.getAlerts();
        if (alerts != null) {
        for (AlertConfig a : alerts.getAlert()) {
            System.out.println("alert name "+a.getName());
            System.out.println("Schedule "+ a.getSchedule());
        }
        }

        MetricGatherers mgs = es.getMetricGatherers();
        if (mgs != null ){
        for (MetricGatherer mg: mgs.getMetricGatherer()){
            System.out.println("metric gatherer type "+ mg.getName());
            System.out.println("metric gatherer rate "+ mg.getCollectionRate());
        }
        }

        Actions ac = es.getActions();
        if (ac != null){
        for (LogAction la: ac.getLogAction())  {
            System.out.println("name "+ la.getName());
            System.out.println("log-level "+ la.getLogLevel());
        }
        }
/*        LibVirtVirtualMachine lvvm = new LibVirtVirtualMachine() ;
        VirtualMachineInfo vmi =  lvvm.getInfo();
        try {
            System.out.println("cpu "+ vmi.cpuTime());
        } catch ( org.glassfish.virtualization.spi.VirtException ex)     {
            System.out.print("exception getting cpu numbers");
        }                                        */

        System.out.println("done");



    }
}
