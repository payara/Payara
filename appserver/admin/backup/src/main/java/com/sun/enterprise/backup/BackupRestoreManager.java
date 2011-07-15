/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.backup;

import com.sun.enterprise.util.io.FileUtils;
import java.io.*;

/**
 * Baseclass for BackupManager and RestoreManager.  Common code between 
 * the two goes in here.
 * @author  Byron Nevins
 */

abstract class BackupRestoreManager {

    public BackupRestoreManager(BackupRequest req) throws BackupException {
        if(req == null)
            throw new BackupException("backup-res.InternalError", 
                getClass().getName() + ".ctor: null BackupRequest object");
		
        this.request = req;
        init();
        LoggerHelper.finest("Request DUMP **********\n" + req);
    }
	
    void init() throws BackupException {

        // only do once!
        if(wasInitialized)
            return;
		
        if(request == null)
            throw new BackupException("backup-res.InternalError",
                                      "null BackupRequest reference");
		
        // add a timestamp
        request.timestamp = System.currentTimeMillis();
                
        // validate domains dir
        if (request.domainsDir == null ||
            !FileUtils.safeIsDirectory(request.domainsDir))
            throw new BackupException("backup-res.NoDomainsDir",
                                      request.domainsDir);
				
        if (request.domainName != null)
            request.domainDir = new File(request.domainsDir, request.domainName);

        LoggerHelper.setLevel(request);
    }

    /**
     * If both the backupDir and backupConfig are not set then this method
     * behaves as it did in v2.  It returns a path to the 
     * domainDir + BACKUP_DIR (backups).
     * If a backupConfig has been associated with the request and the
     * backupDir is not set then it returns a path to domainDir + backupConfig.
     * If a backupConfig has been associated with the request and the
     * backupDir is set then it returns a path to backupDir + domainName +
     * backupConfig.
     * If a backupConfig has not been associated with the request and the
     * backupDir is set then it returns a path to backupDir + domainName.
     */
    protected File getBackupDirectory(BackupRequest request) {
        File backupDir = null;

        // The v2 case.
        if (request.backupDir == null && request.backupConfig == null) {
            return (new File(request.domainDir, Constants.BACKUP_DIR));
        }

        if (request.backupDir == null && request.backupConfig != null) {
            return (new File(new File(request.domainDir, Constants.BACKUP_DIR),
                             request.backupConfig));
        }

        if (request.backupDir != null && request.backupConfig != null) {
            return (new File(new File(request.backupDir, request.domainName),
                             request.backupConfig));
        }

        // backupDir != null && backupConfig == null
        return (new File(request.backupDir, request.domainName));
    }


    BackupRequest   request;
    private boolean wasInitialized = false;
}
