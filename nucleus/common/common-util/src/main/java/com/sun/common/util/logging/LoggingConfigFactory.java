/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package com.sun.common.util.logging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

@Service
@Singleton
public class LoggingConfigFactory implements Factory<LoggingConfig> {

    private static final Logger LOGGER = Logger.getLogger(LoggingConfigFactory.class.getName());

    @Inject
    private ServiceLocator locator;

    private final Map<String, LoggingConfig> configs;

    public LoggingConfigFactory() {
        configs = new HashMap<>();
    }

    @Override
    public void dispose(LoggingConfig config) {
        if (config == null) {
            return;
        }
        Iterator<Entry<String, LoggingConfig>> configIterator = configs.entrySet().iterator();
        while (configIterator.hasNext()) {
            Entry<String, LoggingConfig> configEntry = configIterator.next();
            if (configEntry.getValue().equals(config)) {
                configIterator.remove();
            }
        }
    }

    public LoggingConfig provide(String target) {
        // Try and fetch from the map
        LoggingConfig config = configs.get(target);
        if (config != null) {
            return config;
        }

        // Create a new one
        config = locator.createAndInitialize(LoggingConfigImpl.class);
        try {
            config.initialize(target);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "An error occurred while reading from the logs for the target '" + target + "'.", ex);
            return null;
        }
        configs.put(target, config);
        return config;
    }

    @Override
    public LoggingConfig provide() {
        return provide(null);
    }

}