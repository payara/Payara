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

package com.sun.enterprise.admin.cli.optional;

import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import java.io.File;

import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.admin.cli.remote.RemoteCommand;
import com.sun.enterprise.backup.BackupException;
import com.sun.enterprise.backup.BackupManager;
import com.sun.enterprise.backup.BackupWarningException;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.ObjectAnalyzer;

/**
 * This is a local command for backing-up domains.
 * The Options:
 *  <ul>
 *  <li>domaindir
 *  </ul>
 * The Operand:
 *  <ul>
 *  <li>domain
 *  </ul>
 */
@Service(name = "backup-domain")
@PerLookup
public final class BackupDomainCommand extends BackupCommands {

    @Param(name = "description", optional = true)
    String description;

    @Param(name="_force",optional=true)
    String force;

    @Param(name="_recyclelimit",optional=true)
    String recycleLimit;

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(BackupDomainCommand.class);

    @Override
    protected void validate()
            throws CommandException {
        // only if domain name is not specified, it should try to find one
        if (domainName == null)
            super.validate();
        
        checkOptions();

        setDomainName(domainName);
        initDomain();

        File domainFile = new File(new File(domainDirParam), domainName);

        if (!isWritableDirectory(domainFile)) {
            throw new CommandException(
                strings.get("InvalidDirectory", domainFile.getPath()));
        }

        if (force == null ) {
            if (isRunning()) {
                boolean suspendAvailable = canSuspend();

                if (suspendAvailable && !isSuspended()) {
                    throw new CommandException(
                        strings.get("DomainIsNotSuspended", domainName));
                } else if (!suspendAvailable) {
                    throw new CommandException(strings.get("DomainIsNotStopped",
                        domainName));
                }
            } 
        }

        int limit = 0;
        if (recycleLimit != null ) {
            try {
                limit = Integer.parseInt(recycleLimit.trim());
            } catch (NumberFormatException ex) {
                limit = -1;
            }
            if (limit < 0) {
                throw new CommandException(
                        strings.get("InvalidBackupRecycleLimit", recycleLimit));
            }
        }

        setDescription(description);
        setBackupDir(backupdir);
        setRecycleLimit(limit);
        prepareRequest();
        initializeLogger();     // in case program options changed
    }

    /**
     */
    @Override
    protected int executeCommand()
            throws CommandException {

        try {            
            BackupManager mgr = new BackupManager(request);
            logger.info(mgr.backup());            
        } catch (BackupWarningException bwe) {
            logger.info(bwe.getMessage());
        } catch (BackupException be) {
            throw new CommandException(be);
        }
        return 0;
    }

    @Override
    public String toString() {
        return super.toString() + "\n" + ObjectAnalyzer.toString(this);
    }

    /**
     * This method determines if the DAS has the ability to suspend itself.
     */
    private boolean canSuspend() {

        try {
            RemoteCLICommand cmd = new RemoteCLICommand("list-commands", 
                                                  programOpts, env);
            String response = cmd.executeAndReturnOutput("list-commands");

            if (response.indexOf("suspend-domain") >= 0)
                return true;
        } catch (Exception e) {
            logger.info("Exception while probing DAS (list-commands): " +
                e.getMessage());
        }

        return false;
    } 

    /**
     * This method determines if the DAS is currently suspended.
     */
    private boolean isSuspended() {

        try {
            RemoteCLICommand cmd = new RemoteCLICommand("suspend-domain", 
                                                  programOpts, env);
            String response = cmd.executeAndReturnOutput("suspend-domain",
                                                         "--_test=true");

            if (response.indexOf("SUSPENDED=TRUE") >= 0)
                return true;

        } catch (Exception e) {
            logger.info("Exception while probing DAS (suspend-domain): " +
                e.getMessage());
        }

        return false;
    } 
}
