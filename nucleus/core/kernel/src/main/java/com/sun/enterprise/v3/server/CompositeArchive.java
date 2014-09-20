/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.server;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.CompositeHandler;
import com.sun.enterprise.deploy.shared.AbstractReadableArchive;

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.jar.Manifest;

/**
 * A composite archive is a readable archive that hides the sub archives.
 *
 * @author Jerome Dochez
 */
public class CompositeArchive extends AbstractReadableArchive {

    final ReadableArchive delegate;
    final CompositeHandler filter;

    public CompositeArchive(ReadableArchive delegate, CompositeHandler filter) {
        this.delegate = delegate;
        this.filter = filter;
    }

    public InputStream getEntry(String name) throws IOException {
        if (filter.accept(delegate, name)) {
            return delegate.getEntry(name);
        }
        return null;
    }

    public boolean exists(String name) throws IOException {
        if (filter.accept(delegate, name)) {
            return delegate.exists(name);                                    
        }
        return false;
    }

    public long getEntrySize(String name) {
        if (filter.accept(delegate, name)) {
            return delegate.getEntrySize(name);
        }
        return 0;
    }

    public void open(URI uri) throws IOException {
        delegate.open(uri);
    }

    public ReadableArchive getSubArchive(String name) throws IOException {
        if (filter.accept(delegate, name)) {
            return delegate.getSubArchive(name);
        }
        return null;
    }

    public boolean exists() {
        return delegate.exists();
    }

    public boolean delete() {
        return delegate.delete();
    }

    public boolean renameTo(String name) {
        return delegate.renameTo(name);
    }

    public void close() throws IOException {
        delegate.close();
    }

    public Enumeration<String> entries() {

        Enumeration<String> original = delegate.entries();
        Vector<String> results = new Vector<String>();
        while (original.hasMoreElements()) {
            String entryName = original.nextElement();
            if (filter.accept(delegate, entryName)) {
                results.add(entryName);
            }
        }
        return results.elements();
    }

    public Enumeration<String> entries(String prefix) {

        Enumeration<String> original = delegate.entries(prefix);
        Vector<String> results = new Vector<String>();
        while (original.hasMoreElements()) {
            String entryName = original.nextElement();
            if (filter.accept(delegate, entryName)) {
                results.add(entryName);
            }
        }
        return results.elements();
    }

    public boolean isDirectory(String name) {
        if (filter.accept(delegate, name)) {
            return delegate.isDirectory(name);
        }
        return false;
    }

    public Manifest getManifest() throws IOException {
        return delegate.getManifest();
    }

    public URI getURI() {
        return delegate.getURI();
    }

    public long getArchiveSize() throws SecurityException {
        return delegate.getArchiveSize();
    }

    public String getName() {
        return delegate.getName();
    }

    // we don't hide the top level directories as we need to use them
    // to figure out whether the EarSniffer can handle it in the 
    // case of optional application.xml
    public Collection<String> getDirectories() throws IOException {
        return delegate.getDirectories();
    }
}
