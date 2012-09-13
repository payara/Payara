/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;

/**
 * Interface for implementing read access to an underlying archive on a unspecified medium
 *
 * @author Jerome Dochez
 */
@Contract
public interface ReadableArchive extends Archive {

    /**
     * Returns the InputStream for the given entry name
     * The file name must be relative to the root of the module.
     *
     * @param name the file name relative to the root of the module.
     * @return the InputStream for the given entry name or null if not found.
     */
    public InputStream getEntry(String name) throws IOException;

    /**
     * Returns the existence of the given entry name
     * The file name must be relative to the root of the module.
     *
     * @param name the file name relative to the root of the module.
     * @return the existence the given entry name.
     */
    public boolean exists(String name) throws IOException;

    /**
     * Returns the entry size for a given entry name or 0 if not known
     *
     * @param name the entry name
     * @return the entry size
     */
    public long getEntrySize(String name);

    /**
     * Open an abstract archive
     *
     * @param uri path to the archive
     */
    public void open(URI uri) throws IOException;

    /**
     * Returns an instance of this archive abstraction for an embedded
     * archive within this archive.
     *
     * @param name is the entry name relative to the root for the archive
     * @return
     *      the Archive instance for this abstraction,
     *      or null if no such entry exists.
     */
    public ReadableArchive getSubArchive(String name) throws IOException;

    /**
     * @return true if this archive exists
     */
    public boolean exists();

    /**
     * deletes the archive
     */
    public boolean delete();

    /**
     * rename the archive
     *
     * @param name the archive name
     */
    public boolean renameTo(String name);

    /**
     * set the parent archive for this archive
     *
     * @param parentArchive the parent archive
     */
    public void setParentArchive(ReadableArchive parentArchive);

    /**
     * get the parent archive of this archive
     *
     * @return the parent archive
     */
    public ReadableArchive getParentArchive();

    /**
     * Returns any data that could have been calculated as part of
     * the descriptor loading.
     *
     * @param dataType the type of the extra data
     * @return the extra data or null if there are not an instance of
     * type dataType registered.
     */
    public <U> U getExtraData(Class<U> dataType);

    public <U> void setExtraData(Class<U> dataType, U instance);

    public <U> void removeExtraData(Class<U> dataType);

    public void addArchiveMetaData(String metaDataKey, Object metaData);

    public <T> T getArchiveMetaData(String metaDataKey, Class<T> metadataType);

    public void removeArchiveMetaData(String metaDataKey);
}
