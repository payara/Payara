/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.kernel;

import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.api.admin.FileMonitoring;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Jerome Dochez
 */
@Service
public class FileMonitoringImpl implements FileMonitoring, PostConstruct {

    @Inject
    ExecutorService executor;

    @Inject
    ScheduledExecutorService scheduledExecutor;

    @Inject
    Events events;
            
    final Map<File, List<FileChangeListener>> listeners = new HashMap<File, List<FileChangeListener>>();
    final Map<File, Long> monitored = new HashMap<File, Long>();

    public void postConstruct() {
        final ScheduledFuture<?> future = scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                if (monitored.isEmpty()) {
                    return;
                }
                // check our list of monitored files for any changes
                Set<File> monitoredFiles = new HashSet<File>();
                monitoredFiles.addAll(listeners.keySet());
                for (File file : monitoredFiles) {
                    if (!file.exists()) {
                        removed(file);
                        listeners.remove(file);
                        monitored.remove(file);
                    } else 
                    if (file.lastModified()!=monitored.get(file)) {
                        // file has changed
                        monitored.put(file, file.lastModified());
                        changed(file);
                    }
                }

            }
        }, 0, 500, TimeUnit.MILLISECONDS);
        events.register(new org.glassfish.api.event.EventListener() {
            @Override
            public void event(Event event) {
                if (event.is(EventTypes.PREPARE_SHUTDOWN)) {
                    System.out.println("FileMonitoring shutdown");
                    future.cancel(false);
                }
            }
        });
    }

    public synchronized void monitors(File file, FileChangeListener listener) {

        if (monitored.containsKey(file)) {
            listeners.get(file).add(listener);
        } else {
            List<FileChangeListener> list = new ArrayList<FileChangeListener>();
            list.add(listener);
            listeners.put(file, list);
            monitored.put(file, file.lastModified());
        }
    }

    public synchronized void fileModified(File file) {
        monitored.put(file, 0L);
    }

    private void removed(final File file) {
        for (final FileChangeListener listener : listeners.get(file)) {
            executor.submit(new Runnable() {
                public void run() {
                    listener.deleted(file);
                }
            });
        }

    }

    private void changed(final File file) {
        for (final FileChangeListener listener : listeners.get(file)) {
            executor.submit(new Runnable() {
                public void run() {
                    listener.changed(file);
                }
            });
        }
    }
}
