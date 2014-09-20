/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tests.embedded.ejb.remoteejb.test;

import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.tests.embedded.ejb.remoteejb.RemoteEJBInf;
import org.glassfish.tests.embedded.ejb.remoteejb.SampleEjb;
import org.glassfish.tests.embedded.ejb.remoteejb.TimerEjb;
import org.junit.Assert;
import org.junit.Test;

import javax.naming.InitialContext;
import java.io.File;
import java.net.URI;

/**
 * @author bhavanishankar@java.net
 */
public class EJBTest {

    /*
        public static void main(String[] args) {
            EmbeddedTest test = new EmbeddedTest();
            System.setProperty("basedir", System.getProperty());
            test.test();
        }
    */
    GlassFish glassfish;

    @Test
    public void test() throws Exception {

        glassfish = GlassFishRuntime.bootstrap().newGlassFish();
        glassfish.start();


        Deployer deployer = glassfish.getDeployer();
        URI uri = new File(System.getProperty("user.dir"), "target/remoteejb.jar").toURI();
        System.out.println("Deploying [" + uri + "]");
        deployer.deploy(uri);

        InitialContext ic = new InitialContext();

        System.out.println("Looking up SampleEJB.");
        SampleEjb sampleEjb = (SampleEjb) ic.lookup("java:global/remoteejb/SampleEjb");
        System.out.println("Invoking SampleEjb [" + sampleEjb + "]");
        Assert.assertEquals(sampleEjb.saySomething(), "Hello World");
        System.out.println("SampleEjb tested successfully");

        System.out.println("Looking up TimerEjb.");
        TimerEjb timerEjb = (TimerEjb) ic.lookup("java:global/remoteejb/TimerEjb");
        System.out.println("Invoking TimerEjb [" + timerEjb + "]");
        timerEjb.createTimer();
        System.out.println("Verifying TimerEjb [" + timerEjb + "]");
        Thread.sleep(4000);
        boolean result = timerEjb.verifyTimer();
        Assert.assertTrue(result);
        System.out.println("TimerEJB tested successfully.");


//        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
//        try {
            System.out.println("Looking up RemoteEJB.");
//            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            RemoteEJBInf remoteEjb = (RemoteEJBInf) ic.lookup("java:global/remoteejb/RemoteEJB");
            System.out.println("Invoking RemoteEJB [" + remoteEjb + "]");
            Assert.assertEquals(remoteEjb.sayHi(), "Hi Bhavani");
            System.out.println("RemoteEjb tested successfully");
//        } finally {
//            Thread.currentThread().setContextClassLoader(oldCL);
//        }
        
        glassfish.stop();
        glassfish.dispose();

        System.out.println("EmbeddedTest completed.");

    }

    @Test
    public void test2() throws Exception {

    }

}
