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

package com.sun.enterprise.security.auth.realm;

import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.security.SecurityLoggerInfo;

import org.jvnet.hk2.config.types.Property;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RealmConfig usable by standalone : Admin CLI for creating Realms
 * It has a subset of functionality defined in com.sun.enterprise.security.RealmConfig
 */
public class RealmConfig {

    private static Logger logger =
            SecurityLoggerInfo.getLogger();

    public static void createRealms(String defaultRealm, List<AuthRealm> realms) {
        createRealms(defaultRealm, realms, null);
    }
    public static void createRealms(String defaultRealm, List<AuthRealm> realms, String configName) {
        assert(realms != null);

        String goodRealm = null; // need at least one good realm

        for (AuthRealm aRealm : realms) {
            String realmName = aRealm.getName();
            String realmClass = aRealm.getClassname();
            assert (realmName != null);
            assert (realmClass != null);

            try {
                List<Property> realmProps = aRealm.getProperty();
                /*V3 Commented ElementProperty[] realmProps =
                    aRealm.getElementProperty();*/
                Properties props = new Properties();
                for (Property realmProp : realmProps) {
                    props.setProperty(realmProp.getName(), realmProp.getValue());
                }
                Realm.instantiate(realmName, realmClass, props, configName);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Configured realm: " + realmName);
                }

                if (goodRealm == null) {
                    goodRealm = realmName;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING,
                           SecurityLoggerInfo.realmConfigDisabledError, realmName);
                logger.log(Level.WARNING, SecurityLoggerInfo.securityExceptionError, e);
            }
        }

        // done loading all realms, check that there is at least one
        // in place and that default is installed, or change default
        // to the first one loaded (arbitrarily).

        if (goodRealm == null) {
            logger.severe(SecurityLoggerInfo.noRealmsError);

        } else {
            try {
                Realm.getInstance(defaultRealm);
            } catch (Exception e) {
                defaultRealm = goodRealm;
            }
            Realm.setDefaultRealm(defaultRealm);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Default realm is set to: " + defaultRealm);
            }
        }
    }
}
