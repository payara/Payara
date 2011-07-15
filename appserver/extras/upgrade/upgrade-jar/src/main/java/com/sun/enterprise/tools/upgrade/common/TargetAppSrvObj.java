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

package com.sun.enterprise.tools.upgrade.common;

import com.sun.enterprise.tools.upgrade.logging.LogService;
import com.sun.enterprise.util.i18n.StringManager;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rebeccas
 */
public class TargetAppSrvObj extends BaseDomainInfoObj {

    private static final Logger logger = LogService.getLogger();

    private static final StringManager sm =
        StringManager.getManager(TargetAppSrvObj.class);;

    // used only in interactive cases where the user can resolve a name conflict
    private DirectoryMover directoryMover = null;

    @Override
    public boolean isValidPath(String s) {
        File targetPathDir = new File(s);
        
        // fail if path doesn't exist
        if (!targetPathDir.exists()) {
            logger.log(Level.INFO, sm.getString(
                "enterprise.tools.upgrade.target.dir_does_not_exist",
                targetPathDir.getAbsolutePath()));
            return false;
        }
        
        File tmpPath = new File(targetPathDir,
            CommonInfoModel.getInstance().getSource().getDomainName());
        if (!tmpPath.exists()) {
            return true;
        }
        if (directoryMover != null &&
                directoryMover.moveDirectory(tmpPath)) {
            return true;
        }
        logger.log(Level.INFO, sm.getString(
            "enterprise.tools.upgrade.target.dir_domain_exist",
            tmpPath.getAbsolutePath()));
        return false;
    }

    @Override
    public void setInstallDir(String s) {
        super.installDir = s;
        if (s != null) {
            super.domainRoot = super.extractDomainRoot(s);
        }
    }
	
    @Override
    public String getDomainDir() {
        return getInstallDir() + "/" + super.domainName;
    }

    @Override
    public String getConfigXMLFile() {
        return getDomainDir() + "/" + super.CONFIG_DOMAIN_XML_FILE;
    }

    @Override
    public String getVersionEdition() {
        if (versionEdition == null) {
            VersionExtracter vExtracter = new VersionExtracter(
                CommonInfoModel.getInstance());
            version = UpgradeConstants.VERSION_3_1;
            edition = UpgradeConstants.ALL_PROFILE;
            versionEdition = vExtracter.formatVersionEditionStrings(
                version, edition);
        }
        return super.versionEdition;
    }
		
    /*
     * Set this if you want the user to have a chance to resolve
     * directory name conflicts.
     */
    public void setDirectoryMover(DirectoryMover directoryMover) {
        this.directoryMover = directoryMover;
    }

}
