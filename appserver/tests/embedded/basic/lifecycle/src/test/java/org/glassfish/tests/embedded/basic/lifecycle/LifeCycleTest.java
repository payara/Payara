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

package org.glassfish.tests.embedded.basic.lifecycle;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.junit.Test;

import java.io.File;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@dev.java.net
 */

public class LifeCycleTest {

    Logger logger = Logger.getAnonymousLogger();

    GlassFishRuntime runtime;

    @Test
    public void test() throws GlassFishException {
        runtime = GlassFishRuntime.bootstrap();

        GlassFish instance1 = runtime.newGlassFish();
        logger.info("Instance1 created" + instance1);
        instance1.start();
        logger.info("Instance1 started #1");
	sleep();
        instance1.stop();
        logger.info("Instance1 stopped #1");
        instance1.start();
        logger.info("Instance1 started #2");
	sleep();
        instance1.stop();
        logger.info("Instance1 stopped #2");
        instance1.dispose();
        logger.info("Instance1 disposed");
        checkDisposed();


        GlassFishProperties props = new GlassFishProperties();
        props.setProperty("glassfish.embedded.tmpdir", System.getProperty("user.dir"));
        GlassFish instance2 = runtime.newGlassFish(props);
        logger.info("instance2 created" + instance2);
        instance2.start();
        logger.info("Instance2 started #1");
	sleep();
        instance2.stop();
        logger.info("Instance2 stopped #1");
        instance2.start();
        logger.info("Instance2 started #2");
	sleep();
        instance2.stop();
        logger.info("Instance2 stopped #2");
        instance2.dispose();
        logger.info("Instance2 disposed");
        checkDisposed();
    }

    private void sleep() {
	try {
	  Thread.sleep(1000);
	} catch(Exception ex) {
	}
    }
    // throws exception if the temp dir is not cleaned out.

    private void checkDisposed() {
        String instanceRoot = System.getProperty("com.sun.aas.instanceRoot");
        logger.info("Checking whether " + instanceRoot + " is disposed or not");
        if (new File(instanceRoot).exists()) {
            throw new RuntimeException("Directory " + instanceRoot +
                    " is not cleaned up after glassfish.dispose()");
        }
    }
}
