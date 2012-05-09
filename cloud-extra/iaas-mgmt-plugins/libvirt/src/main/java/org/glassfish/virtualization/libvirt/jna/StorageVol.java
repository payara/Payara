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
 * Storage Volume JNA interface
 * @author Jerome Dochez
 */
public class StorageVol extends LibVirtObject {

    private final StorageVolPointer handle;

    public StorageVol(StorageVolPointer handle) {
        this.handle = handle;
    }

    /**
     * Delete the storage volume from the pool
     *
     * @param flags
     *            future flags, use 0 for now
     * @throws VirtException if an error occurs
     */
    public void delete(int flags) throws VirtException {
        libvirt.virStorageVolDelete(handle, flags);
        checkForError();
    }

    /**
     * Fetch the storage volume name. This is unique within the scope of a pool
     *
     * @return the name
     * @throws VirtException if an error occurs
     */
    public String getName() throws VirtException {
        String returnValue = libvirt.virStorageVolGetName(handle);
        checkForError();
        return returnValue;
    }

    /**
     * Fetch the storage volume path. Depending on the pool configuration this
     * is either persistent across hosts, or dynamically assigned at pool
     * startup. Consult pool documentation for information on getting the
     * persistent naming
     *
     * @return the storage volume path
     * @throws VirtException if an error occurs
     */
    public String getPath() throws VirtException {
        String returnValue = libvirt.virStorageVolGetPath(handle);
        checkForError();
        return returnValue;
    }
}
