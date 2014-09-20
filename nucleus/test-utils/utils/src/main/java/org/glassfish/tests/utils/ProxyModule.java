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

package org.glassfish.tests.utils;

import com.sun.enterprise.module.*;

import java.util.List;
import java.io.PrintStream;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: Nov 19, 2008
 * Time: 11:29:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProxyModule implements Module {

    final ClassLoader classLoader;
    final ModuleDefinition moduleDef;
    final ModulesRegistry modulesRegistry;

    public ProxyModule(ModulesRegistry registry, ModuleDefinition moduleDef, ClassLoader cl) {
        this.classLoader = cl;
        this.moduleDef = moduleDef;
        this.modulesRegistry = registry;
    }

    public ModuleDefinition getModuleDefinition() {
        return moduleDef;
    }

    public String getName() {
        return moduleDef.getName();
    }

    public ModulesRegistry getRegistry() {
        return modulesRegistry;
    }

    public ModuleState getState() {
        return ModuleState.READY;
    }

    public void resolve() throws ResolveError {

    }

    public void start() throws ResolveError {

    }

    public boolean stop() {
        return false;
    }

    public void detach() {
    }

    public void refresh() {
    }

    public ModuleMetadata getMetadata() {
        return moduleDef.getMetadata();
    }

    public void addListener(ModuleChangeListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeListener(ModuleChangeListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public List<Module> getImports() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addImport(Module module) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Module addImport(ModuleDependency dependency) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isShared() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isSticky() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setSticky(boolean sticky) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> Iterable<Class<? extends T>> getProvidersClass(Class<T> serviceClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Iterable<Class> getProvidersClass(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean hasProvider(Class serviceClass) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void dumpState(PrintStream writer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void uninstall() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
