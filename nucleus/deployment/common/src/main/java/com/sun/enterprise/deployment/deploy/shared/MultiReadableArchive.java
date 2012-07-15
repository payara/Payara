/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.deploy.shared;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deploy.shared.AbstractReadableArchive;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.jar.Manifest;
import org.glassfish.api.deployment.archive.ReadableArchive;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 * Implements ReadableArchive based on multiple underlying ReadableArchives,
 * each of which will be processed in order when looking up entries, finding
 * the manifest, etc.
 * 
 * @author tjquinn
 */
@Service
@PerLookup
public class MultiReadableArchive extends AbstractReadableArchive {

    private ReadableArchive parentArchive = null;

    private ReadableArchive[] archives;

    @Inject
    private ArchiveFactory archiveFactory;

    @Override
    public InputStream getEntry(String name) throws IOException {
        for (ReadableArchive ra : archives) {
            final InputStream is = ra.getEntry(name);
            if (is != null) {
                return is;
            }
        }
        return null;
    }

    @Override
    public boolean exists(String name) throws IOException {
        for (ReadableArchive ra : archives) {
            if (ra.exists(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getEntrySize(String name) {
        for (ReadableArchive ra : archives) {
            final long size = ra.getEntrySize(name);
            if (size != 0) {
                return size;
            }
        }
        return 0;
    }

    @Override
    public void open(URI uri) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void open(URI... uris) throws IOException {
        archives = new ReadableArchive[uris.length];
        int slot = 0;
        for (URI uri : uris) {
            archives[slot++] = archiveFactory.openArchive(uri);
        }
    }

    @Override
    public ReadableArchive getSubArchive(String name) throws IOException {
        for (ReadableArchive ra : archives) {
            final ReadableArchive subArchive = ra.getSubArchive(name);
            if (subArchive != null) {
                return subArchive;
            }
        }
        return null;
    }

    @Override
    public boolean exists() {
        boolean result = true;
        for (ReadableArchive ra : archives) {
            result &= ra.exists();
        }
        return result;
    }

    @Override
    public boolean delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean renameTo(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setParentArchive(ReadableArchive parentArchive) {
        this.parentArchive = parentArchive;
    }

    @Override
    public ReadableArchive getParentArchive() {
        return parentArchive;
    }

    @Override
    public void close() throws IOException {
        for (ReadableArchive ra : archives) {
            ra.close();
        }
    }

    @Override
    public Enumeration<String> entries() {
        /*
         * Guard against the same entry appearing in multiple archives.  
         * Only one will be returned so only save the name once.
         */
        final LinkedHashSet<String> enums = new LinkedHashSet<String>();
        for (ReadableArchive ra : archives) {
            for (Enumeration<String> e = ra.entries(); e.hasMoreElements(); ) {
                enums.add(e.nextElement());
            }
        }
        return Collections.enumeration(enums);
    }

    @Override
    public Enumeration<String> entries(String prefix) {
        final LinkedHashSet<String> enums = new LinkedHashSet<String>();
        for (ReadableArchive ra : archives) {
            for (Enumeration<String> e = ra.entries(prefix); e.hasMoreElements();) {
                enums.add(e.nextElement());
            }
        }
        return Collections.enumeration(enums);
    }

    @Override
    public Collection<String> getDirectories() throws IOException {
        final Collection<String> result = new LinkedHashSet<String>();
        for (ReadableArchive ra : archives) {
            result.addAll(ra.getDirectories());
        }
        return result;
    }

    @Override
    public boolean isDirectory(String name) {
        for (ReadableArchive ra : archives) {
            if (ra.isDirectory(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Manifest getManifest() throws IOException {
        for (ReadableArchive ra : archives) {
            final Manifest mf = ra.getManifest();
            if (mf != null) {
                return mf;
            }
        }
        return null;
    }

    @Override
    public URI getURI() {
        /*
         * This is not arbitrary.  By convention, the facade ReadableArchive
         * will be listed first when the MultiReadableArchive is created.
         * The URI returned from this method is, for example, added to a
         * class path.  The facade JAR points to the client JAR so adding
         * the facade URI to a class path essentially adds both.
         */
        return archives[0].getURI();
    }

    public URI getURI(final int slot) {
        return archives[slot].getURI();
    }
    
    @Override
    public long getArchiveSize() throws SecurityException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        final StringBuilder name = new StringBuilder();
        for (ReadableArchive a : archives) {
            if (name.length() > 0) {
                name.append(",");
            }
            name.append(a.getName());
        }
        return name.toString();
    }
}
