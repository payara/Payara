/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.virtualization.runtime;

import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Dom;

import java.io.IOException;
import java.lang.Object;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * TemplateCaching facility, will cache templates on machines to avoid copying
 * at the virtual machine instantiation time.
 * @author Jerome Dochez
 */
@Service
class TemplateCaching {

    // templates are organized by machine so I can multi-task the machine refresh without getting into
    // disk contention.
    final Map<Machine, Set<RemoteTemplate>> templateList = new ConcurrentHashMap<Machine, Set<RemoteTemplate>>();
    final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    final ExecutorService byMachineExecutor = Executors.newCachedThreadPool();
    final Virtualization virtualization;
    final ScheduledFuture<?> cacheTask;

    private static class TemplateCachingInstance {

        private final static TemplateCachingInstance instance = new TemplateCachingInstance();

        synchronized TemplateCaching register(RemoteTemplate template) {

            Virtualization templateVirt = template.getDefinition().getVirtualization();
            TemplateCaching cache=null;
            for (TemplateCaching existingCache : caches) {
                if (existingCache.virtualization.getName().equals(templateVirt.getName())) {
                    cache = existingCache;
                    break;
                }
            }
            if (cache==null) {
                cache = new TemplateCaching(templateVirt);
                caches.add(cache);
            }
            Set<RemoteTemplate> templateByMachine = cache.templateList.get(template.getMachine());
            if (templateByMachine==null) {
                templateByMachine = new HashSet<RemoteTemplate>();
                cache.templateList.put(template.getMachine(), templateByMachine);
            }
            templateByMachine.add(template);
            try {
                template.cleanCache(new Integer(templateVirt.getTemplateCacheSize()));
            } catch (IOException e) {
                RuntimeContext.logger.log(Level.WARNING,
                        "Warning, exception while cleaning cache on " + template.getMachine(), e);
            }
            return cache;
        }

    }

    static TemplateCaching register(RemoteTemplate template) {
        return TemplateCachingInstance.instance.register(template);
    }

    static private final List<TemplateCaching> caches = new ArrayList<TemplateCaching>();

    private TemplateCaching(final Virtualization virtualization) {
        this.virtualization = virtualization;
        cacheTask = executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                int vmInStartup = Dom.unwrap(virtualization).getHabitat().
                        forContract(VirtualMachineLifecycle.class).get().vmInStartup();
                if (vmInStartup>0) {
                    RuntimeContext.logger.log(Level.INFO, "IMS in the process of allocating "
                            + vmInStartup + " virtual machines, cache refresh delayed");
                    return;
                } else {
                    RuntimeContext.logger.log(Level.INFO, "IMS cache refresh started");
                }
                for (final Machine machine : templateList.keySet()) {
                    // spawns a new thread for each machine to multi-thread the caches updates.
                    byMachineExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (RemoteTemplate template : templateList.get(machine)) {
                                try {
                                    template.refreshCache(new Integer(virtualization.getTemplateCacheSize()));
                                } catch (Exception e) {
                                    RuntimeContext.logger.log(Level.WARNING,
                                            "Exception while updating cache on machine " + machine, e);
                                }
                            }
                        }
                    });
                }
            }
        }, 0, new Long(virtualization.getTemplateCacheRefreshRate()), TimeUnit.SECONDS);
    }

    void unregister(RemoteTemplate template) {
        templateList.remove(template);
    }

    void cancel() {
        cacheTask.cancel(false);
    }

}
