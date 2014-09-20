/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans.customvalidators;


import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;

import java.util.logging.Logger;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.admin.config.Named;

public class ConfigRefValidator
    implements ConstraintValidator<ConfigRefConstraint, Named>, Payload {

    static final Logger logger = ConfigApiLoggerInfo.getLogger();
    static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ConfigRefValidator.class);

    public void initialize(final ConfigRefConstraint constraint) {       
    }

    @Override
    public boolean isValid(final Named bean,
        final ConstraintValidatorContext constraintValidatorContext) {
        if (bean == null) return true;

        Server server = null ;
        Cluster mycluster = null;
        String configRef = null;
        String serverName = null;
        if (bean instanceof Server)  {
            server = (Server)bean;
            configRef = server.getConfigRef();
            serverName = server.getName();
        } else if (bean instanceof Cluster){
            mycluster = (Cluster)bean  ;
            configRef = mycluster.getConfigRef();
            serverName = mycluster.getName();
        }


        if (configRef == null) return true; // skip validation @NotNull is already on getConfigRef
        
        // cannot use default-config
        if (configRef.equals(SystemPropertyConstants.TEMPLATE_CONFIG_NAME)) {
            logger.warning(ConfigApiLoggerInfo.configRefDefaultconfig);
           return false;
        }
        // cannot change config-ref of DAS
        if (server != null) {
            if (server.isDas() && !configRef.equals(SystemPropertyConstants.DAS_SERVER_CONFIG)) {
                logger.warning(ConfigApiLoggerInfo.configRefDASconfig);
                return false;
            }
            // cannot use server-config if not DAS
            if (!server.isDas() && configRef.equals(SystemPropertyConstants.DAS_SERVER_CONFIG)) {
                logger.warning(ConfigApiLoggerInfo.configRefServerconfig);
                return false;
            }



            final Servers servers = server.getParent(Servers.class);
            final Domain domain = servers.getParent(Domain.class);
            final Configs configs = domain.getConfigs();

            if (servers.getServer(serverName) != null) { // validate for set, not _register-instance
                // cannot change config ref of a clustered instance
                Cluster cluster = domain.getClusterForInstance(serverName);
                if (cluster != null) { // cluster is not null during create-local-instance --cluster c1 i1
                    if (!cluster.getConfigRef().equals(configRef)) {
                        // During set when trying to change config-ref of a clustered instance,
                        // the value of desired config-ref will be different than the current config-ref.
                        // During _register-instance, (create-local-instance --cluster c1 i1)
                        // cluster.getConfigRef().equals(configRef) will be true and not come here.
                        logger.warning(ConfigApiLoggerInfo.configRefClusteredInstance);
                        return false;
                    }
                }
                // cannot use a non-existent config  (Only used by set.  _register-instance will fail earlier)
                if (configs == null || configs.getConfigByName(configRef) == null) {
                    logger.warning(ConfigApiLoggerInfo.configRefNonexistent);
                    return false;
                }
            }
        }
        return true;
    }

}

