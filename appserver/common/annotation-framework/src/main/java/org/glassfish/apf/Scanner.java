/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2026] Payara Foundation and/or affiliates

package org.glassfish.apf;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jvnet.hk2.annotations.Contract;

import java.io.File;
import java.io.IOException;
import java.util.Set;

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
     */
    void process(File archiveFile, T bundleDesc, ClassLoader classLoader) throws IOException;

    /**
     * Returns a ClassLoader capable of loading classes from the
     * underlying medium
     * @return a class loader capable of loading the classes
     */
    ClassLoader getClassLoader();

    /**
     * Return the classes that should be processed for annotations, resolved in the index.
     */
    Set<ClassInfo> getElements();

    /**
     * Return the classes with the given names that should be processed for annotations, e.g. the
     * classes changed by a hot redeployment.
     */
    Set<ClassInfo> getElements(Set<String> classNames);

    /**
     * Return the fields, constructors and methods of a class and its superclasses whose annotations
     * are to be processed.
     */
    ComponentInfo getComponentInfo(ClassInfo componentImpl);

    /**
     * @return the index over the scanned archives, completed by the classes the class loader can
     * see (super types, annotation types and their defaults), resolved without loading them
     */
    IndexView getIndex();
}
