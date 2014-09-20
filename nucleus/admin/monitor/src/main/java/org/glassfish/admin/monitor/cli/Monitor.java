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

package org.glassfish.admin.monitor.cli;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.util.LocalStringManagerImpl;

import javax.inject.Inject;
import java.util.Iterator;

/**
 * Return the version and build number
 *
 * @author Prashanth Abbagani
 */
@Service(name="monitor")
@PerLookup
@I18n("monitor.command")
public class Monitor implements AdminCommand {

    @Param(optional=true)
    private String type;

    @Param(optional=true)
    private String filter;

    @Inject
    private ServiceLocator habitat;

    final private LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(Monitor.class);


    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        MonitorContract mContract = null;
        for (MonitorContract m : habitat.<MonitorContract>getAllServices(MonitorContract.class)) {
            if ((m.getName()).equals(type)) {
                mContract = m;
                break;
            }
        }
        if (mContract != null) {
            mContract.process(report, filter);
            return;
        }
        if (habitat.getAllServices(MonitorContract.class).size() != 0) {
            StringBuffer buf = new StringBuffer();
            Iterator<MonitorContract> contractsIterator = habitat.
                    <MonitorContract>getAllServices(MonitorContract.class).iterator();
            while (contractsIterator.hasNext()) {
                buf.append(contractsIterator.next().getName());
                if (contractsIterator.hasNext()) {
                    buf.append(", ");
                }
            }
            String validTypes = buf.toString();
            report.setMessage(localStrings.getLocalString("monitor.type.error",
                "No type exists in habitat for the given monitor type {0}. " +
                "Valid types are: {1}", type, validTypes));
        } else {
            report.setMessage(localStrings.getLocalString("monitor.type.invalid",
                 "No type exists in habitat for the given monitor type {0}", type));
        }

        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
    }
}
