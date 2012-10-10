/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.config.serverbeans;

import com.sun.enterprise.config.serverbeans.ApplicationConfig;
import com.sun.enterprise.config.serverbeans.Engine;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;


/**
 * Corresponds to the web-module-config element used for recording web
 * module configuration customizations.
 *
 * @author tjquinn
 */
@Configured
public interface WebModuleConfig extends ConfigBeanProxy, ApplicationConfig {

    /**
     * Returns the env-entry objects, if any.
     * @return the env-entry objects
     */
    @Element
    public List<EnvEntry> getEnvEntry();

    /**
     * Returns the context-param objects, if any.
     * @return the context-param objects
     */
    @Element
    public List<ContextParam> getContextParam();

    @DuckTyped public EnvEntry getEnvEntry(final String name);

    @DuckTyped public ContextParam getContextParam(final String name);

    @DuckTyped public void deleteEnvEntry(final String name) throws PropertyVetoException, TransactionFailure;

    @DuckTyped public void deleteContextParam(final String name) throws PropertyVetoException, TransactionFailure;

    @DuckTyped public List<EnvEntry> envEntriesMatching(final String nameOrNull);

    @DuckTyped public List<ContextParam> contextParamsMatching(final String nameOrNull);

    public class Duck {

        public static EnvEntry getEnvEntry(final WebModuleConfig instance,
                final String name) {
            for (EnvEntry entry : instance.getEnvEntry()) {
                if (entry.getEnvEntryName().equals(name)) {
                    return entry;
                }
            }
            return null;
        }

        public static ContextParam getContextParam(final WebModuleConfig instance,
                final String name){
            for (ContextParam param : instance.getContextParam()) {
                if (param.getParamName().equals(name)) {
                    return param;
                }
            }
            return null;
        }

        public static void deleteEnvEntry(final WebModuleConfig instance,
                final String name) throws PropertyVetoException, TransactionFailure {
            final EnvEntry entry = instance.getEnvEntry(name);
            if (entry == null) {
                return;
            }
            ConfigSupport.apply(new SingleConfigCode<WebModuleConfig>(){

                @Override
                public Object run(WebModuleConfig config) throws PropertyVetoException, TransactionFailure {
                    return config.getEnvEntry().remove(entry);
                }

                }, instance);
        }

        public static void deleteContextParam(final WebModuleConfig instance,
                final String name) throws PropertyVetoException, TransactionFailure {
            final ContextParam param = instance.getContextParam(name);
            if (param == null) {
                return;
            }
            ConfigSupport.apply(new SingleConfigCode<WebModuleConfig>(){

                @Override
                public Object run(WebModuleConfig config) throws PropertyVetoException, TransactionFailure {
                    return config.getContextParam().remove(param);
                }

                }, instance);

        }

        

        public static List<EnvEntry> envEntriesMatching(final WebModuleConfig instance,
                final String nameOrNull) {
            List<EnvEntry> result;
            if (nameOrNull != null) {
                final EnvEntry entry = getEnvEntry(instance, nameOrNull);
                if (entry == null) {
                    result = Collections.emptyList();
                } else {
                    result = new ArrayList<EnvEntry>();
                    result.add(entry);
                }
            } else {
                result = instance.getEnvEntry();
            }
            return Collections.unmodifiableList(result);
        }

        public static List<ContextParam> contextParamsMatching(final WebModuleConfig instance,
                final String nameOrNull) {
            List<ContextParam> result;
            if (nameOrNull != null) {
                final ContextParam param = getContextParam(instance, nameOrNull);
                if (param == null) {
                    result = Collections.emptyList();
                } else {
                    result = new ArrayList<ContextParam>();
                    result.add(param);
                }
            } else {
                result = instance.getContextParam();
            }
            return Collections.unmodifiableList(result);

        }

        public static WebModuleConfig webModuleConfig(final Engine engine) {
            return (WebModuleConfig) engine.getApplicationConfig();
        }
    }

}
