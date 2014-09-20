/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.v3.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.module.bootstrap.EarlyLogHandler;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Protocols;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import javax.inject.Inject;
import javax.inject.Named;

import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Adds the needed http.setEncodedSlashEnabled  to domain.xml
 * during an upgrade from a v2.X server. For more information see:
 * https://glassfish.dev.java.net/issues/show_bug.cgi?id=13627
 */
@Service
public class AdminRESTConfigUpgrade
        implements ConfigurationUpgrade, PostConstruct {

    @Inject
    Configs configs;

    // http://java.net/jira/browse/GLASSFISH-15576
    // This will force the Grizzly upgrade code to run before
    // AdminRESTConfigUpgrade runs.
    @Inject @Named("grizzlyconfigupgrade") @Optional
    ConfigurationUpgrade precondition = null;

    @Override
    public void postConstruct() {
        for (Config config : configs.getConfig()) {
            // we only want to handle configs that have an admin listener
            try {
                if (config.getAdminListener() == null) {
                    LogRecord lr = new LogRecord(Level.FINE, String.format(
                            "Skipping config %s. No admin listener.",
                            config.getName()));
                    lr.setLoggerName(getClass().getName());
                    EarlyLogHandler.earlyMessages.add(lr);
                    continue;
                }
            } catch (IllegalStateException ise) {
                /*
                 * I've only seen the exception rather than
                 * getAdminListener returning null. This should
                 * typically happen for any config besides
                 * <server-config>, but we'll proceed if any
                 * config has an admin listener.
                 */
                LogRecord lr = new LogRecord(Level.FINE, String.format(
                        "Skipping config %s. getAdminListener threw: %s",
                        config.getName(), ise.getLocalizedMessage()));
                lr.setLoggerName(getClass().getName());
                EarlyLogHandler.earlyMessages.add(lr);                
                continue;
            }
            Protocols ps = config.getNetworkConfig().getProtocols();
            if (ps != null) {
                for (Protocol p : ps.getProtocol()) {
                    Http h = p.getHttp();
                    if (h != null
                            && "__asadmin".equals(h.getDefaultVirtualServer())) {
                        try {
                            ConfigSupport.apply(new HttpConfigCode(), h);
                        } catch (TransactionFailure tf) {
                            LogRecord lr = new LogRecord(Level.SEVERE,
                                    "Could not upgrade http element for admin console: "+ tf);
                            lr.setLoggerName(getClass().getName());
                            EarlyLogHandler.earlyMessages.add(lr);
                        }
                    }
                }


            }
        }
    }


    static private class HttpConfigCode implements SingleConfigCode<Http> {

        @Override
        public Object run(Http http) throws PropertyVetoException,
                TransactionFailure {

            http.setEncodedSlashEnabled("true");
            return null;
        }
    }
}
