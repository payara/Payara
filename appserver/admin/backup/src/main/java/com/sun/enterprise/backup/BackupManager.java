/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.backup.util.BackupUtils;
import com.sun.enterprise.util.io.FileUtils;
import java.io.*;
import java.util.List;
import java.util.Date;

/**
 *
 * @author  Byron Nevins
 */


public class BackupManager extends BackupRestoreManager {
    public BackupManager(BackupRequest req) throws BackupException {
        super(req);
    }

    //////////////////////////////////////////////////////////////////////

    public final String backup() throws BackupException {
        StringBuilder mesg = new StringBuilder();
        String statusString = writeStatus();


        if (!request.terse) {
            String backupTime = new Date(request.timestamp).toString();

            mesg.append(StringHelper.get("backup-res.SuccessfulBackup",
                                    request.domainName, backupTime));
        }
        
        try {
            ZipStorage zs = new ZipStorage(request);
            zs.store();
            // TODO: RSH - Recycle files. I'm not sure if this is the precise
            // place to do the recycling, but we probably need to do it somewhere
            // in this module since BackupFilenameManager is module private. We
            // should do the recycling after the backup completes. I think it
            // should be safe to recycle after a successful or unsuccessful backup
            // (assuming a failed backup doesn't leave a corrupt ZIP file).
            BackupFilenameManager bfm =
                new BackupFilenameManager(getBackupDirectory(request),
                                          request.domainName);
            List<File> recycleFiles = bfm.getRecycleFiles(request.recycleLimit);
            if (recycleFiles.size() > 0 && request.verbose) {
                mesg.append("\n");
                mesg.append(StringHelper.get("backup-res.recycle",
                                                request.recycleLimit));
                mesg.append("\n");
            }

            for (File f : recycleFiles) {
                if (request.verbose) {
                    mesg.append(StringHelper.get("backup-res.recycleDelete", f));
                    mesg.append("\n");
                }
                if (!f.delete()) {
                    mesg.append(StringHelper.get("backup-res.recycleBadDelete", f));
                    mesg.append("\n");
                }
            }

            if (request.verbose) {
                mesg.append("\n\n");
                mesg.append(statusString);
            }

            //XXX: This needs to be fixed such that if an error occurs
            //     it is propogated up such that the command exits with
            //     the proper exit code.
            return mesg.toString();
        }
        finally {
            status.delete();
            BackupUtils.protect(request.backupFile);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////

    void init() throws BackupException {
        super.init();
        
        if(request.backupFile != null)
            throw new BackupException("backup-res.InternalError",
                "No backupFilename may be specified for a backup -- it is reserved for restore operations only.");
        
        if(!FileUtils.safeIsDirectory(request.domainDir))
            throw new BackupException("backup-res.NoDomainDir", 
                                      request.domainDir);

        File backupDir = getBackupDirectory(request);

        // mkdirs may fail if the directory exists or it could not be created.
        if (!backupDir.mkdirs()) {
            // If it doesn't exist then it is an error.
            if(!FileUtils.safeIsDirectory(backupDir))
                throw new BackupException("backup-res.NoBackupDirCantCreate",
                                      backupDir);
	}

        BackupFilenameManager bfmgr = 
            new BackupFilenameManager(backupDir, request.domainName);
        request.backupFile = bfmgr.next();        

        // get customized description if user hasn't specified one
        if(request.description == null || request.description.length() <= 0)
            request.description = bfmgr.getCustomizedDescription();
    }

    ///////////////////////////////////////////////////////////////////////////
    
    private String writeStatus() {
        status = new Status();
        return status.write(request);
    }
    
    ///////////////////////////////////////////////////////////////////////////

       Status status;
}
