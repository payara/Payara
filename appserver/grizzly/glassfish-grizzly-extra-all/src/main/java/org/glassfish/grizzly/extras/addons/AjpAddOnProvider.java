/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.grizzly.extras.addons;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.config.ConfigAwareElement;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.http.ajp.AjpAddOn;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 * Ajp service.
 *
 * @author Alexey Stashok
 */
@Service(name = "ajp")
@ContractsProvided({AjpAddOnProvider.class, AddOn.class})
public class AjpAddOnProvider extends AjpAddOn implements ConfigAwareElement<Http> {

    protected static final Logger _logger = Logger.getLogger("javax.enterprise.web");

    @Override
    public void configure(final ServiceLocator habitat,
            final NetworkListener networkListener, final Http http) {

        final boolean jkSupportEnabled = http.getJkEnabled() != null
                ? Boolean.parseBoolean(http.getJkEnabled())
                : Boolean.parseBoolean(networkListener.getJkEnabled());
        if (jkSupportEnabled) {
            final String jkPropertiesFilename =
                    Boolean.parseBoolean(http.getJkEnabled())
                    ? http.getJkConfigurationFile()
                    : networkListener.getJkConfigurationFile();

            File propertiesFile = null;

            if (jkPropertiesFilename != null) {
                propertiesFile = new File(jkPropertiesFilename);
            }


            final String systemPropertyFilename =
                    System.getProperty("com.sun.enterprise.web.connector.enableJK.propertyFile");

            if ((propertiesFile == null || !propertiesFile.exists())
                    && systemPropertyFilename != null) {
                propertiesFile = new File(systemPropertyFilename);
            }

            if (propertiesFile == null) {
                if (_logger.isLoggable(Level.FINEST)) {
                    _logger.finest("jk properties configuration file not defined");
                }
                return;
            }

            if (!propertiesFile.exists()) {
                if (_logger.isLoggable(Level.FINEST)) {
                    _logger.log(Level.FINEST,
                            "jk properties configuration file '{0}' doesn't exist",
                            propertiesFile.getAbsoluteFile());
                }
                return;
            }

            if (_logger.isLoggable(Level.FINEST)) {
                _logger.log(Level.FINEST, "Loading glassfish-jk.properties from {0}",
                        propertiesFile.getAbsolutePath());
            }

            Properties properties = null;

            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(propertiesFile));
                properties = new Properties();
                properties.load(is);

            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            configure(properties);
        }
    }
}
