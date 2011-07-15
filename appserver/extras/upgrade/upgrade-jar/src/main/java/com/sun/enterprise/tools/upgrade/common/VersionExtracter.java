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


import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.tools.upgrade.logging.*;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class VersionExtracter {

    private static final StringManager stringManager =
        StringManager.getManager(VersionExtracter.class);
    private static final Logger logger = LogService.getLogger();
    private final CommonInfoModel common;

    public VersionExtracter(CommonInfoModel common) {
        this.common = common;
    }

    /**
     * Method to put together the version and edition (if any) in a simple format.
     */
    public String formatVersionEditionStrings(String v, String e) {
        return v + UpgradeConstants.DELIMITER + e;
    }
  
    /**
     * Method to determine the version/edition information from the config file.
     */
    public String extractVersionFromConfigFile(String cfgFilename) {
        String verEdStr = null;
        File configFile = new File(cfgFilename);
        if (!configFile.exists() || !configFile.isFile()) {
            return null;
        }

        UpgradeUtils upgrUtils = UpgradeUtils.getUpgradeUtils(common);
        Document adminServerDoc =
            upgrUtils.getDomainDocumentElement(cfgFilename);
        if (adminServerDoc == null) {
            return null;
        }

        try {
            String versionString = null;
            String publicID = adminServerDoc.getDoctype().getPublicId();
            String appservString = stringManager.getString(
                "common.versionextracter.appserver.string");
            int indx = publicID.indexOf(appservString);
            if (indx > -1) {
                //- product version is 1st token after the appserver text.
                String tmpS = publicID.substring(indx + appservString.length()).trim();
                String[] s = tmpS.split(" ");
                versionString = s[0];
            }

            verEdStr = formatVersionEditionStrings(versionString, UpgradeConstants.ALL_PROFILE);

        } catch (Exception ex) {
            //- Very basic check that this contain V3 domain XML.
            Element rootElement = adminServerDoc.getDocumentElement();
            if (!"domain".equals(rootElement.getTagName())) {
                logger.log(Level.SEVERE, stringManager.getString("common.versionextracter.dtd_product_version_find_failured"), ex);
            } else {
                verEdStr = formatVersionEditionStrings(UpgradeConstants.VERSION_3_1, UpgradeConstants.ALL_PROFILE);
            }
        }
        return verEdStr;
    }
}
