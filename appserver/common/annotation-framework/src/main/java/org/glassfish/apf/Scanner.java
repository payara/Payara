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
// Portions Copyright [2018-2020] Payara Foundation and/or affiliates

package org.glassfish.apf;

import org.glassfish.hk2.classmodel.reflect.Types;
import org.jvnet.hk2.annotations.Contract;

import java.util.Set;
import java.io.File;
import java.io.IOException;

/** 
 * This interface is responsible for scanning the binary location 
 * provided and provide each binary file through a pull interfaces
 *
 * @author Jerome Dochez
 */
@Contract
public interface Scanner<T> {

    /**
     * Scan the archive file and gather a list of classes 
     * that should be processed for annotations
     * @param archiveFile the archive file for scanning
     * @param bundleDesc the bundle descriptor associated with this archive
     * @param classLoader the classloader used to scan the annotation
     * @throws IOException
     */
    public void process(File archiveFile, T bundleDesc, ClassLoader classLoader) throws IOException;

    
    /**
     * Returns a ClassLoader capable of loading classes from the 
     * underlying medium
     * @return a class loader capable of loading the classes
     */
    public ClassLoader getClassLoader();
    
    /**
     * Return a complete set of classes available from this location.
     * @return the complete set of classes 
     */
    public Set<Class> getElements();
    
    /**
     * Return a class instance available from this location from class name.
     *
     * @param classNames the list of class name
     * @return the set of classes for given names
     */
    public Set<Class> getElements(Set<String> classNames);

    /**
     * Sometimes, annotations processing requires more than a single class, 
     * especially when such classes end up being a Java Component (Java Beans, 
     * Java EE). The implementation returned from the getComponent will be 
     * responsible for defining the complete view of this component starting 
     * from it's implementation class.
     * @param componentImpl class of the component.
     */
    public ComponentInfo getComponentInfo(Class componentImpl);

    /**
     * Return the types information for this module
     * @return types the archive resulting types
     */
    public Types getTypes();
    
}
