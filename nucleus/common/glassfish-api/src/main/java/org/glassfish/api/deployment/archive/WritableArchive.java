/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.api.deployment.archive;

import org.jvnet.hk2.annotations.Contract;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Interface for implementing write access to an underlying archive on a unspecified medium
 *
 * @author Jerome Dochez
 */
@Contract
public interface WritableArchive extends Archive {

    /**
     * creates a new abstract archive with the given path
     *
     * @param uri the path to create the archive
     */
    public void create(URI uri) throws IOException;

    /**
     * Close a previously returned sub archive
     *
     * @param subArchive output stream to close
     * @link Archive.getSubArchive}
     */
    public void closeEntry(WritableArchive subArchive) throws IOException;

    /**
     * Create a new entry in the archive
     *
     * @param name the entry name
     * @returns an @see java.io.OutputStream for a new entry in this
     * current abstract archive.
     */
    public OutputStream putNextEntry(String name) throws java.io.IOException;

    /**
     * closes the current entry
     */
    public void closeEntry() throws IOException;

    /**
     * Returns an instance of this archive abstraction for an embedded
     * archive within this archive.
     *
     * @param name is the entry name relative to the root for the archive
     * @return the Archive instance for this abstraction
     */
    public WritableArchive createSubArchive(String name) throws IOException;
}
