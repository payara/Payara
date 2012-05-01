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

import org.glassfish.api.ContractProvider;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.container.Sniffer;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.BaseServiceLocator;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Inhabitant;
import org.jvnet.hk2.component.Singleton;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

/**
 * This factory class is responsible for creating Archivists
 *
 * @author  Jerome Dochez
 */
@Service
@Scoped(Singleton.class)
public class ArchivistFactory implements ContractProvider {
    @Inject @Optional
    ExtensionsArchivist[] extensionsArchivists;

    @Inject
    BaseServiceLocator habitat;

    public Archivist getArchivist(String archiveType, ClassLoader cl) {
        Archivist result = getArchivist(archiveType);
        if(result != null) {
            result.setClassLoader(cl);
        }
        return result;
    }

    public Archivist getArchivist(String archiveType) {
        Archivist result = null;
        for (Inhabitant<?> inhabitant : ((Habitat) habitat).getInhabitants(ArchivistFor.class)) {
            String indexedType = inhabitant.metadata().get(ArchivistFor.class.getName()).get(0);
            if(indexedType.equals(archiveType)) {
                result = (Archivist) inhabitant.get();
            }
        }
        return result;
    }

    public Archivist getArchivist(ArchiveType moduleType) {
        return getArchivist(String.valueOf(moduleType));
    }

    public List<ExtensionsArchivist> getExtensionsArchivists(Collection<Sniffer> sniffers, ArchiveType moduleType) {
        Set<String> containerTypes = new HashSet<String>();
        for (Sniffer sniffer : sniffers) {
            containerTypes.add(sniffer.getModuleType());
        }
        List<ExtensionsArchivist> archivists = new ArrayList<ExtensionsArchivist>();
        for (String containerType : containerTypes) {
            for (Inhabitant<?> inhabitant : ((Habitat)habitat).getInhabitants(ExtensionsArchivistFor.class)) {
                String indexedType = inhabitant.metadata().get(ExtensionsArchivistFor.class.getName()).get(0);
                if(indexedType.endsWith(containerType)) {
                    ExtensionsArchivist ea = (ExtensionsArchivist) inhabitant.get();
                    if (ea.supportsModuleType(moduleType)) {
                        archivists.add(ea);
                    }
                }
            }
        }
        return archivists;
    }
}
