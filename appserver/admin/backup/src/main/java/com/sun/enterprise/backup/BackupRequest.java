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

/*
 * BackupRequest.java
 *
 * Created on February 22, 2004, 1:40 AM
 */

package com.sun.enterprise.backup;

import java.io.*;

import com.sun.enterprise.util.ObjectAnalyzer;
import com.sun.enterprise.util.io.FileUtils;

/**
 * This class holds all of the values that the caller needs.  
 * An instance of this class can be used to create a request object.
 * @author  bnevins
 */

public class BackupRequest {
    /**
     * Create an instance (generic)
     **/
    public BackupRequest(String domainsDirName, String domain, 
                         File backupDir, String backupConfig,boolean configonly) {
        setDomainsDir(domainsDirName);
        setBackupDir(backupDir);
        setBackupConfig(backupConfig);
        domainName = domain;
        configOnly = configonly;
    }

    /**
     * Create an instance (used by backup-domain and list-backups)
     **/
    public BackupRequest(String domainsDirName, String domain, 
                         File backupDir, String backupConfig,
                         String desc, int limit,boolean configonly) {
        this(domainsDirName, domain, backupDir, backupConfig,configonly);
        setDescription(desc);
        setRecycleLimit(limit);
    }
    
    /**
     * Create an instance (used by restore-domain)
     **/
    public BackupRequest(String domainsDirName, String domain, 
                         File backupDir, String backupConfig,
                         String backupFileName,boolean configonly) {
        this(domainsDirName, domain, backupDir, backupConfig,configonly);
        if (backupFileName != null)
            setBackupFile(backupFileName);
    }

    ///////////////////////////////////////////////////////////////////////////

    public void setTerse(boolean b) {
        terse = b;
    }
    
    public void setVerbose(boolean b) {
        verbose = b;
    }
    
    public String toString() {
        return ObjectAnalyzer.toString(this);
    }

    public void setForce(boolean f) {
        force = f;
    }

    ///////////////////////////////////////////////////////////////////////////
    
    private void setDomainsDir(String name) {
        domainsDir = FileUtils.safeGetCanonicalFile(new File(name));
    }

    private void setBackupDir(File dir) {
        backupDir = dir;
    }

    private void setRecycleLimit(int limit) {
        recycleLimit = limit;
    }

    private void setDescription(String desc) {
        description = desc;
    }

    private void setBackupFile(String name) {
        backupFile = FileUtils.safeGetCanonicalFile(new File(name));
    }

    private void setBackupConfig(String name) {
        backupConfig = name;
    }

    ///////////////////////////////////////////////////////////////////////////
    ////////////     Variables     ////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    
    final static String[] excludeDirs = {Constants.BACKUP_DIR + "/",
                                         Constants.OSGI_CACHE + "/"}; 

    File    domainsDir;
    String  domainName;
    String  description;
    int     recycleLimit = 0;
    File    backupDir = null;
    String  backupConfig = null;
    boolean configOnly = false;

    // VARIABLES POSSIBLY SET AT RUNTIME
    File    backupFile;
    
    // VARIABLES SET AT RUNTIME
    File    domainDir;
    long    timestamp;
    
    boolean terse = false;
    boolean verbose = false;
    boolean force = false;
}
