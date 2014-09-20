/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.servermgmt;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import java.beans.PropertyVetoException;
import java.util.*;
import java.util.logging.Level;
import javax.inject.Inject;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import static com.sun.enterprise.admin.servermgmt.SLogger.*;

/**
 * Change the jvm-options from v2 to v3
 *
 * @author Byron Nevins
 */
@Service
@PerLookup
public class V2ToV3ConfigUpgrade implements ConfigurationUpgrade, PostConstruct {

    @Inject
    Configs configs;

    /**
     * Report the JavaConfig beans for each config. <p> Lets the caller command
     * prepare access checks for security authorization.
     *
     * @return
     */
    public Collection<JavaConfig> getJavaConfigs() {
        final Collection<JavaConfig> result = new ArrayList<JavaConfig>();
        for (Config c : configs.getConfig()) {
            if (c.getJavaConfig() != null) {
                result.add(c.getJavaConfig());
            }
        }
        return result;
    }

    @Override
    public void postConstruct() {
        // the 'prevent' defense
        if (configs == null
                || configs.getConfig() == null
                || configs.getConfig().isEmpty()) {
            return;
        }

        try {
            for (Config c : configs.getConfig()) {
                JavaConfig jc = c.getJavaConfig();
                if (jc == null) {
                    continue;
                }

                // fix issues where each new config gets 2x, 3x, 4x the data
                newJvmOptions.clear();
                oldJvmOptions = Collections.unmodifiableList(jc.getJvmOptions());
                doAdditions("server-config".equals(c.getName()));
                doRemovals();
                ConfigSupport.apply(new JavaConfigChanger(), jc);
            }
        }
        catch (Exception e) {
            getLogger().log(Level.SEVERE, JVM_OPTION_UPGRADE_FAILURE, e);
            throw new RuntimeException(e);
        }
    }

    private void doRemovals() {
        // copy options from old to new.  Don't add items on the removal list
        // note that the remove list also has all the items we just added with
        // doAdditions() so that we don't get duplicate messes.
        for (String s : oldJvmOptions) {
            if (!shouldRemove(s))
                newJvmOptions.add(s);
        }
    }

    private void doAdditions(boolean isDas) {
        // add new options
        doAdditionsFrom(ADD_LIST);
        if (isDas) {
            doAdditionsFrom(ADD_LIST_DAS);
        }
        else {
            doAdditionsFrom(ADD_LIST_NOT_DAS);
        }
    }

    private void doAdditionsFrom(String[] strings) {
        newJvmOptions.addAll(Arrays.asList(strings));
    }

    private boolean shouldRemove(String option) {
        if (!ok(option))
            return true;

        for (String s : REMOVAL_LIST)
            if (option.startsWith(s))
                return true;

        return false;
    }

    private boolean ok(String s) {
        return s != null && s.length() > 0;
    }
    private List<String> oldJvmOptions = null;
    private final List<String> newJvmOptions = new ArrayList<String>();
    private static final String[] BASE_REMOVAL_LIST = new String[]{
        "-Djavax.management.builder.initial",
        "-Dsun.rmi.dgc.server.gcInterval",
        "-Dsun.rmi.dgc.client.gcInterval",
        "-Dcom.sun.enterprise.taglibs",
        "-Dcom.sun.enterprise.taglisteners",
        "-XX:LogFile",};
    // these are added to all configs
    private static final String[] ADD_LIST = new String[]{
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+LogVMOutput",
        "-XX:LogFile=${com.sun.aas.instanceRoot}/logs/jvm.log",
        "-Djava.awt.headless=true",
        "-DANTLR_USE_DIRECT_CLASS_LOADING=true",
        "-Dosgi.shell.telnet.maxconn=1",
        "-Dosgi.shell.telnet.ip=127.0.0.1",
        "-Dgosh.args=--noshutdown -c noop=true",
        "-Dfelix.fileinstall.dir=${com.sun.aas.installRoot}/modules/autostart/",
        "-Dfelix.fileinstall.poll=5000",
        "-Dfelix.fileinstall.debug=3",
        "-Dfelix.fileinstall.bundles.new.start=true",
        "-Dfelix.fileinstall.bundles.startTransient=true",
        "-Dfelix.fileinstall.disableConfigSave=false",
        "-Dfelix.fileinstall.log.level=2",
        "-Djavax.management.builder.initial=com.sun.enterprise.v3.admin.AppServerMBeanServerBuilder",
        "-Dorg.glassfish.web.rfc2109_cookie_names_enforced=false",
        "-Djava.ext.dirs=${com.sun.aas.javaRoot}/lib/ext${path.separator}${com.sun.aas.javaRoot}/jre/lib/ext${path.separator}${com.sun.aas.instanceRoot}/lib/ext",};
    // these are added to DAS only
    private static final String[] ADD_LIST_DAS = new String[]{
        "-Dosgi.shell.telnet.port=6666"
    };
    // these are added to instances
    private static final String[] ADD_LIST_NOT_DAS = new String[]{
        "-Dosgi.shell.telnet.port=${OSGI_SHELL_TELNET_PORT}"
    };
    private static final List<String> REMOVAL_LIST = new ArrayList<String>();

    static {
        Collections.addAll(REMOVAL_LIST, BASE_REMOVAL_LIST);
        Collections.addAll(REMOVAL_LIST, ADD_LIST);
    }

    private class JavaConfigChanger implements SingleConfigCode<JavaConfig> {

        @Override
        public Object run(JavaConfig jc) throws PropertyVetoException, TransactionFailure {
            jc.setJvmOptions(newJvmOptions);
            return jc;
        }
    }
}
