
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.virtualization.libvirt;

import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.spi.StoragePool;
import org.glassfish.virtualization.spi.VirtException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Representation of a storage pool in libvirt
 *
 * @author Jerome Dochez
 */
class LibVirtStoragePool implements StoragePool {

    private final Machine owner;
    private final org.glassfish.virtualization.libvirt.jna.StoragePool pool;
    private final AtomicBoolean valid = new AtomicBoolean(true);

    LibVirtStoragePool(Machine owner, org.glassfish.virtualization.libvirt.jna.StoragePool pool) {
        this.owner = owner;
        this.pool = pool;
    }


    Machine getMachine() {
        return owner;
    }

    public Iterable<LibVirtStorageVol> volumes() throws VirtException {
        List<LibVirtStorageVol> volumes = new ArrayList<LibVirtStorageVol>();
        for (String volumeID : pool.listVolumes()) {
            volumes.add(new LibVirtStorageVol(this, pool.storageVolLookupByName(volumeID)));
        }
        return volumes;
    }

    @Override
    public LibVirtStorageVol allocate(String name, long capacity) throws VirtException {
        StringBuilder sb  = new StringBuilder();
        sb.append("<volume><name>").append(name).append(".img").append(
                "</name> <key>").append(owner).append("/").append(name).append(".img").append(
                "</key><source></source><capacity>").append(capacity).append("</capacity><allocation>0</allocation>").append(
                "<target><path>").append(owner).append("/").append(name).append(".img</path>").append(
                "<format type='raw'/><permissions><mode>0600</mode><owner>").append(owner.getUser().getUserId()).append(
                "</owner><group>").append(owner.getUser().getGroupId()).append(
                "</group><serverPool>").append(owner.getServerPool().getName()).append("</serverPool></permissions></target></volume>");

        return new LibVirtStorageVol(this, pool.storageVolCreateXML(sb.toString(), 0));

    }

    @Override
    public LibVirtStorageVol byName(String name) throws VirtException {
        try {
           for (String volumeID : pool.listVolumes()) {
               if (volumeID.equals(name+".img")) {
                   return new LibVirtStorageVol(this, pool.storageVolLookupByName(volumeID));
               }
           }
           return null;
        } catch(VirtException e) {
            throw new VirtException(e);
        }
    }

    /**
     * Deletes all associated volumes and itself
     * @throws VirtException when deletion of the virtual machine or associated storage failed.
     */
    @Override
    public void delete() throws VirtException {

        if (!valid.compareAndSet(true, false)) {
            // already deleted.
            return;
        }
        for (LibVirtStorageVol volume : volumes()) {
            volume.delete();
        }
        pool.delete(0);
    }
}
