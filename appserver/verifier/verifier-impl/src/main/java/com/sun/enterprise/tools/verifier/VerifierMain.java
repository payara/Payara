/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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


package com.sun.enterprise.tools.verifier;

import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.tools.verifier.gui.MainFrame;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class VerifierMain {

    private static volatile GlassFishRuntime gfr;

    public static void main(String[] args) throws GlassFishException, IOException {
        VerifierFrameworkContext verifierFrameworkContext =
                new Initializer(args).getVerificationContext();

        addShutdownHook(); // Since in gui mode, we don't get a chance to clean up, we need to install a shutdown hook
        gfr = GlassFishRuntime.bootstrap();
        GlassFishProperties gfp = new GlassFishProperties();
        gfp.setProperty(StartupContext.TIME_ZERO_NAME, (new Long(System.currentTimeMillis())).toString());
        final String VERIFIER_MODULE = "org.glassfish.verifier";
        gfp.setProperty(StartupContext.STARTUP_MODULE_NAME, VERIFIER_MODULE);
//        gfp.setConfigFileURI("file:/tmp/domain.xml");
        GlassFish gf = gfr.newGlassFish(gfp);
        gf.start();
        int failedCount = -1;
        Verifier verifier = gf.getService(Verifier.class);
        if (verifierFrameworkContext.isUsingGui()) {
            MainFrame mf = new MainFrame(
                    verifierFrameworkContext.getJarFileName(), true, verifier);
            mf.setSize(800, 600);
            mf.setVisible(true);
        } else {
            LocalStringManagerImpl smh = StringManagerHelper.getLocalStringsManager();
            try {
                verifier.init(verifierFrameworkContext);
                verifier.verify();
            } catch (Exception e) {
                LogRecord logRecord = new LogRecord(Level.SEVERE,
                        smh.getLocalString(
                                verifier.getClass().getName() +
                                ".verifyFailed", // NOI18N
                                "Could not verify successfully.")); // NOI18N
                logRecord.setThrown(e);
                verifierFrameworkContext.getResultManager().log(logRecord);
            }
            verifier.generateReports();
            failedCount = verifierFrameworkContext.getResultManager()
                    .getFailedCount() +
                    verifierFrameworkContext.getResultManager().getErrorCount();
            System.exit(failedCount);
        }
    }

    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread("Verifier Shutdown Hook") {
            public void run() {
                if (gfr == null) return;
                try {
                    gfr.shutdown();
                    gfr = null;
                }
                catch (Exception ex) {
                    System.err.println("Error shutting down glassfish runtime: " + ex);
                    ex.printStackTrace();
                }
            }
        });

    }
}
