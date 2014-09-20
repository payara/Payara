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

package com.sun.enterprise.config.serverbeans;

import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.GenericCrudCommand;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the abstract class which will be used by the config beans
 * {@link Cluster} and {@link Server} classes to copy the default-configs
 * 
 */
public abstract class CopyConfig implements AdminCommand {

    @Param(primary = true, multiple = true)
    protected List<String> configs;
    @Inject
    protected Domain domain;
    @Param(optional = true, separator = ':')
    protected String systemproperties;
    protected Config copyOfConfig;
    @Inject
    ServerEnvironment env;
    @Inject
    ServerEnvironmentImpl envImpl;
    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(CopyConfig.class);

    public Config copyConfig(Configs configs, Config config, String destConfigName, Logger logger) throws PropertyVetoException,
            TransactionFailure {
        final Config destCopy = (Config) config.deepCopy(configs);
        if (systemproperties != null) {
            final Properties properties = GenericCrudCommand.convertStringToProperties(systemproperties, ':');

            for (final Object key : properties.keySet()) {
                final String propName = (String) key;
                //cannot update a system property so remove it first
                List<SystemProperty> sysprops = destCopy.getSystemProperty();
                for (SystemProperty sysprop : sysprops) {
                    if (propName.equals(sysprop.getName())) {
                        sysprops.remove(sysprop);
                        break;
                    }

                }
                SystemProperty newSysProp = destCopy.createChild(SystemProperty.class);
                newSysProp.setName(propName);
                newSysProp.setValue(properties.getProperty(propName));
                destCopy.getSystemProperty().add(newSysProp);
            }
        }
        final String configName = destConfigName;
        destCopy.setName(configName);
        configs.getConfig().add(destCopy);
        copyOfConfig = destCopy;

        String srcConfig = "";
        srcConfig = config.getName();

        File configConfigDir = new File(env.getConfigDirPath(),
                configName);
        for (Config c : configs.getConfig()) {
            File existingConfigConfigDir = new File(env.getConfigDirPath(), c.getName());
            if (!c.getName().equals(configName) && configConfigDir.equals(existingConfigConfigDir)) {
                throw new TransactionFailure(localStrings.getLocalString(
                        "config.duplicate.dir",
                        "Config {0} is trying to use the same directory as config {1}",
                        configName, c.getName()));
            }
        }
        try {
            if (!(new File(configConfigDir, "docroot").mkdirs() &&
                  new File(configConfigDir, "lib/ext").mkdirs())) {
                throw new IOException(localStrings.getLocalString("config.mkdirs",
                        "error creating config specific directories"));
            }

            String srcConfigLoggingFile = env.getInstanceRoot().getAbsolutePath() + File.separator + "config" + File.separator
                    + srcConfig + File.separator + ServerEnvironmentImpl.kLoggingPropertiesFileName;
            File src = new File(srcConfigLoggingFile);

            if (!src.exists()) {
                src = new File(env.getConfigDirPath(), ServerEnvironmentImpl.kLoggingPropertiesFileName);
            }

            File dest = new File(configConfigDir, ServerEnvironmentImpl.kLoggingPropertiesFileName);
            FileUtils.copy(src, dest);
        } catch (Exception e) {
            logger.log(Level.WARNING, ConfigApiLoggerInfo.copyConfigError, e.getLocalizedMessage());
        }
        return destCopy;
    }
}
