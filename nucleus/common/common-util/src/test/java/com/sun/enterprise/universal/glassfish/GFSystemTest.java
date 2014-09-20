/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.universal.glassfish;

import com.sun.enterprise.universal.collections.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author bnevins
 */
public class GFSystemTest {

    public GFSystemTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of GFSystem for the case where there are multiple instances in a JVM
     */
    @Test
    public void threadTest() {
        try {
            Thread t1 = new ParentThread("xxx");
            Thread t2 = new ParentThread("yyy");
            Thread t3 = new ParentThread("zzz");
            t1.start();
            t2.start();
            t3.start();
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(GFSystemTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertFalse(failed);
    }
    public static synchronized void setFailure() {
        failed = true;
    }
    public static volatile boolean failed = false;
}

class ParentThread extends Thread {
    ParentThread(String name) {
        super(name);
    }
    @Override 
    public void run() {
        try {
            GFSystem.setProperty("foo", getName());
            Thread t = new ChildThread(getName(), getName() + "__child");
            t.start();
            String result = GFSystem.getProperty("foo");

            if (result.equals(getName())) {
            } else {
                GFSystemTest.setFailure();
            }
            t.join();
        } catch (InterruptedException ex) {
        }
    }
}


class ChildThread extends Thread {
    ChildThread(String parentName, String name) {
        super(name);
        this.parentName = parentName;
    }
    @Override 
    public void run() {
        try {
            Thread t = new GrandChildThread(parentName, getName() + "__grandchild");
            t.start();
            String result = GFSystem.getProperty("foo");

            if (result.equals(parentName)) {
            } else {
                GFSystemTest.setFailure();
            }
            t.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(ChildThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    String parentName;
}

class GrandChildThread extends Thread {
    GrandChildThread(String grandParentName, String name) {
        super(name);
        this.grandParentName = grandParentName;
    }
    @Override 
    public void run() {
        String result = GFSystem.getProperty("foo");

        if(!result.equals(grandParentName)) {
            GFSystemTest.setFailure();
        }
    }
    String grandParentName;
}


/*
/*
    public static void main(String[] args) {
        Thread t = new TestThread("thread1");
        Thread t2 = new TestThread("thread2");
        t.start();
        t2.start();
    }

}



 */
