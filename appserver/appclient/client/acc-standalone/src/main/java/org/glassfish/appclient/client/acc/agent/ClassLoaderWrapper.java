/*
 * Copyright (c) 2021 Contributors to the Eclipse Foundation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.appclient.client.acc.agent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ClassLoaderWrapper extends ClassLoader implements Supplier<ClassLoader>, Consumer<ClassLoader> {

    private ClassLoader wrapped;

    public ClassLoaderWrapper(ClassLoader wrapped) {
        super(wrapped.getName(), wrapped);
        this.wrapped = wrapped;
    }

    public ClassLoader getWrapped() {
        return this.wrapped;
    }

    public void setWrapped(ClassLoader wrapped) {
        this.wrapped = wrapped;
    }

    public void accept(ClassLoader t) {
        setWrapped(t);
    }

    public ClassLoader get() {
        return getWrapped();
    }

    public int hashCode() {
        return this.wrapped.hashCode();
    }

    public boolean equals(Object obj) {
        return this.wrapped.equals(obj);
    }

    public String toString() {
        return this.wrapped.toString();
    }

    public String getName() {
        if (this.wrapped == null) {
            return super.getName();
        }

        return this.wrapped.getName();
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.wrapped.loadClass(name);
    }

    public URL getResource(String name) {
        return this.wrapped.getResource(name);
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        return this.wrapped.getResources(name);
    }

    public Stream<URL> resources(String name) {
        return this.wrapped.resources(name);
    }

    public InputStream getResourceAsStream(String name) {
        return this.wrapped.getResourceAsStream(name);
    }

    public void setDefaultAssertionStatus(boolean enabled) {
        this.wrapped.setDefaultAssertionStatus(enabled);
    }

    public void setPackageAssertionStatus(String packageName, boolean enabled) {
        this.wrapped.setPackageAssertionStatus(packageName, enabled);
    }

    public void setClassAssertionStatus(String className, boolean enabled) {
        this.wrapped.setClassAssertionStatus(className, enabled);
    }

    public void clearAssertionStatus() {
        this.wrapped.clearAssertionStatus();
    }
}
