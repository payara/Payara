/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.nucleus.healthcheck.stuck;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * Stores all managed and pooled threads
 * for testing if they are unresponsive
 * @since 4.1.2.173
 * @author jonathan coustick
 */
@Service(name = "stuck-threads-store")
@RunLevel(StartupRunLevel.VAL)
public class StuckThreadsStore {

    private ConcurrentHashMap<Long, Long> threads;

    private Logger logger;

    public StuckThreadsStore() {
        threads = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void postConstruct() {
        logger = Logger.getLogger("fish.payara.nucleus.healthcheck");
    }

    /**
     * Registers a thread with the store
     *
     * @param threadid the id of the thread to register
     */    
    public void registerThread(Long threadid){
        threads.put(threadid, System.currentTimeMillis());
    }

    /**
     * Removes a thread from the store. This means that the thread is not stuck.
     *
     * @param threadid the id of the thread to remove
     */    
    public void deregisterThread(long threadid){
        if (threads.remove(threadid) == null) {
            logger.log(Level.FINE, "Tried to deregister non-existent thread {0}", threadid);
        }
    }

    /**
     * Returns a HashMap of the threads in the store with the values of time the thread was registered.
     * @return all threads
     */
    public ConcurrentHashMap<Long, Long> getThreads() {
        return threads;
    }

}
