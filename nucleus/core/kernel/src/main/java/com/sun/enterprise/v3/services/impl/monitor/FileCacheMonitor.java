/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.services.impl.monitor;

import com.sun.enterprise.v3.services.impl.monitor.stats.FileCacheStatsProvider;
import org.glassfish.grizzly.http.server.filecache.FileCache;
import org.glassfish.grizzly.http.server.filecache.FileCacheEntry;
import org.glassfish.grizzly.http.server.filecache.FileCacheProbe;

/**
 *
 * @author oleksiys
 */
public class FileCacheMonitor implements FileCacheProbe {
    private final GrizzlyMonitoring grizzlyMonitoring;
    private final String monitoringId;

    public FileCacheMonitor(GrizzlyMonitoring grizzlyMonitoring,
            String monitoringId, FileCache config) {
        this.grizzlyMonitoring = grizzlyMonitoring;
        this.monitoringId = monitoringId;

        if (grizzlyMonitoring != null) {
            final FileCacheStatsProvider statsProvider =
                    grizzlyMonitoring.getFileCacheStatsProvider(monitoringId);
            if (statsProvider != null) {
                statsProvider.setStatsObject(config);
            }

//            statsProvider.reset();
        }
    }

    @Override
    public void onEntryAddedEvent(final FileCache fileCache, final FileCacheEntry entry) {
        grizzlyMonitoring.getFileCacheProbeProvider().incOpenCacheEntriesEvent(monitoringId);
        switch (entry.type) {
            case HEAP: {
                grizzlyMonitoring.getFileCacheProbeProvider().addHeapSizeEvent(monitoringId, entry.getFileSize(false));
                break;
            }
            case MAPPED: {
                grizzlyMonitoring.getFileCacheProbeProvider().addMappedMemorySizeEvent(monitoringId, entry.getFileSize(false));
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected type: " + entry.type);
            }
        }

    }

    @Override
    public void onEntryRemovedEvent(final FileCache fileCache, final FileCacheEntry entry) {
        grizzlyMonitoring.getFileCacheProbeProvider().decOpenCacheEntriesEvent(monitoringId);
        switch (entry.type) {
            case HEAP: {
                grizzlyMonitoring.getFileCacheProbeProvider().subHeapSizeEvent(monitoringId, entry.getFileSize(false));
                break;
            }
            case MAPPED: {
                grizzlyMonitoring.getFileCacheProbeProvider().subMappedMemorySizeEvent(monitoringId, entry.getFileSize(false));
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected type: " + entry.type);
            }
        }
    }

    @Override
    public void onEntryHitEvent(final FileCache fileCache, final FileCacheEntry entry) {
        grizzlyMonitoring.getFileCacheProbeProvider().countHitEvent(monitoringId);

        switch (entry.type) {
            case HEAP: {
                grizzlyMonitoring.getFileCacheProbeProvider().countInfoHitEvent(monitoringId);
                break;
            }
            case MAPPED: {
                grizzlyMonitoring.getFileCacheProbeProvider().countContentHitEvent(monitoringId);
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected type: " + entry.type);
            }
        }
    }

    @Override
    public void onEntryMissedEvent(final FileCache fileCache, final String host, final String requestURI) {
        grizzlyMonitoring.getFileCacheProbeProvider().countMissEvent(monitoringId);
    }

    @Override
    public void onErrorEvent(final FileCache fileCache, final Throwable error) {
    }
}
