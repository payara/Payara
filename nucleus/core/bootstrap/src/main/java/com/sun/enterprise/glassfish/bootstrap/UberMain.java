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

package com.sun.enterprise.glassfish.bootstrap;

import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * This is main class for the uber jars viz., glassfish-embedded-all.jar and
 * glassfish-embedded-web.jar, to be able to do:
 * <p/>
 * <p/>java -jar glassfish-embedded-all.jar
 * <p/>java -jar glassfish-embedded-web.jar
 *
 * @author bhavanishankar@dev.java.net
 */
public class UberMain {

    GlassFish gf;

    public static void main(String... args) throws Exception {
        new UberMain().run();
    }

    public void run() throws Exception {
        addShutdownHook(); // handle Ctrt-C.

        GlassFishProperties gfProps =new GlassFishProperties();
        gfProps.setProperty("org.glassfish.embeddable.autoDelete",
                System.getProperty("org.glassfish.embeddable.autoDelete", "true"));

        gf = GlassFishRuntime.bootstrap().newGlassFish(gfProps);
        
        gf.start();

        CommandRunner cr = gf.getCommandRunner();

        while (true) {
            System.out.print("\n\nGlassFish $ ");
            String str = null;
            try {
                str = new BufferedReader(new InputStreamReader(System.in)).readLine();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (str != null && str.trim().length() != 0) {
                if ("exit".equalsIgnoreCase(str) || "quit".equalsIgnoreCase(str)) {
                    break;
                }
                String[] split = str.split(" ");
                String command = split[0].trim();
                String[] commandParams = null;
                if (split.length > 1) {
                    commandParams = new String[split.length - 1];
                    for (int i = 1; i < split.length; i++) {
                        commandParams[i - 1] = split[i].trim();
                    }
                }
                try {
                    CommandResult result = commandParams == null ?
                            cr.run(command) : cr.run(command, commandParams);
                    System.out.println("\n" + result.getOutput());
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }

        try {
            gf.stop();
            gf.dispose();
        } catch (Exception ex) {
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(
                "GlassFish Shutdown Hook") {
            public void run() {
                try {
                    if (gf != null) {
                        gf.stop();
                        gf.dispose();
                    }
                } catch (Exception ex) {
                }
            }
        });
    }

}
