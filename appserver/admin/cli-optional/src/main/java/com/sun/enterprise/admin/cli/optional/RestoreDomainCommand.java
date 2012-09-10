/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;

import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.backup.BackupException;
import com.sun.enterprise.backup.BackupRequest;
import com.sun.enterprise.backup.BackupWarningException;
import com.sun.enterprise.backup.RestoreManager;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.ObjectAnalyzer;

/**
 * This is a local command for restoring domains.
 * The Options:
 *  <ul>
 *  <li>domaindir
 *  </ul>
 * The Operand:
 *  <ul>
 *  <li>domain_name
 *  </ul>
 */
@Service(name = "restore-domain")
@PerLookup
public final class RestoreDomainCommand extends BackupCommands {

    @Param(name = "filename", optional = true)
    private String backupFilename;

    @Param(name= "force", optional = true, defaultValue = "false")
    private boolean force;

    @Param(name= "description", optional = true, obsolete = true)
    private String description;

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(BackupDomainCommand.class);

    /**
     */
    @Override
    protected void validate()
            throws CommandException, CommandValidationException {

        boolean domainExists = true;
        
        if (backupFilename == null && domainName == null) {
            if (!force) {
                throw new CommandException(strings.get("UseForceOption"));
            }
            // this will properly initialize the domain dir
            // see LocalDomainCommand.initDomain())
            super.validate();
        }

        checkOptions();

        try {
            setDomainName(domainName);
            initDomain();
        } catch (CommandException e) {
            if (e.getCause() != null &&
                (e.getCause() instanceof java.io.IOException)) {
                // The domain does not exist which is allowed if the
                // force option is used (checked later).
                domainExists = false;
            } else {
                throw e;
            }
        }

        if (domainExists && isRunning()) {
            throw new CommandException(strings.get("DomainIsNotStopped",
                                       domainName));
        }

        if (backupFilename != null) {
            File f = new File(backupFilename);

            if (!f.exists()) {
                throw new CommandValidationException(
                    strings.get("FileDoesNotExist", backupFilename));
            }

            if (!f.canRead()) {
                throw new CommandValidationException(
                    strings.get("FileCanNotRead", backupFilename));
            }

            if (f.isDirectory()) {
                throw new CommandValidationException(
                    strings.get("FileIsDirectory", backupFilename));
            }
        }

        setBackupDir(backupdir);
        initRequest();

        initializeLogger();     // in case program options changed
    }
 
    /**
     */
    @Override
    protected int executeCommand()
            throws CommandException {
        try {            
            RestoreManager mgr = new RestoreManager(request);
            logger.info(mgr.restore());
        } catch (BackupWarningException bwe) {
            logger.info(bwe.getMessage());
        } catch (BackupException be) {
            throw new CommandException(be);
        }
        return 0;
    }

    private void initRequest() throws CommandValidationException {

        File backupdir_f = null;
        if (backupdir != null) {
            backupdir_f = new File(backupdir);
            if (!backupdir_f.isAbsolute()) {
                throw new CommandValidationException(
                    strings.get("InvalidBackupDirPath", backupdir));
            }
        }
        boolean configonlybackup = false;
        if ((configonly != null) && ( Boolean.valueOf(configonly))) {
            configonlybackup = true;
        }
        request = new BackupRequest(domainDirParam, domainName, backupdir_f,
                                    backupConfig, backupFilename,configonlybackup);
        request.setTerse(programOpts.isTerse());
        request.setVerbose(verbose);
        request.setForce(force);
    }

    @Override
    public String toString() {
        return super.toString() + "\n" + ObjectAnalyzer.toString(this);
    }

}
