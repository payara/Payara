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
package org.glassfish.virtualization.os;

import org.jvnet.hk2.annotations.Contract;

import java.io.File;
import java.io.IOException;

/**
 * Interface to create and manipulate a virtual disk
 * @author Jerome Dochez
 */
@Contract
public interface Disk {

    /**
     * Creates a virtual disk
     * @param path path to the virtual disk file
     * @param size size of the virtual disk
     * @param mountPoint directory where the virtual disk should be mounted
     * @return 0 if success, not 0 if failure (passed by the underlying native
     * mechanism
     * @throws IOException when the disk cannot be created.
     */
    public int create(File path, int size, File mountPoint) throws IOException;

    /**
     * Mount an existing virtual disk
     * @param path path to the virtual disk file
     * @param mountPoint directory to use to mount the virtual disk
     * @return 0 if success, not 0 if failure as returned by the native
     * mechanism
     * @throws IOException if the virtual disk cannot be mounted
     */
    public int mount(File path, File mountPoint) throws IOException;

    /**
     * Un-mount this virtual disk instance
     * @throws IOException if the disk cannot be un-mounted
     */
    public int umount() throws IOException;
    
        /**
     * create an ISO File with the content of a directory
     * @param sourceDirectory path to the directory content to put in the iso file
     * @param outputISOFile location of the created iso file. (It will be delete first if present)
     * @return 0 if success, not 0 if failure as returned by the native
     * mechanism
     * @throws IOException if the ISO file cannot be created
     */
    public int createISOFromDirectory(File sourceDirectory, File outputISOFile) throws IOException;

}
