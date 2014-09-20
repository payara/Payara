/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.services.impl.monitor.stats;

import org.glassfish.external.probe.provider.annotations.ProbeListener;
import org.glassfish.external.probe.provider.annotations.ProbeParam;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedObject;

/**
 * Server wide File cache statistics
 * 
 * @author Amy Roh
 */
@AMXMetadata(type="file-cache-mon", group="monitoring")
@ManagedObject
@Description("File Cache Statistics")
public class FileCacheStatsProviderGlobal extends FileCacheStatsProvider {
    
    public FileCacheStatsProviderGlobal(String name) {
        super(name);
    }

    @ProbeListener("glassfish:kernel:file-cache:countHitEvent")
    public void countHitEvent(@ProbeParam("fileCacheName") String fileCacheName) {
        hitsCount.increment();
    }

    @ProbeListener("glassfish:kernel:file-cache:countMissEvent")
    public void countMissEvent(@ProbeParam("fileCacheName") String fileCacheName) {
        missesCount.increment();
    }

    @ProbeListener("glassfish:kernel:file-cache:countInfoHitEvent")
    public void countInfoHitEvent(@ProbeParam("fileCacheName") String fileCacheName) {
        infoHitsCount.increment();
    }

    @ProbeListener("glassfish:kernel:file-cache:countInfoMissEvent")
    public void countInfoMissEvent(@ProbeParam("fileCacheName") String fileCacheName) {
        infoMissesCount.increment();
    }

    @ProbeListener("glassfish:kernel:file-cache:countContentHitEvent")
    public void countContentHitEvent(@ProbeParam("fileCacheName") String fileCacheName) {
        contentHitsCount.increment();
    }

    @ProbeListener("glassfish:kernel:file-cache:countContentMissEvent")
    public void countContentMissEvent(@ProbeParam("fileCacheName") String fileCacheName) {
        contentMissesCount.increment();
    }

    @ProbeListener("glassfish:kernel:file-cache:incOpenCacheEntriesEvent")
    public void incOpenCacheEntriesEvent(@ProbeParam("fileCacheName") String fileCacheName) {
        openCacheEntriesCount.increment();
    }

    @ProbeListener("glassfish:kernel:file-cache:decOpenCacheEntriesEvent")
    public void decOpenCacheEntriesEvent(@ProbeParam("fileCacheName") String fileCacheName) {
        openCacheEntriesCount.decrement();
    }

    @ProbeListener("glassfish:kernel:file-cache:addHeapSizeEvent")
    public void addHeapSizeEvent(@ProbeParam("fileCacheName") String fileCacheName,
            @ProbeParam("size") long size) {
        final long newSize = heapSize.addAndGet(size);
        for(;;) {
            final long maxSize = maxHeapSize.get();
            if (newSize > maxSize) {
                if (maxHeapSize.compareAndSet(maxSize, newSize)) {
                    break;
                }
            } else {
                break;
            }
        }
    }

    @ProbeListener("glassfish:kernel:file-cache:subHeapSizeEvent")
    public void subHeapSizeEvent(@ProbeParam("fileCacheName") String fileCacheName,
            @ProbeParam("size") long size) {
        heapSize.addAndGet(-size);
    }

    @ProbeListener("glassfish:kernel:file-cache:addMappedMemorySizeEvent")
    public void addMappedMemorySizeEvent(
            @ProbeParam("fileCacheName") String fileCacheName,
            @ProbeParam("size") long size) {
        final long newSize = mappedMemorySize.addAndGet(size);
        for(;;) {
            final long maxMemSize = maxMappedMemorySize.get();
            if (newSize > maxMemSize) {
                if (maxMappedMemorySize.compareAndSet(maxMemSize, newSize)) {
                    break;
                }
            } else {
                break;
            }
        }
    }

    @ProbeListener("glassfish:kernel:file-cache:subMappedMemorySizeEvent")
    public void subMappedMemorySizeEvent(
            @ProbeParam("fileCacheName") String fileCacheName,
            @ProbeParam("size") long size) {
        mappedMemorySize.addAndGet(-size);
    }    
}
