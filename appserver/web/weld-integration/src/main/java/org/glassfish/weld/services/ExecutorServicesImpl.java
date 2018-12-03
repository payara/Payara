/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.weld.services;

import com.sun.enterprise.util.Utility;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.jboss.weld.executor.AbstractExecutorServices;
import org.jboss.weld.manager.api.ExecutorServices;

/**
 * Implementation of the Weld Executor Services SPI which uses the Payara Executor
 * Service for its 
 * @author steve
 */
public class ExecutorServicesImpl extends AbstractExecutorServices implements ExecutorServices {
    
    private PayaraExecutorService executor;
    
    public ExecutorServicesImpl(PayaraExecutorService service) {
        executor = service;
    }

    @Override
    public ExecutorService getTaskExecutor() {
        return executor.getUnderlyingExecutorService();
    }

    @Override
    public ScheduledExecutorService getTimerExecutor() {
        return executor.getUnderlyingScheduledExecutorService();
    }

    @Override
    public void cleanup() {
    }

    @Override
    protected int getThreadPoolSize() {
        return executor.getExecutorThreadPoolSize();
    }

    @Override
    public <T> Collection<? extends Callable<T>> wrap(Collection<? extends Callable<T>> tasks) {
        // needs to set the context classloader based on the current context classloader and then reset it back
        ClassLoader TCCL = Thread.currentThread().getContextClassLoader();
        List<Callable<T>> wrapped = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrapped.add(new Callable() {
                @Override
                public Object call() throws Exception {
                    ClassLoader old = Utility.setContextClassLoader(TCCL);
                    try {
                    return task.call();
                    } finally {
                        Utility.setContextClassLoader(old);
                    }
                }
                
            });
        }
        return wrapped;
    }
    
    
    
}
