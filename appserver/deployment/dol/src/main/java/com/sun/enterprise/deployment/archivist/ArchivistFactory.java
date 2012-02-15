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

package com.sun.enterprise.deployment.archivist;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.ContractProvider;
import org.glassfish.api.deployment.archive.ReadableArchive;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Singleton;
import org.jvnet.hk2.component.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * This factory class is responsible for creating Archivists
 *
 * @author  Jerome Dochez
 */
@Service
@Scoped(Singleton.class)
public class ArchivistFactory implements ContractProvider, PostConstruct {

    @Inject
    Archivist[] archivists;

    @Inject
    CompositeArchivist[] compositeArchivists;

    @Inject @Optional
    ExtensionsArchivist[] extensionsArchivists;

    @Inject
    ArchiveFactory archiveFactory;

    @Inject
    Habitat habitat;

    public ArchivistFactory() {
        habitat = null;
    }
    
    public Archivist getArchivist(ReadableArchive archive,
        ClassLoader cl) throws IOException {
        Archivist archivist = getPrivateArchivistFor(archive);
        if (archivist!=null) {
            archivist.setClassLoader(cl);
        }
        return archivist;
    }


    public Archivist getArchivist(ArchiveType moduleType)
        throws IOException {
        return getPrivateArchivistFor(moduleType);
    }

    public List<ExtensionsArchivist> getExtensionsArchists(ArchiveType moduleType) {

        List<ExtensionsArchivist> archivists = new ArrayList<ExtensionsArchivist>();
        for (ExtensionsArchivist ea : extensionsArchivists) {
            if (ea.supportsModuleType(moduleType)) {
                archivists.add(ea);
            }
        }
        return archivists;
    }

    /**
     * Only archivists should have access to this API. we'll see how it works,
     * @param moduleType
     * @return
     * @throws IOException
     */
    Archivist getPrivateArchivistFor(ArchiveType moduleType)
        throws IOException {
        if (moduleType == null) return null;
        for (Archivist pa : archivists) {
            Archivist a = Archivist.class.cast(pa);
            if (a.getModuleType().equals(moduleType)) {
                return copyOf(a);
            }
        }
        return null;
    }

    /**
     * Only archivists should have access to this API. we'll see how it works,
     * @param archive
     * @return
     * @throws IOException
     */
    Archivist getPrivateArchivistFor(ReadableArchive archive)
        throws IOException {
        // do CompositeArchivist first
        Archivist a = getPrivateArchivistFor(archive, compositeArchivists); 
        if (a == null) {
            a = getPrivateArchivistFor(archive, archivists);
        }
        return a;
    }

    private Archivist getPrivateArchivistFor(ReadableArchive archive, 
        Object[] aa) throws IOException {
        //first, check the existence of any deployment descriptors
        for (Object pa : aa) {
            Archivist a = Archivist.class.cast(pa);
            if (a.hasStandardDeploymentDescriptor(archive) ||
                    a.hasRuntimeDeploymentDescriptor(archive)) {
                return copyOf(a);
                }
            }

        // Java EE 5 Specification: Section EE.8.4.2.1

        //second, check file extension if any, excluding .jar as it needs
        //additional processing
        String uri = archive.getURI().getPath();
        File file = new File(uri);
        if (!file.isDirectory() && !uri.endsWith(Archivist.EJB_EXTENSION)) {
            for (Object pa : aa) {
                Archivist a = Archivist.class.cast(pa);
                if (uri.endsWith(a.getArchiveExtension())) {
                    return copyOf(a);
                }
            }
        }

        //finally, still not returned here, call for additional processing
        for (Object pa : aa) {
            Archivist a = Archivist.class.cast(pa);
            if (a.postHandles(archive)) {
                return copyOf(a);
            }
        }

        return null;
    }

    private Archivist copyOf(Archivist a) {
        try {
            return habitat.getComponent(a.getClass());
//            return a.getClass().newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void postConstruct() {
        // move appclient archivist to the last to match as other archives 
        // could have Main-Class in Manifest also
        LinkedList<Archivist> sortedArchivists = new LinkedList<Archivist>();
        for (Archivist a : archivists) {
            if (a instanceof AppClientArchivist) {
                sortedArchivists.addLast(a);
            } else {
                sortedArchivists.addFirst(a);
            }
        }
        archivists = sortedArchivists.toArray(new Archivist[sortedArchivists.size()]);
    }

}
