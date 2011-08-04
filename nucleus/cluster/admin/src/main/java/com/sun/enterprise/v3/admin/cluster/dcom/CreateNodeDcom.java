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
package com.sun.enterprise.v3.admin.cluster.dcom;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.*;
import java.util.logging.Logger;
import org.jvnet.hk2.component.PerLookup;

/**
 * Remote AdminCommand to create a DCOM node
 *
 * @author Byron Nevins
 */
@Service(name = "create-node-dcom")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS})
public class CreateNodeDcom implements AdminCommand {
    @Param(name="name", primary = true)
    private String name;

    @Param(name="nodehost")
    private String nodehost;

    @Param(name = "installdir", optional=true, defaultValue = "xxx")
    private String installdir;

    @Param(name="nodedir", optional=true)
    private String nodedir;

    @Param(name="dcomport", optional=true, defaultValue = "yyy")
    private String dcomport;

    @Param(name = "dcomuser", optional = true, defaultValue = "qqqq")
    private String dcomuser;

    @Param(name = "dcompassword", optional = true, password = true)
    private String dcompassword;

    @Param(name = "force", optional = true, defaultValue = "false")
    private boolean force;

    @Param(optional = true, defaultValue = "false")
    boolean install;

    @Param(optional = true)
    String archive;

    private ActionReport report;
    Logger logger;

    @Override
    public void execute(AdminCommandContext context) {
        report = context.getActionReport();
        logger = context.getLogger();
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        report.setMessage("create-node-dcom is under construction and not yet available for use.");
    }
}
