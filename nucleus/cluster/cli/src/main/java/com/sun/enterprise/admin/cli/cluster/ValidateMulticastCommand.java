/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli.cluster;

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.gms.tools.MulticastTester;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.util.ArrayList;
import java.util.List;

/**
 * asadmin local command that wraps the multicast validator tool
 * in shoal-gms-impl.jar
 */
@Service(name="validate-multicast")
@PerLookup
public final class ValidateMulticastCommand extends CLICommand {

    @Param(name="multicastport", optional=true)
    private String port;

    @Param(name="multicastaddress", optional=true)
    private String address;

    @Param(name="bindaddress", optional=true)
    private String bindInterface;

    @Param(name="sendperiod", optional=true)
    private String period;

    @Param(name="timeout", optional=true)
    private String timeout;

    @Param(name="timetolive", optional=true)
    private String ttl;

    @Param(optional=true, shortName="v", defaultValue="false")
    private boolean verbose;

    @Override
    protected int executeCommand() throws CommandException {
        MulticastTester mt = new MulticastTester();
        return mt.run(createArgs());
    }

    private String [] createArgs() {
        List<String> argList = new ArrayList<String>();
        if (port != null && !port.isEmpty()) {
            argList.add(MulticastTester.PORT_OPTION);
            argList.add(port);
        }
        if (address != null && !address.isEmpty()) {
            argList.add(MulticastTester.ADDRESS_OPTION);
            argList.add(address);
        }
        if (bindInterface != null && !bindInterface.isEmpty()) {
            argList.add(MulticastTester.BIND_OPTION);
            argList.add(bindInterface);
        }
        if (period != null && !period.isEmpty()) {
            argList.add(MulticastTester.WAIT_PERIOD_OPTION);
            argList.add(period);
        }
        if (timeout != null && !timeout.isEmpty()) {
            argList.add(MulticastTester.TIMEOUT_OPTION);
            argList.add(timeout);
        }
        if (ttl != null && !ttl.isEmpty()) {
            argList.add(MulticastTester.TTL_OPTION);
            argList.add(ttl);
        }
        if (verbose) {
            argList.add(MulticastTester.DEBUG_OPTION);
        }
        return argList.toArray(new String[argList.size()]);
    }
}
