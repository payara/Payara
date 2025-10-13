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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.auth.realm;

import static com.sun.enterprise.security.SecurityLoggerInfo.noRealmsError;
import static com.sun.enterprise.security.SecurityLoggerInfo.realmConfigDisabledError;
import static com.sun.enterprise.security.SecurityLoggerInfo.securityExceptionError;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.security.SecurityLoggerInfo;

/**
 * RealmConfig usable by standalone : Admin CLI for creating Realms It has a subset of functionality defined in
 * com.sun.enterprise.security.RealmConfig
 */
public class RealmConfig {

    private static final Logger LOGGER = SecurityLoggerInfo.getLogger();

    public static void createRealms(String defaultRealm, List<AuthRealm> realms) {
        createRealms(defaultRealm, realms, null);
    }

    public static void createRealms(String defaultRealm, List<AuthRealm> realms, String configName) {
        String goodRealm = null; // need at least one good realm

        for (AuthRealm realm : realms) {
            String realmName = realm.getName();
            String realmClass = realm.getClassname();

            try {
                List<Property> realmProperties = realm.getProperty();
                Properties properties = new Properties();
                for (Property realmProperty : realmProperties) {
                    properties.setProperty(realmProperty.getName(), realmProperty.getValue());
                }
                
                Realm.instantiate(realmName, realmClass, properties, configName);
                if (LOGGER.isLoggable(FINE)) {
                    LOGGER.log(FINE, "Configured realm: {0}", realmName);
                }

                if (goodRealm == null) {
                    goodRealm = realmName;
                }
            } catch (Exception e) {
                LOGGER.log(WARNING, realmConfigDisabledError, realmName);
                LOGGER.log(WARNING, securityExceptionError, e);
            }
        }

        // Done loading all realms, check that there is at least one
        // in place and that default is installed, or change default
        // to the first one loaded (arbitrarily).

        if (goodRealm == null) {
            LOGGER.severe(noRealmsError);
        } else {
            try {
                Realm.getInstance(defaultRealm);
            } catch (Exception e) {
                defaultRealm = goodRealm;
            }
            
            Realm.setDefaultRealm(defaultRealm);
            if (LOGGER.isLoggable(FINE)) {
                LOGGER.log(FINE, "Default realm is set to: {0}", defaultRealm);
            }
        }
    }
}
