/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.nucleus.admin.progress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import static org.glassfish.tests.utils.NucleusTestUtils.*;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

/**
 *
 * @author martinmares
 */
@Test(testName="DetachAttachTest")
public class DetachAttachTest {
    
    @AfterTest
    public void cleanUp() throws Exception {
        nadmin("stop-domain");
        JobManagerTest.deleteJobsFile();
        nadmin("start-domain");
    }
    
    public void uptimePeriodically() throws InterruptedException {
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < 3; i++) {
            System.out.println("detachAndAttachUptimePeriodically(): round " + i);
            NadminReturn result = nadminWithOutput("--detach", "--terse", "uptime");
            assertTrue(result.returnValue);
            String id = parseJobIdFromEchoTerse(result.out);
            assertTrue(ids.add(id)); //Must be unique
            Thread.sleep(1000L);
            //Now attach
            result = nadminWithOutput("--terse", "attach", id);
            assertTrue(result.returnValue);
            assertTrue(result.out.contains("uptime"));
        }
    }
    
    public void commandWithProgressStatus() throws InterruptedException {
        NadminReturn result = nadminWithOutput("--detach", "--terse", "progress-custom", "6x1");
        assertTrue(result.returnValue);
        String id = parseJobIdFromEchoTerse(result.out);
        Thread.sleep(2000L);
        //Now attach running
        result = nadminWithOutput("attach", id);
        assertTrue(result.returnValue);
        assertTrue(result.out.contains("progress-custom"));
        List<ProgressMessage> prgs = ProgressMessage.grepProgressMessages(result.out);
        assertFalse(prgs.isEmpty());
        assertTrue(prgs.get(0).getValue() > 0);
        assertEquals(100, prgs.get(prgs.size() - 1).getValue());
        //Now attach finished - must NOT exists - seen progress job is removed
        result = nadminWithOutput("attach", id);
        assertFalse(result.returnValue);
    }
    
    public void detachOnesAttachMulti() throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory() {
                                            @Override
                                            public Thread newThread(Runnable r) {
                                                Thread result = new Thread(r);
                                                result.setDaemon(true);
                                                return result;
                                            }
                                        });
        //Execute command with progress status suport
        NadminReturn result = nadminWithOutput("--detach", "--terse", "progress-custom", "8x1");
        assertTrue(result.returnValue);
        final String id = parseJobIdFromEchoTerse(result.out);
        Thread.sleep(1500L);
        //Now attach
        final int _attach_count = 3;
        Collection<Callable<NadminReturn>> attaches = new ArrayList<Callable<NadminReturn>>(_attach_count);
        for (int i = 0; i < _attach_count; i++) {
            attaches.add(new Callable<NadminReturn>() {
                    @Override
                    public NadminReturn call() throws Exception {
                        return nadminWithOutput("attach", id);
                    }
                });
        }
        List<Future<NadminReturn>> results = pool.invokeAll(attaches);
        //Test results
        for (Future<NadminReturn> fRes : results) {
            NadminReturn res = fRes.get();
            assertTrue(res.returnValue);
            assertTrue(res.out.contains("progress-custom"));
            List<ProgressMessage> prgs = ProgressMessage.grepProgressMessages(res.out);
            assertFalse(prgs.isEmpty());
            assertTrue(prgs.get(0).getValue() > 0);
            assertEquals(100, prgs.get(prgs.size() - 1).getValue());
        }
    }
    
    private String parseJobIdFromEchoTerse(String str) {
        StringTokenizer stok = new StringTokenizer(str, "\n\r");
        assertTrue(stok.hasMoreTokens());
        stok.nextToken();
        //Id is second non empty line
        assertTrue(stok.hasMoreTokens());
        String result = stok.nextToken().trim();
        assertFalse(result.isEmpty());
        assertFalse(result.contains(" ")); //With space does not look like ID but like some error message
        return result;
    }
    
    static class NadminCallable implements Callable<NadminReturn> {
        
        private final String[] args;

        public NadminCallable(String... args) {
            this.args = args;
        }

        @Override
        public NadminReturn call() throws Exception {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
        
    
    }
    
}
