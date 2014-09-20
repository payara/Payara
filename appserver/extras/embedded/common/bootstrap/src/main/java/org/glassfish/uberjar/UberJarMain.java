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

package org.glassfish.uberjar;

import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishRuntime;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.logging.Logger;

import org.glassfish.embeddable.GlassFishProperties;

/**
 *
 * This is a main class for 'java -jar glassfish-uber.jar'
 * 
 * @author bhavanishankar@dev.java.net
 */

public class UberJarMain {

    private static Logger logger = Logger.getLogger("embedded-glassfish");

    public static void main(String... args) throws Exception {
        new UberJarMain().start();
    }

    private void start() throws Exception {
        privilegedStart();
/*
        Properties props = new Properties();
        props.setProperty(Constants.PLATFORM_PROPERTY_KEY,
                System.getProperty(Constants.PLATFORM_PROPERTY_KEY, Constants.Platform.Felix.toString()));

        long startTime = System.currentTimeMillis();

        GlassFishRuntime gfr = GlassFishRuntime.bootstrap(
                props, getClass().getClassLoader());  // don't use thread context classloader, otherwise the META-INF/services will not be found.
        long timeTaken = System.currentTimeMillis() - startTime;

        logger.info("created gfr = " + gfr + ", timeTaken = " + timeTaken);

        startTime = System.currentTimeMillis();
        GlassFish gf = gfr.newGlassFish(props);
        timeTaken = System.currentTimeMillis() - startTime;
        System.out.println("created gf = " + gf + ", timeTaken = " + timeTaken);


        startTime = System.currentTimeMillis();
        gf.start();
        timeTaken = System.currentTimeMillis() - startTime;
        System.out.println("started gf, timeTaken = " + timeTaken);
*/

    }

    private void privilegedStart() throws Exception {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    Properties props = new Properties();
                    props.setProperty(BootstrapProperties.PLATFORM_PROPERTY_KEY,
                            System.getProperty(BootstrapProperties.PLATFORM_PROPERTY_KEY, BootstrapProperties.Platform.Felix.toString()));

                    long startTime = System.currentTimeMillis();

                    GlassFishRuntime gfr = GlassFishRuntime.bootstrap(
                            new BootstrapProperties(props), getClass().getClassLoader());  // don't use thread context classloader, otherwise the META-INF/services will not be found.
                    long timeTaken = System.currentTimeMillis() - startTime;

                    logger.info("created gfr = " + gfr + ", timeTaken = " + timeTaken);

                    startTime = System.currentTimeMillis();
                    GlassFish gf = gfr.newGlassFish(new GlassFishProperties(props));
                    timeTaken = System.currentTimeMillis() - startTime;
                    System.out.println("created gf = " + gf + ", timeTaken = " + timeTaken);


                    startTime = System.currentTimeMillis();
                    gf.start();
                    timeTaken = System.currentTimeMillis() - startTime;
                    System.out.println("started gf, timeTaken = " + timeTaken);

                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                return null;
            }
        });
    }
}
