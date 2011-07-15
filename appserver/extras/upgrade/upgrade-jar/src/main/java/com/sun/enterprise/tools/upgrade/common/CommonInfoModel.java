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

import java.util.logging.Logger;
import com.sun.enterprise.tools.upgrade.logging.LogService;
import com.sun.enterprise.util.i18n.StringManager;

/**
 *
 * author : Gautam Borah
 *
 */
public class CommonInfoModel{
	
    //Logging fields
    private static final StringManager stringManager =
        StringManager.getManager(CommonInfoModel.class);
    private static final Logger logger = LogService.getLogger();

    // singleton
    private static final CommonInfoModel instance = new CommonInfoModel();

    private static final String [] SUPPORTED_VERSIONS = {
        UpgradeConstants.VERSION_91,
        UpgradeConstants.VERSION_3_0,
        UpgradeConstants.VERSION_3_0_1,
        UpgradeConstants.VERSION_3_1
    };

    private TargetAppSrvObj tAppSrvObj = new TargetAppSrvObj();
    private SourceAppSrvObj sAppSrvObj = new SourceAppSrvObj();
    private boolean alreadyCloned = false;
    private CommonInfoModel() {}

    public static CommonInfoModel getInstance() {
        return instance;
    }
    
    public SourceAppSrvObj getSource() {
        return sAppSrvObj;
    }

    public TargetAppSrvObj getTarget() {
        return tAppSrvObj;
    }
	
    public void setupTasks() throws Exception {
        String domainName = sAppSrvObj.getDomainName();

        //- identify target domain to upgrade
        tAppSrvObj.setDomainName(domainName);

        if (!alreadyCloned) {
            UpgradeUtils.getUpgradeUtils(this).cloneDomain(
                sAppSrvObj.getInstallDir(), tAppSrvObj.getDomainDir());
        } else {
            alreadyCloned = false; // reset for next upgrade
        }
    }
	
    /*
     * Called when UpgradeUtils has already made a backup
     * before re-upgrading a domain.
     */
    public void setAlreadyCloned(boolean alreadyCloned) {
        this.alreadyCloned = alreadyCloned;
    }
	
    public boolean isUpgradeSupported() {
        String sourceVersion = sAppSrvObj.getVersion();
        String targetVersion = tAppSrvObj.getVersion();

        for (String version : SUPPORTED_VERSIONS) {
            if (version.equals(sourceVersion)) {
                return true;
        }
    }
	
        logger.info(stringManager.getString(
            "upgrade.common.upgrade_not_supported",
            sourceVersion, sAppSrvObj.getEdition(),
            targetVersion, tAppSrvObj.getEdition()));
        return false;
}

}
