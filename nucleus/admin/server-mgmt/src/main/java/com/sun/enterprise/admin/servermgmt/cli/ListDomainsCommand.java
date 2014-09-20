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
package com.sun.enterprise.admin.servermgmt.cli;

import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.DomainsManager;
import com.sun.enterprise.admin.servermgmt.pe.PEDomainsManager;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.HostAndPort;
import com.sun.enterprise.util.io.DomainDirs;
import java.io.File;
import java.io.IOException;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.*;

/**
 * This is a local command that lists the domains.
 */
@Service(name = "list-domains")
@PerLookup
public final class ListDomainsCommand extends LocalDomainCommand {
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(ListDomainsCommand.class);
    private String domainsRoot = null;

    @Param(name="long", shortName="l", optional=true)
    boolean longOpt;
    
    @Param(shortName="h", optional=true, defaultValue="true")
    boolean header;
    
    /*
     * Override the validate method because super.validate() calls initDomain,
     * and since we don't have a domain name yet, we aren't ready to call that.
     */
    @Override
    protected void validate()
            throws CommandException, CommandValidationException {
    }

    @Override
    protected int executeCommand() throws CommandException, CommandValidationException {
        try {
            File domainsDirFile = ok(domainDirParam)
                    ? new File(domainDirParam) : DomainDirs.getDefaultDomainsDir();

            DomainConfig domainConfig = new DomainConfig(null, domainsDirFile.getAbsolutePath());
            DomainsManager manager = new PEDomainsManager();
            String[] domainsList = manager.listDomains(domainConfig);
            programOpts.setInteractive(false);  // no prompting for passwords
            if (domainsList.length > 0) {
                if (longOpt) {
                    String headings[] = {"DOMAIN", "ADMIN_HOST", "ADMIN_PORT", "RUNNING", "RESTART_REQUIRED"};
                    ColumnFormatter cf = header ? new ColumnFormatter(headings) : 
                            new ColumnFormatter();
                    for (String dn : domainsList) {
                        DomainInfo di = getStatus(dn);
                        cf.addRow(new Object[] {
                            dn, 
                            di.adminAddr.getHost(), 
                            di.adminAddr.getPort(), 
                            di.status,
                            di.restartRequired                              
                        });
                    }
                    logger.info(cf.toString());
                } else {
                    for (String dn : domainsList) {
                        logger.info(getStatus(dn).statusMsg);
                    }
                }               
            }
            else {
                logger.fine(strings.get("NoDomainsToList"));
            }
        }
        catch (Exception ex) {
            throw new CommandException(ex.getLocalizedMessage());
        }
        return 0;
    }

    static class DomainInfo {
        public HostAndPort adminAddr;
        public boolean status;
        public String statusMsg;
        public boolean restartRequired;
    }
    
    private DomainInfo getStatus(String dn) throws IOException, CommandException {
        setDomainName(dn);
        initDomain();
        DomainInfo di = new DomainInfo();
        di.adminAddr = getAdminAddress();
        programOpts.setHostAndPort(di.adminAddr);
        di.status = isThisDAS(getDomainRootDir());
        
        if (di.status) {
            di.statusMsg = strings.get("list.domains.StatusRunning", dn);
            try {
                RemoteCLICommand cmd =
                        new RemoteCLICommand("_get-restart-required",
                        programOpts, env);
                String restartRequired =
                        cmd.executeAndReturnOutput("_get-restart-required");
                di.restartRequired = Boolean.parseBoolean(restartRequired.trim());
                if (di.restartRequired) {
                    di.statusMsg = strings.get("list.domains.StatusRestartRequired", dn);
                }
            }
            catch (Exception ex) {
            }
        }
        else {
            di.statusMsg = strings.get("list.domains.StatusNotRunning", dn);
        }
        return di;
    }
}
