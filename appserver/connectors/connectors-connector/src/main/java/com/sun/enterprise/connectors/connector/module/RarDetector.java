/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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


package com.sun.enterprise.connectors.connector.module;

import com.sun.enterprise.deploy.shared.FileArchive;
import org.glassfish.api.deployment.archive.ArchiveDetector;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import com.sun.enterprise.deployment.deploy.shared.Util;
import org.glassfish.deployment.common.GenericAnnotationDetector;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import javax.inject.Singleton;

import java.io.IOException;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Detects rar type archives.
 * It's rank can be set using system property {@link #RAR_DETECTOR_RANK_PROP}.
 * Default rank is {@link #DEFAULT_RAR_DETECTOR_RANK}.
 *
 * @author sanjeeb.sahoo@oracle.com
 */
@Service(name = RarDetector.ARCHIVE_TYPE)
@Singleton
public class RarDetector implements ArchiveDetector {
    private static final Class[] connectorAnnotations = new Class[]{
            javax.resource.spi.Connector.class};

    public static final String RAR_DETECTOR_RANK_PROP = "glassfish.rar.detector.rank";
    public static final int DEFAULT_RAR_DETECTOR_RANK = 300;
    public static final String ARCHIVE_TYPE = RarType.ARCHIVE_TYPE;
    @Inject
    private RarType archiveType;
    @Inject
    private ConnectorSniffer sniffer;
    @Inject
    private ServiceLocator services;

    private ArchiveHandler archiveHandler; // lazy initialisation
    private Logger logger = Logger.getLogger(getClass().getPackage().getName());

    private static final String RA_XML = "META-INF/ra.xml";
    private static final String RAR_EXTENSION = ".rar";
    @Override
    public int rank() {
        return Integer.getInteger(RAR_DETECTOR_RANK_PROP, DEFAULT_RAR_DETECTOR_RANK);
    }

    @Override
    public ArchiveHandler getArchiveHandler() {
        synchronized (this) {
            if (archiveHandler == null) {
                try {
                    sniffer.setup(null, logger);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                archiveHandler = services.getService(ArchiveHandler.class, ARCHIVE_TYPE);
            }
            return archiveHandler;
        }
    }

    @Override
    public ArchiveType getArchiveType() {
        return archiveType;
    }

    /**
     * {@inheritDoc}
     */
    public boolean handles(ReadableArchive archive) throws IOException {
        boolean handles = false;
        try{
            if (Util.getURIName(archive.getURI()).endsWith(RAR_EXTENSION)) {
                return true;
            }

            handles = archive.exists(RA_XML);
        }catch(IOException ioe){
            //ignore
        }
        if (!handles && (archive instanceof FileArchive)) {
            GenericAnnotationDetector detector =
                    new GenericAnnotationDetector(connectorAnnotations);
            handles = detector.hasAnnotationInArchive(archive);
        }
        return handles;
    }
}
