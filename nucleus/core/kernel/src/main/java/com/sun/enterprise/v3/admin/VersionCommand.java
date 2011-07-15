/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.ActionReport.ExitCode;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;
import com.sun.appserv.server.util.Version;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.Param;

/**
 * Return the version and build number
 *
 * @author Jerome Dochez
 */
@Service(name="version")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
@I18n("version.command")
public class VersionCommand implements AdminCommand {
    
    @Param(optional=true, defaultValue="false", shortName = "v")
    Boolean verbose;
    
    @Inject
    Version version;

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(VersionCommand.class);

    public void execute(AdminCommandContext context) {
        String vers;
        if (verbose) {
            vers = strings.getLocalString("version.verbose",
                "{0}, JRE version {1}",
                version.getFullVersion(), System.getProperty("java.version"));
        } else {
            vers = strings.getLocalString("version",
                "{0}", version.getFullVersion());
        }
        ActionReport report = context.getActionReport();
        report.setActionExitCode(ExitCode.SUCCESS);
        report.setMessage(vers);
    }
}
