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

package com.sun.enterprise.configapi.tests.concurrent;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.configapi.tests.ConfigApiTest;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.util.concurrent.Semaphore;

/**
 * Concurrent access to the configuarion APIs related tests
 * @author Jerome Dochez
 */

public class ConcurrentAccessTest extends ConfigApiTest {

    public String getFileName() {
        return "DomainTest";
    }

    @Test
    public void waitAndSuccessTest() throws TransactionFailure, InterruptedException {
        ConfigSupport.lockTimeOutInSeconds=1;
        runTest(200);
    }

    @Test(expected=TransactionFailure.class)
    public void waitAndTimeOutTest() throws TransactionFailure, InterruptedException {
        ConfigSupport.lockTimeOutInSeconds=1;
        try {
            runTest(1200);
        } catch (TransactionFailure transactionFailure) {
            logger.fine("Got expected transaction failure, access timed out");
            throw transactionFailure;
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        }
    }



    private void runTest(final int waitTime) throws TransactionFailure, InterruptedException {
        
        final Domain domain = getHabitat().getService(Domain.class);

        // my lock.
        final Semaphore lock = new Semaphore(1);
        lock.acquire();

        // end of access
        final Semaphore endOfAccess = new Semaphore(1);
        endOfAccess.acquire();

        final long begin = System.currentTimeMillis();

        // let's start a thread to hold the lock on Domain...
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ConfigSupport.apply(new SingleConfigCode<Domain>() {
                        @Override
                        public Object run(Domain domain) throws PropertyVetoException, TransactionFailure {
                            logger.fine("got the lock at " + (System.currentTimeMillis() - begin));
                            lock.release();
                            try {
                                Thread.sleep(waitTime);
                            } catch (InterruptedException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                            logger.fine("release the lock at " + (System.currentTimeMillis() - begin));
                            return null;
                        };
                    }, domain);
                } catch(TransactionFailure e) {
                    e.printStackTrace();
                }
                endOfAccess.release();
            }
        });
        t.start();


        // let's change the last modified date...
        lock.acquire();
        logger.fine("looking for second lock at " + (System.currentTimeMillis() - begin));

        try {
            ConfigSupport.apply(new SingleConfigCode<Domain>() {
                @Override
                public Object run(Domain domain) throws PropertyVetoException, TransactionFailure {
                    logger.fine("got the second lock at " + (System.currentTimeMillis() - begin));
                    lock.release();
                    return null;
                }
            }, domain);
        } finally {
            endOfAccess.acquire();
        }


    }

}
