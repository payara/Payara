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
 * Virtual Machine (Domain) interface
 * @author Jerome Dochez
 */
public class Domain extends LibVirtObject {

    final DomainPointer handle;

    public Domain(DomainPointer handle) {
        this.handle = handle;
    }

    /**
     * Gets the public name for this domain
     *
     * @return the name
     * @throws VirtException if an error occurs
     */
    public String getName() throws VirtException {
        String returnValue = libvirt.virDomainGetName(handle);
        checkForError();
        return returnValue;
    }

    /**
     * Gets the type of domain operation system.
     *
     * @return the type
     * @throws VirtException if an error occurs
     */
    public String getOSType() throws VirtException {
        String returnValue = libvirt.virDomainGetOSType(handle);
        checkForError();
        return returnValue;
    }

    /**
     * Gets the hypervisor ID number for the domain
     *
     * @return the hypervisor ID
     * @throws VirtException if an error occurs
     */
    public int getID() throws VirtException {
        int returnValue = libvirt.virDomainGetID(handle);
        checkForError();
        return returnValue;
    }

    /**
     * Launches this defined domain. If the call succeed the domain moves from
     * the defined to the running domains pools.
     *
     * @throws VirtException if an error occurs.
     * @return 	0 in case of success, -1 in case of error
     */
    public int create() throws VirtException {
        int returnValue = libvirt.virDomainCreate(handle);
        checkForError();
        return returnValue;
    }

    /**
     * Destroys this domain object. The running instance is shutdown if not down
     * already and all resources used by it are given back to the hypervisor.
     * The data structure is freed and should not be used thereafter if the call
     * does not return an error. This function may requires priviledged access
     *
     * @throws VirtException if an error occurs
     */
    public void destroy() throws VirtException {
        libvirt.virDomainDestroy(handle);
        checkForError();
    }

    /**
     * Suspends this active domain, the process is frozen without further access
     * to CPU resources and I/O but the memory used by the domain at the
     * hypervisor level will stay allocated. Use Domain.resume() to reactivate
     * the domain. This function requires priviledged access.
     *
     * @throws VirtException if a error occurs
     */
    public void suspend() throws VirtException {
        libvirt.virDomainSuspend(handle);
        checkForError();
    }

   /**
     * undefines this domain but does not stop it if it is running
     *
     * @throws VirtException if an error occurs
     */
    public void undefine() throws VirtException {
        libvirt.virDomainUndefine(handle);
        checkForError();
    }

    /**
     * Reboot this domain, the domain object is still usable there after but the
     * domain OS is being stopped for a restart. Note that the guest OS may
     * ignore the request.
     *
     * @param flags extra flags for the reboot operation, not used yet
     * @throws VirtException if an error occurs
     */
    public void reboot(int flags) throws VirtException {
        libvirt.virDomainReboot(handle, flags);
        checkForError();
    }

    /**
     * Resume this suspended domain, the process is restarted from the state
     * where it was frozen by calling virSuspendDomain(). This function may
     * requires privileged access
     *
     * @throws VirtException if an error occurs
     */
    public void resume() throws VirtException {
        libvirt.virDomainResume(handle);
        checkForError();
    }

    /**
     * Returns the domain information object
     *
     * @return  this domain information object
     * @throws VirtException if an error occurs.
     */
    public DomainInfo getInfo() throws VirtException {
        DomainInfo returnValue = new DomainInfo();
        int success = libvirt.virDomainGetInfo(handle, returnValue);
        checkForError();
        return (success==0?returnValue:null);
    }
}
