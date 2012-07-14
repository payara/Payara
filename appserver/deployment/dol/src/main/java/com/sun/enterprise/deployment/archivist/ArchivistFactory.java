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

import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.IndexedFilter;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This factory class is responsible for creating Archivists
 *
 * @author  Jerome Dochez
 */
@Service
@Singleton
public class ArchivistFactory {
    public final static String ARCHIVE_TYPE = "archiveType";
    public final static String EXTENSION_ARCHIVE_TYPE = "extensionArchiveType";
    
    @Inject
    private ServiceLocator habitat;

    public Archivist getArchivist(String archiveType, ClassLoader cl) {
        Archivist result = getArchivist(archiveType);
        if(result != null) {
            result.setClassLoader(cl);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Archivist getArchivist(String archiveType) {
        ActiveDescriptor<Archivist> best = (ActiveDescriptor<Archivist>)
                habitat.getBestDescriptor(new ArchivistFilter(archiveType, ARCHIVE_TYPE, Archivist.class));
        if (best == null) return null;
        
        return habitat.getServiceHandle(best).getService();
    }

    public Archivist getArchivist(ArchiveType moduleType) {
        return getArchivist(String.valueOf(moduleType));
    }

    @SuppressWarnings("unchecked")
    public List<ExtensionsArchivist> getExtensionsArchivists(Collection<Sniffer> sniffers, ArchiveType moduleType) {
        Set<String> containerTypes = new HashSet<String>();
        for (Sniffer sniffer : sniffers) {
            containerTypes.add(sniffer.getModuleType());
        }
        List<ExtensionsArchivist> archivists = new ArrayList<ExtensionsArchivist>();
        for (String containerType : containerTypes) {
            List<ActiveDescriptor<?>> descriptors =
                    habitat.getDescriptors(
                    new ArchivistFilter(containerType, EXTENSION_ARCHIVE_TYPE, ExtensionsArchivist.class));
            
            for (ActiveDescriptor<?> item : descriptors) {
                
                ActiveDescriptor<ExtensionsArchivist> descriptor =
                        (ActiveDescriptor<ExtensionsArchivist>) item;
            
                ServiceHandle<ExtensionsArchivist> handle = habitat.getServiceHandle(descriptor);
                ExtensionsArchivist ea = handle.getService();
                if (ea.supportsModuleType(moduleType)) {
                    archivists.add(ea);
                }
            }
        }
        return archivists;
    }
    
    private static class ArchivistFilter implements IndexedFilter {
        private final String archiveType;
        private final String metadataKey;
        private final Class<?> index;
        
        private ArchivistFilter(String archiveType, String metadataKey, Class<?> index) {
            this.archiveType = archiveType;
            this.metadataKey = metadataKey;
            this.index = index;
        }

        /* (non-Javadoc)
         * @see org.glassfish.hk2.api.Filter#matches(org.glassfish.hk2.api.Descriptor)
         */
        @Override
        public boolean matches(Descriptor d) {
            Map<String, List<String>> metadata = d.getMetadata();
            
            List<String> values = metadata.get(metadataKey);
            if (values == null) {
                return false;
            }
            
            return values.contains(archiveType);
        }

        /* (non-Javadoc)
         * @see org.glassfish.hk2.api.IndexedFilter#getAdvertisedContract()
         */
        @Override
        public String getAdvertisedContract() {
            return index.getName();
        }

        /* (non-Javadoc)
         * @see org.glassfish.hk2.api.IndexedFilter#getName()
         */
        @Override
        public String getName() {
            return null;
        }
        
    }
    
    
}
