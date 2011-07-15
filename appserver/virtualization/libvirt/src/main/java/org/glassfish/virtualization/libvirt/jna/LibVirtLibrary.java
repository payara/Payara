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

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * JNA library interface definition
 * @author Jerome Dochez
 */
public interface LibVirtLibrary  extends Library {

    LibVirtLibrary INSTANCE = (LibVirtLibrary) Native.loadLibrary("virt", LibVirtLibrary.class);

    int virInitialize();


    int virCopyLastError(VirError error);
    void virResetLastError();

    ConnectionPointer virConnectOpen(String uri);
    int virConnectNumOfStoragePools(ConnectionPointer handle);
    void virConnectListStoragePools(ConnectionPointer handle, String[] returnValue, int size);
    StoragePoolPointer virStoragePoolLookupByName(ConnectionPointer handle, String name);
    StoragePoolPointer virStoragePoolCreateXML(ConnectionPointer handle, String xmlDesc, int flags);

    int virConnectNumOfDomains(ConnectionPointer handle);
    int virConnectNumOfDefinedDomains(ConnectionPointer handle);
    void virConnectListDomains(ConnectionPointer handle, int[] ids, int size);
    void virConnectListDefinedDomains(ConnectionPointer handle, String[] returnValue, int size);
    DomainPointer virDomainLookupByID(ConnectionPointer handle, int id);
    DomainPointer virDomainLookupByName(ConnectionPointer handle, String name);
    DomainPointer virDomainDefineXML(ConnectionPointer handle, String xmlDesc);
    String virDomainGetName(DomainPointer handle);
    String virDomainGetOSType(DomainPointer handle);
    int virDomainGetInfo(DomainPointer handle, DomainInfo returnValue);
    void virDomainUndefine(DomainPointer handle);
    void virDomainReboot(DomainPointer handle, int flags);
    int virDomainCreate(DomainPointer handle);
    void virDomainDestroy(DomainPointer handle);
    void virDomainSuspend(DomainPointer handle);
    void virDomainResume(DomainPointer handle);
    int virDomainGetID(DomainPointer handle);

    int virStoragePoolNumOfVolumes(StoragePoolPointer handle);
    void virStoragePoolListVolumes(StoragePoolPointer handle, String[] returnValue, int size);
    StorageVolPointer virStorageVolLookupByName(StoragePoolPointer handle, String name);
    void virStoragePoolDelete(StoragePoolPointer handle, int flags);
    StorageVolPointer virStorageVolCreateXML(StoragePoolPointer handle, String xmlDesc, int flags);

    void virStorageVolDelete(StorageVolPointer handle, int flags);
    String virStorageVolGetName(StorageVolPointer handle);
    String virStorageVolGetPath(StorageVolPointer handle);
}
