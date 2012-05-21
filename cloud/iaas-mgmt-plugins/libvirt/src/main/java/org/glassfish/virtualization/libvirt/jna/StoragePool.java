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

package org.glassfish.virtualization.libvirt.jna;

import org.glassfish.virtualization.spi.VirtException;

/**
 * Storage Pool JNA interface
 * @author Jerome Dochez
 */
public class StoragePool extends LibVirtObject {

    final StoragePoolPointer handle;

    public StoragePool(StoragePoolPointer handle) {
        this.handle = handle;
    }

    /**
     * Fetch the number of storage volumes within a pool
     *
     * @return the number of storage pools
     * @throws VirtException if an error occurs
     */
    public int numOfVolumes() throws VirtException {
        int returnValue = libvirt.virStoragePoolNumOfVolumes(handle);
        checkForError();
        return returnValue;
    }

    /**
     * Fetch list of storage volume names
     *
     * @return an Array of Strings that contains the names of the storage
     *         volumes
     * @throws VirtException if an error occurs
     */
    public String[] listVolumes() throws VirtException {
        int num = numOfVolumes();
        String[] returnValue = new String[num];
        libvirt.virStoragePoolListVolumes(handle, returnValue, num);
        checkForError();
        return returnValue;
    }

    /**
     * Fetch an object representing to a storage volume based on its name within
     * a pool
     *
     * @param name
     *            name of storage volume
     * @return The StorageVol object found
     * @throws VirtException if an error occurs
     */
    public StorageVol storageVolLookupByName(String name) throws VirtException {
        StorageVolPointer ptr = libvirt.virStorageVolLookupByName(handle, name);
        checkForError();
        return new StorageVol(ptr);
    }

    /**
     * Create a storage volume within a pool based on an XML description. Not
     * all pools support creation of volumes
     *
     * @param xmlDesc
     *            description of volume to create
     * @param flags
     *            flags for creation (unused, pass 0)
     * @return the storage volume
     * @throws VirtException if an error occurs
     */
    public StorageVol storageVolCreateXML(String xmlDesc, int flags) throws VirtException {
        StorageVolPointer ptr = libvirt.virStorageVolCreateXML(handle, xmlDesc, flags);
        checkForError();
        return new StorageVol(ptr);
    }

    /**
     * Delete the underlying pool resources. This is a non-recoverable
     * operation. The virStoragePool object itself is not free'd.
     *
     * @param flags flags for obliteration process
     * @throws VirtException if an error occurs
     */
    public void delete(int flags) throws VirtException {
        libvirt.virStoragePoolDelete(handle, flags);
        checkForError();
    }
}
