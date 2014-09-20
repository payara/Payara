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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina;


import java.beans.PropertyChangeListener;


/**
 * A <b>Loader</b> represents a Java ClassLoader implementation that can
 * be used by a Container to load class files (within a repository associated
 * with the Loader) that are designed to be reloaded upon request, as well as
 * a mechanism to detect whether changes have occurred in the underlying
 * repository.
 * <p>
 * In order for a <code>Loader</code> implementation to successfully operate
 * with a <code>Context</code> implementation that implements reloading, it
 * must obey the following constraints:
 * <ul>
 * <li>Must implement <code>Lifecycle</code> so that the Context can indicate
 *     that a new class loader is required.
 * <li>The <code>start()</code> method must unconditionally create a new
 *     <code>ClassLoader</code> implementation.
 * <li>The <code>stop()</code> method must throw away its reference to the
 *     <code>ClassLoader</code> previously utilized, so that the class loader,
 *     all classes loaded by it, and all objects of those classes, can be
 *     garbage collected.
 * <li>Must allow a call to <code>stop()</code> to be followed by a call to
 *     <code>start()</code> on the same <code>Loader</code> instance.
 * <li>Based on a policy chosen by the implementation, must call the
 *     <code>Context.reload()</code> method on the owning <code>Context</code>
 *     when a change to one or more of the class files loaded by this class
 *     loader is detected.
 * </ul>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.3 $ $Date: 2005/12/08 01:27:18 $
 */

public interface Loader {


    // ------------------------------------------------------------- Properties


    /**
     * Return the Java class loader to be used by this Container.
     */
    public ClassLoader getClassLoader();


    /**
     * Return the Container with which this Loader has been associated.
     */
    public Container getContainer();


    /**
     * Set the Container with which this Loader has been associated.
     *
     * @param container The associated Container
     */
    public void setContainer(Container container);

    
    /**
     * Return the "follow standard delegation model" flag used to configure
     * our ClassLoader.
     */
    public boolean getDelegate();


    /**
     * Set the "follow standard delegation model" flag used to configure
     * our ClassLoader.
     *
     * @param delegate The new flag
     */
    public void setDelegate(boolean delegate);


    /**
     * Return descriptive information about this Loader implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo();


    /**
     * Return the reloadable flag for this Loader.
     */
    public boolean getReloadable();


    /**
     * Set the reloadable flag for this Loader.
     *
     * @param reloadable The new reloadable flag
     */
    public void setReloadable(boolean reloadable);


    // --------------------------------------------------------- Public Methods


    /**
     * Add a property change listener to this component.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);


    /**
     * Add a new repository to the set of repositories for this class loader.
     *
     * @param repository Repository to be added
     */
    public void addRepository(String repository);


    /**
     * Return the set of repositories defined for this class loader.
     * If none are defined, a zero-length array is returned.
     */
    public String[] findRepositories();


    /**
     * Has the internal repository associated with this Loader been modified,
     * such that the loaded classes should be reloaded?
     */
    public boolean modified();


    /**
     * Remove a property change listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);


    // START PE 4985680
    /**
     * Adds the given package name to the list of packages that may always be
     * overriden, regardless of whether they belong to a protected namespace
     */
    public void addOverridablePackage(String packageName);
    // END PE 4985680


    // START PWC 1.1 6314481
    public void setIgnoreHiddenJarFiles(boolean ignoreHiddenJarFiles);
    // END PWC 1.1 6314481
}
   
