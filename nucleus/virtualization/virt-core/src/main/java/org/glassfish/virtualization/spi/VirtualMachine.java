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

package org.glassfish.virtualization.spi;

import org.glassfish.virtualization.config.VirtUser;
import org.glassfish.virtualization.config.VirtualMachineConfig;

import java.io.IOException;

/**
 * Defines a Virtual machine
 * @author Jerome Dochez
 */
public interface VirtualMachine {

    public enum PropertyName { INSTALL_DIR };

    /**
     * Returns the machine's name
     * @return machine's name as it is registered in the configuration
     */
    String getName();

    /**
     * Returns the IP address of the machine, which can be varying at runtime
     *
     * @return the machine's IP address
     */
    String getAddress();

    /**
     * Sets the IO address of the machine, usually performed by a back end
     * operation.
     * @param address the new IP address
     */
    void setAddress(String address);

    /**
     * Starts the virtual machine
     *
     * @throws VirtException if the request to start failed.
     */
    void start() throws VirtException;

    /**
     * Suspend the virtual machine
     * @throws VirtException if the request to suspend failed
     */
    void suspend() throws VirtException;

    /**
     * Resumes the virtual machine.
     *
     * @throws VirtException if the request to resume failed
     */
    void resume() throws VirtException;

    /**
     * Stops a virtual machine.
     *
     * @throws VirtException if the request to stop failed
     */
    void stop() throws VirtException;

    /**
     * Deletes the virtual machine and all associated storage
     *
     * @throws VirtException if the deletion failed
     */
    void delete() throws VirtException;

    /**
     * Returns the current machine information
     * @return the machine's current information
     */
    VirtualMachineInfo getInfo();

    /**
     * Returns the server pool this virtual machine was allocated on
     * @return the server pool
     */
    ServerPool getServerPool();

    /**
     * Returns the machine (if exists) on which this virtual machine
     * was allocated on or null if there is no notion of Machines on
     * the server pool used.
     * @return the machine
     */
    Machine getMachine();

    /**
     * Returns the user used to run software in this virtual machine
     * @return the virtual machine user
     */
    VirtUser getUser();

    /**
     * Sets a {@link PropertyName} property on this virtual machine instance.
     *
     * @param name the property name
     * @param value the property value
     */
    void setProperty(PropertyName name, String value);

    /**
     * Gets a property value by its name as defined in {@link PropertyName}
     * @param name the requested property name.
     * @return the property value if found, or null if not found.
     */
    String getProperty(PropertyName name);


    String executeOn(String[] args) throws IOException, InterruptedException;

    /**
     * Returns the persisted information for this virtual machine.
     *
     * @return the configuration of this virtual machine
     */
    VirtualMachineConfig getConfig();
}
