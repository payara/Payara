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
package org.glassfish.virtualization.libvirt;

import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.spi.StorageVol;
import org.glassfish.virtualization.spi.VirtException;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.Dom;
import org.w3c.dom.Node;

/**
 * Representation of a storage pool in libvirt.
 */
class LibVirtStorageVol implements StorageVol {

    private final LibVirtStoragePool owner;
    private final org.glassfish.virtualization.libvirt.jna.StorageVol volume;

    public LibVirtStorageVol(LibVirtStoragePool owner, org.glassfish.virtualization.libvirt.jna.StorageVol volume) {
        this.owner = owner;
        this.volume = volume;
    }

    @Override
    public String getName() throws VirtException {
        return volume.getName();
    }

    public String getPath() throws VirtException {
        return volume.getPath();

    }

    public synchronized void delete() throws VirtException {
        volume.delete(0);
    }

    public DiskReference getDiskReference() {
        // todo : this need to be improved.
        Habitat habitat = Dom.unwrap(owner.getMachine().getConfig()).getHabitat();
        Virtualization virtualization = owner.getMachine().getServerPool().getConfig().getVirtualization();
        return habitat.getComponent(DiskReference.class, virtualization.getName());
    }

    public Node getXML(Node parent, int position) throws VirtException {

        return getDiskReference().save(this.getPath(), parent, position);

    }
}
