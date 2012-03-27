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

import org.glassfish.virtualization.libvirt.LibVirtError;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.util.RuntimeContext;

import java.util.logging.Level;

/**
 * Reprensents a low level connection to a libvirt library.
 * @author Jerome Dochez
 */
public class Connect extends LibVirtObject {

    // Load the native part
    static {
        LibVirtLibrary.INSTANCE.virInitialize();
        try {
            LibVirtError.processError(LibVirtLibrary.INSTANCE);
        } catch(VirtException e)  {
            RuntimeContext.logger.log(Level.SEVERE, "Error initializing libvirt library", e);
        }
    }

    final ConnectionPointer handle;


    /**
     * This function should be called first to get a connection to the Hypervisor and xen store
     *
     * @see <a href="http://libvirt.org/html/libvirt-libvirt.html#virConnectOpen">API
     * @param uri  URI of the hypervisor
     * @throws VirtException uf the connection cannot be established.
     */
    public Connect(String uri) throws VirtException {
        handle = libvirt.virConnectOpen(uri);
        LibVirtError.processError(libvirt);
    }

    /**
     * Provides the number of active domains.
     *
     * @return the number of active domains
     * @throws VirtException if an error occurs
     */
    public int numOfDomains() throws VirtException {
        int returnValue = libvirt.virConnectNumOfDomains(handle);
        checkForError();
        return returnValue;
    }

    /**
     * Lists the active domains.
     *
     * @return and array of the IDs of the active domains
     * @throws VirtException if an error occurs
     */
    public int[] listDomains() throws VirtException {
        int maxids = numOfDomains();
        int[] ids = new int[maxids];

        if (maxids > 0) {
            libvirt.virConnectListDomains(handle, ids, maxids);
            checkForError();
        }
        return ids;
    }

    /**
     * Provides the number of non active domains.
     *
     * @return the number of active domains
     * @throws VirtException if an error occurs
     */
    public int numOfDefinedDomains() throws VirtException {
        int returnValue = libvirt.virConnectNumOfDefinedDomains(handle);
        checkForError();
        return returnValue;
    }

    /**
     * Lists the names of the defined but inactive domains
     *
     * @see <a href="http://libvirt.org/html/libvirt-libvirt.html#virConnectListDefinedDomains">API
     *
     * @return an Array of Strings that contains the names of the defined
     *         domains currently inactive
     * @throws VirtException if an error occurs
     */
    public String[] listDefinedDomains() throws VirtException {
        int maxnames = numOfDefinedDomains();
        String[] names = new String[maxnames];
        if (maxnames > 0) {
            libvirt.virConnectListDefinedDomains(handle, names, maxnames);
            checkForError();
        }
        return names;
    }

    /**
     * Try to find a domain based on the hypervisor ID number
     * Note that this won't work for inactive domains which have an ID of -1,
     * in that case a lookup based on the Name or UUId need to be done instead.
     *
     * @see <a href="http://libvirt.org/html/libvirt-libvirt.html#virDomainLookupByID">API
     *
     * @param id the hypervisor id
     * @return the Domain object or null if the creation failed
     * @throws VirtException if an error occurs
     */
    public Domain domainLookupByID(int id) throws VirtException {
        DomainPointer ptr = libvirt.virDomainLookupByID(handle, id);
        checkForError();
        return (ptr==null?null:new Domain(ptr));
    }

    /**
     * Looks up a domain based on its name.
     *
     * @see <a href="http://libvirt.org/html/libvirt-libvirt.html#virDomainLookupByName">API
     *
     * @param name the name of the domain
     * @return the Domain object of null if it cannot be found
     * @throws VirtException if an error occurs
     */
    public Domain domainLookupByName(String name) throws VirtException {
        DomainPointer ptr = libvirt.virDomainLookupByName(handle, name);
        checkForError();
        return (ptr==null?null:new Domain(ptr));
    }

    /**
     * Provides the number of active storage pools
     *
     * @return the number of pools found
     * @throws VirtException if an error occurs
     */
    public int numOfStoragePools() throws VirtException {
        int returnValue = libvirt.virConnectNumOfStoragePools(handle);
        checkForError();
        return returnValue;
    }

    /**
     * Provides the list of names of active storage pools.
     *
     * @see <a href="http://libvirt.org/html/libvirt-libvirt.html#virConnectListStoragePools">API
     *
     * @return an Array of Strings that contains the names of the defined
     *         storage pools
     * @throws VirtException if an error occurs
     */
    public String[] listStoragePools() throws VirtException {
        int num = numOfStoragePools();
        String[] returnValue = new String[num];
        libvirt.virConnectListStoragePools(handle, returnValue, num);
        checkForError();
        return returnValue;
    }

    /**
     * Fetch a storage pool based on its unique name
     *
     * @see <a href="http://libvirt.org/html/libvirt-libvirt.html#virStoragePoolLookupByName">API
     *
     * @param name name of pool to fetch
     * @return 	a {@link StoragePool} object, or NULL if no matching pool is found
     * @throws VirtException if an error occurs
     */
    public StoragePool storagePoolLookupByName(String name) throws VirtException {
        StoragePoolPointer ptr = libvirt.virStoragePoolLookupByName(handle, name);
        checkForError();
        return (ptr==null?null:new StoragePool(ptr));
    }

    /**
     * Create a new storage based on its XML description. The pool is not
     * persistent, so its definition will disappear when it is destroyed, or if
     * the host is restarted
     *
     * @param xmlDesc XML description for new pool
     * @param flags future flags, use 0 for now
     * @return 	a {@link StoragePool} object, or NULL if creation failed
     * @throws VirtException if an error occurs
    */
    public StoragePool storagePoolCreateXML(String xmlDesc, int flags) throws VirtException {
        StoragePoolPointer ptr = libvirt.virStoragePoolCreateXML(handle, xmlDesc, flags);
        checkForError();
        return (ptr==null?null:new StoragePool(ptr));
    }

    /**
     * Defines a domain, but does not start it. his definition is persistent,
     * until explicitly undefined with virDomainUndefine().
     * A previous definition for this domain would be overriden if it already exists.
     *
     * @see <a href="http://libvirt.org/html/libvirt-libvirt.html#virDomainDefineXML" API
     *
     * @param xmlDesc the XML description for the domain, preferably in UTF-8
     * @return the Domain object, null in case of an error
     * @throws VirtException if an error occurs.
     * @see <a href="http://libvirt.org/format.html#Normal1" > The XML format
     *      description </a>
     */
    public Domain domainDefineXML(String xmlDesc) throws VirtException {
        Domain returnValue = null;
        DomainPointer ptr = libvirt.virDomainDefineXML(handle, xmlDesc);
        checkForError();
        return (ptr==null?null: new Domain(ptr));
    }

}
