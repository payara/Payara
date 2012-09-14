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

import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.glassfish.api.deployment.archive.ReadableArchive;

/**
 * A <strong>lot</strong> of methods need to be written in order to implement
 * ReadableArchive.  The no-op methods are implemented here to make ScatteredWar
 * easier to understand.
 * @author Byron Nevins
 */
abstract public class ReadableArchiveAdapter implements ReadableArchive{

    public long getEntrySize(String arg0) {
        return 0L;
    }

    public void open(URI arg0) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ReadableArchive getSubArchive(String arg0) throws IOException {
        return null;
    }

    public boolean delete() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean renameTo(String arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void close() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getArchiveSize() throws SecurityException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean exists() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Enumeration<String> entries() {
        return null;
    }

    public Enumeration<String> entries(String prefix) {
        return null;
    }

    public Collection<String> getDirectories() throws IOException  {
        return null;
    }

    public boolean isDirectory (java.lang.String name) {
        return false;
    }

    public void setParentArchive(ReadableArchive parentArchive) {
    }

    public ReadableArchive getParentArchive() {
        return null;
    }

    public <U> U getExtraData(Class<U> dataType) {
        return null;
    }

    public <U> void setExtraData(Class<U> dataType, U instance) {
    }

    public <U> void removeExtraData(Class<U> dataType) {
    }

    public void addArchiveMetaData(String metaDataKey, Object metaData) {
    }

    public <T> T getArchiveMetaData(String metaDataKey, Class<T> metadataType) {
        return null;
    }

    public void removeArchiveMetaData(String metaDataKey) {
    }

}
