/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.virtualization.util;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.virtualization.config.Virtualizations;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Defines the virtualization runtime context
 */
@Service
public class RuntimeContext {
    public final static boolean debug = true;
    public final static Logger logger = Logger.getAnonymousLogger();

    @Inject
    CommandRunner commandRunner;


    public static File getCacheDir() throws IOException {
        File instanceRoot = new File(System.getProperty("com.sun.aas.instanceRoot"));
        File virtRoot = new File(instanceRoot, "virt");
        File cacheRoot = new File(virtRoot, "cache");
        if (!cacheRoot.exists() && !cacheRoot.mkdirs()) {
            if (!cacheRoot.exists())
                throw new IOException("Cannot create cache directory at " + cacheRoot.getAbsolutePath());
        }
        return cacheRoot;
    }

    public static File absolutize(File source) {
        return source.isAbsolute()?source:new File(System.getProperty("user.home"), source.getPath());
    }

    public static String getEncodedOS() {
        return  System.getProperty("os.name").replaceAll(" ", "_");
    }

    public final static ExecutorService es = Executors.newFixedThreadPool(5);

    public  boolean executeAdminCommand(ActionReport report, String commandName, String operand, String... parameters) {

        ParameterMap params = new ParameterMap();
        if (operand!=null) {
            params.add("DEFAULT", operand);
        }
        for (int i=0;i<parameters.length;) {
            String key = parameters[i++];
            String value=null;
            if (i<parameters.length) {
                value = parameters[i++];
            }
            params.add(key, value);
        }
        CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation(commandName, report);
        inv.parameters(params);
        inv.execute();
        return (report.getActionExitCode()==ActionReport.ExitCode.SUCCESS);
    }

    public static void ensureTopLevelConfig(Domain domain, ActionReport actionReport) {
        if (domain.getExtensionByType(Virtualizations.class)==null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<Domain>() {
                    @Override
                    public Object run(Domain wDomain) throws PropertyVetoException, TransactionFailure {
                        Virtualizations virts = wDomain.createChild(Virtualizations.class);
                        wDomain.getExtensions().add(virts);
                        return virts;
                    }
                }, domain);
            } catch (TransactionFailure transactionFailure) {
                actionReport.failure(RuntimeContext.logger,
                        "Cannot create parent virtualizations configuration",transactionFailure);
                return;
            }
        }
    }
}
